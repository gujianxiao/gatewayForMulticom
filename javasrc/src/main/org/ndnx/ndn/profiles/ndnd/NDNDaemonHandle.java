/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2012 Palo Alto Research Center, Inc.
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

package	org.ndnx.ndn.profiles.ndnd;

import static org.ndnx.ndn.protocol.ContentName.ROOT;

import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.ContentVerifier;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.NDNNetworkManager.RegisteredPrefix;
import org.ndnx.ndn.impl.encoding.BinaryXMLCodec;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.protocol.Component;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo;

/**
 * Helper class to access NDND information.
 *
 */
public class NDNDaemonHandle {
	
	protected NDNNetworkManager _manager;
	
	public NDNDaemonHandle() { }
	
	public NDNDaemonHandle(NDNNetworkManager manager)  throws NDNDaemonException {
		_manager = manager;
	}
		
	public NDNDaemonHandle(NDNHandle handle)  throws NDNDaemonException {
		_manager = handle.getNetworkManager();
	}
			
	public static String idToString(PublisherPublicKeyDigest digest) {
		byte [] digested;
		digested = digest.digest();
		return Component.printURI(digested);
	}
	
	/**
	 * Send the request for the prefix registration or deregistration to ndnd
	 * 
	 * @param interestNamePrefix
	 * @param encodeMe
	 * @param prefix contains callback for asynchronous requests
	 * @param wait if true wait for return content from ndnd
	 * @return data returned from ndnd in "no wait" case
	 * 
	 * @throws NDNDaemonException
	 */
	protected byte[] sendIt(ContentName interestNamePrefix, GenericXMLEncodable encodeMe, RegisteredPrefix prefix, boolean wait) throws NDNDaemonException {
		byte[] encoded;
		try {
			encoded = encodeMe.encode(BinaryXMLCodec.CODEC_NAME);
		} catch (ContentEncodingException e) {
			String reason = e.getMessage();
			Log.info("Unexpected error encoding encodeMe parameter.  reason: " + e.getMessage());
			throw new IllegalArgumentException("Unexpected error encoding encodeMe parameter.  reason: " + reason);
		}
		KeyManager keyManager = _manager.getKeyManager();
		ContentObject contentOut = ContentObject.buildContentObject(ROOT, SignedInfo.ContentType.DATA, 
														encoded, 
														keyManager.getDefaultKeyID(), 
														new KeyLocator(keyManager.getDefaultPublicKey()), keyManager, 
														/* finalBlockID */ null);
		byte[] contentOutBits;
		try {
			contentOutBits = contentOut.encode(BinaryXMLCodec.CODEC_NAME);
		} catch (ContentEncodingException e) {
			String msg = ("Unexpected ContentEncodingException, reason: " + e.getMessage());
			Log.info(msg);
			throw new NDNDaemonException(msg);
		}
		
		/*
		 * Add the contentOut bits to the name that's passed in.
		 */
		interestNamePrefix = new ContentName(interestNamePrefix, contentOutBits);
		Interest interested = new Interest(interestNamePrefix);
		interested.scope(1);
		ContentObject contentIn = null;

		try {
			if (wait) {
				contentIn = _manager.get(interested, SystemConfiguration.NDND_OP_TIMEOUT);
			} else {
				if (null != prefix) {
					_manager.expressInterest(this, interested, prefix);
				} else
					_manager.write(interested);
			}
		} catch (IOException e) {
			String msg = ("Unexpected IOException in call getting NDNDaemonHandle.sendIt return value, reason: " + e.getMessage());
			Log.info(msg);
			throw new NDNDaemonException(msg);
		} catch (InterruptedException e) {
			String msg = ("Unexpected InterruptedException in call getting NDNDaemonHandle.sendIt return value, reason: " + e.getMessage());
			Log.info(msg);
			throw new NDNDaemonException(msg);
		}
		
		if (wait) {
			if (null == contentIn) {
				String msg = ("Fetch of content from face or prefix registration call failed due to timeout.");
				Log.info(msg);
				throw new NDNDaemonException(msg);
			}
			
			PublisherPublicKeyDigest sentID = contentIn.signedInfo().getPublisherKeyID();
			ContentVerifier verifyer = new ContentObject.SimpleVerifier(sentID, _manager.getKeyManager());
			if (!verifyer.verify(contentIn)) {
				String msg = ("NDNDIdGetter: Fetch of content reply failed to verify.");
				Log.severe(msg);
				throw new NDNDaemonException(msg);
			}
			
			if (contentIn.isNACK()) {
				String msg = ("Received NACK in response to registration/unregistration request");
				Log.fine(msg);
				throw new NDNDaemonException(msg);  // FIX THIS TO GET THE CODE/MESSAGE from the StatusResponse
			}

			byte[] payloadOut = contentIn.content();
			return payloadOut;
		}
		return null;
	} /* protected byte[] sendIt(ContentName interestNamePrefix, byte[] payloadIn) throws NDNDaemonException */

}
