/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.ndnx.ndn;


/**
 * Deprecated - use NDNInterestListener instead
 * 
 * A NDN filter is essentially an "interest in Interests" -- it allows a caller to register
 * to see Interest messages that come in from the network, and optionally generate (or
 * merely provide) data in response. Filters are registered using 
 * NDNBase#registerFilter(ContentName, NDNFilterListener). Note that we will only see
 * interests that match the name we registered in our filter -- in other words, Interests
 * in that name or its children; not its parents. We will also only see Interests that
 * were not already satisfied out of ndnd's own cache (or on the network path to our node).
 * 
 * @see NDNBase
 * @see NDNHandle
 */
@Deprecated
public interface NDNFilterListener extends NDNInterestHandler {

	/**
	 * Callback called when we get a new interest matching our filter.
	 * @param interests The matching interest
	 * @return true if this handler has consumed the interest 
	 * 	(that is the handler returned data satisfying the interest).
	 */
    //public boolean handleInterest(Interest interest);
}
