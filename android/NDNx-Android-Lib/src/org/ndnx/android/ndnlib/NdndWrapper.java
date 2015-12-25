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
 * This is the "client side" interface to the ndnd service.
 */
public final class NdndWrapper extends NDNxWrapper {
	public static final String CLASS_TAG = "NDNxNDNdWrapper";
	
	public static final String OPTION_LOG_LEVEL_DEFAULT = "1";
	
	public enum NDND_OPTIONS {
		NDND_KEYSTORE_DIRECTORY,
		NDND_DEBUG,
		NDN_LOCAL_SOCKNAME,
		NDND_CAP,
		NDND_DATA_PAUSE_MICROSEC,
		NDND_TRYFIB,
		NDN_LOCAL_PORT
	}
	
	public NdndWrapper(Context ctx) {
		super(ctx);
		TAG = CLASS_TAG;
		Log.d(TAG,"Initializing");
		serviceClassName = "org.ndnx.android.services.ndnd.NdndService";
		serviceName = "org.ndnx.android.service.ndnd.SERVICE";
		setOption(NDND_OPTIONS.NDND_DEBUG, OPTION_LOG_LEVEL_DEFAULT);
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
	
	public void setOption(NDND_OPTIONS key, String value) {
		setOption(key.name(), value);
	}
}