/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl.security.keys;

import org.ndnx.ndn.TrustManager;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.PublisherID;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * This is a very basic implementation of a TrustManager.
 * It checks whether a content's publisher matches the expected publisher for a consumer.
 */

public class BasicTrustManager extends TrustManager {

	/**Constructor
	 * 
	 */
	public BasicTrustManager() {
	}

	/**
	 * Checks if the publisher is the expected one.
	 */
	@Override
	public boolean matchesRole(PublisherID desiredRole, PublisherPublicKeyDigest thisKey) {
		if (desiredRole.type() != PublisherID.PublisherType.KEY) {
			Log.info("Cannot yet handle trust match for anything more complicated than a KEY!");
			throw new UnsupportedOperationException("Cannot handle trust match for anything more complicated than a KEY yet!");
		}
		if (thisKey.equals(desiredRole)) {
			return true;
		}
		
		return false;
	}

}
