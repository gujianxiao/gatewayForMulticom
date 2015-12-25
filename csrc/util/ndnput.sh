# Source file: util/ndnget.sh
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2012 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#
# This script should be installed in the same place as ndnpeek, ndnpoke, ...
# adjust the path to get consistency.
D=`dirname "$0"`
export PATH="$D:$PATH"
echo ndnput is a deprecated command name.  The new name is ndnpoke. 1>&2
ndnpoke "$@"
