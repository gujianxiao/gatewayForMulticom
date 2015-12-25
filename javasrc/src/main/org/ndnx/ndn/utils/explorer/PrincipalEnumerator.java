/*
 * A NDNx command line utility.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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
 
package org.ndnx.ndn.utils.explorer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.SortedSet;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.config.UserConfiguration;
import org.ndnx.ndn.io.content.Link;
import org.ndnx.ndn.profiles.nameenum.EnumeratedNameList;
import org.ndnx.ndn.profiles.security.access.group.Group;
import org.ndnx.ndn.profiles.security.access.group.GroupManager;
import org.ndnx.ndn.profiles.security.access.group.MembershipListObject;
import org.ndnx.ndn.protocol.ContentName;

public class PrincipalEnumerator {

	private static long TIMEOUT = 1000;
	
	GroupManager gm;
	ContentName userStorage = new ContentName(UserConfiguration.defaultNamespace(), "Users");
	ContentName groupStorage = new ContentName(UserConfiguration.defaultNamespace(), "Groups");
	
	public PrincipalEnumerator(GroupManager gm) {
		this.gm = gm;
	}
	
	public ArrayList<ContentName> enumerateUsers() {
		return listPrincipals(userStorage);
	}
	
	public ArrayList<ContentName> enumerateGroups() {
		return listPrincipals(groupStorage);
	}
	
	public ArrayList<ContentName> enumerateGroupMembers(String groupFriendlyName) {
		ArrayList<ContentName> members = new ArrayList<ContentName>();
		if (groupFriendlyName != null) {
			try{
				Group g = gm.getGroup(groupFriendlyName, SystemConfiguration.getDefaultTimeout());
				MembershipListObject ml = g.membershipList();
				LinkedList<Link> lll = ml.contents();
				for (Link l: lll) {
					members.add(l.targetName());
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return members;
	}
	
	private ArrayList<ContentName> listPrincipals(ContentName path) {
		ArrayList<ContentName> principalList = new ArrayList<ContentName>();
		
		try {
			EnumeratedNameList userDirectory = new EnumeratedNameList(path, NDNHandle.open());
			userDirectory.waitForChildren(TIMEOUT);
			Thread.sleep(TIMEOUT);
			
			SortedSet<ContentName> availableChildren = userDirectory.getChildren();
			if ((null == availableChildren) || (availableChildren.size() == 0)) {
				System.out.println("No available keystore data in directory " + path + ", giving up.");
			}
			else {
				for (ContentName child : availableChildren) {
					ContentName fullName = path.append(child);
					principalList.add(fullName);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return principalList;
	}
	
}
