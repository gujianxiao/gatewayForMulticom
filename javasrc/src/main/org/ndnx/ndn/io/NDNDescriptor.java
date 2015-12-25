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

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * A file descriptor-style wrapper around NDNVersionedInputStream and NDNVersionedOutputStream.
 *
 */
public class NDNDescriptor {

	public enum OpenMode { O_RDONLY, O_WRONLY }
	public enum SeekWhence {SEEK_SET, SEEK_CUR, SEEK_END};

	protected NDNInputStream _input = null;
	protected NDNOutputStream _output = null;

	/**
	 * Open a new descriptor for reading or writing (but not both).
	 *
	 * @param name see NDNVersionedInputStream for specification
	 * @param publisher see NDNVersionedInputStream for specification
	 * @param handle see NDNVersionedInputStream for specification
	 * @param openForWriting if true, open an output stream. Otherwise open an input stream.
	 * @throws IOException
	 */
	public NDNDescriptor(ContentName name, PublisherPublicKeyDigest publisher, 
						 NDNHandle handle, boolean openForWriting) 
										throws IOException {
		if (openForWriting) {
			openForWriting(name, publisher, handle);
		} else {	
			openForReading(name, publisher, handle);
		}
	}

	protected void openForReading(ContentName name, PublisherPublicKeyDigest publisher, NDNHandle handle) 
	throws IOException {
		ContentName nameToOpen = name;
		if (SegmentationProfile.isSegment(nameToOpen)) {
			nameToOpen = SegmentationProfile.segmentRoot(nameToOpen);
		} 

		_input = new NDNVersionedInputStream(nameToOpen, publisher, handle);
	}

	protected void openForWriting(ContentName name, 
								  PublisherPublicKeyDigest publisher,
								  NDNHandle handle) throws IOException {
		ContentName nameToOpen = name;
		if (SegmentationProfile.isSegment(name)) {
			nameToOpen = SegmentationProfile.segmentRoot(nameToOpen);
		}
		_output = new NDNVersionedOutputStream(nameToOpen, publisher, handle);
	}

	/**
	 * @return If open for reading, returns result of NDNInputStream#available(), otherwise returns 0.
	 * @throws IOException
	 */
	public int available() throws IOException {
		if (!openForReading())
			return 0;
		return _input.available();
	}

	/**
	 * @return true if opened for reading
	 */
	public boolean openForReading() {
		return (null != _input);
	}

	/**
	 * @return true if opened for writing
	 */
	public boolean openForWriting() {
		return (null != _output);
	}

	/**
	 * Close underlying stream.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (null != _input)
			_input.close();
		else
			_output.close();
	}

	/**
	 * Flush output stream if open for writing.
	 * @throws IOException
	 */
	public void flush() throws IOException {
		if (null != _output)
			_output.flush();
	}

	/**
	 * @return true if open for reading and NDNInputStream#eof().
	 */
	public boolean eof() { 
		return openForReading() ? _input.eof() : false;
	}

	/**
	 * See NDNInputStream#read(byte[], int, int).
	 */
	public int read(byte[] buf, int offset, int len) throws IOException {
		if (null != _input)
			return _input.read(buf, offset, len);
		throw new IOException("Descriptor not open for reading!");
	}

	/**
	 * See NDNInputStream#read(byte[]).
	 */
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * See NDNOutputStream#writeToNetwork(byte[], long, long).
	 */
	public void write(byte[] buf, int offset, int len) throws IOException {
		if (null != _output) {
			_output.write(buf, offset, len);
			return;
		}
		throw new IOException("Descriptor not open for writing!");
	}

	/**
	 * Sets the timeout for the underlying stream.
	 * @param timeout in msec
	 */
	public void setTimeout(int timeout) {
		if (null != _input)
			_input.setTimeout(timeout);
		else
			_output.setTimeout(timeout);
	}

}
