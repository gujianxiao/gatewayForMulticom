/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2013 Palo Alto Research Center, Inc.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.repo.BasicPolicy;
import org.ndnx.ndn.impl.repo.PolicyXML;
import org.ndnx.ndn.impl.repo.PolicyXML.PolicyObject;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNInputStream;
import org.ndnx.ndn.io.NDNOutputStream;
import org.ndnx.ndn.io.NDNVersionedInputStream;
import org.ndnx.ndn.io.NDNVersionedOutputStream;
import org.ndnx.ndn.io.RepositoryOutputStream;
import org.ndnx.ndn.io.RepositoryVersionedOutputStream;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.io.content.Link;
import org.ndnx.ndn.io.content.Link.LinkObject;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.profiles.ndnd.NDNDCacheManager;
import org.ndnx.ndn.profiles.repo.RepositoryControl;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.ndnx.ndn.NDNTestHelper;
import org.ndnx.ndn.utils.Flosser;
import org.ndnx.ndn.utils.CreateUserData;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



/**
 * Part of repository test infrastructure. Requires at least repository to be running,
 * and RFSTest to have been run.
 */
public class RepoIOTest extends RepoTestBase {
	protected static final int CACHE_CLEAR_TIMEOUT = 10000;
	
	protected static NDNTestHelper testHelper = new NDNTestHelper(RepoIOTest.class);
	
	protected static String _repoTestDir = "repotest";
	protected static byte [] data = new byte[4000];
	// Test stream and net object names for content written into repo before all test cases
	// Note these have random numbers added in initialization
	protected static ContentName _testPrefix;
	protected static String _testStream = "stream";
	protected static String _testObj = "obj";
	// Test stream and net object names for content not in repo before test cases
	protected static String _testNonRepo = "stream-nr";
	protected static String _testNonRepoObj = "obj-nr";
	protected static String _testLink = "link";
	
	protected static NDNDCacheManager _cacheManager = new NDNDCacheManager();
	protected static ContentName _keyNameForStream;
	protected static ContentName _keyNameForObj;
	
	static String USER_NAMESPACE = "TestRepoUser";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		RepoTestBase.setUpBeforeClass();

		_testPrefix = testHelper.getTestNamespace("testRepoIO");

		_testStream += "-" + rand.nextInt(10000);
		_testObj += "-" + rand.nextInt(10000);
		
		
		byte value = 1;
		for (int i = 0; i < data.length; i++)
			data[i] = value++;

		RepositoryOutputStream ros = new RepositoryOutputStream(new ContentName(_testPrefix, _testStream), putHandle); 
		ros.setBlockSize(100);
		ros.setTimeout(4000);
		ros.write(data, 0, data.length);
		ros.close();
		NDNStringObject so = new NDNStringObject(new ContentName(_testPrefix, _testObj), "Initial string value", SaveType.REPOSITORY, putHandle);
		so.save();
		
		// Floss content into ndnd for tests involving content not already in repo when we start
		Flosser floss = new Flosser();
		
		// Floss user based keys from CreateUserData
		floss.handleNamespace(testHelper.getClassChildName(USER_NAMESPACE) + "/" + CreateUserData.USER_NAMES[0]);
		floss.handleNamespace(testHelper.getClassChildName(USER_NAMESPACE) + "/" + CreateUserData.USER_NAMES[1]);
		
		// So we can test saving keys in the sync tests we build our first sync object (a stream) with
		// an alternate key and the second one (a NDNNetworkObject) with an alternate key locater that is
		// accessed through a link.
		CreateUserData testUsers = new CreateUserData(testHelper.getClassChildName(USER_NAMESPACE), 2, false, null);
		String [] userNames = testUsers.friendlyNames().toArray(new String[2]);
		NDNHandle userHandle = testUsers.getHandleForUser(userNames[0]);
				
		// Build the link and the key it links to. Floss these into ndnd.
		_testNonRepo += "-" + rand.nextInt(10000);
		_testNonRepoObj += "-" + rand.nextInt(10000);
		_testLink += "-" + rand.nextInt(10000);
		ContentName name = new ContentName(_testPrefix, _testNonRepo);
		floss.handleNamespace(name);
		NDNOutputStream cos = new NDNOutputStream(name, userHandle);
		cos.setBlockSize(100);
		cos.setTimeout(4000);
		cos.write(data, 0, data.length);
		_keyNameForStream = cos.getFirstSegment().signedInfo().getKeyLocator().name().name();
		cos.close();
		
		NDNHandle userHandle2 = testUsers.getHandleForUser(userNames[1]);
		KeyLocator userLocator = 
			userHandle2.keyManager().getKeyLocator(userHandle2.keyManager().getDefaultKeyID());
		Link link = new Link(userLocator.name().name());
		ContentName linkName = new ContentName(_testPrefix, _testLink);
		LinkObject lo = new LinkObject(linkName, link, SaveType.RAW, putHandle);
		floss.handleNamespace(lo.getBaseName());
		lo.save();
		
		KeyLocator linkLocator = new KeyLocator(linkName);
		userHandle2.keyManager().setKeyLocator(null, linkLocator);
		name = new ContentName(_testPrefix, _testNonRepoObj);
		floss.handleNamespace(name);
		so = new NDNStringObject(name, "String value for non-repo obj", SaveType.RAW, userHandle2);
		so.save();
		_keyNameForObj = so.getFirstSegment().signedInfo().getKeyLocator().name().name();
		lo.close();
		so.close();
		floss.stop();
	}
	
	@AfterClass
	public static void cleanup() throws Exception {
	}
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testPolicyViaNDN() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testPolicyViaNDN");

		checkNameSpace("/repoTest/data2", true);
		changePolicy("/org/ndnx/ndn/repo/policyTest.xml");
		checkNameSpace("/repoTest/data3", false);
		checkNameSpace("/testNameSpace/data1", true);
		changePolicy("/org/ndnx/ndn/repo/origPolicy.xml");
		checkNameSpace("/repoTest/data4", true);
		
		Log.info(Log.FAC_TEST, "Completed testPolicyViaNDN");
	}
	
	@Test
	public void testReadFromRepo() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testReadFromRepo");

		ContentName name = new ContentName(_testPrefix, _testStream);
		_cacheManager.clearCache(name, getHandle, CACHE_CLEAR_TIMEOUT);
		Thread.sleep(5000);
		NDNInputStream input = new NDNInputStream(name, getHandle);
		byte[] testBytes = new byte[data.length];
		input.read(testBytes);
		Assert.assertArrayEquals(data, testBytes);
		
		Log.info(Log.FAC_TEST, "Completed testReadFromRepo");
	}
	
	@Test
	// The purpose of this test is to do versioned reads from repo
	// of data not already in the ndnd cache, thus testing 
	// what happens if we pull latest version and try to read
	// content in order
	public void testVersionedRead() throws InterruptedException, IOException, MalformedContentNameStringException {
		Log.info(Log.FAC_TEST, "Starting testVersionedRead");

		ContentName versionedNameNormal = new ContentName(_testPrefix, "testVersionNormal");
		NDNVersionedOutputStream ostream = new RepositoryVersionedOutputStream(versionedNameNormal, putHandle);
		ostream.setBlockSize("segment".length() + new Long(5).toString().length());
		for (long i=SegmentationProfile.baseSegment(); i<5; i++) {
			String segmentContent = "segment"+ new Long(i).toString();
			ostream.write(segmentContent.getBytes(), 0, 8);
		}
		ostream.close();
		_cacheManager.clearCache(versionedNameNormal, getHandle, CACHE_CLEAR_TIMEOUT);
		NDNVersionedInputStream vstream = new NDNVersionedInputStream(versionedNameNormal);
		InputStreamReader reader = new InputStreamReader(vstream);
		for (long i=SegmentationProfile.baseSegment(); i<5; i++) {
			String segmentContent = "segment"+ new Long(i).toString();
			char[] cbuf = new char[8];
			int count = reader.read(cbuf, 0, 8);
			System.out.println("for " + i + " got " + count + " (eof " + vstream.eof() + "): " + new String(cbuf));
			Assert.assertEquals(segmentContent, new String(cbuf));
		}
		Assert.assertEquals(-1, reader.read());
		vstream.close();
		
		Log.info(Log.FAC_TEST, "Completed testVersionedRead");
	}
	
	@Test
	public void testLocalSyncInputStream() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalSyncInputStream");

		// This test should run all on single handle, just as client would do
		NDNInputStream input = new NDNInputStream(new ContentName(_testPrefix, _testStream), getHandle);
		// Ignore data in this case, just trigger repo confirmation
		// Setup of this test writes the stream into repo, so we know it is already there --
		// should get immediate confirmation from repo, which means no new repo read starts
		boolean confirm = RepositoryControl.localRepoSync(getHandle, input);
		Assert.assertTrue(confirm);
		input.close();
		
		// Test case of content not already in repo
		Log.info("About to do first sync for stream");
		ContentName name = new ContentName(_testPrefix, _testNonRepo);
		input = new NDNInputStream(name, getHandle);
		Assert.assertFalse(RepositoryControl.localRepoSync(getHandle, input));
		
		Thread.sleep(2000);  // Give repo time to fetch TODO: replace with confirmation protocol
		Log.info("About to do second sync for stream");
		Assert.assertTrue(RepositoryControl.localRepoSync(getHandle, input));	
		input.close();
		
		_cacheManager.clearCache(name, getHandle, CACHE_CLEAR_TIMEOUT);
		_cacheManager.clearCache(_keyNameForStream, getHandle, CACHE_CLEAR_TIMEOUT);
		byte[] testBytes = new byte[data.length];
		input = new NDNInputStream(name, getHandle);
		input.read(testBytes);
		Assert.assertArrayEquals(data, testBytes);
		input.close();
		
		Log.info(Log.FAC_TEST, "Completed testLocalSyncInputStream");
	}
	
	@Test
	public void testLocalSyncNetObj() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalSyncNetObj");

		// This test should run all on single handle, just as client would do
		System.out.println("Testing local repo sync request for network object");	
		NDNStringObject so = new NDNStringObject(new ContentName(_testPrefix, _testObj), getHandle);
		// Ignore data in this case, just trigger repo confirmation
		// Setup of this test writes the object into repo, so we know it is already there --
		// should get immediate confirmation from repo, which means no new repo read starts
		boolean confirm = RepositoryControl.localRepoSync(getHandle, so);
		Assert.assertTrue(confirm);
		so.close();
		
		// Test case of content not already in repo
		ContentName name = new ContentName(_testPrefix, _testNonRepoObj);
		so = new NDNStringObject(name, getHandle);
		Log.info("About to do first sync for object {0}", so.getBaseName());
		Assert.assertFalse(RepositoryControl.localRepoSync(getHandle, so));

		Thread.sleep(2000);  // Give repo time to fetch TODO: replace with confirmation protocol
		Log.info("About to do second sync for object {0}", so.getBaseName());
		Assert.assertTrue(RepositoryControl.localRepoSync(getHandle, so));
		so.close();
		
		_cacheManager.clearCache(name, getHandle, CACHE_CLEAR_TIMEOUT);
		_cacheManager.clearCache(_keyNameForStream, getHandle, CACHE_CLEAR_TIMEOUT);
		_cacheManager.clearCache(new ContentName(_testPrefix, _testLink), getHandle, CACHE_CLEAR_TIMEOUT);
		so = new NDNStringObject(name, getHandle);
		assert(so.string().equals("String value for non-repo obj"));
		
		Log.info(Log.FAC_TEST, "Completed testLocalSyncNetObj");
	}

	private void changePolicy(String policyFile) throws Exception {
		FileInputStream fis = new FileInputStream(_topdir + policyFile);
		PolicyXML pxml = BasicPolicy.createPolicyXML(fis);
		fis.close();
		ContentName policyName = BasicPolicy.getPolicyName(ContentName.fromNative(_globalPrefix));
		PolicyObject po = new PolicyObject(policyName, pxml, SaveType.REPOSITORY, putHandle);
		po.save();
		Thread.sleep(4000);
	}
}
