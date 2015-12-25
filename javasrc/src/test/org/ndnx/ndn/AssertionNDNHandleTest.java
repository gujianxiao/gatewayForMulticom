/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.junit.Before;
import org.junit.Test;

public class AssertionNDNHandleTest {
	static NDNHandle putHandle;
	static NDNTestHelper testHelper = new NDNTestHelper(AssertionNDNHandleTest.class);
	static final int WAIT_TIME = 500;
	
	@Before
	public void setUp() throws Exception {
		putHandle = NDNHandle.open();
	}
	
	@Test
	public void testFilterListenerNoError() throws Exception {
		AssertionNDNHandle getHandle = AssertionNDNHandle.open();
		ContentName filter = testHelper.getTestNamespace("testNoError");
		FilterListenerTester flt = new FilterListenerTester();
		getHandle.registerFilter(filter, flt);
		putHandle.expressInterest(new Interest(filter), new InterestListenerTester());
		getHandle.checkError(WAIT_TIME);
	}
	
	@Test
	public void testFilterListenerAssertError() throws Exception {
		AssertionNDNHandle getHandle = AssertionNDNHandle.open();
		ContentName filter = testHelper.getTestNamespace("testFilterListenerAssertError");
		FilterListenerTester flt = new FilterListenerTester();
		getHandle.registerFilter(filter, flt);
		putHandle.expressInterest(new Interest(filter), new InterestListenerTester());
		getHandle.checkError(WAIT_TIME);
		ContentName pastFilter = new ContentName(filter, "pastFilter");
		putHandle.expressInterest(new Interest(pastFilter), new InterestListenerTester());
		try {
			getHandle.checkError(WAIT_TIME);
		} catch (AssertionFailedError afe) {
			return;
		}
		Assert.fail("Missed an assertion error we should have seen");
	}
	
	private class InterestListenerTester implements NDNContentHandler {
		public Interest handleContent(ContentObject data, Interest interest) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	private class FilterListenerTester implements NDNInterestHandler {
		private int _interestsSeen = 0;
		public boolean handleInterest(Interest interest) {
			Assert.assertTrue("Assertion in handleInterest", ++_interestsSeen < 2);
			return false;
		}
		
	}
}
