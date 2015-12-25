/*
 * A NDNx example of extending ndnb encoding/decoding.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2010, 2011 Palo Alto Research Center, Inc.
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


package org.ndnx.ndn.apps.examples.ndnb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import org.ndnx.ndn.apps.examples.ndnb.Enumeration;
import org.ndnx.ndn.apps.examples.ndnb.Example2Integers;
import org.ndnx.ndn.apps.examples.ndnb.ExampleComplicated;
import org.ndnx.ndn.apps.examples.ndnb.ExampleDictionary;
import org.ndnx.ndn.apps.examples.ndnb.StringBinaryVector;
import org.ndnx.ndn.impl.encoding.BinaryXMLDictionary;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.test.impl.encoding.XMLEncodableTester;
import org.junit.Test;

public class ExampleEncodeDecodeTest {
	
	static {
		BinaryXMLDictionary.pushGlobalXMLDictionary(ExampleDictionary.getDefaultInstance());
	}
	
	public ByteArrayOutputStream encodeOutput(String typeName, GenericXMLEncodable toEncode) {
		System.out.println("Encoding " + typeName + ": " + toEncode);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			toEncode.encode(baos);
		} catch (ContentEncodingException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		System.out.print("Encoded " + typeName + ": ");
		System.out.println(DataUtils.printHexBytes(baos.toByteArray()));
		return baos;
	}
	
	public boolean decodeOutput(String typeName, GenericXMLEncodable decodeTo, ByteArrayOutputStream baos) {
		System.out.println("Decoding " + typeName + ": " + DataUtils.printHexBytes(baos.toByteArray()));
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try {
			decodeTo.decode(bais);
		} catch (ContentDecodingException e) {
			System.out.println("Exception " + e.getClass().getName() + ", message: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Decoded  " + typeName + ": " + decodeTo);
		return true;
	}

	/*
	 * The first set of tests is to verify that Example2Integers correctly encodes
	 * and decodes.
	 */

	public final static Integer integer1 = 100;
	public final static Integer integer2 = 200;
	
	public Example2Integers setupE2I() {
		Example2Integers e2i = new Example2Integers(integer1, integer2);
		return e2i;
	}

	@Test
	public void testE2IEncodeOutputStream() {
		System.out.println();
		System.out.println("Running: testE2IEncodeOutputStream");
		Example2Integers e2iToEncode = setupE2I();
		encodeOutput("Example2Integers", e2iToEncode);
	}

	@Test
	public void testE2IDecodeInputStream() {
		System.out.println();
		System.out.println("Running: testE2IEncodeOutputStream");
		Example2Integers e2iToEncode = setupE2I();
		ByteArrayOutputStream baos = encodeOutput("Example2Integers", e2iToEncode);
		assertNotNull(baos);
		Example2Integers e2iToDecodeTo = new Example2Integers();
		decodeOutput("Example2Integers", e2iToDecodeTo, baos);
		assertEquals("Integer1 missmatch", integer1, e2iToDecodeTo.getInteger1());
		assertEquals("Integer2 missmatch", integer2, e2iToDecodeTo.getInteger2());
	}
	
	@Test
	public void testE2IEncodingDecoding() {
		System.out.println();
		System.out.println("Running: testE2IEncodeOutputStream");
		Example2Integers e2iToEncode = setupE2I();
		Example2Integers  e2iToDecodeInto = new Example2Integers();
		assertNotNull(e2iToDecodeInto);
		Example2Integers  binaryE2IToDecodeInto = new Example2Integers();
		assertNotNull(binaryE2IToDecodeInto);
		XMLEncodableTester.encodeDecodeTest("Example2Integers", e2iToEncode, e2iToDecodeInto, binaryE2IToDecodeInto);
		assertEquals("Integer1 missmatch", integer1, e2iToDecodeInto.getInteger1());
		assertEquals("Integer2 missmatch", integer2, e2iToDecodeInto.getInteger2());
		assertEquals("Integer1 missmatch binary form", integer1, binaryE2IToDecodeInto.getInteger1());
		assertEquals("Integer2 missmatch binary form", integer2, binaryE2IToDecodeInto.getInteger2());
	}


	public final static byte[] binary1 = "This Is Binary1 From A string".getBytes();
	public final static byte[] binary2 = "This Is Binary2 From A string".getBytes();
	public final static byte[] binary3 = "This Is Binary3 From A string".getBytes();
	public final static byte[] binary4 = "This Is Binary4 From A string".getBytes();
	public final static String[] cna1 = {"a", "b", "c", "d"};
	public final static ContentName contentName1 = ContentName.fromNative(cna1);
	public final static String[] cna2 = {"b", "c", "d"};
	public final static ContentName contentName2 = ContentName.fromNative(cna2);
	public final static String[] cna3 = {"c", "d"};
	public final static ContentName contentName3 = ContentName.fromNative(cna3);
	public final static String[] cna4 = {"d"};
	public final static ContentName contentName4 = ContentName.fromNative(cna4);
	
	public final static byte[] digest = {0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 
										0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 
										0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 
										0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, 0x0a, };
	
	public final static Interest interest1 = new Interest(contentName1);
	public final static Interest interest2 = new Interest(contentName2, new PublisherPublicKeyDigest(digest));
	public final static Interest interest3 = new Interest(contentName3);
	public final static Interest interest4 = new Interest(contentName4, new PublisherPublicKeyDigest(digest));
	public final static Vector<ContentName> names = new Vector<ContentName>();
	public final static Vector<Interest> interests = new Vector<Interest>();

	public StringBinaryVector setupSBV(boolean simple, boolean useCNs, boolean useIs) {
		StringBinaryVector sbv = null;
		if (simple) {
			sbv = new StringBinaryVector("Simple SBV string", binary1);
			return sbv;
		}
		
		names.clear();
		interests.clear();
		if (useCNs && useIs) {
			names.add(contentName1);
			names.add(contentName2);
			interests.add(interest1);
			interests.add(interest2);
			sbv = new StringBinaryVector("Complicated SBV string using both ContentNames and Interests", names, interests, binary1);
			return sbv;

		} else if (useCNs) {
			sbv = new StringBinaryVector("Complicated SBV string using ContentNames", binary1);
			sbv.addName(contentName1);
			sbv.addName(contentName2);
			return sbv;

		} else if (useIs) {
			sbv = new StringBinaryVector("Complicated SBV string using Interests", binary1);
			sbv.addInterest(interest1);
			sbv.addInterest(interest2);
			return sbv;

		}
		
		/* 
		 * We can never get here, but the compiler doesn't detect this.
		 */
		return null;
	}
	
	
	@Test
	public void testSimpleSBVEncodeOutputStream() {
		System.out.println();
		System.out.println("Running: testSimpleSBVEncodeOutputStream");
		StringBinaryVector sbvToEncode = setupSBV(true, false, false);
		encodeOutput("StringBinaryVector (simple)", sbvToEncode);
	}

	@Test
	public void testSimpleSBVDecodeInputStream() {
		System.out.println();
		System.out.println("Running: testSimpleSBVDecodeInputStream");
		StringBinaryVector sbvToEncode = setupSBV(true, false, false);
		ByteArrayOutputStream baos = encodeOutput("StringBinaryVector (simple)", sbvToEncode);
		assertNotNull(baos);
		StringBinaryVector sbvToDecodeTo = new StringBinaryVector();
		decodeOutput("StringBinaryVector (simple)", sbvToDecodeTo, baos);
	}
	
	@Test
	public void testSimpleSBVEncodingDecoding() {
		System.out.println();
		System.out.println("Running: testSimpleSBVEncodingDecoding");
		StringBinaryVector sbvToEncode = setupSBV(true, false, false);
		StringBinaryVector sbvToDecodeTo = new StringBinaryVector();
		assertNotNull(sbvToDecodeTo);
		StringBinaryVector binarySBVToDecodeTo = new StringBinaryVector();
		assertNotNull(binarySBVToDecodeTo);
		XMLEncodableTester.encodeDecodeTest("StringBinaryVector (simple)", sbvToEncode, sbvToDecodeTo, binarySBVToDecodeTo);
	}
	
	

	@Test
	public void testComplexSBVEncodeOutputStream() {
		System.out.println();
		System.out.println("Running: testComplexSBVEncodeOutputStream");
		StringBinaryVector sbvToEncode = setupSBV(false, true, true);
		encodeOutput("StringBinaryVector (complex)", sbvToEncode);
	}

	@Test
	public void testComplexSBVDecodeInputStream() {
		System.out.println();
		System.out.println("Running: testComplexSBVDecodeInputStream");
		StringBinaryVector sbvToEncode = setupSBV(false, true, true);
		ByteArrayOutputStream baos = encodeOutput("StringBinaryVector (complex)", sbvToEncode);
		assertNotNull(baos);
		StringBinaryVector sbvToDecodeTo = new StringBinaryVector();
		decodeOutput("StringBinaryVector (complex)", sbvToDecodeTo, baos);
	}
	
	@Test
	public void testComplexSBVEncodingDecoding() {
		System.out.println();
		System.out.println("Running: testComplexSBVEncodingDecoding");
		StringBinaryVector sbvToEncode = setupSBV(false, true, true);
		StringBinaryVector sbvToDecodeTo = new StringBinaryVector();
		assertNotNull(sbvToDecodeTo);
		StringBinaryVector binarySBVToDecodeTo = new StringBinaryVector();
		assertNotNull(binarySBVToDecodeTo);
		XMLEncodableTester.encodeDecodeTest("StringBinaryVector (complex)", sbvToEncode, sbvToDecodeTo, binarySBVToDecodeTo);
	}
	
	
	
	public ExampleComplicated setupEC(int nData, boolean simple, Enumeration e) {
		Vector<StringBinaryVector> data = new Vector<StringBinaryVector>(nData);
		for (int i = 0; i < nData; i++) {
			StringBinaryVector sbv = setupSBV(simple, true, true);
			data.add(sbv);
		}
		ExampleComplicated ec = new ExampleComplicated("Simple EC string", e, data);
		return ec;
	}

	@Test
	public void testSimpleECEncodeOutputStream() {
		System.out.println();
		System.out.println("Running: testSimpleECEncodeOutputStream");
		ExampleComplicated ecToEncode = setupEC(1, true, Enumeration.Enu2);
		encodeOutput("ExampleComplicated (simple)", ecToEncode);
	}

	@Test
	public void testSimpleECDecodeInputStream() {
		System.out.println();
		System.out.println("Running: testSimpleECDecodeInputStream");
		ExampleComplicated ecToEncode = setupEC(1, true, Enumeration.Enu0);
		ByteArrayOutputStream baos = encodeOutput("ExampleComplicated (simple)", ecToEncode);
		assertNotNull(baos);
		ExampleComplicated ecToDecodeTo = new ExampleComplicated();
		decodeOutput("ExampleComplicated (simple)", ecToDecodeTo, baos);
	}
	
	@Test
	public void testSimpleECEncodingDecoding() {
		System.out.println();
		System.out.println("Running: testSimpleECEncodingDecoding");
		ExampleComplicated ecToEncode = setupEC(1, true, Enumeration.Enu1);
		ExampleComplicated ecToDecodeTo = new ExampleComplicated();
		assertNotNull(ecToDecodeTo);
		ExampleComplicated binaryECToDecodeTo = new ExampleComplicated();
		assertNotNull(binaryECToDecodeTo);
		XMLEncodableTester.encodeDecodeTest("ExampleComplicated (simple)", ecToEncode, ecToDecodeTo, binaryECToDecodeTo);
	}
	
	@Test
	public void testComplexECEncodeOutputStream() {
		System.out.println();
		System.out.println("Running: testComplexECEncodeOutputStream");
		ExampleComplicated ecToEncode = setupEC(2, false, Enumeration.Enu0);
		encodeOutput("ExampleComplicated (Complex)", ecToEncode);
	}

	@Test
	public void testComplexECDecodeInputStream() {
		System.out.println();
		System.out.println("Running: testComplexECDecodeInputStream");
		ExampleComplicated ecToEncode = setupEC(2, false, Enumeration.Enu1);
		ByteArrayOutputStream baos = encodeOutput("ExampleComplicated (Complex)", ecToEncode);
		assertNotNull(baos);
		ExampleComplicated ecToDecodeTo = new ExampleComplicated();
		decodeOutput("ExampleComplicated (Complex)", ecToDecodeTo, baos);
	}
	
	@Test
	public void testComplexECEncodingDecoding() {
		System.out.println();
		System.out.println("Running: testComplexECEncodingDecoding");
		ExampleComplicated ecToEncode = setupEC(2, false, Enumeration.Enu2);
		ExampleComplicated ecToDecodeTo = new ExampleComplicated();
		assertNotNull(ecToDecodeTo);
		ExampleComplicated binaryECToDecodeTo = new ExampleComplicated();
		assertNotNull(binaryECToDecodeTo);
		XMLEncodableTester.encodeDecodeTest("ExampleComplicated (Complex)", ecToEncode, ecToDecodeTo, binaryECToDecodeTo);
	}
	

}
