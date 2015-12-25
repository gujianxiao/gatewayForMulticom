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
package org.ndnx.ndn.impl;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.Test;

/**
 * This class contains tests that can be used for diagnosis or other purposes which should
 * not be run as part of the standard test suite
 */
public class NetworkTestExtra extends NDNTestBase implements NDNContentHandler {
	
	static NDNTestHelper testHelper = new NDNTestHelper(NetworkTestExtra.class);
	
	@Test
	public void testThreadOverflow() {
		ContentName name = new ContentName(testHelper.getClassNamespace(), "overflow-test");
		int i = 0;
		while (true) {
			ContentObject obj = ContentObject.buildContentObject(name, ("test-" + i).getBytes());
			try {
				getHandle.expressInterest(new Interest(obj.name()), this);
				putHandle.put(obj);
				Thread.sleep(100);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public Interest handleContent(ContentObject data, Interest interest) {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}

}
