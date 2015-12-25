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

package org.ndnx.ndn.io.content;


import java.io.IOException;
import java.util.Random;

import junit.framework.Assert;

import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.Link;
import org.ndnx.ndn.io.content.Link.LinkObject;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;
import org.ndnx.ndn.NDNTestBase;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.TestUtils;
import org.junit.Test;

/**
 * Test versioned Link objects, written to a repository.
 */
public class LinkObjectTestRepo extends NDNTestBase {


	/**
	 * Handle naming for the test
	 */
	static NDNTestHelper testHelper = new NDNTestHelper(LinkObjectTestRepo.class);

	@Test
	public void testLinks() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLinks");

		ContentName nonLinkName = new ContentName(testHelper.getTestNamespace("testLinks"), "myNonLink");
		ContentName linkName = new ContentName(testHelper.getTestNamespace("testLinks"), "myLink");

		// Write something that isn't a collection
		NDNSerializableStringObject so =
			new NDNSerializableStringObject(nonLinkName, "This is not a link, number " + new Random().nextInt(10000), SaveType.REPOSITORY, putHandle);
		so.save();
		TestUtils.checkObject(putHandle, so);

		try {
			LinkObject notAnObject = new LinkObject(nonLinkName, getHandle);
			notAnObject.waitForData();
			Assert.fail("Reading link from non-link succeeded.");
		} catch (ContentDecodingException ex) {
			// this is what we actually expect
			Log.info(Log.FAC_TEST, "Got expected exception reading link from non-link.");
		} catch (IOException ioe) {
			Log.info(Log.FAC_TEST, "Unexpected: got IOException that wasn't a ContentDecodingException reading link from non-link: {0}", ioe);
			throw ioe;
		} catch (Exception e) {
			Log.info(Log.FAC_TEST, "Got unexpected exception type reading link from non-link: " + e);
			throw e;
		}

		Link lr = new Link(so.getVersionedName());
		LinkObject aLink = new LinkObject(linkName, lr, SaveType.REPOSITORY, putHandle);
		aLink.save();

		ContentObject linkData = getHandle.get(SegmentationProfile.firstSegmentInterest(aLink.getVersionedName(), null), 5000);
		if (null == linkData) {
			Assert.fail("Cannot retrieve first block of saved link: " + aLink.getVersionedName());
		}
		// Make sure we're writing type LINK.
		Assert.assertEquals(linkData.signedInfo().getType(), ContentType.LINK);

		LinkObject readLink = new LinkObject(linkData, getHandle);
		readLink.waitForData();

		Assert.assertEquals(readLink.link(), lr);

		ContentObject firstBlock = aLink.dereference(5000);
		if (null == firstBlock) {
			Assert.fail("Cannot read first block of link target: " + readLink.getTargetName());
		}
		Log.info(Log.FAC_TEST, "Got block of target: " + firstBlock.name());

		// TODO -- not a good test; does dereference get us back the first block? What about the
		// first block of the latest version? What if thing isn't versioned? (e.g. intermediate node)
		NDNSerializableStringObject readString = new NDNSerializableStringObject(firstBlock, getHandle);
		readString.waitForData();

		Assert.assertEquals(readString.string(), so.string());

		Log.info(Log.FAC_TEST, "Completed testLinks");
	}
}
