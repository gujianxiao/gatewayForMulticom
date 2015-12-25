# conf/CYGWIN_NT-6.0.mk
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#
MORE_LDLIBS=../contrib/getaddrinfo/getaddrinfo.o
PLATCFLAGS=-DNEED_GETADDRINFO_COMPAT -Wl,--enable-auto-import -I../contrib/getaddrinfo
PCAP_PROGRAMS=
