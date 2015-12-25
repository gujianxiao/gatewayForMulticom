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

package org.ndnx.ndn.impl.repo;

import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.protocol.ContentObject;

/**
 * Special flow controller to write NDN objects to repository internally
 *
 */

public class RepositoryInternalFlowControl extends NDNFlowControl {
	RepositoryStore _repo;

	public RepositoryInternalFlowControl(RepositoryStore repo, NDNHandle handle) throws IOException {
		super(handle);
		_repo = repo;
	}
	
	/**
	 * Put to the repository instead of ndnd
	 */
	public ContentObject put(ContentObject co) throws IOException {
		try {
			_repo.saveContent(co);
		} catch (RepositoryException e) {
			throw new IOException(e.getMessage());
		}
		return co;
	}
	
	/**
	 * Don't do waitForPutDrain
	 */
	public void afterClose() throws IOException {};
}
