/**
 * @file ndnbuzz.c
 * Pre-reads stuff written by ndnsendchunks, produces no output.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2011, 2013 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * Pre-reads stuff written by ndnsendchunks, produces no output
 * This is meant to be run in parallel with ndncatchunks to experiment
 * with the benefits of one kind of pipelining.
 *
 * The idea is to use the Exclude Bloom filters to artificially divide the 
 * possible interests into several different classes.  For example, you
 * might use 8 bits per Bloom filter, and just one hash function, so the
 * 8 different filters
 *    B0 = 01111111
 *    B1 = 10111111
 *      ...
 *    B8 = 11111110
 * will serve to partition the interests into 8 different classes and so at any
 * given time and node there can be 8 different pending interests for the prefix.
 * When a piece of content arrives at the endpoint, a new interest is issued
 * that uses the same Bloom filter, but is restricted to content with a larger
 * sequence number than the content that just arrived.
 * The "real" consumer gets its content by explicitly using the sequence
 * numbers in its requests; almost all of these will get fulfilled out of a
 * nearby cache and so few of the actual interests will need to propagate
 * out to the network.
 * Note that this scheme does not need to be aware of the sequence numbering
 * algorithm; it only relies on them to be increasing according to the
 * canonical ordering.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ndn/bloom.h>
#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] [-n count] ndn:/a/b\n"
            "   Pre-reads stuff written by ndnsendchunks, produces no output\n"
            "   -a - allow stale data\n"
            "   -n count - specify number of pipeline slots\n",
            progname);
    exit(1);
}

struct mydata {
    int allow_stale;
};

static void
append_bloom_element(struct ndn_charbuf *templ,
                     enum ndn_dtag dtag, struct ndn_bloom *b)
{
        int i;
        ndn_charbuf_append_tt(templ, dtag, NDN_DTAG);
        i = ndn_bloom_wiresize(b);
        ndn_charbuf_append_tt(templ, i, NDN_BLOB);
        ndn_bloom_store_wire(b, ndn_charbuf_reserve(templ, i), i);
        templ->length += i;
        ndn_charbuf_append_closer(templ);
}

/*
 * This appends a tagged, valid, fully-saturated Bloom filter, useful for
 * excluding everything between two 'fenceposts' in an Exclude construct.
 */
static void
append_bf_all(struct ndn_charbuf *c)
{
    unsigned char bf_all[9] = { 3, 1, 'A', 0, 0, 0, 0, 0, 0xFF };
    const struct ndn_bloom_wire *b = ndn_bloom_validate_wire(bf_all, sizeof(bf_all));
    if (b == NULL) abort();
    ndn_charbuf_append_tt(c, NDN_DTAG_Bloom, NDN_DTAG);
    ndn_charbuf_append_tt(c, sizeof(bf_all), NDN_BLOB);
    ndn_charbuf_append(c, bf_all, sizeof(bf_all));
    ndn_charbuf_append_closer(c);
}

static struct ndn_bloom *
make_partition(unsigned i, int lg_n)
{
    struct ndn_bloom_wire template = {0};
    struct ndn_bloom *ans = NULL;
    unsigned j;
    
    if (lg_n > 13 || i >= (1U << lg_n)) abort();
    if (lg_n >= 3)
        template.lg_bits = lg_n;
    else
        template.lg_bits = 3;
    template.n_hash = 1;
    template.method = 'A';
    memset(template.bloom, ~0, sizeof(template.bloom));
    /* This loop is here to replicate out to a byte if lg_n < 3 */
    for (j = i; j < (1U << template.lg_bits); j += (1U << lg_n))
        template.bloom[j / 8] -= (1U << (j % 8));
    ans = ndn_bloom_from_wire(&template, 8 + (1 << (template.lg_bits - 3)));
    return(ans);
}

struct ndn_charbuf *
make_template(struct mydata *md, struct ndn_upcall_info *info, struct ndn_bloom *b)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    const unsigned char *ib = NULL; /* info->interest_ndnb */
    const unsigned char *cb = NULL; /* info->content_ndnb */
    struct ndn_indexbuf *cc = NULL;
    struct ndn_parsed_interest *pi = NULL;
    struct ndn_buf_decoder decoder;
    struct ndn_buf_decoder *d;
    size_t start;
    size_t stop;
    
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    // XXX - use pubid if possible
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 2);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    if (info != NULL) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_Exclude, NDN_DTAG);
        ib = info->interest_ndnb;
        cb = info->content_ndnb;
        cc = info->content_comps;
        append_bf_all(templ);
        /* Insert the last Component in the filter */
        ndn_charbuf_append(templ,
                           cb + cc->buf[cc->n - 2],
                           cc->buf[cc->n - 1] - cc->buf[cc->n - 2]);
        if (b == NULL) {
            /* Look for Bloom in the matched interest */
            pi = info->pi;
            if (pi->offset[NDN_PI_E_Exclude] > pi->offset[NDN_PI_B_Exclude]) {
                start = stop = 0;
                d = ndn_buf_decoder_start(&decoder,
                                          ib + pi->offset[NDN_PI_B_Exclude],
                                          pi->offset[NDN_PI_E_Exclude] -
                                          pi->offset[NDN_PI_B_Exclude]);
                if (!ndn_buf_match_dtag(d, NDN_DTAG_Exclude))
                    d->decoder.state = -1;
                ndn_buf_advance(d);
                if (ndn_buf_match_dtag(d, NDN_DTAG_Bloom)) {
                    start = pi->offset[NDN_PI_B_Exclude] + d->decoder.token_index;
                    ndn_buf_advance(d);
                    if (ndn_buf_match_blob(d, NULL, NULL))
                        ndn_buf_advance(d);
                    ndn_buf_check_close(d);
                    stop = pi->offset[NDN_PI_B_Exclude] + d->decoder.token_index;
                }
                if (ndn_buf_match_dtag(d, NDN_DTAG_Component)) {
                    ndn_buf_advance(d);
                    if (ndn_buf_match_blob(d, NULL, NULL))
                        ndn_buf_advance(d);
                    ndn_buf_check_close(d);
                    start = pi->offset[NDN_PI_B_Exclude] + d->decoder.token_index;
                    if (ndn_buf_match_dtag(d, NDN_DTAG_Bloom)) {
                        ndn_buf_advance(d);
                        if (ndn_buf_match_blob(d, NULL, NULL))
                            ndn_buf_advance(d);
                        ndn_buf_check_close(d);
                    }
                    stop = pi->offset[NDN_PI_B_Exclude] + d->decoder.token_index;
                }
                if (d->decoder.state >= 0)
                    ndn_charbuf_append(templ, ib + start, stop - start);                
            }
        }
        else {
            /* Use the supplied Bloom */
            append_bloom_element(templ, NDN_DTAG_Bloom, b);
        }
        ndn_charbuf_append_closer(templ); /* </Exclude> */
    }
    else if (b != NULL) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_Exclude, NDN_DTAG);
        append_bloom_element(templ, NDN_DTAG_Bloom, b);
        ndn_charbuf_append_closer(templ); /* </Exclude> */
    }
    if (md->allow_stale) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
        ndnb_append_number(templ,
                                                NDN_AOK_DEFAULT | NDN_AOK_STALE);
        ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
    }
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

static enum ndn_upcall_res
incoming_content(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    const unsigned char *ndnb = NULL;
    size_t ndnb_size = 0;
    const unsigned char *data = NULL;
    size_t data_size = 0;
    const unsigned char *cb = NULL; /* info->content_ndnb */
    struct ndn_indexbuf *cc = NULL;
    int res;
    struct mydata *md = selfp->data;
    
    if (kind == NDN_UPCALL_FINAL) {
        if (md != NULL) {
            selfp->data = NULL;
            free(md);
            md = NULL;
        }
        return(NDN_UPCALL_RESULT_OK);
    }
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT)
        return(NDN_UPCALL_RESULT_REEXPRESS);
    if (kind != NDN_UPCALL_CONTENT && kind != NDN_UPCALL_CONTENT_UNVERIFIED)
        return(NDN_UPCALL_RESULT_ERR);
    if (md == NULL)
        selfp->data = md = calloc(1, sizeof(*md));
    ndnb = info->content_ndnb;
    ndnb_size = info->pco->offset[NDN_PCO_E];
    cb = info->content_ndnb;
    cc = info->content_comps;
    res = ndn_content_get_value(ndnb, ndnb_size, info->pco, &data, &data_size);
    if (res < 0) abort();
    
    /* Ask for the next one */
    name = ndn_charbuf_create();
    ndn_name_init(name);
    if (cc->n < 2) abort();
    res = ndn_name_append_components(name, cb, cc->buf[0], cc->buf[cc->n - 1]);
    if (res < 0) abort();
    templ = make_template(md, info, NULL);
    // XXX - this program might not work correctly anymore
    res = ndn_express_interest(info->h, name, /* info->pi->prefix_comps,*/ selfp, templ);
    if (res < 0) abort();
    
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&name);
    
    return(NDN_UPCALL_RESULT_OK);
}

int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_closure *incoming = NULL;
    const char *arg = NULL;
    int res;
    int opt;
    struct mydata *mydata;
    int allow_stale = 0;
    int lg_n = 3;
    unsigned n = 8;
    int i;
    
    while ((opt = getopt(argc, argv, "han:")) != -1) {
        switch (opt) {
            case 'a':
                allow_stale = 1;
                break;
            case 'n':
                n = atoi(optarg);
                if (n < 2 || n > 8*1024) {
                    fprintf(stderr, "invalid -n value\n");
                    usage(argv[0]);
                }
                break;
            case 'h':
            default:
                usage(argv[0]);
        }
    }
    for (lg_n = 0; (1U << lg_n) < n; lg_n++)
        continue;
    n = 1U << lg_n;
    arg = argv[optind];
    if (arg == NULL)
        usage(argv[0]);
    name = ndn_charbuf_create();
    res = ndn_name_from_uri(name, arg);
    if (res < 0) {
        fprintf(stderr, "%s: bad ndn URI: %s\n", argv[0], arg);
        exit(1);
    }
    if (argv[optind + 1] != NULL)
        fprintf(stderr, "%s warning: extra arguments ignored\n", argv[0]);
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    incoming = calloc(1, sizeof(*incoming));
    incoming->p = &incoming_content;
    mydata = calloc(1, sizeof(*mydata));
    mydata->allow_stale = allow_stale;
    incoming->data = mydata;
    
    for (i = 0; i < n; i++) {
        struct ndn_bloom *b = make_partition(i, lg_n);
        templ = make_template(mydata, NULL, b);
        ndn_express_interest(ndn, name, incoming, templ);
        ndn_charbuf_destroy(&templ);
        ndn_bloom_destroy(&b);
    }
    
    ndn_charbuf_destroy(&name);
    while (res >= 0) {
        res = ndn_run(ndn, 1000);
    }
    ndn_destroy(&ndn);
    exit(res < 0);
}
