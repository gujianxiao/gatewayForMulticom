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
package org.ndnx.ndn.io.content;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.ndnx.ndn.NDNInterestHandler;
import org.ndnx.ndn.KeyManager;
import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.security.keys.BasicKeyManager;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.NDNStringObject;
import org.ndnx.ndn.io.content.LocalCopyListener;
import org.ndnx.ndn.io.content.LocalCopyWrapper;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.profiles.ndnd.NDNDaemonException;
import org.ndnx.ndn.profiles.ndnd.PrefixRegistrationManager;
import org.ndnx.ndn.profiles.ndnd.PrefixRegistrationManager.ForwardingEntry;
import org.ndnx.ndn.profiles.repo.RepositoryControl;
import org.ndnx.ndn.profiles.repo.RepositoryOperations;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.Interest;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.ndnx.ndn.AssertionNDNHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Test RepositoryControl, LocalCopyListener, and LocalCopyWrapper to make sure
 * there are no dangling faces in ndnd when they stop.
 * 
 * In general, the tests go like this:
 * - Create an Interest listener for our object
 * - Ask the repo to sync it
 * - Create the object and save it in the Interest handler.
 * - At each step, use the ndnd xml export to verify that the proper number of
 *   prefixes are registered.
 */
public class LocalCopyTestRepo {
	
	AssertionNDNHandle readhandle;
	AssertionNDNHandle listenerhandle;
	int readFaceId;
	int listenerFaceId;
	BasicKeyManager km;
	final static Random _rnd = new Random();
	final static String _prefix = String.format("/test_%016X", _rnd.nextLong());
	
	final static int LONG_TIMEOUT = 1000;
	final static int SHORT_TIMEOUT = 500;
	final static int CHECK_TIMEOUT = 200;
	
	static int _port = NDNNetworkManager.DEFAULT_AGENT_PORT;
	static String _host = NDNNetworkManager.DEFAULT_AGENT_HOST;
	static String _ndndurl;
	
	// by faceid
    final HashMap<Integer,TreeSet<ContentName>> fentries = new HashMap<Integer, TreeSet<ContentName>>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
		// Determine port at which to contact agent
		String portval = System.getProperty(NDNNetworkManager.PROP_AGENT_PORT);
		if (null != portval) {
			try {
				_port = new Integer(portval);
			} catch (Exception ex) {
				throw new IOException("Invalid port '" + portval + "' specified in " + NDNNetworkManager.PROP_AGENT_PORT);
			}
			Log.warning(Log.FAC_NETMANAGER, "Non-standard NDN agent port " + _port + " per property " + NDNNetworkManager.PROP_AGENT_PORT);
		}
		
		String hostval = System.getProperty(NDNNetworkManager.PROP_AGENT_HOST);
		if (null != hostval && hostval.length() > 0) {
			_host = hostval;
			Log.warning(Log.FAC_NETMANAGER, "Non-standard NDN agent host " + _host + " per property " + NDNNetworkManager.PROP_AGENT_HOST);
		}
		
		_ndndurl = String.format("http://%s:%d/?f=xml", _host, _port);
		Log.info("Using ndnd url: " + _ndndurl);
    }
    
 	@Before
	public void setUp() throws Exception {
		Log.info("setUp");
		
		km = new BasicKeyManager();
		km.initialize();
		KeyManager.setDefaultKeyManager(km);
		readhandle = AssertionNDNHandle.open(km);
		listenerhandle = AssertionNDNHandle.open(km);
		
		// Setup a prefix to get my face
		readFaceId = getFaceId(readhandle, "read");
		listenerFaceId = getFaceId(listenerhandle, "listener");
		
		Log.info(String.format("Face IDs: read %d, listen %d", readFaceId, listenerFaceId));
		
	}
	
	@After
	public void tearDown() throws Exception {
		listenerhandle.close();
		readhandle.close();
		KeyManager.closeDefaultKeyManager();
	}
	
//	@Test
//	public void testDumpFaces() throws Exception {
//		getfaces();
//	}
	
	@Test
	public void testRepositoryControlObject() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testRepositoryControlObject");

		MyHandler listener = new MyHandler();
		
		try {
			listener.open();
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));		
			
			RepositoryControl.localRepoSync(readhandle, so_in);
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After localRepoSync on string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			so_in.close();		
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testRepositoryControlObject");
	}

	@Test
	public void testLocalCopyWrapper() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalCopyWrapper");

		MyHandler listener = new MyHandler();
		
		try {
			listener.open();
			
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			
			Thread.sleep(LONG_TIMEOUT);
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			LocalCopyWrapper lcw = new LocalCopyWrapper(so_in);
			Thread.sleep(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper on string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			lcw.close();
			Thread.sleep(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper close");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testLocalCopyWrapper");
	}
	
	@Test
	public void testLocalCopyListener() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalCopyListener");

		MyHandler listener = new MyHandler();

		try {
			listener.open();
			
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			readhandle.checkError(CHECK_TIMEOUT);
			
			Thread.sleep(LONG_TIMEOUT);
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			LocalCopyListener.startBackup(so_in);
			readhandle.checkError(CHECK_TIMEOUT);
			Thread.sleep(LONG_TIMEOUT);
			Log.info("======= After LocalCopyListener.startBackup");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
						
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testLocalCopyListener");
	}
	
	@Test
	public void testLocalCopyWrapperWithSaveAndLcwClose() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalCopyWrapperWithSaveAndLcwClose");

		MyHandler listener = new MyHandler();
		
		try {
			listener.open();
			
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			so_in.setupSave(SaveType.LOCALREPOSITORY);
			readhandle.checkError(LONG_TIMEOUT);
			
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			LocalCopyWrapper lcw = new LocalCopyWrapper(so_in);
			readhandle.checkError(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper on string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			// Now modify the string object and save again.
			so_in.setData(String.format("%016X", _rnd.nextLong()));
			lcw.save();
			so_in.update();
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After LocalCopyWrapper save");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			lcw.close();
			readhandle.checkError(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper close");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testLocalCopyWrapperWithSaveAndLcwClose");
	}
	
	@Test
	public void testLocalCopyWrapperWithSaveAndObjectClose() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalCopyWrapperWithSaveAndObjectClose");

		MyHandler listener = new MyHandler();

		try {
			listener.open();
			
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			so_in.setupSave(SaveType.LOCALREPOSITORY);
			
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			LocalCopyWrapper lcw = new LocalCopyWrapper(so_in);
			readhandle.checkError(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper on string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			// Now modify the string object and save again.
			so_in.setData(String.format("%016X", _rnd.nextLong()));
			lcw.save();
			so_in.update();
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After LocalCopyWrapper save");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			// IMPORTANT: This in an incorrect usage, as we're closing the underlying
			// object, not the localcopywrapper.
			so_in.close();
			readhandle.checkError(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper close");
			getfaces();
			// IMPORTANT: Notice that the readFaceId still has 2 registrations.
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testLocalCopyWrapperWithSaveAndObjectClose");
	}
	
	@Test
	public void testLocalCopyListnerWithSaveAndObjectClose() throws Exception {
		Log.info(Log.FAC_TEST, "Starting testLocalCopyListnerWithSaveAndObjectClose");

		MyHandler listener = new MyHandler();
		
		try {
			listener.open();
			
			String namestring = String.format("%s/obj_%016X", _prefix, _rnd.nextLong());
			ContentName name = ContentName.fromNative(namestring);
			
			NDNStringObject so_in = new NDNStringObject(name, readhandle);
			so_in.setupSave(SaveType.LOCALREPOSITORY);
			
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After reading string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
			LocalCopyListener.startBackup(so_in);
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After LocalCopyWrapper on string object");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			// Now modify the string object and save again.
			so_in.setData(String.format("%016X", _rnd.nextLong()));
			so_in.save();
			so_in.update();
			readhandle.checkError(LONG_TIMEOUT);
			Log.info("======= After LocalCopyWrapper save");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));

			so_in.close();
			readhandle.checkError(SHORT_TIMEOUT);
			Log.info("======= After LocalCopyWrapper close");
			getfaces();
			Assert.assertEquals(1, dumpreg(readFaceId));
			Assert.assertEquals(2, dumpreg(listenerFaceId));
			
		} finally {
			listener.close();
		}
		
		Log.info(Log.FAC_TEST, "Completed testLocalCopyListnerWithSaveAndObjectClose");
	}
	
	// ==============================================================================================
	/**
	 * Interest listener to create random objects for the repo to sync
	 * @author mmosko
	 *
	 */
	private class MyHandler implements NDNInterestHandler {
		// These are the replies we have sent
//		public ConcurrentHashMap<ContentName, String> replies = new ConcurrentHashMap<ContentName, String>();
		public HashSet<ContentName> replies = new HashSet<ContentName>();
		boolean inListener = false;
		
		public void open() throws MalformedContentNameStringException, IOException, InterruptedException {
			listenerhandle.registerFilter(ContentName.fromNative(_prefix), this);
			listenerhandle.checkError(CHECK_TIMEOUT);
		}
		
		public void close() throws MalformedContentNameStringException, InterruptedException {
			listenerhandle.unregisterFilter(ContentName.fromNative(_prefix), this);
			synchronized (this) {
				while (inListener) {
					wait();
				}
			}
			listenerhandle.checkError(CHECK_TIMEOUT);
		}
		
		public boolean handleInterest(Interest interest) {
			boolean ret = false;
			// Ignore start write requests
			
			if( RepositoryOperations.isStartWriteOperation(interest) ) 
				return ret;
			
			if( RepositoryOperations.isCheckedWriteOperation(interest) ) 
				return ret;

			synchronized(replies) {
				if( replies.contains(interest.name()))
					return ret;
				
				Log.info("handleInterest: " + interest.toString());
				ContentName name = VersioningProfile.cutLastVersion(interest.name());
				
				synchronized (this) {
					inListener = true;
				}
				try {
					String s = interest.name().toString();
					NDNStringObject so = new NDNStringObject(name, s, SaveType.RAW, listenerhandle);
					so.saveLaterWithClose();
					replies.add(interest.name());
					ret = true;
				} catch (IOException e) {
					synchronized (this) {
						inListener = false;
						notifyAll();
					}
					Assert.fail(e.getMessage());					
				}
				synchronized (this) {
					inListener = false;
					notifyAll();
				}
				Log.info("handleInterest done: " + interest.toString());

				return ret;
			}
		}
	}
	
	/*
	 <ndnd>
	   <identity><ndndid>0E0BBF5633A562DDD2D354150FBB6D0055042A389F10700D5C010DA621B6586E</ndndid>
	   <apiversion>3002</apiversion>
	   <starttime>1303848970.212333</starttime>
	   <now>1303852704.538848</now>
	   </identity>
	   <cobs>
	      <accessioned>302</accessioned>
	      <stored>302</stored>
	      <stale>47</stale>
	      <sparse>0</sparse>
	      <duplicate>0</duplicate>
	      <sent>352</sent>
	   </cobs>
	   <interests>
	      <names>15</names>
	      <pending>0</pending>
	      <propagating>0</propagating>
	      <noted>0</noted>
	      <accepted>376</accepted>
	      <dropped>0</dropped>
	      <sent>600</sent>
	      <stuffed>0</stuffed>
	   </interests>
	   <faces>
	      <face>
	         <faceid>0</faceid>
	         <faceflags>000c</faceflags>
	         <pending>0</pending>
	         <recvcount>0</recvcount>
	         <meters>
	            <bytein><total>45140</total><persec>0</persec></bytein><byteout><total>26665</total><persec>0</persec></byteout><datain><total>46</total><persec>0</persec></datain><introut><total>51</total><persec>0</persec></introut><dataout><total>0</total><persec>0</persec></dataout><intrin><total>0</total><persec>0</persec></intrin>
	         </meters>
	      </face>
		<face><faceid>1</faceid><faceflags>400c</faceflags><pending>0</pending><recvcount>0</recvcount></face>
		<face><faceid>2</faceid><faceflags>5012</faceflags><pending>0</pending><recvcount>0</recvcount><ip>0.0.0.0:9695</ip></face>
		<face><faceid>3</faceid><faceflags>5010</faceflags><pending>0</pending><recvcount>0</recvcount><ip>0.0.0.0:9695</ip></face>
		<face><faceid>4</faceid><faceflags>4042</faceflags><pending>0</pending><recvcount>0</recvcount><ip>[::]:9695</ip></face>
		<face><faceid>5</faceid><faceflags>4040</faceflags><pending>0</pending><recvcount>0</recvcount><ip>[::]:9695</ip></face>
		<face>
		   <faceid>6</faceid>
		   <faceflags>1014</faceflags>
		   <pending>0</pending>
		   <recvcount>6</recvcount>
		   <ip>127.0.0.1:57037</ip>
		   <meters><bytein><total>1949</total><persec>0</persec></bytein><byteout><total>4006</total><persec>0</persec></byteout><datain><total>1</total><persec>0</persec></datain><introut><total>1</total><persec>0</persec></introut><dataout><total>5</total><persec>0</persec></dataout><intrin><total>5</total><persec>0</persec></intrin>
		   </meters>
		</face>
		<face><faceid>7</faceid><faceflags>1014</faceflags><pending>0</pending><recvcount>14</recvcount><ip>127.0.0.1:57040</ip><meters><bytein><total>4121</total><persec>0</persec></bytein><byteout><total>36582</total><persec>0</persec></byteout><datain><total>5</total><persec>0</persec></datain><introut><total>273</total><persec>0</persec></introut><dataout><total>7</total><persec>0</persec></dataout><intrin><total>9</total><persec>0</persec></intrin>
		</meters></face>
	</faces>
	<forwarding>
		<fentry>
			<prefix>ndn:/%C1.M.S.localhost/%C1.M.SRV/ndnd</prefix>
			<dest><faceid>0</faceid>
				<flags>3</flags>
				<expires>2147479917</expires>
			</dest>
		</fentry>
		<fentry><prefix>ndn:/ndnx/ping</prefix><dest><faceid>0</faceid><flags>3</flags><expires>2147479917</expires></dest></fentry><fentry><prefix>ndn:/ndnx/%0E%0B%BFV3%A5b%DD%D2%D3T%15%0F%BBm%00U%04%2A8%9F%10p%0D%5C%01%0D%A6%21%B6Xn</prefix><dest><faceid>0</faceid><flags>17</flags><expires>2147479917</expires></dest></fentry><fentry><prefix>ndn:/ndnx.org/Users/Repository/Keys/%C1.M.K%00%00%96%96c1%3D%A5%D5%E4%C8%29B%08%B1t%80D%1D%2A%BC3%BA%9A6%90N%09%8D%A2t%27%24</prefix><dest><faceid>6</faceid><flags>3</flags><expires>2147479922</expires></dest></fentry><fentry><prefix>ndn:/</prefix><dest><faceid>7</faceid><flags>43</flags><expires>2147479927</expires></dest></fentry><fentry><prefix>ndn:/%C1.M.S.neighborhood</prefix><dest><faceid>0</faceid><flags>3</flags><expires>2147479917</expires></dest></fentry><fentry><prefix>ndn:/%C1.M.S.localhost</prefix><dest><faceid>0</faceid><flags>23</flags><expires>2147479917</expires></dest></fentry><fentry><prefix>ndn:/%C1.M.S.localhost/%C1.M.SRV/repository/KEY</prefix><dest><faceid>6</faceid><flags>3</flags><expires>2147479922</expires></dest></fentry>
	</forwarding>
</ndnd>


	 */
	
	private void getfaces() throws ParserConfigurationException, SAXException, IOException, MalformedContentNameStringException, DOMException {
        javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        javax.xml.parsers.DocumentBuilder parser = dbf.newDocumentBuilder();
        org.w3c.dom.Document document;
        
        synchronized(fentries) {
        	fentries.clear();
        	
	        document = parser.parse(_ndndurl);
	        NodeList nodes = document.getElementsByTagName("fentry");
	        Log.info("fentry count: " + nodes.getLength());
	        
	        for(int i = 0; i< nodes.getLength(); i++) {
//	        	System.out.println("Parsing entry " + i);
	        	Node node = nodes.item(i);    	
	        	FEntry fentry = new FEntry(node);
	        	
	        	for(Dest dest : fentry.dests) {
	        		TreeSet<ContentName> regs = fentries.get(dest.faceid);
	        		if( null == regs ) {
	        			regs = new TreeSet<ContentName>();
	        			fentries.put(dest.faceid, regs);
	        		}
		        	regs.add(fentry.entryprefix);
	        	}
	        }
	        
//	        for(Integer faceid : fentries.keySet() ) {
//	        	dumpreg(faceid);
//	        }
        }
	}
	
	private static class Dest {
		int faceid = -1;
		int flags = 0;
		long expires = -1;
		
		public Dest(Node node) {
			if( node.getNodeName() != "dest" )
				throw new ClassCastException("node is not of type 'dest': " + node.getNodeName());
			NodeList children = node.getChildNodes();
			for(int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				
				if( child.getNodeName() == "faceid" ) 
					faceid = Integer.parseInt(child.getFirstChild().getNodeValue());
				
				if( child.getNodeName() == "flags" )
					flags = Integer.parseInt(child.getFirstChild().getNodeValue());

				if( child.getNodeName() == "expires" )
					expires = Long.parseLong(child.getFirstChild().getNodeValue());

			}
		}
		
		public String toString() {
			return String.format("faceid %d flags %08X expires %d", faceid, flags, expires);
		}
	}
	
	private static class FEntry {
		ContentName entryprefix = null;
		LinkedList<Dest> dests = new LinkedList<Dest>();
		
		public FEntry(final Node node) throws MalformedContentNameStringException, DOMException {
			if( node.getNodeName() != "fentry" )
				throw new ClassCastException("node is not of type 'fentry'");
			
			NodeList children = node.getChildNodes();
			for(int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if( child.getNodeName() == "dest" ) {
					Dest dest = new Dest(child);
//					System.out.println("adding: " + dest.toString());
					dests.add(dest);
				}
				
				if( child.getNodeName() == "prefix" ) {
					entryprefix = ContentName.fromURI(child.getFirstChild().getNodeValue());
//					System.out.println("prefix " + entryprefix.toURIString());
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("prefix: ");
			sb.append(entryprefix.toURIString());
			sb.append('\n');
			for(Dest dest : dests) {
				sb.append("  faceid: ");
				sb.append(dest.faceid);
				sb.append('\n');
			}
			return sb.toString();
		}	
	}
	
	private int getFaceId(AssertionNDNHandle handle, String type) throws NDNDaemonException, InterruptedException, Error {
		PrefixRegistrationManager prm = new PrefixRegistrationManager(handle);
		String reg = String.format("ndn:%s/%s_%016X", _prefix, type, _rnd.nextLong());
		ForwardingEntry entry = prm.selfRegisterPrefix(reg);
		handle.checkError(CHECK_TIMEOUT);
		return entry.getFaceID();
	}
	
	/**
	 * 
	 * @param faceid
	 * @return the # of prefixes registered on the face
	 */
	private int dumpreg(int faceid) {
		int count = 0;
		synchronized(fentries) {
			TreeSet<ContentName> regs = fentries.get(faceid);
			Log.info("Registrations for faceid " + faceid);
			for(ContentName name : regs) {
				Log.info("   " + name.toURIString());
				count++;
			}
		}
		return count;
	}
}
