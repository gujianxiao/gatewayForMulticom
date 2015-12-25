/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2013 Palo Alto Research Center, Inc.
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

import static org.ndnx.ndn.impl.encoding.NDNProtocolDTags.ConfigSlice;
import static org.ndnx.ndn.impl.encoding.NDNProtocolDTags.ConfigSliceList;
import static org.ndnx.ndn.impl.encoding.NDNProtocolDTags.ConfigSliceOp;
import static org.ndnx.ndn.impl.encoding.NDNProtocolDTags.SyncVersion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.security.crypto.NDNDigestHelper;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.profiles.sync.Sync;
import org.ndnx.ndn.protocol.ContentName;

public class ConfigSlice extends GenericXMLEncodable {
	
	public int version = Sync.SLICE_VERSION;
	public ContentName topo;
	public ContentName prefix;

	protected LinkedList<Filter> filters = new LinkedList<Filter>();
	
	/**
	 * Config slice lists require a ConfigSliceOp written before the
	 * ContentName, although it does nothing. This class
	 * encodes and decodes the preceding ConfigSliceOp and the
	 * ContentName together, representing a single filter element.
	 */
	@SuppressWarnings("serial")
	public static class Filter extends ContentName {

		public Filter() {
		}
		
		public Filter(ContentName cn) {
			super(cn);
		}
		
		public Filter(byte[][] arg) {
			super(arg);
		}

		@Override
		public void decode(XMLDecoder decoder) throws ContentDecodingException {
			decoder.readIntegerElement(ConfigSliceOp);
			super.decode(decoder);
		}

		@Override
		public void encode(XMLEncoder encoder) throws ContentEncodingException {
			encoder.writeElement(ConfigSliceOp, 0);
			super.encode(encoder);
		}
	}
	
	public ConfigSlice() {}
	
	public ConfigSlice(ContentName topo, ContentName prefix, Collection<Filter> new_filters) {
		this.topo = topo;
		this.prefix = prefix;
		if (new_filters != null)
			filters.addAll(new_filters);
	}
	
	/**
	 * Check that a sync ConfigSlice exists in the local repository, and if not create one.
	 * @param handle
	 * @param topo from ConfigSlice
	 * @param prefix from ConfigSlice
	 * @param filters from ConfigSlice
	 * @throws IOException
	 */
	public static ConfigSlice checkAndCreate(ContentName topo, ContentName prefix, Collection<Filter> filters, NDNHandle handle) throws ContentDecodingException, IOException {
		ConfigSlice slice = new ConfigSlice(topo, prefix, filters);
		//ConfigSlice.NetworkObject csno = new ConfigSlice.NetworkObject(slice.getHash(), handle);
		ConfigSliceObject csno = new ConfigSliceObject(slice, handle);
		boolean updated = csno.update(SystemConfiguration.SHORT_TIMEOUT);
		if (updated)
			Log.fine(Log.FAC_SYNC, "found this slice in my repo! {0}", csno.getVersionedName());
		else
			Log.fine(Log.FAC_SYNC, "didn't find a slice in my repo.");
		if (!updated || (updated && (!csno.available() || csno.isGone()))) {
			Log.fine(Log.FAC_SYNC, "need to save my data to create the slice for the repo!");
			csno.setData(slice);
			csno.save();
		} else {
			Log.fine(Log.FAC_SYNC, "don't need to do anything...  returning the existing slice");
		}
		csno.close();
		return slice;
	}
	
	public void checkAndCreate(NDNHandle handle) throws ContentDecodingException, ContentEncodingException, IOException{
		ConfigSliceObject existingSlice;
		try {
			//existingSlice = new ConfigSlice.NetworkObject(this.getHash(), handle);
			existingSlice = new ConfigSliceObject(this, handle);
			boolean updated = existingSlice.update(SystemConfiguration.SHORT_TIMEOUT);
			if (!updated || (updated && (!existingSlice.available() || existingSlice.isGone()))) {
				existingSlice.setData(this);
				existingSlice.save();
			}
		} catch (ContentDecodingException e) {
			Log.warning(Log.FAC_REPO, "ContentDecodingException: Unable to read in existing slice data from repository.");
			throw e;
		} catch (IOException e) {
			Log.warning(Log.FAC_REPO, "IOException: error when attempting to retrieve existing slice");
			throw e;
		}
		existingSlice.close();
	}
	
	public boolean deleteSlice(NDNHandle handle) throws IOException{
		
		ConfigSliceObject existingSlice;
		
		try {
			existingSlice = new ConfigSliceObject(this.getHash(), handle);
			return existingSlice.saveAsGone();
		} catch (ContentDecodingException e) {
			Log.warning(Log.FAC_REPO, "ContentDecodingException: Unable to read in existing slice data from repository.");
			throw new IOException("Unable to delete slice from repository: " + e.getMessage());
		} catch (IOException e) {
			Log.warning(Log.FAC_REPO, "IOException: error when attempting to retrieve existing slice before deletion");
			throw new IOException("Unable to delete slice from repository: " + e.getMessage());
		}	
	}
	
	public byte[] getHash() {
		try {
			return NDNDigestHelper.digest(encode());
		} catch (ContentEncodingException e) {
			// should never happen since we're encoding our own data
			throw new RuntimeException(e);
		}
	}

	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		decoder.readStartElement(getElementLabel());
		version = decoder.readIntegerElement(SyncVersion);
		topo = new ContentName();
		topo.decode(decoder);
		prefix = new ContentName();
		prefix.decode(decoder);
		decoder.readStartElement(ConfigSliceList);
		while (decoder.peekStartElement(NDNProtocolDTags.ConfigSliceOp)) {
			Filter f = new Filter();
			f.decode(decoder);
			filters.add(f);
		}
		decoder.readEndElement();
		decoder.readEndElement();
	}

	@Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		encoder.writeStartElement(getElementLabel());
		encoder.writeElement(SyncVersion, version);
		topo.encode(encoder);
		prefix.encode(encoder);
		encoder.writeStartElement(ConfigSliceList);
		for(Filter f : filters)
			f.encode(encoder);
		encoder.writeEndElement();
		encoder.writeEndElement();
	}

	@Override
	public long getElementLabel() {
		return ConfigSlice;
	}

	@Override
	public boolean validate() {
		return true;
	}
	
	public int hashCode() {
		return Arrays.hashCode(getHash());
	}
	
	public boolean equals(Object obj) {
		if (null == obj)
			return false;
		if (! (obj instanceof ConfigSlice))
			return false;
		ConfigSlice otherSlice = (ConfigSlice)obj;
		return Arrays.equals(this.getHash(), otherSlice.getHash());
	}
}
