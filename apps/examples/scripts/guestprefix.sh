# apps/examples/scripts/guestprefix.sh
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

# This is an example script that demonstrates how a node that is acting as
# a local hub can provide to local clients a guest prefix that can be routed
# to that client.
#
# In almost all cases, some local customization will be needed.
#
# To use: sh apps/examples/scripts/guestprefix.sh run
#

# This should be edited to contain a prefix that will get routed to
# our hub from everywhere.
PREFIX_STEM=ndn:/xyzzy/example-site

# This is the prefix that we will use to publish the guest prefixes under.
# Since each guest gets a unique prefix, we publish them under distinct names.
# The IP address is incorporated into this distinctive part, since it is a
# piece of infomation shared by the guest node and the hub node.  Note that
# the IP address is not in the guest prefix itself, only in the name used to
# convey it to the guest.
LP=ndn:/local/prefix

# for logging
SELF=$0

# put our start time in the generated names
GEN=`date +%s`

# We will need our NDNDID
# This is one way to get it, complete with %-escapes for use in a URI
ndndid () {
    ndndstatus | sed -n 's@^ ndn:/ndnx/\(.................................*\) face: 0 .*$@\1@p'
}

NDNDID=`ndndid`

if [ "$NDNDID" = "" ]; then
    echo Unable to get NDNDID >&2
fi

# This is called when a new ipv4 face is made.
# This may be caused by traffic over udp, a configured prefix, or
# an incoming tcp connection that carries ndnx traffic.
incoming_ip4 () { # PROTO IP PORT
    # FLAGS and FACE are alredy set by caller
    PROTO=$1
    IP=$2
    PORT=$3
    case $IP in
        0.0.0.0) ;;   # Ignore wildcard
        127.0.0.1) ;; # Ignore localhost
        255.255.255.255) ;; # Ignore broadcast
        *.*.*.*)    # This pattern may be more specific, e.g. the local subnet
            GUEST_PREFIX=$PREFIX_STEM/$GEN/guest$FACE
            ndndc add $GUEST_PREFIX $PROTO $IP $PORT
            echo $GUEST_PREFIX | ndnseqwriter -x 5 -r $LP/ip~$IP
            ;;
        *)  logger -i -s -t $SELF -- I do not recognize $IP;;
    esac
}

# Used for splitting the host from the port.
# IPv6 numeric hosts may contain colons, so split at the last one.
split_last_colon () {
    echo $1 | sed -e 's/:\([^:]*\)$/ \1/'
}

# Called with each face status event.
process_event () { # FUNC FACE FLAGS REMOTE;
    FUNC=$1
    FACE=$2
    FLAGS=$3
    REMOTE=$4
    #echo FUNC=$FUNC FACE=$FACE FLAGS=$FLAGS REMOTE=$REMOTE
    case $FUNC+$FLAGS in
        newface+0x*10) incoming_ip4 tcp `split_last_colon $REMOTE`;;
        newface+0x*12) incoming_ip4 udp `split_last_colon $REMOTE`;;
    esac
}

# Reads the stream of events and calls process_event for each
process_events () {
  while read LINE; do
    echo =============== $LINE >&2
    echo $LINE | tr '(,);' '    ' | { read FUNC FACE FLAGS REMOTE ETC;
        process_event $FUNC $FACE $FLAGS $REMOTE; }
    done
}

init_temporary_repo () {
    export NDNR_DIRECTORY=/tmp/gp.$$.dir
    pfx=/local/$NDNDID
    mkdir -p $NDNR_DIRECTORY || exit 1
    echo NDNR_DIRECTORY=$NDNR_DIRECTORY     >> $NDNR_DIRECTORY/config
    echo NDNS_ENABLE=0                      >> $NDNR_DIRECTORY/config
    echo NDNR_START_WRITE_SCOPE_LIMIT=1     >> $NDNR_DIRECTORY/config
    echo NDNR_GLOBAL_PREFIX=$pfx            >> $NDNR_DIRECTORY/config
    ndnr 2>>$NDNR_DIRECTORY/log &
}

guestprefix_run () {
    ndnpeek -c ndn:/ndnx/$NDNDID/notice.txt > /dev/null
    ndnsimplecat ndn:/ndnx/$NDNDID/notice.txt | process_events
}

# Only run if asked, so that this script may be sourced in a wrapper script
if [ "$1" = "run" ]; then
    init_temporary_repo
    guestprefix_run
fi
