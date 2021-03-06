/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl.security.keys;


import junit.framework.Assert;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.io.content.PublicKeyObject;
import org.ndnx.ndn.protocol.NDNTime;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.utils.CreateUserData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests a number of things about key publishing, including management of
 * key locators.
 */
public class KeyPublishingTestRepo {
	
	static NDNTestHelper testHelper = new NDNTestHelper(KeyPublishingTestRepo.class);
	
	public static final int NUM_USERS = 3; // plus the user running the test. should coordinate with other
											// tests so we only have to run key generation once...
	static CreateUserData testUsers;
	static PublicKeyObject [] userKeyObjects;
	static String [] userNames;
	static NDNHandle [] userHandles;
	static String USER_NAMESPACE = "Users";
	static final char [] PASSWORD = "password".toCharArray();
	static String guineaPig;
	static NDNHandle myHandle;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		myHandle = NDNHandle.open();
		// Generate users to a repository
		testUsers = new CreateUserData(testHelper.getClassChildName(USER_NAMESPACE), NUM_USERS, true, PASSWORD);
		userNames = testUsers.friendlyNames().toArray(new String[NUM_USERS]);
		userHandles = new NDNHandle[NUM_USERS];
		for (int i=0; i < NUM_USERS; ++i) {
			userHandles[i] = testUsers.getHandleForUser(userNames[i]);
		}
		userKeyObjects = testUsers.publishUserKeysToRepositorySetLocators(testHelper.getClassChildName(USER_NAMESPACE));
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		myHandle.close();
	}
	
	@Test
	public void testSetLocator() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testSetLocator");

		Log.info(Log.FAC_TEST, "User {0} has a key {1} published with key locator {2}", userNames[0], userKeyObjects[0].getVersionedName(), userKeyObjects[0].getPublisherKeyLocator());
		// Set a key locator to be the user location we just published to, and see if we use that when we publish
		// Given what TUD does, the current locator should be unversioned, no publisher.
		
		// Write and see what KL we get.
		NDNStringObject testString = new 
				NDNStringObject(testHelper.getTestChildName("testSetLocator", "testString"), "A test!", 
									SaveType.REPOSITORY, userHandles[0]);
		testString.save();
		Log.info(Log.FAC_TEST, "Wrote data {0} publisher {1} KL {2}", testString.getVersionedName(), testString.getContentPublisher(), testString.getPublisherKeyLocator());
		
		Assert.assertEquals(testString.getPublisherKeyLocator(), userHandles[0].keyManager().getKeyLocator((PublisherPublicKeyDigest)null));
		// now read it back in and see what we get
		NDNStringObject readString = new NDNStringObject(testString.getBaseName(), userHandles[1]);
		Assert.assertEquals(testString.getPublisherKeyLocator(), readString.getPublisherKeyLocator());
		
		// Publish a key somewhere other than the regular place, and see if we can use it as a key locator.
		// First, hand-build self-signed key object. Need to improve built-in API for this.
		NDNTime version = new NDNTime();
		ContentName newKeyName = testHelper.getTestChildName("testSetLocator", "newKeyName");
		KeyLocator newerKeyLocator = new KeyLocator(new ContentName(newKeyName, version), 
							userHandles[NUM_USERS-1].getDefaultPublisher());
		PublicKeyObject newPublicKey = 
			new PublicKeyObject(newKeyName, userHandles[NUM_USERS-1].keyManager().getDefaultPublicKey(),
					SaveType.REPOSITORY,
					userHandles[NUM_USERS-1].keyManager().getDefaultKeyID(), 
					newerKeyLocator, userHandles[NUM_USERS-1]);
		newPublicKey.save(version);
		Assert.assertEquals(newerKeyLocator.name().name(), newPublicKey.getVersionedName());
		
		userHandles[NUM_USERS-1].keyManager().setKeyLocator(null, newerKeyLocator);
		NDNStringObject testString2 = new NDNStringObject(testHelper.getTestChildName("testSetLocator", "testString2"), 
								"A test!", SaveType.REPOSITORY, userHandles[NUM_USERS-1]);
		testString2.save();
		Log.info(Log.FAC_TEST, "Wrote data {0} publisher {1} KL {2}", testString2.getVersionedName(), testString2.getContentPublisher(), 
				testString2.getPublisherKeyLocator());
		
		Assert.assertFalse(testString2.getPublisherKeyLocator().equals(userKeyObjects[NUM_USERS-1].getPublisherKeyLocator()));
		Assert.assertEquals(newerKeyLocator, testString2.getPublisherKeyLocator());
		// now read it back in and see what we get
		NDNStringObject readString2 = new NDNStringObject(testString2.getBaseName(), userHandles[1]);
		Assert.assertEquals(newerKeyLocator, readString2.getPublisherKeyLocator());
	
		Log.info(Log.FAC_TEST, "Completed testSetLocator");
	}

}
