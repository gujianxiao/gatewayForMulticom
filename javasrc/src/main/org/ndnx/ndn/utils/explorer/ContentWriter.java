/*
 * A NDNx command line utility.
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

package org.ndnx.ndn.utils.explorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JEditorPane;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.io.RepositoryFileOutputStream;
import org.ndnx.ndn.profiles.SegmentationProfile;
import org.ndnx.ndn.protocol.ContentName;

/**
 * Class used by the ContentExplorer to write files to a repository.
 *
 */
public class ContentWriter implements Runnable{

	private File file = null;
	private ContentName ndnName = null;
	private NDNHandle handle = null;
	private JEditorPane htmlPane = null;
	
	
	/**
	 * Constructor for a ContentWriter.
	 * 
	 * @param h NDNHandle
	 * @param name ContentName to write the file to
	 * @param f The File selected to store in a repo
	 * @param pane JEditorPane for displaying status
	 */
	public ContentWriter(NDNHandle h, ContentName name, File f, JEditorPane pane){
		handle = h;
		file = f;
		ndnName = name;
		htmlPane = pane;
	}
	
	/**
	 * Method to set the JEditorPane.
	 * @param pane JEditorPane used for displaying status.
	 * @return void
	 */
	public void setHTMLPane(JEditorPane pane){
		htmlPane = pane;
	}
	
	/**
	 * Method to set the file selected for storing in a repository
	 * @param f File to store in a repository
	 * @return void
	 */
	public void setFile(File f){
		file = f;
	}
	
	/**
	 * Method to set the ContentName to write the file to.
	 * 
	 * @param name ContentName for the file to store in a repository
	 * @return void
	 */
	public void setContentName(ContentName name){
		ndnName = name;
	}
	
	/**
	 * Method to set the NDNHandle for storing the file in a repository.
	 * @param h NDNHandle for writing out the file
	 * @return void
	 */
	public void setNDNHandle(NDNHandle h){
		handle = h;
	}
	
	/**
	 * Run method for the ContentWriter thread.  The method checks if all of the
	 * relevant variables are not null. The thread then creates a RepositoryFileOutputStream
	 * to write the file out to a repository.  The preview pane is used to
	 * display the status of the upload and if it fails, the exception message.
	 * 
	 * @return void
	 * 
	 * @see RepositoryFileOutputStream
	 */
	public void run() {
		
		if (htmlPane == null) {
			System.err.println("Must set htmlPane to view status messages");
			return;
		}
		
		if (ndnName == null) {
			System.err.println("Must set ContentName for content objects");
			return;
		}
		
		if (file == null) {
			System.err.println("Must set file to write out to NDN");
			return;
		}
		
		if (handle == null) {
			System.err.println("Must set NDNHandle");
			return;
		}
		
		try {
			RepositoryFileOutputStream fos = new RepositoryFileOutputStream(ndnName, handle);
			FileInputStream fs = new FileInputStream(file);
			int bytesRead = 0;
			byte[] buffer = new byte[SegmentationProfile.DEFAULT_BLOCKSIZE];

			while ((bytesRead = fs.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}

			fos.close();
			htmlPane.setText("Finished writing file "+file.getName()+" as "+ndnName);
		} catch (IOException e) {
			htmlPane.setText("Error writing file "+file.getName()+" as "+ndnName+"\n\n"+"Error: "+e.getMessage());
			System.err.println("error writing file to repo: "+e.getMessage());
		}
	}
}
