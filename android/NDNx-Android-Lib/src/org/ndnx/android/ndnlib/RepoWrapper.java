/*
 * NDNx Android Helper Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010, 2011 Palo Alto Research Center, Inc.
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

package org.ndnx.android.ndnlib;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This is the "client side" interface to the repository service.
 */
public final class RepoWrapper extends NDNxWrapper {
	private static final String CLASS_TAG = "NDNxRepoWrapper";
	
	public static final String OPTION_LOG_LEVEL_DEFAULT = "WARNING";
	
	public enum REPO_OPTIONS { /* repo1 */
		REPO_DIRECTORY,
		REPO_DEBUG,
		REPO_LOCAL,
		REPO_GLOBAL,
		REPO_NAMESPACE
	}
	
	public enum NDNR_OPTIONS { /* repo2 */
		NDNR_DEBUG,
		NDNR_DIRECTORY,
		NDNR_GLOBAL_PREFIX,
		NDNR_BTREE_MAX_FANOUT,
		NDNR_BTREE_MAX_LEAF_ENTRIES,
		NDNR_BTREE_MAX_NODE_BYTES,
		NDNR_BTREE_NODE_POOL,
		NDNR_CONTENT_CACHE,
		NDNR_MIN_SEND_BUFSIZE,
		NDNR_PROTO,
		NDNR_LISTEN_ON,
		NDNR_STATUS_PORT
	}
	
	public enum NDNS_OPTIONS { /* sync */
		NDNS_DEBUG,
		NDNS_ENABLE,
		NDNS_REPO_STORE,
		NDNS_STABLE_ENABLED,
		NDNS_FAUX_ERROR,
		NDNS_HEARTBEAT_MICROS,
		NDNS_ROOT_ADVISE_FRESH,
		NDNS_ROOT_ADVISE_LIFETIME,
		NDNS_NODE_FETCH_LIFETIME,
		NDNS_MAX_FETCH_BUSY,
		NDNS_MAX_COMPARES_BUSY,
		NDNS_NOTE_ERR,
		NDNS_SYNC_SCOPE
	}
	
	public RepoWrapper(Context ctx) {
		super(ctx);
		TAG = CLASS_TAG;
		Log.d(TAG,"Initializing");
		serviceClassName = "org.ndnx.android.services.repo.RepoService";
		serviceName = "org.ndnx.android.service.repo.SERVICE";
		// setOption(REPO_OPTIONS.REPO_DEBUG, OPTION_LOG_LEVEL_DEFAULT);
	}
	
	@Override
	protected Intent getBindIntent() {
		Intent i = new Intent(serviceName);
		return i;
	}

	@Override
	protected Intent getStartIntent() {
		Intent i = new Intent(serviceName);
		fillIntentOptions(i);
		return i;
	}
	
	public void setOption(REPO_OPTIONS key, String value) {
		setOption(key.name(), value);
	}
	
	public void setOption(NDNR_OPTIONS key, String value) {
		setOption(key.name(), value);
	}
	
	public void setOption(NDNS_OPTIONS key, String value) {
		setOption(key.name(), value);
	}
}
