# apps/HttpProxy/dir.mk
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2011 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

LDLIBS = -L$(NDNLIBDIR) $(MORE_LDLIBS) $(OPENSSL_LIBS) -lndn -lcrypto
NDNLIBDIR = ../../csrc/lib
# Do not install these yet - we should choose names more appropriate for
# a flat namespace in /usr/local/bin
# INSTALLED_PROGRAMS = NetFetch HttpProxy
PROGRAMS = NetFetch HttpProxy

CSRC = HttpProxy.c NetFetch.c ProxyUtil.c SockHop.c

default all: $(PROGRAMS)

$(PROGRAMS): $(NDNLIBDIR)/libndn.a

HttpProxy: HttpProxy.o ProxyUtil.o SockHop.o
	$(CC) $(CFLAGS) -o $@ HttpProxy.o ProxyUtil.o SockHop.o $(LDLIBS)

NetFetch: NetFetch.o ProxyUtil.o SockHop.o
	$(CC) $(CFLAGS) -o $@ NetFetch.o ProxyUtil.o SockHop.o $(LDLIBS)

clean:
	rm -f *.o $(PROGRAMS) depend
	rm -rf *.dSYM $(DEBRIS) *% *~

test: default

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
HttpProxy.o: HttpProxy.c ProxyUtil.h SockHop.h ProxyUtil.h \
  ../include/ndn/fetch.h ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h
NetFetch.o: NetFetch.c ProxyUtil.h SockHop.h ProxyUtil.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/keystore.h ../include/ndn/signing.h
ProxyUtil.o: ProxyUtil.c ProxyUtil.h
SockHop.o: SockHop.c SockHop.h ProxyUtil.h ProxyUtil.h
