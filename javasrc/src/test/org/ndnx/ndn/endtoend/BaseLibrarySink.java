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

package org.ndnx.ndn.endtoend;

import java.util.Random;
import java.util.concurrent.Semaphore;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.ContentObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * Part of the end to end test infrastructure.
 * NOTE: This test requires ndnd to be running and complementary source process
 */
public class BaseLibrarySink {
	static NDNHandle handle = null;
	Semaphore sema = new Semaphore(0);
	int next = 0;
	protected static Throwable error = null; // for errors in callback
	protected static Random rand;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		handle = NDNHandle.open();
		rand = new Random();
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		handle.close();
	}

	/**
	 * Subclassible object processing operations, to make it possible to easily
	 * implement tests based on this one.
	 * @author smetters
	 *
	 */
	public void checkGetResults(ContentObject getResults) {
		Log.info(Log.FAC_TEST, "Got result: " + getResults.name());
	}
}
