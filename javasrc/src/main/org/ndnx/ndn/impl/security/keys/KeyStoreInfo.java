/*
 * Part of the NDNx Java Library.
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
package org.ndnx.ndn.impl.security.keys;

import java.security.KeyStore;

import org.ndnx.ndn.protocol.NDNTime;

/**
 * Track a set of information about keystore files that we load, so that we can use it
 * (e.g. versioning) for representing those keys in NDN, or storing related configuration
 * data in the same directory.
 *
 */
public class KeyStoreInfo {
	// Where did we load this from
	String _keyStoreURI;
	String _configurationFileURI;
	KeyStore _keyStore;
	NDNTime _version;
	
	public KeyStoreInfo(String keyStoreURI, KeyStore keyStore, NDNTime version) {
		_keyStoreURI = keyStoreURI;
		_keyStore = keyStore;
		_version = version;
	}
	
	/**
	 * In case we don't know the 
	 * @param keyStore
	 */
	public void setKeyStore(KeyStore keyStore) {
		_keyStore = keyStore;
	}
	
	public void setVersion(NDNTime version) {
		_version = version;
	}
	
	public void setConfigurationFileURI(String configurationFileURI) {
		_configurationFileURI = configurationFileURI;
	}
	
	public KeyStore getKeyStore() { return _keyStore; }
	public NDNTime getVersion() { return _version; }
	public String getKeyStoreURI() { return _keyStoreURI; }
	public String getConfigurationFileURI() { return _configurationFileURI; }

}
