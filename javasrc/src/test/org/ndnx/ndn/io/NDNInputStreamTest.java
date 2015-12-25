/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011, 2012, 2013 Palo Alto Research Center, Inc.
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

import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNInputStream;
import org.ndnx.ndn.io.NDNOutputStream;
import org.ndnx.ndn.io.NDNAbstractInputStream.FlagTypes;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.ThreadAssertionRunner;
import org.junit.Test;

public class NDNInputStreamTest extends NDNTestBase {
	static NDNTestHelper testHelper = new NDNTestHelper(NDNInputStreamTest.class);

	@Test
	public void testTimeouts() throws Exception {
		Log.info(Log.FAC_TEST, "Started testTimeouts");

		ContentName testName = testHelper.getTestNamespace("testInput/timeouts");
		NDNInputStream cis = new NDNInputStream(testName, getHandle);
		long startTime = System.currentTimeMillis();
		cis.setTimeout(9000);
		try {
			cis.read();
		} catch (IOException e) {}
		Assert.assertTrue("Input stream timed out early", (System.currentTimeMillis() - startTime) > 9000);

		cis.close();

		testName = testHelper.getTestNamespace("testInput/no/timeout");
		NDNInputStream	stream = new NDNInputStream(testName, getHandle);
		BackgroundStreamer bas = new BackgroundStreamer(stream, true, SystemConfiguration.NO_TIMEOUT);
		ThreadAssertionRunner tar = new ThreadAssertionRunner(new Thread(bas));
		tar.start();
		tar.join(SystemConfiguration.EXTRA_LONG_TIMEOUT * 2);
		bas.close();

		Log.info(Log.FAC_TEST, "Completed testTimeouts");
	}

	@Test
	public void testBlocking() throws Exception {
		Log.info(Log.FAC_TEST, "Started testBlocking");

		ContentName testName = testHelper.getTestNamespace("testInput/blocking");
		NDNInputStream stream = new NDNInputStream(testName, getHandle);
		stream.addFlag(FlagTypes.BLOCKING);
		BackgroundStreamer bas = new BackgroundStreamer(stream, false, 0);
		ThreadAssertionRunner tar = new ThreadAssertionRunner(new Thread(bas));
		tar.start();
		Thread.sleep(SystemConfiguration.getDefaultTimeout() * 2);
		NDNOutputStream ostream = new NDNOutputStream(testName, putHandle);
		ostream.setBlockSize(100);
		byte[] bytes = new byte[400];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte)i;
		ostream.write(bytes);
		ostream.close();
		tar.join(SystemConfiguration.EXTRA_LONG_TIMEOUT * 2);
		bas.close();

		Log.info(Log.FAC_TEST, "Completed testBlocking");
	}

	@Test
	public void testBlockAfterFirstSegment() throws Exception {
		Log.info(Log.FAC_TEST, "Started testBlockAfterFirstSegment");

		ContentName testName = testHelper.getTestNamespace("testInput/blockAfterFirst");
		NDNInputStream stream = new NDNInputStream(testName, getHandle);
		stream.addFlag(FlagTypes.BLOCK_AFTER_FIRST_SEGMENT);
		BackgroundStreamer bas = new BackgroundStreamer(stream, false, 0);
		ThreadAssertionRunner tar = new ThreadAssertionRunner(new Thread(bas));
		tar.start();
		NDNOutputStream ostream = new NDNOutputStream(testName, putHandle);
		ostream.setBlockSize(100);
		ostream.setTimeout(SystemConfiguration.NO_TIMEOUT);
		byte[] bytes = new byte[400];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte)i;
		ostream.write(bytes);
		ostream.flush();
		Thread.sleep(SystemConfiguration.getDefaultTimeout() * 2);
		ostream.write(bytes);
		ostream.close();
		tar.join(SystemConfiguration.EXTRA_LONG_TIMEOUT * 2);
		bas.close();

		Log.info(Log.FAC_TEST, "Completed testBlockAfterFirstSegment");
	}

	
	@Test
	public void testBasename() {
		//create a segment object to open a stream with, then check basename
		ContentName streamName = testHelper.getTestNamespace("testBasename");
		streamName = VersioningProfile.addVersion(streamName);
		ContentName fullStreamName = SegmentationProfile.segmentName(streamName, SegmentationProfile.BASE_SEGMENT);
		ContentObject co = ContentObject.buildContentObject(fullStreamName, ContentType.DATA, "here is some content to name".getBytes(), null, null, null, null, SegmentationProfile.getSegmentNumberNameComponent(SegmentationProfile.BASE_SEGMENT));

		try {
			NDNInputStream stream = new NDNInputStream(co, null, getHandle);
			Assert.assertTrue(streamName.equals(stream.getBaseName()));
		} catch (IOException e) {
			Assert.fail("failed to open stream: "+e.getMessage());
		}

	}
	
	
	protected class BackgroundStreamer implements Runnable {
		NDNInputStream _stream = null;

		public BackgroundStreamer(NDNInputStream stream, boolean useTimeout, int timeout) {
			_stream = stream;
			if (useTimeout)
				_stream.setTimeout(timeout);
		}

		public void close() throws IOException {
			_stream.close();
		}

		public void run() {
			try {
				int val;
				do {
					val = _stream.read();
				} while (val != -1);
			} catch (IOException e) {
				Assert.fail("Input stream timed out or read failed: " + e.getMessage());
			}
		}
	}
}
