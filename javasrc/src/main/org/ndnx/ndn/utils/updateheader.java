/*
 * A NDNx command line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
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

import java.io.IOException;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.Header.HeaderObject;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.profiles.metadata.MetadataProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;

/*
 * Deprecated. We don't believe there are many (if any) old repositories with old style headers at this point.
 */
@Deprecated
public class updateheader {

	public static void usage() {
		System.out.println("usage: updateheader [-log level] <ndnname> [<ndnname>*>]\n Assumes content is in a repository.");
	}

	public static void moveHeader(String ndnxName, NDNHandle handle) throws MalformedContentNameStringException, IOException {
		
		ContentName fileName = ContentName.parse(ndnxName);
		
		// Want a versioned name, either this version or latest version
		
		ContentName fileVersionedName = null;
		if (VersioningProfile.hasTerminalVersion(fileName)) {
			fileVersionedName = fileName;
		} else {
			ContentObject fileObject = VersioningProfile.getLatestVersion(fileName, null, SystemConfiguration.getDefaultTimeout(), null, handle);
			if (null == fileObject) {
				System.out.println("Cannot find file " + fileName + " to update. Skipping.");
				return;
			}
			fileVersionedName = fileObject.name().subname(0, fileName.count() + 1);
		}
		
		// Should only update content in repositories -- makes no sense to bother with other
		// content, really. 
		
		HeaderObject newHeader = new HeaderObject(MetadataProfile.headerName(fileVersionedName), null, handle);
		newHeader.updateInBackground();
		
		HeaderObject oldHeader = new HeaderObject(MetadataProfile.oldHeaderName(fileVersionedName), null, handle);
		oldHeader.updateInBackground();
		
		oldHeader.waitForData(SystemConfiguration.getDefaultTimeout());
		
		if (!oldHeader.available()) {
			System.out.println("No old-style header found. Skipping " + ndnxName);
			oldHeader.cancelInterest();
			newHeader.cancelInterest();
			return;
		}
		// if we get here, the initial background update should have completed for oldHeader
		
		if (newHeader.available()) {
			System.out.println("Already have a new header: " + newHeader.getVersionedName() + ", skipping file " + ndnxName);
		} else {
			newHeader.cancelInterest();
			newHeader.setupSave(SaveType.REPOSITORY);
			newHeader.save(oldHeader.header());
			newHeader.close();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int arg = 0;
		
		if ((args.length == 0) || ((args.length >= 1) && ((args[0].equals("--help")) || (args[0].equals("-h"))))) {
			usage();
			System.exit(1);
		}
		
		if ((args.length > 2) && (args[0].equals("-log"))) {
			Log.setDefaultLevel(Level.parse(args[1]));
			arg += 2;
		}
		
		NDNHandle handle = NDNHandle.getHandle();
		
		for (int i=arg; i < args.length; ++i) {
			try {
				moveHeader(args[i], handle);
			} catch (Exception e) {
				System.out.println("Exception processing file " + args[i] + ": " + e);
				e.printStackTrace();
			}
		}
		handle.close();
		KeyManager.closeDefaultKeyManager();
	}
}
