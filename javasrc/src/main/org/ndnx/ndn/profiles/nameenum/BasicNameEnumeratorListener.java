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

package org.ndnx.ndn.profiles.nameenum;

import java.util.ArrayList;

import org.ndnx.ndn.protocol.ContentName;


/**
 * Interface for classes making name enumeration requests to allow callbacks with content matching registered prefixes.
 *
 * @see org.ndnx.ndn.profiles.nameenume.NDNNameEnumerator
 */

public interface BasicNameEnumeratorListener {

	/**
	 * Callback called when we get a collection matching a registered prefix.
	 * @param prefix The ContentName prefix with matching responses.
	 * @param  names An ArrayList of ContentNames matching the prefix.
	 *   
	 * @return int The number of names in the collection.
	 */
	public int handleNameEnumerator(ContentName prefix, ArrayList<ContentName> names);

}
