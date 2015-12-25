/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.repo;

import static org.ndnx.ndn.profiles.CommandMarker.COMMAND_MARKER_BASIC_ENUMERATION;
import static org.ndnx.ndn.protocol.Component.NONCE;

import java.io.IOException;

import junit.framework.Assert;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.io.RepositoryOutputStream;
import org.ndnx.ndn.io.content.Collection.CollectionObject;
import org.ndnx.ndn.profiles.versioning.VersionNumber;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.junit.Test;

public final class SimpleNameEnumerationTest {

	public SimpleNameEnumerationTest() throws Exception {}

	ContentName baseName = ContentName.fromNative("/testNE");
	NDNHandle handle = NDNHandle.getHandle();

	public VersionNumber doNameEnumerationRequest() throws IOException {
		ContentName neRequest = new ContentName(baseName, COMMAND_MARKER_BASIC_ENUMERATION);
		ContentObject co = handle.get(neRequest, 2000);
		Assert.assertNotNull(co);
		CollectionObject response = new CollectionObject(co, handle);
		return response.getVersionNumber();
	}

	@Test
	public void testNameEnumeration() throws Exception {
		// do a name enumeration request, see what version response we get
		VersionNumber first = doNameEnumerationRequest();

		// clear the ndnd cache
		Runtime.getRuntime().exec("ndnrm /");

		// do another name enumeration request, check we get the same version
		VersionNumber second = doNameEnumerationRequest();
		Assert.assertEquals(first, second);

		// write something to the repo
		ContentName freshContent = new ContentName(baseName, NONCE);
		new RepositoryOutputStream(freshContent, handle).close();

		// clear the ndnd cache
		Runtime.getRuntime().exec("ndnrm /");

		// do another name enumeration request, check we get a different version
		VersionNumber third = doNameEnumerationRequest();
		Assert.assertTrue(second != third);
	}
}
