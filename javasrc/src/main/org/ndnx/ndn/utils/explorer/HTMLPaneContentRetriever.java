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

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.io.NDNFileInputStream;
import org.ndnx.ndn.profiles.security.access.AccessDeniedException;
import org.ndnx.ndn.protocol.ContentName;

/**
 * Class used by the ContentExplorer to retrieve content and display
 * .txt and .text files in the GUI preview pane.  This will be updated
 * in a future release to handle more content types.
 * 
 */
public class HTMLPaneContentRetriever implements Runnable {

	private String name = null;
	private JEditorPane htmlPane = null;
	private NDNHandle handle = null;
	private boolean txtpopup = false;
	
	/**
	 * Constructor for the HTMLPaneContentRetriever.
	 * 
	 * @param h NDNHandle to use for downloading the content
	 * @param p JEditorPane to use for status messages
	 * @param n String representation of the content object name to download
	 */
	public HTMLPaneContentRetriever(NDNHandle h, JEditorPane p, String n){
		handle = h;
		htmlPane = p;
		name = n;
	}
	
	
	public void setTextPopup(boolean b) {
		txtpopup = b;
	}
	
	/**
	 * Set method for the ContentObject to download.
	 * 
	 * @param n String representation of the content
	 * @return void
	 */
	public void setFileName(String n){
		name = n;
	}
	
	/**
	 * Set method for the preview pane used for status messages.
	 * 
	 * @param pane JEditorPane used for status updates
	 * @return void
	 */
	public void setHTMLPane(JEditorPane pane){
		htmlPane = pane;
	}
	
	/**
	 * Set method for the NDNHandle.
	 * 
	 * @param h NDNHandle used to retrieve the content
	 * @return void
	 */
	public void setNDNHandle(NDNHandle h){
		handle = h;
	}
	
	/**
	 * Run method for the thread that will be used to download the content.
	 * As bytes for the object are retrieved, they will be displayed in the
	 * preview pane.
	 * 
	 * @return void
	 * 
	 * @see NDNFileInputStream
	 */
	public void run() {
		
		if (name == null) {
			System.err.println("Must set file name for retrieval");
			return;
		}
		
		if (htmlPane == null) {
			System.err.println("Must set htmlPane");
			return;
		}
		
		if (handle == null) {
			System.err.println("Must set NDNHandle");
			return;
		}
			
		ContentName fileName = null;
		
		try{
			//get the file name as a ContentName
			fileName = ContentName.fromURI(name);

			NDNFileInputStream fis = new NDNFileInputStream(fileName, handle);
				
			htmlPane.read(fis, fileName);
		} catch (AccessDeniedException ade) {
			htmlPane.setText("You don't have the decryption key for file " + name);
			JOptionPane.showMessageDialog(htmlPane, "You don't have the decryption key for file " + name);
			txtpopup = false;
		} catch (Exception e) {
			System.err.println("Could not retrieve file: "+name);
			htmlPane.setText(name + " is not available at this time.");
			e.printStackTrace();
		}
		
		if (txtpopup && fileName!=null) {
			try {
				//System.out.println("attempting to open: "+fileName);
				ShowTextDialog dialog = new ShowTextDialog(fileName, handle);
				dialog.displayText();
				dialog.setVisible(true);
			} catch (Exception e) {
				//Log.logException("Could not display the file", e);
				e.printStackTrace();
			}
		}

	}

}
