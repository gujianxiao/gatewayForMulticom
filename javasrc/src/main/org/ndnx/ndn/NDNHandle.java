/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011, 2012 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.security.keys.BasicKeyManager;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * The core class encapsulating a Java interface to the NDN network.
 * It implements the NDNBase core methods for reading and writing to NDN,
 * using an encapsulated NDNNetworkManager to interface with the local
 * NDN agent. It encapsulates a KeyManager to interface with the user's
 * collection of signing and verification keys. A typical application
 * may have one NDNHandle or many; each encapsulates a single connection
 * to the local NDN agent.
 * 
 * Once a handle is closed, it cannot be used anymore.  It will throw
 * an IOException in those cases.
 */
public class NDNHandle implements NDNBase {
	
	protected static NDNHandle _handle = null;

	protected final KeyManager _keyManager;
	
	// To give each handle a unique ID so they can be debuged
	protected final static AtomicInteger _handleIdCount = new AtomicInteger(0);
	protected final int _handleId;
	protected final String _handleIdString;
	
	/**
	 * A NDNNetworkManager embodies a connection to ndnd.
	 */
	protected final NDNNetworkManager _networkManager;
	protected final Object _openLock = new Object();
	protected boolean _isOpen = false;
	
	/*
	 * In order to have a handle automaticaly add a scope to every Interest, we provide a way to set
	 * a default scope to use, and enable/disable machinery.  (The defines for the scope don't belong
	 * here really, but it seems useful to have them specified.)
	 */
	protected Integer _scope = disableScope;
	public static final Integer disableScope = new Integer(-1);
	public static final Integer ndndScope = new Integer(0);
	public static final Integer localScope = new Integer(1);
	public static final Integer neighborhood = new Integer(2);
	
	
	/**
	 * Create a new NDNHandle, opening a new connection to the NDN network
	 * @return the NDNHandle
	 * @throws ConfigurationException if there is an issue in the user or system configuration
	 * 	which we cannot overcome without outside intervention. See the error message for details.
	 * @throws IOException if we encounter an error reading system, configuration, or keystore
	 * 		data that we expect to be well-formed.
	 */
	public static NDNHandle open() throws ConfigurationException, IOException { 
		try {
			return new NDNHandle();
		} catch (ConfigurationException e) {
			Log.severe(Log.FAC_NETMANAGER, "Configuration exception initializing NDN library: " + e.getMessage());
			throw e;
		} catch (IOException e) {
			Log.severe(Log.FAC_NETMANAGER, "IO exception initializing NDN library: " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Create a new NDNHandle, opening a new connection to the NDN network, and
	 * specifying the KeyManager it should use. Particularly useful in testing, to 
	 * run as if you were a different "user", with a different collection of keys.
	 * @param keyManager the KeyManager to use
	 * @return the NDNHandle
	 * @throws IOException 
	 */
	public static NDNHandle open(KeyManager keyManager) throws IOException { 
		return new NDNHandle(keyManager);
	}
	
	/**
	 * Returns a static NDNHandle that is made available as a default.
	 * @return the shared static NDNHandle
	 */
	public static NDNHandle getHandle() { 
		if (null != _handle) 
			return _handle;
		try {
			return create();
		} catch (ConfigurationException e) {
			Log.warning(Log.FAC_NETMANAGER, "Configuration exception attempting to create handle: " + e.getMessage());
			Log.warningStackTrace(e);
			throw new RuntimeException("Error in system configuration. Cannot create handle.",e);
		} catch (IOException e) {
			Log.warning(Log.FAC_NETMANAGER, "IO exception attempting to create handle: " + e.getMessage());
			Log.warningStackTrace(e);
			throw new RuntimeException("Error in system IO. Cannot create handle.",e);
		}
	}

	/**
	 * Internal synchronized creation method
	 * @return a new NDNHandle
	 * @throws ConfigurationException if there is an issue in the user or system configuration
	 * 	which we cannot overcome without outside intervention. See the error message for details.
	 * @throws IOException if we encounter an error reading system, configuration, or keystore
	 * 		data that we expect to be well-formed.
	 */
	protected static synchronized NDNHandle create() throws ConfigurationException, IOException {
		if (null == _handle) {
			KeyManager km = KeyManager.getDefaultKeyManager();
			if (km instanceof BasicKeyManager) {
				_handle = ((BasicKeyManager) km).handle();
			} else {
				_handle = new NDNHandle();
			}
		}
		return _handle;
	}

	/**
	 * Create a NDNHandle using the specified KeyManager
	 * @param keyManager the KeyManager to use. cannot be null.
	 * @throws IOException 
	 */
	protected NDNHandle(KeyManager keyManager) throws IOException {
		_handleId = _handleIdCount.incrementAndGet();
		_handleIdString = String.format("NDNHandle %d: ", _handleId);
		
		if (null == keyManager) {
			throw new IOException(formatMessage("Cannot instantiate handle -- KeyManager is null. Use NDNHandle() constructor to get default KeyManager, or specify one here."));
		}
		_keyManager = keyManager;
		// force initialization of network manager
		try {
			_networkManager = new NDNNetworkManager(_keyManager);
		} catch (IOException ex){
			Log.warning(formatMessage("IOException instantiating network manager: " + ex.getMessage()));
			Log.warningStackTrace(ex);
			throw ex;
		}
			
		synchronized(_openLock) {
			_isOpen = true;
		}
		
		if( Log.isLoggable(Log.FAC_NETMANAGER, Level.INFO) )
			Log.info(Log.FAC_NETMANAGER, formatMessage("Handle is now open"));
	}

	/**
	 * Create a NDNHandle using the default KeyManager for this user
	 * @throws ConfigurationException if there is an issue in the user or system configuration
	 * 	which we cannot overcome without outside intervention. See the error message for details.
	 * @throws IOException if we encounter an error reading system, configuration, or keystore
	 * 		data that we expect to be well-formed.
	 */
	protected NDNHandle() throws ConfigurationException, IOException {
		this(KeyManager.getDefaultKeyManager());
	}
	
	/**
	 * For testing only
	 */
	protected NDNHandle(boolean useNetwork) {
		_handleId = _handleIdCount.incrementAndGet();
		_handleIdString = String.format("NDNHandle %d: ", _handleId);
		_networkManager = null;
		_keyManager = null;
		
		if( Log.isLoggable(Log.FAC_NETMANAGER, Level.INFO) )
			Log.info(Log.FAC_NETMANAGER, formatMessage("Handle is now open (testing only)"));
	}
	
	/**
	 * Retrieve this handle's network manager. Should only be called by low-level
	 * methods seeking direct access to the network.
	 * 
	 * If the handle is closed, this will return null.
	 * 
	 * @return the NDN network manager
	 */
	public NDNNetworkManager getNetworkManager() { 
		synchronized(_openLock) {
			if( !_isOpen )
				return null;
		}

		return _networkManager;
	}

//	/**
//	 * Change the KeyManager this NDNHandle is using.
//	 * @param keyManager the new KeyManager to use
//	 */
//	public void setKeyManager(KeyManager keyManager) {
//		if (null == keyManager) {
//			Log.warning(formatMessage("StandardNDNLibrary::setKeyManager: Key manager cannot be null!"));
//			throw new IllegalArgumentException(formatMessage("Key manager cannot be null!"));
//		}
//		_keyManager = keyManager;
//	}
	
	/**
	 * Return the KeyManager we are using.
	 * @return our current KeyManager
	 */
	public KeyManager keyManager() { return _keyManager; }

	/**
	 * Get the publisher ID of the default public key we use to sign content
	 * @return our default publisher ID
	 */
	public PublisherPublicKeyDigest getDefaultPublisher() {
		return keyManager().getDefaultKeyID();
	}
	
	
	/**
	 * Sets the scope to be set if an Interest doesn't already have one set.
	 * @param scope		Scope -1 disable or if in the range of [0..2]
	 * @return
	 */
	public void setScope(Integer scope) throws IOException {
		if (this == _handle) {
			if (Log.isLoggable(Level.INFO)) Log.info(Log.FAC_NETMANAGER, formatMessage("setScope called on static handle"));
			throw new IOException(formatMessage("setScope called on a shared handle."));
		}
		if (scope == disableScope) {

		} else if (scope < 0 || scope > 2) {
			if (Log.isLoggable(Level.INFO)) Log.info(Log.FAC_NETMANAGER, formatMessage("setScope scope out of range " + scope));
			throw new IOException(formatMessage("setScope called with scope that is out of range [0..2] " + scope));
		}
		_scope = scope;
		if (Log.isLoggable(Level.INFO)) Log.info(Log.FAC_NETMANAGER, formatMessage("setScope set " + scope));
	}
	
	/**
	 * Gets the current value of the default scope.
	 * @return	The current default.
	 */
	public Integer getScope() throws IOException {
		if (this == _handle) {
			if (Log.isLoggable(Level.INFO)) Log.info(Log.FAC_NETMANAGER, formatMessage("getScope called on static handle"));
			throw new IOException(formatMessage("getScope called on a shared handle."));
		}
		return _scope;
	}
	
	/**
	 * Helper method wrapped around NDNBase#get(Interest, long)
	 * @param name name to query for
	 * @param timeout timeout for get
	 * @return the object retrieved, or null if timed out
	 * @throws IOException on error
	 * @see NDNBase#get(Interest, long)
	 */
	public ContentObject get(ContentName name, long timeout) throws IOException {
		Interest interest = new Interest(name);
		return get(interest, timeout);
	}
	
	/**
	 * Helper method wrapped around NDNBase#get(Interest, long)
	 * @param name name to query for
	 * @param publisher the desired publisher for the content
	 * @param timeout timeout for get
	 * @return the object retrieved, or null if timed out
	 * @throws IOException on error
	 * @see NDNBase#get(Interest, long)
	 */
	public ContentObject get(ContentName name, PublisherPublicKeyDigest publisher, long timeout) throws IOException {
		Interest interest = new Interest(name, publisher);
		return get(interest, timeout);
	}
	
	/**
	 * Get a single piece of content from NDN. This is a blocking get, it will return
	 * when matching content is found or it times out, whichever comes first.
	 * @param interest
	 * @param timeout
	 * @return the content object
	 * @throws IOException
	 */
	public ContentObject get(Interest interest, long timeout) throws IOException {		
		while (true) {
			synchronized(_openLock) {
				if( !_isOpen )
					throw new IOException(formatMessage("Handle is closed"));
			}
			try {
				if (_scope != disableScope) {
					if (interest.scope() == null) {
						interest.scope(_scope);
					}
				}
				return getNetworkManager().get(interest, timeout);
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * Put a single content object into the network. This is a low-level put,
	 * and typically should only be called by a flow controller, in response to
	 * a received Interest. Attempting to write to ndnd without having first
	 * received a corresponding Interest violates flow balance, and the content
	 * will be dropped.
	 * @param co the content object to write. This should be complete and well-formed -- signed and
	 * 	so on.
	 * @return the object that was put if successful, otherwise null.
	 * @throws IOException
	 */
	public ContentObject put(ContentObject co) throws IOException {
		boolean interrupted = false;
		do {
			synchronized(_openLock) {
				if( !_isOpen )
					throw new IOException(formatMessage("Handle is closed"));
			}

			try {
				if( Log.isLoggable(Level.FINEST) )
					Log.finest(Log.FAC_NETMANAGER, formatMessage("Putting content on wire: " + co.name()));
				return getNetworkManager().put(co);
			} catch (InterruptedException e) {
				interrupted = true;
			}
		} while (interrupted);
		return null;
	}

	/**
	 * Register a standing interest filter with callback to receive any 
	 * matching interests seen
	 * @param filter
	 * @param callbackHandler
	 */
	public void registerFilter(ContentName filter,
			NDNInterestHandler callbackHandler) throws IOException {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("registerFilter " + filter.toString()));

		synchronized(_openLock) {
			if( !_isOpen )
				throw new IOException(formatMessage("Handle is closed"));
		}

		getNetworkManager().setInterestFilter(this, filter, callbackHandler);
	}
	
	@Deprecated
	public void registerFilter(ContentName filter,
			NDNFilterListener callbackListener) throws IOException {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("registerFilter " + filter.toString()));

		synchronized(_openLock) {
			if( !_isOpen )
				throw new IOException(formatMessage("Handle is closed"));
		}

		getNetworkManager().setInterestFilter(this, filter, callbackListener);
	}
	
	/**
	 * Unregister a standing interest filter
	 * @param filter
	 * @param callbackHandler
	 * @throws IOException if handle is closed
	 */	
	public void unregisterFilter(ContentName filter,
			NDNInterestHandler callbackHandler) {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("unregisterFilter " + filter.toString()));

		synchronized(_openLock) {
			if( !_isOpen ) {
				Log.warning(formatMessage("Called unregisterFilter on a closed handle"));
				return;
			}
		}

		getNetworkManager().cancelInterestFilter(this, filter, callbackHandler);		
	}
	
	@Deprecated
	public void unregisterFilter(ContentName filter,
			NDNFilterListener callbackListener) {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("unregisterFilter " + filter.toString()));

		synchronized(_openLock) {
			if( !_isOpen ) {
				Log.warning(formatMessage("Called unregisterFilter on a closed handle"));
				return;
			}
		}

		getNetworkManager().cancelInterestFilter(this, filter, callbackListener);		
	}
	
	/**
	 * Query, or express an interest in particular
	 * content. This request is sent out over the
	 * NDN to other nodes. On any results, the
	 * callbackHandler if given, is notified.
	 * Results may also be cached in a local repository
	 * for later retrieval by get().
	 * Get and expressInterest could be implemented
	 * as a single function that might return some
	 * content immediately and others by callback;
	 * we separate the two for now to simplify the
	 * interface.
	 * 
	 * Pass it on to the NDNInterestManager to
	 * forward to the network. Also express it to the
	 * repositories we manage, particularly the primary.
	 * Each might generate their own NDNQueryDescriptor,
	 * so we need to group them together.
	 * @param interest
	 * @param handler
	 */
	public void expressInterest(
			Interest interest,
			final NDNContentHandler handler) throws IOException {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("expressInterest " + interest.name().toString()));

		synchronized(_openLock) {
			if( !_isOpen )
				throw new IOException(formatMessage("Handle is closed"));
		}

		// Will add the interest to the listener.
		getNetworkManager().expressInterest(this, interest, handler);
	}
	
	public void registerInterest(
			Interest interest,
			final NDNContentHandler handler) throws IOException {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("expressInterest " + interest.name().toString()));

		synchronized(_openLock) {
			if( !_isOpen )
				throw new IOException(formatMessage("Handle is closed"));
		}

		// Will add the interest to the listener.
		getNetworkManager().registerInterest(this, interest, handler);
	}
	
	@Deprecated
	public void expressInterest(
			Interest interest,
			final NDNInterestListener listener) throws IOException {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("expressInterest " + interest.name().toString()));

		synchronized(_openLock) {
			if( !_isOpen )
				throw new IOException(formatMessage("Handle is closed"));
		}

		if (_scope != disableScope) {
			if (interest.scope() == null) {
				interest.scope(_scope);
			}
			NDNInterestListener myListener = new NDNInterestListener() {
				public Interest handleContent(ContentObject data, Interest interest) {
					Interest i = listener.handleContent(data, interest);
					if (i != null) {
						if (_scope != disableScope) {
							if (i.scope() == null) {
								i.scope(_scope);
							}
						}
					}
					return i;
				}
			};
			getNetworkManager().expressInterest(this, interest, myListener);

		} else {

			// Will add the interest to the listener.
			getNetworkManager().expressInterest(this, interest, listener);
		}
	}

	/**
	 * Cancel this interest. 
	 * @param interest
	 * @param handler Used to distinguish the same interest
	 * 	requested by more than one listener.
	 */
	public void cancelInterest(Interest interest, NDNContentHandler handler) {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("cancelInterest " + interest.name().toString()));

		synchronized(_openLock) {
			if( !_isOpen ) {
				Log.warning(Log.FAC_NETMANAGER, formatMessage("Called cancelInterest on a closed handle"));
				return;
			}
		}

		getNetworkManager().cancelInterest(this, interest, handler);
	}
	
	@Deprecated
	public void cancelInterest(Interest interest, NDNInterestListener listener) {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("cancelInterest " + interest.name().toString()));

		synchronized(_openLock) {
			if( !_isOpen ) {
				Log.warning(Log.FAC_NETMANAGER, formatMessage("Called cancelInterest on a closed handle"));
				return;
			}
		}

		getNetworkManager().cancelInterest(this, interest, listener);
	}

	/**
	 * Shutdown the handle and its associated resources
	 */
	public void close() {
		if( Log.isLoggable(Level.FINE) )
			Log.fine(Log.FAC_NETMANAGER, formatMessage("Closing handle"));

		synchronized(_openLock) {
			if( _isOpen ) {
				_isOpen = false;
				_networkManager.shutdown();
			} else {
				Log.warning(Log.FAC_NETMANAGER, formatMessage("Handle is already closed.  DIAGNOSTIC STACK DUMP."));
				Thread.dumpStack();
			}
		}

		synchronized (NDNHandle.class) {
			if (_handle == this) {
				_handle = null;
			}
		}
	}

	/**
	 * Provide a default verification implementation for users that do not want to
	 * alter the standard verification process.
	 * @return a basic ContentVerifier that simply verifies that a piece of content was
	 * 	correctly signed by the key it claims to have been published by, implying no
	 * 	semantics about the "trustworthiness" of that content or its publisher
	 */
	public ContentVerifier defaultVerifier() {
		return _keyManager.getDefaultVerifier();
	}
	
	protected String formatMessage(String message) {
		return _handleIdString + message;
	}
}
