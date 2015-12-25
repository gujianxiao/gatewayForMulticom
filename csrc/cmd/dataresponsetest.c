/**
 * @file dataresponsetest.c
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011 Palo Alto Research Center, Inc.
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
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>

#include <ndn/ndn.h>

static struct options {
    int logging;
    int nointerest;
    int reconnect;
} options = {0, 0, 0};

struct handlerstate {
    int next;
    int count;
    struct handlerstateitem {
        char *filename;
        unsigned char *contents;
        size_t	size;
        struct ndn_parsed_ContentObject x;
        struct ndn_indexbuf *components;
    } *items;
};

enum ndn_upcall_res interest_handler(struct ndn_closure *selfp,
                                     enum ndn_upcall_kind,
                                     struct ndn_upcall_info *info);

int
main (int argc, char *argv[]) {
    struct ndn *ndn = NULL;
    struct ndn_closure *action;
    struct ndn_charbuf *namebuf = NULL;
    struct ndn_charbuf *interestnamebuf = NULL;
    struct ndn_charbuf *interesttemplatebuf = NULL;
    struct ndn_buf_decoder decoder;
    struct ndn_buf_decoder *d;
    struct handlerstate *state;
    char *filename;
    char rawbuf[1024 * 1024];
    ssize_t rawlen;
    int i, n, res;
    int fd = -1;

    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("ndn_connect");
        exit(1);
    }
    
    state = calloc(1, sizeof(struct handlerstate));
    action = calloc(1, sizeof(struct ndn_closure));
    action->p = interest_handler;

    namebuf = ndn_charbuf_create();
    if (namebuf == NULL) {
        fprintf(stderr, "ndn_charbuf_create\n");
        exit(1);
    }
    res = ndn_name_init(namebuf);
    if (res < 0) {
        fprintf(stderr, "ndn_name_init\n");
        exit(1);
    }

    interestnamebuf = ndn_charbuf_create();
    interesttemplatebuf = ndn_charbuf_create();
    if (interestnamebuf == NULL || interesttemplatebuf == NULL) {
        fprintf(stderr, "ndn_charbuf_create\n");
        exit(1);
    }
    res = ndn_name_init(interestnamebuf);
    if (res < 0) {
        fprintf(stderr, "ndn_name_init\n");
        exit(1);
    }

    n = 0;
    for (i = 1; i < argc; i++) {
        if (fd != -1) close(fd);
        filename = argv[i];
        if (0 == strcmp(filename, "-d")) {
            options.logging++;
            continue;
        }
        if (0 == strcmp(filename, "-nointerest")) {
            options.nointerest = 1;
            continue;
        }
        if (0 == strcmp(filename, "-reconnect")) {
            options.reconnect = 1;
            continue;
        }
        
        if (options.logging > 0) fprintf(stderr, "Processing %s ", filename);
        fd = open(filename, O_RDONLY);
        if (fd == -1) {
            perror("- open");
            continue;
        }

        rawlen = read(fd, rawbuf, sizeof(rawbuf));
        if (rawlen <= 0) {
            perror("- read");
            continue;
        }
        
        d = ndn_buf_decoder_start(&decoder, (unsigned char *)rawbuf, rawlen);

        if (ndn_buf_match_dtag(d, NDN_DTAG_ContentObject)) {
            state->items = realloc(state->items, (n + 1) * sizeof(*(state->items)));
            if (state->items == NULL) {
                perror(" - realloc failed");
                exit(1);
            }
            memset(&(state->items[n]), 0, sizeof(*(state->items)));
            state->items[n].components = ndn_indexbuf_create();
            res = ndn_parse_ContentObject((unsigned char *)rawbuf, rawlen, &(state->items[n].x), state->items[n].components);
            if (res < 0) {
                if (options.logging > 0) fprintf(stderr, "Processing %s ", filename);
                fprintf(stderr, "- skipping: ContentObject error %d\n", res);
                ndn_indexbuf_destroy(&state->items[n].components);
                continue;
            }
            if (options.logging > 0) fprintf(stderr, "- ok\n");
            state->items[n].filename = filename;
            state->items[n].contents = malloc(rawlen);
            state->items[n].size = rawlen;
            memcpy(state->items[n].contents, rawbuf, rawlen);
            n++;
        } else if (ndn_buf_match_dtag(d, NDN_DTAG_Interest)) {
            struct ndn_parsed_interest interest = {0};
            if (options.nointerest == 0) {
                size_t name_start;
                size_t name_size;
                interestnamebuf->length = 0;
                interesttemplatebuf->length = 0;
                res = ndn_parse_interest((unsigned char *)rawbuf, rawlen, &interest, NULL);
                name_start = interest.offset[NDN_PI_B_Name];
                name_size = interest.offset[NDN_PI_E_Name] - name_start;
                ndn_charbuf_append(interestnamebuf, rawbuf + name_start, name_size);
                ndn_charbuf_append(interesttemplatebuf, rawbuf, rawlen);
                res = ndn_express_interest(ndn, interestnamebuf, action, interesttemplatebuf);
            }
        } else {
            if (options.logging == 0) fprintf(stderr, "Processing %s ", filename);
            fprintf(stderr, "- skipping: unknown type\n");
        }
    }
    state->count = n;
    action->data = state;

    if (ndn_name_init(namebuf) == -1) {
        fprintf(stderr, "ndn_name_init\n");
        exit(1);
    }

    res = ndn_set_interest_filter(ndn, namebuf, action);
    for (;;) {
        res = ndn_run(ndn, -1);
        ndn_disconnect(ndn);
        if (!options.reconnect)
            break;
        sleep(2);
        ndn_connect(ndn, NULL);
    }
    ndn_destroy(&ndn);
    exit(0);
}

int
match_components(unsigned char *msg1, struct ndn_indexbuf *comp1,
                 unsigned char *msg2, struct ndn_indexbuf *comp2) {
    int matched;
    int lc1, lc2;
    unsigned char *c1p, *c2p;

    for (matched = 0; (matched < comp1->n - 1) && (matched < comp2->n - 1); matched++) {
        lc1 = comp1->buf[matched + 1] - comp1->buf[matched];
        lc2 = comp2->buf[matched + 1] - comp2->buf[matched];
        if (lc1 != lc2) return (matched);

        c1p = msg1 + comp1->buf[matched];
        c2p = msg2 + comp2->buf[matched];
        if (memcmp(c1p, c2p, lc1) != 0) return (matched);
    }
    return (matched);
}

enum ndn_upcall_res
interest_handler(struct ndn_closure *selfp,
                 enum ndn_upcall_kind upcall_kind,
                 struct ndn_upcall_info *info)
{
    int i, c, mc, match, res;
    struct handlerstateitem item;
    struct handlerstate *state;
    size_t ndnb_size = 0;

    state = selfp->data;
    switch(upcall_kind) {
    case NDN_UPCALL_FINAL:
        fprintf(stderr, "Upcall final\n");
        return (0);

    case NDN_UPCALL_INTEREST_TIMED_OUT:
        fprintf(stderr, "refresh\n");
        return (NDN_UPCALL_RESULT_REEXPRESS);
        
    case NDN_UPCALL_CONTENT:
    case NDN_UPCALL_CONTENT_UNVERIFIED:
        ndnb_size = info->pco->offset[NDN_PCO_E];
        c = state->count;
        for (i = 0; i < c; i++) {
            if (info->content_comps->n == state->items[i].components->n) {
                mc = match_components((unsigned char *)info->content_ndnb, info->content_comps,
                                  state->items[i].contents, state->items[i].components);
                if (mc == (info->content_comps->n - 1)) {
                    fprintf(stderr, "Duplicate content\n");
                    return (0);
                }
            }
        }
        fprintf(stderr, "Storing content item %d ", c);
        state->items = realloc(state->items, (c + 1) * sizeof(*(state->items)));
        if (state->items == NULL) {
            perror("realloc failed");
            exit(1);
        }
        memset(&(state->items[c]), 0, sizeof(*(state->items)));
        state->items[c].components = ndn_indexbuf_create();
        /* XXX: probably should not have to do this re-parse of the content object */
        res = ndn_parse_ContentObject(info->content_ndnb, ndnb_size, &(state->items[c].x), state->items[c].components);
        if (res < 0) {
            fprintf(stderr, "- skipping: Not a ContentObject\n");
            ndn_indexbuf_destroy(&state->items[c].components);
            return (-1);
        }
        fprintf(stderr, "- ok\n");
        state->items[c].filename = "ephemeral";
        state->items[c].contents = malloc(ndnb_size);
        state->items[c].size = ndnb_size;
        memcpy(state->items[c].contents, info->content_ndnb, ndnb_size);
        state->count = c + 1;
        return (0);

    case NDN_UPCALL_CONTENT_BAD:
	fprintf(stderr, "Content signature verification failed! Discarding.\n");
	return (-1);

    case NDN_UPCALL_CONSUMED_INTEREST:
        fprintf(stderr, "Upcall consumed interest\n");
        return (-1); /* no data */

    case NDN_UPCALL_INTEREST:
        c = state->count;
        for (i = 0; i < c; i++) {
            match = ndn_content_matches_interest(state->items[i].contents,
                                                 state->items[i].size,
                                                 1,
                                                 NULL,
                                                 info->interest_ndnb,
                                                 info->pi->offset[NDN_PI_E],
                                                 info->pi);
            if (match) {
                ndn_put(info->h, state->items[i].contents, state->items[i].size);
                fprintf(stderr, "Sending %s\n", state->items[i].filename);
                if (i < c - 1) {
                    item = state->items[i];
                    memmove(&(state->items[i]), &(state->items[i+1]), sizeof(item) * ((c - 1) - i));
                    state->items[c - 1] = item;
                }
                return (1);
            }
        }
        return(0);
    case NDN_UPCALL_CONTENT_KEYMISSING:
    case NDN_UPCALL_CONTENT_RAW:
        /* should not happen */
        return (-1);
    }
    return (-1);
}
