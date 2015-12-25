/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.io.content;

import java.util.Collection;
import java.util.TreeMap;

import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.KeyValueSet;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.ndnx.ndn.impl.encoding.XMLEncodableTester;
import org.junit.Assert;
import org.junit.Test;

/**
 * Implicitly tests KeyValuePair also
 */
public class KeyValueSetTest {
	
	private static String[] testKeys = {"test1", "test2", "test3"};
	public static void setUpBeforeClass() throws Exception {
		
	}

	@Test
	public void testData() {
		Log.info(Log.FAC_TEST, "Starting testData");

		KeyValueSet kvs = new KeyValueSet();
		Assert.assertNotNull(kvs);
		Assert.assertTrue(kvs.validate());
		try {
			kvs.put("test1", 1);
			kvs.put("test2", 1.5F);
			kvs.put("test3", "test3");
			kvs.put("test4", new byte[]{1,2,3,4});
			kvs.put("test5", ContentName.fromNative("/ndn/TestKeySetTest"));
		} catch (Exception e) {
			Assert.fail("Couldn't put valid value into KeyValueSet");
		}
		try {
			kvs.put("test6", new KeyValueSetTest());
			Assert.fail("Attempt to put bad value succeeded");
		} catch (Exception e) {} // Exception expected
		try {
			kvs.put("test5", 2);
			Assert.fail("Attempt to put duplicate value succeeded");
		} catch (Exception e) {} // Exception expected
		
		Log.info(Log.FAC_TEST, "Completed testData");
	}

	@Test
	public void testMethods() {
		Log.info(Log.FAC_TEST, "Starting testMethods");

		KeyValueSet kvs = new KeyValueSet();
		kvs.put("test3", 3);
		kvs.put("test2", 2);
		kvs.put("test1", 1);
		Assert.assertTrue(kvs.size() == 3);
		Assert.assertEquals(kvs.get("test1"), 1);
		Assert.assertEquals(kvs.get("test4"), null);
		Assert.assertTrue(kvs.containsKey("test1"));
		Assert.assertFalse(kvs.containsKey("test4"));
		Assert.assertTrue(kvs.containsValue(1));
		Assert.assertFalse(kvs.containsValue(4));
		Assert.assertFalse(kvs.isEmpty());
		int i = 0;
		for (String k : kvs.keySet()) {
			Assert.assertEquals(k, testKeys[i++]);
		}
		TreeMap<String, Object>testMap = new TreeMap<String, Object>();
		testMap.put("test4", 4);
		testMap.put("test5", 5);
		testMap.put("test6", 6);
		kvs.putAll(testMap);
		Assert.assertEquals(kvs.size(), 6);
		Assert.assertTrue(kvs.containsValue(6));
		Assert.assertEquals(kvs.remove("test6"), 6);
		Assert.assertFalse(kvs.containsValue(6));
		Assert.assertEquals(kvs.size(), 5);
		Collection<Object> c = kvs.values();
		i = 0;
		for (Object o : c) {
			Assert.assertEquals(o, i+1);
			i++;
		}
		kvs.clear();
		Assert.assertTrue(kvs.isEmpty());
		
		Log.info(Log.FAC_TEST, "Completed testMethods");
	}

	@Test
	public void testEncodeDecode() throws MalformedContentNameStringException {
		Log.info(Log.FAC_TEST, "Starting testEncodeDecode");

		KeyValueSet cd = new KeyValueSet();
		KeyValueSet cdec = new KeyValueSet();
		KeyValueSet bdec = new KeyValueSet();

		cd.put("test1", 1);
		cd.put("test2", 1.5F);
		cd.put("test3", "test3");
		cd.put("test4", new byte[]{1,2,3,4});
		cd.put("test5", ContentName.fromNative("/ndn/TestKeySetTest"));
		XMLEncodableTester.encodeDecodeTest("Collection", cd, cdec, bdec);
		
		Log.info(Log.FAC_TEST, "Completed testEncodeDecode");
	}
}
