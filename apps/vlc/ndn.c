/**
 * @file apps/vlc/ndn.c
 * 
 * NDNx input module for vlc.
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

/*****************************************************************************
 * Preamble
 *****************************************************************************/

#ifdef HAVE_CONFIG_H
# include "config.h"
#endif

#include <limits.h>
#include <poll.h>
#include <errno.h>

#include <vlc_common.h>
#include <vlc_plugin.h>
#include <vlc_access.h>
#include <vlc_url.h>
#include <vlc_threads.h>

#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/uri.h>
#include <ndn/header.h>

/*****************************************************************************
 * Disable internationalization
 *****************************************************************************/
#define _(str) (str)
#define N_(str) (str)
/*****************************************************************************
 * Module descriptor
 *****************************************************************************/
#define NDN_VERSION_TIMEOUT 5000
#define NDN_HEADER_TIMEOUT 1000
#define NDN_DEFAULT_PREFETCH 12

#define NDN_PREFETCH_LIFETIME 1023
#define NDN_DATA_LIFETIME 1024
#define PREFETCH_TEXT N_("Prefetch offset")
#define PREFETCH_LONGTEXT N_(                                          \
"Number of content objects prefetched, "                       \
"and offset from content object received for next interest.")
#define SEEKABLE_TEXT N_("NDN streams can seek")
#define SEEKABLE_LONGTEXT N_(               \
"Enable or disable seeking within a NDN stream.")
#define VERSION_TIMEOUT_TEXT N_("Version timeout (ms)")
#define VERSION_TIMEOUT_LONGTEXT N_(                                          \
"Maximum number of milliseconds to wait for resolving latest version of media.")
#define HEADER_TIMEOUT_TEXT N_("Header timeout (ms)")
#define HEADER_TIMEOUT_LONGTEXT N_(                                          \
"Maximum number of milliseconds to wait for resolving latest version of header.")
#define TCP_CONNECT_TEXT N_("Connect to ndnd with TCP")
#define TCP_CONNECT_LONGTEXT N_(                                        \
"Connect to ndnd with TCP instead of Unix domain socket")


static int  NDNOpen(vlc_object_t *);
static void NDNClose(vlc_object_t *);
static block_t *NDNBlock(access_t *);
#if (VLCPLUGINVER >= 10100)
static int NDNSeek(access_t *, uint64_t);
#else
static int NDNSeek(access_t *, int64_t);
#endif
static int NDNControl(access_t *, int, va_list);

vlc_module_begin();
set_shortname(N_("NDNx"));
set_description(N_("Access streams via NDNx"));
set_category(CAT_INPUT);
set_subcategory(SUBCAT_INPUT_ACCESS);
#if (VLCPLUGINVER < 10200)
add_integer("ndn-prefetch", NDN_DEFAULT_PREFETCH, NULL,
            PREFETCH_TEXT, PREFETCH_LONGTEXT, true);
add_bool("ndn-streams-seekable", true, NULL,
         SEEKABLE_TEXT, SEEKABLE_LONGTEXT, true )
#else
add_integer("ndn-prefetch", NDN_DEFAULT_PREFETCH,
            PREFETCH_TEXT, PREFETCH_LONGTEXT, true);
add_integer("ndn-version-timeout", NDN_VERSION_TIMEOUT,
            VERSION_TIMEOUT_TEXT, VERSION_TIMEOUT_LONGTEXT, true);
add_integer("ndn-header-timeout", NDN_HEADER_TIMEOUT,
            HEADER_TIMEOUT_TEXT, HEADER_TIMEOUT_LONGTEXT, true);
add_bool("ndn-streams-seekable", true,
         SEEKABLE_TEXT, SEEKABLE_LONGTEXT, true )
add_bool("ndn-tcp-connect", true,
         TCP_CONNECT_TEXT, TCP_CONNECT_LONGTEXT, true )
#endif
change_safe();
set_capability("access", 0);
add_shortcut("ndn");
add_shortcut("ndnx");
set_callbacks(NDNOpen, NDNClose);
vlc_module_end();

/*****************************************************************************
 * Local prototypes
 *****************************************************************************/
struct access_sys_t
{
    int i_chunksize;        /**< size of NDN ContentObject data blocks */
    int i_prefetch;         /**< offset for prefetching */
    int i_version_timeout;  /**< timeout in seconds for getting latest media version */
    int i_header_timeout;   /**< timeout in seconds for getting latest header version */
    int i_missed_co;        /**< number of content objects we missed in NDNBlock */
    struct ndn *ndn;        /**< NDN handle */
    struct ndn *ndn_pf;     /**< NDN handle for prefetch thread */
    struct ndn_closure *prefetch;   /**< closure for handling prefetch content */
    struct ndn_charbuf *p_name;     /**< base name for stream including version */
    struct ndn_charbuf *p_prefetch_template; /**< interest expression template */
    struct ndn_charbuf *p_data_template; /**< interest expression template */
    struct ndn_charbuf *p_content_object; /**< content object storage */
    struct ndn_indexbuf *p_compsbuf; /**< name components indexbuf scratch storage */
    vlc_thread_t thread;    /**< thread that is running prefetch ndn_run loop */
    vlc_mutex_t lock;       /**< mutex protecting ndn_pf handle */
};

static enum ndn_upcall_res discard_content(struct ndn_closure *selfp,
                                           enum ndn_upcall_kind kind,
                                           struct ndn_upcall_info *info);
static void *ndn_prefetch_thread(void *p_this);
static void sequenced_name(struct ndn_charbuf *name,
                           struct ndn_charbuf *basename, uintmax_t seq);
static struct ndn_charbuf *make_prefetch_template();
static struct ndn_charbuf *make_data_template();

/*****************************************************************************
 * p_sys_clean: 
 *****************************************************************************/

static void p_sys_clean(struct access_sys_t *p_sys) {
    ndn_destroy(&(p_sys->ndn));
    ndn_destroy(&(p_sys->ndn_pf));
    if (p_sys->prefetch) {
        free(p_sys->prefetch);
        p_sys->prefetch = NULL;
    }
    ndn_charbuf_destroy(&p_sys->p_name);
    ndn_charbuf_destroy(&p_sys->p_prefetch_template);
    ndn_charbuf_destroy(&p_sys->p_data_template);
    ndn_charbuf_destroy(&p_sys->p_content_object);
    ndn_indexbuf_destroy(&p_sys->p_compsbuf);
    vlc_mutex_destroy(&p_sys->lock);
}

/*****************************************************************************
 * NDNOpen: 
 *****************************************************************************/
#define CHECK_NOMEM(x, msg) if ((x) == NULL) {\
i_err = VLC_ENOMEM; msg_Err(p_access, msg); goto exit; }

static int
NDNOpen(vlc_object_t *p_this)
{
    access_t     *p_access = (access_t *)p_this;
    access_sys_t *p_sys = NULL;
    int i_ret = 0;
    int i_err = VLC_EGENERIC;
    int i;
    struct ndn_charbuf *p_name = NULL;
    struct ndn_header *p_header = NULL;
    bool b_tcp;
    
    /* Init p_access */
    access_InitFields(p_access);
    msg_Info(p_access, "NDNOpen called");
    ACCESS_SET_CALLBACKS(NULL, NDNBlock, NDNControl, NDNSeek);
    p_sys = calloc(1, sizeof(access_sys_t));
    if (p_sys == NULL) {
        msg_Err(p_access, "NDNOpen failed: no memory for p_sys");
        return (VLC_ENOMEM);
    }
    p_access->p_sys = p_sys;
    p_sys->i_chunksize = -1;
    p_sys->i_missed_co = 0;
    p_sys->i_prefetch = var_CreateGetInteger(p_access, "ndn-prefetch");
    p_sys->i_version_timeout = var_CreateGetInteger(p_access, "ndn-version-timeout");
    p_sys->i_header_timeout = var_CreateGetInteger(p_access, "ndn-header-timeout");
    b_tcp = var_CreateGetBool(p_access, "ndn-tcp-connect");
    p_access->info.i_size = LLONG_MAX;	/* don't know yet, but bigger is better */
    
    p_sys->prefetch = calloc(1, sizeof(struct ndn_closure));
    CHECK_NOMEM(p_sys->prefetch, "NDNOpen failed: no memory for prefetch ndn_closure");
    p_sys->p_prefetch_template = make_prefetch_template();
    CHECK_NOMEM(p_sys->p_prefetch_template, "NDNOpen failed: no memory for prefetch template");
    p_sys->p_data_template = make_data_template();
    CHECK_NOMEM(p_sys->p_data_template, "NDNOpen failed: no memory for data template");
    
#if (VLCPLUGINVER >= 10200)
    msg_Dbg(p_access, "NDNOpen %s", p_access->psz_location);
#else
    msg_Dbg(p_access, "NDNOpen %s", p_access->psz_path);
#endif
    vlc_mutex_init(&p_sys->lock);
    p_sys->prefetch->data = p_access; /* so NDN callbacks can find p_sys */
    p_sys->prefetch->p = &discard_content; /* the NDN callback */
    
    p_sys->ndn = ndn_create();
    if (p_sys->ndn == NULL || ndn_connect(p_sys->ndn, b_tcp ? "tcp" : NULL) == -1) {
        msg_Err(p_access, "NDNOpen failed: unable to allocate handle and connect to ndnd");
        goto exit;
    }
    p_sys->ndn_pf = ndn_create();
    if (p_sys->ndn_pf == NULL || ndn_connect(p_sys->ndn_pf, b_tcp ? "tcp" : NULL) == -1) {
        msg_Err(p_access, "NDNOpen failed: unable to allocate prefetch handle and connect to ndnd");
        goto exit;
    }
    msg_Info(p_access, "NDNOpen connected to ndnd%s", b_tcp ? " with TCP" : "");
    
    p_name = ndn_charbuf_create();
    CHECK_NOMEM(p_name, "NDNOpen failed: no memory for name charbuf");
    p_sys->p_compsbuf = ndn_indexbuf_create();
    CHECK_NOMEM(p_sys->p_compsbuf, "NDNOpen failed: no memory for name components indexbuf");
    
#if (VLCPLUGINVER >= 10200)
    i_ret = ndn_name_from_uri(p_name, p_access->psz_location);
#else
    i_ret = ndn_name_from_uri(p_name, p_access->psz_path);
#endif
    if (i_ret < 0) {
        msg_Err(p_access, "NDNOpen failed: unable to parse NDN URI");
        goto exit;
    }
    p_sys->p_name = ndn_charbuf_create_n(p_name->length + 16);
    CHECK_NOMEM(p_sys->p_name, "NDNOpen failed: no memory for global name charbuf");
    i_ret = ndn_resolve_version(p_sys->ndn, p_name,
                                NDN_V_HIGHEST, p_sys->i_version_timeout);
    if (i_ret < 0) {
        msg_Err(p_access, "NDNOpen failed: unable to determine version");
        goto exit;
    }
    ndn_charbuf_append_charbuf(p_sys->p_name, p_name);
    /* name is versioned, so get the header to obtain the length */
    p_header = ndn_get_header(p_sys->ndn, p_name, p_sys->i_header_timeout);
    if (p_header != NULL) {
        p_access->info.i_size = p_header->length;
        p_sys->i_chunksize = p_header->block_size;
        ndn_header_destroy(&p_header);
    }
    msg_Dbg(p_access, "NDNOpen set length %"PRId64, p_access->info.i_size);
    ndn_charbuf_destroy(&p_name);
    
    p_sys->p_content_object = ndn_charbuf_create();
    CHECK_NOMEM(p_sys->p_content_object, "NDNOpen failed: no memory for initial content");
    /* make sure we can get the first block, or fail early */
    p_name = ndn_charbuf_create();
    sequenced_name(p_name, p_sys->p_name, 0);
    i_ret = ndn_get(p_sys->ndn, p_name, p_sys->p_data_template, 5000, p_sys->p_content_object, NULL, NULL, 0);
    if (i_ret < 0) {
        ndn_charbuf_destroy(&p_name);
        msg_Err(p_access, "NDNOpen failed: unable to locate specified input");
        goto exit;
    }
    if (0 != vlc_clone(&(p_sys->thread), ndn_prefetch_thread, p_access, VLC_THREAD_PRIORITY_INPUT)) {
        msg_Err(p_access, "NDNOpen failed: unable to vlc_clone for NDN prefetch thread");
        goto exit;
    }
    /* start prefetches for some more, unless it's a short file */
    vlc_mutex_lock(&p_sys->lock);
    for (i=1; i <= p_sys->i_prefetch; i++) {
        if (i * p_sys->i_chunksize >= p_access->info.i_size)
            break;
        sequenced_name(p_name, p_sys->p_name, i);
        i_ret = ndn_express_interest(p_sys->ndn_pf, p_name, p_sys->prefetch,
                                     p_sys->p_prefetch_template);
    }
    vlc_mutex_unlock(&p_sys->lock);
    ndn_charbuf_destroy(&p_name);
    return (VLC_SUCCESS);
    
exit:
    ndn_charbuf_destroy(&p_name);
    p_sys_clean(p_sys);
    free(p_sys);
    p_access->p_sys = NULL;
    return (i_err);
}

/*****************************************************************************
 * NDNClose: free unused data structures
 *****************************************************************************/
static void
NDNClose(vlc_object_t *p_this)
{
    access_t     *p_access = (access_t *)p_this;
    access_sys_t *p_sys = p_access->p_sys;
    
    msg_Info(p_access, "NDNClose called, missed %d blocks", p_sys->i_missed_co);
    ndn_run(p_sys->ndn, 100);
    ndn_disconnect(p_sys->ndn);
    vlc_mutex_lock(&p_sys->lock);
    ndn_disconnect(p_sys->ndn_pf);
    vlc_mutex_unlock(&p_sys->lock);
    msg_Info(p_access, "NDNClose about to join prefetch thread");
    vlc_join(p_sys->thread, NULL);
    msg_Info(p_access, "NDNClose joined prefetch thread");
    p_sys_clean(p_sys);
    free(p_sys);
}

/*****************************************************************************
 * NDNBlock:
 *****************************************************************************/
static block_t *
NDNBlock(access_t *p_access)
{
    access_sys_t *p_sys = p_access->p_sys;
    block_t *p_block = NULL;
    struct ndn_charbuf *p_name = NULL;
    struct ndn_parsed_ContentObject pcobuf = {0};
    const unsigned char *data = NULL;
    size_t data_size = 0;
    uint64_t start_offset = 0;
    uint64_t i_nextpos;
    int i_ret;
    bool b_last = false;
    
    if (p_access->info.b_eof) {
        msg_Dbg(p_access, "NDNBlock eof");
        return NULL;
    }
    // start
    p_name = ndn_charbuf_create();
    sequenced_name(p_name, p_sys->p_name, p_access->info.i_pos / p_sys->i_chunksize);
    i_ret = ndn_get(p_sys->ndn, p_name, p_sys->p_data_template, 250, p_sys->p_content_object, &pcobuf, p_sys->p_compsbuf, 0);
    if (i_ret < 0) {
        ndn_charbuf_destroy(&p_name);
        msg_Dbg(p_access, "NDNBlock unable to retrieve requested content: retrying");
        p_sys->i_missed_co++;
        return NULL;
    }
    i_ret = ndn_content_get_value(p_sys->p_content_object->buf, p_sys->p_content_object->length, &pcobuf, &data, &data_size);
    if (ndn_is_final_pco(p_sys->p_content_object->buf, &pcobuf, p_sys->p_compsbuf) == 1 || data_size < p_sys->i_chunksize)
        b_last = true;
    if (data_size > 0) {
        start_offset = p_access->info.i_pos % p_sys->i_chunksize;
        /* Ask for next fragment as soon as possible */
        if (!b_last) {
            i_nextpos = p_access->info.i_pos + (data_size - start_offset);
            /* prefetch a fragment if it's not past the end */
            if (p_sys->i_prefetch * p_sys->i_chunksize <= p_access->info.i_size - i_nextpos) {
                sequenced_name(p_name, p_sys->p_name, p_sys->i_prefetch + i_nextpos / p_sys->i_chunksize);
                vlc_mutex_lock(&p_sys->lock);
                i_ret = ndn_express_interest(p_sys->ndn_pf, p_name, p_sys->prefetch, p_sys->p_prefetch_template);
                vlc_mutex_unlock(&p_sys->lock);
            }
        }
        if (start_offset > data_size) {
            msg_Err(p_access, "NDNBlock start_offset %"PRId64" > data_size %zu", start_offset, data_size);
        } else {
            p_block = block_New(p_access, data_size - start_offset);
            memcpy(p_block->p_buffer, data + start_offset, data_size - start_offset);
        }
        p_access->info.i_pos += (data_size - start_offset);
    }
    ndn_charbuf_destroy(&p_name);
    
    // end
    if (b_last) {
        p_access->info.i_size = p_access->info.i_pos;
        p_access->info.b_eof = true;
    }
    return (p_block);
}

/*****************************************************************************
 * NDNSeek:
 *****************************************************************************/
/* XXX - VLC behavior when playing an MP4 file is to seek back and forth for
 * the audio and video, which may be separated by many megabytes, so it is
 * a much better (and possibly required) that the code not discard all
 * previously buffered data when seeking, since the app is likely to seek back
 * close to where it was very quickly.
 */
#if (VLCPLUGINVER < 10100)
static int NDNSeek(access_t *p_access, int64_t i_pos)
#else
static int NDNSeek(access_t *p_access, uint64_t i_pos)
#endif
{
    access_sys_t *p_sys = p_access->p_sys;
    struct ndn_charbuf *p_name;
    int i, i_prefetch, i_base;
    
#if (VLCPLUGINVER < 10100)
    if (i_pos < 0) {
        msg_Warn(p_access, "NDNSeek attempting to seek before the beginning %"PRId64".", i_pos);
        i_pos = 0;
    }
#endif
    /* prefetch, but only do full amount if going forward */
    if (i_pos > p_access->info.i_pos)
        i_prefetch = p_sys->i_prefetch;
    else
        i_prefetch = p_sys->i_prefetch / 2;
    i_base = i_pos / p_sys->i_chunksize;
    p_name = ndn_charbuf_create();
    for (i = 0; i <= i_prefetch; i++) {
        sequenced_name(p_name, p_sys->p_name, i_base + i);
        vlc_mutex_lock(&p_sys->lock);
        ndn_express_interest(p_sys->ndn_pf, p_name, p_sys->prefetch,
                             p_sys->p_prefetch_template);
        vlc_mutex_unlock(&p_sys->lock);
    }
    ndn_charbuf_destroy(&p_name);        
    
    p_access->info.i_pos = i_pos;
    p_access->info.b_eof = false;
    return (VLC_SUCCESS);
}
/*****************************************************************************
 * Control:
 *****************************************************************************/
static int
NDNControl(access_t *p_access, int i_query, va_list args)
{
    bool   *pb_bool;
    int64_t      *pi_64;
    
    switch(i_query)
    {
        case ACCESS_CAN_SEEK:
        case ACCESS_CAN_FASTSEEK:
            pb_bool = (bool*)va_arg(args, bool *);
            *pb_bool = var_CreateGetBool(p_access, "ndn-streams-seekable");
            break;
            
        case ACCESS_CAN_CONTROL_PACE:
        case ACCESS_CAN_PAUSE:
            pb_bool = (bool*)va_arg(args, bool *);
            *pb_bool = true;
            break;
            
        case ACCESS_GET_PTS_DELAY:
            pi_64 = (int64_t*)va_arg(args, int64_t *);
            *pi_64 = INT64_C(1000) *
            (int64_t) var_InheritInteger(p_access, "network-caching");
            break;
            
        case ACCESS_SET_PAUSE_STATE:
            pb_bool = (bool*)va_arg(args, bool *);
            break;
            
        case ACCESS_GET_TITLE_INFO:
        case ACCESS_GET_META:
        case ACCESS_SET_TITLE:
        case ACCESS_SET_SEEKPOINT:
        case ACCESS_SET_PRIVATE_ID_STATE:
    	case ACCESS_SET_PRIVATE_ID_CA:
        case ACCESS_GET_PRIVATE_ID_STATE:
        case ACCESS_GET_CONTENT_TYPE:
            return VLC_EGENERIC;
            
        default:
            msg_Warn(p_access, "NDNControl unimplemented query in control - %d", i_query);
            return VLC_EGENERIC;
            
    }
    return VLC_SUCCESS;
}

static void *
ndn_prefetch_thread(void *p_this)
{
    access_t *p_access = (access_t *)p_this;
    access_sys_t *p_sys = p_access->p_sys;
    struct pollfd fds[1];
    int i_ret = 0;
    
    msg_Info(p_access, "ndn_prefetch_thread starting");
    fds[0].fd = ndn_get_connection_fd(p_sys->ndn_pf);
    fds[0].events = POLLIN;
    do {
        i_ret = poll(fds, 1, 200);
        if (i_ret < 0 && errno != EINTR)    /* a real error occurred */
            break;
        if (i_ret > 0) {
            vlc_mutex_lock(&p_sys->lock);
            i_ret = ndn_run(p_sys->ndn_pf, 0);
            vlc_mutex_unlock(&p_sys->lock);
        }
    } while (i_ret == 0 && ndn_get_connection_fd(p_sys->ndn_pf) >= 0);
    msg_Info(p_access, "ndn_prefetch_thread exiting");
    return NULL;
}

static enum ndn_upcall_res
discard_content(struct ndn_closure *selfp,
                enum ndn_upcall_kind kind,
                struct ndn_upcall_info *info)
{
    return(NDN_UPCALL_RESULT_OK);
}

static void
sequenced_name(struct ndn_charbuf *name, struct ndn_charbuf *basename, uintmax_t seq)
{
    ndn_charbuf_reset(name);
    if (basename != NULL) {
        ndn_charbuf_append_charbuf(name, basename);
        ndn_name_append_numeric(name, NDN_MARKER_SEQNUM, seq);
    }
}

static struct ndn_charbuf *
make_prefetch_template()
{
    struct ndn_charbuf *templ = ndn_charbuf_create_n(16);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    ndnb_append_tagged_binary_number(templ, NDN_DTAG_InterestLifetime, NDN_PREFETCH_LIFETIME);
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

static struct ndn_charbuf *
make_data_template()
{
    struct ndn_charbuf *templ = ndn_charbuf_create_n(16);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Interest, NDN_DTAG);
    ndn_charbuf_append_tt(templ, NDN_DTAG_Name, NDN_DTAG);
    ndn_charbuf_append_closer(templ); /* </Name> */
    ndn_charbuf_append_tt(templ, NDN_DTAG_MaxSuffixComponents, NDN_DTAG);
    ndnb_append_number(templ, 1);
    ndn_charbuf_append_closer(templ); /* </MaxSuffixComponents> */
    ndnb_append_tagged_binary_number(templ, NDN_DTAG_InterestLifetime, NDN_DATA_LIFETIME);
    ndn_charbuf_append_closer(templ); /* </Interest> */
    return(templ);
}

