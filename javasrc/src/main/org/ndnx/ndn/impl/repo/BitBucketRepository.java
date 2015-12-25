/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2011, 2013 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation. 
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

package org.ndnx.ndn.impl.repo;

import java.io.File;
import java.util.ArrayList;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.repo.Policy;
import org.ndnx.ndn.impl.repo.RepositoryException;
import org.ndnx.ndn.impl.repo.RepositoryInfo;
import org.ndnx.ndn.impl.repo.RepositoryStoreBase;
import org.ndnx.ndn.profiles.nameenum.NameEnumerationResponse;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;

/**
 * Test repository backend. Should not be used in production code.
 */
public class BitBucketRepository extends RepositoryStoreBase {
	
	public boolean checkPolicyUpdate(ContentObject co)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	public ContentObject getContent(Interest interest)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	public NameEnumerationResponse getNamesWithPrefix(Interest i, ContentName responseName) {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] getRepoInfo(ArrayList<ContentName> names) {
		try {
			return (new RepositoryInfo("1.0", "/named-data.net/ndn/Repos", "Repository")).encode();
		} catch (Exception e) {}
		return null;
	}

	public static String getUsage() {
		return null;
	}

	public void initialize(String repositoryRoot, 
							File policyFile, String localName, 
							String globalPrefix,
							String nameSpace, NDNHandle handle) throws RepositoryException {
		
		// Doesn't create a _handle -- no handle for this repository. 
	}

	public NameEnumerationResponse saveContent(ContentObject content) throws RepositoryException {
		return null;
	}

	public void setPolicy(Policy policy) {
	}
	
	public ArrayList<ContentName> getNamespace() {
		ArrayList<ContentName> al = new ArrayList<ContentName>();
		try {
			al.add(ContentName.fromNative("/"));
		} catch (MalformedContentNameStringException e) {}
		return al;
	}

	public boolean diagnostic(String name) {
		// No supported diagnostics
		return false;
	}

	public void shutDown() {		
	}

	public Policy getPolicy() {
		return null;
	}

	@Override
	public String getVersion() {
		return null;
	}

	public Object getStatus(String type) {
		return null;
	}

	public boolean hasContent(ContentName name) throws RepositoryException {
		return false;
	}

	public boolean bulkImport(String name) throws RepositoryException {
		return false;
	}

	public void policyUpdate() throws RepositoryException {}
}
