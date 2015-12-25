/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.search;

import java.io.IOException;
import java.util.Set;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.config.SystemConfiguration;
import org.ndnx.ndn.profiles.VersioningProfile;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;

/**
 * Like ObjectPathfinder, this subclass searches for a content object with a specific postfix
 * along the path from a starting point to a stopping point.
 * We can search for matching content that is either closest or furthest from the starting point. 
 * 
 * When the closest (or furthest) matching content object is found,
 * LatestVersionPathfinder retrieves the latest version of that object.
 * If the latest version is not GONE (or if goneOK is True), the latest version is returned.
 * Otherwise, we update the starting point or stopping point (depending on _closestOnPath)
 * and start a new search if the new range is non empty.
 *
 */

public class LatestVersionPathfinder extends ObjectPathfinder {

	public LatestVersionPathfinder(ContentName startingPoint, ContentName stoppingPoint, ContentName desiredPostfix, 
			  boolean closestOnPath, boolean goneOK,
			  int timeout, 
			  Set<ContentName> searchedPathCache,
			  NDNHandle handle) throws IOException {
		super(startingPoint, stoppingPoint, desiredPostfix, closestOnPath, goneOK, timeout, searchedPathCache, handle);
	}

	@Override
	public synchronized SearchResults waitForResults() {
		boolean searchSpaceEmpty = false;
		while (! searchSpaceEmpty) {
			SearchResults sr = super.waitForResults();
			ContentObject co = sr.getResult(); 
			if (null != co) {
				try {
					// get the latest version
					ContentObject coLV = 
						VersioningProfile.getFirstBlockOfLatestVersion(co.name(), null, null, 
								SystemConfiguration.getDefaultTimeout(), _handle.defaultVerifier(), _handle);
					if (null != coLV) co = coLV;
					if (goneOK() || (! co.isGone())) {
						sr.setResult(co);
						return sr;
					}
					// the latest version of the ContentObject found is GONE, and goneOK = false
					// so we cancel outstanding interests (if any),
					// we update the starting point or stopping point (depending on _closestOnPath)
					// and start a new search if the new range is non empty
					if (! done()) stopSearch();
					if (_closestOnPath) {
						ContentName icn = sr.getInterestName();
						icn = icn.cut(icn.count() - _postfix.count());
						System.out.println("Stopping point " + _stoppingPoint);
						System.out.println("interest: " + icn);
						if (icn.count() > _stoppingPoint.count()) {
							_startingPoint = icn.cut(icn.count() - 1);
							System.out.println("new starting point: " + _startingPoint);
						}
						else searchSpaceEmpty = true;
					}
					else {
						ContentName icn = sr.getInterestName();
						icn = icn.cut(icn.count() - _postfix.count());
						if (icn.count() < _startingPoint.count()) {
							_stoppingPoint = _startingPoint.cut(icn.count() + 1);												
						}
						else searchSpaceEmpty = true;
					}
					if (! searchSpaceEmpty) {
						startSearch();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
					return new SearchResults(null, null);
				}
			}
			else return sr;
		}
		return new SearchResults(null, null);
	}
	
}
