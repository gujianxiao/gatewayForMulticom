/**
 * @file ndnseqwriter.c
 * Streams data from stdin into ndn
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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
#include <ndn/seqwriter.h>

static void
usage(const char *progname)
{
        fprintf(stderr,
                "%s [-h] [-b 0<blocksize<=4096] [-r] ndn:/some/uri\n"
                "    Reads stdin, sending data under the given URI"
                " using ndn versioning and segmentation.\n"
                "    -h generate this help message.\n"
                "    -b specify the block (segment) size for content objects.  Default 1024.\n"
                "    -r generate start-write interest so a repository will"
                " store the content.\n"
                "    -s n set scope of start-write interest.\n"
                "       n = 1(local), 2(neighborhood), 3(everywhere) Default 1.\n"
                "    -x specify the freshness for content objects.\n",
                progname);
        exit(1);
}
/*
 * make_template: construct an interest template containing the specified scope
 *     An unlimited scope is passed in as 3, and the omission of the scope
 *     field from the template indicates this.
 */
struct ndn_charbuf *
make_template(int scope)
{
    struct ndn_charbuf *templ = NULL;
    templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    if (0 <= scope && scope <= 2)
        ndnb_tagged_putf(templ, NDN_DTAG_Scope, "%d", scope);
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

int
main(int argc, char **argv)
{
    const char *progname = argv[0];
    struct ndn *ndn = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_seqwriter *w = NULL;
    int blocksize = 1024;
    int freshness = -1;
    int torepo = 0;
    int scope = 1;
    int i;
    int status = 0;
    int res;
    ssize_t read_res;
    size_t blockread;
    unsigned char *buf = NULL;
    struct ndn_charbuf *templ;
    
    while ((res = getopt(argc, argv, "hrb:s:x:")) != -1) {
        switch (res) {
            case 'b':
                blocksize = atoi(optarg);
                if (blocksize <= 0 || blocksize > 4096)
                    usage(progname);
                break;
            case 'r':
                torepo = 1;
                break;
            case 's':
                scope = atoi(optarg);
                if (scope < 1 || scope > 3)
                    usage(progname);
                break;
            case 'x':
                freshness = atoi(optarg);
                if (freshness < 0)
                    usage(progname);
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
    
    w = ndn_seqw_create(ndn, name);
    if (w == NULL) {
        fprintf(stderr, "ndn_seqw_create failed\n");
        exit(1);
    }
    ndn_seqw_set_block_limits(w, blocksize, blocksize);
    if (freshness > -1)
        ndn_seqw_set_freshness(w, freshness);
    if (torepo) {
        struct ndn_charbuf *name_v = ndn_charbuf_create();
        ndn_seqw_get_name(w, name_v);
        ndn_name_from_uri(name_v, "%C1.R.sw");
        ndn_name_append_nonce(name_v);
        templ = make_template(scope);
        res = ndn_get(ndn, name_v, templ, 60000, NULL, NULL, NULL, 0);
        ndn_charbuf_destroy(&templ);
        ndn_charbuf_destroy(&name_v);
        if (res < 0) {
            fprintf(stderr, "No response from repository\n");
            exit(1);
        }
    }
    blockread = 0;
    for (i = 0;; i++) {
        while (blockread < blocksize) {
            ndn_run(ndn, 1);
            read_res = read(0, buf + blockread, blocksize - blockread);
            if (read_res == 0)
                goto cleanup;
            if (read_res < 0) {
                perror("read");
                status = 1;
                goto cleanup;
            }
            blockread += read_res;
        }
        res = ndn_seqw_write(w, buf, blockread);
        while (res == -1) {
            ndn_run(ndn, 100);
            res = ndn_seqw_write(w, buf, blockread);
        }
        if (res != blockread)
            abort(); /* hmm, ndn_seqw_write did a short write or something */
        blockread = 0;
    }
    
cleanup:
    // flush out any remaining data and close
    if (blockread > 0) {
        res = ndn_seqw_write(w, buf, blockread);
        while (res == -1) {
            ndn_run(ndn, 100);
            res = ndn_seqw_write(w, buf, blockread);
        }
    }
    ndn_seqw_close(w);
    ndn_run(ndn, 1);
    free(buf);
    buf = NULL;
    ndn_charbuf_destroy(&name);
    ndn_destroy(&ndn);
    exit(status);
}
