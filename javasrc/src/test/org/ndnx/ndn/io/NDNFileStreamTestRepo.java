/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011-2013 Palo Alto Research Center, Inc.
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
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.ndnx.ndn.impl.security.crypto.NDNDigestHelper;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNFileInputStream;
import org.ndnx.ndn.io.RepositoryFileOutputStream;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.TestUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test class for NDNFileStream; tests writing file streams to a repository.
 */
public class NDNFileStreamTestRepo extends NDNTestBase {

	static Random random = new Random();

	/**
	 * Handle naming for the test
	 */
	static NDNTestHelper testHelper = new NDNTestHelper(NDNFileStreamTestRepo.class);

	static final int BUF_SIZE = 1024;

	public static class CountAndDigest {
		int _count;
		byte [] _digest;

		public CountAndDigest(int count, byte [] digest) {
			_count = count;
			_digest = digest;
		}

		public int count() { return _count; }
		public byte [] digest() { return _digest; }
	}

	@Test
	public void testRepoFileOutputStream() throws Exception {
		Log.info(Log.FAC_TEST, "Started testRepoFileOutputStream");

		int fileSize = random.nextInt(50000);
		ContentName fileName = new ContentName(testHelper.getTestNamespace("testRepoFileOutputStream"), "outputFile.bin");

		// Write to a repo. Read it back in. See if repo gets the header.
		RepositoryFileOutputStream rfos = new RepositoryFileOutputStream(fileName, putHandle);
		byte [] digest = writeRandomFile(fileSize, rfos);
		Log.info(Log.FAC_TEST, "Wrote file to repository: " + rfos.getBaseName());

		NDNFileInputStream fis = new NDNFileInputStream(fileName, getHandle);
		TestUtils.checkFile(getHandle, fis);
		CountAndDigest readDigest = readRandomFile(fis);

		Log.info(Log.FAC_TEST, "Read file from repository: " + fis.getBaseName() + " has header? " +
				fis.hasHeader());
		if (!fis.hasHeader()) {
			Log.info(Log.FAC_TEST, "No header yet, waiting..");
			fis.waitForHeader();
		}
		Assert.assertTrue(fis.hasHeader());
		Log.info(Log.FAC_TEST, "Read file size: " + readDigest.count() + " written size: " + fileSize + " header file size " + fis.header().length());
		Assert.assertEquals(readDigest.count(), fileSize);
		Assert.assertEquals(fileSize, fis.header().length());
		Log.info(Log.FAC_TEST, "Read digest: " + DataUtils.printBytes(readDigest.digest()) + " wrote digest: " + DataUtils.printBytes(digest));
		Assert.assertArrayEquals(digest, readDigest.digest());

		NDNFileInputStream fis2 = new NDNFileInputStream(rfos.getBaseName(), getHandle);
		CountAndDigest readDigest2 = readRandomFile(fis2);

		Log.info(Log.FAC_TEST, "Read file from repository again: " + fis2.getBaseName() + " has header? " +
				fis2.hasHeader());
		if (!fis2.hasHeader()) {
			Log.info(Log.FAC_TEST, "No header yet, waiting..");
			fis2.waitForHeader();
		}
		Assert.assertTrue(fis2.hasHeader());
		Log.info(Log.FAC_TEST, "Read file size: " + readDigest2.count() + " written size: " + fileSize + " header file size " + fis.header().length());
		Assert.assertEquals(readDigest2.count(), fileSize);
		Assert.assertEquals(fileSize, fis2.header().length());
		Log.info(Log.FAC_TEST, "Read digest: " + DataUtils.printBytes(readDigest2.digest()) + " wrote digest: " + DataUtils.printBytes(digest));
		Assert.assertArrayEquals(digest, readDigest2.digest());

		Log.info(Log.FAC_TEST, "Completed testRepoFileOutputStream");
	}

	public static byte [] writeRandomFile(int bytes, OutputStream out) throws IOException {
		try {
			DigestOutputStream dos = new DigestOutputStream(out, MessageDigest.getInstance(NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM));

			byte [] buf = new byte[BUF_SIZE];
			int count = 0;
			int towrite = 0;
			while (count < bytes) {
				random.nextBytes(buf);
				towrite = ((bytes - count) > buf.length) ? buf.length : (bytes - count);
				dos.write(buf, 0, towrite);
				count += towrite;
			}
			dos.flush();
			dos.close();
			return dos.getMessageDigest().digest();

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Cannot find digest algorithm: " + NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
		}
	}

	public static CountAndDigest readRandomFile(InputStream in) throws IOException {
		try {
			DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM));

			byte [] buf = new byte[BUF_SIZE];
			int count = 0;
			int read = 0;
			while (read >= 0) {
				read = dis.read(buf);
				if (read > 0)
				 count += read;
			}
			return new CountAndDigest(count, dis.getMessageDigest().digest());

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Cannot find digest algorithm: " + NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
		}
	}
}
