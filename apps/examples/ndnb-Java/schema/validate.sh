#!/bin/sh
# schema/validate.sh

# Portions Copyright (C) 2013 Regents of the University of California.
# 
# Based on the CCNx C Library by PARC.
# Copyright (C) 2011 Palo Alto Research Center, Inc.
# 
# This work is free software; you can redistribute it and/or modify it under
#  the terms of the GNU General Public License version 2 as published by the
#  Free Software Foundation.
#  This work is distributed in the hope that it will be useful, but WITHOUT ANY
#  WARRANTY; without even the implied warranty of MERCHANTABILITY or
#  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
#  for more details. You should have received a copy of the GNU General Public
#  License along with this program; if not, write to the
# Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
#  Boston, MA 02110-1301, USA.

SCHEMA=example.xsd
XML_EXAMPLES="2-integers-test01 complicated-test01 complicated-test02"

set -e
NDNxDIR=`dirname $0`/../../../../
echo == Make sure NDNx directories have been prepared
test -x $NDNxDIR/bin/ndn_xmltondnb || exit 1
export PATH=$NDNxDIR/bin:$PATH
test -f $NDNxDIR/schema/validation/XMLSchema.xsd || (cd $NDNxDIR/schema/validation && make test)
echo == Creating symlinks to access external schemata
EXTSCHEMA=`(cd $NDNxDIR/schema/validation && echo *.xsd)`
for x in $EXTSCHEMA; do
    test -f $NDNxDIR/schema/validation/$x && \
      rm -f $x                            && \
      ln -s $NDNxDIR/schema/validation/$x
done

echo == Validating $SCHEMA
xmllint --schema XMLSchema.xsd --noout $SCHEMA

ValidateXML () {
local X
X="$1"
echo == Normalizing ${X}.xml to use base64Binary
# Note for this purpose it does not matter that ndn_ndnbtoxml is ignorant of
#  the project-specific DTAG values, since we're not trying to do anything
#  with the intermediate ndnb except to turn it right back into text.
cat ${X}.xml | ndn_xmltondnb -w - | ndn_ndnbtoxml -b - | xmllint --format - > ${X}-base64.xml
echo == Validating ${X}
xmllint --schema $SCHEMA --noout ${X}-base64.xml
}
for i in $XML_EXAMPLES; do
    ValidateXML $i
done

echo == Yippee\!
