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

package org.ndnx.ndn.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNOutputStream;
import org.ndnx.ndn.io.NDNReader;
import org.ndnx.ndn.io.NDNVersionedInputStream;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.utils.Flosser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for versioned input streams.
 */
public class NDNVersionedInputStreamTest {

	static ContentName defaultStreamName;
	static ContentName firstVersionName;
	static int firstVersionLength;
	static int firstVersionMaxSegment;
	static byte [] firstVersionDigest;
	static ContentName middleVersionName;
	static int middleVersionLength;
	static int middleVersionMaxSegment;
	static byte [] middleVersionDigest;
	static ContentName latestVersionName;
	static int latestVersionLength;
	static int latestVersionMaxSegment;
	static byte [] latestVersionDigest;
	static NDNHandle outputHandle;
	static NDNHandle inputHandle;
	static NDNHandle inputHandle2;
	static NDNReader reader;
	static final int MAX_FILE_SIZE = 1024*1024; // 1 MB
	static final int BUF_SIZE = 4096;

	static final int MERKLE_TREE_LENGTH = SegmentationProfile.DEFAULT_BLOCKSIZE * NDNOutputStream.BLOCK_BUF_COUNT;
	static int [] problematicLengths = {
		SegmentationProfile.DEFAULT_BLOCKSIZE,
		SegmentationProfile.DEFAULT_BLOCKSIZE/2,
		SegmentationProfile.DEFAULT_BLOCKSIZE*2,
		((int)(SegmentationProfile.DEFAULT_BLOCKSIZE*1.5)),
		((int)(SegmentationProfile.DEFAULT_BLOCKSIZE*2.5)),
		MERKLE_TREE_LENGTH + SegmentationProfile.DEFAULT_BLOCKSIZE,
		MERKLE_TREE_LENGTH + SegmentationProfile.DEFAULT_BLOCKSIZE/2,
		MERKLE_TREE_LENGTH + SegmentationProfile.DEFAULT_BLOCKSIZE*2,
		MERKLE_TREE_LENGTH + ((int)(SegmentationProfile.DEFAULT_BLOCKSIZE*1.5)),
		MERKLE_TREE_LENGTH + ((int)(SegmentationProfile.DEFAULT_BLOCKSIZE*2.5))};
	static byte [][] problematicDigests = new byte[problematicLengths.length][];
	static ContentName [] problematicNames = new ContentName[problematicLengths.length];

	/**
	 * Handle naming for the test
	 */
	static NDNTestHelper testHelper = new NDNTestHelper(NDNVersionedInputStreamTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Random randBytes = new Random(); // doesn't need to be secure
		outputHandle = NDNHandle.open();
		inputHandle = NDNHandle.open();
		inputHandle2 = NDNHandle.open();
		reader = new NDNReader(inputHandle);
		Flosser flosser = new Flosser();

		// Write a set of output
		defaultStreamName = new ContentName(testHelper.getClassNamespace(), "LongOutput.bin");

		firstVersionName = VersioningProfile.addVersion(defaultStreamName);
		firstVersionLength = randBytes.nextInt(MAX_FILE_SIZE);
		firstVersionMaxSegment = (firstVersionLength == 0) ? 0 :
			((firstVersionLength + SegmentationProfile.DEFAULT_BLOCKSIZE - 1) / SegmentationProfile.DEFAULT_BLOCKSIZE) - 1;
		firstVersionDigest = writeFile(flosser, firstVersionName, firstVersionLength, randBytes);

		middleVersionName = VersioningProfile.addVersion(defaultStreamName);
		middleVersionLength = randBytes.nextInt(MAX_FILE_SIZE);
		middleVersionMaxSegment = (middleVersionLength == 0) ? 0 :
			((middleVersionLength + SegmentationProfile.DEFAULT_BLOCKSIZE - 1) / SegmentationProfile.DEFAULT_BLOCKSIZE) - 1;
		middleVersionDigest = writeFile(flosser, middleVersionName, middleVersionLength, randBytes);

		latestVersionName = VersioningProfile.addVersion(defaultStreamName);
		latestVersionLength = randBytes.nextInt(MAX_FILE_SIZE);
		latestVersionMaxSegment = (latestVersionLength == 0) ? 0 :
			((latestVersionLength + SegmentationProfile.DEFAULT_BLOCKSIZE - 1) / SegmentationProfile.DEFAULT_BLOCKSIZE) - 1;
		latestVersionDigest = writeFile(flosser, latestVersionName, latestVersionLength, randBytes);

		for (int i=0; i < problematicLengths.length; ++i) {
			problematicNames[i] = VersioningProfile.addVersion(
					testHelper.getClassChildName("LengthTest-" + problematicLengths[i]));
			problematicDigests[i] = writeFile(flosser, problematicNames[i], problematicLengths[i], randBytes);
		}
		flosser.stop();
	}

	@AfterClass
	public static void cleanupAfterClass() {
		outputHandle.close();
		inputHandle.close();
		inputHandle2.close();
	}

	/**
	 * @param completeName
	 * @param fileLength
	 * @param randBytes
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static byte [] writeFile(Flosser flosser, ContentName completeName, int fileLength, Random randBytes) throws IOException, NoSuchAlgorithmException {
		flosser.handleNamespace(completeName);
		NDNOutputStream stockOutputStream = new NDNOutputStream(completeName, outputHandle);

		DigestOutputStream digestStreamWrapper = new DigestOutputStream(stockOutputStream, MessageDigest.getInstance("SHA1"));
		byte [] bytes = new byte[BUF_SIZE];
		int elapsed = 0;
		int nextBufSize = 0;

		Log.info(Log.FAC_TEST, "Writing file: " + completeName + " bytes: " + fileLength);

		final double probFlush = .3;

		while (elapsed < fileLength) {
			nextBufSize = ((fileLength - elapsed) > BUF_SIZE) ? BUF_SIZE : (fileLength - elapsed);
			randBytes.nextBytes(bytes);
			digestStreamWrapper.write(bytes, 0, nextBufSize);
			elapsed += nextBufSize;
			Log.info(completeName + " wrote " + elapsed + " out of " + fileLength + " bytes.");
			if (randBytes.nextDouble() < probFlush) {
				Log.info(Log.FAC_TEST, "Flushing buffers.");

				digestStreamWrapper.flush();
			}
		}
		digestStreamWrapper.close();

		Log.info(Log.FAC_TEST, "Finished writing file " + completeName);
		return digestStreamWrapper.getMessageDigest().digest();
	}

	public static byte [] readFile(InputStream inputStream, int fileLength) throws IOException {

		DigestInputStream dis = null;
		try {
			dis = new DigestInputStream(inputStream, MessageDigest.getInstance("SHA1"));
		} catch (NoSuchAlgorithmException e) {
			Log.severe(Log.FAC_TEST, "No SHA1 available!");
			Assert.fail("No SHA1 available!");
		}
		int elapsed = 0;
		int read = 0;
		byte [] bytes = new byte[BUF_SIZE];
		while (elapsed < fileLength) {
			read = dis.read(bytes);
			if (read < 0) {
				Log.info(Log.FAC_TEST, "EOF read at " + elapsed + " bytes out of " + fileLength);
				break;
			} else if (read == 0) {
				Log.info(Log.FAC_TEST, "0 bytes read at " + elapsed + " bytes out of " + fileLength);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {

				}
			}
			elapsed += read;
			Log.info(Log.FAC_TEST, " read " + elapsed + " bytes out of " + fileLength);
		}
		return dis.getMessageDigest().digest();
	}

	@Test
	public void testNDNVersionedInputStreamContentNameLongPublisherKeyIDNDNLibrary() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentNameLongPublisherKeyIDNDNLibrary");

		// we can make a new handle; as long as we don't use the outputHandle it should work
		Log.info(Log.FAC_TEST, "first: "+firstVersionName);
		Log.info(Log.FAC_TEST, "middle: "+middleVersionName);
		Log.info(Log.FAC_TEST, "latest: "+latestVersionName);

		NDNVersionedInputStream vfirst =
			new NDNVersionedInputStream(firstVersionName,
					((3 > firstVersionMaxSegment) ? firstVersionMaxSegment : 3L), outputHandle.getDefaultPublisher(), inputHandle);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(defaultStreamName,
				((3 > latestVersionMaxSegment) ? latestVersionMaxSegment : 3L), outputHandle.getDefaultPublisher(), inputHandle);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentNameLongPublisherKeyIDNDNLibrary");
	}

	@Test
	public void testNDNVersionedInputStreamContentNamePublisherKeyIDNDNLibrary() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentNamePublisherKeyIDNDNLibrary");

		Log.info(Log.FAC_TEST, "firstVersionName: "+firstVersionName);
		Log.info(Log.FAC_TEST, "middle: "+middleVersionName);
		Log.info(Log.FAC_TEST, "latest: "+latestVersionName);
		Log.info(Log.FAC_TEST, "defaultStreamName: "+defaultStreamName);

		// we can make a new handle; as long as we don't use the outputHandle it should work
		NDNVersionedInputStream vfirst = new NDNVersionedInputStream(firstVersionName, outputHandle.getDefaultPublisher(), inputHandle);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(defaultStreamName, outputHandle.getDefaultPublisher(), inputHandle2);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentNamePublisherKeyIDNDNLibrary");

	}

	@Test
	public void testNDNVersionedInputStreamContentName() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentName");

		// we can make a new handle; as long as we don't use the outputHandle it should work
		NDNVersionedInputStream vfirst = new NDNVersionedInputStream(firstVersionName);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(defaultStreamName);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentName");
	}

	@Test
	public void testNDNVersionedInputStreamContentNameNDNLibrary() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentNameNDNLibrary");

		NDNVersionedInputStream vfirst = new NDNVersionedInputStream(firstVersionName, inputHandle);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(defaultStreamName, inputHandle);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentNameNDNLibrary");
	}

	protected void testArgumentRunner(NDNVersionedInputStream vfirst,
									  NDNVersionedInputStream vlatest) throws Exception {
		Assert.assertEquals(vfirst.getBaseName(), firstVersionName);
		Assert.assertEquals(VersioningProfile.cutTerminalVersion(vfirst.getBaseName()).first(), defaultStreamName);
		byte b = (byte)vfirst.read();
		if (b != 0x00) {
		}
		Assert.assertEquals(VersioningProfile.getLastVersionAsTimestamp(firstVersionName),
				VersioningProfile.getLastVersionAsTimestamp(vfirst.getBaseName()));
		Assert.assertEquals(VersioningProfile.getLastVersionAsTimestamp(firstVersionName),
				vfirst.getVersionAsTimestamp());

		Log.info(Log.FAC_TEST, "Opened stream on latest version, expected: " + latestVersionName + " got: " +
				vlatest.getBaseName());
		b = (byte)vlatest.read();
		Log.info(Log.FAC_TEST, "Post-read: Opened stream on latest version, expected: " + latestVersionName + " got: " +
				vlatest.getBaseName());
		Log.info(Log.FAC_TEST, "versions as TS: "+VersioningProfile.getLastVersionAsTimestamp(latestVersionName)+" "+vlatest.getVersion());

		Assert.assertEquals(vlatest.getBaseName(), latestVersionName);
		Assert.assertEquals(VersioningProfile.cutTerminalVersion(vlatest.getBaseName()).first(), defaultStreamName);
		Assert.assertEquals(VersioningProfile.getLastVersionAsLong(latestVersionName),
				VersioningProfile.getLastVersionAsLong(vlatest.getBaseName()));
		Assert.assertEquals(VersioningProfile.getLastVersionAsTimestamp(latestVersionName),
				VersioningProfile.getLastVersionAsTimestamp(vlatest.getBaseName()));
		Assert.assertEquals(VersioningProfile.getLastVersionAsTimestamp(latestVersionName),
				vlatest.getVersionAsTimestamp());
	}

	@Test
	public void testNDNVersionedInputStreamContentNameInt() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentNameInt");

		// we can make a new handle; as long as we don't use the outputHandle it should work
		NDNVersionedInputStream vfirst =
			new NDNVersionedInputStream(firstVersionName, Math.min(4L, firstVersionMaxSegment), null);
		NDNVersionedInputStream vlatest =
			new NDNVersionedInputStream(defaultStreamName, Math.min(4L, latestVersionMaxSegment), null);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentNameInt");
	}

	@Test
	public void testNDNVersionedInputStreamContentObjectNDNLibrary() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testNDNVersionedInputStreamContentObjectNDNLibrary");

		// we can make a new handle; as long as we don't use the outputHandle it should work
		ContentObject firstVersionBlock = inputHandle.get(firstVersionName, SystemConfiguration.getDefaultTimeout());
		ContentObject latestVersionBlock = reader.get(Interest.last(defaultStreamName, defaultStreamName.count(), null), SystemConfiguration.getDefaultTimeout());
		NDNVersionedInputStream vfirst = new NDNVersionedInputStream(firstVersionBlock, null, inputHandle);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(latestVersionBlock, null, inputHandle);
		testArgumentRunner(vfirst, vlatest);

		Log.info(Log.FAC_TEST, "Completed testNDNVersionedInputStreamContentObjectNDNLibrary");
	}

	@Test
	public void testReadByteArray() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testReadByteArray");

		// Test other forms of read in superclass test.
		NDNVersionedInputStream vfirst = new NDNVersionedInputStream(firstVersionName, inputHandle);
		byte [] readDigest = readFile(vfirst, firstVersionLength);
		Assert.assertArrayEquals(firstVersionDigest, readDigest);
		NDNVersionedInputStream vmiddle = new NDNVersionedInputStream(middleVersionName, inputHandle);
		readDigest = readFile(vmiddle, middleVersionLength);
		Assert.assertArrayEquals(middleVersionDigest, readDigest);
		NDNVersionedInputStream vlatest = new NDNVersionedInputStream(defaultStreamName, inputHandle);
		readDigest = readFile(vlatest, latestVersionLength);
		Assert.assertArrayEquals(latestVersionDigest, readDigest);

		Log.info(Log.FAC_TEST, "Completed testReadByteArray");
	}

	@Test
	public void testReadProblematicLengths() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testReadProblematicLengths");

		NDNVersionedInputStream vstream;
		byte [] readDigest;

		for (int i=0; i < problematicLengths.length; ++i) {
			vstream = new NDNVersionedInputStream(problematicNames[i], inputHandle);
			readDigest = readFile(vstream, problematicLengths[i]);
			Assert.assertArrayEquals("Stream " + i + " failed to match, length " + problematicLengths[i],
									problematicDigests[i], readDigest);
		}

		Log.info(Log.FAC_TEST, "Completed testReadProblematicLengths");
	}
}
