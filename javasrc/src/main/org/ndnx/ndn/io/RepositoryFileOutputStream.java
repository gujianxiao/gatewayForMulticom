/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2010 Palo Alto Research Center, Inc.
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
import org.ndnx.ndn.impl.repo.RepositoryFlowControl;
import org.ndnx.ndn.impl.security.crypto.ContentKeys;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;


/**
 * A subclass of NDNFileOutputStream which writes its segments to a repository,
 * using versioned names and writing file-level metadata. 
 * If the specified name includes a version, it will write that specific
 * version; otherwise it will add a version to the name of the stream it writes.
 * If the local boolean is set to true, the file will be written to a repo on the
 * local device.
 * If no repository is available, it will throw an exception.
 * 
 * Data written using this class can be read using a normal NDNFileInputStream; that
 * class doesn't care whether its content comes from a repository or a cache (or a mix of the two).
 */
public class RepositoryFileOutputStream extends NDNFileOutputStream {

	public RepositoryFileOutputStream(ContentName name, NDNHandle handle) throws IOException {
		this(name, (PublisherPublicKeyDigest)null, handle);
	}
	
	public RepositoryFileOutputStream(ContentName name,
								  	  PublisherPublicKeyDigest publisher, 
								  	  NDNHandle handle) throws IOException {
		this(name, null, publisher, null, null, handle);
	}

	public RepositoryFileOutputStream(ContentName name, 
								      ContentKeys keys, 
								      NDNHandle handle) throws IOException {
		this(name, null, null, null, keys, handle);
	}
	
	public RepositoryFileOutputStream(ContentName name, 
									  KeyLocator locator, 
									  PublisherPublicKeyDigest publisher,
									  ContentKeys keys,
									  NDNHandle handle) throws IOException {
		this(name, locator, publisher, null, keys, handle);
	}

	public RepositoryFileOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentType type,
								  ContentKeys keys, 
								  NDNHandle handle) throws IOException {
		super(name, locator, publisher, type, keys, 
			  new RepositoryFlowControl(VersioningProfile.cutTerminalVersion(name).first(), handle));
	}
	
	public RepositoryFileOutputStream(ContentName name, NDNHandle handle, boolean local) throws IOException {
		this(name, (PublisherPublicKeyDigest)null, handle, local);
	}
	
	public RepositoryFileOutputStream(ContentName name,
								  	  PublisherPublicKeyDigest publisher, 
								  	  NDNHandle handle,
								  	  boolean local) throws IOException {
		this(name, null, publisher, null, null, handle, local);
	}

	public RepositoryFileOutputStream(ContentName name, 
								      ContentKeys keys, 
								      NDNHandle handle,
								      boolean local) throws IOException {
		this(name, null, null, null, keys, handle, local);
	}
	
	public RepositoryFileOutputStream(ContentName name, 
									  KeyLocator locator, 
									  PublisherPublicKeyDigest publisher,
									  ContentKeys keys,
									  NDNHandle handle,
									  boolean local) throws IOException {
		this(name, locator, publisher, null, keys, handle, local);
	}

	public RepositoryFileOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentType type,
								  ContentKeys keys, 
								  NDNHandle handle,
								  boolean local) throws IOException {
		super(name, locator, publisher, type, keys, 
			  new RepositoryFlowControl(VersioningProfile.cutTerminalVersion(name).first(), handle, local));
	}
}
