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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNFileInputStream;
import org.ndnx.ndn.io.NDNInputStream;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.profiles.metadata.MetadataProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;

/**
 * A command-line utility for pulling meta files associated with a file
 * out of a repository. The "metaname" should be the relative path (including filename) for
 * the desired metadata only.
 * Note class name needs to match command name to work with ndn_run
 */
public class ndngetmeta implements Usage {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Log.setDefaultLevel(Level.WARNING);
		Usage u = new ndngetmeta();

		for (int i = 0; i < args.length; i++) {
			if (!CommonArguments.parseArguments(args, i, u)) {
				if (i >= args.length - 3) {
					CommonParameters.startArg = i;
					break;
				}
				u.usage(CommonArguments.getExtraUsage());
			}
			i = CommonParameters.startArg;
		}

		if (args.length != CommonParameters.startArg + 3) {
			u.usage(CommonArguments.getExtraUsage());
		}

		try {
			int readsize = 1024; // make an argument for testing...

			NDNHandle handle = NDNHandle.open();

			String metaArg = args[CommonParameters.startArg + 1];
			if (!metaArg.startsWith("/"))
				metaArg = "/" + metaArg;
			ContentName fileName = MetadataProfile.getLatestVersion(ContentName.fromURI(args[CommonParameters.startArg]),
					ContentName.fromNative(metaArg), CommonParameters.timeout, handle);

			if (fileName == null) {
				//This base content does not exist...  cannot get metadata associated with the base name.
				System.out.println("File " + args[CommonParameters.startArg] + " does not exist");
				System.exit(1);
			}

			if (VersioningProfile.hasTerminalVersion(fileName)) {
				//MetadataProfile has found a terminal version...  we have something to get!
			} else {
				//MetadataProfile could not find a terminal version...  nothing to get
				System.out.println("File " + fileName + " does not exist...  exiting");
				System.exit(1);
			}

			File theFile = new File(args[CommonParameters.startArg + 2]);
			if (theFile.exists()) {
				System.out.println("Overwriting file: " + args[CommonParameters.startArg + 1]);
			}
			FileOutputStream output = new FileOutputStream(theFile);

			long starttime = System.currentTimeMillis();
			NDNInputStream input;
			if (CommonParameters.unversioned)
				input = new NDNInputStream(fileName, handle);
			else
				input = new NDNFileInputStream(fileName, handle);
			if (CommonParameters.timeout != null) {
				input.setTimeout(CommonParameters.timeout);
			}
			byte [] buffer = new byte[readsize];

			int readcount = 0;
			long readtotal = 0;
			//while (!input.eof()) {
			while ((readcount = input.read(buffer)) != -1){
				//readcount = input.read(buffer);
				readtotal += readcount;
				output.write(buffer, 0, readcount);
				output.flush();
			}
			if (CommonParameters.verbose)
				System.out.println("ndngetfile took: "+(System.currentTimeMillis() - starttime)+"ms");
			System.out.println("Retrieved content " + args[CommonParameters.startArg + 1] + " got " + readtotal + " bytes.");
			System.exit(0);

		} catch (ConfigurationException e) {
			System.out.println("Configuration exception in ndngetfile: " + e.getMessage());
			e.printStackTrace();
		} catch (MalformedContentNameStringException e) {
			System.out.println("Malformed name: " + args[CommonParameters.startArg] + " " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Cannot write file or read content. " + e.getMessage());
			e.printStackTrace();
		}
		System.exit(1);
	}

	public void usage(String extraArgs) {
		System.out.println("usage: ndngetmeta " + extraArgs + "[-v (verbose)] [-unversioned] [-timeout millis] [-as pathToKeystore] [-ac (access control)] <ndnname> <metaname> <filename>");
		System.exit(1);
	}

}
