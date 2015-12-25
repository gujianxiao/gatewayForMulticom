/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011, 2013 Palo Alto Research Center, Inc.
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.InterestTable;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;

/**
 * An enhanced NDNHandle used for logging/tracking during tests.
 */
public class NDNLibraryTestHarness extends NDNHandle {
	
	private ConcurrentLinkedQueue<ContentObject> _outputQueue = new ConcurrentLinkedQueue<ContentObject>();
	private InterestTable<NDNInterestHandler> _handlers = new InterestTable<NDNInterestHandler>();

	public NDNLibraryTestHarness() throws ConfigurationException,
			IOException {
		super(false);
	}
	
	public void reset() {
		_outputQueue.clear();
		_handlers.clear();
	}
	
	public Queue<ContentObject> getOutputQueue() {
		return _outputQueue;
	}
	
	@Override
	public ContentObject put(ContentObject co) throws IOException {
		_outputQueue.add(co);
		return co;
	}
	
	@Override
	public void registerFilter(ContentName filter,
			NDNInterestHandler handler) {
		_handlers.add(new Interest(filter), handler);
	}
	
	@Override
	public void unregisterFilter(ContentName filter,
			NDNInterestHandler handler) {
		_handlers.remove(new Interest(filter), handler);		
	}
	
	@Override
	public ContentObject get(Interest interest, long timeout) throws IOException {
		for (NDNInterestHandler handler : _handlers.getValues(interest.name())) {
			handler.handleInterest(interest);
		}
		return _outputQueue.remove();
	}
	
	@Override
	public ContentObject get(ContentName name, long timeout) throws IOException {
		Interest interest = new Interest(name);
		return get(interest, timeout);
	}
}
