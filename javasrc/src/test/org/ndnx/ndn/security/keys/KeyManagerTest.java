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

package org.ndnx.ndn.security.keys;


import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.security.crypto.NDNDigestHelper;
import org.ndnx.ndn.impl.security.keys.PublicKeyCache;
import org.ndnx.ndn.impl.security.keys.KeyServer;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo;
import org.ndnx.ndn.utils.Flosser;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Initial test of KeyManager functionality.
 *
 */
public class KeyManagerTest {

	protected static Random _rand = new Random(); // don't need SecureRandom

	protected static final int KEY_COUNT = 5;
	protected static final int DATA_COUNT_PER_KEY = 3;
	protected static KeyPair [] pairs = new KeyPair[KEY_COUNT];
	static ContentName testprefix = new ContentName("test","pubidtest");
	static ContentName keyprefix = new ContentName(testprefix,"keys");
	static ContentName dataprefix = new ContentName(testprefix,"data");

	static PublisherPublicKeyDigest [] publishers = new PublisherPublicKeyDigest[KEY_COUNT];
	static KeyLocator [] keyLocs = new KeyLocator[KEY_COUNT];

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			Security.addProvider(new BouncyCastleProvider());

			// generate key pair
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(512); // go for fast
			for (int i=0; i < KEY_COUNT; ++i) {
				pairs[i] = kpg.generateKeyPair();
				publishers[i] = new PublisherPublicKeyDigest(pairs[i].getPublic());
				keyLocs[i] = new KeyLocator(new ContentName(keyprefix, publishers[i].digest()));
			}
		} catch (Exception e) {
			System.out.println("Exception in test setup: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testWriteContent() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testWriteContent");

		// insert your preferred way of writing to the repo here
		// I'm actually about to add a bunch of lower-level write stuff
		// for the access control, but that's not in place now.
		Flosser flosser = new Flosser(testprefix);
		NDNHandle thandle = NDNHandle.open();
		NDNFlowControl fc = new NDNFlowControl(testprefix, thandle);

		KeyManager km = KeyManager.getDefaultKeyManager();

		// Important -- make a different key repository, with a separate cache, so when
		// we retrieve we don't pull from our own cache.
		NDNHandle handle = NDNHandle.open(km);
		PublicKeyCache kr = new PublicKeyCache();
		KeyServer ks = new KeyServer(handle);
		for (int i=0; i < KEY_COUNT; ++i) {
			ks.serveKey(keyLocs[i].name().name(), pairs[i].getPublic(), km.getDefaultKeyID(), null);
		}

		Random rand = new Random();
		for (int i=0; i < DATA_COUNT_PER_KEY; ++i) {
			byte [] buf = new byte[1024];
			rand.nextBytes(buf);
			byte [] digest = NDNDigestHelper.digest(buf);
			// make the names strings if it's clearer, this allows you to pull the name
			// and compare it to the actual content digest 
			ContentName dataName = new ContentName(dataprefix, digest);
			for (int j=0; j < KEY_COUNT; ++j) {
				SignedInfo si = new SignedInfo(publishers[j], keyLocs[j]);
				ContentObject co = new ContentObject(dataName, si, buf, pairs[j].getPrivate());
				System.out.println("Key " + j + ": " + publishers[j] + " signed content " + i + ": " + dataName);
				fc.put(co);
			}
		}

		// now we try getting it back..
		for (int i=0; i < KEY_COUNT; ++i) {
			System.out.println("Attempting to retrieive key " + i + ":");
			PublicKey pk = kr.getPublicKey(publishers[i], keyLocs[i], SystemConfiguration.getDefaultTimeout(), handle);
			if (null == pk) {
				System.out.println("..... failed.");
			} else {
				System.out.println("..... got it! Correct key? " + (pk.equals(pairs[i].getPublic())));
			}
		}

		flosser.stop();
		thandle.close();
		
		Log.info(Log.FAC_TEST, "Completed testWriteContent");
	}
}
