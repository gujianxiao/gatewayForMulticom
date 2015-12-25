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

import org.ndnx.ndn.impl.encoding.XMLEncodable;


/**
 * Subclass of NetworkObject that wraps classes implementing XMLEncodable, and uses
 * XMLEncodable's encode() and decode() methods to read and write those objects to 
 * an OutputStream. Not NDN-specific, the NDN variant of this is NDNEncodableObject.
 */
public class EncodableObject<E extends XMLEncodable> extends NetworkObject<E> {
		
	public EncodableObject(Class<E> type, boolean contentIsMutable) {
		super(type, contentIsMutable);
	}
	
	public EncodableObject(Class<E> type, boolean contentIsMutable, E data) {
		super(type, contentIsMutable, data);
	}
	
	protected void writeObjectImpl(OutputStream output) throws ContentEncodingException, IOException {
		_data.encode(output);
	}

	protected E readObjectImpl(InputStream input) throws ContentDecodingException, IOException {
		E newData = factory();
		newData.decode(input);
		return newData;
	}
}
