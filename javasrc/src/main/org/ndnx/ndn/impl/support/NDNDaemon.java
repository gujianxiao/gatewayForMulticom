/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl.support;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.ndnx.ndn.impl.NDNNetworkManager;


/**
 * Main ndnd command line daemon.
 * Allows start & stop of ndnd, as well as interactive runs.
 * 
 * TODO This is not actually yet used in any tests and therefore is itself not well tested
 */
public class NDNDaemon extends Daemon {
	public static final String PROP_NDND_COMMAND = "ndnd.command";
	public static final String PROP_NDND_DEBUG = "ndnd.debug";
	
	private static final String DEFAULT_NDND_COMMAND_STRING = "../ndnd/agent/ndnd";
	protected String _command = DEFAULT_NDND_COMMAND_STRING;
	protected Process _ndndProcess = null;
	protected NDNDaemon _daemon = null;
	
	/**
	 * Stop ndnd on exit from daemon
	 */
	protected class NDNDShutdownHook extends Thread {
		public void run() {
			if (_ndndProcess != null)
				_ndndProcess.destroy();
		}
	}
	
	protected class NDNDWorkerThread extends Daemon.WorkerThread {

		private static final long serialVersionUID = -6093561895394961537L;
		protected boolean _shutdown = false;
						
		protected NDNDWorkerThread(String daemonName) {
			super(daemonName);
		}
		
		public void work() {
			synchronized(this) {
				boolean interrupted = false;
				do {
					try {
						interrupted = false;
						wait();
					} catch (InterruptedException e) {
						interrupted = true;
					}		
				} while (interrupted && !_shutdown);
			}
		}
		
		/**
		 * Start ndnd but set up a shutdown hook to allow it to stop
		 */
		public void initialize() {
			String commandVal = System.getProperty(PROP_NDND_COMMAND);
			if (commandVal != null) {
				_command = commandVal;
			}
			Runtime.getRuntime().addShutdownHook(new NDNDShutdownHook());
			ProcessBuilder pb = new ProcessBuilder(_command);		
			Map<String, String> env = pb.environment();
			pb.redirectErrorStream(true);
			String portval = System.getProperty(NDNNetworkManager.PROP_AGENT_PORT);
			if (portval != null) {
				env.put("NDN_LOCAL_PORT", portval);
			}
			String debugVal = System.getProperty(PROP_NDND_DEBUG);
			if (debugVal != null) {
				env.put("NDND_DEBUG", debugVal);
			}
			try {
				_ndndProcess = pb.start();
			} catch (IOException e) {
				Log.logStackTrace(Level.WARNING, e);
				e.printStackTrace();
			}
			String outputFile = System.getProperty(PROP_DAEMON_OUTPUT);
			if (outputFile != null) {
				try {
					new DaemonOutput(_ndndProcess.getInputStream(), new FileOutputStream(outputFile, true));
				} catch (FileNotFoundException e) {
					Log.logStackTrace(Level.WARNING, e);
					e.printStackTrace();
				}
			}
		}
		
		
		public void finish() {
			synchronized (this) {
				_shutdown = true;
				notify();
			}
		}
		
		public boolean signal(String name) {
			return false;
		}
		
		public Object status(String type) {
			return "running";
		}
	}
	
	public NDNDaemon() {
		super();
		// This is a daemon: it should not do anything in the
		// constructor but everything in the initialize() method
		// which will be run in the process that will finally 
		// execute as the daemon, rather than in the launching
		// and stopping processes also.
		_daemonName = "ndnd";
		_daemon = this;
	}
	
	protected void initialize(String[] args, Daemon daemon) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-command")) {
				_command = args[i + 1];
			}
		}
	}
	
	protected WorkerThread createWorkerThread() {
		return new NDNDWorkerThread(daemonName());
	}
	
	protected void usage() {
		try {
			String msg = "usage: " + this.getClass().getName() + "[-start | -stop | -interactive | -signal <signal>] [-command <command>]";
			System.out.println(msg);
			Log.severe(msg);
		} catch (Exception e) {
			e.printStackTrace();
			Log.logStackTrace(Level.SEVERE, e);
		}
		System.exit(1);
	}

	public static void main(String[] args) {
		NDNDaemon daemon = null;
		try {
			daemon = new NDNDaemon();
			runDaemon(daemon, args);
		} catch (Exception e) {
			System.err.println("Error attempting to start daemon.");
			Log.warning("Error attempting to start daemon.");
			Log.warningStackTrace(e);
		}
	}
}
