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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.security.crypto.ContentKeys;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.io.content.Header;
import org.ndnx.ndn.io.content.Header.HeaderObject;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.metadata.MetadataProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;

/**
 * A versioned output stream that adds a header containing file-level metadata
 * to every stream it outputs (see Header for contents). Reading this
 * content with NDNFileInputStream will allow retrieval of both the 
 * content (including automatic retrieval of the latest version, if desired),
 * and the header.
 */
public class NDNFileOutputStream extends NDNVersionedOutputStream {

	public NDNFileOutputStream(ContentName name, NDNHandle handle) throws IOException {
		this(name, (PublisherPublicKeyDigest)null, handle);
	}

	public NDNFileOutputStream(ContentName name,
						   	   PublisherPublicKeyDigest publisher,
						   	   NDNHandle handle) throws IOException {
		this(name, null, publisher, null, null, handle);
	}

	public NDNFileOutputStream(ContentName name, 
							   ContentKeys keys, 
							   NDNHandle handle) throws IOException {
		this(name, null, null, null, keys, handle);
	}

	public NDNFileOutputStream(ContentName name, 
			  			   	   KeyLocator locator, 
			  			   	   PublisherPublicKeyDigest publisher,
			  			   	   ContentKeys keys,
			  			   	   NDNHandle handle) throws IOException {
		this(name, locator, publisher, null, keys, handle);
	}

	public NDNFileOutputStream(ContentName name, 
							   KeyLocator locator,
							   PublisherPublicKeyDigest publisher, 
							   ContentType type, 
							   ContentKeys keys, 
							   NDNHandle handle)
			throws IOException {
		super(name, locator, publisher, type, keys, handle);
	}
	
	protected NDNFileOutputStream(ContentName name, 
								  KeyLocator locator, 
								  PublisherPublicKeyDigest publisher,
								  ContentType type, 
								  ContentKeys keys,
								  NDNFlowControl flowControl) throws IOException {
		super(name, locator, publisher, type, keys, flowControl);
	}

	/**
	 * Writes the header to the network.
	 * @throws IOException
	 */
	protected void writeHeader() throws ContentEncodingException, IOException {
		// What do we put in the header if we have multiple merkle trees?
		putHeader(_baseName, lengthWritten(), getBlockSize(), _dh.digest(), null);
	}
	
	/**
	 * Subclasses that want to do something other than write a header at the end
	 * should override this, not close(), because NDNOutputStream.close() currently
	 * calls waitForPutDrain, and we don't want to call that till after we've put the header.
	 * 
	 * When we can, we might want to write the header earlier. Here we wait
	 * till we know how many bytes are in the file.
	 * @throws ContentEncodingException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws NoSuchAlgorithmException 
	 * @throws SignatureException 
	 * @throws InvalidKeyException 
	 */
	@Override
	protected void closeNetworkData() 
			throws ContentEncodingException, IOException, InvalidKeyException, SignatureException, 
						NoSuchAlgorithmException, InterruptedException  {
		super.closeNetworkData();
		writeHeader();
	}
	
	/**
	 * Actually put the header blocks (versioned, though that isn't necessary) onto the wire.
	 */
	protected void putHeader(
			ContentName name, long contentLength, int blockSize, byte [] contentDigest, 
			byte [] contentTreeAuthenticator) throws ContentEncodingException, IOException  {


		ContentName headerName = MetadataProfile.headerName(name);
		
		// Really want to query the segmenter about the last block we wrote.
		Header headerData = new Header(SegmentationProfile.baseSegment(), this._baseNameIndex, blockSize, contentLength, contentDigest, contentTreeAuthenticator);
		if (Log.isLoggable(Log.FAC_IO, Level.FINEST))
			Log.finest(Log.FAC_IO, "HEADER: Writing header, starting segment " + headerData.start() + " count " + headerData.count() + " length " + headerData.length());
		// DKS TODO -- deal with header encryption, making sure it has same publisher as
		// rest of file via the segmenter
		// The segmenter contains the flow controller. Should do the right thing whether this
		// is a raw stream or a repo stream. It should also already have the keys. Could just share
		// the segmenter. For now, use our own.
		HeaderObject header = new HeaderObject(headerName, headerData, this._publisher, this._locator, this.getSegmenter().getFlowControl());
		header.save();
	}
}
