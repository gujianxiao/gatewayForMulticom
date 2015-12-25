/*
 * A NDNx command line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2012 Palo Alto Research Center, Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.ndnx.ndn.NDNContentHandler;
import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.encoding.BinaryXMLCodec;
import org.ndnx.ndn.impl.encoding.TextXMLCodec;
import org.ndnx.ndn.io.NDNWriter;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.Interest;


/**
 * Low-level writing of packets to file.  This program is designed to 
 * generate signed and encoded packets using only the base library facilities,
 * i.e. not even fragmentation and versioning but just basic interest and data
 */
public class puttap implements NDNContentHandler {

	public static final int CHUNK_SIZE = 432;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args.length < 4) || (args.length > 5)) {
			usage();
		}
		
		boolean result = (new puttap().go(args[0], args[1], args[2], args[3]));
		if (result) {
			System.exit(0);
		} else {
			System.exit(1);
		}
	}
	
	public boolean go(String encFlag, String ndnName, String tapName, String readName) {
		NDNNetworkManager manager = null;
		try {
			if (encFlag.equals("0")) {
				SystemConfiguration.setDefaultEncoding(TextXMLCodec.codecName());
			} else {
				SystemConfiguration.setDefaultEncoding(BinaryXMLCodec.codecName());
			}
			File theFile = new File(readName);
			if (!theFile.exists()) {
				System.out.println("No such file: " + readName);
				usage();
				return false;
			}
			
			// Get writing handle 
			NDNHandle handle = NDNHandle.open();
			manager = handle.getNetworkManager();
			// Set up tap so packets get written to file
			manager.setTap(tapName);
			
			ContentName name = ContentName.fromURI(ndnName);
			
			// Register standing interest so our put's will flow
			// This must be through separate handle instance so it 
			// appears that there is an interest from a separate app
			// because interest from the same app as the writer will 
			// not consume the data and therefore will block
			NDNHandle reader = NDNHandle.open();
			reader.expressInterest(new Interest(ndnName), this);
			
			// Remove automatic verification at this level. Can put it back in at
			// the tap level. NDNWriter is a segmenting writer, it makes no real
			// sense for it to return "a" ContentObject to verify.
			
			// Dump the file in small packets
	        InputStream is = new FileInputStream(theFile);
	        byte[] bytes = new byte[CHUNK_SIZE];
	        int i = 0;
	        NDNWriter writer = new NDNWriter(name, handle);
	        writer.disableFlowControl();
	        while (is.read(bytes) >= 0) {
	        	writer.put(new ContentName(name, new Integer(i++).toString()), bytes);
	        }
	        
	        return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (null != manager) {
		        // Need to call shutdown directly on manager at this point
				manager.shutdown();
			}
		}

	}

	public static void usage() {
		System.out.println("usage: puttap 0|1 <ndnname> <tapname> <filename>");
		System.exit(1);
	}

	public Interest handleContent(ContentObject data, Interest interest) {
		// Intentional no-op
		return null;
	}
}
