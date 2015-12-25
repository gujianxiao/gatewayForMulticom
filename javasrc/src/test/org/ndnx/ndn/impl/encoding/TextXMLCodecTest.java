/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011, 2012, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl.encoding;

import java.text.ParseException;

import junit.framework.Assert;

import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.TextXMLCodec;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.protocol.NDNTime;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test portions of the encoder/decoder infrastructure related to encoding dates
 * and times. Largely superseded by NDNTime.
 */
public class TextXMLCodecTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}
	
	@Test
	public void testTagMap() {
		Log.info(Log.FAC_TEST, "Starting testTagMap");

		String name;
		Long tag;
		for (int i=1; i <= NDNProtocolDTags.Parameters; ++i) {
			name = NDNProtocolDTags.tagToString(i);
			if (name != null) {
				tag = NDNProtocolDTags.stringToTag(name);
				Assert.assertEquals(tag.longValue(), i);
			}
		}
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.Interest);
		Assert.assertEquals("Interest", name);
		
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.ExtOpt);
		Assert.assertEquals("ExtOpt", name);

		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.RootDigest);
		Assert.assertEquals("RootDigest", name);

		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.Nonce);
		Assert.assertEquals("Nonce", name);

		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.AnswerOriginKind);
		Assert.assertEquals("AnswerOriginKind", name);
		
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.Witness);
		Assert.assertEquals("Witness", name);

		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.FinalBlockID);
		Assert.assertEquals("FinalBlockID", name);
		
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.EncryptedKey);
		Assert.assertEquals("EncryptedKey", name);
		
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.BinaryValue);
		Assert.assertEquals("BinaryValue", name);
		
		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.ProfileName);
		Assert.assertEquals("ProfileName", name);

		name = NDNProtocolDTags.tagToString(NDNProtocolDTags.Parameters);
		Assert.assertEquals("Parameters", name);
		
		Log.info(Log.FAC_TEST, "Completed testTagMap");
	}
	
	@Test
	public void testParseDateTime() {
		Log.info(Log.FAC_TEST, "Starting testParseDateTime");

		NDNTime now = NDNTime.now();
		testDateTime(now);
		
		now.setNanos(384);
		testDateTime(now);

		now.setNanos(1105384);
		testDateTime(now);
		now.setNanos(550105384);
		testDateTime(now);
		now.setNanos(550000000);
		
		testDateTime(now);
		now.setNanos(953405384);
		testDateTime(now);
		
		now.setNanos(110672800);
		testDateTime(now);
		
		Log.info(Log.FAC_TEST, "Completed testParseDateTime");
	}
	
	public void testDateTime(NDNTime testDateTime) {
		String strDateTime = TextXMLCodec.formatDateTime(testDateTime);
		System.out.println("DateTime: " + testDateTime + " XML version: " + strDateTime);
		NDNTime parsedDateTime = null;
		try {
			parsedDateTime = TextXMLCodec.parseDateTime(strDateTime);
		} catch (ParseException e) {
			System.out.println("Exception parsing date time: " + e.getMessage());
			e.printStackTrace();
			Assert.fail("Failed to parse date time: " + strDateTime);
		}
		System.out.println("Parsed version: " + parsedDateTime);
		if (!parsedDateTime.equals(testDateTime)) {
			System.out.println("Time : " + parsedDateTime + "(long: " + parsedDateTime.getTime() + ") does not equal " + testDateTime + "(long: " + testDateTime.getTime() + ")");
		}
		Assert.assertTrue(parsedDateTime.equals(testDateTime));
	}

}
