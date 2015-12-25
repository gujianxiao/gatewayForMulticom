/*
 * A NDNx command line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.ndnx.ndn.impl.security.crypto.util.CryptoUtil;
import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;

/**
 * Command-line utility program to verify NDNx objects stored in a file.
 */
public class ndn_verify {

	public static void usage() {
		System.out.println("ndn_verify [key_file] ndnb_input_file [input_file [input_file...]] (if no key file, input file must contain KEY type key locator)");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1)  {
			usage();
			return;
		}
		try {

			PublicKey pubKey = null;
			int start = 0;

			if (args.length > 1) {
				pubKey = readKeyFile(args[0]);
				start++;
			}

			for (int i=start; i < args.length; ++i) {
				ContentObject co = readObjectFile(args[i]);

				if ((null == pubKey) && (KeyLocator.KeyLocatorType.KEY == co.signedInfo().getKeyLocator().type())) {
					pubKey = co.signedInfo().getKeyLocator().key();
				}

				if (null != pubKey) {
					if (!co.verify(pubKey)) {
						System.out.println("BAD: Object: " + co.name() + " in file: " + args[i] + " failed to verify.");

						debugSig(pubKey, co.signature().signature());
					} else {
						System.out.println("GOOD: Object: " + co.name() + " in file: " + args[i] + " verified.");
						debugSig(pubKey, co.signature().signature());
					}
				} else {
					System.out.println("NO KEY PROVIDED TO VERIFY OBJECT: " + co.name());
				}
			}
		} catch (Exception e) {
			System.out.println("Exception in ndn_verify: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void debugSig(PublicKey pubKey, byte [] signature) {
		
		if (!(pubKey instanceof RSAPublicKey)) {
			return;
		}
		// take it apart
		RSAPublicKey rsaKey = (RSAPublicKey) pubKey;
		BigInteger sigInt = new BigInteger(1, signature);
		BigInteger sigSigInt = new BigInteger(signature);
		System.out.println("Signature length " + signature.length + " sign? " + sigSigInt.signum());

		BigInteger paddedMessage = sigInt.modPow(rsaKey.getPublicExponent(), rsaKey.getModulus());
		System.out.println("\nSignature: " + DataUtils.printHexBytes(signature) + "\n");
		System.out.println("Inverted signature: " + DataUtils.printHexBytes(paddedMessage.toByteArray()) + "\n");
	
		if (sigSigInt.signum() < 0) {
			BigInteger paddedSignedMessage = sigSigInt.modPow(rsaKey.getPublicExponent(), rsaKey.getModulus());
			System.out.println("Inverted signed signature: " + DataUtils.printHexBytes(paddedSignedMessage.toByteArray()) + "\n");
		
		}
	}

	public static ContentObject readObjectFile(String filePath) 
	throws ContentDecodingException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(filePath);
		BufferedInputStream bis = new BufferedInputStream(fis);
		ContentObject co = new ContentObject();
		co.decode(bis);
		return co;

	}

	public static PublicKey readKeyFile(String filePath) 
	throws ContentDecodingException, IOException, FileNotFoundException, 
	CertificateEncodingException, InvalidKeySpecException, NoSuchAlgorithmException {
		ContentObject keyObject = readObjectFile(filePath);
		try {
			return CryptoUtil.getPublicKey(keyObject.content());
		} catch (InvalidKeySpecException e) {
			System.out.println("Exception decoding public key! " + filePath + " " + e.getClass().getName() + ": " + e.getMessage());
			FileOutputStream fos = new FileOutputStream("contentDump.der");
			fos.write(keyObject.content());
			fos.close();
			throw e;
		}
	}

}
