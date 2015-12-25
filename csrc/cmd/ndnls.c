/**
 * @file ndnls.c
 * Attempts to list name components available at the next level of the hierarchy.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011 Palo Alto Research Center, Inc.
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
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>

struct upcalldata {
    int magic; /* 856372 */
    long *counter;
    unsigned warn;
    unsigned option;
    int n_excl;
    int scope;
    struct ndn_charbuf **excl; /* Array of n_excl items */
};

#define MUST_VERIFY 0x01

static int /* for qsort */
namecompare(const void *a, const void *b)
{
    const struct ndn_charbuf *aa = *(const struct ndn_charbuf **)a;
    const struct ndn_charbuf *bb = *(const struct ndn_charbuf **)b;
    int ans = ndn_compare_names(aa->buf, aa->length, bb->buf, bb->length);
    if (ans == 0)
        fprintf(stderr, "wassat? %d\n", __LINE__);
    return (ans);
}

enum ndn_upcall_res
incoming_content(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct ndn_charbuf *c = NULL;
    struct ndn_charbuf *comp = NULL;
    struct ndn_charbuf *uri = NULL;
    struct ndn_charbuf *templ = NULL;
    const unsigned char *ndnb = NULL;
    size_t ndnb_size = 0;
    struct ndn_indexbuf *comps = NULL;
    int matched_comps = 0;
    int res;
    int i;
    struct upcalldata *data = selfp->data;
    
    if (data->magic != 856372) abort();
    if (kind == NDN_UPCALL_FINAL)
        return(NDN_UPCALL_RESULT_OK);
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT)
        return(NDN_UPCALL_RESULT_REEXPRESS);
    if (kind == NDN_UPCALL_CONTENT_UNVERIFIED) {
        if ((data->option & MUST_VERIFY) != 0)
        return(NDN_UPCALL_RESULT_VERIFY);
        }
    else if (kind != NDN_UPCALL_CONTENT) abort();
    
    ndnb = info->content_ndnb;
    ndnb_size = info->pco->offset[NDN_PCO_E];
    comps = info->content_comps;
    matched_comps = info->pi->prefix_comps;
    c = ndn_charbuf_create();
    uri = ndn_charbuf_create();
    templ = ndn_charbuf_create();
    /* note that comps->n is 1 greater than the number of explicit components */
    if (matched_comps > comps->n) {
        ndn_uri_append(c, ndnb, ndnb_size, 1);
        fprintf(stderr, "How did this happen?  %s\n", ndn_charbuf_as_string(uri));
        exit(1);
    }
    data->counter[0]++;
    /* Recover the same prefix as before */
    ndn_name_init(c);
    res = ndn_name_append_components(c, info->interest_ndnb,
                                     info->interest_comps->buf[0],
                                     info->interest_comps->buf[matched_comps]);
    if (res < 0) abort();
    
    comp = ndn_charbuf_create();
    ndn_name_init(comp);
    if (matched_comps + 1 == comps->n) {
        /* Reconstruct the implicit ContentObject digest component */
        ndn_digest_ContentObject(ndnb, info->pco);
        ndn_name_append(comp, info->pco->digest, info->pco->digest_bytes);
    }
    else if (matched_comps < comps->n) {
        ndn_name_append_components(comp, ndnb,
                                   comps->buf[matched_comps],
                                   comps->buf[matched_comps + 1]);
    }
    res = ndn_uri_append(uri, comp->buf, comp->length, 0);
    if (res < 0 || uri->length < 1)
        fprintf(stderr, "*** Error: ndnls line %d res=%d\n", __LINE__, res);
    else {
        if (uri->length == 1)
            ndn_charbuf_append(uri, ".", 1);
        printf("%s%s\n", ndn_charbuf_as_string(uri) + 1,
               kind == NDN_UPCALL_CONTENT ? " [verified]" : " [unverified]");
    }
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append(templ, c->buf, c->length); /* Name */
    if (matched_comps == comps->n) {
        /* The interest supplied the digest component */
        ndn_charbuf_destroy(&comp);
        /*
         * We can't rely on the Exclude filter to keep from seeing this, so 
         * say that we need at least one more name component.
         */
        ndn_charbuf_append_tt(templ, NDN_DTAG_MinSuffixComponents, NDN_DTAG);
        ndn_charbuf_append_tt(templ, 1, NDN_UDATA);
        ndn_charbuf_append(templ, "1", 1);
        ndn_charbuf_append_closer(templ); /* </MinSuffixComponents> */
    }
    else {
        data->excl = realloc(data->excl, (data->n_excl + 1) * sizeof(data->excl[0]));
        data->excl[data->n_excl++] = comp;
        comp = NULL;
    }
    qsort(data->excl, data->n_excl, sizeof(data->excl[0]), &namecompare);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Exclude, NDN_DTAG);
    for (i = 0; i < data->n_excl; i++) {
        comp = data->excl[i];
        if (comp->length < 4) abort();
        ndn_charbuf_append(templ, comp->buf + 1, comp->length - 2);
    }
    comp = NULL;
    ndn_charbuf_append_closer(templ); /* </Exclude> */
    ndnb_tagged_putf(templ, NDN_DTAG_AnswerOriginKind, "%d", NDN_AOK_CS);
    if (data->scope > -1)
       ndnb_tagged_putf(templ, NDN_DTAG_Scope, "%d", data->scope);
    ndn_charbuf_append_closer(templ); /* </Interest> */
    if (templ->length > data->warn) {
        fprintf(stderr, "*** Interest packet is %d bytes\n", (int)templ->length);
        data->warn = data->warn * 8 / 5;
    }
    ndn_express_interest(info->h, c, selfp, templ);
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&c);
    ndn_charbuf_destroy(&uri);
    return(NDN_UPCALL_RESULT_OK);
}

void
usage(const char *prog)
{
    fprintf(stderr, "Usage: %s uri\n"
            "   Prints names with uri as prefix\n"
            "     environment var NDN_SCOPE is scope for interests (0, 1 or 2, no default)\n"
            "     environment var NDN_LINGER is no-data timeout (seconds) default 0.5s\n"
            "     environment var NDN_VERIFY indicates signature verification is required (non-zero)\n", prog);
    exit(1);
}

int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *c = NULL;
    struct ndn_charbuf *templ = NULL;
    struct upcalldata *data = NULL;
    int i;
    int n;
    int res;
    long counter = 0;
    struct ndn_closure *cl = NULL;
    int timeout_ms = 500;
    const char *env_timeout = getenv("NDN_LINGER");
    const char *env_verify = getenv("NDN_VERIFY");
    const char *env_scope = getenv("NDN_SCOPE");

    if (argv[1] == NULL || argv[2] != NULL)
        usage(argv[0]);

    if (env_timeout != NULL && (i = atoi(env_timeout)) > 0)
        timeout_ms = i * 1000;

    c = ndn_charbuf_create();
    res = ndn_name_from_uri(c, argv[1]);
    if (res < 0)
        usage(argv[0]);
        
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    
    data = calloc(1, sizeof(*data));
    data->magic = 856372;
    data->warn = 1492;
    data->counter = &counter;
    data->option = 0;
    if (env_verify && *env_verify)
        data->option |= MUST_VERIFY;
    data->scope = -1;
    if (env_scope != NULL && (i = atoi(env_scope)) >= 0)
      data->scope = i;
    cl = calloc(1, sizeof(*cl));
    cl->p = &incoming_content;
    cl->data = data;
    if (data->scope > -1) {
        templ = ndn_charbuf_create();
        ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
        ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
        ndn_charbuf_append_closer(templ); /* </Name> */
        ndnb_tagged_putf(templ, NDN_DTAG_Scope, "%d", data->scope);
        ndn_charbuf_append_closer(templ); /* </Interest> */
    }
    ndn_express_interest(ndn, c, cl, templ);
    ndn_charbuf_destroy(&templ);
    cl = NULL;
    data = NULL;
    for (i = 0;; i++) {
        n = counter;
        ndn_run(ndn, timeout_ms); /* stop if we run dry for 1/2 sec */
        fflush(stdout);
        if (counter == n)
            break;
    }
    ndn_destroy(&ndn);
    ndn_charbuf_destroy(&c);
    exit(0);
}
