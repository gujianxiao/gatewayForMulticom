# apps/examples/scripts/neighborhood.sh
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

# This is an example of discovery of neighboring NDNx nodes.
# It works by local broadcast.

# To run this script, use sh:
# sh apps/examples/scripts/neighborhood.sh

# The all-ones IP broadcast address is used by default.
# An alternative would be a link-local multicast address, but that
# depends on the neighbors having joined the group.
ADDR=${1:-255.255.255.255}

# The first step is to add a FIB entry to direct the discovery interests to
# the broadcast address.  This has a short timeout so that the prefix
# goes away in a little while.
ndndc -t 3 add ndn:/%C1.M.S.neighborhood/%C1.M.SRV/ndnd/KEY udp $ADDR

# Now use ndnls to do the discovery work for us.
# The scope restriction will keep our interests from getting forwarded
# farther than the nodes that can hear us directly.
NDN_SCOPE=2 ndnls ndn:/%C1.M.S.neighborhood/%C1.M.SRV/ndnd/KEY

# Let's destroy this face now, so that it will not cause confusion.
ndndc destroy udp $ADDR

# Show the status, which has the IP addresses of our new-found neighbors.
ndndstatus

# Save the content objects that have been collected in the cache.
# This also makes them stale, which reduces the possibility that they
# might be served up to somebody else.
ndnrm -o neighborhood.ndnb ndn:/%C1.M.S.neighborhood/%C1.M.SRV/ndnd/KEY

# Show the names of the keys we have collected.
ndnnamelist neighborhood.ndnb

# BUGS
# This script is intended as a basic demonstration of discovery of
# local peers using broadcast.  It has a number of problems:
# - We may end up destroying a broadcast face that we did not create.
# - The association between peer keys and IP addresses is not recorded.
# - If NDND_AUTOREG is in use on the peers, they will start forwarding
#   interests to us.  That may not be our intent.
# - If NDND_AUTOREG is in use our our node, we will start forwarding
#   interests to our neighbors.  This might not be our intention, either.
# - Since the neighbors won't realize they have received a broadcast,
#   their answer will be unicast, and will not have the approriate
#   randomized delays.
