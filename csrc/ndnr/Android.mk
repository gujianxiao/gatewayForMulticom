# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009-2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
# for more details. You should have received a copy of the GNU General Public
# License along with this program; if not, write to the
# Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
# Boston, MA 02110-1301, USA.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE		:= libndnr
LOCAL_C_INCLUDES	:= $(LOCAL_PATH)
LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/../include 
LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/..

LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/../../android/external/openssl-armv5/include

NDNROBJ := ndnr_dispatch.o ndnr_forwarding.o ndnr_init.o ndnr_internal_client.o ndnr_io.o ndnr_link.o ndnr_main.o ndnr_match.o ndnr_msg.o ndnr_net.o ndnr_proto.o ndnr_sendq.o ndnr_stats.o ndnr_store.o ndnr_sync.o ndnr_util.o ../sync/IndexSorter.o ../sync/SyncActions.o ../sync/SyncBase.o ../sync/SyncHashCache.o ../sync/SyncNode.o ../sync/SyncRoot.o ../sync/SyncTreeWorker.o ../sync/SyncUtil.o
NDNRSRC := $(NDNROBJ:.o=.c)

LOCAL_SRC_FILES := $(NDNRSRC)
LOCAL_CFLAGS := -g
LOCAL_STATIC_LIBRARIES := libcrypto libndnx
LOCAL_SHARED_LIBRARIES :=

include $(BUILD_STATIC_LIBRARY)
