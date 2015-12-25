/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2011, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.ndnd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.NDNNetworkManager.NetworkProtocol;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.profiles.ndnd.NDNDaemonException;
import org.ndnx.ndn.profiles.ndnd.FaceManager;
import org.ndnx.ndn.profiles.ndnd.FaceManager.ActionType;
import org.ndnx.ndn.profiles.ndnd.FaceManager.FaceInstance;
import org.ndnx.ndn.LibraryTestBase;
import org.ndnx.ndn.impl.encoding.XMLEncodableTester;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test basic version manipulation.
 */
public class FaceManagerTest extends LibraryTestBase {
	
	FaceManager fm;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LibraryTestBase.setUpBeforeClass();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		LibraryTestBase.tearDownAfterClass();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		fm = new FaceManager();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Test method for org.ndnx.ndn.profiles.VersioningProfile#addVersion(org.ndnx.ndn.protocol.ContentName, long).
	 */
	@Test
	public void testEncodeOutputStream() {
		Log.info(Log.FAC_TEST, "Starting testEncodeOutputStream");

		FaceInstance face = new FaceInstance(ActionType.NewFace, null, NetworkProtocol.TCP, "TheNameDoesntMatter", 
				new Integer(5),	"WhoCares", new Integer(42), new Integer(100));
		// ActionType.NewFace, _ndndId, ipProto, host, port,  multicastInterface, multicastTTL, freshnessSeconds
		System.out.println("Encoding: " + face);
		assertNotNull(face);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			face.encode(baos);
		} catch (ContentEncodingException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Encoded: " );
		System.out.println(baos.toString());
		
		Log.info(Log.FAC_TEST, "Completed testEncodeOutputStream");
	}

	@Test
	public void testDecodeInputStream() {
		Log.info(Log.FAC_TEST, "Starting testDecodeInputStream");

		FaceInstance faceToEncode = new FaceInstance(ActionType.NewFace, null, NetworkProtocol.TCP, "TheNameDoesntMatter", 
				new Integer(5),	"WhoCares", new Integer(42), new Integer(100));
		System.out.println("Encoding: " + faceToEncode);
		assertNotNull(faceToEncode);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			faceToEncode.encode(baos);
		} catch (ContentEncodingException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Encoded: " );
		System.out.println(baos.toString());
		
		System.out.println("Decoding: ");
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		FaceInstance faceToDecodeTo = new FaceInstance();  /* We need an empty one to decode into */
		try {
			faceToDecodeTo.decode(bais);
		} catch (ContentDecodingException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Decoded: " + faceToDecodeTo);
		assertEquals(faceToEncode, faceToDecodeTo);
		
		Log.info(Log.FAC_TEST, "Completed testDecodeInputStream");
	}
	
	@Test
	public void testEncodingDecoding() {
		Log.info(Log.FAC_TEST, "Starting testEncodingDecoding");

		FaceInstance faceToEncode = new FaceInstance(ActionType.NewFace, null, NetworkProtocol.TCP, "TheNameDoesntMatter", 
				new Integer(5),	"WhoCares", new Integer(42), new Integer(100));
		System.out.println("Encoding: " + faceToEncode);

		FaceInstance  textFaceToDecodeInto = new FaceInstance();
		assertNotNull(textFaceToDecodeInto);
		FaceInstance  binaryFaceToDecodeInto = new FaceInstance();
		assertNotNull(binaryFaceToDecodeInto);
		XMLEncodableTester.encodeDecodeTest("FaceIntance", faceToEncode, textFaceToDecodeInto, binaryFaceToDecodeInto);
		
		Log.info(Log.FAC_TEST, "Completed testEncodingDecoding");
	}
	
	@Test
	public void testCreation() {
		Log.info(Log.FAC_TEST, "Starting testCreation");

		Integer faceID = new Integer(-142);
		FaceManager mgr = null;
		try {
			mgr = new FaceManager(putHandle);
			faceID = mgr.createFace(NetworkProtocol.UDP, "10.1.1.1", new Integer(NDNNetworkManager.DEFAULT_AGENT_PORT));
			System.out.println("Created face: " + faceID);
		} catch (NDNDaemonException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			System.out.println("Failed to create face.");
			e.printStackTrace();
			fail("Failed to create face.");
		}
		assertNotNull(mgr);
		try {
			mgr.deleteFace(faceID);
		}catch (NDNDaemonException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			System.out.println("Failed to delete face.");
			e.printStackTrace();
			fail("Failed to delete face.");
		}
		
		try {
			mgr.deleteFace(faceID);
			fail("Failed to receive expected NDNDaemonException deleting already deleted face.");
		}catch (NDNDaemonException e) {
			System.out.println("Received expected exception " + e.getClass().getName() + ", message: " + e.getMessage());
		}
		
		Log.info(Log.FAC_TEST, "Completed testCreation");
	}
}
