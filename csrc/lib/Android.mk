# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009, 2010, 2012 Palo Alto Research Center, Inc.
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

LOCAL_MODULE		:= libndnx
LOCAL_C_INCLUDES	:= $(LOCAL_PATH)
LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/../include 
LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/..

LOCAL_C_INCLUDES	+= $(LOCAL_PATH)/../../android/external/openssl-armv5/include

NDNLIBOBJ := ndn_client.o ndn_charbuf.o ndn_indexbuf.o ndn_coding.o \
		ndn_dtag_table.o ndn_schedule.o ndn_extend_dict.o \
		ndn_buf_decoder.o ndn_uri.o ndn_buf_encoder.o ndn_bloom.o \
		ndn_name_util.o ndn_face_mgmt.o ndn_reg_mgmt.o ndn_digest.o \
		ndn_interest.o ndn_keystore.o ndn_seqwriter.o ndn_signing.o \
		ndn_sockcreate.o ndn_traverse.o \
		ndn_match.o hashtb.o ndn_merkle_path_asn1.o \
		ndn_sockaddrutil.o ndn_setup_sockaddr_un.o \
		ndn_bulkdata.o ndn_versioning.o ndn_header.o ndn_fetch.o \
		ndn_btree.o ndn_btree_content.o ndn_btree_store.o

NDNLIBSRC := $(NDNLIBOBJ:.o=.c)

LOCAL_SRC_FILES := $(NDNLIBSRC)
LOCAL_CFLAGS := -g
LOCAL_STATIC_LIBRARIES := libcrypto libssl
LOCAL_SHARED_LIBRARIES :=

include $(BUILD_STATIC_LIBRARY)
