/**
 * @file ndnsendchunks.c
 * Injects chunks of data from stdin into ndn
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
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ndn/ndn.h>
#include <ndn/uri.h>
#include <ndn/keystore.h>
#include <ndn/signing.h>

struct mydata {
    int content_received;
    int content_sent;
    int outstanding;
};

enum ndn_upcall_res
incoming_content(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct mydata *md = selfp->data;

    if (kind == NDN_UPCALL_FINAL)
        return(NDN_UPCALL_RESULT_OK);
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT)
        return(NDN_UPCALL_RESULT_OK);
    if ((kind != NDN_UPCALL_CONTENT && kind != NDN_UPCALL_CONTENT_UNVERIFIED) || md == NULL)
        return(NDN_UPCALL_RESULT_ERR);
    md->content_received++;
    ndn_set_run_timeout(info->h, 0);
    return(NDN_UPCALL_RESULT_OK);
}

enum ndn_upcall_res
incoming_interest(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct mydata *md = selfp->data;

    if (kind == NDN_UPCALL_FINAL)
        return(NDN_UPCALL_RESULT_OK);
    if (kind != NDN_UPCALL_INTEREST || md == NULL)
        return(NDN_UPCALL_RESULT_ERR);
    if ((info->pi->answerfrom & NDN_AOK_NEW) != 0) {
        if (md->outstanding < 10)
            md->outstanding = 10;
        ndn_set_run_timeout(info->h, 0);
    }
    return(NDN_UPCALL_RESULT_OK);
}

ssize_t
read_full(int fd, unsigned char *buf, size_t size)
{
    size_t i;
    ssize_t res = 0;
    for (i = 0; i < size; i += res) {
        res = read(fd, buf + i, size - i);
        if (res == -1) {
            if (errno == EAGAIN || errno == EINTR)
                res = 0;
            else
                return(res);
        }
        else if (res == 0)
            break;
    }
    return(i);
}

static void
usage(const char *progname)
{
        fprintf(stderr,
                "%s [-h] [-x freshness_seconds] [-b blocksize] URI\n"
                " Chops stdin into blocks (1K by default) and sends them "
                "as consecutively numbered ContentObjects "
                "under the given uri\n", progname);
        exit(1);
}

int
main(int argc, char **argv)
{
    const char *progname = argv[0];
    struct ndn *ndn = NULL;
    struct ndn_charbuf *root = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *temp = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;
    long expire = -1;
    long blocksize = 1024;
    int i;
    int status = 0;
    int res;
    ssize_t read_res;
    unsigned char *buf = NULL;
    struct mydata mydata = { 0 };
    struct ndn_closure in_content = {.p=&incoming_content, .data=&mydata};
    struct ndn_closure in_interest = {.p=&incoming_interest, .data=&mydata};
    while ((res = getopt(argc, argv, "hx:b:")) != -1) {
        switch (res) {
            case 'x':
                expire = atol(optarg);
                if (expire <= 0)
                    usage(progname);
                break;
            case 'b':
                blocksize = atol(optarg);
                break;
            default:
            case 'h':
                usage(progname);
                break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc != 1)
        usage(progname);
    name = ndn_charbuf_create();
    res = ndn_name_from_uri(name, argv[0]);
    if (res < 0) {
        fprintf(stderr, "%s: bad NDN URI: %s\n", progname, argv[0]);
        exit(1);
    }
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    
    buf = calloc(1, blocksize);
    root = name;
    name = ndn_charbuf_create();
    temp = ndn_charbuf_create();
    templ = ndn_charbuf_create();

    /* Set up a handler for interests */
    ndn_charbuf_append(name, root->buf, root->length);
    ndn_set_interest_filter(ndn, name, &in_interest);
    
    /* Initiate check to see whether there is already something there. */
    ndn_charbuf_reset(temp);
    ndn_charbuf_putf(temp, "%d", 0);
    ndn_name_append(name, temp->buf, temp->length);
    ndn_charbuf_reset(templ);
    ndnb_element_begin(templ, NDN_DTAG_Interest);
    ndnb_element_begin(templ, NDN_DTAG_Name);
    ndnb_element_end(templ); /* </Name> */
    ndnb_tagged_putf(templ, NDN_DTAG_MaxSuffixComponents, "%d", 1);
    // XXX - use pubid
    ndnb_element_end(templ); /* </Interest> */
    res = ndn_express_interest(ndn, name, &in_content, templ);
    if (res < 0) abort();
    
    sp.freshness = expire;
    for (i = 0;; i++) {
        read_res = read_full(0, buf, blocksize);
        if (read_res < 0) {
            perror("read");
            read_res = 0;
            status = 1;
        }
        if (read_res < blocksize) {
            sp.sp_flags |= NDN_SP_FINAL_BLOCK;
        }
        ndn_charbuf_reset(name);
        ndn_charbuf_append(name, root->buf, root->length);
        ndn_charbuf_reset(temp);
        ndn_charbuf_putf(temp, "%d", i);
        ndn_name_append(name, temp->buf, temp->length);
        ndn_charbuf_reset(temp);
        ndn_charbuf_append(temp, buf, read_res);
        ndn_charbuf_reset(temp);
        res = ndn_sign_content(ndn, temp, name, &sp, buf, read_res);
        if (res != 0) {
            fprintf(stderr, "Failed to sign ContentObject (res == %d)\n", res);
            exit(1);
        }
        /* Put the keylocator in the first block only. */
        sp.sp_flags |= NDN_SP_OMIT_KEY_LOCATOR;
        if (i == 0) {
            /* Finish check for old content */
            if (mydata.content_received == 0)
                ndn_run(ndn, 100);
            if (mydata.content_received > 0) {
                fprintf(stderr, "%s: name is in use: %s\n", progname, argv[0]);
                exit(1);
            }
            mydata.outstanding++; /* the first one is free... */
        }
        res = ndn_put(ndn, temp->buf, temp->length);
        if (res < 0) {
            fprintf(stderr, "ndn_put failed (res == %d)\n", res);
            exit(1);
        }
        if (read_res < blocksize)
            break;
        if (mydata.outstanding > 0)
            mydata.outstanding--;
        else
            res = 10;
        res = ndn_run(ndn, res * 100);
        if (res < 0) {
            status = 1;
            break;
        }
    }
    
    free(buf);
    buf = NULL;
    ndn_charbuf_destroy(&root);
    ndn_charbuf_destroy(&name);
    ndn_charbuf_destroy(&temp);
    ndn_destroy(&ndn);
    exit(status);
}
