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

package org.ndnx.ndn.io;

import java.io.IOException;

import junit.framework.Assert;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNInputStream;
import org.ndnx.ndn.io.NDNVersionedInputStream;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PipelineTest {

	public static NDNTestHelper testHelper = new NDNTestHelper(PipelineTest.class);
	
	public static ContentName testName;
	
	public static NDNHandle readHandle;
	public static NDNHandle writeHandle;
	
	private static NDNInputStream istream;
	private static NDNVersionedInputStream vistream;
	
	private static long segments = 20;
	
	private static long bytesWritten;
	private static byte[] firstDigest = null;
	
	//need a stream to test with
	
	//need a responder with objects to pipeline
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		readHandle = NDNHandle.open();
		writeHandle = NDNHandle.open();
		
		ContentName namespace = testHelper.getTestNamespace("pipelineTest");
		testName = new ContentName(namespace, "PipelineSegments");
		testName = VersioningProfile.addVersion(testName);
		try {
			putSegments();
		} catch (Exception e) {
			Log.info(Log.FAC_TEST, "failed to put objects for pipeline test: "+e.getMessage());
			Assert.fail();
		}
		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		readHandle.close();
		writeHandle.close();
		KeyManager.closeDefaultKeyManager();
	}
	
	//put segments
	private static void putSegments() throws Exception{
		ContentName segment;
		ContentObject object;
		long lastMarkerTest = 1;
		bytesWritten = 0;
		byte[] toWrite;
		for (int i = 0; i < segments; i++) {
			if (i>0)
				lastMarkerTest = segments-1;
			segment = SegmentationProfile.segmentName(testName, i);
			toWrite = ("this is segment "+i+" of "+segments).getBytes();
			bytesWritten = bytesWritten + toWrite.length;
			object = ContentObject.buildContentObject(segment, toWrite, null, null, SegmentationProfile.getSegmentNumberNameComponent(lastMarkerTest));
			if (i == 0) {
				firstDigest = object.digest();
			}
			writeHandle.put(object);
		}
		System.out.println("wrote "+bytesWritten+" bytes");
	}
	
	
	//need to ask for objects...  start in order
	@Test
	public void testGetAllSegmentsFromPipeline() {
		Log.info(Log.FAC_TEST, "Starting testGetAllSegmentsFromPipeline");

		long received = 0;
		byte[] bytes = new byte[1024];
		
		try {
			istream = new NDNInputStream(testName, readHandle);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "failed to open stream for pipeline test: "+e1.getMessage());
			Assert.fail();
		}
		
		while (!istream.eof()) {
			try {
				received += istream.read(bytes);
			} catch (IOException e) {
				Log.warning(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Log.info(Log.FAC_TEST, "read "+received+" from stream");
		Assert.assertTrue(received == bytesWritten);
		
		Log.info(Log.FAC_TEST, "Completed testGetAllSegmentsFromPipeline");
	}
	
	
	//skip
	@Test
	public void testSkipWithPipeline() {
		Log.info(Log.FAC_TEST, "Starting testSkipWithPipeline");

		long received = 0;
		byte[] bytes = new byte[100];
		
		boolean skipDone = false;
		
		try {
			istream = new NDNInputStream(testName, readHandle);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "Failed to get new stream: "+e1.getMessage());
			Assert.fail();
		}
		
		while (!istream.eof()) {
			try {
				Log.info(Log.FAC_TEST, "Read so far: "+received);
				if (received > 0 && received < 250 && !skipDone) {
					//want to skip some segments
					istream.skip(100);
					skipDone = true;
				}
				received += istream.read(bytes);
			} catch (IOException e) {
				Log.info(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Log.info(Log.FAC_TEST, "read "+received+" from stream");
		Assert.assertTrue(received == bytesWritten - 100);
		
		Log.info(Log.FAC_TEST, "Completed testSkipWithPipeline");
	}
	
	//seek
	
	@Test
	public void testSeekWithPipeline() {
		Log.info(Log.FAC_TEST, "Starting testSeekWithPipeline");

		long received = 0;
		byte[] bytes = new byte[100];
		
		boolean seekDone = false;
		long skipped = 0;
		
		try {
			istream = new NDNInputStream(testName, readHandle);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "Failed to seek stream to byte 0: "+e1.getMessage());
			Assert.fail();
		}
		
		while (!istream.eof()) {
			try {
				Log.info(Log.FAC_TEST, "Read so far: "+received);
				if (received > 0 && received < 150 && !seekDone) {
					//want to skip some segments
					istream.seek(200);
					seekDone = true;
					skipped = 200 - received;
				}
				received += istream.read(bytes);
			} catch (IOException e) {
				Log.info(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Log.info(Log.FAC_TEST, "read "+received+" from stream");
		Assert.assertTrue(received == bytesWritten - skipped);
		
		Log.info(Log.FAC_TEST, "Completed testSeekWithPipeline");
	}
	
	//restart
	@Test
	public void testResetWithPipeline() {
		Log.info(Log.FAC_TEST, "Starting testResetWithPipeline");

		long received = 0;
		byte[] bytes = new byte[100];
		
		boolean resetDone = false;
		long resetBytes = 0;
		
		try {
			istream = new NDNInputStream(testName, readHandle);
			istream.mark(0);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "Failed to get new stream: "+e1.getMessage());
			Assert.fail();
		}
		
		while (!istream.eof()) {
			try {
				Log.info(Log.FAC_TEST, "Read so far: "+received);
				if (received > 0 && received < 250 && !resetDone) {
					//want to skip some segments
					istream.reset();
					resetDone = true;
					resetBytes = received;
				}
				received += istream.read(bytes);
			} catch (IOException e) {
				Log.info(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Log.info(Log.FAC_TEST, "read "+received+" from stream");
		Assert.assertTrue(received == bytesWritten + resetBytes);
		
		Log.info(Log.FAC_TEST, "Completed testResetWithPipeline");
	}
	
	//test interface with versioning
	@Test
	public void testVersionedNameWithPipeline() {
		Log.info(Log.FAC_TEST, "Starting testVersionedNameWithPipeline");

		long received = 0;
		byte[] bytes = new byte[1024];
		
		try {
			vistream = new NDNVersionedInputStream(VersioningProfile.cutLastVersion(testName), readHandle);
		} catch (IOException e1) {
			Log.info(Log.FAC_TEST, "failed to open stream for pipeline test: "+e1.getMessage());
			Assert.fail();
		}
		
		while (!vistream.eof()) {
			try {
				received += vistream.read(bytes);
			} catch (IOException e) {
				Log.warning(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Log.info(Log.FAC_TEST, "read "+received+" from stream");
		Assert.assertTrue(received == bytesWritten);
		
		Log.info(Log.FAC_TEST, "Completed testVersionedNameWithPipeline");
	}
	
	@Test
	public void testGetFirstDigest() {
		Log.info(Log.FAC_TEST, "Starting testGetFirstDigest");

		long received = 0;
		byte[] bytes = new byte[1024];

		try {
			istream = new NDNInputStream(testName, readHandle);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "failed to open stream for pipeline test: "+e1.getMessage());
			Assert.fail();
		}
		
		try {
			Assert.assertTrue(DataUtils.arrayEquals(firstDigest, istream.getFirstDigest()));
		} catch (IOException e3) {
			Log.warning(Log.FAC_TEST, "failed to get first digest for pipeline test:");
			Assert.fail();
		}
		try {
			istream.close();
		} catch (IOException e2) {
			Log.warning(Log.FAC_TEST, "failed to close stream for pipeline test: "+e2.getMessage());
			Assert.fail();
		}		
		Log.info(Log.FAC_TEST, "start first segment digest "+ DataUtils.printBytes(firstDigest));
		
		try {
			istream = new NDNInputStream(testName, readHandle);
		} catch (IOException e1) {
			Log.warning(Log.FAC_TEST, "failed to open stream for pipeline test: "+e1.getMessage());
			Assert.fail();
		}		
		
		while (!istream.eof()) {
			try {
				received += istream.read(bytes);
			} catch (IOException e) {
				Log.warning(Log.FAC_TEST, "failed to read segments: "+e.getMessage());
				Assert.fail();
			}
		}
		Assert.assertTrue(received == bytesWritten);
		try {
			Assert.assertTrue(DataUtils.arrayEquals(firstDigest, istream.getFirstDigest()));
		} catch (IOException e) {
			Log.warning(Log.FAC_TEST, "failed to get first digest after reading in pipeline test:");
			Assert.fail();
		}
		Log.info(Log.FAC_TEST, "end first segment digest "+ DataUtils.printBytes(firstDigest));
		
		Log.info(Log.FAC_TEST, "Completed testGetFirstDigest");
	}
}
