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
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.security.crypto.ContentKeys;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;


/**
 * An output stream that adds a version to the names it outputs. Reading this
 * output with NDNVersionedInputStream allows retrieval of the "latest version"
 * of a stream.
 */
public class NDNVersionedOutputStream extends NDNOutputStream {

	/**
	 * Constructor for a NDN output stream writing under a versioned name.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param handle if null, new handle created with NDNHandle#open().
	 * @throws IOException if stream setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName, NDNHandle handle) throws IOException {
		this(baseName, (PublisherPublicKeyDigest)null, handle);
	}

	/**
	 * Constructor for a NDN output stream writing under a versioned name.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param publisher key to use to sign the segments, if null, default for user is used.
	 * @param handle if null, new handle created with NDNHandle#open()
	 * @throws IOException if stream setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName,
						   			PublisherPublicKeyDigest publisher,
						   			NDNHandle handle) throws IOException {
		this(baseName, null, publisher, null, null, handle);
	}

	/**
	 * Constructor for a NDN output stream writing under a versioned name.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param keys keys with which to encrypt content, if null content either unencrypted
	 * 		or keys retrieved according to local policy
	 * @param handle if null, new handle created with NDNHandle#open()
	 * @throws IOException if stream setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName, 
									ContentKeys keys, 
									NDNHandle handle) throws IOException {
		this(baseName, null, null, null, keys, handle);
	}

	/**
	 * Constructor for a NDN output stream writing under a versioned name.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param locator key locator to use, if null, default for key is used.
	 * @param publisher key to use to sign the segments, if null, default for user is used.
	 * @param keys keys with which to encrypt content, if null content either unencrypted
	 * 		or keys retrieved according to local policy
	 * @param handle if null, new handle created with NDNHandle#open()
	 * @throws IOException if stream setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName, 
			  			   			KeyLocator locator, 
			  			   			PublisherPublicKeyDigest publisher,
			  			   			ContentKeys keys,
			  			   			NDNHandle handle) throws IOException {
		this(baseName, locator, publisher, null, keys, handle);
	}

	/**
	 * Constructor for a NDN output stream writing under a versioned name.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param locator key locator to use, if null, default for key is used.
	 * @param publisher key to use to sign the segments, if null, default for user is used.
	 * @param type type to mark content (see ContentType), if null, DATA is used; if
	 * 			content encrypted, ENCR is used.
	 * @param keys keys with which to encrypt content, if null content either unencrypted
	 * 		or keys retrieved according to local policy
	 * @param handle if null, new handle created with NDNHandle#open().
	 * @throws IOException if stream setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName, 
									KeyLocator locator,
									PublisherPublicKeyDigest publisher, 
									ContentType type, 
									ContentKeys keys, 
									NDNHandle handle)
			throws IOException {
		/*
		 * The Flow Controller must register a Filter above the version no. for someone else's
		 * getLatestVersion interests to see this stream.
		 */
		this(baseName, locator, publisher, type, keys, 
			 new NDNFlowControl(VersioningProfile.cutTerminalVersion(baseName).first(), handle));
	}

	/**
	 * Low-level constructor used by clients that need to specify flow control behavior.
	 * @param baseName name prefix under which to write content segments; if it is already
	 *   versioned, that version is used, otherwise a new version is added.
	 * @param locator key locator to use, if null, default for key is used.
	 * @param publisher key to use to sign the segments, if null, default for user is used.
	 * @param type type to mark content (see ContentType), if null, DATA is used; if
	 * 			content encrypted, ENCR is used.
	 * @param keys keys with which to encrypt content, if null content either unencrypted
	 * 		or keys retrieved according to local policy
	 * @param flowControl flow controller used to buffer output content
	 * @throws IOException if flow controller setup fails
	 */
	public NDNVersionedOutputStream(ContentName baseName, 
									   KeyLocator locator, 
									   PublisherPublicKeyDigest publisher,
									   ContentType type, 
									   ContentKeys keys,
									   NDNFlowControl flowControl) throws IOException {
		super((VersioningProfile.hasTerminalVersion(baseName) ? baseName : VersioningProfile.addVersion(baseName)), 
				locator, publisher, type, keys, flowControl);
	}
}
