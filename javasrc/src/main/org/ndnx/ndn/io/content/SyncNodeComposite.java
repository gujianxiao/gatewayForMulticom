/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2012, 2013 Palo Alto Research Center, Inc.
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

import static org.ndnx.ndn.impl.encoding.NDNProtocolDTags.SyncVersion;

import java.util.ArrayList;
import java.util.Arrays;

import org.ndnx.ndn.NDNSync;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.sync.Sync;
import org.ndnx.ndn.protocol.Component;
import org.ndnx.ndn.protocol.ContentName;

/**
 * A SyncNodeComposite object holds the necessary data for a sync tree node
 */    
public class SyncNodeComposite extends GenericXMLEncodable implements XMLEncodable, Cloneable {
	public enum SyncNodeType {HASH, LEAF, COMPONENT, BINARY};
	
	public static class SyncNodeElement extends GenericXMLEncodable implements XMLEncodable {
		public SyncNodeType _type = SyncNodeType.LEAF;
		public ContentName _name;
		public byte[] _data;
		
		public SyncNodeElement() {}
		
		public SyncNodeElement(ContentName name) {
			_name = name;
		}
		
		public SyncNodeElement(byte[] data) {
			_data = data;
			_type = SyncNodeType.HASH;
		}
		
		public SyncNodeType getType() {
			return _type;
		}
		
		public ContentName getName() {
			return _name;
		}
		
		public byte[] getData() {
			return _data;
		}
		
		public void decode(XMLDecoder decoder) throws ContentDecodingException {
			if (decoder.peekStartElement(NDNProtocolDTags.Name)) {
				_name = new ContentName();
				_name.decode(decoder);
			} else if (decoder.peekStartElement(NDNProtocolDTags.SyncContentHash)) {
				_data = decoder.readBinaryElement(NDNProtocolDTags.SyncContentHash);
				_type = SyncNodeType.HASH;
			} else if (decoder.peekStartElement(NDNProtocolDTags.Component)) {
				_data = decoder.readBinaryElement(NDNProtocolDTags.Component);
				_type = SyncNodeType.COMPONENT;
			} else if (decoder.peekStartElement(NDNProtocolDTags.BinaryValue)) {
				_data = decoder.readBinaryElement(NDNProtocolDTags.BinaryValue);
				_type = SyncNodeType.BINARY;
			} else
				throw new ContentDecodingException("Unexpected element in SyncNodeElements");
		}

		@Override
		public void encode(XMLEncoder encoder) throws ContentEncodingException {
			if (!validate())
				throw new ContentEncodingException("Link failed to validate!");

			encoder.writeStartElement(getElementLabel());
			switch (_type) {
			case LEAF:
				_name.encode(encoder);
				break;
			case HASH:
				encoder.writeElement(NDNProtocolDTags.SyncContentHash, _data);
				break;
			case COMPONENT:
				encoder.writeElement(NDNProtocolDTags.Component, _data);
				break;
			case BINARY:
				encoder.writeElement(NDNProtocolDTags.BinaryValue, _data);
				break;
			default:
				break;
			}	
			encoder.writeEndElement();   		
		}

		@Override
		public long getElementLabel() {
			return NDNProtocolDTags.SyncNodeElement;
		}

		@Override
		public boolean validate() {
			return true;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (! (obj instanceof SyncNodeElement)) {
					return false;
			}
			SyncNodeElement other = (SyncNodeElement) obj;
			if (_type != other._type)
				return false;
			if (_type == SyncNodeType.LEAF) {
				if (!_name.equals(other._name))
					return false;
			} else {
				if (!Arrays.equals(_data, other._data))
					return false;
			}
			return true;
		}
		
		public int hashCode() {
			if (_type == SyncNodeType.LEAF)
				return _name.hashCode();
			return Arrays.hashCode(_data);
		}
	}
	
	public int _version;
	public ArrayList<SyncNodeElement> _refs = new ArrayList<SyncNodeElement>();
	public byte[] _longhash = null;
	public SyncNodeElement _minName;
	public SyncNodeElement _maxName;
	public int _kind;
	public int _leafCount;
	public int _treeDepth;
	public int _byteCount = 0;
	public boolean _retrievable = true;	// Its "retrievable" if we got it from the network
										// If we built it ourselves, we may not know how to redo that
	
	public SyncNodeComposite() {}
	
	public SyncNodeComposite(ArrayList<SyncNodeElement> refs, SyncNodeElement minName, SyncNodeElement maxName, int leafCount, int depth) {
		_refs = refs;
		_minName = minName;
		_maxName = maxName;
		_leafCount = leafCount;
		computeHash();
		
		_version = Sync.SYNC_VERSION;
		
		_treeDepth = depth;
		_retrievable= false;
	}
	
	public ArrayList<SyncNodeElement> getRefs() {
		return _refs;
	}
	
	public void setLeafCount(int count) {
		_leafCount = count;
	}
	
	public SyncNodeElement getElement(int position) {
		if (position >= _refs.size())
			return null;
		return _refs.get(position);
	}

	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		decoder.readStartElement(getElementLabel());
		_version = decoder.readIntegerElement(SyncVersion);
		if (_version != Sync.SYNC_VERSION)
			throw new ContentDecodingException("Sync version mismatch: " + _version);
		if (decoder.peekStartElement(NDNProtocolDTags.SyncNodeElements)) {
			decoder.readStartElement(NDNProtocolDTags.SyncNodeElements);
			while (true) {
				try {
					SyncNodeElement ref = new SyncNodeElement();
					ref.decode(decoder);
					_refs.add(ref);
				} catch (ContentDecodingException cde) {
					break;
				}
			}
			decoder.readEndElement();
		}
		if (decoder.peekStartElement(NDNProtocolDTags.SyncContentHash)) {
			decoder.readStartElement(NDNProtocolDTags.SyncContentHash);
			_longhash = decoder.readBlob();
		}
		
		_minName = new SyncNodeElement();
		_minName.decode(decoder);
		_maxName = new SyncNodeElement();
		_maxName.decode(decoder);
		_kind = decoder.readIntegerElement(NDNProtocolDTags.SyncNodeKind);
		_leafCount = decoder.readIntegerElement(NDNProtocolDTags.SyncLeafCount);
		_treeDepth = decoder.readIntegerElement(NDNProtocolDTags.SyncTreeDepth);
		_byteCount = decoder.readIntegerElement(NDNProtocolDTags.SyncByteCount);
		decoder.readEndElement();
	}

	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		// No current need to encode this - will add if needed
	}
	
	/**
	 * Note we are purposely not comparing byte counts - this is too complicated
	 * to figure out accurately for us and since we won't be transmitting nodes, the
	 * place where it is important to be accurate are in the other values (actually
	 * depth isn't really important either but that's easy - I think - to compute)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (! (obj instanceof SyncNodeComposite)) {
				return false;
		}
		SyncNodeComposite other = (SyncNodeComposite) obj;
		if (_version != other._version)
			return false;
		if (_refs.size() != other._refs.size())
			return false;
		for (int i = 0; i < _refs.size(); i++) {
			if (! _refs.get(i).equals(other._refs.get(i)))
				return false;
		}
		if (! _minName.equals(other._minName))
			return false;
		if (! _maxName.equals(other._maxName))
			return false;
		if (!Arrays.equals(_longhash, other._longhash))
			return false;
		if (_kind != other._kind)
			return false;
		if (_leafCount != other._leafCount)
			return false;
		if (_treeDepth != other._treeDepth)
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _version;
		for (SyncNodeElement sne : _refs) {
			result = prime * result + sne.hashCode();
		}
		result = prime
				* result
				+ ((_minName == null) ? 0 : _minName.hashCode());
		result = prime
				* result
				+ ((_maxName == null) ? 0 : _maxName.hashCode());
		result = prime * result + _kind;
		result = prime * result + _leafCount;
		result = prime * result + _treeDepth;
		return result;
	}

	public long getElementLabel() {
		return NDNProtocolDTags.SyncNode;
	}
	
	public boolean retrievable() {
		return _retrievable;
	}

	public boolean validate() {
		return _version == Sync.SYNC_VERSION;
	}
	
	public SyncNodeElement getMinName() {
		return _minName;
	}
	
	public SyncNodeElement getMaxName() {
		return _maxName;
	}
	
	public byte[] getHash() {
		return _longhash;
	}
	
	public int getLeafCount() {
		return _leafCount;
	}
	
	public int getDepth() {
		return _treeDepth;
	}
	
	public static void decodeLogging(SyncNodeComposite node) {
		Log.finest(Log.FAC_SYNC, "decode node for {0} depth = {1} refs = {2}", Component.printURI(node._longhash), 
				node._treeDepth, node.getRefs().size());
		Log.finest(Log.FAC_SYNC, "min is {0}, max is {1}, expanded min is {2}, expanded max is {3}", 
				SegmentationProfile.getSegmentNumber(node._minName.getName().parent()), 
				SegmentationProfile.getSegmentNumber(node._maxName.getName().parent()),
				node._minName.getName(), node._maxName.getName());
	}
	
	/**
	 * The C code handles different sized hashes and digests. Since I currently
	 * believe that digests are always 32 bytes, I'm not worrying about that for now...
	 */
	private void computeHash() {
		byte[] tmpHash = new byte[NDNSync.SYNC_HASH_MAX_LENGTH];
		for (SyncNodeElement sne : _refs) {
			switch (sne.getType()) {
			case LEAF:
				ContentName name = sne.getName();
				byte[] nc = name.lastComponent();
				if (null != nc) { // Should always be true
					accumHash(nc, tmpHash);
				}
				break;
			case HASH:
				accumHash(sne.getData(), tmpHash);
				break;
			default:
				break;
			}
		}
		int hashLength = NDNSync.SYNC_HASH_MAX_LENGTH;
		for (int i = 0; i < tmpHash.length; i++) {
			if (tmpHash[i] == 0)
				hashLength--;
			else
				break;
		}
		_longhash = new byte[hashLength];
		System.arraycopy(tmpHash, NDNSync.SYNC_HASH_MAX_LENGTH - hashLength, _longhash, 0, hashLength);
	}
	
	private void accumHash(byte[] toAdd, byte[] hash) {
		int c = 0;
		int as = hash.length;
		int xs = toAdd.length;
		
		// first accum from digest until no more bytes
		while (xs > 0 && as > 0) {
			xs--;
			as--;
			int val = c;
			val = val + (hash[as] & 255) + (toAdd[xs] & 255);
			c = (val >> 8) & 255;
			hash[as] = (byte)(val & 255);
		}
		
		// Now propagate the carry (if any)
		while (c > 0 && as > 0) {
			as--;
			c += hash[as];
			hash[as] = (byte)(c & 255);
			c = (c >> 8) & 255;
		}
	}
}
