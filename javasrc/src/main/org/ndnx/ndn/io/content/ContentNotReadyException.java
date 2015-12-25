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

package org.ndnx.ndn.io.content;

import java.io.IOException;

/**
 * IOException type to indicate when a given NetworkObject has not yet retrieved its
 * content.
 */
public class ContentNotReadyException extends IOException {

	private static final long serialVersionUID = 6732044053240082669L;

	/**
	 * TODO add constructor taking Throwable when move to 1.6
	 */
	public ContentNotReadyException() {
		super();
	}

	public ContentNotReadyException(String s) {
		super(s);
	}

}
