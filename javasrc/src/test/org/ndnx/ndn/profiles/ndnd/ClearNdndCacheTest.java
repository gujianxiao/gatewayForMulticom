/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011-2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.ndnd;

import junit.framework.Assert;

import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.profiles.ndnd.NDNDCacheManager;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.junit.Test;

public class ClearNdndCacheTest extends NDNTestBase {
	static NDNTestHelper testHelper = new NDNTestHelper(ClearNdndCacheTest.class);
	
	@Test
	public void testClearCache() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testClearCache");

		ContentName prefix = new ContentName(testHelper.getClassNamespace(), "AreaToClear");
		NDNFlowControl fc = new NDNFlowControl(prefix, putHandle);
		for (int i = 0; i < 10; i++) {
			ContentName name = new ContentName(prefix, "head-" + i);
			ContentObject obj = ContentObject.buildContentObject(name, "test".getBytes());
			fc.put(obj);
			obj = getHandle.get(prefix, SystemConfiguration.MEDIUM_TIMEOUT);
			Assert.assertNotNull(obj);
			for (int j = 0; j < 10; j++) {
				ContentName subName = new ContentName(name, "subObj-" + j);
				ContentObject subObj = ContentObject.buildContentObject(subName, "test".getBytes());
				fc.put(subObj);
				obj = getHandle.get(prefix, SystemConfiguration.MEDIUM_TIMEOUT);
				Assert.assertNotNull(obj);
			}
		}
		fc.close();
		new NDNDCacheManager().clearCache(prefix, getHandle, 100 * SystemConfiguration.SHORT_TIMEOUT);
		
		for (int i = 0; i < 10; i++) {
			ContentName name = new ContentName(prefix, "head-" + i);
			ContentObject co = getHandle.get(name, SystemConfiguration.SHORT_TIMEOUT);
			Assert.assertEquals(null, co);
			for (int j = 0; j < 10; j++) {
				ContentName subName = new ContentName(name, "subObj-" + j);
				co = getHandle.get(subName, SystemConfiguration.SHORT_TIMEOUT);
				Assert.assertEquals(null, co);
			}
		}
		
		Log.info(Log.FAC_TEST, "Completed testClearCache");
	}
}
