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

import java.util.ArrayList;

import org.ndnx.ndn.impl.repo.LogStructRepoStore;
import org.ndnx.ndn.impl.repo.RepositoryInfo;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.impl.encoding.XMLEncodableTester;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the RepositoryInfo data structure.
 * Part of repository test infrastructure.
 */
public class RepositoryInfoTest {

	private static String CURRENT_VERSION = LogStructRepoStore.CURRENT_VERSION;
	private static String DEFAULT_LOCAL_NAME = "Repository";
	private static String DEFAULT_GLOBAL_NAME = "/named-data.net/ndn/Repos";
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Test method for org.ndnx.ndn.impl.repo.RepositoryInfo#encode(org.ndnx.ndn.impl.encoding.XMLEncoder).
	 */
	@Test
	public void testDecodeInputStream() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testDecodeInputStream");
		
		RepositoryInfo ri = new RepositoryInfo(CURRENT_VERSION, DEFAULT_GLOBAL_NAME, DEFAULT_LOCAL_NAME);
		RepositoryInfo dri = new RepositoryInfo();
		RepositoryInfo bri = new RepositoryInfo();
		XMLEncodableTester.encodeDecodeTest("RepositoryInfo", ri, dri, bri);
		
		ArrayList<ContentName> names = new ArrayList<ContentName>();
		names.add(ContentName.fromNative("/aprefix/asuffix"));
		names.add(ContentName.fromNative("/aprefix/anothersuffix"));
		names.add(new ContentName(names.get(0), "moresuffix"));
		RepositoryInfo rin = new RepositoryInfo(CURRENT_VERSION, DEFAULT_GLOBAL_NAME, DEFAULT_LOCAL_NAME, names);
		RepositoryInfo drin = new RepositoryInfo();
		RepositoryInfo brin = new RepositoryInfo();
		XMLEncodableTester.encodeDecodeTest("RepositoryInfo(Names)", rin, drin, brin);
		
		Log.info(Log.FAC_TEST, "Completed testDecodeInputStream");
	}

}
