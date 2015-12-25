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

package org.ndnx.ndn.protocol;

import java.security.cert.CertificateEncodingException;
import java.util.Arrays;

import org.bouncycastle.asn1.x509.DigestInfo;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.security.crypto.NDNDigestHelper;
import org.ndnx.ndn.impl.security.crypto.MerklePath;
import org.ndnx.ndn.impl.security.crypto.util.OIDLookup;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;

/**
 * A class to encapsulate Signature data within a ContentObject. 
 * A Signature contains three components: the digestAlgorithm used to generate the
 * digest, the bits of the signature itself, and an optional "witness" which is used in the
 * verification of aggregated signatures -- signatures that are generated over
 * multiple objects at once. Each object in such a signature group has its own
 * witness, which is necessary to verify that object with respect to that signature.
 * For example, if a set of content objects is digested into a Merkle hash tree,
 * the signature bits for each member of set would contain the same public
 * key signature on the root of the hash tree, and the witness would contain
 * a representation of the path through the Merkle tree that one needs to traverse
 * to verify that individual block.
 * 
 * For an explanation of why we separate the digest algorithm in the signature
 * (rather than no algorithm at all, or a composite signature algorithm), see
 * ndnx.xsd.
 */
public class Signature extends GenericXMLEncodable implements XMLEncodable,
		Comparable<Signature>, Cloneable {
	
    byte [] _witness;
	byte [] _signature;
	String _digestAlgorithm;
	
	/**
	 * Build a Signature
	 * @param digestAlgorithm if null, will use default
	 * @param witness can be null
	 * @param signature
	 */
	public Signature(String digestAlgorithm, byte [] witness, byte [] signature) {
    	_witness = witness;
    	_signature = signature;
    	_digestAlgorithm = digestAlgorithm;
	}

	/**
	 * Builds a Signature using the default digest algorithm.
	 * @param witness can be null
	 * @param signature
	 */
	public Signature(byte [] witness,
					 byte [] signature) {
		this(null, witness, signature);
	}

	/**
	 * Builds a Signature with the default digest algorithm and no witness
	 * @param signature
	 */
	public Signature(byte [] signature) {
		this(null, null, signature);
	}
	
	/**
	 * For use by decoders
	 */
	public Signature() {} 
	
	/**
	 * Get the signature
	 * @return the signature
	 */
	public final byte [] signature() { return _signature; }
	
	/**
	 * Get the witness
	 * @return the witness
	 */
	public final byte [] witness() { return _witness; }

	/**
	 * Get the digest algorithm
	 * @return the digest algorithm
	 */
	public String digestAlgorithm() {
		if (null == _digestAlgorithm)
			return NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM;
		return _digestAlgorithm;
	}
	
	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		decoder.readStartElement(getElementLabel());

		if (decoder.peekStartElement(NDNProtocolDTags.DigestAlgorithm)) {
			_digestAlgorithm = decoder.readUTF8Element(NDNProtocolDTags.DigestAlgorithm); 
		}

		if (decoder.peekStartElement(NDNProtocolDTags.Witness)) {
			_witness = decoder.readBinaryElement(NDNProtocolDTags.Witness); 
		}

		_signature = decoder.readBinaryElement(NDNProtocolDTags.SignatureBits);
		
		decoder.readEndElement();
	}

    @Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
    	
		if (!validate()) {
			throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
		}
		
		encoder.writeStartElement(getElementLabel());
		
		if ((null != digestAlgorithm()) && (!digestAlgorithm().equals(NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM))) {
			encoder.writeElement(NDNProtocolDTags.DigestAlgorithm, OIDLookup.getDigestOID(digestAlgorithm()));
		}
		
		if (null != witness()) {
			// needs to handle null witness
			encoder.writeElement(NDNProtocolDTags.Witness, _witness);
		}

		encoder.writeElement(NDNProtocolDTags.SignatureBits, _signature);

		encoder.writeEndElement();   		
	}

	@Override
	public long getElementLabel() { return NDNProtocolDTags.Signature; }

	@Override
	public boolean validate() {
		return null != signature();
	}

	/**
	 * Implement Cloneable
	 */
	public Signature clone() {
		Signature s;
		try {
			s = (Signature)super.clone();
			s._witness = (null == _witness) ? null : _witness.clone();
			s._signature = _signature.clone();
			return s;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Implement Comparable
	 */
	public int compareTo(Signature o) {
		int result = 0;
		if (null == digestAlgorithm()) {
			if (null != o.digestAlgorithm())
				result = NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM.compareTo(o.digestAlgorithm());
		} else {
			result = digestAlgorithm().compareTo((null == o.digestAlgorithm()) ? NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM : o.digestAlgorithm());
		}
		if (result == 0)
			result = DataUtils.compare(witness(), o.witness());
		if (result == 0)
			result = DataUtils.compare(this.signature(), o.signature());
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(_signature);
		result = prime * result + Arrays.hashCode(_witness);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Signature other = (Signature) obj;
		if (null == digestAlgorithm()) {
			if (null != other.digestAlgorithm())
				if (!NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM.equals(other.digestAlgorithm()))
					return false;
		} else {
			if (!digestAlgorithm().equals((null == other.digestAlgorithm()) ? 
							NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM : other.digestAlgorithm()))
				return false;
		}
		if (!Arrays.equals(_signature, other._signature))
			return false;
		if (!Arrays.equals(_witness, other._witness))
			return false;
		return true;
	}

	/**
	 * Compute the content proxy for a given node. This should likely move somewhere else
	 * @param nodeContent the content stored at this node
	 * @param isDigest is the content already digested
	 * @return the proxy digest (for example, the computed root of the Merkle hash tree) for this node
	 * @throws CertificateEncodingException if we cannot decode the witness
	 */
	public byte[] computeProxy(byte[] nodeContent, boolean isDigest) throws CertificateEncodingException {
		if (null == witness())
			return null;
		
		DigestInfo info = NDNDigestHelper.digestDecoder(witness());
		
		byte [] proxy = null;
		
		if (MerklePath.isMerklePath(info)) {
			MerklePath mp = new MerklePath(info.getDigest());
			proxy = mp.root(nodeContent, isDigest);
		} else {
			Log.warning("Unexpected witness type: " + info.getAlgorithmId().toString());
		}
		return proxy;
	}
}
