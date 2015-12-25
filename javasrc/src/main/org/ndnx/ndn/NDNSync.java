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
package org.ndnx.ndn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.impl.sync.ProtocolBasedSyncMonitor;
import org.ndnx.ndn.impl.sync.SyncMonitor;
import org.ndnx.ndn.impl.sync.SyncNodeCache;
import org.ndnx.ndn.io.content.ConfigSlice;
import org.ndnx.ndn.io.content.ConfigSlice.Filter;
import org.ndnx.ndn.protocol.Component;
import org.ndnx.ndn.protocol.ContentName;

public class NDNSync {
	
	public static final int SYNC_HASH_MAX_LENGTH = 40;
	public static final int NODE_SPLIT_TRIGGER = 4000; // bytes
	public static final int HASH_SPLIT_TRIGGER = 17; // bytes
	
	protected SyncMonitor syncMon = null;
	
	/**
	 * 
	 * @param handle
	 * @param topo
	 * @param prefix
	 * @param filters
	 * @param startHash
	 * @param startName Start enumerating names after this one. NOTE - this is currently implemented in a
	 * 				    simplistic way which may not work in cases of disjoint hashes.
	 * @param syncCallback
	 * @return
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	public ConfigSlice startSync(NDNHandle handle, ContentName topo, ContentName prefix, Collection<ContentName> filters, byte[] startHash, ContentName startName, NDNSyncHandler syncCallback) throws IOException, ConfigurationException{
		if (handle == null)
			handle = NDNHandle.getHandle();
		Collection<Filter> f = new ArrayList<Filter>();
		if (filters!=null) {
			for (ContentName cn: filters)
				f.add(new Filter(cn));
		}
		try {
			ConfigSlice slice = ConfigSlice.checkAndCreate(topo, prefix, f, handle);
			if (syncMon == null)
				syncMon = new ProtocolBasedSyncMonitor(handle);
			syncMon.registerCallback(syncCallback, slice, startHash, startName);
			if (Log.isLoggable(Log.FAC_SYNC, Level.INFO))
				Log.info("Started sync with topo: {0} and prefix: {1}", topo, prefix);
			return slice;
		} catch (Exception e) {
			Log.warning(Log.FAC_REPO, "Error when starting sync for slice with prefix: {0} {1} {2}", prefix, null == startHash ? "" : ("with starthash : " + Component.printURI(startHash)),
								null == startName ? "" : ("with startName: " + startName));
			throw new IOException("Unable to create sync slice: "+e.getMessage());
		}
	}
	
	public ConfigSlice startSync(NDNHandle handle, ContentName topo, ContentName prefix, NDNSyncHandler syncCallback) throws IOException, ConfigurationException{
		return startSync(handle, topo, prefix, null, null, null, syncCallback);
	}
	
	public void stopSync(NDNSyncHandler syncHandler, ConfigSlice syncSlice) throws IOException{
		//will unregister the callback here
		syncMon.removeCallback(syncHandler, syncSlice);
	}
	
	public void shutdown(ConfigSlice slice) {
		syncMon.shutdown(slice);
	}
	
	public SyncNodeCache getNodeCache(ConfigSlice slice) {
		return syncMon.getNodeCache(slice);
	}
}
