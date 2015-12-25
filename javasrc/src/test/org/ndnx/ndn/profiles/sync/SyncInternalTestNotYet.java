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

import java.util.TreeSet;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.impl.sync.NodeBuilder;
import org.ndnx.ndn.impl.sync.ProtocolBasedSyncMonitor;
import org.ndnx.ndn.impl.sync.SyncHashCache;
import org.ndnx.ndn.impl.sync.SyncNodeCache;
import org.ndnx.ndn.impl.sync.SyncTreeEntry;
import org.ndnx.ndn.io.content.ConfigSlice;
import org.ndnx.ndn.io.content.SyncNodeComposite;
import org.ndnx.ndn.io.content.SyncNodeComposite.SyncNodeElement;
import org.ndnx.ndn.io.content.SyncNodeComposite.SyncNodeType;
import org.ndnx.ndn.profiles.sync.Sync;
import org.ndnx.ndn.protocol.Component;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SyncInternalTestNotYet extends NDNTestBase implements NDNContentHandler {
	
	public static NDNTestHelper testHelper = new NDNTestHelper(SyncInternalTestNotYet.class);
	ContentName prefix;
	ContentName topo;
	ContentObject receivedNode = null;
	SyncNodeCache cache = new SyncNodeCache();
	SyncHashCache shc = new SyncHashCache();
	
	@Before
	public void setUpNameSpace() {
		prefix = testHelper.getTestNamespace("ndnSyncInternalTest");
		topo = testHelper.getTestNamespace("topoPrefix");
		Log.fine(Log.FAC_TEST, "setting up namespace for sync test  data: {0} syncControlTraffic: {1}", prefix, topo);
	}
	
	/**
	 * Test to make sure that our internal build of nodes builds nodes correctly
	 * 
	 * TODO This test is supposed to ensure that nodes are built the same way C side sync does but
	 * we aren't doing that yet.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSyncNodeBuild() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testSyncNodeBuild");

		ContentName prefix1;
		prefix1 = prefix.append("slice1");
		ConfigSlice slice1 = ConfigSlice.checkAndCreate(topo, prefix1, null, putHandle);
		Assert.assertTrue("Didn't create slice: " + prefix1, slice1 != null);
		
		//the slice should be written..  now save content and get a callback.
		Log.fine(Log.FAC_TEST, "writing out file: {0}", prefix1);
		
		// Write a 100 block file to test a true sync tree
		SyncTestCommon.writeFile(prefix1, false, SystemConfiguration.BLOCK_SIZE * 100, putHandle);
		
		SyncNodeComposite repoNode = SyncTestCommon.getRootAdviseNode(slice1, getHandle);
		Assert.assertTrue(null != repoNode);
		
		TreeSet<ContentName> names = new TreeSet<ContentName>();
		for (SyncNodeElement sne : repoNode.getRefs()) {
			if (sne.getType() == SyncNodeType.LEAF) {
				names.add(sne.getName());
			}
			if (sne.getType() == SyncNodeType.HASH) {
				synchronized (this) {
					receivedNode = null;
					ProtocolBasedSyncMonitor.requestNode(slice1, sne.getData(), getHandle, this);
					wait(SystemConfiguration.EXTRA_LONG_TIMEOUT);
				}
				assert(receivedNode != null);
				SyncNodeComposite snc = new SyncNodeComposite();
				snc.decode(receivedNode.content());
				SyncNodeComposite.decodeLogging(snc);
				for (SyncNodeElement tsne : snc.getRefs()) {
					Assert.assertTrue(tsne.getType() == SyncNodeType.LEAF);
					names.add(tsne.getName());
				}
			}
		}
		
		NodeBuilder nb = new NodeBuilder();
		SyncTreeEntry testNode = nb.newNode(names, shc, cache);
		Assert.assertTrue(testNode.getNode().equals(repoNode));
		
		Log.info(Log.FAC_TEST, "Completed testSyncNodeBuild");
	}

	public Interest handleContent(ContentObject data, Interest interest) {
		ContentName name = data.name();

		int hashComponent = name.containsWhere(Sync.SYNC_NODE_FETCH_MARKER);
		Assert.assertTrue(hashComponent > 0 && name.count() > (hashComponent + 1));
		byte[] hash = name.component(hashComponent + 2);
		Log.fine(Log.FAC_TEST, "Saw data from nodefind in test: hash: {0}", Component.printURI(hash));
		receivedNode = data;
		synchronized (this) {
			notify();
		}
		return null;
	}
}
