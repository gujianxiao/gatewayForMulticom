# csrc/Makefile
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009, 2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

SUBDIRS = lib ndnd sync ndnr libexec cmd util tests exttests
OBJTREE = build
MAKEF =  -f ../conf.mk -f dir.mk -f ../subr.mk
CFLAGS = $(COPT)

default:

conf.mk:
	./configure

include conf.mk

default all clean depend coverage test check shared install uninstall config_subdir: conf.mk subr.mk generic.mk _always
	test -d include/ndn || (test -d ../include/ndn && mkdir -p include && ln -s ../../include/ndn include/ndn)
	for i in $(SUBDIRS); do         \
	  (cd "$$i" && pwd && $(MAKE) $(MAKEF) "COPT=$(CFLAGS)" CFLAGS='$$(REAL_CFLAGS)' SRCDIR=../$(SRCDIR)/$$i $@) || exit 1;	\
	done
	@test -d $(OBJTREE) && echo "+++ NOTE - $(OBJTREE) exists" ||:
	@rm -f _always

subr.mk generic.mk:
	test -f ./$(SRCDIR)/$@ && ln -s ./$(SRCDIR)/$@

test check: default

objtree: conf.mk _always
	mkdir -p $(OBJTREE)
	echo SRCDIR = ..  > $(OBJTREE)/Makefile
	cat Makefile     >> $(OBJTREE)/Makefile
	for i in $(SUBDIRS); do						\
	  mkdir -p $(OBJTREE) &&					\
	    (cd $$i && pwd &&						\
		$(MAKE) $(MAKEF) OBJDIR=../$(OBJTREE)/$$i SRCDIR=. 	\
			../$(OBJTREE)/$$i/dir.mk	 		\
	     || exit 1);						\
	done
	cd $(OBJTREE) && ../configure

html: _always

documentation: _always
	doxygen

dist-docs: _always
	doxygen Doxyfile.dist

_always:

.PHONY: _always
