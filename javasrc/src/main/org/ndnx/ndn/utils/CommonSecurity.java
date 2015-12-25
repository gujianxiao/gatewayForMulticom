/*
 * Part of the NDNx command line utilities
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

import java.io.File;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.UserConfiguration;
import org.ndnx.ndn.profiles.security.access.group.GroupAccessControlManager;
import org.ndnx.ndn.protocol.ContentName;

public class CommonSecurity {
	
	public static void setUser(String pathToKeystore) {
		File userDirectory = new File(pathToKeystore);
		String userConfigDir = userDirectory.getAbsolutePath();
		System.out.println("Loading keystore from: " + userConfigDir);
		UserConfiguration.setUserConfigurationDirectory(userConfigDir);
		// Assume here that the name of the file is the userName
		String userName = userDirectory.getName();
		if (userName != null) {
			System.out.println("User: " + userName);
			UserConfiguration.setUserName(userName);
		}
	}
	
	public static void setAccessControl() {
		// register a group access control manager with the namespace manager
		try {
			// TODO -- broken  -- turns on access control probably before we've done -as for user.
			NDNHandle ourHandle = NDNHandle.open();
			GroupAccessControlManager gacm = new GroupAccessControlManager(ContentName.fromNative("/"), 
					CommonParameters.groupStorage, CommonParameters.userStorage, NDNHandle.open());
			ourHandle.keyManager().rememberAccessControlManager(gacm);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
