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
import java.util.EnumSet;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.security.crypto.ContentKeys;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * This input stream expects to do packet-oriented reading of
 * fixed chunks. The chunks can be individually signed or authenticated
 * using a Merkle Hash Tree, but read will return when it gets a single
 * block of content, and will not fill buffers across content blocks.
 * This will consume data written by either NDNBlockOutputStream,
 * or by the C program ndnsendchunks.
 * The intent is to read packet-oriented protocols; possibly a better
 * abstraction is to move this to be a subclass of DatagramSocket.
 */
public class NDNBlockInputStream extends NDNAbstractInputStream {
    
	public NDNBlockInputStream(ContentName baseName) throws IOException {
		this(baseName, null, null, null);
	}
    
	public NDNBlockInputStream(ContentName baseName, NDNHandle handle) throws IOException {
		this(baseName, null, null, handle);
	}
    
	public NDNBlockInputStream(ContentName baseName, 
                               PublisherPublicKeyDigest publisher, NDNHandle handle) throws IOException {
		this(baseName, null, publisher, handle);
	}
    
	public NDNBlockInputStream(ContentName baseName, Long segmentNumber, NDNHandle handle) throws IOException {
		this(baseName, segmentNumber, null, handle);
	}
    
	public NDNBlockInputStream(ContentName baseName, Long startingSegmentNumber,
							   PublisherPublicKeyDigest publisher, NDNHandle handle) throws IOException {
		super(baseName, startingSegmentNumber, publisher, null, null, handle);
		setTimeout(SystemConfiguration.NO_TIMEOUT);
	}
    
	public NDNBlockInputStream(ContentName baseName, Long startingSegmentNumber, 
                               PublisherPublicKeyDigest publisher, ContentKeys keys, NDNHandle handle) throws IOException {
		super(baseName, startingSegmentNumber, publisher, keys, null, handle);
		setTimeout(SystemConfiguration.NO_TIMEOUT);
	}
    
	public NDNBlockInputStream(ContentObject firstSegment, EnumSet<FlagTypes> flags, NDNHandle handle) throws IOException {
		super(firstSegment, null, flags, handle);
	}
    
	public NDNBlockInputStream(ContentObject firstSegment, ContentKeys keys, EnumSet<FlagTypes> flags, NDNHandle handle) throws IOException {
		super(firstSegment, keys, flags, handle);
	}
    
	/**
	 * Implement sequential reads of data quantized into segments. Will read the remainder
	 * of the current segment on each read(byte[], int, int) call, when a given
	 * segment runs out of bytes returns -1. Next read(byte[], int, int) call
	 * will retrieve the next segment. Meant for reading complete segments at a time.
	 */
	@Override
	protected int readInternal(byte [] buf, int offset, int len) throws IOException {
		
		if (Log.isLoggable(Log.FAC_IO, Level.INFO))
            Log.info(Log.FAC_IO, "NDNBlockInputStream: reading " + len + " bytes into buffer of length " + 
                     ((null != buf) ? buf.length : "null") + " at offset " + offset);
        // is this the first block?
        if (null == _currentSegment) {
            // This will throw an exception if no block found, which is what we want.
            setFirstSegment(getFirstSegment());
        } 
        
        // Now we have a block in place. Read from it. If we run out of block before
        // we've read len bytes, return what we read. On next read, pull next block.
        int remainingBytes = _segmentReadStream.available();
        
        if (remainingBytes <= 0) {
            if (!hasNextSegment()) {
                return -1;
            }
            ContentObject nextSegment = getNextSegment();
            if (null == nextSegment) {
                // in socket implementation, this would be EAGAIN
                return -1;
            }
            setCurrentSegment(nextSegment);
            remainingBytes = _segmentReadStream.available();
        }
        // Read minimum of remainder of this block and available buffer.
        long readCount = (remainingBytes > len) ? len : remainingBytes;
        if (null != buf) { // use for skip
            readCount = _segmentReadStream.read(buf, offset, len);
        } else {
            readCount = _segmentReadStream.skip(len);
        }
        if (Log.isLoggable(Log.FAC_IO, Level.INFO))
            Log.info(Log.FAC_IO, "NDNBlockInputStream: read " + readCount + " bytes from block " + _currentSegment.name());
        return (int)readCount;
    }
}
