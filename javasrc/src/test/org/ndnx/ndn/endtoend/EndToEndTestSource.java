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

package org.ndnx.ndn.endtoend;


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.SignatureException;

import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNWriter;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.junit.Test;


/**
 * Part of the end to end test infrastructure.
 * NOTE: This test requires ndnd to be running and complementary sink process
 */
public class EndToEndTestSource extends BaseLibrarySource implements NDNInterestHandler {
	protected NDNWriter _writer;
	
	@Test
	public void source() throws Throwable {
		Log.info(Log.FAC_TEST, "Starting source");
		sync();
		puts();
		server();
		Log.info(Log.FAC_TEST, "Completed source");
	}
	
	/**
	 * This does a simple sync with EndToEndTestSink so that we know its started up before we
	 * start counting on it doing other things.
	 * @throws MalformedContentNameStringException
	 * @throws IOException
	 * @throws SignatureException
	 */
	public void sync() throws IOException, MalformedContentNameStringException, SignatureException {
		ContentName syncBaseName = ContentName.fromNative("/BaseLibraryTest/sync");
		ContentName syncReturnName = ContentName.fromNative("/BaseLibraryTest/sync/return");
		NDNWriter writer = new NDNWriter(syncBaseName, handle);
		writer.setTimeout(100000);
		ContentName syncName = new ContentName(syncBaseName, new Integer(rand.nextInt(5000)).toString());		
		writer.put(syncName, "Hi Sink!");
		handle.get(syncReturnName, SystemConfiguration.NO_TIMEOUT);
	}

	public void puts() throws Throwable {
		assert(count <= Byte.MAX_VALUE);
		Log.info(Log.FAC_TEST, "Put sequence started");
		NDNWriter writer = new NDNWriter("/BaseLibraryTest", handle);
		writer.setTimeout(5000);
		for (int i = 0; i < count; i++) {
			Thread.sleep(rand.nextInt(50));
			byte[] content = getRandomContent(i);
			ContentName putResult = writer.put(ContentName.fromNative("/BaseLibraryTest/gets/" + new Integer(i).toString()), content);
			Log.info(Log.FAC_TEST, "Put " + i + " done: " + content.length + " content bytes");
			checkPutResults(putResult);
		}
		writer.close();
		Log.info(Log.FAC_TEST, "Put sequence finished");
	}
	
	public void server() throws Throwable {
		Log.info("PutServer started");
		name = ContentName.fromNative("/BaseLibraryTest/");
		_writer = new NDNWriter(name, handle);
		_writer.setTimeout(5000);
		handle.registerFilter(name, this);
		// Block on semaphore until enough data has been received
		sema.acquire();
		handle.unregisterFilter(name, this);
		if (null != error) {
			throw error;
		}
	}
	
	public synchronized boolean handleInterest(Interest interest) {
		boolean result = false;
		try {
			if (next >= count) {
				return false;
			}
			assertTrue(name.isPrefixOf(interest.name()));
			byte[] content = getRandomContent(next);
			ContentName putResult = _writer.put(ContentName.fromNative("/BaseLibraryTest/server/" + new Integer(next).toString()), content);
			result = true;
			Log.info(Log.FAC_TEST, "Put " + next + " done: " + content.length + " content bytes");
			checkPutResults(putResult);
			next++;
			if (next >= count) {
				sema.release();
			}
		} catch (Throwable e) {
			error = e;
		}
		return result;
	}
}
