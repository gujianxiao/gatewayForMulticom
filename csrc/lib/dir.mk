# lib/dir.mk
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
EXPATLIBS = -lexpat
NDNLIBDIR = ../lib

PROGRAMS = hashtbtest skel_decode_test \
    encodedecodetest signbenchtest basicparsetest ndnbtreetest

BROKEN_PROGRAMS =
DEBRIS = ndn_verifysig _bt_* test.keystore
CSRC = ndn_bloom.c \
       ndn_btree.c ndn_btree_content.c ndn_btree_store.c \
       ndn_buf_decoder.c ndn_buf_encoder.c ndn_bulkdata.c \
       ndn_charbuf.c ndn_client.c ndn_coding.c ndn_digest.c ndn_extend_dict.c \
       ndn_dtag_table.c ndn_indexbuf.c ndn_interest.c ndn_keystore.c \
       ndn_match.c ndn_reg_mgmt.c ndn_face_mgmt.c \
       ndn_merkle_path_asn1.c ndn_name_util.c ndn_schedule.c \
       ndn_seqwriter.c ndn_signing.c \
       ndn_sockcreate.c ndn_traverse.c ndn_uri.c \
       ndn_verifysig.c ndn_versioning.c \
       ndn_header.c \
       ndn_fetch.c \
       lned.c \
       encodedecodetest.c hashtb.c hashtbtest.c \
       signbenchtest.c skel_decode_test.c \
       basicparsetest.c ndnbtreetest.c \
       ndn_sockaddrutil.c ndn_setup_sockaddr_un.c
LIBS = libndn.a
LIB_OBJS = ndn_client.o ndn_charbuf.o ndn_indexbuf.o ndn_coding.o \
       ndn_dtag_table.o ndn_schedule.o ndn_extend_dict.o \
       ndn_buf_decoder.o ndn_uri.o ndn_buf_encoder.o ndn_bloom.o \
       ndn_name_util.o ndn_face_mgmt.o ndn_reg_mgmt.o ndn_digest.o \
       ndn_interest.o ndn_keystore.o ndn_seqwriter.o ndn_signing.o \
       ndn_sockcreate.o ndn_traverse.o \
       ndn_match.o hashtb.o ndn_merkle_path_asn1.o \
       ndn_sockaddrutil.o ndn_setup_sockaddr_un.o \
       ndn_bulkdata.o ndn_versioning.o ndn_header.o ndn_fetch.o \
       ndn_btree.o ndn_btree_content.o ndn_btree_store.o \
       lned.o

default all: dtag_check lib $(PROGRAMS)
# Don't try to build shared libs right now.
# all: shlib

all: ndn_verifysig

$(PROGRAMS) $(DEBRIS): libndn.a

install: install_headers
install_headers:
	@test -d $(DINST_INC) || (echo $(DINST_INC) does not exist.  Please mkdir -p $(DINST_INC) if this is what you intended. && exit 2)
	mkdir -p $(DINST_INC)/ndn
	for i in `cd ../include/ndn && echo *.h`; do                \
	    cmp -s ../include/ndn/$$i $(DINST_INC)/ndn/$$i || \
	        cp ../include/ndn/$$i $(DINST_INC)/ndn/$$i || \
	        exit 1;                                             \
	done

uninstall: uninstall_headers
uninstall_headers:
	test -L $(DINST_INC)/ndn && $(RM) $(DINST_INC)/ndn ||:
	test -L $(DINST_INC) || $(RM) -r $(DINST_INC)/ndn

shlib: $(SHLIBNAME)

lib: libndn.a

test: default encodedecodetest ndnbtreetest
	./encodedecodetest -o /dev/null
	./ndnbtreetest
	./ndnbtreetest - < q.dat
	$(RM) -R _bt_*

dtag_check: _always
	@./gen_dtag_table 2>/dev/null | diff - ndn_dtag_table.c | grep '^[<]' >/dev/null && echo '*** Warning: ndn_dtag_table.c may be out of sync with tagnames.cvsdict' || :

libndn.a: $(LIB_OBJS)
	$(RM) $@
	$(AR) crus $@ $(LIB_OBJS)

shared: $(SHLIBNAME)

$(SHLIBNAME): libndn.a $(SHLIBDEPS)
	$(LD) $(SHARED_LD_FLAGS) $(OPENSSL_LIBS) -lcrypto -o $@ libndn.a

$(PROGRAMS): libndn.a

hashtbtest: hashtbtest.o
	$(CC) $(CFLAGS) -o $@ hashtbtest.o $(LDLIBS)

skel_decode_test: skel_decode_test.o
	$(CC) $(CFLAGS) -o $@ skel_decode_test.o $(LDLIBS)

basicparsetest: basicparsetest.o libndn.a
	$(CC) $(CFLAGS) -o $@ basicparsetest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

encodedecodetest: encodedecodetest.o
	$(CC) $(CFLAGS) -o $@ encodedecodetest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

ndn_digest.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_digest.c

ndn_extend_dict.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_extend_dict.c

ndn_keystore.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_keystore.c

ndn_signing.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_signing.c

ndn_sockcreate.o:
	$(CC) $(CFLAGS) -c ndn_sockcreate.c

ndn_traverse.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_traverse.c

ndn_merkle_path_asn1.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_merkle_path_asn1.c

ndn_header.o:
	$(CC) $(CFLAGS) -c ndn_header.c

ndn_fetch.o:
	$(CC) $(CFLAGS) -c ndn_fetch.c

ndn_verifysig.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c ndn_verifysig.c

ndn_verifysig: ndn_verifysig.o
	$(CC) $(CFLAGS) -o $@ ndn_verifysig.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

signbenchtest.o:
	$(CC) $(CFLAGS) $(OPENSSL_CFLAGS) -c signbenchtest.c

signbenchtest: signbenchtest.o
	$(CC) $(CFLAGS) -o $@ signbenchtest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto 

ndnbtreetest.o:
	$(CC) $(CFLAGS) -Dndnbtreetest_main=main -c ndnbtreetest.c

ndnbtreetest: ndnbtreetest.o libndn.a
	$(CC) $(CFLAGS) -o $@ ndnbtreetest.o $(LDLIBS) $(OPENSSL_LIBS) -lcrypto

clean:
	rm -f *.o libndn.a libndn.1.$(SHEXT) $(PROGRAMS) depend
	rm -rf *.dSYM $(DEBRIS) *% *~

###############################
# Dependencies below here are checked by depend target
# but must be updated manually.
###############################
ndn_bloom.o: ndn_bloom.c ../include/ndn/bloom.h
ndn_btree.o: ndn_btree.c ../include/ndn/charbuf.h ../include/ndn/hashtb.h \
  ../include/ndn/btree.h
ndn_btree_content.o: ndn_btree_content.c ../include/ndn/btree.h \
  ../include/ndn/charbuf.h ../include/ndn/hashtb.h \
  ../include/ndn/btree_content.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/indexbuf.h \
  ../include/ndn/bloom.h ../include/ndn/uri.h
ndn_btree_store.o: ndn_btree_store.c ../include/ndn/btree.h \
  ../include/ndn/charbuf.h ../include/ndn/hashtb.h
ndn_buf_decoder.o: ndn_buf_decoder.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h
ndn_buf_encoder.o: ndn_buf_encoder.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/signing.h \
  ../include/ndn/ndn_private.h
ndn_bulkdata.o: ndn_bulkdata.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h
ndn_charbuf.o: ndn_charbuf.c ../include/ndn/charbuf.h
ndn_client.o: ndn_client.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/ndn_private.h ../include/ndn/ndnd.h \
  ../include/ndn/digest.h ../include/ndn/hashtb.h \
  ../include/ndn/reg_mgmt.h ../include/ndn/schedule.h \
  ../include/ndn/signing.h ../include/ndn/keystore.h ../include/ndn/uri.h
ndn_coding.o: ndn_coding.c ../include/ndn/coding.h
ndn_digest.o: ndn_digest.c ../include/ndn/digest.h
ndn_extend_dict.o: ndn_extend_dict.c ../include/ndn/charbuf.h \
  ../include/ndn/extend_dict.h ../include/ndn/coding.h
ndn_dtag_table.o: ndn_dtag_table.c ../include/ndn/coding.h
ndn_indexbuf.o: ndn_indexbuf.c ../include/ndn/indexbuf.h
ndn_interest.o: ndn_interest.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h
ndn_keystore.o: ndn_keystore.c ../include/ndn/keystore.h
ndn_match.o: ndn_match.c ../include/ndn/bloom.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/digest.h
ndn_reg_mgmt.o: ndn_reg_mgmt.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/reg_mgmt.h
ndn_face_mgmt.o: ndn_face_mgmt.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/face_mgmt.h \
  ../include/ndn/sockcreate.h
ndn_merkle_path_asn1.o: ndn_merkle_path_asn1.c \
  ../include/ndn/merklepathasn1.h
ndn_name_util.o: ndn_name_util.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/random.h
ndn_schedule.o: ndn_schedule.c ../include/ndn/schedule.h
ndn_seqwriter.o: ndn_seqwriter.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/seqwriter.h
ndn_signing.o: ndn_signing.c ../include/ndn/merklepathasn1.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/signing.h \
  ../include/ndn/random.h
ndn_sockcreate.o: ndn_sockcreate.c ../include/ndn/sockcreate.h
ndn_traverse.o: ndn_traverse.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndn_uri.o: ndn_uri.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndn_verifysig.o: ndn_verifysig.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/keystore.h \
  ../include/ndn/signing.h
ndn_versioning.o: ndn_versioning.c ../include/ndn/bloom.h \
  ../include/ndn/ndn.h ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h \
  ../include/ndn/ndn_private.h
ndn_header.o: ndn_header.c ../include/ndn/ndn.h ../include/ndn/coding.h \
  ../include/ndn/charbuf.h ../include/ndn/indexbuf.h \
  ../include/ndn/header.h
ndn_fetch.o: ndn_fetch.c ../include/ndn/fetch.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/uri.h
lned.o: lned.c ../include/ndn/lned.h
encodedecodetest.o: encodedecodetest.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/bloom.h ../include/ndn/uri.h \
  ../include/ndn/digest.h ../include/ndn/keystore.h \
  ../include/ndn/signing.h ../include/ndn/random.h
hashtb.o: hashtb.c ../include/ndn/hashtb.h
hashtbtest.o: hashtbtest.c ../include/ndn/hashtb.h
signbenchtest.o: signbenchtest.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/keystore.h
skel_decode_test.o: skel_decode_test.c ../include/ndn/charbuf.h \
  ../include/ndn/coding.h
basicparsetest.o: basicparsetest.c ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/charbuf.h \
  ../include/ndn/indexbuf.h ../include/ndn/face_mgmt.h \
  ../include/ndn/sockcreate.h ../include/ndn/reg_mgmt.h \
  ../include/ndn/header.h
ndnbtreetest.o: ndnbtreetest.c ../include/ndn/btree.h \
  ../include/ndn/charbuf.h ../include/ndn/hashtb.h \
  ../include/ndn/btree_content.h ../include/ndn/ndn.h \
  ../include/ndn/coding.h ../include/ndn/indexbuf.h ../include/ndn/uri.h
ndn_sockaddrutil.o: ndn_sockaddrutil.c ../include/ndn/charbuf.h \
  ../include/ndn/sockaddrutil.h
ndn_setup_sockaddr_un.o: ndn_setup_sockaddr_un.c ../include/ndn/ndnd.h \
  ../include/ndn/ndn_private.h ../include/ndn/charbuf.h
