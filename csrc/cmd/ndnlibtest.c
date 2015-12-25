/**
 * @file cmd/ndnlibtest.c
 */
/*
 * A NDNx program.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009, 2010, 2012 Palo Alto Research Center, Inc.
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
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>

#include <ndn/ndn.h>
#include <ndn/reg_mgmt.h>
#include <ndn/uri.h>

int verbose;

void
printraw(const void *r, int n)
{
    int i, l;
    const unsigned char *p = r;
    
    if (verbose == 0)
        return;
    while (n > 0) {
        l = (n > 40 ? 40 : n);
        for (i = 0; i < l; i++)
            printf(" %c", (' ' <= p[i] && p[i] <= '~') ? p[i] : '.');
        printf("\n");
        for (i = 0; i < l; i++)
            printf("%02X", p[i]);
        printf("\n");
        p += l;
        n -= l;
    }
}
/* Use some static data for this simple program */
static unsigned char rawbuf[65536];
static ssize_t rawlen;

#define N_POOLS 10
#define MINI_STORE_LIMIT 10
struct mini_store {
    struct ndn_closure me;
    struct ndn_charbuf *cob[MINI_STORE_LIMIT];
};
static struct ndn_closure incoming_content_action[N_POOLS];
static struct mini_store store[N_POOLS];

int
add_to_pool(int pool, const unsigned char *r, size_t n)
{
    int i, j;
    struct ndn_charbuf **coba;
    
    coba = store[pool].cob;
    for (i = 0, j = 0; i < MINI_STORE_LIMIT; i++) {
        if (coba[i] != NULL)
            coba[j++] = coba[i];
    }
    for (i = j; i < MINI_STORE_LIMIT; i++)
        coba[i] = NULL;
    if (j < MINI_STORE_LIMIT) {
        coba[j] = ndn_charbuf_create();
        ndn_charbuf_append(coba[j], r, n);
        return(j + 1);
    }
    return(-1);
}

int
n_pool(int pool)
{
    int i, n;
    struct ndn_charbuf **coba;
    
    coba = store[pool].cob;
    for (i = 0, n = 0; i < MINI_STORE_LIMIT; i++)
        if (coba[i] != NULL)
            n++;
    return(n);
}

enum ndn_upcall_res
incoming_content(struct ndn_closure *selfp,
                 enum ndn_upcall_kind kind,
                 struct ndn_upcall_info *info)
{
    if (kind == NDN_UPCALL_FINAL)
        return(NDN_UPCALL_RESULT_OK);
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT)
        return(NDN_UPCALL_RESULT_REEXPRESS);
    if (kind != NDN_UPCALL_CONTENT && kind != NDN_UPCALL_CONTENT_UNVERIFIED)
        return(NDN_UPCALL_RESULT_ERR);
    printf("Got content matching %d components:\n", info->pi->prefix_comps);
    printraw(info->content_ndnb, info->pco->offset[NDN_PCO_E]);
    add_to_pool(selfp->intdata, info->content_ndnb, info->pco->offset[NDN_PCO_E]);
    return(NDN_UPCALL_RESULT_OK);
}

int
cob_matches(struct ndn_upcall_info *info, struct ndn_charbuf *cob)
{
    int ans;
    
    ans = ndn_content_matches_interest(cob->buf, cob->length, 1, NULL,
                                       info->interest_ndnb,
                                       info->pi->offset[NDN_PI_E],
                                       info->pi);
    return(ans);
}

enum ndn_upcall_res
outgoing_content(struct ndn_closure *selfp,
                 enum ndn_upcall_kind kind,
                 struct ndn_upcall_info *info)
{
    struct mini_store *md;
    struct ndn_charbuf *cob = NULL;
    int i;
    int res = 0;
    int which;
    
    md = selfp->data;
    which = md->me.intdata;
    if (kind == NDN_UPCALL_FINAL) {
        fprintf(stderr, "NDN_UPCALL_FINAL for store %d\n", which);
        for (i = 0; i < MINI_STORE_LIMIT; i++)
            ndn_charbuf_destroy(&md->cob[i]);
        return(NDN_UPCALL_RESULT_OK);
    }
    printf("Store %d got interest matching %d components, kind = %d",
           which, info->matched_comps, kind);
    /* Look through our little pile of content and send one that matches */
    if (kind == NDN_UPCALL_INTEREST) {
        for (i = 0; i < MINI_STORE_LIMIT; i++) {
            cob = md->cob[i];
            if (cob != NULL && cob_matches(info, cob)) {
                res = ndn_put(info->h, cob->buf, cob->length);
                if (res == -1) {
                    fprintf(stderr, "... error sending data\n");
                    return(NDN_UPCALL_RESULT_ERR);
                }
                else {
                    printf("... sent my content:\n");
                    printraw(cob->buf, cob->length);
                    ndn_charbuf_destroy(&md->cob[i]);
                    return(NDN_UPCALL_RESULT_INTEREST_CONSUMED);
                }
            }
        }
        printf("... no match\n");
    }
    else
        printf("\n");
    return(NDN_UPCALL_RESULT_ERR);
}

#define USAGE "ndnlibtest [-hv] (pool n | flags x | prefix uri | reconnect secs | run millis | file.ndnb) ..."

void
usage(void)
{
    fprintf(stderr, USAGE "\n");
    exit(1);
}

int
main(int argc, char **argv)
{
    char *arg = NULL;
    struct ndn *ndnH = NULL;
    struct ndn_parsed_interest interest = {0};
    struct ndn_charbuf *c = ndn_charbuf_create();
    struct ndn_charbuf *templ = ndn_charbuf_create();
    struct ndn_indexbuf *comps = ndn_indexbuf_create();
    int i;
    int millis = 0;
    int secs = 0;
    int opt;
    int pool = 0;
    int regflgs = (NDN_FORW_CHILD_INHERIT | NDN_FORW_ACTIVE);
    int res;
    int status = 0;
    int val = 0;
    
    while ((opt = getopt(argc, argv, "hv")) != -1) {
        switch (opt) {
            default:
            case 'h':
                usage();
                break;
            case 'v':
                verbose++;
                break;
        }
    }
    argc -= optind;
    argv += optind;
    ndnH = ndn_create();
    if (ndn_connect(ndnH, NULL) == -1) {
        ndn_perror(ndnH, "ndn_connect");
        exit(1);
    }
    for (i = 0; i < N_POOLS; i++) {
        store[i].me.p = &outgoing_content;
        store[i].me.data = &store[i];
        store[i].me.intdata = i;
        incoming_content_action[i].p = &incoming_content;
        incoming_content_action[i].intdata = i;
    }
    for (i = 0; i < argc; i++) {
        arg = argv[i];
        if (0 == strcmp(arg, "reconnect")) {
            if (argv[i+1] == NULL)
                usage();
            secs = atoi(argv[i+1]);
            if (secs <= 0 && strcmp(argv[i+1], "0") != 0)
                usage();
            i++;
            ndn_disconnect(ndnH);
            sleep(secs);
            if (ndn_connect(ndnH, NULL) == -1) {
                ndn_perror(ndnH, "ndn_connect");
                exit(1);
            }
            continue;
        }
        if (0 == strcmp(arg, "pool")) {
            if (argv[i+1] == NULL)
                usage();
            pool = argv[i+1][0] - '0';
            if (argv[i+1][1] || pool < 0 || pool >= N_POOLS)
                usage();
            fprintf(stderr, "Pool %d\n", pool);
            i++;
            continue;
        }
        if (0 == strcmp(arg, "prefix")) {
            if (argv[i+1] == NULL)
                usage();
            c->length = 0;
            res = ndn_name_from_uri(c, argv[i+1]);
            if (res < 0)
                usage();
            fprintf(stderr, "Prefix ff=%#x %s pool %d\n",
                    regflgs, argv[i+1], pool);
            if (store[pool].me.intdata != pool) {
                abort();
            }
            res = ndn_set_interest_filter_with_flags(ndnH, c, &store[pool].me, regflgs);
            if (res < 0) {
                ndn_perror(ndnH, "ndn_set_interest_filter_with_flags");
                status = 1;
            }
            res = ndn_run(ndnH, 2);
            if (res < 0)
                break;
            i++;
            continue;
        }
        if (0 == strcmp(arg, "flags")) {
            if (argv[i+1] == NULL)
                usage();
            regflgs = atoi(argv[i+1]);
            if (regflgs <= 0 && strcmp(argv[i+1], "0") != 0)
                usage();
            i++;
            continue;
        }
        if (0 == strcmp(arg, "mincob")) {
            if (argv[i+1] == NULL)
                usage();
            val = atoi(argv[i+1]);
            if (val <= 0 && strcmp(argv[i+1], "0") != 0)
                usage();
            i++;
            if (n_pool(pool) < val) {
                fprintf(stderr, "Pool %d has %d cobs, expected at least %d\n",
                        pool, n_pool(pool), val);
                exit(1);
            }
            continue;
        }
        if (0 == strcmp(arg, "maxcob")) {
            if (argv[i+1] == NULL)
                usage();
            val = atoi(argv[i+1]);
            if (val <= 0 && strcmp(argv[i+1], "0") != 0)
                usage();
            i++;
            if (n_pool(pool) > val) {
                fprintf(stderr, "Pool %d has %d cobs, expected at most %d\n",
                        pool, n_pool(pool), val);
                exit(1);
            }
            continue;
        }
        if (0 == strcmp(arg, "run")) {
            if (argv[i+1] == NULL)
                usage();
            millis = atoi(argv[i+1]);
            if (millis <= 0 && strcmp(argv[i+1], "0") != 0)
                usage();
            i++;
            res = ndn_run(ndnH, millis);
            if (res < 0) {
                ndn_perror(ndnH, "ndn_run");
                exit(1);
            }
            continue;
        }
        close(0);
        res = open(arg, O_RDONLY);
        if (res != 0) {
            perror(arg);
            exit(1);
        }
        fprintf(stderr, "Reading %s ... ", arg);
        rawlen = read(0, rawbuf, sizeof(rawbuf));
        if (rawlen < 0) {
            perror("skipping");
            // XXX - status
            continue;
        }
        // XXX - Should do a skeleton check before parse
        res = ndn_parse_interest(rawbuf, rawlen, &interest, NULL);
        if (res >= 0) {
            size_t name_start = interest.offset[NDN_PI_B_Name];
            size_t name_size = interest.offset[NDN_PI_E_Name] - name_start;
            templ->length = 0;
            ndn_charbuf_append(templ, rawbuf, rawlen);
            fprintf(stderr, "Expressing interest with %d name components\n", res);
            c->length = 0;
            ndn_charbuf_append(c, rawbuf + name_start, name_size);
            // XXX - res is currently ignored
            ndn_express_interest(ndnH, c, &(incoming_content_action[pool]), templ);
        }
        else {
            struct ndn_parsed_ContentObject obj = {0};
            int try;
            res = ndn_parse_ContentObject(rawbuf, rawlen, &obj, comps);
            if (res >= 0) {
                for (try = 0; try < 5; try++) {
                    res = add_to_pool(pool, rawbuf, rawlen);
                    if (res >= 0) {
                        fprintf(stderr, "Added to pool %d\n", pool);
                        break;
                    }
                    if (try == 5) {
                        fprintf(stderr, "No buffer for %s\n", arg);
                        status = 1;
                        break;
                    }
                    fprintf(stderr, "Pool %d full - wait for drain\n", pool);
                    if (ndn_run(ndnH, 1000) < 0)
                        break;
                }
                res = ndn_run(ndnH, 10);
            }
            else {
                fprintf(stderr, "What is that?\n");
                status = 1;
            }
        }
        res = ndn_run(ndnH, 10);
        if (res < 0) {
            ndn_perror(ndnH, "oops");
            status = 1;
        }
    }
    res = ndn_run(ndnH, 10);
    if (res < 0)
        status = 1;
    ndn_destroy(&ndnH);
    exit(status);
}
