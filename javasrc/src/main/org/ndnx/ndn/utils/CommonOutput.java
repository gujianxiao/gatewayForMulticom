/*
 * Part of the NDNx command line utilities
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2010, 2012 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.ConfigurationException;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.NDNFileOutputStream;
import org.ndnx.ndn.io.NDNOutputStream;
import org.ndnx.ndn.io.RepositoryFileOutputStream;
import org.ndnx.ndn.io.RepositoryOutputStream;
import org.ndnx.ndn.protocol.NDNTime;
import org.ndnx.ndn.protocol.ContentName;

public abstract class CommonOutput {

	protected NDNTime doPut(NDNHandle handle, String fileName,
			ContentName nodeName) throws IOException, InvalidKeyException, ConfigurationException {
		InputStream is;
		if (CommonParameters.verbose)
			System.out.printf("filename %s\n", fileName);
		if (fileName.startsWith("http://")) {
			if (CommonParameters.verbose)
				System.out.printf("filename is http\n");
			is = new URL(fileName).openStream();
		} else {
			if (CommonParameters.verbose)
				System.out.printf("filename is file\n");
			File theFile = new File(fileName);

			if (!theFile.exists()) {
				System.out.println("No such file: " + theFile.getName());
				usage(CommonArguments.getExtraUsage());
			}
			is = new FileInputStream(theFile);
		}

		NDNOutputStream ostream;

		// Use file stream in both cases to match behavior. NDNOutputStream doesn't do
		// versioning and neither it nor NDNVersionedOutputStream add headers.
		if (CommonParameters.rawMode) {
			if (CommonParameters.unversioned)
				ostream = new NDNOutputStream(nodeName, handle);
			else
				ostream = new NDNFileOutputStream(nodeName, handle);
		} else {
			if (CommonParameters.unversioned)
				ostream = new RepositoryOutputStream(nodeName, handle, CommonParameters.local);
			else
				ostream = new RepositoryFileOutputStream(nodeName, handle, CommonParameters.local);
		}
		if (CommonParameters.timeout != null)
			ostream.setTimeout(CommonParameters.timeout);
		do_write(ostream, is);

		return ostream.getVersion();
	}

	private void do_write(NDNOutputStream ostream, InputStream is) throws IOException {
		long time = System.currentTimeMillis();
		int size = CommonParameters.BLOCK_SIZE;
		int readLen = 0;
		byte [] buffer = new byte[CommonParameters.BLOCK_SIZE];
		if( Log.isLoggable(Level.FINER)) {
			Log.finer("do_write: " + is.available() + " bytes left.");
			while ((readLen = is.read(buffer, 0, size)) != -1){
				ostream.write(buffer, 0, readLen);
				Log.finer("do_write: wrote " + size + " bytes.");
				Log.finer("do_write: " + is.available() + " bytes left.");
			}
		} else {
			while ((readLen = is.read(buffer, 0, size)) != -1){
				ostream.write(buffer, 0, readLen);
			}
		}
		ostream.close();
		Log.fine("finished write: {0}", System.currentTimeMillis() - time);
	}

	protected abstract void usage(String extraUsage);
}
