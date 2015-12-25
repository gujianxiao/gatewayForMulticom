/*
 * NDNx Android Services
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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

package org.ndnx.android.services;

import org.ndnx.android.ndnlib.NDNxServiceControl;
import org.ndnx.android.ndnlib.NDNxServiceCallback;
import org.ndnx.android.ndnlib.NDNxServiceStatus.SERVICE_STATUS;
import org.ndnx.android.ndnlib.NdndWrapper.NDND_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.NDNR_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.REPO_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.NDNS_OPTIONS;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.MenuItem;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.net.Uri;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Android UI for controlling NDNx services.
 */
public final class Controller extends Activity implements OnClickListener {
	public final static String TAG = "NDNx Service Controller";
    public static final String NDNX_WS_URL = "http://127.0.0.1:9695";
	private Button mAllBtn;
	private ProgressDialog pd;

	private Context _ctx;
	
	private TextView tvNdndStatus;
	private TextView tvRepoStatus;
	private TextView deviceIPAddress = null;
	
	private NDNxServiceControl control;
	private String mReleaseVersion = "Unknown";
    private BroadcastReceiver mReceiver;

	// Create a handler to receive status updates
	private final Handler _handler = new Handler() {
		public void handleMessage(Message msg){
			SERVICE_STATUS st = SERVICE_STATUS.fromOrdinal(msg.what);
			Log.d(TAG,"Received new status from NDNx Services: " + st.name());
			// This is very very lazy.  Instead of checking what we got, we'll just
			// update the state and let that get our new status
			// Considering above comment, we should decide whether this is overly complex and implement a state machine
			// that can be rigorously tested for state transitions, and is adhered to in the UI status notifications
			if ((st == SERVICE_STATUS.START_ALL_DONE) || (st == SERVICE_STATUS.STOP_ALL_DONE)) {
				mAllBtn.setText(R.string.allStartButton);
				mAllBtn.setEnabled(true);
			} 

			if (st == SERVICE_STATUS.START_ALL_ERROR) {
				Toast.makeText(_ctx, "Unable to Start Services.  Reason:" + control.getErrorMessage(), 20).show();
				mAllBtn.setText(R.string.allStartButton_Error);
			} 
			// Update the UI after we receive a notification, otherwise we won't capture all state changes
			updateState();
		}
	};
	
	NDNxServiceCallback cb = new NDNxServiceCallback() {
		public void newNDNxStatus(SERVICE_STATUS st) {
			_handler.sendEmptyMessage(st.ordinal());
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controllermain);   
        
        Log.d(TAG,"Creating Service Controller");
        
        _ctx = this.getApplicationContext();
        
		init();
		initUI();
		updateState();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	// We should be saving out the state here for the UI so we don't lose user settings
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
    	control.disconnect();
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	super.onResume();

        IntentFilter intentfilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received android.net.conn.CONNECTIVITY_CHANGE");
                // updateIPAddress();
                new GetIPAddrAsyncTask().execute();
            }
        };
        this.registerReceiver(mReceiver, intentfilter);
        //
        // Update on resume, as frequently, in old Android esp, WIFI gets
        // shut off and may lose the address it had
        //
        // updateIPAddress();
        new GetIPAddrAsyncTask().execute();

        // We should updateState on resuming, in case Service state has changed
        updateState();
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.servicemenu, menu);
    	return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.reset:
	        	// Need to figure out if this is always safe to call even when nothing is running
	            control.stopAll();
	            control.clearErrorMessage();
	            Toast.makeText(this, "Reset NDNxServiceStatus complete, new status is: {ndnd: " + control.getNdndStatus().name() + 
	            	", repo: " + control.getRepoStatus().name() + "}", 10).show();
	            return true;
	        case R.id.ndndstatus:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(NDNX_WS_URL));
                startActivity(intent);
	            return true;
	        case R.id.about:
	        	setContentView(R.layout.aboutview);
	        	TextView aboutdata = (TextView) findViewById(R.id.about_text);
	        	aboutdata.setText(mReleaseVersion + "\n" + aboutdata.getText());

	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
    private void init(){
		Log.d(TAG, "init()");
    	control = new NDNxServiceControl(this);
    	control.registerCallback(cb);
    	control.connect();
    	try {
    		PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			mReleaseVersion = TAG + " " + pInfo.versionName;
		} catch(NameNotFoundException e) {
			Log.e(TAG, "Could not find package name.  Reason: " + e.getMessage());
		}
    }

	public void onClick(View v) {
		switch( v.getId() ) {
		case R.id.allStartButton:
			allButton();
			break;
		default:
			Log.e(TAG, "Clicked unknown view");
		}
	}

	private void updateState(){
		if(control.isAllRunning()){
			mAllBtn.setText(R.string.allStopButton);
            mAllBtn.setEnabled(true);
            Log.d(TAG, "Repo and NDND are running, enable button");
		} else if (((control.getNdndStatus() == SERVICE_STATUS.SERVICE_OFF) && (control.getRepoStatus() == SERVICE_STATUS.SERVICE_OFF)) ||
			((control.getNdndStatus() == SERVICE_STATUS.SERVICE_FINISHED) && (control.getRepoStatus() == SERVICE_STATUS.SERVICE_FINISHED))
		) {
			Log.d(TAG, "Repo and NDND are both finished/off");
		} else {
			// We've potentially got to wait longer, or we've got problems
			// If we've got problems, report it via notifcation to taskbar
			if ((control.getNdndStatus() == SERVICE_STATUS.SERVICE_ERROR) || (control.getRepoStatus() == SERVICE_STATUS.SERVICE_ERROR)) {
				Log.e(TAG, "Error in NDNxServiceStatus.  Need to clear error and reset state");
				// Toast it for now
				Toast.makeText(this, "Error in NDNxServiceStatus.  Need to clear error and reset state", 20).show();
			}
		}
		tvNdndStatus.setText(control.getNdndStatus().name());
		tvRepoStatus.setText(control.getRepoStatus().name());
	}
	
	/**
	 * Start all services in the background
	 */
	private void allButton(){
        // Always disable the button after a click until we
        // reach a stable state, or hit error condition
        mAllBtn.setEnabled(false);
        mAllBtn.setText(R.string.allStartButton_Processing);

        Log.d(TAG, "Disabling All Button");
		if(control.isAllRunning()){
			// Everything is ready, we must stop
			control.stopAll();
		} else { /* Note, this doesn't take into account partially running state */
			// Not all running... attempt to start them
			// but first, get the user settings
			// Consider these to be our defaults
			// We don't really check validity of the data in terms of constraints
			// so we should shore this up to be more robust
			final EditText ndnrDir = (EditText) findViewById(R.id.key_ndnr_directory);  
			String val = ndnrDir.getText().toString();  
			if (isValid(val)) {
				control.setNdnrOption(NDNR_OPTIONS.NDNR_DIRECTORY, val);
			} else {
				// Toast it, and return (so the user fixes the bum field)
				Toast.makeText(this, "NDNR_DIRECTORY field is not valid.  Please set and then start.", 10).show();
				return;
			}
			
			final EditText ndnrGlobalPrefix= (EditText) findViewById(R.id.key_ndnr_global_prefix);  
			val = ndnrGlobalPrefix.getText().toString();  
			if (isValid(val)) {
				control.setNdnrOption(NDNR_OPTIONS.NDNR_GLOBAL_PREFIX, val);
			} else {
				// Toast it, and return (so the user fixes the bum field)
				Toast.makeText(this, "NDNR_GLOBAL_PREFIX field is not valid.  Please set and then start.", 10).show();
				return;
			}
			
			final Spinner ndnrDebugSpinner = (Spinner) findViewById(R.id.key_ndnr_debug);  
			val = ndnrDebugSpinner.getSelectedItem().toString();
			if (isValid(val)) {
				control.setNdnrOption(NDNR_OPTIONS.NDNR_DEBUG, val);
			} else {
				// Toast it, and return (so the user fixes the bum field)
				// XXX I Don't think this will ever happen
				Toast.makeText(this, "NDNR_DEBUG field is not valid.  Please set and then start.", 10).show();
				return;
			}
			final Spinner ndnsDebugSpinner = (Spinner) findViewById(R.id.key_ndns_debug);  
			val = ndnsDebugSpinner.getSelectedItem().toString();
			if (isValid(val)) {
				control.setSyncOption(NDNS_OPTIONS.NDNS_DEBUG, val);
			} else {
				// Toast it, and return (so the user fixes the bum field)
				// XXX I Don't think this will ever happen
				Toast.makeText(this, "NDNS_DEBUG field is not valid.  Please set and then start.", 10).show();
				return;
			}
			control.startAllInBackground();
		}
		// updateState();
	}

	private void initUI() {
		mAllBtn = (Button)findViewById(R.id.allStartButton);
        mAllBtn.setOnClickListener(this);

        tvNdndStatus = (TextView)findViewById(R.id.tvNdndStatus);
        tvRepoStatus = (TextView)findViewById(R.id.tvRepoStatus);
        deviceIPAddress = (TextView)findViewById(R.id.deviceIPAddress);
        new GetIPAddrAsyncTask().execute();
        //
        // Grab the LinearLayout in the 0th child element
        //
        
        ViewGroup layout = (ViewGroup) findViewById(R.id.scrollcontainer); // .getChildAt(0);
        ViewGroup layoutchild = (ViewGroup) layout.getChildAt(0);
        if (Build.VERSION.SDK_INT >= 0x0000000e) { // ICS or greater, requires at least SDK 14 to compile
			final android.widget.Switch swbtn = new android.widget.Switch(this);
			swbtn.setOnCheckedChangeListener(new ToggleOptionChangeListener(NDNS_OPTIONS.NDNS_ENABLE));
			swbtn.setChecked(true);
			layoutchild.addView(swbtn);
		} else { // Fall back to pre-ICS widget
			android.widget.ToggleButton tbtn = new android.widget.ToggleButton(this);
			tbtn.setOnCheckedChangeListener(new ToggleOptionChangeListener(NDNS_OPTIONS.NDNS_ENABLE));
			tbtn.setChecked(true);
			layoutchild.addView(tbtn);
		}
	}

	private class ToggleOptionChangeListener implements CompoundButton.OnCheckedChangeListener {
		//
		// In principle, it would be nice to have a generalized listener for toggle poperties, rather than writing
		// one for each property.  Due to the fact our OPTIONS are not Strings or int primitives, it gets a little
		// messy to make this in a general way.  We should consider changing the OPTIONS to be int primitives while
		// the impact of such changes will be minimal rather than maintaining this as a set of enums, which has been a
		// pain
		//
		private NDNS_OPTIONS mNdns_option = null;
		private NDNR_OPTIONS mNdnr_option = null;
		private REPO_OPTIONS mRepo_option = null;
		private NDND_OPTIONS mNdnd_option = null;

		public ToggleOptionChangeListener(NDNS_OPTIONS option) {
			mNdns_option = option;
		}

		public ToggleOptionChangeListener(NDNR_OPTIONS option) {
			mNdnr_option = option;
		}

		public ToggleOptionChangeListener(REPO_OPTIONS option) {
			mRepo_option = option;
		}

		public ToggleOptionChangeListener(NDND_OPTIONS option) {
			mNdnd_option = option;
		}

		public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
			String val = isChecked ? "1" : "0";
			if (control == null) {
				Log.e(TAG, "control is null, failing");
				return;
			}
			if (mNdns_option != null) {
				control.setSyncOption(mNdns_option, val);
			} else if (mNdnr_option != null) {
				control.setNdnrOption(mNdnr_option, val);
			} else if (mRepo_option != null) {
				control.setRepoOption(mRepo_option, val);
			} else if (mNdnd_option != null) {
				control.setNdndOption(mNdnd_option, val);
			} else {
				Log.e(TAG, "null OPTION specificed for ToggleOptionChangeListener, ignoring.");
			}
		}
	}

	private String getIPAddress() {
		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface nic = e.nextElement();
                Log.d(TAG,"---------------------------------");
                Log.d(TAG,"NIC: " + nic.toString());
                Log.d(TAG,"---------------------------------");
				for (Enumeration<InetAddress> enumIpAddr = nic.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress addr = enumIpAddr.nextElement();
                    if(addr != null)
                    {
                        Log.d(TAG, "      HostName: " + addr.getHostName());
                        Log.d(TAG, "         Class: " + addr.getClass().getSimpleName());
                        Log.d(TAG, "            IP: " + addr.getHostAddress());
                        Log.d(TAG, " CanonicalHost: " + addr.getCanonicalHostName());
                        Log.d(TAG, " Is SiteLocal?: " + addr.isSiteLocalAddress());
                    }
					if (!addr.isLoopbackAddress()) {
						return addr.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Toast.makeText(this, "Error obtaining IP Address.  Reason: " + ex.getMessage(), 10).show();
			// If we can't get our IP, we got problems
			// Report it
			Log.e(TAG, "Error obtaining IP Address.  Reason: " + ex.getMessage());
		}
		return null;
	}

	private boolean isValid(String val) {
		// Normally we'd do real field validation to make sure input matches type of input
		return (!((val == null) || (val.length() == 0)));
	}

    private void updateIPAddress(String ipaddr) {
        if (ipaddr != null) {
            deviceIPAddress.setText(ipaddr);
        } else {
            deviceIPAddress.setText("Unable to determine IP Address");
        }
    }

	public void aboutviewButtonListener (View view) {
		// Called with user clicks OK, return to main view
		setContentView(R.layout.controllermain);
		initUI();
		updateState();
	}

	private class GetIPAddrAsyncTask extends AsyncTask<Void, Void, String>  {

	    @Override
		protected String doInBackground(Void... params) {
			String result = getIPAddress();
			Log.d(TAG, "GetIPAddrAsyncTask result = " + result);
			return getIPAddress();
		}

	    @Override
	    protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Controller.this.updateIPAddress(result);
	    }
	}
}
