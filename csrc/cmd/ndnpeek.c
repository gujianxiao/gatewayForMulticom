/**
 * @file ndnpeek.c
 * Get one content item matching the name prefix and write it to stdout.
 * Written as test for ndn_get, but probably useful for debugging.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2010 Palo Alto Research Center, Inc.
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
#include <errno.h>
#include <ndn/bloom.h>
#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] [-c] [-l lifetime] [-s scope] [-u] [-v] [-w timeout] ndn:/a/b\n"
            "   Get one content item matching the name prefix and write it to stdout"
            "\n"
            "   -a - allow stale data\n"
            "   -c - content only, not full ndnb\n"
            "   -l x - lifetime (seconds) of interest. 0.00012 < x <= 30.0000, Default 4.\n"
            "   -s {0,1,2} - scope of interest.  Default none.\n"
            "   -u - allow unverified content\n"
            "   -v - resolve version number\n"
            "   -w x - wait time (seconds) for response.  0.001 <= timeout <= 60.000, Default 3.0\n",
           progname);
    exit(1);
}

int
main(int argc, char **argv)
{
    struct ndn *h = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_charbuf *resultbuf = NULL;
    const char *arg = NULL;
    struct ndn_parsed_ContentObject pcobuf = { 0 };
    int res;
    int opt;
    int allow_stale = 0;
    int content_only = 0;
    int scope = -1;
    const unsigned char *ptr;
    size_t length;
    int resolve_version = 0;
    int timeout_ms = 3000;
    const unsigned lifetime_default = NDN_INTEREST_LIFETIME_SEC << 12;
    unsigned lifetime_l12 = lifetime_default;
    double lifetime;
    int get_flags = 0;
    
    while ((opt = getopt(argc, argv, "acl:s:uvw:h")) != -1) {
        switch (opt) {
            case 'a':
                allow_stale = 1;
                break;
            case 'c':
                content_only = 1;
                break;
            case 'l':
                errno = 0;
                lifetime = strtod(optarg, NULL);
                if (errno != 0) {
                    perror(optarg);
                    exit(1);
                }
                lifetime_l12 = 4096 * (lifetime + 1.0/8192.0);
                if (lifetime_l12 == 0 || lifetime_l12 > (30 << 12)) {
                    fprintf(stderr, "%.5f: invalid lifetime. %.5f < lifetime <= 30.0\n", lifetime, 1.0/8192.0);
                    exit(1);
                }
                break;
            case 's':
                scope = atoi(optarg);
                if (scope < 0 || scope > 2) {
                    fprintf(stderr, "%d: invalid scope.  0 <= scope <= 2\n", scope);
                    exit(1);
                }
            case 'u':
                get_flags |= NDN_GET_NOKEYWAIT;
                break;
            case 'v':
                if (resolve_version == 0)
                    resolve_version = NDN_V_HIGHEST;
                else
                    resolve_version = NDN_V_HIGH;
                break;
            case 'w':
                timeout_ms = strtod(optarg, NULL) * 1000;
                if (timeout_ms <= 0 || timeout_ms > 60000) {
                    fprintf(stderr, "%s: invalid timeout.  0.001 <= timeout <= 60.000\n", optarg);
                    exit(1);
                }
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
    h = ndn_create();
    res = ndn_connect(h, NULL);
    if (res < 0) {
        ndn_perror(h, "ndn_connect");
        exit(1);
    }
    if (res < 0) {
        fprintf(stderr, "%s: bad ndn URI: %s\n", argv[0], arg);
        exit(1);
    }
	if (allow_stale || lifetime_l12 != lifetime_default || scope != -1) {
        templ = ndn_charbuf_create();
        ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
        ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
        ndn_charbuf_append_closer(templ); /* </Name> */
		if (allow_stale) {
			ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
			ndnb_append_number(templ,
							   NDN_AOK_DEFAULT | NDN_AOK_STALE);
			ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
		}
        if (scope != -1) {
            ndnb_tagged_putf(templ, NDN_DTAG_Scope, "%d", scope);
        }
		if (lifetime_l12 != lifetime_default) {
			/*
			 * Choose the interest lifetime so there are at least 3
			 * expressions (in the unsatisfied case).
			 */
			unsigned char buf[3] = { 0 };
			int i;
			for (i = sizeof(buf) - 1; i >= 0; i--, lifetime_l12 >>= 8)
				buf[i] = lifetime_l12 & 0xff;
			ndnb_append_tagged_blob(templ, NDN_DTAG_InterestLifetime, buf, sizeof(buf));
		}
        ndn_charbuf_append_closer(templ); /* </Interest> */
    }
    resultbuf = ndn_charbuf_create();
    if (resolve_version != 0) {
        res = ndn_resolve_version(h, name, resolve_version, 500);
        if (res >= 0) {
            ndn_uri_append(resultbuf, name->buf, name->length, 1);
            fprintf(stderr, "== %s\n",
                            ndn_charbuf_as_string(resultbuf));
            resultbuf->length = 0;
        }
    }
    res = ndn_get(h, name, templ, timeout_ms, resultbuf, &pcobuf, NULL, get_flags);
    if (res >= 0) {
        ptr = resultbuf->buf;
        length = resultbuf->length;
        if (content_only)
            ndn_content_get_value(ptr, length, &pcobuf, &ptr, &length);
        if (length > 0)
            res = fwrite(ptr, length, 1, stdout) - 1;
    }
    ndn_charbuf_destroy(&resultbuf);
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&name);
    ndn_destroy(&h);
    exit(res < 0);
}
