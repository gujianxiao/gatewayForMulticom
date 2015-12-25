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

package org.ndnx.ndn;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;


/**
 * Part of older test infrastructure. Tests signature generation and verification
 * as part of simple object/based read/write test.
 */
public class BaseSecurityTest extends BasePutGetTest {
	
	protected static final String testName = "/test/smetters/signTestContent.txt";
	protected static final String testContent = "Mary had a little lamb. Its fleece was reasonably white for a sheep who'd been through that sort of thing.";
	
	
	public void checkGetResults(ArrayList<ContentObject> getResults) {
		boolean verifySig = false;
		for (int i=0; i < getResults.size(); ++i) {
			try {
				verifySig = getResults.get(i).verify(getHandle.keyManager());
				if (!verifySig) {
					SystemConfiguration.logObject("checkGetResults: verification failed", getResults.get(i));
				} else {
					SystemConfiguration.logObject("checkGetResults: verification succeeded", getResults.get(i));
				}
				assertTrue(verifySig);
			} catch (Exception e) {
				Log.warning(Log.FAC_TEST, "Exception in checkGetResults for name: " + getResults.get(i).name() +": " + e.getClass().getName() + " " + e.getMessage());
				Log.warningStackTrace(Log.FAC_TEST, e);
				fail();			
			}
		} 
	}
	
	public void checkPutResults(ContentObject putResult) {
		try {
			// Check content and verify signature.
			// If we have a static fragmentation marker, remove it.
			ContentName baseName = SegmentationProfile.segmentRoot(putResult.name());
			int val = Integer.parseInt(new String(baseName.component(baseName.count()-1)));
			ContentObject co = new ContentObject(putResult.name(), putResult.signedInfo(), Integer.toString(val).getBytes(), putResult.signature());
			boolean b = co.verify(putHandle.keyManager());
			if (!b) {
				SystemConfiguration.logObject("checkPutResults: verification failed", co);
			} else {
				SystemConfiguration.logObject("checkPutResults: verification succeeded", co);
			}
		} catch (Exception e) {
			Log.warning(Log.FAC_TEST, "Exception in checkPutResults for name: " + putResult.name() +": " + e.getClass().getName() + " " + e.getMessage());
			Log.warningStackTrace(Log.FAC_TEST, e);
			fail();			
		}
	}

}
