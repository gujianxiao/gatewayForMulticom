/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.impl.encoding;

/**
 * Uses the NDNProtocolDTags enum type to implement a dictionary.
 */
public class NDNProtocolDictionary implements  XMLDictionary {
	
	private static NDNProtocolDictionary _defaultInstance = new NDNProtocolDictionary();
	
	public static NDNProtocolDictionary getDefaultInstance() { return _defaultInstance; }
	
	/**
	 * Use getDefaultInstance()
	 */
	private NDNProtocolDictionary() {}

	public Long stringToTag(String tag) {
		Long tagVal = null;
		try {
			tagVal = NDNProtocolDTags.stringToTag(tag);
			if (null != tagVal) {
				return tagVal;
			}
		} catch (IllegalArgumentException e) {
			// do nothing
		} 
		return null; // no tag with that name
	}

	/**
	 * This is the slow way, but we should only have to do this if printing things
	 * out as text...
	 */
	public String tagToString(long tagVal) {
		return NDNProtocolDTags.tagToString(tagVal);
	}
}
