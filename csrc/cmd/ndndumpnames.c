/**
 * @file ndndumpnames.c
 *
 * Dumps names of everything quickly retrievable to stdout
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

/**
 * This is a temporary interface, don't even bother to put it in a header file.
 */
void
ndn_dump_names(struct ndn *h, struct ndn_charbuf *name_prefix, int local_scope, int allow_stale);

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] [uri]\n"
            "   Dumps names of everything quickly retrievable\n"
            "   -a - allow stale data\n",
            progname);
    exit(1);
}

int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *c = NULL;
    int allow_stale = 0;
    int opt;
    int res;
    
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
    
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    c = ndn_charbuf_create();
    if (argv[optind] == NULL)
        ndn_name_init(c);
    else {
        res = ndn_name_from_uri(c, argv[optind]);
        if (res < 0) {
            fprintf(stderr, "%s: bad ndn URI: %s\n", argv[0], argv[optind]);
            exit(1);
        }
        if (argv[optind+1] != NULL)
            fprintf(stderr, "%s warning: extra arguments ignored\n", argv[0]);
    }
    ndn_dump_names(ndn, c, 1, allow_stale);
    exit(0);
}
