# libexec/dir.mk
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

LDLIBS = -L$(NDNLIBDIR) $(MORE_LDLIBS) -lndn
NDNLIBDIR = ../lib

INSTALLED_PROGRAMS = ndndc
DEBRIS = ndndc-inject
PROGRAMS = $(INSTALLED_PROGRAMS) udplink
CSRC = ndndc-log.c ndndc-main.c ndndc-srv.c ndndc.c udplink.c
HSRC = ndndc-log.h ndndc-srv.h ndndc.h

default all: $(PROGRAMS)

$(PROGRAMS): $(NDNLIBDIR)/libndn.a

ndndc: ndndc-log.o ndndc-srv.o ndndc.o ndndc-main.o
	$(CC) $(CFLAGS) -o $@ ndndc-log.o ndndc-srv.o ndndc.o ndndc-main.o $(LDLIBS) $(OPENSSL_LIBS) $(RESOLV_LIBS) -lcrypto

udplink: udplink.o
	$(CC) $(CFLAGS) -o $@ udplink.o $(LDLIBS)  $(OPENSSL_LIBS) -lcrypto

clean:
	rm -f *.o *.a $(PROGRAMS) depend
	rm -rf *.dSYM $(DEBRIS)

test:
	@echo "Sorry, no libexec unit tests at this time"

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
ndndc-log.o: ndndc-log.c
ndndc-main.o: ndndc-main.c ndndc.h ../include/ndn/charbuf.h ndndc-log.h \
  ndndc-srv.h
ndndc-srv.o: ndndc-srv.c ndndc.h ../include/ndn/charbuf.h ndndc-srv.h \
  ndndc-log.h ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/reg_mgmt.h
ndndc.o: ndndc.c ndndc.h ../include/ndn/charbuf.h ndndc-log.h ndndc-srv.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndnd.h ../include/ndn/uri.h ../include/ndn/signing.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/reg_mgmt.h
udplink.o: udplink.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndnd.h
