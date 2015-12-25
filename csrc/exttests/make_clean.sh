# exttests/make_clean.sh
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2011-2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#
echo Running $0 >&2
cd `dirname $0` || exit 1
grep '[c]leaning' make_clean.sh >&2 || exit 1
# cleaning test debris
# Please keep preserve+ignore and .gitignore in sync - this is a reminder
test -f .gitignore && { cat preserve ignore | diff -u - .gitignore || echo ''; }
eval rm -R -f `cat ignore`
