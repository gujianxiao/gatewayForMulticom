# doc/technical/Makefile
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

# The order here determines the order in the generated index.
DERIVED_HTML =				\
	NDNxProtocol.html		\
	Name.html			\
	URI.html			\
	ContentObject.html		\
	CryptographicAlgorithms.html	\
	InterestMessage.html		\
	CanonicalOrder.html		\
	BinaryEncoding.html		\
	NameConventions.html		\
	Timestamp.html			\
	DigestComponent.html		\
	Registration.html		\
	StatusResponse.html		\
	SignatureGeneration.html	\
	Staleness.html			\
	NameEnumerationProtocol.html	\
	RepoPolicies.html		\
	RepoProtocol.html		\
	RepositoryInfoObject.html	\
	SynchronizationProtocol.html	\
	CreateCollectionProtocol.html	\
	LinkMessages.html		\
	dtd.html			\
	xsd.html			\
	repository.dtd.html			\
	repository.xsd.html			\
	sync.dtd.html			\
	sync.xsd.html			\
	DTAG.html			\
	NDNDStatus.html			\

DERIVED_INDEX = ../index.html index.html index.txt
DEBRIS = .tagname.csv
SRC_HTML = # any checked-in html files should go here
INDEXED_HTML = $(DERIVED_HTML) $(SRC_HTML)
SCHEMA = ../../schema
XSDSRC = $(SCHEMA)/ndnx.xsd $(SCHEMA)/repository.xsd $(SCHEMA)/security.xsd
DTDSRC = $(SCHEMA)/ndnx.dtd $(SCHEMA)/repository.dtd $(SCHEMA)/security.dtd
all default:
	: To build the documentation: $(MAKE) documentation

documentation dist-docs html: $(DERIVED_HTML) $(DERIVED_INDEX)


clean-documentation: _always
	rm -f $(DERIVED_HTML) $(DERIVED_INDEX) $(DEBRIS)

clean:
	: To clean the documentation: $(MAKE) clean-documentation 
	rm -f $(DEBRIS)

DigestComponent.html: DigestComponent.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  DigestComponent.txt

DTAG.html: DTAG.txt $(SCHEMA)/tagname.csvdict
	sed -e 's/$$/,,,,/g' $(SCHEMA)/tagname.csvdict | \
					cut -d , -f 1-3   > .tagname.csv
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe DTAG.txt || : might fail if your asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  is too old
	rm -f .tagname.csv

dtd.html: dtd.txt $(XSDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe dtd.txt

xsd.html: xsd.txt $(DTDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe xsd.txt

repository.dtd.html: repository.dtd.txt $(XSDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe repository.dtd.txt

repository.xsd.html: repository.xsd.txt $(DTDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe repository.xsd.txt

sync.dtd.html: sync.dtd.txt $(XSDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe sync.dtd.txt

sync.xsd.html: sync.xsd.txt $(DTDSRC)
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe sync.xsd.txt

CryptographicAlgorithms.html: CryptographicAlgorithms.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  CryptographicAlgorithms.txt

InterestMessage.html: InterestMessage.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  InterestMessage.txt

CanonicalOrder.html: CanonicalOrder.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  CanonicalOrder.txt

NameConventions.html: NameConventions.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  NameConventions.txt

Timestamp.html: Timestamp.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  Timestamp.txt

Registration.html: Registration.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  Registration.txt

StatusResponse.html: StatusResponse.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  StatusResponse.txt

SignatureGeneration.html: SignatureGeneration.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  SignatureGeneration.txt

Staleness.html: Staleness.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  Staleness.txt

URI.html: URI.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  URI.txt

ContentObject.html: ContentObject.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  ContentObject.txt

Name.html: Name.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  Name.txt

BinaryEncoding.html: BinaryEncoding.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  BinaryEncoding.txt

NDNxProtocol.html: NDNxProtocol.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  NDNxProtocol.txt

NameEnumerationProtocol.html: NameEnumerationProtocol.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  NameEnumerationProtocol.txt

RepoPolicies.html: RepoPolicies.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  RepoPolicies.txt	

RepoProtocol.html: RepoProtocol.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  RepoProtocol.txt	

RepositoryInfoObject.html: RepositoryInfoObject.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  RepositoryInfoObject.txt

SynchronizationProtocol.html: SynchronizationProtocol.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  SynchronizationProtocol.txt

CreateCollectionProtocol.html: CreateCollectionProtocol.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  CreateCollectionProtocol.txt

LinkMessages.html: LinkMessages.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  LinkMessages.txt

NDNDStatus.html: NDNDStatus.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  NDNDStatus.txt

../index.html: ../index.txt ../../README ../../LICENSE
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  --unsafe ../index.txt

index.html: index.txt
	asciidoc -a linkcss -a stylesheet=../styles/ndn_asciidoc.css  index.txt

index.txt: $(INDEXED_HTML) .genindex.sh
	rm -f index.html
	sh .genindex.sh $(INDEXED_HTML) > index.txt

default html all clean depend test check shared documentation dist-docs testinstall install uninstall: _always

_always:

.PHONY: _always
