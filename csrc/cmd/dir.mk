# cmd/dir.mk
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009-2013 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

LDLIBS = -L$(NDNLIBDIR) $(MORE_LDLIBS) -lndn
EXPATLIBS = -lexpat
NDNLIBDIR = ../lib
SYNCLIBS = -L../sync -lndnsync

INSTALLED_PROGRAMS = \
    ndn_ndnbtoxml ndn_splitndnb ndnc ndndumpnames ndnnamelist ndnrm \
    ndnls ndnslurp ndnbx ndncat ndnbasicconfig \
    ndnsendchunks ndncatchunks ndncatchunks2 \
    ndnpoke ndnpeek ndnhexdumpdata \
    ndnseqwriter ndnsimplecat \
    ndnfilewatch \
    ndnguestprefix \
    ndninitkeystore \
    ndnlibtest \
    ndnsyncwatch ndnsyncslice \
    ndn-pubkey-name \
    $(EXPAT_PROGRAMS) $(PCAP_PROGRAMS)

PROGRAMS = $(INSTALLED_PROGRAMS) \
    ndnbuzz  \
    dataresponsetest \
    ndn_fetch_test \
    ndnsnew \
   $(PCAP_PROGRAMS)

EXPAT_PROGRAMS = ndn_xmltondnb
#PCAP_PROGRAMS = ndndumppcap
BROKEN_PROGRAMS =
DEBRIS =
SCRIPTSRC = ndn_initkeystore.sh
CSRC =  ndn_ndnbtoxml.c ndn_splitndnb.c ndn_xmltondnb.c ndnbasicconfig.c \
       ndnbuzz.c ndnbx.c \
       ndnc.c \
       ndncat.c ndnsimplecat.c ndncatchunks.c ndncatchunks2.c \
       ndndumpnames.c ndndumppcap.c ndnfilewatch.c \
       ndnguestprefix.c ndnpeek.c ndnhexdumpdata.c \
       ndninitkeystore.c ndnls.c ndnnamelist.c ndnpoke.c ndnrm.c ndnsendchunks.c \
       ndnseqwriter.c \
       ndnsnew.c \
       ndnsyncwatch.c ndnsyncslice.c ndn_fetch_test.c ndnlibtest.c ndnslurp.c dataresponsetest.c \
       ndn-pubkey-name.c

default all: $(PROGRAMS)
# Don't try to build broken programs right now.
# all: $(BROKEN_PROGRAMS)

test: default

$(PROGRAMS): $(NDNLIBDIR)/libndn.a

ndn_ndnbtoxml: ndn_ndnbtoxml.o
	$(CC) $(CFLAGS) -o $@ ndn_ndnbtoxml.o $(LDLIBS)

ndn_xmltondnb: ndn_xmltondnb.o
	$(CC) $(CFLAGS) -o $@ ndn_xmltondnb.o $(LDLIBS) $(EXPATLIBS)

ndn_splitndnb: ndn_splitndnb.o
	$(CC) $(CFLAGS) -o $@ ndn_splitndnb.o $(LDLIBS)

hashtbtest: hashtbtest.o
	$(CC) $(CFLAGS) -o $@ hashtbtest.o $(LDLIBS)

matrixtest: matrixtest.o
	$(CC) $(CFLAGS) -o $@ matrixtest.o $(LDLIBS)

skel_decode_test: skel_decode_test.o
	$(CC) $(CFLAGS) -o $@ skel_decode_test.o $(LDLIBS)

ndnlibtest: ndnlibtest.o
	$(CC) $(CFLAGS) -o $@ ndnlibtest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

dataresponsetest: dataresponsetest.o
	$(CC) $(CFLAGS) -o $@ dataresponsetest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

encodedecodetest: encodedecodetest.o
	$(CC) $(CFLAGS) -o $@ encodedecodetest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnc: ndnc.o
	$(CC) $(CFLAGS) -o $@ ndnc.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnguestprefix: ndnguestprefix.o
	$(CC) $(CFLAGS) -o $@ ndnguestprefix.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndndumpnames: ndndumpnames.o
	$(CC) $(CFLAGS) -o $@ ndndumpnames.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnls: ndnls.o
	$(CC) $(CFLAGS) -o $@ ndnls.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnnamelist: ndnnamelist.o
	$(CC) $(CFLAGS) -o $@ ndnnamelist.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnrm: ndnrm.o
	$(CC) $(CFLAGS) -o $@ ndnrm.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnslurp: ndnslurp.o
	$(CC) $(CFLAGS) -o $@ ndnslurp.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnbx: ndnbx.o
	$(CC) $(CFLAGS) -o $@ ndnbx.o $(LDLIBS)   $(OPENSSL_LIBS) -lcrypto

ndncat: ndncat.o
	$(CC) $(CFLAGS) -o $@ ndncat.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnsimplecat: ndnsimplecat.o
	$(CC) $(CFLAGS) -o $@ ndnsimplecat.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnsendchunks: ndnsendchunks.o
	$(CC) $(CFLAGS) -o $@ ndnsendchunks.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnseqwriter: ndnseqwriter.o
	$(CC) $(CFLAGS) -o $@ ndnseqwriter.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndn_fetch_test: ndn_fetch_test.o
	$(CC) $(CFLAGS) -o $@ ndn_fetch_test.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndncatchunks: ndncatchunks.o
	$(CC) $(CFLAGS) -o $@ ndncatchunks.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndncatchunks2: ndncatchunks2.o
	$(CC) $(CFLAGS) -o $@ ndncatchunks2.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnbasicconfig: ndnbasicconfig.o
	$(CC) $(CFLAGS) -o $@ ndnbasicconfig.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnbuzz: ndnbuzz.o
	$(CC) $(CFLAGS) -o $@ ndnbuzz.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnpoke: ndnpoke.o
	$(CC) $(CFLAGS) -o $@ ndnpoke.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnpeek: ndnpeek.o
	$(CC) $(CFLAGS) -o $@ ndnpeek.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnhexdumpdata: ndnhexdumpdata.o
	$(CC) $(CFLAGS) -o $@ ndnhexdumpdata.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndninitkeystore: ndninitkeystore.o
	$(CC) $(CFLAGS) -o $@ ndninitkeystore.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndndumppcap: ndndumppcap.o
	$(CC) $(CFLAGS) -o $@ ndndumppcap.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto -lpcap

ndnfilewatch: ndnfilewatch.o
	$(CC) $(CFLAGS) -o $@ ndnfilewatch.o

ndnsnew: ndnsnew.o
	$(CC) $(CFLAGS) -o $@ ndnsnew.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnsyncwatch: ndnsyncwatch.o
	$(CC) $(CFLAGS) -o $@ ndnsyncwatch.o $(SYNCLIBS) $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndnsyncslice: ndnsyncslice.o
	$(CC) $(CFLAGS) -o $@ ndnsyncslice.o $(SYNCLIBS) $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndn-pubkey-name: ndn-pubkey-name.o
	$(CC) $(CFLAGS) -o $@ ndn-pubkey-name.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

clean:
	rm -f *.o libndn.a libndn.1.$(SHEXT) $(PROGRAMS) depend
	rm -rf *.dSYM $(DEBRIS) *% *~

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
ndn_ndnbtoxml.o: ndn_ndnbtoxml.c ../include/ndn/charbuf.h \
  ../include/ndn/coding.h ../include/ndn/extend_dict.h
ndn_splitndnb.o: ndn_splitndnb.c ../include/ndn/coding.h
ndn_xmltondnb.o: ndn_xmltondnb.c ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/extend_dict.h
ndnbasicconfig.o: ndnbasicconfig.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/ndnd.h ../include/ndn/uri.h \
  ../include/ndn/face_mgmt.h ../include/ndn/sockcreate.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/signing.h \
  ../include/ndn/keystore.h
ndnbuzz.o: ndnbuzz.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnbx.o: ndnbx.c ../include/ndn/charbuf.h ../include/ndn/coding.h \
  ../include/ndn/ndn.h ../include/ndn/indexbuf.h
ndnc.o: ndnc.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndn_private.h ../include/ndn/lned.h ../include/ndn/uri.h
ndncat.o: ndncat.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/fetch.h
ndnsimplecat.o: ndnsimplecat.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndncatchunks.o: ndncatchunks.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndncatchunks2.o: ndncatchunks2.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/schedule.h \
  ../include/ndn/uri.h
ndndumpnames.o: ndndumpnames.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndndumppcap.o: ndndumppcap.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndnd.h
ndnfilewatch.o: ndnfilewatch.c
ndnguestprefix.o: ndnguestprefix.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnpeek.o: ndnpeek.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnhexdumpdata.o: ndnhexdumpdata.c ../include/ndn/coding.h \
  ../include/ndn/ndn.h ../include/ndn/charbuf.h ../include/ndn/indexbuf.h
ndninitkeystore.o: ndninitkeystore.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/keystore.h
ndnls.o: ndnls.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnnamelist.o: ndnnamelist.c ../include/ndn/coding.h ../include/ndn/uri.h \
  ../include/ndn/charbuf.h
ndnpoke.o: ndnpoke.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/keystore.h ../include/ndn/signing.h
ndnrm.o: ndnrm.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnsendchunks.o: ndnsendchunks.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/keystore.h ../include/ndn/signing.h
ndnseqwriter.o: ndnseqwriter.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/seqwriter.h
ndnsnew.o: ndnsnew.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h
ndnsyncwatch.o: ndnsyncwatch.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/sync.h ../include/ndn/uri.h
ndnsyncslice.o: ndnsyncslice.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/sync.h ../include/ndn/uri.h
ndn_fetch_test.o: ndn_fetch_test.c ../include/ndn/fetch.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndnlibtest.o: ndnlibtest.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/uri.h
ndnslurp.o: ndnslurp.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
dataresponsetest.o: dataresponsetest.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h
