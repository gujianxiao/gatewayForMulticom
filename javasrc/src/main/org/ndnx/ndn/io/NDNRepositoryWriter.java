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
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.repo.RepositoryFlowControl;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;

/**
 * A NDNWriter subclass that writes to a repository.
 *
 */
public class NDNRepositoryWriter extends NDNWriter {

	public NDNRepositoryWriter(String namespace, NDNHandle handle)
			throws MalformedContentNameStringException, IOException {
		super(namespace, handle);
	}

	public NDNRepositoryWriter(ContentName namespace, NDNHandle handle)
			throws IOException {
		super(namespace, handle);
	}

	public NDNRepositoryWriter(NDNHandle handle) throws IOException {
		super(handle);
	}

	public NDNRepositoryWriter(NDNFlowControl flowControl) {
		super(flowControl);
	}

	/**
	 * Create a repository flow controller. 
	 * @param namespace
	 * @param handle
	 * @return
	 * @throws IOException 
	 */
	protected NDNFlowControl getFlowController(ContentName namespace, NDNHandle handle) throws IOException {
		if (null != namespace) {
			return new RepositoryFlowControl(namespace, handle);
		}
		return new RepositoryFlowControl(handle);
	}
	
	/**
	 * Create a repository flow controller. 
	 * @param namespace
	 * @param handle
	 * @param local
	 * @return
	 * @throws IOException 
	 */
	protected NDNFlowControl getFlowController(ContentName namespace, NDNHandle handle, boolean local) throws IOException {
		if (null != namespace) {
			return new RepositoryFlowControl(namespace, handle, local);
		}
		return new RepositoryFlowControl(handle, local);
	}
}
