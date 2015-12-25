/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010, 2011, 2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.profiles.context;


import junit.framework.Assert;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.profiles.context.ServiceDiscoveryProfile;
import org.ndnx.ndn.profiles.repo.RepositoryControl;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test writing data under the localhost scope in the local repo and reading
 * it back again.
 */
public class LocalDataTestRepo {
	
	static NDNHandle defaultHandle;
	static NDNHandle readHandle;
	static NDNTestHelper testHelper = new NDNTestHelper(
					ServiceDiscoveryProfile.localhostScopeName(), 
					LocalDataTestRepo.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		defaultHandle = NDNHandle.getHandle();
		readHandle = NDNHandle.open();
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		readHandle.close();
	}
	
	@Test
	public void testWriteLocalData() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testWriteLocalData");

		ContentName localStringName = testHelper.getTestChildName("testWriteLocalData", "a string");
		
		NDNStringObject localString = new NDNStringObject(localStringName, "Some local data.", 
					SaveType.REPOSITORY, defaultHandle);
		localString.save();
		
		NDNStringObject readString = new NDNStringObject(localStringName, readHandle);
		
		Assert.assertTrue(readString.available());
			
		Boolean inRepo = RepositoryControl.localRepoSync(defaultHandle, localString);
		
		Assert.assertTrue("Data is in the repo", inRepo);
		
		Log.info(Log.FAC_TEST, "Completed testWriteLocalData");
	}

}
