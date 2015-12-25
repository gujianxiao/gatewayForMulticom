/**
 * @file ndnbasicconfig.c
 * Bring up a link to another ndnd.
 *
 * This is a very basic version that just registers a single prefix, possibly
 * creating a new face in the process.  Symbolic proto, host, and port are not
 * handled.  You might want to consider looking at ndndc instead.
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
#include <ndn/bloom.h>
#include <ndn/ndn.h>
#include <ndn/ndnd.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>
#include <ndn/face_mgmt.h>
#include <ndn/reg_mgmt.h>
#include <ndn/sockcreate.h>
#include <ndn/signing.h>
#include <ndn/keystore.h>

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s ndn:/prefix/to/register proto host [port]\n"
            "   Bring up a link to another ndnd, registering a prefix\n",
            progname);
    exit(1);
}

#define CHKRES(resval) chkres((resval), __LINE__)

static void
chkres(int res, int lineno)
{
    if (res >= 0)
        return;
    fprintf(stderr, "failure at "
                    "ndnbasicconfig.c"
                    ":%d (res = %d)\n", lineno, res);
    exit(1);
}

int
main(int argc, char **argv)
{
    struct ndn *h = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *null_name = NULL;
    struct ndn_charbuf *name_prefix = NULL;
    struct ndn_charbuf *newface = NULL;
    struct ndn_charbuf *prefixreg = NULL;
    struct ndn_charbuf *resultbuf = NULL;
    struct ndn_charbuf *temp = NULL;
    struct ndn_charbuf *templ = NULL;
    const unsigned char *ptr = NULL;
    size_t length = 0;
    const char *arg = NULL;
    const char *progname = NULL;
    struct ndn_parsed_ContentObject pcobuf = {0};
    struct ndn_face_instance face_instance_storage = {0};
    struct ndn_face_instance *face_instance = &face_instance_storage;
    struct ndn_forwarding_entry forwarding_entry_storage = {0};
    struct ndn_forwarding_entry *forwarding_entry = &forwarding_entry_storage;
    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;
    struct ndn_charbuf *keylocator_templ = NULL;
    struct ndn_keystore *keystore = NULL;
    long expire = -1;
    int ipproto;
    unsigned char ndndid_storage[32] = {0};
    const unsigned char *ndndid = NULL;
    size_t ndndid_size = 0;
    int res;
    int opt;
    
    progname = argv[0];
    while ((opt = getopt(argc, argv, "h")) != -1) {
        switch (opt) {
            case 'h':
            default:
                usage(progname);
        }
    }

    /* Sanity check the URI and argument count */
    arg = argv[optind];
    if (arg == NULL)
        usage(progname);
    name = ndn_charbuf_create();
    res = ndn_name_from_uri(name, arg);
    if (res < 0) {
        fprintf(stderr, "%s: bad ndn URI: %s\n", progname, arg);
        exit(1);
    }
    if (argc - optind < 3 || argc - optind > 4)
        usage(progname);
    
    h = ndn_create();
    res = ndn_connect(h, NULL);
    if (res < 0) {
        ndn_perror(h, "ndn_connect");
        exit(1);
    }

    newface = ndn_charbuf_create();
    temp = ndn_charbuf_create();
    templ = ndn_charbuf_create();
    keylocator_templ = ndn_charbuf_create();
    
    resultbuf = ndn_charbuf_create();
    name_prefix = ndn_charbuf_create();
    null_name = ndn_charbuf_create();
    CHKRES(ndn_name_init(null_name));

    keystore = ndn_keystore_create();
        
    /* We need to figure out our local ndnd's CCIDID */
    /* Set up our Interest template to indicate scope 1 */
    ndn_charbuf_reset(templ);
    ndnb_element_begin(templ, NDN_DTAG_Interest);
    ndnb_element_begin(templ, NDN_DTAG_Name);
    ndnb_element_end(templ);	/* </Name> */
    ndnb_tagged_putf(templ, NDN_DTAG_Scope, "1");
    ndnb_element_end(templ);	/* </Interest> */
    
    ndn_charbuf_reset(name);
    CHKRES(res = ndn_name_from_uri(name, "ndn:/%C1.M.S.localhost/%C1.M.SRV/ndnd/KEY"));
    CHKRES(res = ndn_get(h, name, templ, 200, resultbuf, &pcobuf, NULL, 0));
    res = ndn_ref_tagged_BLOB(NDN_DTAG_PublisherPublicKeyDigest,
                              resultbuf->buf,
                              pcobuf.offset[NDN_PCO_B_PublisherPublicKeyDigest],
                              pcobuf.offset[NDN_PCO_E_PublisherPublicKeyDigest],
                              &ndndid, &ndndid_size);
    CHKRES(res);
    if (ndndid_size > sizeof(ndndid_storage))
        CHKRES(-1);
    memcpy(ndndid_storage, ndndid, ndndid_size);
    ndndid = ndndid_storage;
    
    face_instance->action = "newface";
    face_instance->ndnd_id = ndndid;
    face_instance->ndnd_id_size = ndndid_size;
    if (strcmp(argv[optind + 1], "tcp") == 0)
        ipproto = 6;
    else if (strcmp(argv[optind + 1], "udp") == 0)
        ipproto = 17;
    else
        ipproto = atoi(argv[optind + 1]);
    face_instance->descr.ipproto = ipproto; // XXX - 6 = tcp or 17 = udp
    face_instance->descr.address = argv[optind + 2];
    face_instance->descr.port = argv[optind + 3];
    if (face_instance->descr.port == NULL)
        face_instance->descr.port = NDN_DEFAULT_UNICAST_PORT;
    face_instance->descr.mcast_ttl = -1;
    face_instance->lifetime = (~0U) >> 1;
    
    CHKRES(res = ndnb_append_face_instance(newface, face_instance));
    temp->length = 0;
    CHKRES(ndn_charbuf_putf(temp, "%s/.ndnx/.ndnx_keystore", getenv("HOME")));
    res = ndn_keystore_init(keystore,
                            ndn_charbuf_as_string(temp),
                            "Th1s1sn0t8g00dp8ssw0rd.");
    CHKRES(res);

    ndnb_element_begin(keylocator_templ, NDN_DTAG_SignedInfo);
    ndnb_element_begin(keylocator_templ, NDN_DTAG_KeyLocator);
    ndnb_element_begin(keylocator_templ, NDN_DTAG_Key);
    CHKRES(ndn_append_pubkey_blob(keylocator_templ, ndn_keystore_public_key(keystore)));
    ndnb_element_end(keylocator_templ);	/* </Key> */
    ndnb_element_end(keylocator_templ);	/* </KeyLocator> */
    ndnb_element_end(keylocator_templ);    /* </SignedInfo> */
    sp.template_ndnb = keylocator_templ;
    sp.sp_flags |= NDN_SP_TEMPL_KEY_LOCATOR;
    sp.freshness = expire;
    ndn_charbuf_reset(temp);
    res = ndn_sign_content(h, temp, null_name, &sp,
                           newface->buf, newface->length);
    CHKRES(res);
    
    /* Create the new face */
    CHKRES(ndn_name_init(name));
    CHKRES(ndn_name_append_str(name, "ndnx"));
    CHKRES(ndn_name_append(name, ndndid, ndndid_size));
    CHKRES(ndn_name_append(name, "newface", 7));
    CHKRES(ndn_name_append(name, temp->buf, temp->length));
    res = ndn_get(h, name, templ, 1000, resultbuf, &pcobuf, NULL, 0);
    if (res < 0) {
        fprintf(stderr, "no response from face creation request\n");
        exit(1);
    }
    ptr = resultbuf->buf;
    length = resultbuf->length;
    res = ndn_content_get_value(resultbuf->buf, resultbuf->length, &pcobuf, &ptr, &length);
    CHKRES(res);
    face_instance = ndn_face_instance_parse(ptr, length);
    if (face_instance == NULL)
        CHKRES(res = -1);
    CHKRES(face_instance->faceid);
    
    /* Finally, register the prefix */
    ndn_charbuf_reset(name_prefix);
    CHKRES(ndn_name_from_uri(name_prefix, arg));
    forwarding_entry->action = "prefixreg";
    forwarding_entry->name_prefix = name_prefix;
    forwarding_entry->ndnd_id = ndndid;
    forwarding_entry->ndnd_id_size = ndndid_size;
    forwarding_entry->faceid = face_instance->faceid;
    forwarding_entry->flags = -1; /* let ndnd decide */
    forwarding_entry->lifetime = (~0U) >> 1;
    prefixreg = ndn_charbuf_create();
    CHKRES(res = ndnb_append_forwarding_entry(prefixreg, forwarding_entry));
    ndn_charbuf_reset(temp);
    res = ndn_sign_content(h, temp, null_name, &sp,
                           prefixreg->buf, prefixreg->length);
    CHKRES(res);
    CHKRES(ndn_name_init(name));
    CHKRES(ndn_name_append_str(name, "ndnx"));
    CHKRES(ndn_name_append(name, ndndid, ndndid_size));
    CHKRES(ndn_name_append_str(name, "prefixreg"));
    CHKRES(ndn_name_append(name, temp->buf, temp->length));
    res = ndn_get(h, name, templ, 1000, resultbuf, &pcobuf, NULL, 0);
    if (res < 0) {
        fprintf(stderr, "no response from prefix registration request\n");
        exit(1);
    }
    fprintf(stderr, "Prefix %s will be forwarded to face %d\n", arg, face_instance->faceid);

    /* We're about to exit, so don't bother to free everything. */
    ndn_destroy(&h);
    exit(res < 0);
}
