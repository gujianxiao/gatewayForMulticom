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

package org.ndnx.ndn.io.content;

import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.impl.support.ConcurrencyUtils.Waiter;
import org.ndnx.ndn.io.content.NDNNetworkObject;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.io.content.Collection;
import org.ndnx.ndn.io.content.Link;
import org.ndnx.ndn.io.content.LinkAuthenticator;
import org.ndnx.ndn.protocol.NDNTime;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.PublisherID;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.utils.Flosser;

/**
 * Common code between NDNObjectTests
 */
public class NDNNetworkObjectTestBase extends NDNTestBase {
	static final int UPDATE_TIMEOUT = 5000;
	static final int MAX_REPO_WAIT = 1000;
	static final int REPO_WAIT_INCR = 100;

	static String stringObjName = "StringObject";
	static String collectionObjName = "CollectionObject";
	static String prefix = "CollectionObject-";
	static ContentName [] ns = null;

	static public byte [] contenthash1 = new byte[32];
	static public byte [] contenthash2 = new byte[32];
	static public byte [] publisherid1 = new byte[32];
	static public byte [] publisherid2 = new byte[32];
	static PublisherID pubID1 = null;
	static PublisherID pubID2 = null;
	static int NUM_LINKS = 15;
	static LinkAuthenticator [] las = new LinkAuthenticator[NUM_LINKS];
	static Link [] lrs = null;

	static Collection small1;
	static Collection small2;
	static Collection empty;
	static Collection big;
	static String [] numbers = new String[]{"ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN"};
	static NDNHandle handle;

	static Flosser flosser = null;

	public <T> NDNTime saveAndLog(String name, NDNNetworkObject<T> ecd, NDNTime version, T data) throws IOException {
		NDNTime oldVersion = ecd.getVersion();
		ecd.save(version, data);
		Log.info(Log.FAC_TEST, name + " Saved " + name + ": " + ecd.getVersionedName() + " (" + ecd.getVersion() + ", updated from " + oldVersion + ")" +  " gone? " + ecd.isGone() + " data: " + ecd);
		return ecd.getVersion();
	}

	public <T> NDNTime saveAsGoneAndLog(String name, NDNNetworkObject<T> ecd) throws IOException {
		NDNTime oldVersion = ecd.getVersion();
		ecd.saveAsGone();
		Log.info(Log.FAC_TEST, "Saved " + name + ": " + ecd.getVersionedName() + " (" + ecd.getVersion() + ", updated from " + oldVersion + ")" +  " gone? " + ecd.isGone() + " data: " + ecd);
		return ecd.getVersion();
	}

	public NDNTime waitForDataAndLog(String name, NDNNetworkObject<?> ecd) throws IOException {
		ecd.waitForData();
		Log.info(Log.FAC_TEST, "Initial read " + name + ", name: " + ecd.getVersionedName() + " (" + ecd.getVersion() +")" +  " gone? " + ecd.isGone() + " data: " + ecd);
		return ecd.getVersion();
	}

	public NDNTime updateAndLog(String name, NDNNetworkObject<?> ecd, ContentName updateName) throws IOException {
		if ((null == updateName) ? ecd.update() : ecd.update(updateName, null))
			Log.info(Log.FAC_TEST, "Updated " + name + ", to name: " + ecd.getVersionedName() + " (" + ecd.getVersion() +")" +  " gone? " + ecd.isGone() + " data: " + ecd);
		else
			Log.info(Log.FAC_TEST, "No update found for " + name + ((null != updateName) ? (" at name " + updateName) : "") + ", still: " + ecd.getVersionedName() + " (" + ecd.getVersion() +")" +  " gone? " + ecd.isGone() + " data: " + ecd);
		return ecd.getVersion();
	}

	public void doWait(NDNStringObject cso, NDNTime t) throws Exception {
		new Waiter(UPDATE_TIMEOUT) {
			@Override
			protected boolean check(Object o, Object check) throws Exception {
				return ((NDNStringObject)o).getVersion().equals(check);
			}
		}.wait(cso, t);
	}

}
