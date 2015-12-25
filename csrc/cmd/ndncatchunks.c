/**
 * @file ndncatchunks.c
 * Reads stuff written by ndnsendchunks, writes to stdout.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2010 Palo Alto Research Center, Inc.
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

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] ndn:/a/b\n"
            "   Reads stuff written by ndnsendchunks under"
            " the given uri and writes to stdout\n"
            "   -a - allow stale data\n",
            progname);
    exit(1);
}

struct mydata {
    int allow_stale;
};

struct ndn_charbuf *
make_template(struct mydata *md, struct ndn_upcall_info *info)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    // XXX - use pubid if possible
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    if (md->allow_stale) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
        ndnb_append_number(templ, NDN_AOK_DEFAULT | NDN_AOK_STALE);
        ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
    }
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

#define CHUNK_SIZE 1024

enum ndn_upcall_res
incoming_content(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_charbuf *temp = NULL;
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
    if (kind != NDN_UPCALL_CONTENT && kind != NDN_UPCALL_CONTENT_UNVERIFIED)
        return(NDN_UPCALL_RESULT_ERR);
    if (md == NULL)
        selfp->data = md = calloc(1, sizeof(*md));
    ndnb = info->content_ndnb;
    ndnb_size = info->pco->offset[NDN_PCO_E];
    ib = info->interest_ndnb;
    ic = info->interest_comps;
    /* XXX - must verify sig, and make sure it is LEAF content */
    res = ndn_content_get_value(ndnb, ndnb_size, info->pco, &data, &data_size);
    if (res < 0) abort();
    if (data_size > CHUNK_SIZE) {
        /* For us this is spam. Give up now. */
        fprintf(stderr, "*** Segment %d found with a data size of %d."
                        " This program only works with segments of 1024 bytes."
                        " Try ndncatchunks2 instead.\n",
                        (int)selfp->intdata, (int)data_size);
        exit(1);
    }
    
    /* OK, we will accept this block. */
    
    written = fwrite(data, data_size, 1, stdout);
    if (written != 1)
        exit(1);
    
    /* A short block signals EOF for us. */
    if (data_size < CHUNK_SIZE)
        exit(0);
    
    /* Ask for the next one */
    name = ndn_charbuf_create();
    ndn_name_init(name);
    if (ic->n < 2) abort();
    res = ndn_name_append_components(name, ib, ic->buf[0], ic->buf[ic->n - 2]);
    if (res < 0) abort();
    temp = ndn_charbuf_create();
    ndn_charbuf_putf(temp, "%d", ++(selfp->intdata));
    ndn_name_append(name, temp->buf, temp->length);
    ndn_charbuf_destroy(&temp);
    templ = make_template(md, info);
    
    res = ndn_express_interest(info->h, name, selfp, templ);
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
    ndn_name_append(name, "0", 1);
    incoming = calloc(1, sizeof(*incoming));
    incoming->p = &incoming_content;
    mydata = calloc(1, sizeof(*mydata));
    mydata->allow_stale = allow_stale;
    incoming->data = mydata;
    templ = make_template(mydata, NULL);
    ndn_express_interest(ndn, name, incoming, templ);
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&name);
    /* Run a little while to see if there is anything there */
    res = ndn_run(ndn, 200);
    if (incoming->intdata == 0) {
        fprintf(stderr, "%s: not found: %s\n", argv[0], arg);
        exit(1);
    }
    /* We got something, run until end of data or somebody kills us */
    while (res >= 0) {
        fflush(stdout);
        res = ndn_run(ndn, 200);
    }
    ndn_destroy(&ndn);
    exit(res < 0);
}
