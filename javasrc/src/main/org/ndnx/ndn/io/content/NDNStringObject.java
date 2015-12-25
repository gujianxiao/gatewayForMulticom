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

package org.ndnx.ndn.io.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.io.ErrorStateException;
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
public class NDNStringObject extends NDNNetworkObject<String> {

	/**
	 * Write constructor. Use this
	 * 	constructor with null data to avoid an initial blocking call to
	 * update in the constructor, for example if you are going to call updateInBackground.
	 * @param name
	 * @param data Initial value for data. Can be null; but needs to be set with
	 * 	setData before save is called (or passed as an argument to save). 
	 * @param saveType where this object saves its data to; options include
	 * 	RAW and REPOSITORY
	 * @param handle
	 * @throws IOException
	 */
	public NDNStringObject(ContentName name, String data, SaveType saveType, NDNHandle handle) 
				throws IOException {
		super(String.class, false, name, data, saveType, handle);
	}
	
	/**
	 * Write constructor.
	 * @param name
	 * @param data
	 * @param saveType where this object saves its data to; options include
	 * 	RAW and REPOSITORY
	 * @param publisher
	 * @param locator
	 * @param handle
	 * @throws IOException
	 */
	public NDNStringObject(ContentName name, String data, SaveType saveType, PublisherPublicKeyDigest publisher, 
							KeyLocator locator, NDNHandle handle) throws IOException {
		super(String.class, false, name, data, saveType, publisher, locator, handle);
	}

	/**
	 * Read constructor.
	 * @param name
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public NDNStringObject(ContentName name, NDNHandle handle) 
				throws ContentDecodingException, IOException {
		super(String.class, false, name, (PublisherPublicKeyDigest)null, handle);
	}
	
	/**
	 * Read constructor.
	 * @param name
	 * @param publisher
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public NDNStringObject(ContentName name, PublisherPublicKeyDigest publisher,
							NDNHandle handle) 
				throws ContentDecodingException, IOException {
		super(String.class, false, name, publisher, handle);
	}
	
	/**
	 * Read constructor.
	 * @param firstBlock
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public NDNStringObject(ContentObject firstBlock, NDNHandle handle) 
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
	public NDNStringObject(ContentName name, String data, 
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
	public NDNStringObject(ContentName name, PublisherPublicKeyDigest publisher,
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
	public NDNStringObject(ContentObject firstSegment, NDNFlowControl flowControl) 
					throws ContentDecodingException, IOException {
		super(String.class, false, firstSegment, flowControl);
	}

	@Override
	protected String readObjectImpl(InputStream input) throws ContentDecodingException, IOException {
		// assume we read until we have all the bytes, then decode.
		byte [] contentBytes = DataUtils.getBytesFromStream(input);
		// do something if contentBytes is null?
		return DataUtils.getUTF8StringFromBytes(contentBytes);
	}

	@Override
	protected void writeObjectImpl(OutputStream output) throws ContentEncodingException, IOException {
		if (null == data())
			throw new ContentNotReadyException("No content available to save for object " + getBaseName());
		byte [] stringData = DataUtils.getBytesFromUTF8String(data());
		output.write(stringData);
	}

	public String string() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }
}
