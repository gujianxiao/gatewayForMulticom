/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl;

import java.util.ArrayList;
import java.util.Random;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class NDNNetworkTestRepo extends NDNTestBase {
	
	protected static final int TEST_TIMEOUT = SystemConfiguration.MEDIUM_TIMEOUT;
	
	protected static Random _rnd = new Random();
	static NDNTestHelper testHelper = new NDNTestHelper(NDNNetworkTestRepo.class);
	static ContentName testPrefix = testHelper.getClassChildName("networkTestRepo");
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NDNTestBase.setUpBeforeClass();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		NDNTestBase.tearDownAfterClass();
	}
	
	/**
	 * This test once uncovered an error in the network manager due to prefix registration timing
	 */
	@Test
	public void testObjectIOLoop() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testObjectIOLoop");
		
		NDNHandle handle = NDNHandle.getHandle();
		ContentName basename = testHelper.getTestNamespace("content_"  + _rnd.nextLong());

	    // Send a stream of string objects
	    ArrayList<NDNStringObject> sent = new ArrayList<NDNStringObject>();
	         int tosend = 100;
	    for(int i = 0; i < tosend; i++) {
	       // Save content
	       try {
	    	   System.out.println("Trying for object " + i);
	           NDNStringObject so = new NDNStringObject(basename,
	                   String.format("string object %d", i),
	                   SaveType.LOCALREPOSITORY, handle);
	           so.save();
	           so.close();
	           sent.add(so);
	       } catch(Exception e) {
	           e.printStackTrace();
	           throw e;
	       }
	       System.out.println(i);
	    }
	   
		Log.info(Log.FAC_TEST, "Completed testObjectIOLoop");
	}
	
	@Test
	public void testBadCallback() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testBadCallback");

		BogusFilterListener bfl = new BogusFilterListener();
		TestListener tl = new TestListener();
		putHandle.registerFilter(testPrefix, bfl);
		putHandle.checkError(TEST_TIMEOUT);
		Interest interest = new Interest(testPrefix);
		synchronized (bfl) {
			getHandle.expressInterest(interest, tl);
			putHandle.checkError(TEST_TIMEOUT);
			long beforeTime = System.currentTimeMillis();
			bfl.wait(SystemConfiguration.SHORT_TIMEOUT);
			// Make sure this works via log
			putHandle.getNetworkManager().dumpHandlerStackTrace("Testing handler dump - this is expected");
			bfl.wait(SystemConfiguration.MAX_TIMEOUT * 2);
			if (System.currentTimeMillis() - beforeTime >= (SystemConfiguration.MAX_TIMEOUT * 2))
				Assert.fail("Network manager failed to interrupt hung handler");
		}
		getHandle.cancelInterest(interest, tl);
		putHandle.checkError(TEST_TIMEOUT);
		
		Log.info(Log.FAC_TEST, "Completed testBadCallback");
	}
	
	class TestListener implements NDNContentHandler {

		public Interest handleContent(ContentObject co,
				Interest interest) {
			Assert.assertFalse(co == null);
			return null;
		}
	}
	class BogusFilterListener implements NDNInterestHandler {

		public boolean handleInterest(Interest interest) {
			try {
				NDNStringObject cso = new NDNStringObject(new ContentName(testPrefix, "bogus"), "bogus",
						SaveType.REPOSITORY, putHandle);
				cso.setTimeout(SystemConfiguration.NO_TIMEOUT);
				cso.save();		// This is bogus
				Assert.fail("Unexpected return");
			} catch (Exception e) {
				synchronized (this) {
					notifyAll();
				}
			}
			return true;
		}	
	}	
}
