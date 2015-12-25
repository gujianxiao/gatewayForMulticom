/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010-2012 Palo Alto Research Center, Inc.
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
package org.ndnx.ndn.profiles.namespace;

import org.ndnx.ndn.profiles.NDNProfile;
import org.ndnx.ndn.profiles.CommandMarker;
import org.ndnx.ndn.protocol.ContentName;

public class NamespaceProfile implements NDNProfile {

	public static final CommandMarker NAMESPACE_POLICY_MARKER = 
		CommandMarker.commandMarker(CommandMarker.MARKER_NAMESPACE, "policy");
	public static final byte [] NAMESPACE_POLICY_MARKER_BYTES = NAMESPACE_POLICY_MARKER.getBytes();
	protected static final ContentName POLICY_POSTFIX_NAME = 
		new ContentName(NAMESPACE_POLICY_MARKER_BYTES);

	/**
	 * Return the set of name components to add to get the policy path
	 */
	public static ContentName policyPostfix() {
		return POLICY_POSTFIX_NAME;
	}
	
	public static ContentName policyNamespace(ContentName name) {
		return name.append(POLICY_POSTFIX_NAME);
	}
	
}
