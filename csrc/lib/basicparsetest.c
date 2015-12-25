/**
 * @file basicparsetest.c
 * 
 * A NDNx test program.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009 Palo Alto Research Center, Inc.
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

#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/coding.h>
#include <ndn/face_mgmt.h>
#include <ndn/sockcreate.h>
#include <ndn/reg_mgmt.h>
#include <ndn/header.h>

/**
 * This is for testing.
 *
 * Reads ndnb-encoded data from stdin and 
 * tries parsing with various parsers, and when successful turns
 * the result back into ndnb and tests for goodness.
 *
 */
int
main (int argc, char **argv)
{
    unsigned char buf[8800];
    ssize_t size;
    struct ndn_face_instance *face_instance;
    struct ndn_forwarding_entry *forwarding_entry;
    struct ndn_header *header;
    int res = 1;
    struct ndn_charbuf *c = ndn_charbuf_create();
    int i;
    struct ndn_parsed_interest parsed_interest = {0};
    struct ndn_parsed_interest *pi = &parsed_interest;
    struct ndn_parsed_Link parsed_link = {0};
    struct ndn_parsed_Link *pl = &parsed_link;
    struct ndn_buf_decoder decoder;
    struct ndn_buf_decoder *d;


    size = read(0, buf, sizeof(buf));
    if (size < 0)
        exit(0);
    
    face_instance = ndn_face_instance_parse(buf, size);
    if (face_instance != NULL) {
        printf("face_instance OK\n");
        c->length = 0;
        res = ndnb_append_face_instance(c, face_instance);
        if (res != 0)
            printf("face_instance append failed\n");
        if (memcmp(buf, c->buf, c->length) != 0)
            printf("face_instance mismatch\n");
        ndn_face_instance_destroy(&face_instance);
        face_instance = ndn_face_instance_parse(c->buf, c->length);
        if (face_instance == NULL) {
            printf("face_instance reparse failed\n");
            res = 1;
        }
    }
    ndn_face_instance_destroy(&face_instance);
    
    forwarding_entry = ndn_forwarding_entry_parse(buf, size);
    if (forwarding_entry != NULL) {
        printf("forwarding_entry OK\n");
        c->length = 0;
        res = ndnb_append_forwarding_entry(c, forwarding_entry);
        if (res != 0)
            printf("forwarding_entry append failed\n");
        if (memcmp(buf, c->buf, c->length) != 0)
            printf("forwarding_entry mismatch\n");
        ndn_forwarding_entry_destroy(&forwarding_entry);
        forwarding_entry = ndn_forwarding_entry_parse(c->buf, c->length);
        if (forwarding_entry == NULL) {
            printf("forwarding_entry reparse failed\n");
            res = 1;
        }
    }
    ndn_forwarding_entry_destroy(&forwarding_entry);
    
    header = ndn_header_parse(buf, size);
    if (header != NULL) {
        printf("header OK\n");
        c->length = 0;
        res = ndnb_append_header(c, header);
        if (res != 0)
            printf("header append failed\n");
        if (memcmp(buf, c->buf, c->length) != 0)
            printf("header mismatch\n");
        ndn_header_destroy(&header);
        header = ndn_header_parse(c->buf, c->length);
        if (header == NULL) {
            printf("header reparse failed\n");
            res = 1;
        }
    }
    ndn_header_destroy(&header);

    i = ndn_parse_interest(buf, size, pi, NULL);
    if (i >= 0) {
        res = 0;
        printf("interest OK lifetime %jd (%d seconds)\n",
               ndn_interest_lifetime(buf, pi),
               ndn_interest_lifetime_seconds(buf, pi));
    }

    d = ndn_buf_decoder_start(&decoder, buf, size);
    i = ndn_parse_Link(d, pl, NULL);
    if (i >= 0) {
        res = 0;
        printf("link OK\n");
    }

    d = ndn_buf_decoder_start(&decoder, buf, size);
    i = ndn_parse_Collection_start(d);
    if (i >= 0) {
        while ((i = ndn_parse_Collection_next(d, pl, NULL)) > 0) {
	  printf("collection link OK\n");
        }
        if (i == 0) {
            res = 0;
            printf("collection OK\n");
        }
    }
    if (res != 0) {
        printf("URP\n");
    }
    exit(res);
}
