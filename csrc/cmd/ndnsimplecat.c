/**
 * @file ndnsimplecat.c
 * Reads streams at the given NDNx URIs and writes to stdout
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2011 Palo Alto Research Center, Inc.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>

/**
 * Provide usage hints for the program and then exit with a non-zero status.
 */
static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] ndn:/a/b ...\n"
            "   Reads streams at"
            " the given ndn URIs and writes to stdout\n"
            "   -a - allow stale data\n",
            progname);
    exit(1);
}

struct mydata {
    int *done;
    int allow_stale;
};

/**
 * Construct a template suitable for use with ndn_express_interest
 * indicating at least one suffix component, and stale data if so
 * requested.
 */
struct ndn_charbuf *
make_template(struct mydata *md, struct ndn_upcall_info *info)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    // XXX - use pubid if possible
    ndn_charbuf_append_tt(templ, NDN_DTAG_MinSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MinSuffixComponents> */
    if (md->allow_stale) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
        ndnb_append_number(templ, NDN_AOK_DEFAULT | NDN_AOK_STALE);
        ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
    }
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

/**
 * Handle the incoming content messages. Extracts the data, and
 * requests the next block in sequence if the received block was
 * not the final one.
 */
enum ndn_upcall_res
incoming_content(struct ndn_closure *selfp,
                 enum ndn_upcall_kind kind,
                 struct ndn_upcall_info *info)
{
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    const unsigned char *ndnb = NULL;
    size_t ndnb_size = 0;
    const unsigned char *data = NULL;
    size_t data_size = 0;
    size_t written;
    const unsigned char *ib = NULL; /* info->interest_ndnb */
    struct ndn_indexbuf *ic = NULL;
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
    if (kind == NDN_UPCALL_CONTENT_UNVERIFIED)
        return(NDN_UPCALL_RESULT_VERIFY);
    if (kind != NDN_UPCALL_CONTENT)
        return(NDN_UPCALL_RESULT_ERR);
    if (md == NULL)
        selfp->data = md = calloc(1, sizeof(*md));
    ndnb = info->content_ndnb;
    ndnb_size = info->pco->offset[NDN_PCO_E];
    ib = info->interest_ndnb;
    ic = info->interest_comps;
    res = ndn_content_get_value(ndnb, ndnb_size, info->pco, &data, &data_size);
    if (res < 0) abort();
    if (info->pco->type != NDN_CONTENT_DATA) {
        /* For us this is spam. For now, give up. */
        fprintf(stderr, "*** spammed at block %d\n", (int)selfp->intdata);
        exit(1);
    }
    
    /* OK, we will accept this block. */
    if (data_size == 0)
        *(md->done) = 1;
    else {
        written = fwrite(data, data_size, 1, stdout);
        if (written != 1)
            exit(1);
    }
    // XXX The test below should get refactored into the library
    if (info->pco->offset[NDN_PCO_B_FinalBlockID] !=
        info->pco->offset[NDN_PCO_E_FinalBlockID]) {
        const unsigned char *finalid = NULL;
        size_t finalid_size = 0;
        const unsigned char *nameid = NULL;
        size_t nameid_size = 0;
        struct ndn_indexbuf *cc = info->content_comps;
        ndn_ref_tagged_BLOB(NDN_DTAG_FinalBlockID, ndnb,
                            info->pco->offset[NDN_PCO_B_FinalBlockID],
                            info->pco->offset[NDN_PCO_E_FinalBlockID],
                            &finalid,
                            &finalid_size);
        if (cc->n < 2) abort();
        ndn_ref_tagged_BLOB(NDN_DTAG_Component, ndnb,
                            cc->buf[cc->n - 2],
                            cc->buf[cc->n - 1],
                            &nameid,
                            &nameid_size);
        if (finalid_size == nameid_size &&
              0 == memcmp(finalid, nameid, nameid_size))
            *(md->done) = 1;
    }
    
    if (*(md->done)) {
        ndn_set_run_timeout(info->h, 0);
        return(NDN_UPCALL_RESULT_OK);
    }
    
    /* Ask for the next fragment */
    name = ndn_charbuf_create();
    ndn_name_init(name);
    if (ic->n < 2) abort();
    res = ndn_name_append_components(name, ib, ic->buf[0], ic->buf[ic->n - 2]);
    if (res < 0) abort();
    ndn_name_append_numeric(name, NDN_MARKER_SEQNUM, ++(selfp->intdata));
    templ = make_template(md, info);
    
    res = ndn_express_interest(info->h, name, selfp, templ);
    if (res < 0) abort();
    
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&name);
    
    return(NDN_UPCALL_RESULT_OK);
}

/**
 * Process options and then loop through command line NDNx URIs retrieving
 * the data and writing it to stdout.
 */
int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_closure *incoming = NULL;
    const char *arg = NULL;
    int i;
    int res;
    int opt;
    struct mydata *mydata;
    int allow_stale = 0;
    int *done;
    int exit_status = 0;
    
    done = calloc(1, sizeof(*done));
    
    while ((opt = getopt(argc, argv, "ha")) != -1) {
        switch (opt) {
            case 'a':
                allow_stale = 1;
                break;
            case 'h':
            default:
                usage(argv[0]);
        }
    }
    arg = argv[optind];
    if (arg == NULL)
        usage(argv[0]);
    name = ndn_charbuf_create();
    /* Check the args first */
    for (i = optind; argv[i] != NULL; i++) {
        name->length = 0;
        res = ndn_name_from_uri(name, argv[i]);
        if (res < 0) {
            fprintf(stderr, "%s: bad ndn URI: %s\n", argv[0], argv[i]);
            exit(1);
        }
    }
    for (i = optind; (arg = argv[i]) != NULL; i++) {
        *done = 0;
        name->length = 0;
        res = ndn_name_from_uri(name, arg);
        ndn = ndn_create();
        if (ndn_connect(ndn, NULL) == -1) {
            perror("Could not connect to ndnd");
            exit(1);
        }
        ndn_resolve_version(ndn, name, NDN_V_HIGHEST, 50);
        ndn_name_append_numeric(name, NDN_MARKER_SEQNUM, 0);
        incoming = calloc(1, sizeof(*incoming));
        incoming->p = &incoming_content;
        mydata = calloc(1, sizeof(*mydata));
        mydata->allow_stale = allow_stale;
        mydata->done = done;
        incoming->data = mydata;
        templ = make_template(mydata, NULL);
        ndn_express_interest(ndn, name, incoming, templ);
        ndn_charbuf_destroy(&templ);
        /* Run a little while to see if there is anything there */
        res = ndn_run(ndn, 200);
        if ((!*done) && incoming->intdata == 0) {
            fprintf(stderr, "%s: not found: %s\n", argv[0], arg);
            res = -1;
        }
        /* We got something; run until end of data or somebody kills us */
        while (res >= 0 && !*done) {
            fflush(stdout);
            res = ndn_run(ndn, 333);
        }
        if (res < 0)
            exit_status = 1;
        ndn_destroy(&ndn);
        fflush(stdout);
        free(incoming);
        incoming = NULL;
    }
    ndn_charbuf_destroy(&name);
    free(done);
    exit(exit_status);
}
