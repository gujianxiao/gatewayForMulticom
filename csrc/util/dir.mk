# util/dir.mk
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

SCRIPTSRC = shebang \
	ndndstart.sh ndndstop.sh ndndstatus.sh ndndlogging.sh ndnget.sh ndnput.sh \
	ndntestloop-trampoline \
	ndnd-publish-local-info.sh ndnrpolicyedit.sh

PROGRAMS = ndndstart ndndstop ndndstatus ndntestloop ndndlogging ndnget ndnput \
	 ndnd-publish-local-info ndnrpolicyedit

OTHER_SCRIPTS = ndn-name-dnsifier.py ndnd-autoconfig ndn-install-pubcert \
	ndn-extract-public-key ndn-sign-key

INSTALLED_PROGRAMS = $(PROGRAMS) $(OTHER_SCRIPTS)

default all: $(SCRIPTSRC) $(PROGRAMS)

# ndnd-autoconfig requires bash (uses #!/usr/bin/env bash)
ndndstart ndndstop ndndstatus ndndlogging ndnget ndnput ndnd-publish-local-info: $(SCRIPTSRC) shebang
	./shebang $(SH) $(@:=.sh) > $@
	chmod +x $@

ndntestloop: ndntestloop-trampoline shebang
	./shebang $(SH) ndntestloop-trampoline > $@
	chmod +x $@

clean:
	rm -f $(PROGRAMS) depend

test:
	@echo "Sorry, no util unit tests at this time"
