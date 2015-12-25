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

package org.ndnx.ndn.profiles.search;

import junit.framework.Assert;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.profiles.search.Pathfinder;
import org.ndnx.ndn.profiles.search.Pathfinder.SearchResults;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PathfinderTestRepo {
	
	static final String TARGET_POSTFIX = "/TheTarget";
	static ContentName TARGET_POSTFIX_NAME;
	
	static NDNTestHelper testHelper = new NDNTestHelper(PathfinderTestRepo.class);
	static NDNHandle writeHandle;
	static NDNHandle readHandle;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		writeHandle = NDNHandle.open();
		readHandle = NDNHandle.open();
		TARGET_POSTFIX_NAME  = ContentName.fromNative(TARGET_POSTFIX);
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		writeHandle.close();
		readHandle.close();
	}

	@Test
	public void testPathfinder() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testPathfinder");

		// Make the content
		ContentName testRoot = testHelper.getTestNamespace("testPathfinder");
		ContentName startingPoint = new ContentName(testRoot, "This", "is", "a", "longer", "path", "than", "necessary.");
		
		NDNStringObject targetObject = new NDNStringObject(
				new ContentName(startingPoint.parent().parent().parent(), TARGET_POSTFIX_NAME), "The target!", SaveType.REPOSITORY, writeHandle);
		targetObject.save();
		
		Pathfinder finder = new Pathfinder(startingPoint,null, TARGET_POSTFIX_NAME, true, false, 
									SystemConfiguration.SHORT_TIMEOUT, null, readHandle);
		SearchResults results = finder.waitForResults();
		Assert.assertNotNull(results.getResult());
		
		Log.info(Log.FAC_TEST, "Completed testPathfinder");
	}
	

}
