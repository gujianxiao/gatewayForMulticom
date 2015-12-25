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

package org.ndnx.ndn;

/**
 * Top-level Exception subclass to mark NDN exceptions. There are some NDN exceptions
 * that do not extend this type, because they need to extend IOException.
 */
public class NDNException extends Exception {

	private static final long serialVersionUID = 1010994050811465163L;

	public NDNException() {	}

	public NDNException(String message) {
		super(message);
	}

	public NDNException(Throwable cause) {
		super(cause);
	}

	public NDNException(String message, Throwable cause) {
		super(message, cause);
	}

}
