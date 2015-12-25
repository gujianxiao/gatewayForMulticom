/**
 * @file ndnrm.c
 * Mark as stale any local items a matching given prefixes.
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
#include <ndn/uri.h>

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-o outfile] ndn:/a/b ...\n"
            "   Remove (mark stale) content matching the given ndn URIs\n"
            "   -o outfile - write the ndnb-encoded content to the named file\n",
            progname);
    exit(1);
}

/***********
<Interest>
  <Name/>
  <AnswerOriginKind>19</AnswerOriginKind>
  <Scope>0</Scope>
</Interest>
**********/
struct ndn_charbuf *
local_scope_rm_template(void)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    ndnb_tagged_putf(templ, NDN_DTAG_AnswerOriginKind, "%2d",
                     (NDN_AOK_EXPIRE | NDN_AOK_DEFAULT));
    ndnb_tagged_putf(templ, NDN_DTAG_Scope, "0");
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

static
struct mydata {
    int nseen;
    FILE *output;
} mydata = {0, NULL};

enum ndn_upcall_res
incoming_content(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct mydata *md = selfp->data;

    if (kind == NDN_UPCALL_FINAL)
        return(NDN_UPCALL_RESULT_OK);
    if (md == NULL)
        return(NDN_UPCALL_RESULT_ERR);
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT)
        return(NDN_UPCALL_RESULT_REEXPRESS);
    if ((kind != NDN_UPCALL_CONTENT && kind != NDN_UPCALL_CONTENT_UNVERIFIED))
        return(NDN_UPCALL_RESULT_ERR);
    md->nseen++;
    if (md->output != NULL)
        fwrite(info->content_ndnb, info->pco->offset[NDN_PCO_E], 1, md->output);
    return(NDN_UPCALL_RESULT_REEXPRESS);
}

/* Use some static data for this simple program */
static struct ndn_closure incoming_content_action = {
    .p = &incoming_content,
    .data = &mydata
};

int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *c = NULL;
    struct ndn_charbuf *templ = NULL;
    int i;
    int res;
    int opt;
    FILE* closethis = NULL;
    
    while ((opt = getopt(argc, argv, "ho:")) != -1) {
        switch (opt) {
            case 'o':
                if (strcmp(optarg, "-") == 0)
                    mydata.output = stdout;
                else
                    mydata.output = closethis = fopen(optarg, "wb");
                if (mydata.output == NULL) {
                    perror(optarg);
                    exit(1);
                }
                break;
            case 'h': /* FALLTHRU */
            default: usage(argv[0]);
        }
    }
    
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
    c = ndn_charbuf_create();
    /* set scope to only address ndnd, expire anything we get */
    templ = local_scope_rm_template();
    for (i = optind; argv[i] != NULL; i++) {
        c->length = 0;
        res = ndn_name_from_uri(c, argv[i]);
        if (res < 0) {
            fprintf(stderr, "%s: bad ndn URI: %s\n", argv[0], argv[i]);
            exit(1);
        }
        ndn_express_interest(ndn, c, &incoming_content_action, templ);
    }
    if (i == optind)
        usage(argv[0]);
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&c);
    for (i = 0;; i++) {
        res = mydata.nseen;
        ndn_run(ndn, 100); /* stop if we run dry for 1/10 sec */
        if (res == mydata.nseen)
            break;
    }
    if (closethis != NULL)
        fclose(closethis);
    ndn_destroy(&ndn);
    fprintf(stderr, "marked stale: %d\n", mydata.nseen);
    exit(0);
}
