/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2012 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.protocol;

import java.util.Arrays;

import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;


/**
 * This represents a Component with an Exclude filter
 */
public class ExcludeComponent extends Exclude.Element implements Comparable<ExcludeComponent>, ContentName.ComponentProvider {

	protected byte [] body = null;
	
	public ExcludeComponent(byte [] component) {
		body = component.clone();
	}

	public ExcludeComponent() {
	}
	
	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		body = decoder.readBinaryElement(getElementLabel());
	}

	@Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		encoder.writeElement(getElementLabel(), body);
	}
	
	public int compareTo(ExcludeComponent component) {
		return DataUtils.compare(body, component.body);
	}

	public int compareTo(byte [] component) {
		return DataUtils.compare(body, component);
	}

	@Override
	public long getElementLabel() { return NDNProtocolDTags.Component; }

	@Override
	public boolean validate() {
		return body != null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof ExcludeComponent))
			return false;
		ExcludeComponent ec = (ExcludeComponent) obj;
		return DataUtils.arrayEquals(body, ec.body);
	}
	
	public int hashCode() {
		return (body == null) ? 0 : Arrays.hashCode(body);
	}

	public byte [] getBytes() {
		return body.clone();
	}

	public byte[] getComponent() {
		return body;
	}
}
