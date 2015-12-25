# ndnr/dir.mk
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

LDLIBS = -L$(NDNLIBDIR) -L$(SYNCLIBDIR) $(MORE_LDLIBS) -lndnsync -lndn
NDNLIBDIR = ../lib
SYNCLIBDIR = ../sync
# Override conf.mk or else we don't pick up all the includes
CPREFLAGS = -I../include -I..

INSTALLED_PROGRAMS = ndnr
PROGRAMS = $(INSTALLED_PROGRAMS)
DEBRIS = 

BROKEN_PROGRAMS = 
CSRC = ndnr_dispatch.c ndnr_forwarding.c ndnr_init.c ndnr_internal_client.c ndnr_io.c ndnr_link.c ndnr_main.c ndnr_match.c ndnr_msg.c ndnr_net.c ndnr_proto.c ndnr_sendq.c ndnr_stats.c ndnr_store.c ndnr_sync.c ndnr_util.c
HSRC = ndnr_dispatch.h ndnr_forwarding.h ndnr_init.h ndnr_internal_client.h        \
       ndnr_io.h ndnr_link.h ndnr_match.h ndnr_msg.h ndnr_net.h ndnr_private.h     \
       ndnr_proto.h ndnr_sendq.h ndnr_stats.h ndnr_store.h ndnr_sync.h ndnr_util.h

SCRIPTSRC = 

default: $(PROGRAMS)

all: default $(BROKEN_PROGRAMS)

$(PROGRAMS): $(NDNLIBDIR)/libndn.a $(SYNCLIBDIR)/libndnsync.a

NDNR_OBJ = ndnr_dispatch.o ndnr_forwarding.o ndnr_init.o ndnr_internal_client.o ndnr_io.o ndnr_link.o ndnr_main.o ndnr_match.o ndnr_msg.o ndnr_net.o ndnr_proto.o ndnr_sendq.o ndnr_stats.o ndnr_store.o ndnr_sync.o ndnr_util.o

ndnr: $(NDNR_OBJ)
	$(CC) $(CFLAGS) -o $@ $(NDNR_OBJ) $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

clean:
	rm -f *.o *.a $(PROGRAMS) $(BROKEN_PROGRAMS) depend
	rm -rf *.dSYM *.gcov *.gcda *.gcno $(DEBRIS)

check test: ndnr $(SCRIPTSRC)
	: ---------------------- :
	:  ndnr unit tests pass  :
	: ---------------------- :

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
ndnr_dispatch.o: ndnr_dispatch.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ../sync/SyncBase.h \
  ../include/ndn/loglevels.h ../sync/sync_plumbing.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_dispatch.h ndnr_forwarding.h ndnr_io.h \
  ndnr_link.h ndnr_match.h ndnr_msg.h ndnr_proto.h ndnr_sendq.h \
  ndnr_stats.h ndnr_store.h ndnr_sync.h ndnr_util.h
ndnr_forwarding.o: ndnr_forwarding.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_forwarding.h ndnr_io.h ndnr_link.h \
  ndnr_match.h ndnr_msg.h ../include/ndn/loglevels.h ndnr_stats.h \
  ndnr_util.h
ndnr_init.o: ndnr_init.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ../sync/sync_plumbing.h \
  ../sync/SyncActions.h ../sync/SyncBase.h ../include/ndn/loglevels.h \
  ../sync/sync_plumbing.h ../sync/SyncRoot.h ../sync/SyncUtil.h \
  ../sync/IndexSorter.h ndnr_private.h ../include/ndn/seqwriter.h \
  ndnr_init.h ndnr_dispatch.h ndnr_forwarding.h ndnr_internal_client.h \
  ndnr_io.h ndnr_msg.h ndnr_net.h ndnr_proto.h ndnr_sendq.h ndnr_store.h \
  ndnr_sync.h ndnr_util.h
ndnr_internal_client.o: ndnr_internal_client.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/schedule.h ../include/ndn/sockaddrutil.h \
  ../include/ndn/uri.h ../include/ndn/keystore.h ndnr_private.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/seqwriter.h \
  ndnr_internal_client.h ndnr_forwarding.h ../include/ndn/hashtb.h \
  ndnr_io.h ndnr_msg.h ../include/ndn/loglevels.h ndnr_proto.h \
  ndnr_util.h
ndnr_io.o: ndnr_io.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_io.h ndnr_forwarding.h \
  ndnr_internal_client.h ndnr_link.h ndnr_msg.h \
  ../include/ndn/loglevels.h ndnr_sendq.h ndnr_stats.h
ndnr_link.o: ndnr_link.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_link.h ndnr_forwarding.h \
  ndnr_internal_client.h ndnr_io.h ndnr_match.h ndnr_msg.h \
  ../include/ndn/loglevels.h ndnr_sendq.h ndnr_stats.h ndnr_store.h \
  ndnr_util.h
ndnr_main.o: ndnr_main.c ndnr_private.h ../include/ndn/ndn_private.h \
  ../include/ndn/coding.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/charbuf.h ../include/ndn/schedule.h \
  ../include/ndn/seqwriter.h ndnr_init.h ndnr_dispatch.h ndnr_msg.h \
  ../include/ndn/loglevels.h ndnr_stats.h
ndnr_match.o: ndnr_match.c ../include/ndn/bloom.h \
  ../include/ndn/btree_content.h ../include/ndn/btree.h \
  ../include/ndn/charbuf.h ../include/ndn/hashtb.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndn_private.h ../include/ndn/face_mgmt.h \
  ../include/ndn/sockcreate.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_match.h ndnr_forwarding.h ndnr_io.h \
  ndnr_msg.h ../include/ndn/loglevels.h ndnr_sendq.h ndnr_store.h
ndnr_msg.o: ndnr_msg.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ndnr_private.h ../include/ndn/ndn_private.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/schedule.h ../include/ndn/seqwriter.h ndnr_msg.h \
  ../include/ndn/loglevels.h
ndnr_net.o: ndnr_net.c ndnr_private.h ../include/ndn/ndn_private.h \
  ../include/ndn/coding.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/charbuf.h ../include/ndn/schedule.h \
  ../include/ndn/seqwriter.h ndnr_net.h ndnr_io.h ndnr_msg.h \
  ../include/ndn/loglevels.h
ndnr_proto.o: ndnr_proto.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndn_private.h ../include/ndn/hashtb.h \
  ../include/ndn/schedule.h ../include/ndn/sockaddrutil.h \
  ../include/ndn/uri.h ../sync/SyncBase.h ../include/ndn/loglevels.h \
  ../sync/sync_plumbing.h ndnr_private.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/seqwriter.h ndnr_proto.h ndnr_dispatch.h \
  ndnr_forwarding.h ndnr_init.h ndnr_io.h ndnr_msg.h ndnr_sendq.h \
  ndnr_store.h ndnr_sync.h ndnr_util.h
ndnr_sendq.o: ndnr_sendq.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_sendq.h ndnr_io.h ndnr_link.h \
  ndnr_msg.h ../include/ndn/loglevels.h ndnr_store.h
ndnr_stats.o: ndnr_stats.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/schedule.h ../include/ndn/sockaddrutil.h \
  ../include/ndn/hashtb.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/ndn_private.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/seqwriter.h ndnr_stats.h ndnr_io.h ndnr_msg.h \
  ../include/ndn/loglevels.h
ndnr_store.o: ndnr_store.c ../include/ndn/bloom.h \
  ../include/ndn/btree_content.h ../include/ndn/btree.h \
  ../include/ndn/charbuf.h ../include/ndn/hashtb.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndn_private.h ../include/ndn/face_mgmt.h \
  ../include/ndn/sockcreate.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_stats.h ndnr_store.h ndnr_init.h \
  ndnr_link.h ndnr_util.h ndnr_proto.h ndnr_msg.h \
  ../include/ndn/loglevels.h ndnr_sync.h ndnr_match.h ndnr_sendq.h \
  ndnr_io.h
ndnr_sync.o: ndnr_sync.c ../include/ndn/btree.h ../include/ndn/charbuf.h \
  ../include/ndn/hashtb.h ../include/ndn/btree_content.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/indexbuf.h \
  ../include/ndn/schedule.h ../sync/SyncBase.h ../include/ndn/loglevels.h \
  ../sync/sync_plumbing.h ndnr_private.h ../include/ndn/ndn_private.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/seqwriter.h ndnr_dispatch.h \
  ndnr_io.h ndnr_link.h ndnr_msg.h ndnr_proto.h ndnr_store.h ndnr_sync.h \
  ndnr_util.h ../sync/sync_plumbing.h
ndnr_util.o: ndnr_util.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndn_private.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/hashtb.h ../include/ndn/schedule.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h ndnr_private.h \
  ../include/ndn/seqwriter.h ndnr_util.h
