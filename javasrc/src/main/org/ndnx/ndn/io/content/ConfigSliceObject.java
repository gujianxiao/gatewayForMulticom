/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2012 Palo Alto Research Center, Inc.
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

import static org.ndnx.ndn.impl.NDNFlowControl.SaveType.LOCALREPOSITORY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.io.content.ConfigSlice.Filter;
import org.ndnx.ndn.profiles.sync.Sync;
import org.ndnx.ndn.protocol.ContentName;

public class ConfigSliceObject extends NDNNetworkObject<ConfigSlice> {

	/**
	 * Read constructor. Use when you have a slice hash (perhaps from enumeration),
	 * and want to know if it's present or not.
	 * @param hash of slice data.
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public ConfigSliceObject(byte[] hash, NDNHandle handle) throws ContentDecodingException, IOException {
		super(ConfigSlice.class, true, ConfigSliceObject.nameFromHash(hash), handle);
		setSaveType(LOCALREPOSITORY);
	}

	/**
	 * Write constructor.
	 * @param data Used to generate the full object name (which is a hash
	 * of the data).
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public ConfigSliceObject(ConfigSlice data, NDNHandle handle) throws IOException {
		super(ConfigSlice.class, false, nameFromHash(data.getHash()), data, LOCALREPOSITORY, handle);
	}
	
	/**
	 * Convenience write constructor.
	 * Creates an ConfigSlice, calculates the hash and creates a NetworkObject together.
	 */
	public ConfigSliceObject(ContentName topo, ContentName prefix, Collection<Filter> filters, NDNHandle handle) throws IOException {
		this(new ConfigSlice(topo, prefix, filters), handle);
	}
	
	public static ContentName nameFromHash(byte[] hash) {
		return new ContentName(Sync.SYNC_SLICE_PREFIX, hash);
	}

	public ConfigSlice getData() { return _data; }
	
	@Override
	protected void writeObjectImpl(OutputStream output)
			throws ContentEncodingException, IOException {
		if (null == data())
			throw new ContentNotReadyException("No content available to save for object " + getBaseName());
		byte [] data = getData().encode();
		output.write(data);		
	}

	@Override
	protected ConfigSlice readObjectImpl(InputStream input)
			throws ContentDecodingException, IOException {
		byte [] contentBytes = DataUtils.getBytesFromStream(input);
		// do something if contentBytes is null?
		ConfigSlice slice = new ConfigSlice();
		slice.decode(contentBytes);
		return slice;
	}
}
