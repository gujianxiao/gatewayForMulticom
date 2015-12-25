/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011, 2013 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.security.access;


import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNWriter;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.utils.Flosser;
import org.junit.BeforeClass;
import org.junit.Test;



public class AccessControlManagerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void testPartialComponentMatch() {
		Log.info(Log.FAC_TEST, "Starting testPartialComponentMatch");

		NDNHandle handle = null;
		try {
			ContentName testPrefix = ContentName.fromNative("/ndn/test/content/");
			Flosser flosser = new Flosser(testPrefix);
			
			ContentName versionPrefix = VersioningProfile.addVersion(testPrefix);
			ContentName aname = new ContentName(versionPrefix, "aaaaa");
			ContentName bname = new ContentName(versionPrefix, "bbbbb");
			ContentName abname = new ContentName(versionPrefix, "aaaaa:bbbbb");
			
			NDNWriter writer = new NDNWriter(versionPrefix, NDNHandle.open());
			writer.put(bname, "Some b's.".getBytes());
			writer.put(abname, "Some a's and b's.".getBytes());
			
			handle = NDNHandle.open();
			ContentObject bobject = handle.get(bname, 1000);
			if (bobject != null) {
				System.out.println("Queried for bname, got back: " + bobject.name());
			}
			ContentObject aobject = handle.get(aname, 1000);
			if (aobject != null) {
				System.out.println("Queried for aname, got back: " + aobject.name());
			} else {
				System.out.println("Queried for aname, got back nothing.");
			}
			writer.put(aname, "Some a's.".getBytes());
			ContentObject aobject2 = handle.get(versionPrefix, 1000);
			if (aobject2 != null) {
				System.out.println("Queried for aname, again got back: " + aobject2.name());
			}
			flosser.stop();
		} catch (Exception e) {
			Log.warning(Log.FAC_TEST, "Exception : " + e.getClass().getName() + ": " + e.getMessage());
			Log.warningStackTrace(Log.FAC_TEST, e);
		} finally {
			if (null != handle)
				handle.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testPartialComponentMatch");
	}
}
