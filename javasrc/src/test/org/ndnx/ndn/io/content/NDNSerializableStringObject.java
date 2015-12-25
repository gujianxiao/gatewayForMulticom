/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.io.content;

import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.io.content.NDNSerializableObject;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentGoneException;
import org.ndnx.ndn.io.content.ContentNotReadyException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * A NDNNetworkObject wrapper around Java Strings, which uses Java serialization
 * to write those strings. Allows reading and writing of
 * versioned strings to NDN, and background updating of same. Very useful class
 * for writing simple tests and applications, but requires both communicating
 * partners to speak Java Serialization. See NDNStringObject for a more generally
 * useful string object that serializes the string in pure UTF-8, making
 * something that can be more easily read from other languages.
 */
public class NDNSerializableStringObject extends NDNSerializableObject<String> {
	
	public NDNSerializableStringObject(ContentName name, String data, SaveType saveType, NDNHandle handle) 
	throws IOException {
		super(String.class, false, name, data, saveType, handle);
	}

	public NDNSerializableStringObject(ContentName name, String data, SaveType saveType, PublisherPublicKeyDigest publisher, 
			KeyLocator locator, NDNHandle handle) throws IOException {
		super(String.class, false, name, data, saveType, publisher, locator, handle);
	}

	public NDNSerializableStringObject(ContentName name, NDNHandle handle) 
	throws ContentDecodingException, IOException {
		super(String.class, false, name, (PublisherPublicKeyDigest)null, handle);
	}

	public NDNSerializableStringObject(ContentName name, PublisherPublicKeyDigest publisher,
			NDNHandle handle) 
	throws ContentDecodingException, IOException {
		super(String.class, false, name, publisher, handle);
	}

	public NDNSerializableStringObject(ContentObject firstBlock, NDNHandle handle) 
	throws ContentDecodingException, IOException {
		super(String.class, false, firstBlock, handle);
	}

	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public NDNSerializableStringObject(ContentName name, String data, 
			PublisherPublicKeyDigest publisher, 
			KeyLocator locator,
			NDNFlowControl flowControl) throws IOException {
		super(String.class, false, name, data, publisher, locator, flowControl);
	}

	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public NDNSerializableStringObject(ContentName name, PublisherPublicKeyDigest publisher,
			NDNFlowControl flowControl) throws ContentDecodingException, IOException {
		super(String.class, false, name, publisher, flowControl);
	}

	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public NDNSerializableStringObject(ContentObject firstSegment, NDNFlowControl flowControl) 
	throws ContentDecodingException, IOException {
		super(String.class, false, firstSegment, flowControl);
	}

	public String string() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }
}
