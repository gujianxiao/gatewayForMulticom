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
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * Provides persistence for classes implementing XMLEncodable using a NDN network to store/load
 * the data. This is similar to the Data Access Object pattern.
 * 
 * The data supplier (class implementing XMLEncodable's encode() and decode() methods) is called
 * to read and write those objects to NDN.
 */
public class NDNEncodableObject<E extends XMLEncodable> extends NDNNetworkObject<E> {
	
	public NDNEncodableObject(Class<E> type, boolean contentIsMutable,
							  ContentName name, E data, SaveType saveType,
							  NDNHandle handle) throws IOException {
		super(type, contentIsMutable, name, data, saveType, null, null, handle);
	}
	
	public NDNEncodableObject(Class<E> type, boolean contentIsMutable,
							 ContentName name, E data,
							 SaveType saveType, PublisherPublicKeyDigest publisher, 
							 KeyLocator keyLocator, NDNHandle handle) throws IOException {
		super(type, contentIsMutable, name, data, saveType, publisher, keyLocator, handle);
	}

	protected NDNEncodableObject(Class<E> type, boolean contentIsMutable,
								ContentName name, E data, PublisherPublicKeyDigest publisher,
								KeyLocator keyLocator, NDNFlowControl flowControl) throws IOException {
		super(type, contentIsMutable, name, data, publisher, keyLocator, flowControl);
	}
	
	public NDNEncodableObject(Class<E> type, boolean contentIsMutable, 
							  ContentName name, 
							  NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(type, contentIsMutable, name, (PublisherPublicKeyDigest)null, handle);
	}
	
	public NDNEncodableObject(Class<E> type, boolean contentIsMutable, 
							  ContentName name, PublisherPublicKeyDigest publisher,
							  NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(type, contentIsMutable, name, publisher, handle);
	}

	protected NDNEncodableObject(Class<E> type, boolean contentIsMutable, ContentName name,
								 PublisherPublicKeyDigest publisher, NDNFlowControl flowControl)
			throws ContentDecodingException, IOException {
		super(type, contentIsMutable, name, publisher, flowControl);
	}

	public NDNEncodableObject(Class<E> type, boolean contentIsMutable, 
						      ContentObject firstBlock,
							  NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(type, contentIsMutable, firstBlock, handle);
	}

	protected NDNEncodableObject(Class<E> type, boolean contentIsMutable, 
								ContentObject firstBlock,
								NDNFlowControl flowControl) 
			throws ContentDecodingException, IOException {
		super(type, contentIsMutable, firstBlock, flowControl);
	}

	protected NDNEncodableObject(Class<E> type, NDNEncodableObject<? extends E> other) {
		super(type, other);
	}
	
	@Override
	protected E readObjectImpl(InputStream input) throws ContentDecodingException, IOException {
		E newData = factory();
		newData.decode(input);	
		return newData;
	}

	@Override
	protected void writeObjectImpl(OutputStream output) throws ContentEncodingException, IOException {
		if (null == _data)
			throw new ContentNotReadyException("No content available to save for object " + getBaseName());
		_data.encode(output);
	}
}
