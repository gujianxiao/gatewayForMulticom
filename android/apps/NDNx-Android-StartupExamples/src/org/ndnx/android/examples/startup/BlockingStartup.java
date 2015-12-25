package org.ndnx.android.examples.startup;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.concurrent.CountDownLatch;

import org.ndnx.android.ndnlib.NDNxConfiguration;
import org.ndnx.android.ndnlib.NDNxServiceCallback;
import org.ndnx.android.ndnlib.NDNxServiceControl;
import org.ndnx.android.ndnlib.NDNxServiceStatus.SERVICE_STATUS;
import org.ndnx.android.ndnlib.NdndWrapper.NDND_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.REPO_OPTIONS;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.config.UserConfiguration;
import org.ndnx.ndn.profiles.ndnd.NDNDaemonException;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class BlockingStartup extends StartupBase {
	protected String TAG="BlockingStartup";

	// ===========================================================================
	// Process control Methods

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TextView title = (TextView) findViewById(R.id.tvTitle);
		title.setText("BlockingStartup");
	}

	@Override
	public void onStart() {
		super.onStart();	
		Log.i(TAG,"onStart");
		_worker = new BlockingWorker();
		_thd = new Thread(_worker);
		_thd.start();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG,"onResume");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i(TAG,"onPause");
	}

	@Override
	public void onStop() {
		super.onPause();
		Log.i(TAG,"onStop");
		_worker.stop();
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");

		_worker.stop();
		super.onDestroy();
	}

	// ===========================================================================
	// UI Methods

	@Override
	void doExit() {
		
	}
	
	@Override
	void doShutdown() {
		_worker.shutdown();
	}

	// ====================================================================
	// Internal implementation

	protected BlockingWorker _worker = null;
	protected Thread _thd = null;

	
	// ===============================================
	protected class BlockingWorker implements Runnable, NDNxServiceCallback {
		protected final static String TAG="BlockingWorker";

		/**
		 * Create a worker thread to handle all the NDNx calls.
		 */
		public BlockingWorker() {
			_context = BlockingStartup.this.getBaseContext();

			postToUI("Setting NDNxConfiguration");
			
			// Use a shared key directory
			NDNxConfiguration.config(_context, false);

			File ff = getDir("storage", Context.MODE_WORLD_READABLE);
			postToUI("Setting setUserConfigurationDirectory: " + ff.getAbsolutePath());
			
			Log.i(TAG,"getDir = " + ff.getAbsolutePath());
			UserConfiguration.setUserConfigurationDirectory( ff.getAbsolutePath() );

			// Do these NDNx operations after we created ChatWorker
			ScreenOutput("User name = " + UserConfiguration.userName());
			ScreenOutput("ndnDir    = " + UserConfiguration.userConfigurationDirectory());
			ScreenOutput("Waiting for NDN Services to become ready");
		}

		/**
		 * Exit the worker thread, but keep services running
		 */
		public synchronized void stop() {
			// this is called form onDestroy too, so only do something
			// if the user didn't select a menu option to exit or shutdown.
			if( _latch.getCount() > 0 ) {
				_latch.countDown();
				_ndnxService.disconnect();
			}
		}

		/**
		 * Exit the worker thread and shutdown services
		 */
		public synchronized void shutdown() {
			_latch.countDown();
			try {
				_ndnxService.stopAll();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * Runnable method
		 */
		@Override
		public void run() {
			// Startup NDNx in a blocking call
			postToUI("Starting NDNx in blocking mode");

			if( !initializeNDNx() ) {
				Log.e(TAG, "Could not start NDNx services!");
			} 

			// wait for shutdown
			postToUI("Worker thread now blocking until exit");

			while( _latch.getCount() > 0 ) {
				try {
					_latch.await();
				} catch (InterruptedException e) {
				}
			}
			
			Log.i(TAG, "service_run() exits");		}

		// ==============================================================================
		// Internal implementation
		protected final CountDownLatch _latch = new CountDownLatch(1);
		protected final Context _context;
		protected NDNxServiceControl _ndnxService;

		/*********************************************/
		// These are all run in the NDN thread

		private boolean initializeNDNx() {
			_ndnxService = new NDNxServiceControl(_context);
			_ndnxService.registerCallback(this);
			_ndnxService.setNdndOption(NDND_OPTIONS.NDND_DEBUG, "1");
			_ndnxService.setRepoOption(REPO_OPTIONS.REPO_DEBUG, LOG_LEVEL);
			postToUI("calling _ndnxService.startAll");
			return _ndnxService.startAll();
		}

		/**
		 * Called from NDNxServiceControl
		 */
		@Override
		public void newNDNxStatus(SERVICE_STATUS st) {
			postToUI("NDNxStatus: " + st.toString());
			
			switch(st) {

			case START_ALL_DONE:
				try {
					postToUI("Opening NDN key manager/handle");
					openNdn();
					
					setupFace();
					postToUI("Finished NDNx Initialization");

					
				} catch (NDNDaemonException e) {
					e.printStackTrace();
					postToUI("SimpleFaceControl error: " + e.getMessage());
				} catch (ConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			case START_ALL_ERROR:
				postToUI("NDNxStatus ERROR");
				break;
			}
		}
	}
}
