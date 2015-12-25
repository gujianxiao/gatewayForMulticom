/**
 * @file ndncat.c
 * Reads streams at the given NDNx URIs and writes to stdout
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2012 Palo Alto Research Center, Inc.
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
#include <ndn/fetch.h>

/**
 * Provide usage hints for the program and then exit with a non-zero status.
 */
static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-h] [-d flags] [-p pipeline] [-s scope] [-a] ndn:/a/b ...\n"
            "  Reads streams at the given ndn URIs and writes to stdout\n"
            "  -h produces this message\n"
            "  -d flags specifies the fetch debug flags which are the sum of\n"
            "    NoteGlitch = 1,\n"
            "    NoteAddRem = 2,\n"
            "    NoteNeed = 4,\n"
            "    NoteFill = 8,\n"
            "    NoteFinal = 16,\n"
            "    NoteTimeout = 32,\n"
            "    NoteOpenClose = 64\n"
            "  -p pipeline specifies the size of the pipeline.  Default 4.\n"
            "     pipeline >= 0.\n"
            "  -s scope specifies the scope for the interests.  Default unlimited.\n"
            "     scope = 0 (cache), 1 (local), 2 (neighborhood), 3 (unlimited).\n"
            "  -a allow stale data\n",
            progname);
    exit(1);
}

struct ndn_charbuf *
make_template(int allow_stale, int scope)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    // XXX - use pubid if possible
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    if (allow_stale) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
        ndnb_append_number(templ, NDN_AOK_DEFAULT | NDN_AOK_STALE);
        ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
    }
    if (scope >= 0 && scope <= 2) {
        ndnb_tagged_putf(templ, NDN_DTAG_Scope, "%d", scope);
    }
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}


/**
 * Process options and then loop through command line NDNx URIs retrieving
 * the data and writing it to stdout.
 */
int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_fetch *fetch = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    const char *arg = NULL;
    int dflag = 0;
    int allow_stale = 0;
    int scope = -1;
    int pipeline = 4;
    unsigned char buf[8192];
    int i;
    int res;
    int opt;
    int assumeFixed = 0; // variable only for now
    
    while ((opt = getopt(argc, argv, "had:p:s:")) != -1) {
        switch (opt) {
            case 'a':
                allow_stale = 1;
                break;
            case 'd':
                dflag = atoi(optarg);
                break;
            case 'p':
                pipeline = atoi(optarg);
                if (pipeline < 0)
                    usage(argv[0]);
                break;
            case 's':
                scope = atoi(optarg);
                if (scope < 0 || scope > 3)
                    usage(argv[0]);
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
    
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    
    templ = make_template(allow_stale, scope);
    
    fetch = ndn_fetch_new(ndn);
    if (dflag) {
        ndn_fetch_set_debug(fetch, stderr, dflag);
    }
    
    for (i = optind; (arg = argv[i]) != NULL; i++) {
        name->length = 0;
        res = ndn_name_from_uri(name, argv[i]);
        struct ndn_fetch_stream *stream = ndn_fetch_open(fetch, name, arg, templ, pipeline, NDN_V_HIGHEST, assumeFixed);
        if (NULL == stream) {
            continue;
        }
        while ((res = ndn_fetch_read(stream, buf, sizeof(buf))) != 0) {
            if (res > 0) {
                fwrite(buf, res, 1, stdout);
            } else if (res == NDN_FETCH_READ_NONE) {
                fflush(stdout);
                if (ndn_run(ndn, 1000) < 0) {
                    fprintf(stderr, "%s: error during ndn_run\n", argv[0]);
                    exit(1);
                }
            } else if (res == NDN_FETCH_READ_END) {
                break;
            } else if (res == NDN_FETCH_READ_TIMEOUT) {
                /* eventually have a way to handle long timeout? */
                ndn_reset_timeout(stream);
                fflush(stdout);
                if (ndn_run(ndn, 1000) < 0) {
                    fprintf(stderr, "%s: error during ndn_run\n", argv[0]);
                    exit(1);
                }
            } else {
                /* fatal stream error; shuld report this! */
                fprintf(stderr, "%s: fetch error: %s\n", argv[0], arg);
                exit(1);
            }
        }
        stream = ndn_fetch_close(stream);
    }
    fflush(stdout);
    fetch = ndn_fetch_destroy(fetch);
    ndn_destroy(&ndn);
    ndn_charbuf_destroy(&name);
    exit(0);
}
