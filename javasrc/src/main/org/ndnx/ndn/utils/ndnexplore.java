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

package org.ndnx.ndn.utils;

import org.ndnx.ndn.utils.explorer.ContentExplorer;

/**
 * A command-line wrapper class for running an explorer from ndn_run script.
 * Note class name in utils package needs to match command name to work with ndn_run, 
 * hence this wrapper exists so the real implementation can be in its own subpackage
 * of utils.
 */
public class ndnexplore {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ContentExplorer.main(args);
	}

}
