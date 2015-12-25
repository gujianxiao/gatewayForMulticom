/*
 * A NDNx library test.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2013 Palo Alto Research Center, Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.repo.LogStructRepoStore;
import org.ndnx.ndn.impl.repo.RepositoryStore;
import org.ndnx.ndn.impl.repo.RepositoryException;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNOutputStream;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;


/**
 * Part of repository test infrastructure.
 */
public class RepoPerformanceTester extends NDNOutputStream {	

	private static RepoPerformanceTester _rpt = new RepoPerformanceTester();

	private class TestFlowControl extends NDNFlowControl {
		
		private RepositoryStore _repo = null;
		
		public TestFlowControl(String repoName, ContentName name, NDNHandle handle)
				throws RepositoryException, IOException {
			super(name, handle);
			if (repoName != null) {
				_repo = new LogStructRepoStore();
				_repo.initialize(repoName, null, null, null, null, null);
			}
		}
		
		public ContentObject put(ContentObject co) throws IOException {
			if (_repo != null) {
				try {
					_repo.saveContent(co);
				} catch (RepositoryException e) {
					throw new IOException(e.getMessage());
				}
			}
			return co;
		}
	}
	
	public RepoPerformanceTester() {}
	
	public RepoPerformanceTester(String repoName, ContentName name, NDNHandle handle)
			throws IOException, RepositoryException {
		super(name, null, null, null, null, _rpt.new TestFlowControl(repoName, name, handle));
	}
	
	public RepoPerformanceTester(ContentName name, NDNFlowControl cf)
			throws IOException, RepositoryException {
		super(name, null, null, null, null, cf);
	}
	
	public RepoPerformanceTester getTester(String repoName, ContentName name, NDNHandle handle) 
			throws MalformedContentNameStringException, IOException, RepositoryException {
		return new RepoPerformanceTester(repoName, name, handle);
	}
	
	public void doTest(String[] args) {
		ContentName argName;
		long startTime = new Date().getTime();
		Log.setLevel(Level.SEVERE);	// turn off logging
		try {
			argName = ContentName.fromURI(args[0]);
			NDNHandle handle = NDNHandle.open();
			
			File theFile = new File(args[1]);
			if (!theFile.exists()) {
				System.out.println("No such file: " + args[1]);
				return;
			}
			Log.info("repo_test: putting file " + args[1] + " bytes: " + theFile.length());
			RepoPerformanceTester ostream = getTester(args.length > 2 ? args[2] : null, argName, handle);
			do_write(ostream, theFile);
			
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		long endTime = new Date().getTime();
		System.out.println("Inserted file " + args[1] + " in " + (endTime - startTime) + " ms");
		System.exit(0);
		
	}
	
	private static int BLOCK_SIZE = 8096;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		_rpt.doTest(args);
	}
	
	private static void do_write(NDNOutputStream ostream, File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		int size = BLOCK_SIZE;
		byte [] buffer = new byte[BLOCK_SIZE];
		do {
			Log.info("do_write: " + fis.available() + " bytes left.");
			if (size > fis.available())
				size = fis.available();
			if (size > 0) {
				fis.read(buffer, 0, size);
				ostream.write(buffer, 0, size);
				Log.info("do_write: wrote " + size + " bytes.");
			}
		} while (fis.available() > 0);
		ostream.close();
	}
	
}
