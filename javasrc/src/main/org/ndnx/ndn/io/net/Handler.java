/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.ndnx.ndn.io.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Initial steps to making Java's built in URL parsing and loading
 * infrastructure be able to handle ndn: URLs. 
 * Needs additional testing, not guaranteed to work yet.
 */
public class Handler extends URLStreamHandler {

	public Handler() {
		super();
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new NDNURLConnection(url);
	}
	
	public static void register() {
		final String packageName =
			Handler.class.getPackage().getName();
		final String pkg = packageName.substring(0, packageName.lastIndexOf('.'));
		final String protocolPathProp = "java.protocol.handler.pkgs";

		String uriHandlers = System.getProperty(protocolPathProp, "");
		if (uriHandlers.indexOf(pkg) == -1) {
			if (uriHandlers.length() != 0)
				uriHandlers += "|";
			uriHandlers += pkg;
			System.setProperty(protocolPathProp, uriHandlers);
		}
	}
}
