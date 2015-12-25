/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011 Palo Alto Research Center, Inc.
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

import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;

/**
 * This is the lowest-level interface to NDN, and describes in its entirety,
 * the interface the library has to speak to the NDN network layer. It consists of only a small number
 * of methods, all other operations in NDN are built on top of these methods together
 * with the constraint specifications allowed by Interest.
 * 
 * Clients wishing to build simple test programs can access an implementation of
 * these methods most easily using the NDNReader and NDNWriter class. Clients wishing
 * to do more sophisticated IO should look at the options available in the
 * org.ndnx.ndn.io and org.ndnx.ndn.io.content packages.
 * 
 * @see NDNHandle
 */
public interface NDNBase {
	
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
	public ContentObject put(ContentObject co) throws IOException;
	
	/**
	 * Get a single piece of content from NDN. This is a blocking get, it will return
	 * when matching content is found or it times out, whichever comes first.
	 * @param interest
	 * @param timeout
	 * @return the content object
	 * @throws IOException
	 */
	public ContentObject get(Interest interest, long timeout) throws IOException;
	
	/**
	 * Register a standing interest filter with callback to receive any 
	 * matching interests seen
	 * @param filter
	 * @param callbackListener
	 * @throws IOException 
	 */
	public void registerFilter(ContentName filter,
							   NDNInterestHandler callbackHandler) throws IOException;
	
	/**
	 * Unregister a standing interest filter
	 * @param filter
	 * @param callbackListener
	 */
	public void unregisterFilter(ContentName filter,
								 NDNInterestHandler callbackHandler);
	
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
	 */
	public void expressInterest(
			Interest interest,
			NDNContentHandler handler) throws IOException;

	/**
	 * Cancel this interest. 
	 * @param interest
	 * @param listener Used to distinguish the same interest
	 * 	requested by more than one handler.
	 */
	public void cancelInterest(Interest interest, NDNContentHandler handler);
}
