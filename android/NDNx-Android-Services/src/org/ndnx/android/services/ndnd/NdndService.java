/*
 * NDNx Android Services
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010, 2011 Palo Alto Research Center, Inc.
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

package org.ndnx.android.services.ndnd;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map.Entry;
import java.util.HashMap;

import org.ndnx.android.ndnlib.NDNxServiceStatus.SERVICE_STATUS;
import org.ndnx.android.ndnlib.NdndWrapper.NDND_OPTIONS;
import org.ndnx.android.services.NDNxService;
import org.ndnx.ndn.impl.security.keys.BasicKeyManager;
import org.ndnx.android.ndnlib.NDNxLibraryCheck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.util.Log;

/**
 * NDNxService specialization for ndnd.
 * 
 * The NDND keystore directory is created MODE_PRIVATE.  This only
 * works if it is on the internal storage, not an sdcard.
 * 
 * The ndnd unix domain socket is created in the keystore directory, so
 * obviously that cannot be used with MODE_PRIVATE.  At the present,
 * nothing using the unix domain socket on Android. 
 */
public final class NdndService extends NDNxService {
	public static final String CLASS_TAG = "NDNxNDNdService";
	
	private String KEYSTORE_NAME = ".ndnd_keystore_";
	private final static char [] KEYSTORE_PASS = "\010\043\103\375\327\237\152\351\155".toCharArray();
	
	private final static String OPTION_NDND_CAP_DEFAULT = "500";
	private final static String OPTION_NDN_PORT_DEFAULT = "9695";
	
	protected static final String [] libs = { "controller" };
	
	public NdndService(){
		TAG=CLASS_TAG;
		
		// make sure libraries are loaded
		try {
			for( int i = 0; i < libs.length; i++ ) {
				System.loadLibrary(libs[i]);
			}
		} catch(Throwable e) {
			// Need to clean this up to catch each exception, may be that we can recover or handle effectively
			// Why in the world would we not handle this?  Nothing will work if we have a runtime 
			// exception loading libraries
			e.printStackTrace();
		}
	}

	protected void onStartService(Intent intent) {
		Log.d(TAG, "Starting");
		boolean isPrefSet = false;

		// Get all the NDND options from the intent 
		// If no option is found on intent, look in System properties
		// If no system property is set, fallback to preferences
		// And while settings OPTIONS, set preferences
		SharedPreferences.Editor prefsEditor = mNDNxServicePrefs.edit();
        
        
        if (intent != null) {
			for( NDND_OPTIONS opt : NDND_OPTIONS.values() ) {
				if(! intent.hasExtra(opt.name())){
					continue;
				}
				String s = intent.getStringExtra( opt.name() );
				if( null == s ) 
					s = System.getProperty(opt.name());
					Log.d(TAG,"setting option " + opt.name() + " = " + s);
				if( s != null ) {
					options.put(opt.name(), s);
					isPrefSet = true;
					prefsEditor.putString(opt.name(), s);
				}
				
			}
			if (isPrefSet) {
				prefsEditor.commit();
			}
		} else {
			// We must load options from prefs
			options = new HashMap<String, String>((HashMap<String, String>)mNDNxServicePrefs.getAll());
		}

		Load();	
	}
	
	public void runService(){
		setStatus(SERVICE_STATUS.SERVICE_INITIALIZING);
		
		String ndnd_port = options.get(NDND_OPTIONS.NDN_LOCAL_PORT.name());
		if( ndnd_port == null ) {
			ndnd_port = OPTION_NDN_PORT_DEFAULT;
			options.put(NDND_OPTIONS.NDN_LOCAL_PORT.name(), ndnd_port);
		}
		Log.d(TAG,NDND_OPTIONS.NDN_LOCAL_PORT.name() + " = " + options.get(NDND_OPTIONS.NDN_LOCAL_PORT.name()));
		
		String ndnd_keydir = options.get(NDND_OPTIONS.NDND_KEYSTORE_DIRECTORY.name());
		if( ndnd_keydir == null ) {
			File f = getDir("ndnd", Context.MODE_PRIVATE );
			ndnd_keydir = f.getAbsolutePath();
			options.put(NDND_OPTIONS.NDND_KEYSTORE_DIRECTORY.name(), ndnd_keydir);
		}
		
		if(options.get(NDND_OPTIONS.NDND_CAP.name()) == null) {
			options.put(NDND_OPTIONS.NDND_CAP.name(), OPTION_NDND_CAP_DEFAULT);
		}
		
		if(options.get(NDND_OPTIONS.NDN_LOCAL_SOCKNAME.name()) == null) {
			options.put(NDND_OPTIONS.NDN_LOCAL_SOCKNAME.name(), ndnd_keydir + "/ndnd.sock");
		}
		
		dumpOptions();

		
		try {
			createKeystore(ndnd_keydir, KEYSTORE_NAME + ndnd_port);
			for( Entry<String,String> entry : options.entrySet() ) {
				setenv(entry.getKey(), entry.getValue(), 1);
			}
			// Shouldn't we check to see that we aren't already running before we run?
			ndndCreate();
			try {
				setStatus(SERVICE_STATUS.SERVICE_RUNNING);
				ndndRun();
			} catch(RuntimeException rte) {
				Log.e(TAG, "RuntimeException while starting up NdndService");
			} finally {
				ndndDestroy();		
			}
		} catch(Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Exception caught while starting up/shutting down.  Reason: " + e.getMessage()); 
			setStatus(SERVICE_STATUS.SERVICE_ERROR);
			// returning will end the thread
		}
		serviceStopped();
	}
	
	protected void createKeystore(String dir_name, String keystore_name) {
		File dir = new File(dir_name);

		// This is to get a keystore file
		// Does dir/.ndnd_keystore_xxx exist?
		File try_keystore = new File(dir, keystore_name);

		if( try_keystore.exists() ) {
			Log.d(TAG, "Keystore Exists! " + try_keystore.getAbsolutePath());
			return;
		}
		
		Log.d(TAG,"Creating Keystore @ " + try_keystore.getAbsolutePath());

		try {
			//
			// In order to give us a chance to properly report service state avoid a mess on 
			// subsequent attempts to start up this service (manual or via intent), we should
			NDNxLibraryCheck.checkBCP();	   
			FileOutputStream stream = new FileOutputStream(try_keystore);
			BasicKeyManager.createKeyStore(stream, null, "ndnd", KEYSTORE_PASS, "NDND");
			stream.close();
		} catch(RuntimeException rte) {
			// There are a few which can fail and this makes the service unstable since
			// subsequent service invocations will find the keystore, partially baked, return, and later crash, 
			// can cause dependent applications.  Handle this by:
			// 0. Log it
			// 1. Delete any keystore cruft
			// 2. set error status (do not pass go)
			// 3. Rethrow so this can bubble up properly
			// We may find there are outher errors that warrant handling
			Log.e(TAG, "Error loading class.  Reason: " + rte.getMessage());
			if (try_keystore.exists()) {
				Log.d(TAG, "Deleting existing keystore");
				try_keystore.delete();
			}
			setStatus(SERVICE_STATUS.SERVICE_ERROR);
			// We want to throw this so that runService() gets a chance to handle and close down
			throw rte;
		} catch(Exception e) {
			// Need to clean this up to catch each exception, may be that we can recover or handle effectively
			// What other exceptions do we see?  Should just deal with each case
			// 1. File Access/Permission Denied
			e.printStackTrace();
			Log.d(TAG, "Exception while creating keystore.  Reason: " + e.getMessage()); 
			setStatus(SERVICE_STATUS.SERVICE_ERROR);
		} 

	}

	protected void stopService(){
		setStatus(SERVICE_STATUS.SERVICE_TEARING_DOWN);
		kill();
		setStatus(SERVICE_STATUS.SERVICE_FINISHED);
	}
	
	/* ************************************************************* */

	protected native void ndndCreate();
	protected native void ndndRun();
	protected native void ndndDestroy();
	protected native void kill();
	protected native void setenv(String key, String value, int overwrite);
}
