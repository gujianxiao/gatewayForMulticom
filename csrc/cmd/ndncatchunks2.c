/**
 * @file ndncatchunks2.c
 * Reads stuff written by ndnsendchunks, writes to stdout.
 *
 * A NDNx command-line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2010, 2013 Palo Alto Research Center, Inc.
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
#include <sys/time.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/schedule.h>
#include <ndn/uri.h>

#define PIPELIMIT (1U << 7)
//#define GOT_HERE() fprintf(stderr, "LINE %d\n", __LINE__)
#define GOT_HERE() ((void)(__LINE__))

struct excludestuff;

struct ooodata {
    struct ndn_closure closure;     /* closure per slot */
    unsigned char *raw_data;        /* content that has arrived out-of-order */
    size_t raw_data_size;           /* its size (plus 1) in bytes */
};

struct mydata {
    struct ndn *h;
    int allow_stale;
    int use_decimal;
    int dummy;
    unsigned ooo_base;
    unsigned ooo_count;
    unsigned curwindow;
    unsigned maxwindow;
    unsigned sendtime;
    unsigned sendtime_slot;
    unsigned rtt;
    unsigned rtte;
    unsigned backoff;
    unsigned finalslot;
    struct ndn_charbuf *name;
    struct ndn_charbuf *templ;
    struct excludestuff *excl;
    struct ndn_schedule *sched;
    struct ndn_scheduled_event *report;
    struct ndn_scheduled_event *holefiller;
    intmax_t interests_sent;
    intmax_t pkts_recvd;
    intmax_t co_bytes_recvd;
    intmax_t delivered;
    intmax_t delivered_bytes;
    intmax_t junk;
    intmax_t holes;
    intmax_t timeouts;
    intmax_t dups;
    intmax_t lastcheck;
    intmax_t unverified;
    struct timeval start_tv;
    struct timeval stop_tv;
    struct ooodata ooo[PIPELIMIT];
};

static int fill_holes(struct ndn_schedule *sched, void *clienth, 
                      struct ndn_scheduled_event *ev, int flags);

static FILE* logstream = NULL;

static void
usage(const char *progname)
{
    fprintf(stderr,
            "%s [-a] [-p n] ndn:/a/b\n"
            "   Reads stuff written by ndnsendchunks under"
            " the given uri and writes to stdout\n"
            "   -a - allow stale data\n"
            "   -d - discard data instead of writing (also skips verification)\n"
            "   -p n - use up to n pipeline slots\n"
            "   -s - use new-style segmentation markers\n",
            progname);
    exit(1);
}

static void
mygettime(const struct ndn_gettime *self, struct ndn_timeval *result)
{
    struct timeval now = {0};
    gettimeofday(&now, 0);
    result->s = now.tv_sec;
    result->micros = now.tv_usec;
}

static struct ndn_gettime myticker = {
    "timer",
    &mygettime,
    1000000,
    NULL
};

static void
update_rtt(struct mydata *md, int incoming, unsigned slot)
{
    struct timeval now = {0};
    unsigned t, delta, rtte;
    
    if (!incoming && md->sendtime_slot == ~0)
        md->sendtime_slot = slot;
    if (slot != md->sendtime_slot)
        return;
    gettimeofday(&now, 0);
    t = ((unsigned)(now.tv_sec) * 1000000) + (unsigned)(now.tv_usec);
    if (incoming) {
        delta = t - md->sendtime;
        md->rtt = delta;
        if (delta <= 30000000) {
            rtte = md->rtte;
            if (delta > rtte)
                rtte = rtte + (rtte >> 3);
            else
                rtte = rtte - (rtte >> 7);
            if (rtte < 127)
                rtte = delta;
            md->rtte = rtte;
        }
        if (md->holefiller == NULL)
            md->holefiller = ndn_schedule_event(md->sched, 10000, &fill_holes, NULL, 0);
        md->sendtime_slot = ~0;
        if (logstream)
            fprintf(logstream,
                    "%ld.%06u ndncatchunks2: "
                    "%jd isent, %jd recvd, %jd junk, %jd holes, %jd t/o, %jd unvrf, "
                    "%u curwin, %u rtt, %u rtte\n",
                    (long)now.tv_sec,
                    (unsigned)now.tv_usec,
                    md->interests_sent,
                    md->pkts_recvd,
                    md->junk,
                    md->holes,
                    md->timeouts,
                    md->unverified,
                    md->curwindow,
                    md->rtt,
                    md->rtte
                    );
    }
    else
        md->sendtime = t;
}

static int
reporter(struct ndn_schedule *sched, void *clienth, 
         struct ndn_scheduled_event *ev, int flags)
{
    struct timeval now = {0};
    struct mydata *md = clienth;
    gettimeofday(&now, 0);
    fflush(stdout);
    fprintf(stderr,
            "%ld.%06u ndncatchunks2[%d]: "
            "%jd isent, %jd recvd, %jd junk, %jd holes, %jd t/o, %jd unvrf, "
            "%u curwin, %u rtt, %u rtte\n",
            (long)now.tv_sec,
            (unsigned)now.tv_usec,
            (int)getpid(),
            md->interests_sent,
            md->pkts_recvd,
            md->junk,
            md->holes,
            md->timeouts,
            md->unverified,
            md->curwindow,
            md->rtt,
            md->rtte
            );
    if ((flags & NDN_SCHEDULE_CANCEL) != 0) {
        md->report = NULL;
        return(0);
    }
    return(3000000);
}

void
print_summary(struct mydata *md)
{
    const char *expid;
    const char *dlm = " ";
    double elapsed = 0.0;
    intmax_t delivered_bytes;
    double rate = 0.0;
    
    expid = getenv("NDN_EXPERIMENT_ID");
    if (expid == NULL)
        expid = dlm = "";
    gettimeofday(&md->stop_tv, 0);
    elapsed = (double)(long)(md->stop_tv.tv_sec - md->start_tv.tv_sec);
    elapsed += ((int)md->stop_tv.tv_usec - (int)md->start_tv.tv_usec)/1000000.0;
    delivered_bytes = md->delivered_bytes;
    if (elapsed > 0.00001)
        rate = delivered_bytes/elapsed;
    fprintf(stderr,
            "%ld.%06u ndncatchunks2[%d]: %s%s"
            "%jd bytes transferred in %.6f seconds (%.0f bytes/sec)"
            "\n",
            (long)md->stop_tv.tv_sec,
            (unsigned)md->stop_tv.tv_usec,
            (int)getpid(),
            expid,
            dlm,
            delivered_bytes,
            elapsed,
            rate
            );
}

struct ndn_charbuf *
make_template(struct mydata *md)
{
    struct ndn_charbuf *templ = ndn_charbuf_create();
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    // XXX - use pubid if possible
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    if (md->allow_stale) {
        ndn_charbuf_append_tt(templ, NDN_DTAG_AnswerOriginKind, NDN_DTAG);
        ndnb_append_number(templ, NDN_AOK_DEFAULT | NDN_AOK_STALE);
        ndn_charbuf_append_closer(templ); /* </AnswerOriginKind> */
    }
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

static struct ndn_charbuf *
sequenced_name(struct mydata *md, uintmax_t seq)
{
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *temp = NULL;
    
    name = ndn_charbuf_create();
    ndn_charbuf_append(name, md->name->buf, md->name->length);
    if (md->use_decimal) {
        temp = ndn_charbuf_create();
        ndn_charbuf_putf(temp, "%ju", seq);
        ndn_name_append(name, temp->buf, temp->length);
        ndn_charbuf_destroy(&temp);
    }
    else
        ndn_name_append_numeric(name, NDN_MARKER_SEQNUM, seq);
    return(name);
}

static void
ask_more(struct mydata *md, uintmax_t seq)
{
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    int res;
    unsigned slot;
    struct ndn_closure *cl = NULL;
    
    slot = seq % PIPELIMIT;
    cl = &md->ooo[slot].closure;
    if (cl->intdata == -1)
        cl->intdata = seq;
    assert(cl->intdata == seq);
    assert(md->ooo[slot].raw_data_size == 0);
    name = sequenced_name(md, seq);
    templ = make_template(md);
    update_rtt(md, 0, slot);
    res = ndn_express_interest(md->h, name, cl, templ);
    if (res < 0) abort();
    md->interests_sent++;
    ndn_charbuf_destroy(&templ);
    ndn_charbuf_destroy(&name);
    if (seq == md->delivered + md->ooo_count)
        md->ooo_count++;
    assert(seq >= md->delivered);
    assert(seq < md->delivered + md->ooo_count);
    assert(md->ooo_count < PIPELIMIT);
}

static enum ndn_upcall_res
hole_filled(struct ndn_closure *selfp,
    enum ndn_upcall_kind kind,
    struct ndn_upcall_info *info)
{
    if (kind == NDN_UPCALL_FINAL)
        free(selfp);
    return(NDN_UPCALL_RESULT_OK);
}

static int
fill_holes(struct ndn_schedule *sched, void *clienth, 
         struct ndn_scheduled_event *ev, int flags)
{
    struct mydata *md = clienth;
    struct ndn_charbuf *name = NULL;
    struct ndn_charbuf *templ = NULL;
    struct ndn_closure *cl = NULL;
    unsigned backoff;
    int delay;
    
    if ((flags & NDN_SCHEDULE_CANCEL) != 0) {
        md->holefiller = NULL;
        return(0);
    }
    backoff = md->backoff;
    if (md->delivered == md->lastcheck && md->ooo_count > 0) {
        if (backoff == 0) {
            md->holes++;
            fprintf(stderr, "*** Hole at %jd\n", md->delivered);
            reporter(sched, md, NULL, 0);
            md->curwindow = 1;
            cl = calloc(1, sizeof(*cl));
            cl->p = &hole_filled;
            name = sequenced_name(md, md->delivered);
            templ = make_template(md);
            ndn_express_interest(md->h, name, cl, templ);
            md->interests_sent++;
            ndn_charbuf_destroy(&templ);
            ndn_charbuf_destroy(&name);
        }
        if ((6000000 >> backoff) > md->rtte)
            backoff++;
    }
    else {
        md->lastcheck = md->delivered;
        backoff = 0;
    }
    md->backoff = backoff;
    delay = (md->rtte << backoff);
    if (delay < 10000)
        delay = 10000;
    return(delay);
}

enum ndn_upcall_res
incoming_content(struct ndn_closure *selfp,
                 enum ndn_upcall_kind kind,
                 struct ndn_upcall_info *info)
{
    const unsigned char *ndnb = NULL;
    size_t ndnb_size = 0;
    const unsigned char *data = NULL;
    size_t data_size = 0;
    size_t written;
    int res;
    struct mydata *md = selfp->data;
    unsigned slot;
    
    if (kind == NDN_UPCALL_FINAL) {
        selfp->intdata = -1;
        return(NDN_UPCALL_RESULT_OK);
    }
GOT_HERE();
    if (kind == NDN_UPCALL_INTEREST_TIMED_OUT) {
        md->timeouts++;
        if (selfp->refcount > 1 || selfp->intdata == -1)
            return(NDN_UPCALL_RESULT_OK);
        md->interests_sent++;
        md->curwindow = 1;
        // XXX - may need to reseed bloom filter
        return(NDN_UPCALL_RESULT_REEXPRESS);
    }
GOT_HERE();
    assert(md != NULL);
    switch (kind) {
        case NDN_UPCALL_CONTENT:
             break;
        case NDN_UPCALL_CONTENT_UNVERIFIED:
             if (md->pkts_recvd == 0)
            return(NDN_UPCALL_RESULT_VERIFY);
            md->unverified++;
            break;
#if (NDN_API_VERSION >= 4004)
        case NDN_UPCALL_CONTENT_RAW:
            break;
        case NDN_UPCALL_CONTENT_KEYMISSING:
            if (md->pkts_recvd == 0)
            return(NDN_UPCALL_RESULT_FETCHKEY);
            md->unverified++;
            break;
#endif
        default:
            return(NDN_UPCALL_RESULT_ERR);
    }
    md->pkts_recvd++;
    if (selfp->intdata == -1) {
        /* Outside the window we care about. Toss it. */
        md->dups++;
        return(NDN_UPCALL_RESULT_OK);
    }
    ndnb = info->content_ndnb;
    ndnb_size = info->pco->offset[NDN_PCO_E];
    res = ndn_content_get_value(ndnb, ndnb_size, info->pco, &data, &data_size);
    if (res < 0) abort();
GOT_HERE();
    /* OK, we will accept this block. */
    md->co_bytes_recvd += data_size;
    slot = ((uintptr_t)selfp->intdata) % PIPELIMIT;
    assert(selfp == &md->ooo[slot].closure);
    if (ndn_is_final_block(info)) {
        GOT_HERE();
        md->finalslot = slot;
    }
    if (slot != md->ooo_base || md->ooo_count == 0) {
        /* out-of-order data, save for later */
        struct ooodata *ooo = &md->ooo[slot];
        if (ooo->raw_data_size == 0) {
GOT_HERE();
            update_rtt(md, 1, slot);
            ooo->raw_data = malloc(data_size);
            memcpy(ooo->raw_data, data, data_size);
            ooo->raw_data_size = data_size + 1;
        }
        else
            md->dups++;
        if (md->curwindow > 1)
            md->curwindow--;
    }
    else {
        assert(md->ooo[slot].raw_data_size == 0);
        update_rtt(md, 1, slot);
        md->ooo[slot].closure.intdata = -1;
        md->delivered++;
        md->delivered_bytes += data_size;
        written = md->dummy;
        if (! written)
            written = fwrite(data, data_size, 1, stdout);
        if (written != 1)
            exit(1);
        /* Check for EOF */
        if (slot == md->finalslot) {
            GOT_HERE();
            ndn_schedule_destroy(&md->sched);
            print_summary(md);
            exit(0);
        }
        md->ooo_count--;
        slot = (slot + 1) % PIPELIMIT;
        if (md->curwindow < md->maxwindow)
            md->curwindow++;
        while (md->ooo_count > 0 && md->ooo[slot].raw_data_size != 0) {
            struct ooodata *ooo = &md->ooo[slot];
            md->delivered++;
            md->delivered_bytes += (ooo->raw_data_size - 1);
            written = md->dummy;
            if (! written)
                written = fwrite(ooo->raw_data, ooo->raw_data_size - 1, 1, stdout);
            if (written != 1)
                exit(1);
            /* Check for EOF */
            if (slot == md->finalslot) {
                GOT_HERE();
                ndn_schedule_destroy(&md->sched);
                print_summary(md);
                exit(0);
            }
            free(ooo->raw_data);
            ooo->raw_data = NULL;
            ooo->raw_data_size = 0;
            slot = (slot + 1) % PIPELIMIT;
            md->ooo_count--;
        }
        md->ooo_base = slot;
    }
    
    /* Ask for the next one or two */
    if (md->ooo_count < md->curwindow)
        ask_more(md, md->delivered + md->ooo_count);
    if (md->ooo_count < md->curwindow)
        ask_more(md, md->delivered + md->ooo_count);
    
    return(NDN_UPCALL_RESULT_OK);
}

int
main(int argc, char **argv)
{
    struct ndn *ndn = NULL;
    struct ndn_charbuf *name = NULL;
    struct ndn_closure *incoming = NULL;
    const char *arg = NULL;
    int res;
    int micros;
    int opt;
    struct mydata *mydata;
    int allow_stale = 0;
    int use_decimal = 1;
    int i;
    unsigned maxwindow = PIPELIMIT-1;
    int dummy = 0;
    
    if (maxwindow > 31)
        maxwindow = 31;
    
    while ((opt = getopt(argc, argv, "hadp:s")) != -1) {
        switch (opt) {
            case 'a':
                allow_stale = 1;
                break;
            case 'd':
                dummy = 1;
                break;
            case 'p':
                res = atoi(optarg);
                if (1 <= res && res < PIPELIMIT)
                    maxwindow = res;
                else
                    usage(argv[0]);
                break;
            case 's':
                use_decimal = 0;
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
    ndn = ndn_create();
    if (ndn_connect(ndn, NULL) == -1) {
        perror("Could not connect to ndnd");
        exit(1);
    }
#if (NDN_API_VERSION >= 4004)
    if (dummy)
        ndn_defer_verification(ndn, 1);
#endif
    mydata = calloc(1, sizeof(*mydata));
    mydata->h = ndn;
    mydata->name = name;
    mydata->allow_stale = allow_stale;
    mydata->use_decimal = use_decimal;
    mydata->excl = NULL;
    mydata->sched = ndn_schedule_create(mydata, &myticker);
    mydata->report = ndn_schedule_event(mydata->sched, 0, &reporter, NULL, 0);
    mydata->holefiller = NULL;
    mydata->maxwindow = maxwindow;
    mydata->finalslot = ~0;
    mydata->dummy = dummy;
    for (i = 0; i < PIPELIMIT; i++) {
        incoming = &mydata->ooo[i].closure;
        incoming->p = &incoming_content;
        incoming->data = mydata;
        incoming->intdata = -1;
    }
    mydata->ooo_base = 0;
    mydata->ooo_count = 0;
    mydata->curwindow = 1;
    gettimeofday(&mydata->start_tv, 0);
    logstream = NULL;
    // logstream = fopen("xxxxxxxxxxxxxxlogstream" + (unsigned)getpid()%10, "wb"); 
    ask_more(mydata, 0);
    /* Run a little while to see if there is anything there */
    res = ndn_run(ndn, 500);
    if (mydata->delivered == 0) {
        fprintf(stderr, "%s: not found: %s\n", argv[0], arg);
        exit(1);
    }
    /* We got something, run until end of data or somebody kills us */
    while (res >= 0) {
        micros = ndn_schedule_run(mydata->sched);
        if (micros < 0)
            micros = 10000000;
        res = ndn_run(ndn, micros / 1000);
    }
    ndn_schedule_destroy(&mydata->sched);
    print_summary(mydata);
    ndn_destroy(&ndn);
    exit(res < 0);
}
