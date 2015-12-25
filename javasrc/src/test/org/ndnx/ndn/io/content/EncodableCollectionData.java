/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2012, 2013 Palo Alto Research Center, Inc.
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

import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.io.content.Collection;
import org.ndnx.ndn.io.content.ContentGoneException;
import org.ndnx.ndn.io.content.ContentNotReadyException;
import org.ndnx.ndn.io.content.EncodableObject;

/**
 * Helper class testing low-level (non-NDN) encodable object functionality.
 */
public class EncodableCollectionData extends EncodableObject<Collection> {

	public EncodableCollectionData() {
		super(Collection.class, true);
	}
	
	public EncodableCollectionData(Collection collectionData) {
		super(Collection.class, true, collectionData);
	}
	
	public Collection collection() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }
}
