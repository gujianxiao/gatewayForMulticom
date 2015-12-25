/*
 * A NDNx chat program.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2010, 2011 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.apps.ndnchat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.ndnx.ndn.apps.ndnchat.NDNChatNet.NDNChatCallback;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;


/**
 * Based on a client/server chat example in Robert Sedgewick's Algorithms
 * in Java.
 * 
 * Refactored to be just the JFrame UI.
 */
public class NDNChat extends JFrame implements ActionListener, NDNChatCallback {
	private static final long serialVersionUID = -8779269133035264361L;

    // Chat window
    protected JTextArea  _messagePane = new JTextArea(10, 32);
    private JTextField _typedText   = new JTextField(32);

    private final NDNChatNet _chat;
    
    public NDNChat(String namespace) throws MalformedContentNameStringException {

    	_chat = new NDNChatNet(this, namespace);
    	
    	// close output stream  - this will cause listen() to stop and exit
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    try {
						stop();
					} catch (IOException e1) {
						System.out.println("IOException shutting down listener: " + e1);
						e1.printStackTrace();
					}
                }
            }
        );
        
        
        // Make window
        _messagePane.setEditable(false);
        _messagePane.setBackground(Color.LIGHT_GRAY);
        _messagePane.setLineWrap(true);
        _typedText.addActionListener(this);

        Container content = getContentPane();
        content.add(new JScrollPane(_messagePane), BorderLayout.CENTER);
        content.add(_typedText, BorderLayout.SOUTH);
        
        // display the window, with focus on typing box
        setTitle("NDNChat 1.2: [" + namespace + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        _typedText.requestFocusInWindow();
        setVisible(true);
    }
	
	/**
	 * Process input to TextField after user hits enter.
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			String newText = _typedText.getText();
			if ((null != newText) && (newText.length() > 0)) {
				_chat.sendMessage(newText);
			}

		} catch (Exception e1) {
			System.err.println("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
			e1.printStackTrace();
			recvMessage("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
		}
        _typedText.setText("");
        _typedText.requestFocusInWindow();
	}

	
	/**
	 * Add a message to the output.
	 * @param message
	 */
	public void recvMessage(String message) {
		_messagePane.insert(message, _messagePane.getText().length());
        _messagePane.setCaretPosition(_messagePane.getText().length());
	}
	
    public static void usage() {
    	System.err.println("usage: NDNChat <ndn URI>");
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(-1);
		}
		NDNChat client;
		try {
			client = new NDNChat(args[0]);
			client.start();
		} catch (MalformedContentNameStringException e) {
			System.err.println("Not a valid ndn URI: " + args[0] + ": " + e.getMessage());
			e.printStackTrace();
		} catch (ConfigurationException e) {
			System.err.println("Configuration exception running ndnChat: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException handling chat messages: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// =========================================================
	// Internal methods
	
	/**
	 * Called by window thread when when window closes
	 */
	protected void stop() throws IOException {
		_chat.shutdown();
	}
	
	/**
	 * This blocks until _chat.shutdown() called
	 * @throws IOException 
	 * @throws MalformedContentNameStringException 
	 * @throws ConfigurationException 
	 */
	protected void start() throws ConfigurationException, MalformedContentNameStringException, IOException {
		_chat.listen();
	}
}
