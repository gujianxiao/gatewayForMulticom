/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2012, 2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.impl.security.keys;

import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.security.keys.SecureKeyCache;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.utils.Flosser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SymmetricKeyTest extends NDNTestBase {
	
	static Flosser flosser = null;
	
	static NDNTestHelper testHelper = new NDNTestHelper(SymmetricKeyTest.class);
	static KeyGenerator kg;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		NDNTestBase.setUpBeforeClass();
		kg = KeyGenerator.getInstance("HMAC-SHA256", KeyManager.PROVIDER);
	}

	/**
	 * Tests signing and verification of data using symmetric keys.
	 * @throws Exception
	 */
	@Test
	public void testSymmetricKeys() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testSymmetricKeys");
		
		flosser = new Flosser();
		SecretKey sk = kg.generateKey();
		SecureKeyCache skc = putHandle.getNetworkManager().getKeyManager().getSecureKeyCache();
		PublisherPublicKeyDigest publisher = new PublisherPublicKeyDigest(sk);
		skc.addSecretKey(null, publisher.digest(), sk);
		ContentName name = testHelper.getTestChildName("testSymmetricKeys", "testString");
		NDNStringObject testString1 = new NDNStringObject(name, "A test!", 
									SaveType.RAW, publisher, null, putHandle);
		flosser.handleNamespace(name);
		testString1.save();
		NDNStringObject testString2 = new NDNStringObject(name, publisher,getHandle);
		testString2.waitForData(SystemConfiguration.EXTRA_LONG_TIMEOUT);
		Assert.assertEquals(testString2.string(), "A test!");
		testString1.close();
		testString2.close();
		flosser.stopMonitoringNamespaces();
		
		Log.info(Log.FAC_TEST, "Completed testSymmetricKeys");
	}
	
	@Test
	public void testCorruptContent() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testCorruptContent");

		ContentName name = testHelper.getTestChildName("testCorruptContent", "testString");
		ContentObject co = ContentObject.buildContentObject(name, "This is a test".getBytes());
		SecretKey sk = kg.generateKey();
		co.sign(sk);
		Random rand = new Random();
		int start = rand.nextInt(co.contentLength() - 8);
		System.arraycopy(new byte[] {0xd, 0xe, 0xa, 0xd, 0xb, 0xe, 0xe, 0xf}, 0, co.content(), start, 8);
		Assert.assertFalse(co.verify(sk));
		
		Log.info(Log.FAC_TEST, "Completed testCorruptContent");
	}
}
