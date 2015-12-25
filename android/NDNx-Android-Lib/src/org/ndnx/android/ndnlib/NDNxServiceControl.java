/*
 * NDNx Android Helper Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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

import org.ndnx.android.ndnlib.NDNxServiceStatus.SERVICE_STATUS;
import org.ndnx.android.ndnlib.NdndWrapper.NDND_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.REPO_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.NDNS_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.NDNR_OPTIONS;

import android.content.Context;
import android.util.Log;
import android.os.Environment;

import java.io.File;

/**
 * This is a helper class to access the ndnd and repo services. It provides
 * abstractions so that the programs can start and stop the services as well
 * as interact with them for configuration and monitoring.
 */
public final class NDNxServiceControl {
	public final static Long MINIMUM_SECONDS_SINCE_EPOCH = 946684800L;
	private final static String TAG = "NDNxServiceControl";
	private static String mErrorMessage = "";

	NdndWrapper ndndInterface;
	RepoWrapper repoInterface;
	Context _ctx;
	
	NDNxServiceCallback _cb = null;
	
	SERVICE_STATUS ndndStatus = SERVICE_STATUS.SERVICE_OFF;
	SERVICE_STATUS repoStatus = SERVICE_STATUS.SERVICE_OFF;
	
	NDNxServiceCallback ndndCallback = new NDNxServiceCallback(){
		@Override
		public void newNDNxStatus(SERVICE_STATUS st) {
			Log.i(TAG,"ndndCallback ndndStatus = " + st.toString());
			ndndStatus = st;
			switch(ndndStatus){
			case SERVICE_OFF:
				newNDNxAPIStatus(SERVICE_STATUS.NDND_OFF);
				break;
			case SERVICE_INITIALIZING:
				newNDNxAPIStatus(SERVICE_STATUS.NDND_INITIALIZING);
				break;
			case SERVICE_TEARING_DOWN:
				newNDNxAPIStatus(SERVICE_STATUS.NDND_TEARING_DOWN);
				break;
			case SERVICE_RUNNING:
				newNDNxAPIStatus(SERVICE_STATUS.NDND_RUNNING);
				break;
			case SERVICE_ERROR:
				newNDNxAPIStatus(SERVICE_STATUS.SERVICE_ERROR);
				break;
			default:
				Log.d(TAG, "ndndCallback, ignoring status = " + st.toString());
			}
		}
	};
	
	NDNxServiceCallback repoCallback = new NDNxServiceCallback(){
		@Override
		public void newNDNxStatus(SERVICE_STATUS st) {
			Log.i(TAG,"repoCallback repoStatus = " + st.toString());
			repoStatus = st;	
			switch(repoStatus){
			case SERVICE_OFF:
				newNDNxAPIStatus(SERVICE_STATUS.REPO_OFF);
				break;
			case SERVICE_INITIALIZING:
				newNDNxAPIStatus(SERVICE_STATUS.REPO_INITIALIZING);
				break;
			case SERVICE_TEARING_DOWN:
				newNDNxAPIStatus(SERVICE_STATUS.REPO_TEARING_DOWN);
				break;
			case SERVICE_RUNNING:
				newNDNxAPIStatus(SERVICE_STATUS.REPO_RUNNING);
				break;
			case SERVICE_ERROR:
				newNDNxAPIStatus(SERVICE_STATUS.SERVICE_ERROR);
				break;
			default:
				Log.d(TAG, "repoCallback, ignoring status = " + st.toString());
			}
		}
	};
	
	public NDNxServiceControl(Context ctx) {
		_ctx = ctx;
		ndndInterface = new NdndWrapper(_ctx);
		ndndInterface.setCallback(ndndCallback);
		ndndStatus = ndndInterface.getStatus();
		repoInterface = new RepoWrapper(_ctx);
		repoInterface.setCallback(repoCallback);
		repoStatus = repoInterface.getStatus();
		
	}
	
	public void registerCallback(NDNxServiceCallback cb){
		_cb = cb;
	}
	
	public void unregisterCallback(){
		_cb = null;
	}
	
	/**
	 * Start the NDN daemon and Repo 
	 * If configuration parameters have been set these will be used
	 * This is a BLOCKING call
	 * 
	 * @return true if everything started correctly, false otherwise
	 */
	public boolean startAll(){
		if (checkSystemOK()) {
			newNDNxAPIStatus(SERVICE_STATUS.START_ALL_INITIALIZING);
			Log.i(TAG,"startAll waiting for NDND startService");
			ndndInterface.startService();
			Log.i(TAG,"startAll waiting for NDND waitForReady");
			ndndInterface.waitForReady();
			newNDNxAPIStatus(SERVICE_STATUS.START_ALL_NDND_DONE);
			if(!ndndInterface.isReady()){
				mErrorMessage = mErrorMessage.concat("Unable to start ndnd service.");
				newNDNxAPIStatus(SERVICE_STATUS.START_ALL_ERROR);
				return false;
			}
			Log.i(TAG,"startAll waiting for REPO startService");
			repoInterface.startService();
			Log.i(TAG,"startAll waiting for REPO waitForReady");
			repoInterface.waitForReady();
			newNDNxAPIStatus(SERVICE_STATUS.START_ALL_REPO_DONE);
			if(!repoInterface.isReady()){
				mErrorMessage = mErrorMessage.concat("Unable to start repo service.");
				newNDNxAPIStatus(SERVICE_STATUS.START_ALL_ERROR);
				return false;
			} 
			newNDNxAPIStatus(SERVICE_STATUS.START_ALL_DONE);
			return true;
		} else {
			newNDNxAPIStatus(SERVICE_STATUS.START_ALL_ERROR);
			return false;
		}

	}
	
	/**
	 * Start the NDN daemon and Repo 
	 * If configuration parameters have been set these will be used
	 * This is a non-blocking call.  If you want to be notified when everything
	 * has started then you should register a callback before issuing this call.
	 */
	public void startAllInBackground(){
		Runnable r = new Runnable(){
			public void run() {
				startAll();
			}
		};
		Thread thd = new Thread(r);
		thd.start();
	}
	
	public void connect(){
		ndndInterface.bindIfRunning();
		repoInterface.bindIfRunning();
	}
	
	/**
	 * Disconnect from the services.  This is needed for a clean exit from an application. It leaves the services running.
	 */
	public void disconnect(){
		ndndInterface.unbindService();
		repoInterface.unbindService();
	}
	
	/**
	 * Stop the NDN daemon and Repo 
	 * This call will unbind from the service and stop it. There is no need to issue a disconnect().
	 */
	public void stopAll(){
		repoInterface.stopService();
		ndndInterface.stopService();
		newNDNxAPIStatus(SERVICE_STATUS.STOP_ALL_DONE);
	}

	public boolean checkSystemOK() {
		//
		// Do a quick check of things before we start.  If we can't properly initialize the following, don't pass go.
		// We fail right away rather than checking everything.
		// 1) system time
		// 2) check external storage writable
		// 3) other checks - TBD ... In the future we may want to verify that we have at least one usable *face
		//
		Log.d(TAG, "Checking current time in millis: " + System.currentTimeMillis() + " and date today = " + new java.util.Date());
		if (System.currentTimeMillis()/1000 < MINIMUM_SECONDS_SINCE_EPOCH) {
			// Realistically no modern device will be shipping from the factory without a reasonable default
			// near or close to the current date at manufacture, nor will it lack the ability to get time
			// from the network.  However, in dealing with Android "open source", some devices still seem to 
			// ship with time set to the beginning of the epoch, i.e., 0.
			Log.e(TAG,"Error in checkSystemOK(), please set OS System Time to valid, non-default date.");
			mErrorMessage = mErrorMessage.concat("Please set OS System Time before running this service.");
			return false;
		}

		if (!Environment.getExternalStorageDirectory().canWrite()) {
			// Again, not a likely scenario, but it's been seen before that some Android devices have either 
			// low quality media or problems in the design of the SDCARD reader that prevent the external storage
			// from build a valid write target.  Since we'll need both access to this storage and write 
			// access to it, we should not proceed if we fail to get writable external storage.
			// Future, more robust versions of this service should look for alternatives (app data space)
			// before failing completely.
			Log.e(TAG,"Error in checkSystemOK(), please fix permissions to access external storage for write, or insert writable media.");
			mErrorMessage = mErrorMessage.concat("Please check external SDCARD is available and writable.");
			return false;
		}

		return true;
	}
	public boolean isNdndRunning(){
		return ndndInterface.isRunning();
	}
	
	public boolean isRepoRunning(){
		return repoInterface.isRunning();
	}
	
	public void startNdnd(){
		ndndInterface.startService();
	}
	
	public void stopNdnd(){
		ndndInterface.stopService();
	}
	
	public void startRepo(){
		repoInterface.startService();
	}
	
	public void stopRepo(){
		repoInterface.stopService();
	}
	
	public void newNDNxAPIStatus(SERVICE_STATUS s){
		Log.d(TAG,"newNDNxAPIStatus sending " + s.toString());
		try {
			if(_cb != null) {
				_cb.newNDNxStatus(s);
			}
		} catch(Exception e){
			// Did the callback just throw an exception??
			// We're going to ignore it, it's not our problem (right?)
			Log.e(TAG,"The client callback has thrown an exception");
			e.printStackTrace();
		}
	}

	public void setNdndOption(NDND_OPTIONS option, String value) {
		ndndInterface.setOption(option, value);
	}
	
	public void setRepoOption(REPO_OPTIONS option, String value) {
		repoInterface.setOption(option, value);
	}
	
	public void setSyncOption(NDNS_OPTIONS option, String value) {
		repoInterface.setOption(option, value);
	}
	
	public void setNdnrOption(NDNR_OPTIONS option, String value) {
		repoInterface.setOption(option, value);
	}
	
	public String getErrorMessage() {
		return mErrorMessage;
	}

	public void clearErrorMessage() {
		mErrorMessage = "";
	}
	/**
	 * Are ndnd and the repo running and ready?
	 * @return true if BOTH ndnd and the repo are in state Running
	 */
	public boolean isAllRunning(){
		return(SERVICE_STATUS.SERVICE_RUNNING.equals(ndndStatus) &&
			   SERVICE_STATUS.SERVICE_RUNNING.equals(repoStatus));
	}
	
	public SERVICE_STATUS getNdndStatus(){
		return ndndStatus;
	}
	
	public SERVICE_STATUS getRepoStatus(){
		return repoStatus;
	}
}
