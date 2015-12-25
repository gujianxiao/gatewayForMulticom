/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2013 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation. 
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

package org.ndnx.ndn.io.content;

import java.io.Serializable;
import java.security.PublicKey;

import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.io.content.ContentGoneException;
import org.ndnx.ndn.io.content.ContentNotReadyException;
import org.ndnx.ndn.io.content.SerializableObject;


/**
 * Helper class testing low-level (non-NDN) serializable object functionality.
 */
public class SerializablePublicKey extends SerializableObject<PublicKey> implements Serializable {
	
	private static final long serialVersionUID = 1235874939485391189L;

	public SerializablePublicKey() {
		super(PublicKey.class, false);
	}
	
	public SerializablePublicKey(PublicKey publicKey) {
		super(PublicKey.class, false, publicKey);
	}
	
	public PublicKey publicKey() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }

}
