# sync/dir.mk
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009-2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

NDNLIBDIR = ../lib
LDLIBS = -L$(NDNLIBDIR) $(MORE_LDLIBS) -lndn -L. -lndnsync

INSTALLED_PROGRAMS = 

PROGRAMS = $(INSTALLED_PROGRAMS) \
    SyncTest

BROKEN_PROGRAMS =
DEBRIS =
SCRIPTSRC = 
CSRC = \
 IndexSorter.c \
 SyncActions.c \
 SyncBase.c \
 SyncHashCache.c \
 SyncNode.c \
 SyncRoot.c \
 SyncTest.c \
 SyncTreeWorker.c \
 SyncUtil.c \
 UpdateSketch.c \
 sync_api.c \
 sync_diff.c

HSRC = IndexSorter.h SyncActions.h SyncBase.h SyncHashCache.h SyncMacros.h \
       SyncNode.h SyncPrivate.h SyncRoot.h SyncTreeWorker.h SyncUtil.h \
       sync_plumbing.h sync_diff.h 

LIBS = libndnsync.a
LIB_OBJS = IndexSorter.o SyncBase.o SyncHashCache.o SyncNode.o SyncRoot.o \
       SyncTreeWorker.o SyncUtil.o SyncActions.o sync_diff.o sync_api.o

default all: $(PROGRAMS) $(LIBS)
all: $(BROKEN_PROGRAMS)

test: default

$(PROGRAMS): $(NDNLIBDIR)/libndn.a $(LIBS)

lib:	$(LIBS)

libndnsync.a:	$(LIB_OBJS)
	$(RM) $@
	$(AR) crus $@ $(LIB_OBJS)

SyncTest_OBJ = SyncTest.o
SyncTest: $(SyncTest_OBJ)
	$(CC) $(CFLAGS) -o $@ $(SyncTest_OBJ) $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

clean:
	$(RM) *.o *.a $(PROGRAMS) $(BROKEN_PROGRAMS) depend
	$(RM) -r *.dSYM *.gcov *.gcda *.gcno $(DEBRIS)

check test: SyncTest $(SCRIPTSRC)
	@echo No sync_exp tests hooked up yet.
	: -------------------------- :
	:  sync_exp unit tests pass  :
	: -------------------------- :

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
IndexSorter.o: IndexSorter.c IndexSorter.h
SyncActions.o: SyncActions.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/schedule.h SyncActions.h SyncBase.h \
  ../include/ndn/loglevels.h sync_plumbing.h SyncRoot.h SyncUtil.h \
  IndexSorter.h SyncNode.h SyncMacros.h SyncPrivate.h SyncTreeWorker.h \
  SyncHashCache.h
SyncBase.o: SyncBase.c SyncMacros.h SyncBase.h ../include/ndn/loglevels.h \
  sync_plumbing.h ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h SyncPrivate.h \
  SyncRoot.h SyncUtil.h IndexSorter.h ../include/ndn/uri.h
SyncHashCache.o: SyncHashCache.c SyncBase.h ../include/ndn/loglevels.h \
  sync_plumbing.h ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h SyncHashCache.h \
  SyncNode.h SyncMacros.h SyncRoot.h SyncUtil.h IndexSorter.h
SyncNode.o: SyncNode.c SyncNode.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h SyncMacros.h SyncRoot.h SyncUtil.h \
  IndexSorter.h
SyncRoot.o: SyncRoot.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/digest.h ../include/ndn/schedule.h ../include/ndn/uri.h \
  ../include/ndn/loglevels.h SyncMacros.h SyncPrivate.h SyncBase.h \
  sync_plumbing.h SyncRoot.h SyncUtil.h IndexSorter.h SyncHashCache.h
SyncTest.o: SyncTest.c SyncActions.h ../include/ndn/charbuf.h SyncBase.h \
  ../include/ndn/loglevels.h sync_plumbing.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/indexbuf.h SyncRoot.h SyncUtil.h \
  IndexSorter.h SyncHashCache.h SyncNode.h SyncMacros.h SyncPrivate.h \
  SyncTreeWorker.h ../include/ndn/digest.h ../include/ndn/fetch.h \
  ../include/ndn/uri.h ../include/ndn/seqwriter.h
SyncTreeWorker.o: SyncTreeWorker.c SyncMacros.h SyncNode.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h SyncTreeWorker.h SyncHashCache.h SyncUtil.h \
  IndexSorter.h
SyncUtil.o: SyncUtil.c SyncBase.h ../include/ndn/loglevels.h \
  sync_plumbing.h ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h SyncHashCache.h \
  SyncNode.h SyncMacros.h SyncPrivate.h SyncRoot.h SyncUtil.h \
  IndexSorter.h ../include/ndn/uri.h
UpdateSketch.o: UpdateSketch.c
sync_api.o: sync_api.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/digest.h ../include/ndn/loglevels.h \
  ../include/ndn/schedule.h ../include/ndn/sync.h ../include/ndn/uri.h \
  ../include/ndn/ndn_private.h sync_diff.h SyncHashCache.h SyncRoot.h \
  SyncTreeWorker.h SyncUtil.h IndexSorter.h SyncNode.h SyncMacros.h \
  SyncPrivate.h SyncBase.h sync_plumbing.h
sync_diff.o: sync_diff.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/digest.h ../include/ndn/schedule.h ../include/ndn/sync.h \
  ../include/ndn/uri.h IndexSorter.h SyncNode.h SyncMacros.h \
  SyncPrivate.h SyncBase.h ../include/ndn/loglevels.h sync_plumbing.h \
  SyncRoot.h SyncUtil.h SyncTreeWorker.h SyncHashCache.h sync_diff.h
