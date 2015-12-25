/**
 * @file sync/SyncTest.c
 * 
 * Part of NDNx Sync.
 */
/*
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2013 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */


#include "SyncActions.h"
#include "SyncBase.h"
#include "SyncHashCache.h"
#include "SyncNode.h"
#include "SyncPrivate.h"
#include "SyncRoot.h"
#include "SyncUtil.h"
#include "SyncTreeWorker.h"
#include "IndexSorter.h"

#include <errno.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/time.h>

#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/digest.h>
#include <ndn/fetch.h>
#include <ndn/seqwriter.h>
#include <ndn/uri.h>

#define MAX_READ_LEN 1000000
#define DEFAULT_CMD_TIMEOUT 6000

struct SyncTestParms {
    struct SyncBaseStruct *base;
    struct SyncRootStruct *root;
    int mode;
    int mark;
    int digest;
    int scope;
    int syncScope;
    int life;
    int sort;
    int bufs;
    int verbose;
    int resolve;
    int segmented;
    int noDup;
    int noSend;
    int blockSize;
    char *inputName;
    char *target;
    int nSplits;
    int *splits;
    struct timeval startTime;
    struct timeval stopTime;
    intmax_t fSize;
};

//////////////////////////////////////////////////////////////////////
// Dummy for ndnr routines (needed to avoid link errors)
//////////////////////////////////////////////////////////////////////

#include "sync_plumbing.h"


////////////////////////////////////////
// Error reporting
////////////////////////////////////////

static int
noteErr(const char *fmt, ...) {
    struct timeval t;
    va_list ap;
    struct ndn_charbuf *b = ndn_charbuf_create();
    ndn_charbuf_reserve(b, 1024);
    gettimeofday(&t, NULL);
    ndn_charbuf_putf(b, "** ERROR: %s\n", fmt);
    char *fb = ndn_charbuf_as_string(b);
    va_start(ap, fmt);
    vfprintf(stderr, fb, ap);
    va_end(ap);
    fflush(stderr);
    ndn_charbuf_destroy(&b);
    return -1;
}

////////////////////////////////////////
// Simple builder
////////////////////////////////////////

static int 
parseAndAccumName(char *s, struct SyncNameAccum *na) {
    int i = 0;
    for (;;) {
        char c = s[i];
        int d = SyncDecodeUriChar(c);
        if (d <= 0) break;
        i++;
    }
    char save = s[i];
    s[i] = 0;
    struct ndn_charbuf *cb = ndn_charbuf_create();
    int skip = ndn_name_from_uri(cb, (const char *) s);
    s[i] = save;
    if (skip <= 0) {
        // not legal, so don't append the name
        ndn_charbuf_destroy(&cb);
        return skip;
    }
    // extract the size, which is the next numeric string
    // (no significant checking here)
    intmax_t size = 0;
    for (;;) {
        char c = s[i];
        if (c >= '0' && c <= '9') break;
        if (c < ' ') break;
        i++;
    }
    for (;;) {
        char c = s[i];
        if (c < '0' || c > '9') break;
        size = size * 10 + SyncDecodeHexDigit(c);
        i++;
    }
    // finally, append the name in the order it arrived
    SyncNameAccumAppend(na, cb, size);
    return skip;    
}

static struct SyncNameAccum *
readAndAccumNames(FILE *input, int rem) {
    struct SyncNameAccum *na = SyncAllocNameAccum(4);
    static int tempLim = 4*1024;
    char *temp = NEW_ANY(tempLim+4, char);
    while (rem > 0) {
        // first, read a line
        int len = 0;
        while (len < tempLim) {
            int c = fgetc(input);
            if (c < 0 || c == '\n') break;
            temp[len] = c;
            len++;
        }
        temp[len] = 0;
        if (len == 0)
        // blank line stops us
        break;
        // now grab the name we found
        int pos = 0;
        static char *key = "ndn:";
        int keyLen = strlen(key);
        int found = 0;
        while (pos < len) {
            if (strncasecmp(temp+pos, key, keyLen) == 0) {
                // found the name start
                parseAndAccumName(temp+pos, na);
                found++;
                break;
            }
            pos++;
        }
        if (found == 0) {
            // did not get "ndn:" so try for "/" start
            for (pos = 0; pos < len; pos++) {
                if (temp[pos]== '/') {
                    parseAndAccumName(temp+pos, na);
                    break;
                }
            }
        }
        rem--;
    }
    free(temp);
    return na;
}

////////////////////////////////////////
// Tree print routines
////////////////////////////////////////

static void
printTreeInner(struct SyncTreeWorkerHead *head,
               struct ndn_charbuf *tmpB,
               struct ndn_charbuf *tmpD,
               FILE *f) {
    int i = 0;
    struct SyncTreeWorkerEntry *ent = SyncTreeWorkerTop(head);
    struct SyncHashCacheEntry *ce = ent->cacheEntry;
    if (ce == NULL) {
        fprintf(f, "?? no cacheEntry ??\n");
        return;
    }
    struct SyncNodeComposite *nc = ce->ncL;
    if (nc == NULL) nc = ce->ncR;
    if (nc == NULL) {
        fprintf(f, "?? no cacheEntry->nc ??\n");
        return;
    }
    for (i = 1; i < head->level; i++) fprintf(f, "  | ");
    char *hex = SyncHexStr(nc->hash->buf, nc->hash->length);
    fprintf(f, "node, depth = %d, refs = %d, leaves = %d, hash = %s\n",
            (int) nc->treeDepth, (int) nc->refLen, (int) nc->leafCount, hex);
    free(hex);
    ssize_t pos = 0;
    while (pos < nc->refLen) {
        struct SyncNodeElem *ep = &nc->refs[pos];
        ent->pos = pos;
        if (ep->kind & SyncElemKind_leaf) {
            // a leaf, so the element name is inline
            struct ndn_buf_decoder nameDec;
            struct ndn_buf_decoder *nameD = NULL;
            nameD = SyncInitDecoderFromOffset(&nameDec, nc, ep->start, ep->stop);
            ndn_charbuf_reset(tmpB);
            ndn_charbuf_reset(tmpD);
            SyncAppendElementInner(tmpB, nameD);
            ndn_uri_append(tmpD, tmpB->buf, tmpB->length, 1);
            for (i = 0; i < head->level; i++) fprintf(f, "  | ");
            fprintf(f, "%s\n", ndn_charbuf_as_string(tmpD));
        } else {
            // a node, so try this recursively
            SyncTreeWorkerPush(head);
            printTreeInner(head, tmpB, tmpD, f);
            SyncTreeWorkerPop(head);
        }
        pos++;
    }
}

static void
printTree(struct SyncTreeWorkerHead *head, FILE *f) {
    struct ndn_charbuf *tmpB = ndn_charbuf_create();
    struct ndn_charbuf *tmpD = ndn_charbuf_create();
    printTreeInner(head, tmpB, tmpD, f);
    ndn_charbuf_destroy(&tmpB);
    ndn_charbuf_destroy(&tmpD);
}

static void putMark(FILE *f) {
    struct timeval mark;
    gettimeofday(&mark, 0);
    fprintf(f, "%ju.%06u: ",
            (uintmax_t) mark.tv_sec,
            (unsigned) mark.tv_usec);
}

////////////////////////////////////////
// Test routines
////////////////////////////////////////

// generate the encoding of a test object 
static struct SyncNodeComposite *
testGenComposite(struct SyncBaseStruct *base, int nRefs) {
    int res = 0;
    struct SyncNodeComposite *nc = SyncAllocComposite(base);
    struct ndn_charbuf *tmp = ndn_charbuf_create();
    
    // append the references
    while (nRefs > 0 && res == 0) {
        ndn_charbuf_reset(tmp);
        res |= SyncAppendRandomName(tmp, 5, 12);
        SyncNodeAddName(nc, tmp);
        nRefs--;
    }
    
    SyncEndComposite(nc); // appends finals counts
    ndn_charbuf_destroy(&tmp);
    
    nc->err = res;
    return nc;
}

static int
testEncodeDecode(struct SyncTestParms *parms) {
    struct SyncBaseStruct *base = parms->base;
    struct ndn_charbuf *cb = ndn_charbuf_create();
    cb->length = 0;
    ndnb_element_begin(cb, NDN_DTAG_Content); // artificial!  only for testing!
    fwrite(cb->buf, sizeof(unsigned char), cb->length, stdout);
    
    struct SyncNodeComposite *nc = testGenComposite(base, 4);
    
    SyncWriteComposite(nc, stdout);
    
    struct ndn_buf_decoder ds;
    struct ndn_buf_decoder *d = SyncInitDecoderFromCharbuf(&ds, nc->cb, 0);
    struct SyncNodeComposite *chk = SyncAllocComposite(base);
    SyncParseComposite(chk, d);
    SyncWriteComposite(chk, stdout);
    SyncFreeComposite(chk);
    
    int pos = cb->length;
    ndnb_element_end(cb);  // NDN_DTAG_Content
    fwrite(cb->buf+pos, sizeof(unsigned char), cb->length-pos, stdout);
    fflush(stdout);
    
    SyncFreeComposite(nc);
    
    cb->length = 0;
    ndn_charbuf_destroy(&cb);
    
    return 0;
}

static int
testReader(struct SyncTestParms *parms) {
    char *fn = parms->inputName;
    int sort = parms->sort;
    FILE *f = fopen(fn, "r");
    int res = 0;
    if (f != NULL) {
        int64_t startTime = SyncCurrentTime();
        struct SyncNameAccum *na = readAndAccumNames(f, MAX_READ_LEN);
        fclose(f);
        struct ndn_charbuf *tmp = ndn_charbuf_create();
        int i = 0;
        IndexSorter_Base ixBase = NULL;
        int accumNameBytes = 0;
        int accumContentBytes = 0;
        if (sort > 0) {
            IndexSorter_Index ixLim = na->len;
            ixBase = IndexSorter_New(ixLim, -1);
            ixBase->sorter = SyncNameAccumSorter;
            ixBase->client = na;
            IndexSorter_Index ix = 0;
            for (; ix < ixLim; ix++) IndexSorter_Add(ixBase, ix);
        }
        struct ndn_charbuf *lag = NULL;
        for (;i < na->len; i++) {
            int j = i;
            if (ixBase != NULL) j = IndexSorter_Rem(ixBase);
            struct ndn_charbuf *each = na->ents[j].name;
            if (sort == 1 && lag != NULL) {
                int cmp = SyncCmpNames(each, lag);
                if (cmp < 0)
                return noteErr("bad sort (order)!");
                if (cmp == 0)
                return noteErr("bad sort (duplicate)!");
            }
            struct ndn_charbuf *repl = each;
            accumNameBytes = accumNameBytes + repl->length;
            ssize_t size = na->ents[j].data;
            accumContentBytes = accumContentBytes + size;
            ndn_charbuf_reset(tmp);
            ndn_uri_append(tmp, repl->buf, repl->length, 1);
            if (sort != 2) {
                fprintf(stdout, "%4d", i);
                if (sort) fprintf(stdout, ", %4d", j);
                fprintf(stdout, ", %8zd, ", size);
            }
            fprintf(stdout, "%s\n", ndn_charbuf_as_string(tmp));
            lag = each;
            if (repl != each) ndn_charbuf_destroy(&repl);
        }
        int64_t dt = SyncDeltaTime(startTime, SyncCurrentTime());
        dt = (dt + 500)/ 1000;
        fprintf(stdout, "-- %d names, %d name bytes, %d content bytes, %d.%03d seconds\n",
                na->len, accumNameBytes, accumContentBytes,
                (int) (dt / 1000), (int) (dt % 1000));
        if (ixBase != NULL) IndexSorter_Free(&ixBase);
        ndn_charbuf_destroy(&tmp);
        na = SyncFreeNameAccum(na);
    } else {
        return noteErr("testReader, could not open %s", fn);
    }
    return res;
}

static int
testReadBuilder(struct SyncTestParms *parms) {
    FILE *f = fopen(parms->inputName, "r");
    int ns = parms->nSplits;
    int res = 0;
    
    if (f != NULL) {
        struct SyncRootStruct *root = parms->root;
        
        if (root == NULL) {
            // need a new one
            struct ndn_charbuf *topo = ndn_charbuf_create();
            ndn_name_from_uri(topo, "/ndn/test/sync");
            
            struct ndn_charbuf *prefix = ndn_charbuf_create();
            ndn_name_from_uri(prefix, "/ndn/test");
            
            root = SyncAddRoot(parms->base,
                               parms->syncScope,
                               topo,
                               prefix,
                               NULL);
            parms->root = root;
            ndn_charbuf_destroy(&topo);
            ndn_charbuf_destroy(&prefix);
        }
        
        if (root->namesToAdd != NULL)
        SyncFreeNameAccum(root->namesToAdd);
        
        struct SyncLongHashStruct longHash;
        int split = 0;
        memset(&longHash, 0, sizeof(longHash));
        longHash.pos = MAX_HASH_BYTES;
        for (;;) {
            int i = 0;
            if (ns == 0) {
                root->namesToAdd = readAndAccumNames(f, MAX_READ_LEN);
            } else {
                int p = 0;
                int k = parms->splits[split];
                if (split > 0) p = parms->splits[split-1];
                if (k <= 0 || k >= ns) {
                    return noteErr("splits: bad k %d", k);
                    break;
                }
                if (p < 0 || p >= k) {
                    return noteErr("splits: bad p %d", k);
                    break;
                }
                root->namesToAdd = readAndAccumNames(f, k-p);
            }
            
            if (root->namesToAdd == NULL || root->namesToAdd->len <= 0)
            // the data ran out first
            break;
            
            for (i = 0; i < root->namesToAdd->len; i++) {
                SyncAccumHash(&longHash, root->namesToAdd->ents[i].name);
            }
            // TBD: fix this -- SyncUpdateRoot(root);
            
            struct ndn_charbuf *hb = SyncLongHashToBuf(&longHash);
            struct ndn_charbuf *rb = root->currentHash;
            if (rb->length != hb->length
                || memcmp(rb->buf, hb->buf, hb->length) != 0) {
                // this is not right!
                char *hexL = SyncHexStr(hb->buf, hb->length);
                char *hexR = SyncHexStr(rb->buf, rb->length);
                res = noteErr("hexL %s, hexR %s", hexL, hexR);
                free(hexL);
                free(hexR);
                return res;
            }
            ndn_charbuf_destroy(&hb);
            
            struct SyncHashCacheEntry *ce = SyncRootTopEntry(root);
            struct SyncTreeWorkerHead *tw = SyncTreeWorkerCreate(root->ch, ce);
            switch (parms->mode) {
                case 0: {
                    // no output
                    break;
                }
                case 1: {
                    // binary output
                    SyncWriteComposite(ce->ncL, stdout);
                    break;
                }
                case 2: {
                    // text output
                    SyncTreeWorkerInit(tw, ce);
                    printTree(tw, stdout);
                    fprintf(stdout, "-----------------------\n");
                    break;
                }
                default: {
                    // no output
                    break;
                }
            }
            
            // release intermediate resources
            tw = SyncTreeWorkerFree(tw);
            split++;
            if (ns > 0 && split >= ns) break;
        }
        
        fclose(f);
        return 0;
        
    } else {
        return noteErr("testReadBuilder, could not open %s", parms->inputName);
    }
}

// generate a simple canned root for routing
// based on anticipated root for the routing info
static struct SyncRootStruct *
genTestRootRouting(struct SyncTestParms *parms) {
    struct SyncBaseStruct *base = parms->base;
    struct ndn_charbuf *topoPrefix = ndn_charbuf_create();
    struct ndn_charbuf *namingPrefix = ndn_charbuf_create();
    
    ndn_name_from_uri(topoPrefix, "/ndn/test/sync");
    ndn_name_from_uri(namingPrefix, "/ndn/test/routing");
    struct SyncRootStruct *root = SyncAddRoot(base,
                                              parms->syncScope,
                                              topoPrefix,
                                              namingPrefix,
                                              NULL);
    ndn_charbuf_destroy(&topoPrefix);
    ndn_charbuf_destroy(&namingPrefix);
    return root;
}

// generate a simple canned root for repos
// based on anticipated root for the routing info
static struct SyncRootStruct *
genTestRootRepos(struct SyncTestParms *parms) {
    struct SyncBaseStruct *base = parms->base;
    struct ndn_charbuf *topoPrefix = ndn_charbuf_create();
    struct ndn_charbuf *namingPrefix = ndn_charbuf_create();
    
    ndn_name_from_uri(topoPrefix, "/ndn/test/sync");
    ndn_name_from_uri(namingPrefix, "/ndn/test/repos");
    
    struct SyncNameAccum *filter = SyncAllocNameAccum(4);
    struct ndn_charbuf *clause = ndn_charbuf_create();
    ndn_name_from_uri(clause, "/NDN");
    SyncNameAccumAppend(filter, clause, 0);
    
    struct SyncRootStruct *root = SyncAddRoot(base,
                                              parms->syncScope,
                                              topoPrefix,
                                              namingPrefix,
                                              filter);
    ndn_charbuf_destroy(&topoPrefix);
    ndn_charbuf_destroy(&namingPrefix);
    ndn_charbuf_destroy(&clause);
    SyncFreeNameAccum(filter);
    
    return root;
}

static struct SyncRootStruct *
testRootCoding(struct SyncTestParms *parms, struct SyncRootStruct *root) {
    struct SyncBaseStruct *base = parms->base;
    struct ndn_charbuf *cb1 = ndn_charbuf_create();
    int res = 0;
    SyncRootAppendSlice(cb1, root);  // generate the encoding
    
    SyncRemRoot(root);  // smoke test the removal
    
    struct ndn_buf_decoder ds;
    struct ndn_buf_decoder *d = SyncInitDecoderFromCharbuf(&ds, cb1, 0);
    root = SyncRootDecodeAndAdd(base, d);
    if (root == NULL) {
        res = noteErr("SyncRootDecodeAndAdd, failed");
    }
    if (res ==0) {
        // we have a root
        struct ndn_charbuf *cb2 = ndn_charbuf_create();
        SyncRootAppendSlice(cb2, root);
        
        if (res == 0) {
            // compare the encoding lengths
            if (cb1->length == 0 || cb1->length != cb2->length) {
                res = noteErr("testRootCoding, bad encoding lengths, %d != %d",
                              (int) cb1->length, (int) cb2->length);
            }
        }
        if (res == 0) {
            // compare the encoding contents
            ssize_t cmp = memcmp(cb1->buf, cb2->buf, cb1->length);
            if (cmp != 0) {
                res = noteErr("testRootCoding, bad encoding data",
                              (int) cb1->length, (int) cb2->length);
                res = -1;
            }
        }
        ndn_charbuf_destroy(&cb2);
    }
    ndn_charbuf_destroy(&cb1);
    
    if (res == 0) return root;
    
    SyncRemRoot(root);
    return NULL;
    
}

static int
testRootLookup (struct SyncTestParms *parms, struct SyncRootStruct *root,
                char * goodName, char * badName) {
    int res = 0;
    // now try a few lookups
    struct ndn_charbuf *name = ndn_charbuf_create();
    ndn_name_from_uri(name, goodName);
    enum SyncRootLookupCode ec = SyncRootLookupName(root, name);
    if (ec != SyncRootLookupCode_covered) {
        res = noteErr("testRootLookup, good name not covered, %s",
                      goodName);
    }
    ndn_charbuf_reset(name);
    ndn_name_from_uri(name, badName);
    ec = SyncRootLookupName(root, name);
    if (ec != SyncRootLookupCode_none) {
        res = noteErr("testRootLookup, bad name not rejected, %s",
                      badName);
    }
    return res;
}

static int
testRootBasic(struct SyncTestParms *parms) {
    int res = 0;
    struct SyncRootStruct *root = NULL;
    
    struct ndn_charbuf *cb = ndn_charbuf_create();
    uintmax_t val = 37;
    res |= SyncAppendTaggedNumber(cb, NDN_DTAG_SyncVersion, val);
    
    if (res == 0) {
        struct ndn_buf_decoder ds;
        struct ndn_buf_decoder *d = ndn_buf_decoder_start(&ds, cb->buf, cb->length);
        if (SyncParseUnsigned(d, NDN_DTAG_SyncVersion) != val
            || d->decoder.state < 0)
        res = -__LINE__;
    }
    
    if (res < 0) {
        return noteErr("testRootBasic, basic numbers failed, %d", res);
    }
    
    // test no filter
    root = genTestRootRouting(parms);
    root = testRootCoding(parms, root);
    res = testRootLookup(parms, root,
                         "ndn:/ndn/test/routing/XXX",
                         "ndn:/ndn/test/repos/NDN/XXX");
    SyncRemRoot(root);
    if (res < 0) return res;
    
    // test with filter
    root = genTestRootRepos(parms);
    root = testRootCoding(parms, root);
    res = testRootLookup(parms, root,
                         "ndn:/ndn/test/repos/NDN/XXX",
                         "ndn:/ndn/test/routing/XXX");
    SyncRemRoot(root);
    if (res < 0) {
        return noteErr("testRootBasic, failed");
    }
    
    return res;
}

static int
localStore(struct SyncTestParms *parms,
           struct ndn *ndn, struct ndn_charbuf *nm, struct ndn_charbuf *cb) {
    int res = 0;
    struct ndn_charbuf *template = SyncGenInterest(NULL,
                                                   1,  // always local
                                                   parms->life,
                                                   -1, -1, NULL);
    struct ndn_charbuf *tmp = ndn_charbuf_create();
    ndn_create_version(ndn, nm, NDN_V_NOW, 0, 0);
    ndn_charbuf_append_charbuf(tmp, nm);
    ndn_name_from_uri(tmp, "%C1.R.sw");
    ndn_name_append_nonce(tmp);
    ndn_get(ndn, tmp, NULL, DEFAULT_CMD_TIMEOUT, NULL, NULL, NULL, 0);
    ndn_charbuf_destroy(&tmp);
    ndn_charbuf_destroy(&template);
    if (res < 0) return res;
    
    struct ndn_charbuf *cob = ndn_charbuf_create();
    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;
    const void *cp = NULL;
    size_t cs = 0;
    if (cb != NULL) {
        sp.type = NDN_CONTENT_DATA;
        cp = (const void *) cb->buf;
        cs = cb->length;
    } else {
        sp.type = NDN_CONTENT_GONE;
    }
    ndn_name_append_numeric(nm, NDN_MARKER_SEQNUM, 0);
    sp.sp_flags |= NDN_SP_FINAL_BLOCK;
    res |= ndn_sign_content(ndn,
                            cob,
                            nm,
                            &sp,
                            cp,
                            cs);
    res |= ndn_put(ndn, (const void *) cob->buf, cob->length);
    // ndn_run(ndn, 150);
    ndn_charbuf_destroy(&cob);
    return res;
}

static int
sendSlice(struct SyncTestParms *parms,
          char *topo, char *prefix,
          int count, char **clauses) {
    // constructs a simple config slice and sends it to an attached repo
    struct ndn_charbuf *cb = ndn_charbuf_create();
    struct ndn_charbuf *hash = ndn_charbuf_create();
    struct ndn_charbuf *nm = ndn_charbuf_create();
    int i = 0;
    int res = 0;
    res |= ndnb_element_begin(cb, NDN_DTAG_SyncConfigSlice);
    res |= SyncAppendTaggedNumber(cb, NDN_DTAG_SyncVersion, SLICE_VERSION);
    res |= ndn_name_from_uri(nm, topo);
    res |= ndn_charbuf_append_charbuf(cb, nm);
    res |= ndn_name_from_uri(nm, prefix);
    res |= ndn_charbuf_append_charbuf(cb, nm);
    res |= ndnb_element_begin(cb, NDN_DTAG_SyncConfigSliceList);
    for (i = 0; i < count ; i++) {
        res |= SyncAppendTaggedNumber(cb, NDN_DTAG_SyncConfigSliceOp, 0);
        res |= ndn_name_from_uri(nm, clauses[i]);
        res |= ndn_charbuf_append_charbuf(cb, nm);
    }
    res |= ndnb_element_end(cb);
    res |= ndnb_element_end(cb);
    
    if (res >= 0) {
        // now we have the encoding, so make the hash
        struct ndn *ndn = NULL;
        struct ndn_digest *cow = ndn_digest_create(NDN_DIGEST_DEFAULT);
        size_t sz = ndn_digest_size(cow);
        unsigned char *dst = ndn_charbuf_reserve(hash, sz);
        ndn_digest_init(cow);
        ndn_digest_update(cow, cb->buf, cb->length);
        ndn_digest_final(cow, dst, sz);
        hash->length = sz;
        ndn_digest_destroy(&cow);
        
        // form the Sync protocol name
        static char *localLit = "\xC1.M.S.localhost";
        static char *sliceCmd = "\xC1.S.cs";
        res |= ndn_name_init(nm);
        res |= ndn_name_append_str(nm, localLit);
        res |= ndn_name_append_str(nm, sliceCmd);
        res |= ndn_name_append(nm, hash->buf, hash->length);
        
        if (parms->noSend) {
            // don't send the slice, just print the hash as a URI
            struct ndn_charbuf *hName = ndn_charbuf_create();
            ndn_name_init(hName);
            ndn_name_append(hName, hash->buf, hash->length);
            struct ndn_charbuf *uri = SyncUriForName(hName);
            fprintf(stdout, "%s\n", ndn_charbuf_as_string(uri));
            ndn_charbuf_destroy(&hName);
            ndn_charbuf_destroy(&uri);
            return 0;
        }
        
        ndn = ndn_create();
        if (ndn_connect(ndn, NULL) == -1) {
            perror("Could not connect to ndnd");
            exit(1);
        }
        if (res >= 0) res |= localStore(parms, ndn, nm, cb);
        if (res < 0) {
            res = noteErr("sendSlice, failed");
        } else {
            if (parms->mode != 0) {
                struct ndn_charbuf *uri = SyncUriForName(nm);
                if (parms->mark) putMark(stdout);
                fprintf(stdout, "sendSlice, sent %s\n",
                        ndn_charbuf_as_string(uri));
                ndn_charbuf_destroy(&uri);
            }
        }
        
        ndn_destroy(&ndn);
    }
    
    ndn_charbuf_destroy(&cb);
    ndn_charbuf_destroy(&hash);
    ndn_charbuf_destroy(&nm);
    if (res > 0) res = 0;
    return res;
}

struct storeFileStruct {
    struct SyncTestParms *parms;
    struct ndn_charbuf *nm;
    struct ndn_charbuf *cb;
    struct ndn *ndn;
    off_t bs;
    off_t fSize;
    FILE *file;
    unsigned char *segData;
    int nSegs;
    int stored;
    struct ndn_charbuf *template;
};

static int64_t
segFromInfo(struct ndn_upcall_info *info) {
	// gets the current segment number for the info
	// returns -1 if not known
	if (info == NULL) return -1;
	const unsigned char *ndnb = info->content_ndnb;
	struct ndn_indexbuf *cc = info->content_comps;
	if (cc == NULL || ndnb == NULL) {
		// go back to the interest
		cc = info->interest_comps;
		ndnb = info->interest_ndnb;
		if (cc == NULL || ndnb == NULL) return -1;
	}
	int ns = cc->n;
	if (ns > 2) {
		// assume that the segment number is the last component
		int start = cc->buf[ns - 2];
		int stop = cc->buf[ns - 1];
		if (start < stop) {
			size_t len = 0;
			const unsigned char *data = NULL;
			ndn_ref_tagged_BLOB(NDN_DTAG_Component, ndnb, start, stop, &data, &len);
			if (len > 0 && data != NULL) {
				// parse big-endian encoded number
				// TBD: where is this in the library?
				if (data[0] == NDN_MARKER_SEQNUM) {
                    int64_t n = 0;
                    int i = 0;
                    for (i = 1; i < len; i++) {
                        n = n * 256 + data[i];
                    }
                    return n;
                }
			}
		}
	}
	return -1;
}

static enum ndn_upcall_res
storeHandler(struct ndn_closure *selfp,
             enum ndn_upcall_kind kind,
             struct ndn_upcall_info *info) {
    struct storeFileStruct *sfd = selfp->data;
    enum ndn_upcall_res ret = NDN_UPCALL_RESULT_OK;
    switch (kind) {
        case NDN_UPCALL_FINAL:
        free(selfp);
        break;
        case NDN_UPCALL_INTEREST: {
            int64_t seg = segFromInfo(info);
            if (seg < 0) seg = 0;
            struct ndn_charbuf *uri = ndn_charbuf_create();
            ndn_uri_append(uri, sfd->nm->buf, sfd->nm->length, 0);
            char *str = ndn_charbuf_as_string(uri);
            ret = NDN_UPCALL_RESULT_INTEREST_CONSUMED;
            if (seg >= 0 && seg < sfd->nSegs) {
                struct ndn_charbuf *name = SyncCopyName(sfd->nm);
                struct ndn_charbuf *cb = ndn_charbuf_create();
                struct ndn_charbuf *cob = ndn_charbuf_create();
                off_t bs = sfd->bs;
                off_t pos = seg * bs;
                off_t rs = sfd->fSize - pos;
                if (rs > bs) rs = bs;
                
                ndn_charbuf_reserve(cb, rs);
                cb->length = rs;
                char *cp = ndn_charbuf_as_string(cb);
                
                // fill in the contents
                int res = fseeko(sfd->file, pos, SEEK_SET);
                if (res >= 0) {
                    res = fread(cp, rs, 1, sfd->file);
                    if (res < 0) {
                        char *eMess = strerror(errno);
                        fprintf(stderr, "ERROR in fread, %s, seg %d, %s\n",
                                eMess, (int) seg, str);
                    }
                } else {
                    char *eMess = strerror(errno);
                    fprintf(stderr, "ERROR in fseeko, %s, seg %d, %s\n",
                            eMess, (int) seg, str);
                }
                
                if (res >= 0) {
                    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;
                    const void *cp = NULL;
                    sp.type = NDN_CONTENT_DATA;
                    cp = (const void *) cb->buf;
                    sp.template_ndnb = sfd->template;
                    
                    if (seg+1 == sfd->nSegs) sp.sp_flags |= NDN_SP_FINAL_BLOCK;
                    ndn_name_append_numeric(name, NDN_MARKER_SEQNUM, seg);
                    res |= ndn_sign_content(sfd->ndn,
                                            cob,
                                            name,
                                            &sp,
                                            cp,
                                            rs);
                    if (sfd->parms->digest) {
                        // not sure if this generates the right hash
                        struct ndn_parsed_ContentObject pcos;
                        ndn_parse_ContentObject(cob->buf, cob->length,
                                                &pcos, NULL);
                        ndn_digest_ContentObject(cob->buf, &pcos);
                        if (pcos.digest_bytes > 0)
                            res |= ndn_name_append(name, pcos.digest, pcos.digest_bytes);
                    }
                    res |= ndn_put(sfd->ndn, (const void *) cob->buf, cob->length);
                    
                    if (res < 0) {
                        return noteErr("seg %d, %s",
                                       (int) seg,
                                       str);
                    } else if (sfd->parms->verbose) {
                        if (sfd->parms->mark) putMark(stdout);
                        struct ndn_charbuf *nameUri = ndn_charbuf_create();
                        ndn_uri_append(nameUri, name->buf, name->length, 0);
                        char *nameStr = ndn_charbuf_as_string(nameUri);
                        fprintf(stdout, "put seg %d, %s\n",
                                (int) seg,
                                nameStr);
                        ndn_charbuf_destroy(&nameUri);
                    }
                    
                    // update the tracking
                    unsigned char uc = sfd->segData[seg];
                    if (uc == 0) {
                        uc++;
                        sfd->stored++;
                    } else {
                        if (sfd->parms->noDup) {
                            fprintf(stderr,
                                    "ERROR in storeHandler, duplicate segment request, seg %d, %s\n",
                                    (int) seg, str);
                        }
                        if (uc < 255) uc++;
                    }
                    sfd->segData[seg] = uc;
                }
                
                ndn_charbuf_destroy(&name);
                ndn_charbuf_destroy(&cb);
                ndn_charbuf_destroy(&cob);
                
            }
            ndn_charbuf_destroy(&uri);
            break;
        }
        default:
        ret = NDN_UPCALL_RESULT_ERR;
        break;
    }
    return ret;
}

static void
formatStats(struct SyncTestParms *parms) {
    int64_t dt = (1000000*(parms->stopTime.tv_sec-parms->startTime.tv_sec)
                  + parms->stopTime.tv_usec-parms->startTime.tv_usec);
    if (dt <= 0) dt = 1;
    int64_t rate = 0;
    
    switch (parms->mode) {
        case 0: {
            // silent
            break;
        }
        case 3: {
            // catchunks2 compatible
            const char *expid = getenv("NDN_EXPERIMENT_ID");
            const char *sep = " ";
            if (expid == NULL) {
                expid = "";
                sep = "";
            }
            rate = (parms->fSize * 1000000) / dt;
            if (parms->mark) putMark(stderr);
            fprintf(stderr,
                    "%ld.%06u SyncTest[%d]: %s%s"
                    "%jd bytes transferred in %ld.%06u seconds (%ld bytes/sec)"
                    "\n",
                    (long) parms->stopTime.tv_sec,
                    (unsigned) parms->stopTime.tv_usec,
                    (int)getpid(),
                    expid,
                    sep,
                    (intmax_t) parms->fSize,
                    (long) (dt / 1000000),
                    (unsigned) (dt % 1000000),
                    (long) rate
                    );
            break;
        }
        default: {
            // brief mode
            dt = (dt + 500) / 1000;
            if (dt <= 0) dt = 1;
            rate = parms->fSize / dt;
            
            if (parms->mark) putMark(stdout);
            fprintf(stdout, "transferred %jd bytes in %d.%03d seconds = %d.%03d MB/sec\n",
                    (intmax_t) parms->fSize,
                    (int) (dt / 1000), (int) dt % 1000,
                    (int) (rate / 1000), (int) rate % 1000);
            break;
        }
        
    }
}

static int
getFile(struct SyncTestParms *parms, char *src, char *dst) {
    // gets the file, stores it to stdout
    
    FILE *file = NULL;
    
    if (dst != NULL) {
        file = fopen(dst, "w");
        if (file == NULL) {
            perror("fopen failed");
            return -1;
        }
    }
    
    struct ndn *ndn = NULL;
    ndn = ndn_create();
    // special case to remove verification overhead
    if (dst == NULL)
    ndn_defer_verification(ndn, 1);
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        return -1;
    }
    struct ndn_charbuf *cb = ndn_charbuf_create();
    struct ndn_charbuf *nm = ndn_charbuf_create();
    int bs = parms->blockSize;
    
    int res = ndn_name_from_uri(nm, src);
    if (res < 0) {
        perror("ndn_name_from_uri failed");
        return -1;
    }
    
    if (parms->resolve) {
        res = ndn_resolve_version(ndn, nm, NDN_V_HIGH, parms->life*1000);
        // TBD: use parms to determine versioning_flags and timeout_ms?
        if (res < 0) {
            perror("ndn_resolve_version failed");
            return -1;
        }
    }
    
    struct ndn_fetch *cf = ndn_fetch_new(ndn);
    struct ndn_charbuf *template = SyncGenInterest(NULL,
                                                   parms->scope,
                                                   parms->life,
                                                   -1, -1, NULL);
    
    if (parms->verbose) {
        ndn_fetch_set_debug(cf, stderr,
                            ndn_fetch_flags_NoteOpenClose
                            | ndn_fetch_flags_NoteNeed
                            | ndn_fetch_flags_NoteFill
                            | ndn_fetch_flags_NoteTimeout
                            | ndn_fetch_flags_NoteFinal);
    }
    gettimeofday(&parms->startTime, 0);
    
    if (parms->segmented == 0) {
        // no segments, so use a single get
        struct ndn_parsed_ContentObject pcos;
        res = ndn_get(ndn, nm, template,
                      parms->life*1000,
                      cb, &pcos, NULL, 0);
        ndn_charbuf_destroy(&template);
        if (res < 0) {
            perror("get failed");
            return -1;
        }
        if (file != NULL) {
            size_t nItems = fwrite(ndn_charbuf_as_string(cb), cb->length, 1, file);
            if (nItems < 1) {
                perror("fwrite failed");
                return -1;
            }
        }
        parms->fSize = parms->fSize + cb->length;
        
    } else {
        // segmented, so use fetch.h
        struct ndn_fetch_stream *fs = ndn_fetch_open(cf, nm,
                                                     "SyncTest",
                                                     template,
                                                     parms->bufs,
                                                     0, 0);
        ndn_charbuf_destroy(&template);
        if (fs == NULL) {
            perror("ndn_fetch_open failed");
            return -1;
        }
        ndn_charbuf_reserve(cb, bs);
        cb->length = bs;
        char *cp = ndn_charbuf_as_string(cb);
        
        for (;;) {
            intmax_t av = ndn_fetch_avail(fs);
            if (av == NDN_FETCH_READ_NONE) {
                res = ndn_run(ndn, 1);
                if (res < 0) {
                    perror("ndn_run failed");
                    return -1;
                }
                continue;
            }
            int nb = ndn_fetch_read(fs, cp, bs);
            if (nb > 0) {
                if (file != NULL) {
                    size_t nItems = fwrite(cp, nb, 1, file);
                    if (nItems < 1) {
                        perror("fwrite failed");
                        exit(1);
                    }
                }
                parms->fSize = parms->fSize + nb;
            } else if (nb == NDN_FETCH_READ_NONE) {
                // try again
                res = ndn_run(ndn, 1);
                if (res < 0) {
                    perror("ndn_run failed");
                    return -1;
                }                
            } else {
                if (nb == NDN_FETCH_READ_END) break;
                if (nb == NDN_FETCH_READ_TIMEOUT) {
                    perror("read failed, timeout");
                    exit(1);
                }
                char temp[256];
                snprintf(temp, sizeof(temp), "ndn_fetch_read failed: %d", nb);
                perror(temp);
                return -1;
            }
        }
        ndn_fetch_close(fs);
    }
    
    gettimeofday(&parms->stopTime, 0);
    
    if (file != NULL)
    fclose(file);
    
    ndn_fetch_destroy(cf);
    
    ndn_destroy(&ndn);
    ndn_charbuf_destroy(&cb);
    ndn_charbuf_destroy(&nm);
    
    formatStats(parms);
    
    if (res > 0) res = 0;
    return res;
}

static int
putFile(struct SyncTestParms *parms, char *src, char *dst) {
    // stores the src file to the dst file (in the repo)
    
    struct stat myStat;
    int res = stat(src, &myStat);
    if (res < 0) {
        perror("putFile, stat failed");
        return -1;
    }
    off_t fSize = myStat.st_size;
    
    if (fSize == 0) {
        return noteErr("putFile, stat failed, empty src");
    }
    FILE *file = fopen(src, "r");
    if (file == NULL) {
        perror("putFile, fopen failed");
        return -1;
    }
    
    struct ndn *ndn = NULL;
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        return noteErr("putFile, could not connect to ndnd");
    }
    struct ndn_charbuf *cb = ndn_charbuf_create();
    struct ndn_charbuf *nm = ndn_charbuf_create();
    struct ndn_charbuf *cmd = ndn_charbuf_create();
    int bs = parms->blockSize;
    
    res = ndn_name_from_uri(nm, dst);
    if (res < 0) {
        return noteErr("putFile, ndn_name_from_uri failed");
    }
    ndn_create_version(ndn, nm, NDN_V_NOW, 0, 0);
    
    struct storeFileStruct *sfData = NEW_STRUCT(1, storeFileStruct);
    sfData->parms = parms;
    sfData->file = file;
    sfData->bs = bs;
    sfData->nm = nm;
    sfData->cb = cb;
    sfData->ndn = ndn;
    sfData->fSize = fSize;
    sfData->nSegs = (fSize + bs -1) / bs;
    sfData->segData = NEW_ANY(sfData->nSegs, unsigned char);
    
    {
        // make a template to govern the timestamp for the segments
        // this allows duplicate segment requests to return the same hash
        const unsigned char *vp = NULL;
        ssize_t vs;
        SyncGetComponentPtr(nm, SyncComponentCount(nm)-1, &vp, &vs);
        if (vp != NULL && vs > 0) {
            sfData->template = ndn_charbuf_create();
            ndnb_element_begin(sfData->template, NDN_DTAG_SignedInfo);
            ndnb_append_tagged_blob(sfData->template, NDN_DTAG_Timestamp, vp, vs);
            ndnb_element_end(sfData->template);
        } else return noteErr("putFile, create store template failed");
    }
    
    struct ndn_charbuf *template = SyncGenInterest(NULL,
                                                   parms->scope,
                                                   parms->life,
                                                   -1, -1, NULL);
    struct ndn_closure *action = NEW_STRUCT(1, ndn_closure);
    action->p = storeHandler;
    action->data = sfData;
    
    parms->fSize = fSize;
    
    // fire off a listener
    res = ndn_set_interest_filter(ndn, nm, action);
    if (res < 0) {
        return noteErr("putFile, ndn_set_interest_filter failed");
    }
    res = ndn_run(ndn, 40);
    if (res < 0) {
        return noteErr("putFile, ndn_run failed");
    }
    // initiate the write
    // construct the store request and "send" it as an interest
    ndn_charbuf_append_charbuf(cmd, nm);
    ndn_name_from_uri(cmd, "%C1.R.sw");
    ndn_name_append_nonce(cmd);
    
    if (parms->verbose && parms->mode != 0) {
        struct ndn_charbuf *uri = SyncUriForName(nm);
        if (parms->mark) putMark(stdout);
        fprintf(stdout, "put init, %s\n",
                ndn_charbuf_as_string(uri));
        ndn_charbuf_destroy(&uri);
    }
    gettimeofday(&parms->startTime, 0);
    ndn_get(ndn, cmd, template, DEFAULT_CMD_TIMEOUT, NULL, NULL, NULL, 0);
    ndn_charbuf_destroy(&template);
    if (res < 0) {
        return noteErr("putFile, ndn_get failed");
    }
    
    // wait for completion
    res = 0;
    while (res == 0 && sfData->stored < sfData->nSegs) {
        res = ndn_run(ndn, 2);
    }
    if (res < 0) {
        return noteErr("putFile, ndn_run failed while storing");
    }
    
    gettimeofday(&parms->stopTime, 0);
    
    res = ndn_set_interest_filter(ndn, nm, NULL);
    if (res < 0) {
        return noteErr("putFile, ndn_set_interest_filter failed (removal)");
    }
    res = ndn_run(ndn, 40);
    if (res < 0) {
        return noteErr("putFile, ndn_run failed");
    }
    
    ndn_charbuf_destroy(&sfData->template);
    free(sfData->segData);
    free(sfData);
    ndn_destroy(&ndn);
    fclose(file);
    ndn_charbuf_destroy(&cb);
    ndn_charbuf_destroy(&cmd);
    ndn_charbuf_destroy(&nm);
    
    formatStats(parms);
    
    if (res > 0) res = 0;
    return res;
}

extern int
appendComponents(struct ndn_charbuf *dst,
                 const struct ndn_charbuf *src,
                 int start, int len) {
    struct ndn_buf_decoder sbd;
    struct ndn_buf_decoder *s = SyncInitDecoderFromCharbuf(&sbd, src, 0);
    int count = 0;
    int pos = 0;
    if (!ndn_buf_match_dtag(s, NDN_DTAG_Name))
        // src is not a name
        return -__LINE__;
    ndn_buf_advance(s);
    int lim = start + len;
    while (count < lim) {
        if (!ndn_buf_match_dtag(s, NDN_DTAG_Component)) {
            ndn_buf_check_close(s);
            if (SyncCheckDecodeErr(s)) return -__LINE__;
            break;
        }
        ndn_buf_advance(s);
        const unsigned char *cPtr = NULL;
        size_t cSize = 0;
        if (ndn_buf_match_blob(s, &cPtr, &cSize)) ndn_buf_advance(s);
        if (cPtr == NULL)
            return -__LINE__;
        if (count >= start) {
            if (ndn_name_append(dst, cPtr, cSize) < 0)
                return -__LINE__;
        }
        count++;
        ndn_buf_check_close(s);
        if (SyncCheckDecodeErr(s)) return -__LINE__;
        pos++;
    }
    return count;
}

static int
putFileList(struct SyncTestParms *parms, char *listName) {
    struct ndn *ndn = NULL;
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        return noteErr("putFile, could not connect to ndnd");
    }
    FILE *listFile = fopen(listName, "r");
    if (listFile == NULL) {
        return noteErr("putFileList, failed to open list file");
    }
    int ret = 0;
    struct SyncNameAccum *na = readAndAccumNames(listFile, MAX_READ_LEN);
    int i = 0;
    fclose(listFile);
    struct ndn_charbuf *tmp = ndn_charbuf_create();
    struct ndn_charbuf *template = SyncGenInterest(NULL,
                                                   parms->scope,
                                                   parms->life,
                                                   -1, -1, NULL);
    while (i < na->len) {
        tmp->length = 0;
        ndn_name_init(tmp);
        struct ndn_charbuf *each = na->ents[i].name;
        int nc = SyncComponentCount(each);
        if (parms->verbose) {
            struct ndn_charbuf *uri = SyncUriForName(each);
            if (parms->mark) putMark(stdout);
            fprintf(stdout, "putFileList %d, %s\n",
                    i, ndn_charbuf_as_string(uri));
            fflush(stdout);
            ndn_charbuf_destroy(&uri);
        }
        if (nc < 3) {
            ret = noteErr("putFileList, bad name");
            break;
        }
        const unsigned char *xp = NULL;
        ssize_t xs = -1;
        SyncGetComponentPtr(each, nc-2, &xp, &xs);
        if (xs > 0 && xp[0] == '\000') {
            // segment info, so split the name
            ret |= appendComponents(tmp, each, 0, nc-2);
            ret |= ndn_name_append_str(tmp, "\xC1.R.sw-c");
            ret |= ndn_name_append_nonce(tmp);
            ret |= appendComponents(tmp, each, nc-2, 2);
        } else {
            // no segment, so use the whole name
            ret |= appendComponents(tmp, each, 0, nc);
            ret |= ndn_name_append_str(tmp, "\xC1.R.sw-c");
            ret |= ndn_name_append_nonce(tmp);
        }
        
        if (ret < 0) {
            ret = noteErr("putFileList, bad name");
            break;
        }
        ndn_get(ndn, tmp, template, DEFAULT_CMD_TIMEOUT, NULL, NULL, NULL, 0);
        ret = ndn_run(ndn, 10);
        if (ret < 0) {
            ret = noteErr("putFileList, ndn_run failed");
            break;
        }
        i++;
    }
    ndn_charbuf_destroy(&template);
    ndn_charbuf_destroy(&tmp);
    na = SyncFreeNameAccumAndNames(na);
    ndn_destroy(&ndn);
    return ret;
}

static int
existingRootOp(struct SyncTestParms *parms,
               char *topo, char *prefix,
               int delete) {
    // constructs a simple config slice and sends it to an attached repo
    // now we have the encoding, so make the hash
    struct ndn *ndn = NULL;
    int res = 0;
    
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    
    // form the Sync protocol name
    static char *cmdLit = "\xC1.S.rs";
    struct ndn_charbuf *nm = ndn_charbuf_create();
    if (delete) cmdLit = "\xC1.S.cs";
    
    res |= ndn_name_init(nm);
    res |= ndn_name_from_uri(nm, topo);
    if (prefix != NULL) {
        struct ndn_charbuf *pre = ndn_charbuf_create();
        res |= ndn_name_from_uri(pre, prefix);
        res |= ndn_name_append_str(nm, cmdLit);
        res |= SyncAppendAllComponents(nm, pre);
        ndn_charbuf_destroy(&pre);
    }
    
    struct ndn_charbuf *cb = ndn_charbuf_create();
    if (delete) {
        // requesting deletion
        res |= localStore(parms, ndn, nm, NULL);
        if (res < 0) {
            res = noteErr("requestDelete, failed");
        } else {
            // claimed success 
            struct ndn_charbuf *uri = SyncUriForName(nm);
            if (parms->mark) putMark(stdout);
            fprintf(stdout, "requestDelete, sent %s\n",
                    ndn_charbuf_as_string(uri));
            ndn_charbuf_destroy(&uri);
        }
    } else {
        // requesting stats
        struct ndn_charbuf *tmpl = SyncGenInterest(NULL, 1, 2, -1, 1, NULL);
        res |= ndn_get(ndn, nm, tmpl, DEFAULT_CMD_TIMEOUT, cb, NULL, NULL, 0);
        
        const unsigned char *xp = NULL;
        size_t xs = 0;
        if (res < 0) {
            res = noteErr("requestStats, ndn_get failed");
        } else {
            res |= SyncPointerToContent(cb, NULL, &xp, &xs);
            
            if (res < 0 || xs == 0) {
                res = noteErr("requestStats, failed");
            } else {
                if (parms->mark) putMark(stdout);
                fwrite(xp, xs, sizeof(char), stdout);
                fprintf(stdout, "\n");
            }
        }
        ndn_charbuf_destroy(&tmpl);
    }
    ndn_charbuf_destroy(&cb);
    ndn_charbuf_destroy(&nm);
    ndn_destroy(&ndn);
    if (res > 0) res = 0;
    return res;
}

static void
my_r_sync_msg(struct sync_plumbing *sd, const char *fmt, ...) {
    if (sd != NULL && fmt != NULL) {
        va_list ap;
        va_start(ap, fmt);
        vfprintf(stdout, fmt, ap);
        va_end(ap);
    }
}

struct sync_plumbing_client_methods client_methods = {
    &my_r_sync_msg,
    NULL,
    NULL,
    NULL,
    NULL
};

static void
SyncFreeBase(struct SyncBaseStruct *base) {
    struct sync_plumbing *sd = base->sd;
    struct ndn_charbuf *state_buf = ndn_charbuf_create();
    sd->sync_methods->sync_stop(sd, state_buf);
    ndn_charbuf_destroy(&state_buf);
}

// TBD: make this NOT cloned, but also not taken from ndnr!
int
ndnr_msg_level_from_string(const char *s)
{
    long v;
    char *ep;
    
    if (s == NULL || s[0] == 0)
        return(1);
    if (0 == strcasecmp(s, "NONE"))
        return(NDNL_NONE);
    if (0 == strcasecmp(s, "SEVERE"))
        return(NDNL_SEVERE);
    if (0 == strcasecmp(s, "ERROR"))
        return(NDNL_ERROR);
    if (0 == strcasecmp(s, "WARNING"))
        return(NDNL_WARNING);
    if (0 == strcasecmp(s, "INFO"))
        return(NDNL_INFO);
    if (0 == strcasecmp(s, "FINE"))
        return(NDNL_FINE);
    if (0 == strcasecmp(s, "FINER"))
        return(NDNL_FINER);
    if (0 == strcasecmp(s, "FINEST"))
        return(NDNL_FINEST);
    v = strtol(s, &ep, 10);
    if (v > NDNL_FINEST || v < 0 || ep[0] != 0)
        return(-1);
    return(v);
}

int
main(int argc, char **argv) {
    int i = 1;
    int seen = 0;
    int res = 0;
    struct sync_plumbing sdStruct;
    struct sync_plumbing *sd = &sdStruct;
    struct SyncTestParms parmStore;
    struct SyncTestParms *parms = &parmStore;
    
    sd->client_methods = &client_methods;
    struct SyncBaseStruct *base = SyncNewBase(sd);
    
    memset(parms, 0, sizeof(parmStore));
    
    parms->mode = 1;
    parms->scope = 1;
    parms->syncScope = 2;
    parms->life = 4;
    parms->bufs = 4;
    parms->blockSize = 4096;
    parms->base = base;
    parms->resolve = 1;
    parms->segmented = 1;
    
    while (i < argc && res >= 0) {
        char * sw = argv[i];
        i++;
        char *arg1 = NULL;
        char *arg2 = NULL;
        if (i < argc) arg1 = argv[i];
        if (i+1 < argc) arg2 = argv[i+1];
        if (strcasecmp(sw, "-debug") == 0 || strcasecmp(sw, "-d") == 0) {
            i++;
            base->debug = ndnr_msg_level_from_string(arg1);
            if (base->debug < 0) {
                res = noteErr("invalid debug level %s", arg1);
            }
        } else if (strcasecmp(sw, "-v") == 0) {
            parms->verbose = 1;
        } else if (strcasecmp(sw, "-cat2") == 0) {
            parms->mode = 3;
        } else if (strcasecmp(sw, "-mark") == 0) {
            parms->mark = 1;
        } else if (strcasecmp(sw, "-digest") == 0) {
            parms->digest = 1;
        } else if (strcasecmp(sw, "-null") == 0) {
            parms->mode = 0;
        } else if (strcasecmp(sw, "-binary") == 0) {
            parms->mode = 1;
        } else if (strcasecmp(sw, "-ndnb") == 0) {
            parms->mode = 1;
        } else if (strcasecmp(sw, "-text") == 0) {
            parms->mode = 2;
        } else if (strcasecmp(sw, "-nodup") == 0) {
            parms->noDup = 1;
        } else if (strcasecmp(sw, "-nores") == 0) {
            parms->resolve = 0;
        } else if (strcasecmp(sw, "-noseg") == 0) {
            parms->segmented = 0;
        } else if (strcasecmp(sw, "-nosend") == 0) {
            parms->noSend = 1;
        } else if (strcasecmp(sw, "-bs") == 0) {
            i++;
            if (arg1 != NULL) {
                int bs = atoi(arg1);
                if (bs <= 0 || bs > 64*1024) {
                    res = noteErr("invalid block size %s", arg1);
                }
                parms->blockSize = bs;
            } else
            res = noteErr("missing block size");
            seen++;
        } else if (strcasecmp(sw, "-bufs") == 0) {
            if (arg1 != NULL) {
                i++;
                int bufs = atoi(arg1);
                if (bufs <= 0 || bufs > 1024) {
                    res = noteErr("invalid number of buffers %s", arg1);
                    break;
                }
                parms->bufs = bufs;
            } else 
            res = noteErr("missing number of buffers");
        } else if (strcasecmp(sw, "-scope") == 0) {
            if (arg1 != NULL) {
                int scope = atoi(arg1);
                if (scope < -1 || scope > 2) {
                    res = noteErr("invalid scope %s", arg1);
                    break;
                }
                parms->scope = scope;
                i++;
            } else
                res = noteErr("missing scope");
            seen++;
        } else if (strcasecmp(sw, "-syncScope") == 0) {
            if (arg1 != NULL) {
                int scope = atoi(arg1);
                if (scope < -1 || scope > 2) {
                    res = noteErr("invalid scope %s", arg1);
                    break;
                }
                parms->syncScope = scope;
                i++;
            } else
                res = noteErr("missing scope");
            seen++;
        } else if (strcasecmp(sw, "-life") == 0) {
            if (arg1 != NULL) {
                int life = atoi(arg1);
                if (life < -1 || life > 30) {
                    res = noteErr("invalid interest lifetime %s", arg1);
                    break;
                }
                parms->life = life;
                i++;
            } else
            res = noteErr("missing interest lifetime");
            seen++;
        } else if (strcasecmp(sw, "-basic") == 0) {
            res = testRootBasic(parms);
            seen++;
        } else if (strcasecmp(sw, "-target") == 0) {
            if (arg1 != NULL) {
                parms->target = arg1;
                i++;
            } else
            res = noteErr("missing target");
            seen++;
        } else if (strcasecmp(sw, "-build") == 0) {
            if (arg1 != NULL) {
                i++;
                parms->inputName = arg1;
                res = testReadBuilder(parms);
            } else
            res = noteErr("missing file name");
            seen++;
        } else if (strcasecmp(sw, "-read") == 0) {
            if (arg1 != NULL) {
                i++;
                parms->inputName = arg1;
                parms->sort = 0;
                res = testReader(parms);
            } else
            res = noteErr("missing file name");
            seen++;
        } else if (strcasecmp(sw, "-sort") == 0) {
            if (arg1 != NULL) {
                i++;
                parms->inputName = arg1;
                parms->sort = 1;
                res = testReader(parms);
            } else
            res = noteErr("missing file name");
            seen++;
        } else if (strcasecmp(sw, "-abs") == 0) {
            if (arg1 != NULL) {
                i++;
                parms->inputName = arg1;
                parms->sort = 2;
                res = testReader(parms);
            } else
            res = noteErr("missing file name");
            seen++;
        } else if (strcasecmp(sw, "-splits") == 0) {
            int n = 0;
            while (i >= argc) {
                char *x = argv[i];
                char c = x[0];
                if (c < '0' || c > '9') break;
                n++;
                i++;
            }
            parms->nSplits = n;
            if (parms->splits != NULL) free(parms->splits);
            parms->splits = NULL;
            if (n > 0) {
                int j = 0;
                parms->splits = NEW_ANY(n, int);
                i = i - n;
                while (j < n) {
                    parms->splits[j] = atoi(argv[i]);
                    i++;
                    j++;
                }
            }
            seen++;
        } else if (strcasecmp(sw, "-encode") == 0) {
            res = testEncodeDecode(parms);
            seen++;
        } else if (strcasecmp(sw, "-slice") == 0) {
            char **clauses = NEW_ANY(argc, char *);
            int count = 0;
            if (arg1 != NULL && arg2 != NULL) {
                i++;
                i++;
                while (i < argc) {
                    char *clause = argv[i];
                    if (clause[0] == '-' || clause[0] == 0) break;
                    i++;
                    clauses[count] = clause;
                    count++;
                }
                res = sendSlice(parms, arg1, arg2, count, clauses);
            } else
            res = noteErr("missing slice topo or prefix");
            seen++;
        } else if (strcasecmp(sw, "-get") == 0) {
            if (arg1 != NULL) {
                i++;
                if (arg2 != NULL) {
                    // dst is optional, elide if it looks like a switch
                    if (arg2[0] != '-') i++;
                    else arg2 = NULL;
                }
                res = getFile(parms, arg1, arg2);
            } else {
                res = noteErr("missing src file");
            }
            seen++;
        } else if (strcasecmp(sw, "-put") == 0) {
            if (arg1 == NULL) {
                res = noteErr("missing src file");
            } else if (arg2 == NULL) {
                res = noteErr("missing dst file");
            } else {
                i++;
                i++;
                res = putFile(parms, arg1, arg2);
            }
            seen++;
        } else if (strcasecmp(sw, "-putList") == 0) {
            if (arg1 == NULL) {
                res = noteErr("missing list file");
            } else {
                i++;
                i++;
                res = putFileList(parms, arg1);
            }
            seen++;
        } else if (strcasecmp(sw, "-stats") == 0) {
            if (arg1 != NULL && arg2 != NULL) {
                i++;
                i++;
                res = existingRootOp(parms, arg1, arg2, 0);
            } else
            res = noteErr("missing topo or hash");
            seen++;
        } else if (strcasecmp(sw, "-delete") == 0) {
            if (arg1 != NULL && arg2 != NULL) {
                i++;
                i++;
                res = existingRootOp(parms, arg1, arg2, 1);
            } else
            res = noteErr("missing topo or hash");
            seen++;
		} else {
            // can't understand this sw
            noteErr("invalid switch: %s", sw);
            seen = 0;
            break;
        }
    }
    if (parms->splits != NULL) free(parms->splits);
    if (parms->root != NULL) SyncRemRoot(parms->root);
    SyncFreeBase(base);
    if (seen == 0 && res >= 0) {
        printf("usage: \n");
        printf("    -debug S        set debug level {NONE, SEVERE, ERROR, WARNING, INFO, FINE, FINER, FINEST}\n");
        printf("    -v              verbose\n");
        printf("    -null           no output\n");
        printf("    -ndnb           use binary output\n");
        printf("    -binary         use binary output\n");
        printf("    -text           use text output\n");
        printf("    -cat2           use ndncatchunks2 format\n");
        printf("    -mark           print a time code prefix\n");
        printf("    -digest         show the digest when doing a put\n");
        printf("    -nodup          disallow duplicate segment requests for -put\n");
        printf("    -nores          avoid resolve version\n");
        printf("    -noseg          no segments\n");
        printf("    -nosend         no send of the slice\n");
        printf("    -scope N        scope=N for repo commands (default 1)\n");
        printf("    -life N         life=N for interests (default 4)\n");
        printf("    -bs N           set block size for put (default 4096)\n");
        printf("    -bufs N         number of buffers for get (default 4)\n");
        printf("    -basic          some very basic tests\n");
        printf("    -read F         read names from file F\n");
        printf("    -sort F         read names from file F, sort them\n");
        printf("    -encode         simple encode/decode test\n");
        printf("    -build F        build tree from file F\n");
        printf("    -get src [dst]  src is uri in repo, dst is file name (optional)\n");
        printf("    -put src dst    src is file name, dst is uri in repo\n");
        printf("    -putList L      does checked write of each name, L is file name of name list\n");
        printf("    -slice T P C*   topo, prefix, clause ... (send slice to repo)\n");
        printf("    -delete T H     delete root with topo T, hash H from the repo\n");
        printf("    -stats T H      print statistics for root with topo T, hash H\n");
    }
    return res;
}
