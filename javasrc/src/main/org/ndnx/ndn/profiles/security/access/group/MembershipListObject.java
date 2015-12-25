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

package org.ndnx.ndn.profiles.security.access.group;

import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.io.content.Collection;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentGoneException;
import org.ndnx.ndn.io.content.ContentNotReadyException;
import org.ndnx.ndn.io.content.Link;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * This class records the membership list of a Group, which can consist of
 * individual users or other groups). This is sometimes redundant with other
 * representations of the membership of a Group or association; it would be
 * good in future work to make explicit membership lists optional (TODO).
 * 
 * Might want to define its own tag for encoding; right now it encodes as a straight
 * Collection.
 */
public class MembershipListObject extends Collection.CollectionObject {

	/**
	 * Write constructors. Prepare to save object.
	 * @param name
	 * @param data
	 * @param saveType
	 * @param handle
	 * @throws IOException
	 */
	public MembershipListObject(ContentName name, Collection data, SaveType saveType, NDNHandle handle) 
			throws IOException {
		super(name, data, saveType, handle);
	}
	
	public MembershipListObject(ContentName name, Collection data, SaveType saveType,
			PublisherPublicKeyDigest publisher, KeyLocator keyLocator,
			NDNHandle handle) throws IOException {
		super(name, data, saveType, publisher, keyLocator, handle);
	}

	public MembershipListObject(ContentName name, java.util.Collection<Link> data,
			SaveType saveType, NDNHandle handle) throws IOException {
		super(name, data, saveType, handle);
	}

	public MembershipListObject(ContentName name, java.util.Collection<Link> data,
			SaveType saveType, PublisherPublicKeyDigest publisher,
			KeyLocator keyLocator, NDNHandle handle) throws IOException {
		super(name, data, saveType, publisher, keyLocator, handle);
	}

	public MembershipListObject(ContentName name, Link[] contents, SaveType saveType,
			NDNHandle handle) throws IOException {
		super(name, contents, saveType, handle);
	}

	public MembershipListObject(ContentName name, Link[] contents, SaveType saveType,
			PublisherPublicKeyDigest publisher, KeyLocator keyLocator,
			NDNHandle handle) throws IOException {
		super(name, contents, saveType, publisher, keyLocator, handle);
	}

	public MembershipListObject(ContentName name, Collection data, 
			PublisherPublicKeyDigest publisher, 
			KeyLocator keyLocator, NDNFlowControl flowControl) throws IOException {
		super(name, data, publisher, keyLocator, flowControl);
	}

	/**
	 * Read constructor -- opens existing object.
	 * @param name
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public MembershipListObject(ContentName name, NDNHandle handle) throws ContentDecodingException, IOException {
		super(name, (PublisherPublicKeyDigest)null, handle);
	}

	public MembershipListObject(ContentName name, PublisherPublicKeyDigest publisher,
			NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(name, publisher, handle);
	}
	
	
	public MembershipListObject(ContentObject firstBlock, NDNHandle handle) 
				throws ContentDecodingException, IOException {
		super(firstBlock, handle);
	}
	
	public MembershipListObject(ContentName name,
			PublisherPublicKeyDigest publisher, NDNFlowControl flowControl)
	throws ContentDecodingException, IOException {
		super(name, publisher, flowControl);
	}

	public MembershipListObject(ContentObject firstBlock,
			NDNFlowControl flowControl) 
	throws ContentDecodingException, IOException {
		super(firstBlock, flowControl);
	}

	/**
	 * Returns the membership list as a collection.
	 * @return
	 * @throws ContentNotReadyException
	 * @throws ContentGoneException
	 * @throws ErrorStateException 
	 */
	public Collection membershipList() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }

}
