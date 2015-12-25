/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2012-2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.profiles.sync;

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Random;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.security.crypto.NDNDigestHelper;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.RepositoryFileOutputStream;
import org.ndnx.ndn.io.content.ConfigSlice;
import org.ndnx.ndn.io.content.SyncNodeComposite;
import org.ndnx.ndn.profiles.sync.Sync;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;

public class SyncTestCommon {
	public static final int BUF_SIZE = 1024;
	public static int maxBytes = 10 * BUF_SIZE;

	public static int writeFile(ContentName name, boolean random, int size, NDNHandle handle) throws Exception {
		int segmentsToWrite = 0;
		RepositoryFileOutputStream rfos = new RepositoryFileOutputStream(name.append("randomFile"), handle);
		DigestOutputStream dos = new DigestOutputStream(rfos, MessageDigest.getInstance(NDNDigestHelper.DEFAULT_DIGEST_ALGORITHM));

		byte [] buf = new byte[BUF_SIZE];
		int count = 0;
		int towrite = 0;
		Random rand = new Random();
		int bytes = 0;
		if (random) {
			bytes = rand.nextInt(maxBytes) + 1;
		} else
			bytes = size;
		double block = (double)bytes/(double)SystemConfiguration.BLOCK_SIZE;
		segmentsToWrite = (int) (Math.ceil(block) + 1);
		Log.fine(Log.FAC_TEST, "bytes: {0} block size: {1} div: {2} ceil: {3}", bytes, SystemConfiguration.BLOCK_SIZE, block, (int)Math.ceil(block));
		Log.fine(Log.FAC_TEST, "will write out a {0} byte file, will have {1} segments (1 is a header)", bytes, segmentsToWrite);
		while (count < bytes) {
			rand.nextBytes(buf);
			towrite = ((bytes - count) > buf.length) ? buf.length : (bytes - count);
			dos.write(buf, 0, towrite);
			count += towrite;
		}
		dos.flush();
		dos.close();
		Log.info(Log.FAC_TEST, "Wrote file to repository: {0} with {1} segments", rfos.getBaseName(), segmentsToWrite);
		return segmentsToWrite;
	}
	
	public static SyncNodeComposite getRootAdviseNode(ConfigSlice slice, NDNHandle handle) throws IOException {
		ContentName rootAdvise = new ContentName(slice.topo, Sync.SYNC_ROOT_ADVISE_MARKER, slice.getHash());
		Interest interest = new Interest(rootAdvise);
		interest.scope(1);
		ContentObject obj = handle.get(interest, SystemConfiguration.EXTRA_LONG_TIMEOUT);
		if (null == obj)
			return null;
		SyncNodeComposite snc = new SyncNodeComposite();
		snc.decode(obj.content());
		SyncNodeComposite.decodeLogging(snc);
		return snc;
	}
}
