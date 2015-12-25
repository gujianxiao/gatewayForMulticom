/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011, 2013 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.repo;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowServer;
import org.ndnx.ndn.impl.repo.RepositoryInfo;
import org.ndnx.ndn.impl.support.Log;

import static org.ndnx.ndn.impl.repo.RepositoryInfo.RepoInfoType.DATA;
import org.ndnx.ndn.io.NDNOutputStream;
import static org.ndnx.ndn.profiles.repo.RepositoryControl.doLocalCheckedWrite;
import org.ndnx.ndn.protocol.ContentName;
import org.junit.Assert;
import org.junit.Test;

public class CheckedWriteTest {

	@Test
	public void testCheckedWrite() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testCheckedWrite");

		NDNHandle handle = NDNHandle.getHandle();
		ContentName baseName = ContentName.fromNative("/testChecked");
		NDNFlowServer server = new NDNFlowServer(baseName, 1, true, handle);
		NDNOutputStream os = new NDNOutputStream(baseName, null, null, null, null, server);
		os.close();

		Long startingSegmentNumber = 0L;

		RepositoryInfo ri = doLocalCheckedWrite(baseName, startingSegmentNumber, os.getFirstDigest(), handle);
		Assert.assertFalse(ri.getType() == DATA);

		Thread.sleep(2000);

		ri = doLocalCheckedWrite(baseName, startingSegmentNumber, os.getFirstDigest(), handle);
		Assert.assertTrue(ri.getType() == DATA);
		
		Log.info(Log.FAC_TEST, "Completed testCheckedWrite");
	}
}
