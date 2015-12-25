/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011, 2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn;

import java.io.IOException;
import java.util.ArrayList;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;

/**
 * This class is designed to handle actually erroring in assertions that fail within NDN handlers. Normally
 * since the handler is called by a different thread than the test, an assertion failure within the handler
 * would not actually cause the test to fail.
 * 
 * To use this test, replace NDNHandle with an AssertionNDNHandle. Then after each expressInterest or registerFilter
 * call within your test you must call checkError to insure that the handler ran without error.
 */
public class AssertionNDNHandle extends NDNHandle {
	protected Error _error = null;
	protected boolean _callbackSeen = false;
	protected ArrayList<RelatedInterestHandler> _contentHandlers = new ArrayList<RelatedInterestHandler>();
	protected ArrayList<RelatedFilterListener> _interestHandlers = new ArrayList<RelatedFilterListener>();
	
	protected class RelatedInterestHandler {
		AssertionContentHandler _aHandler;
		NDNContentHandler _handler;
		
		protected RelatedInterestHandler(AssertionContentHandler aListener, NDNContentHandler handler) {
			_aHandler = aListener;
			_handler = handler;
		}
	}
	
	protected class RelatedFilterListener {
		AssertionInterestHandler _aHandler;
		NDNInterestHandler _handler;
		
		protected RelatedFilterListener(AssertionInterestHandler aListener, NDNInterestHandler handler) {
			_aHandler = aListener;
			_handler = handler;
		}
	}

	protected AssertionNDNHandle() throws ConfigurationException, IOException {
		super();
	}
	
	protected AssertionNDNHandle(KeyManager keyManager) throws IOException {
		super(keyManager);
	}
	
	public static AssertionNDNHandle open() throws ConfigurationException, IOException { 
		try {
			return new AssertionNDNHandle();
		} catch (ConfigurationException e) {
			Log.severe(Log.FAC_NETMANAGER, "Configuration exception initializing NDN library: " + e.getMessage());
			throw e;
		} catch (IOException e) {
			Log.severe(Log.FAC_NETMANAGER, "IO exception initializing NDN library: " + e.getMessage());
			throw e;
		}
	}
	
	public static AssertionNDNHandle open(KeyManager keyManager) throws IOException { 
		return new AssertionNDNHandle(keyManager);
	}
	
	/**
	 * Overrides of NDNHandle calls referencing the listener
	 */
	public void expressInterest(
			Interest interest,
			NDNContentHandler handler) throws IOException {
		AssertionContentHandler ail = null;
		synchronized (_contentHandlers) {
			ail = getInterestListener(handler);
			if (null == ail) {
				ail = new AssertionContentHandler(handler);
				_contentHandlers.add(new RelatedInterestHandler(ail, handler));
			}
			ail._references++;
		}
		super.expressInterest(interest, ail);
	}
	
	public void cancelInterest(Interest interest, NDNContentHandler handler) {
		AssertionContentHandler toCancel = null;
		synchronized (_contentHandlers) {
			toCancel = getInterestListener(handler);
			if (null == toCancel) {
				Log.warning("Questionable cancel of never expressed interest: %0", interest);
				toCancel = new AssertionContentHandler(handler);
				_contentHandlers.add(new RelatedInterestHandler(toCancel, handler));
			}
		}
		super.cancelInterest(interest, toCancel);

		synchronized (_contentHandlers) {
			if (--toCancel._references <= 0)
				_contentHandlers.remove(new RelatedInterestHandler(toCancel, handler));
		}
	}
	
	public void registerFilter(ContentName filter,
			NDNInterestHandler handler) throws IOException {
		AssertionInterestHandler afh = null;
		synchronized (_interestHandlers) {
			afh = getInterestHandler(handler);
			if (null == afh) {
				afh = new AssertionInterestHandler(handler);
				_interestHandlers.add(new RelatedFilterListener(afh, handler));
			}
			afh._references++;
		}
		super.registerFilter(filter, afh);
	}
	
	public void unregisterFilter(ContentName filter, NDNInterestHandler handler) {
		AssertionInterestHandler toUnregister = null;
		synchronized (_interestHandlers) {
			toUnregister = getInterestHandler(handler);
			if (null == toUnregister) {
				Log.warning("Questionable unregister of never registered filter: %0", filter);
				toUnregister = new AssertionInterestHandler(handler);
				_interestHandlers.add(new RelatedFilterListener(toUnregister, handler));
			}
		}
		super.unregisterFilter(filter, toUnregister);
		synchronized (_interestHandlers) {
			if (--toUnregister._references <= 0)
				_interestHandlers.remove(new RelatedFilterListener(toUnregister, handler));
		}
	}
	
	/**
	 * Should be called after any callback has been triggered on the handle that would have
	 * received the callback
	 * @param timeout millis to wait for callback to occur - doesn't wait if NO_TIMEOUT is used
	 * @throws Error
	 * @throws InterruptedException
	 */
	public void checkError(long timeout) throws Error, InterruptedException {
		if (timeout > 0 || timeout == SystemConfiguration.NO_TIMEOUT) {
			synchronized (this) {
				long startTime = System.currentTimeMillis();
				while (!_callbackSeen) {
					if (timeout == SystemConfiguration.NO_TIMEOUT)
						wait();
					else {
						wait(timeout);
						if ((System.currentTimeMillis() - startTime) > timeout)
							break;
					}
				}
				_callbackSeen = false;
			}
		}
		if (null != _error)
			throw _error;
	}
	
	private AssertionContentHandler getInterestListener(NDNContentHandler handler) {
		for (RelatedInterestHandler ril : _contentHandlers) {
			if (ril._handler == handler) {
				return ril._aHandler;
			}
		}
		return null;
	}
	
	private AssertionInterestHandler getInterestHandler(NDNInterestHandler handler) {
		for (RelatedFilterListener rfl : _interestHandlers) {
			if (rfl._handler == handler) {
				return rfl._aHandler;
			}
		}
		return null;
	}
	
	protected class AssertionInterestHandler implements NDNInterestHandler {
		
		protected NDNInterestHandler _handler;
		protected int _references = 0;
		
		public AssertionInterestHandler(NDNInterestHandler handler) {
			_handler = handler;
		}

		public boolean handleInterest(Interest interest) {
			boolean result = false;
			_callbackSeen = true;
			try {
				result = _handler.handleInterest(interest);
			} catch (Error e) {
				_error = e;
			}
			synchronized (this) {
				notifyAll();
			}
			if (null != _error)
				throw _error;
			return result;
		}	
	}
	
	protected class AssertionContentHandler implements NDNContentHandler {
		
		protected NDNContentHandler _handler;
		protected int _references = 0;
		
		public AssertionContentHandler(NDNContentHandler handler) {
			_handler = handler;
		}

		public Interest handleContent(ContentObject data, Interest interest) {
			Interest result = null;
			_callbackSeen = true;
			try {
				result = _handler.handleContent(data, interest);
			} catch (Error e) {
				_error = e;
			}
			synchronized (this) {
				notifyAll();
			}
			if (null != _error)
				throw _error;
			return result;
		}
	}
}
