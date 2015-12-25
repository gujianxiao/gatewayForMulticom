# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2009-2013 Palo Alto Research Center, Inc.
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
############
# User-settable things

APK_NAME = NDNx-Android-Services-debug.apk
BIN = bin
GEN = gen
GENERATED_SOURCE_FILES = obj

############
# Nothing tweekable down here

.PHONY: all ndnd environment prereq clean simpleclean distclean checkndnxdeps
.SUFFIXES: .jar .properties .xml

##########
# This is a lit of the targets in our libs directory
JARS = libs/ndn.jar

TARGET = $(BIN)/$(APK_NAME)

default all: $(TARGET) 

with-bcp: clean bcprov default

bcprov:
	./download.sh libs http://repo2.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.43 bcprov-jdk16-1.43.jar

$(TARGET): $(JARS) ndnd local.properties project.properties build.xml
	ant debug

local.properties project.properties:
	@mkdir -p libs
	$(ANDROID_SDK)/tools/android update project --name NDNx-Android-Services -p . -t android-15 --library ../NDNx-Android-Lib/

# Need to revisit this for newer NDK.
# Compensate for the lack of proper dependencies by removing any ndnx object
# files that are older than the newest ndnx header file.  The android build
# seems to deal OK with changes to the .c files themselves.
#
# The NDK does not know about bin_ant, so it is always under bin
NDNXOBJDIRS = bin/ndk/local/armeabi/objs/ndnd

checkndnxdeps:
	touch -r `find ../../csrc -name \*.h | xargs ls -t | head -n 1` ndnxheader.date
	-if [ -d bin/ndk/local/armeabi/objs ]; then \
		find $(NDNXOBJDIRS) \! -newer ndnxheader.date -name \*.o -print -exec rm {} \; ; fi
	rm ndnxheader.date

ndnd: environment checkndnxdeps
	@echo "Building NDND"
	mkdir -p libs
	if test -n "$$APP_ABI"; then \
			$(ANDROID_NDK)/ndk-build APP_ABI="$$APP_ABI" ; \
	else \
				$(ANDROID_NDK)/ndk-build ; \
	fi

checkndnjar:
	@test -f ../../javasrc/ndn.jar || (echo Missing ndn.jar.  Please make NDNx javasrc before the Android port; \
	exit 1;)

libs/ndn.jar: checkndnjar
	mkdir -p $(dir $@)
	rm -f $@

######################################
# Maintenance targets
#
install:
	adb install -r $(BIN)/$(APK_NAME)

uninstall:
	adb uninstall org.ndnx.android.services

environment:
	@if test "$(ANDROID_SDK)" = "" ; then \
		echo "Please set ANDROID_SDK path to point to an r16 or later" && exit 1; \
	fi
	@if test "$(ANDROID_NDK)" = "" ; then \
		echo "Please set ANDROID_NDK path to point to an r7 or later" && exit 1; \
	fi

clean: simpleclean objclean

# Does not remove external NDK object files
# But does remove project OBJ files 
simpleclean:
	rm -rf $(BIN) $(GEN)
	rm -rf libs/*
	rm -f project.properties
	rm -f local.properties

objclean:
	rm -f obj/local/**/*/controller/*.o
	rm -f obj/local/**/*/ndnr/*.o
	rm -f obj/local/**/*/ndnd/*.o
	rm -f obj/local/**/*/ndnx/*.o
	rm -f obj/local/**/*/sync/*.o

distclean: environment simpleclean
	rm -rf $(GENERATED_SOURCE_FILES)

test:
	@echo "No automated tests for services"
