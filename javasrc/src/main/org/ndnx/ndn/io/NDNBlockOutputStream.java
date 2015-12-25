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

package org.ndnx.ndn.io;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNSegmenter;
import org.ndnx.ndn.impl.security.crypto.NDNBlockSigner;
import org.ndnx.ndn.impl.security.crypto.ContentKeys;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.SegmentationProfile.SegmentNumberType;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo;


/**
 * This class acts as a packet-oriented stream of data. It might be
 * better implemented as a subclass of DatagramSocket. Given a base name
 * and signing information, it writes content blocks under that base name,
 * where each block is named according to the base name concatenated with a
 * sequence number identifying the specific block. 
 * 
 * Each call to write writes one or more individual ContentObjects. The
 * maximum size is given by parameters of the segmenter used; if buffers
 * are larger than that size they are output as multiple fragments.
 * 
 * It does offer flexible content name increment options. The creator
 * can specify an initial block id (default is 0), and an increment (default 1)
 * for fixed-width blocks, or blocks can be identified by byte offset
 * in the running stream, or by another integer metric (e.g. time offset),
 * by supplying a multiplier to convert the byte offset into a metric value.
 * Finally, writers can specify the block identifier with a write.
 * Currently, however, the corresponding reader org.ndnx.ndn.io.NDNBlockInputStream expects
 * sequential segment numbering (and constraints based on the low-level NDN
 * Interest specification may make this difficult to overcome).
 */
public class NDNBlockOutputStream extends NDNAbstractOutputStream {

	public NDNBlockOutputStream(ContentName baseName, SignedInfo.ContentType type) throws IOException {
		this(baseName, type, null, null);
	}
		
	public NDNBlockOutputStream(ContentName baseName, SignedInfo.ContentType type,
								PublisherPublicKeyDigest publisher,
								NDNHandle handle)
								throws IOException {
		this(baseName, type, null, publisher, null, new NDNFlowControl((null == handle) ? NDNHandle.getHandle() : handle));
	}

	public NDNBlockOutputStream(ContentName baseName, SignedInfo.ContentType type,
			KeyLocator locator, PublisherPublicKeyDigest publisher,
			ContentKeys keys, NDNFlowControl flowControl)
			throws IOException {
		super((SegmentationProfile.isSegment(baseName) ? SegmentationProfile.segmentRoot(baseName) : baseName),
			  locator, publisher, type, keys, new NDNSegmenter(flowControl, new NDNBlockSigner()));
		startWrite(); // set up flow controller to write
	}

	public void useByteCountSequenceNumbers() {
		getSegmenter().setSequenceType(SegmentNumberType.SEGMENT_BYTE_COUNT);
		getSegmenter().setByteScale(1);
	}

	public void useFixedIncrementSequenceNumbers(int increment) {
		getSegmenter().setSequenceType(SegmentNumberType.SEGMENT_FIXED_INCREMENT);
		getSegmenter().setBlockIncrement(increment);
	}

	public void useScaledByteCountSequenceNumbers(int scale) {
		getSegmenter().setSequenceType(SegmentNumberType.SEGMENT_BYTE_COUNT);
		getSegmenter().setByteScale(scale);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			getSegmenter().put(_baseName, b, off, len, false, getType(), null, null, null, _keys);
		} catch (InvalidKeyException e) {
			throw new IOException("Cannot sign content -- invalid key!: " + e.getMessage());
		} catch (SignatureException e) {
			throw new IOException("Cannot sign content -- signature failure!: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Cannot sign content -- unknown algorithm!: " + e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			throw new IOException("Cannot encrypt content -- bad algorithm parameter!: " + e.getMessage());
		} 
	}

}
