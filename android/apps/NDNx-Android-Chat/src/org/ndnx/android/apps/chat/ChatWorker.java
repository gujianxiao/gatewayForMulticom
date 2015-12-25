/*
 * NDNx Android Chat
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
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

package org.ndnx.android.apps.chat;

/**
 * Worker thread for ndn Chat.  This does all the networking stuff.
 * ChatScreen.java is the UI.
 * 
 * Starts NDNx with a blocking call in our thread, then notifies
 * the UI when the services are ready.
 */
import java.io.IOException;

import org.ndnx.android.ndnlib.NDNxConfiguration;
import org.ndnx.android.ndnlib.NDNxServiceCallback;
import org.ndnx.android.ndnlib.NDNxServiceControl;
import org.ndnx.android.ndnlib.NDNxServiceStatus.SERVICE_STATUS;
import org.ndnx.android.ndnlib.NdndWrapper.NDND_OPTIONS;
import org.ndnx.android.ndnlib.RepoWrapper.REPO_OPTIONS;
import org.ndnx.ndn.apps.ndnchat.NDNChatNet;
import org.ndnx.ndn.apps.ndnchat.NDNChatNet.NDNChatCallback;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.profiles.ndnd.NDNDaemonException;
import org.ndnx.ndn.profiles.ndnd.SimpleFaceControl;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;

import android.content.Context;
import android.util.Log;

/**
 * All the NDNx code for Chat is in this worker thread.  It's basically the code
 * from the original ndnChat wrapped inside the worker thread.
 */
public class ChatWorker implements Runnable, NDNxServiceCallback, NDNChatCallback {
	protected final static String TAG="ChatWorker";

	/**
	 * Create a worker thread to handle all the NDNx calls.
	 * 
	 * @param ctx The UI context, needed to start/stop services
	 * @param callback The UI callback when we receive a chat message or a NDNx service status
	 */
	public ChatWorker(Context ctx, ChatCallback callback) {
		_context = ctx;
		_thd = new Thread(this, "ChatWorker");
		_chatCallback = callback;

		// Use a shared key directory
		NDNxConfiguration.config(ctx, false);
	}

	/**
	 * Start the worker thread, along with NDN services
	 * @param username Your "handle" on the Chat
	 * @param namespace The chat ndn:/ namespace
	 * @throws MalformedContentNameStringException 
	 */
	public synchronized void start(String username, String namespace, String remotehost, String remoteport) {
		if( false == _running ) {
			try {
				_remotehost = remotehost;
				_remoteport = remoteport;
				_chat = new NDNChatNet(this, namespace);
				_running = true;
				_finished = false;
				_thd.start();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Exit the worker thread, but keep services running
	 */
	public synchronized void stop() {
		if( !_finished ) {
			_finished = true;
			try {
				_chat.shutdown();
			} catch (IOException e) {
				e.printStackTrace();
			}
			_ndnxService.disconnect();
		}
	}

	/**
	 * Exit the worker thread and shutdown services
	 */
	public synchronized void shutdown() {
		if( !_finished ) {
			_finished = true;
			try {
				_chat.shutdown();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				_ndnxService.stopAll();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sent a chat message to the network
	 * @param text
	 * @return true if sent, false if some NDN error
	 */
	public synchronized boolean send(String text) {
		Log.d(TAG, "send text = " + text);

		try {
			_chat.sendMessage(text);
		} catch(Exception e) {
			return false;
		}

		return true;
	}

	/**
	 * Runnable method
	 */
	@Override
	public void run() {
		service_run();
	}

	// ==============================================================================
	// Internal implementation

	protected NDNChatNet _chat;
	protected final ChatCallback _chatCallback;
	protected final Context _context;
	protected NDNxServiceControl _ndnxService;

	protected final Thread _thd;
	protected boolean _running = false;
	protected boolean _finished = true;
	
	protected String _remotehost = null;
	protected String _remoteport = "9695";

	/*********************************************/
	// These are all run in the NDN thread

	/**
	 * @param args
	 */
	protected void service_run() {

		// Startup NDNx in a blocking call
		if( !initializeNDNx() ) {
			Log.e(TAG, "Could not start NDNx services!");
		} else {
			Log.i(TAG,"Starting ndnChatNet.listen() loop");
			
			// Now do the Chat event loop
			try {
				_chat.listen();
			} catch (ConfigurationException e) {
				System.err.println("Configuration exception running ndnChat: "
						+ e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("IOException handling chat messages: "
						+ e.getMessage());
				e.printStackTrace();
			} catch(Exception e) {
				System.err.println("Exception handling chat messages: "
						+ e.getMessage());
				e.printStackTrace();	
			}
		}

		Log.i(TAG, "service_run() exits");
	}

	private boolean initializeNDNx() {
		_ndnxService = new NDNxServiceControl(_context);
		_ndnxService.registerCallback(this);
		_ndnxService.setNdndOption(NDND_OPTIONS.NDND_DEBUG, "1");
		_ndnxService.setRepoOption(REPO_OPTIONS.REPO_DEBUG, "WARNING");
		return _ndnxService.startAll();
	}

	/**
	 * Called from NDNxServiceControl
	 */
	@Override
	public void newNDNxStatus(SERVICE_STATUS st) {
		// NOw pass on the status to the app
		if( null != _chatCallback ) {
			switch(st) {
			case START_ALL_DONE:
				try {
					// If we specified a remote host, use it not multicast
					if( null != _remotehost && _remotehost.length() > 0 ) {
						SimpleFaceControl.getInstance().connectTcp(_remotehost, Integer.parseInt(_remoteport));
					} else {
						SimpleFaceControl.getInstance().openMulicastInterface();
					}
					_chatCallback.ndnxServices(true);
				} catch (NDNDaemonException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					_chatCallback.ndnxServices(false);
				}

				break;
			case START_ALL_ERROR:
				_chatCallback.ndnxServices(false);
				break;
			}
		}	
	}

	/**
	 * called from ndnChatNet when there's a new message.
	 * Pass it on to the UI.
	 */
	@Override
	public void recvMessage(String message) {
		Log.d(TAG, "recv text = " + message);
		_chatCallback.recv(message);
	}
}
