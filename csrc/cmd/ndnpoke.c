/**
 * @file ndnpoke.c
 * Injects one chunk of data from stdin into ndn.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2012 Palo Alto Research Center, Inc.
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
#include <unistd.h>
#include <ndn/ndn.h>
#include <ndn/uri.h>
#include <ndn/keystore.h>
#include <ndn/signing.h>

static ssize_t
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
            "%s [-hflv] [-k key_uri] [-t type] [-V seg] [-w timeout] [-x freshness_seconds]"
            " ndn:/some/place\n"
            " Reads data from stdin and sends it to the local ndnd"
            " as a single ContentObject under the given URI\n"
            "  -h - print this message and exit\n"
            "  -e file - extopt from supplied file\n"
            "  -f - force - send content even if no interest received\n"
            "  -l - set FinalBlockId from last segment of URI\n"
            "  -v - verbose\n"
            "  -k key_uri - use this name for key locator\n"
            "  -p n - limit registration to n (>=0) components of the given URI in the interest filter.\n"
            "  -t ( DATA | ENCR | GONE | KEY | LINK | NACK ) - set type\n"
            "  -V seg - generate version, use seg as name suffix\n"
            "  -w seconds - fail after this long if no interest arrives\n"
            "  -x seconds - set FreshnessSeconds\n"
            , progname);
    exit(1);
}

enum ndn_upcall_res
incoming_interest(
    struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    struct ndn_charbuf *cob = selfp->data;
    int res;
    
    switch (kind) {
        case NDN_UPCALL_FINAL:
            break;
        case NDN_UPCALL_INTEREST:
            if (ndn_content_matches_interest(cob->buf, cob->length,
                    1, NULL,
                    info->interest_ndnb, info->pi->offset[NDN_PI_E],
                    info->pi)) {
                res = ndn_put(info->h, cob->buf, cob->length);
                if (res >= 0) {
                    selfp->intdata = 1;
                    ndn_set_run_timeout(info->h, 0);
                    return(NDN_UPCALL_RESULT_INTEREST_CONSUMED);
                }
            }
            break;
        default:
            break;
    }
    return(NDN_UPCALL_RESULT_OK);
}

int
main(int argc, char **argv)
{
    const char *progname = argv[0];
    struct ndn *ndn = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *pname = NULL;
    struct ndn_charbuf *temp = NULL;
    struct ndn_charbuf *extopt = NULL;
    long expire = -1;
    int versioned = 0;
    size_t blocksize = 8*1024;
    int status = 0;
    int res;
    ssize_t read_res;
    unsigned char *buf = NULL;
    enum ndn_content_type content_type = NDN_CONTENT_DATA;
    struct ndn_closure in_interest = {.p=&incoming_interest};
    const char *postver = NULL;
    const char *key_uri = NULL;
    int force = 0;
    int verbose = 0;
    int timeout = -1;
    int setfinal = 0;
    int prefixcomps = -1;
    int fd;
    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;
    
    while ((res = getopt(argc, argv, "e:fhk:lvV:p:t:w:x:")) != -1) {
        switch (res) {
            case 'e':
                if (extopt == NULL)
                    extopt = ndn_charbuf_create();
                fd = open(optarg, O_RDONLY);
                if (fd < 0) {
                    perror(optarg);
                    exit(1);
                }
                for (;;) {
                    read_res = read(fd, ndn_charbuf_reserve(extopt, 64), 64);
                    if (read_res <= 0)
                        break;
                    extopt->length += read_res;
                }
                if (read_res < 0)
                    perror(optarg);
                close(fd);
                break;
            case 'f':
                force = 1;
                break;
            case 'l':
                setfinal = 1; // set FinalBlockID to last comp of name
                break;
            case 'k':
                key_uri = optarg;
                break;
            case 'p':
                prefixcomps = atoi(optarg);
                if (prefixcomps < 0)
                    usage(progname);
                break;
            case 'x':
                expire = atol(optarg);
                if (expire <= 0)
                    usage(progname);
                break;
            case 'v':
                verbose = 1;
                break;
            case 'V':
                versioned = 1;
                postver = optarg;
                if (0 == memcmp(postver, "%00", 3))
                    setfinal = 1;
                break;
            case 'w':
                timeout = atol(optarg);
                if (timeout <= 0)
                    usage(progname);
                timeout *= 1000;
                break;
            case 't':
                if (0 == strcasecmp(optarg, "DATA")) {
                    content_type = NDN_CONTENT_DATA;
                    break;
                }
                if (0 == strcasecmp(optarg, "ENCR")) {
                    content_type = NDN_CONTENT_ENCR;
                    break;
                }
                if (0 == strcasecmp(optarg, "GONE")) {
                    content_type = NDN_CONTENT_GONE;
                    break;
                }
                if (0 == strcasecmp(optarg, "KEY")) {
                    content_type = NDN_CONTENT_KEY;
                    break;
                }
                if (0 == strcasecmp(optarg, "LINK")) {
                    content_type = NDN_CONTENT_LINK;
                    break;
                }
                if (0 == strcasecmp(optarg, "NACK")) {
                    content_type = NDN_CONTENT_NACK;
                    break;
                }
                content_type = atoi(optarg);
                if (content_type > 0 && content_type <= 0xffffff)
                    break;
                fprintf(stderr, "Unknown content type %s\n", optarg);
                /* FALLTHRU */
            default:
            case 'h':
                usage(progname);
                break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argv[0] == NULL)
        usage(progname);
    name = ndn_charbuf_create();
    res = ndn_name_from_uri(name, argv[0]);
    if (res < 0) {
        fprintf(stderr, "%s: bad ndn URI: %s\n", progname, argv[0]);
        exit(1);
    }
    if (argv[1] != NULL)
        fprintf(stderr, "%s warning: extra arguments ignored\n", progname);
    
    /* Preserve the original prefix, in case we add versioning,
     * but trim it down if requested for the interest filter registration
     */
    pname = ndn_charbuf_create();
    ndn_charbuf_append(pname, name->buf, name->length);
    if (prefixcomps >= 0) {
        res = ndn_name_chop(pname, NULL, prefixcomps);
        if (res < 0) {
            fprintf(stderr, "%s: unable to trim name to %d component%s.\n",
                    progname, prefixcomps, prefixcomps == 1 ? "" : "s");
            exit(1);
        }
    }
    /* Connect to ndnd */
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }

    /* Read the actual user data from standard input */
    buf = calloc(1, blocksize);
    read_res = read_full(0, buf, blocksize);
    if (read_res < 0) {
        perror("read");
        read_res = 0;
        status = 1;
    }
        
    /* Tack on the version component if requested */
    if (versioned) {
        res = ndn_create_version(ndn, name, NDN_V_REPLACE | NDN_V_NOW | NDN_V_HIGH, 0, 0);
        if (res < 0) {
            fprintf(stderr, "%s: ndn_create_version() failed\n", progname);
            exit(1);
        }
        if (postver != NULL) {
            res = ndn_name_from_uri(name, postver);
            if (res < 0) {
                fprintf(stderr, "-V %s: invalid name suffix\n", postver);
                exit(0);
            }
        }
    }
    temp = ndn_charbuf_create();
    
    /* Ask for a FinalBlockID if appropriate. */
    if (setfinal)
        sp.sp_flags |= NDN_SP_FINAL_BLOCK;
    
    if (res < 0) {
        fprintf(stderr, "Failed to create signed_info (res == %d)\n", res);
        exit(1);
    }
    
    /* Set content type */
    sp.type = content_type;
    
    /* Set freshness */
    if (expire >= 0) {
        if (sp.template_ndnb == NULL) {
            sp.template_ndnb = ndn_charbuf_create();
            ndn_charbuf_append_tt(sp.template_ndnb, NDN_DTAG_SignedInfo, NDN_DTAG);
        }
        else if (sp.template_ndnb->length > 0) {
            sp.template_ndnb->length--;
        }
        ndnb_tagged_putf(sp.template_ndnb, NDN_DTAG_FreshnessSeconds, "%ld", expire);
        sp.sp_flags |= NDN_SP_TEMPL_FRESHNESS;
        ndn_charbuf_append_closer(sp.template_ndnb);
    }
    
    /* Set key locator, if supplied */
    if (key_uri != NULL) {
        struct ndn_charbuf *c = ndn_charbuf_create();
        res = ndn_name_from_uri(c, key_uri);
        if (res < 0) {
            fprintf(stderr, "%s is not a valid ndnx URI\n", key_uri);
            exit(1);
        }
        if (sp.template_ndnb == NULL) {
            sp.template_ndnb = ndn_charbuf_create();
            ndn_charbuf_append_tt(sp.template_ndnb, NDN_DTAG_SignedInfo, NDN_DTAG);
        }
        else if (sp.template_ndnb->length > 0) {
            sp.template_ndnb->length--;
        }
        ndn_charbuf_append_tt(sp.template_ndnb, NDN_DTAG_KeyLocator, NDN_DTAG);
        ndn_charbuf_append_tt(sp.template_ndnb, NDN_DTAG_KeyName, NDN_DTAG);
        ndn_charbuf_append(sp.template_ndnb, c->buf, c->length);
        ndn_charbuf_append_closer(sp.template_ndnb);
        ndn_charbuf_append_closer(sp.template_ndnb);
        sp.sp_flags |= NDN_SP_TEMPL_KEY_LOCATOR;
        ndn_charbuf_append_closer(sp.template_ndnb);
        ndn_charbuf_destroy(&c);
    }

    if (extopt != NULL && extopt->length > 0) {
        if (sp.template_ndnb == NULL) {
            sp.template_ndnb = ndn_charbuf_create();
            ndn_charbuf_append_tt(sp.template_ndnb, NDN_DTAG_SignedInfo, NDN_DTAG);
        }
        else if (sp.template_ndnb->length > 0) {
            sp.template_ndnb->length--;
        }
        ndnb_append_tagged_blob(sp.template_ndnb, NDN_DTAG_ExtOpt,
                                extopt->buf, extopt->length);
        sp.sp_flags |= NDN_SP_TEMPL_EXT_OPT;
        ndn_charbuf_append_closer(sp.template_ndnb);
    }
    
    /* Create the signed content object, ready to go */
    temp->length = 0;
    res = ndn_sign_content(ndn, temp, name, &sp, buf, read_res);
    if (res != 0) {
        fprintf(stderr, "Failed to encode ContentObject (res == %d)\n", res);
        exit(1);
    }
    if (read_res == blocksize) {
        read_res = read_full(0, buf, 1);
        if (read_res == 1) {
            fprintf(stderr, "%s: warning - truncated data\n", argv[0]);
            status = 1;
        }
    }
    free(buf);
    buf = NULL;
    if (force) {
        /* At user request, send without waiting to see an interest */
        res = ndn_put(ndn, temp->buf, temp->length);
        if (res < 0) {
            fprintf(stderr, "ndn_put failed (res == %d)\n", res);
            exit(1);
        }
    }
    else {
        in_interest.data = temp;
        /* Set up a handler for interests */
        res = ndn_set_interest_filter(ndn, pname, &in_interest);
        if (res < 0) {
            fprintf(stderr, "Failed to register interest (res == %d)\n", res);
            exit(1);
        }
        res = ndn_run(ndn, timeout);
        if (in_interest.intdata == 0) {
            if (verbose)
                fprintf(stderr, "Nobody's interested\n");
            exit(1);
        }
    }
    
    if (verbose) {
        struct ndn_charbuf *uri = ndn_charbuf_create();
        uri->length = 0;
        ndn_uri_append(uri, name->buf, name->length, 1);
        printf("wrote %s\n", ndn_charbuf_as_string(uri));
        ndn_charbuf_destroy(&uri);
    }
    ndn_destroy(&ndn);
    ndn_charbuf_destroy(&name);
    ndn_charbuf_destroy(&pname);
    ndn_charbuf_destroy(&temp);
    ndn_charbuf_destroy(&sp.template_ndnb);
    ndn_charbuf_destroy(&extopt);
    exit(status);
}
