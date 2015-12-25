# util/ndnrpolicyedit.sh
# 
# Part of the NDNx distribution.
#
# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2013 Palo Alto Research Center, Inc.
#
# This work is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License version 2 as published by the
# Free Software Foundation.
# This work is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.
#

D=`dirname "$0"`
export PATH="$D:$PATH"
Fail () {
  echo Failed - $* >&2
  exit 1
}
. $HOME/.ndnx/ndndrc
[ -z "$NDNR_GLOBAL_PREFIX" ] && \
  Fail NDNR_GLOBAL_PREFIX is not set in $HOME/.ndnx/ndndrc
X=`mktemp ${TMPDIR:-/tmp}/policyXXXXXX`
if ! type xmllint 2>/dev/null >/dev/null; then
  xmllint () {
    cat
  }
fi
trap "rm -f $X $X.ndnb" EXIT
ndncat $NDNR_GLOBAL_PREFIX/data/policy.xml | \
  ndn_ndnbtoxml -xv - | xmllint --format - > $X
${EDITOR:-vi} $X || Fail edit aborted
ndn_xmltondnb -w - < $X > $X.ndnb || Fail Malformed XML
ndnseqwriter -r $NDNR_GLOBAL_PREFIX/data/policy.xml < $X.ndnb
