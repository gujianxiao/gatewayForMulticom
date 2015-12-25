/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.versioning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.profiles.CommandMarker;
import org.ndnx.ndn.profiles.versioning.InterestData;
import org.ndnx.ndn.profiles.versioning.VersionNumber;
import org.ndnx.ndn.profiles.versioning.VersioningInterestManager;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.junit.Assert;

public class VersioningHelper {

	public static void compareReceived(NDNHandle handle, ArrayList<NDNStringObject> sent, TestListener listener) throws Exception {
		Assert.assertEquals(sent.size(), listener.received.size());

		// make a usable data structure from the ContentObjects
		ArrayList<String> recv = new ArrayList<String>();
		for( ReceivedData datum : listener.received) {
			NDNStringObject so = new NDNStringObject(datum.object, handle);
			recv.add(so.string());
		}

		for( int i = 0; i < sent.size(); i++ ) {
			// now make sure what we got is what we expected
			String string_sent = sent.get(i).string();

			// make sure it exists in received.  does not need
			// to be in the same order as sent
			Assert.assertTrue(recv.contains(string_sent));
		}
	}

	public static ArrayList<NDNStringObject> sendEventStream(NDNHandle handle, ContentName basename, int tosend) throws Exception {

		// Send a stream of string objects
		ArrayList<NDNStringObject> sent = new ArrayList<NDNStringObject>();

		for(int i = 0; i < tosend; i++) {
			// Save content
			try {
				NDNStringObject so = new NDNStringObject(basename,
						String.format("string object %d", i), 
						SaveType.LOCALREPOSITORY, handle);
				so.save();
				so.close();
				sent.add(so);

				//				System.out.println("Sent version " + VersioningProfile.printAsVersionComponent(so.getVersion()));

				// Flow controller just cannot go this fast, so need to slow it down.
				Thread.sleep(10);

			} catch(Exception e) {
				e.printStackTrace();
				throw e;
			}
			//			System.out.println(i);
		}

		return sent;
	}

	public static class ConditionLong {
		protected Lock lock = new ReentrantLock();
		protected Condition cond  = lock.newCondition();

		public long value = 0;

		public ConditionLong(long value) {
			this.value = value;
		}

		public long setValue(long value) throws InterruptedException {
			lock.lock();
			try {
				long oldValue = this.value;
				this.value = value;
				cond.signal();
				return oldValue;
			} finally {
				lock.unlock();
			}
		}

		public long getValue() {
			lock.lock();
			try {
				return value;
			} finally {
				lock.unlock();
			}
		}

		public long increment() {
			lock.lock();
			try {
				value++;
				cond.signal();
				return value;
			} finally {
				lock.unlock();
			}
		}

		public long decrement() {
			lock.lock();
			try {
				value--;
				cond.signal();
				return value;
			} finally {
				lock.unlock();
			}
		}

		/**
		 * @param value = to wait for
		 * @param timeout = in milliseconds
		 * @return true if value found, false if timeout
		 * @throws InterruptedException
		 */
		public boolean waitForValue(long value, long timeout) throws InterruptedException {
			boolean rtn = true;
			lock.lock();
			try {
				while ( rtn && this.value != value ) {
					rtn = cond.await(timeout, TimeUnit.MILLISECONDS);
				}

			} finally {
				lock.unlock();
			}
			return rtn;
		}
	}

	public static class ReceivedData {
		public final ContentObject object;
		public final Interest interest;
		public ReceivedData(ContentObject object, Interest interest) {
			this.object = object;
			this.interest = interest;
		}
	}

	public static class TestListener implements NDNContentHandler {
		public final ConditionLong cl = new ConditionLong(0);
		public final ArrayList<ReceivedData> received = new ArrayList<ReceivedData>();
		public InterestData id = null;
		public int runCount = 0;
		public boolean debugOutput = false;
		// if true, run() method will send an initial interest, otherwise it will assume there
		// is already an interest outstanding.
		public boolean sendFirstInterest = true;

		public synchronized Interest handleContent(ContentObject data, Interest interest) {
			if( debugOutput )
				System.out.println("handleContent: " + data.name());
			
			received.add(new ReceivedData(data, interest));

			try {
				if( null != id ) {
					try {
						VersionNumber version = new VersionNumber(data.name());
						id.addExclude(version);
					} catch(Exception e) {
						e.printStackTrace();
					}

					if( cl.getValue() < runCount ) {
						Interest newinterest = id.buildInterest();
						if( debugOutput )
							System.out.println("Return interest: " + newinterest);
						return newinterest;
					}
				}
				if( debugOutput )
					System.out.println("Return interest: null");
				return null;
			} finally {
				// want to do this at the very end, so anyting waiting on the
				// value to be incremented will get it after this procedure is done.
				cl.increment();
			}
		}

		public void setInterestData(InterestData id) {
			this.id = id;
		}

		/**
		 * Sends interests, blocks until done or timeout
		 * @param handle
		 * @param count
		 * @param timeout
		 * @return true if count received, false otherwise
		 * @throws IOException
		 * @throws InterruptedException
		 */
		public boolean run(NDNHandle handle, int count, long timeout) throws IOException, InterruptedException {
			runCount = count;
			if( count > 0 && sendFirstInterest ) {
				Interest interest = id.buildInterest();
				handle.expressInterest(interest, this);
			}

			// wait for it to be done
			boolean rtn = cl.waitForValue(count, timeout);
			System.out.println(String.format("run received %d objects", cl.getValue()));
			return rtn;
		}
	}

	public static class TestFilterListener implements NDNInterestHandler {
		public final ConditionLong cl = new ConditionLong(0);
		public final ArrayList<Interest> received = new ArrayList<Interest>();
		public int runCount = 0;
		public boolean debugOutput = false;

		public synchronized boolean handleInterest(Interest interest) {
			if( debugOutput )
				System.out.println("handleInterest: " + interest.name());
			
			received.add(interest);
			cl.increment();
			
			return false;
		}
	}
	
	/**
	 * Track interests
	 */
	public static class SinkHandle extends NDNHandle {
		// The set of pending interests
		public final ArrayList<Interest> interests = new ArrayList<Interest>();
		public final ConditionLong count = new ConditionLong(0);
		public final ConditionLong total_count = new ConditionLong(0);

		protected SinkHandle() throws ConfigurationException, IOException {
			super();
		}

		protected SinkHandle(KeyManager keyManager) throws IOException {
			super(keyManager);
		}

		public static SinkHandle open(KeyManager keyManager) throws IOException { 
			synchronized (NDNHandle.class) {
				return new SinkHandle(keyManager);
			}
		}

		public static SinkHandle open(NDNHandle handle) throws IOException {
			return open(handle.keyManager());
		}

		@Override
		public synchronized void expressInterest(
				Interest interest,
				NDNContentHandler handler) throws IOException {

			// ignore startwrites
			if( !interest.name().contains(CommandMarker.COMMAND_MARKER_REPO_START_WRITE.getBytes()) ) {
				interests.add(interest);

				if( Log.isLoggable(Log.FAC_ENCODING, Level.FINER))
					Log.finer(Log.FAC_ENCODING, String.format("expressInterest (%d): %s",
							interests.size(), interest.toString()));
				count.increment();
				total_count.increment();
			}
			super.expressInterest(interest, handler);
		}

		@Override
		public synchronized void cancelInterest(Interest interest, NDNContentHandler handler) {
			if( !interest.name().contains(CommandMarker.COMMAND_MARKER_REPO_START_WRITE.getBytes()) ) {
				interests.remove(interest);
				//				System.out.println(String.format("cancelInterest  (%d): %s",
				//						interests.size(), interest.toString()));

				count.decrement();
			}
			super.cancelInterest(interest, handler);
		}

	}
	public static class TestVIM extends VersioningInterestManager {
		protected boolean _sendInterests = false;

		public TestVIM(NDNHandle handle, ContentName name,
				Set<VersionNumber> exclusions, VersionNumber startingVersion,
				NDNContentHandler handler) {
			super(handle, name, exclusions, startingVersion, handler);
		}

		public Interest exposeReceive(ContentObject data, Interest interest) {
			return receive(data, interest);
		}

		public void setSendInterest(boolean enable) {
			_sendInterests = enable;
		}

		public TreeSet<InterestData> getInterestDataTree() {
			return _interestData;
		}

		public TreeSet<VersionNumber> getExclusions() {
			return _exclusions;
		}

		/**
		 * Don't actually send an interest
		 */
		@Override
		protected void sendInterest(InterestData id) {
			Interest old = id.getLastInterest();
			Interest interest = id.buildInterest();
			synchronized(_interestMap) {
				// Remove the old interest so we never match more than one
				// thing to an INterestData
				if( null != old )
					_interestMap.remove(old);

				try {
					if( _sendInterests )
						_handle.expressInterest(interest, this);
					_interestMap.put(interest, new InterestMapData(id));
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
