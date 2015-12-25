/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2013 Palo Alto Research Center, Inc.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;


/**
 * A representation of a collection of NDN objects, represented as a list of Link s.
 * See Link for a discussion of what such links can refer to and their security.
 * A Collection is the easiest way in NDN of representing extensional containment -- a fixed
 * list of items that form a group, where it is possible to say that an item is either
 * in or out of the group. Containment in NDN can also be represented intentionally -- 
 * by looking at the set of children a given name currently has. In that latter case,
 * it is impossible to say in general whether something is not in the set, as there
 * might exist a child with that name, but it is not accessible to your network at
 * the moment.
 * 
 * By tailoring the meaning of and labels attached to the Links in a Collection,
 * one can generate special-purpose Collection subclasses with particular semantics,
 * useful in particular applications. See ACL and the Name Enumeration protocol
 * for examples of this.
 */
public class Collection extends GenericXMLEncodable implements XMLEncodable, Iterable<Link>, Cloneable {
	
	/**
	 * A NDNNetworkObject wrapper around Collection, used for easily saving and retrieving
	 * versioned Collections to NDN. A typical pattern for using network objects to save
	 * objects that happen to be encodable or serializable is to incorporate such a static
	 * member wrapper class subclassing NDNEncodableObject, NDNSerializableObject, or
	 * NDNNetworkObject itself inside the main class definition.
	 */
	public static class CollectionObject extends NDNEncodableObject<Collection> {
		
		public CollectionObject(ContentName name, Collection data, 
								SaveType saveType, NDNHandle handle) throws IOException {
			super(Collection.class, true, name, data,saveType, handle);
		}
		
		public CollectionObject(ContentName name, 
								java.util.Collection<Link> contents, 
								SaveType saveType, NDNHandle handle) throws IOException {
			this(name, new Collection(contents), saveType, handle);
		}
		
		public CollectionObject(ContentName name, Link [] contents, 
								SaveType saveType, NDNHandle handle) throws IOException {
			this(name, new Collection(contents),saveType,  handle);			
		}

		public CollectionObject(ContentName name, Collection data, SaveType saveType,
								PublisherPublicKeyDigest publisher, 
								KeyLocator keyLocator, NDNHandle handle) throws IOException {
			super(Collection.class, true, name, data, saveType, publisher, keyLocator, handle);
		}

		public CollectionObject(ContentName name, 
								java.util.Collection<Link> contents, 
								SaveType saveType,
								PublisherPublicKeyDigest publisher, KeyLocator keyLocator, NDNHandle handle) throws IOException {
			this(name, new Collection(contents), saveType, publisher, keyLocator, handle);
		}
		
		public CollectionObject(ContentName name, Link [] contents, 
								SaveType saveType,
								PublisherPublicKeyDigest publisher, 
								KeyLocator keyLocator, NDNHandle handle) throws IOException {
			this(name, new Collection(contents), saveType, publisher, keyLocator, handle);			
		}

		public CollectionObject(ContentName name, PublisherPublicKeyDigest publisher, NDNHandle handle) 
				throws ContentDecodingException, IOException {
			super(Collection.class, true, name, publisher, handle);
		}
		
		public CollectionObject(ContentObject firstBlock, NDNHandle handle) 
				throws ContentDecodingException, IOException {
			super(Collection.class, true, firstBlock, handle);
		}
		
		public CollectionObject(ContentName name, NDNHandle handle) 
				throws ContentDecodingException, IOException {
			super(Collection.class, true, name, (PublisherPublicKeyDigest)null, handle);
		}

		public CollectionObject(ContentName name, Collection data, 
				PublisherPublicKeyDigest publisher, 
				KeyLocator keyLocator, NDNFlowControl flowControl) throws IOException {
			super(Collection.class, true, name, data, publisher, keyLocator, flowControl);
		}

		public CollectionObject(ContentName name,
				PublisherPublicKeyDigest publisher, NDNFlowControl flowControl)
		throws ContentDecodingException, IOException {
			super(Collection.class, true, name, publisher, flowControl);
		}

		public CollectionObject(ContentObject firstBlock,
				NDNFlowControl flowControl) 
		throws ContentDecodingException, IOException {
			super(Collection.class, true, firstBlock, flowControl);
		}

		public Collection collection() throws ContentNotReadyException, ContentGoneException, ErrorStateException {
			return data();
		}
		
		public LinkedList<Link> contents() throws ContentNotReadyException, ContentGoneException, ErrorStateException { 
			if (null == data())
				return null;
			return data().contents(); 
		}
	}
	
	protected LinkedList<Link> _contents = new LinkedList<Link>();
	
	public Collection() {
	}
	
	@SuppressWarnings("unchecked")
	public Collection clone() {
		try {
			Collection c = (Collection)super.clone();
			c._contents = (LinkedList<Link>)_contents.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
	public Collection(java.util.Collection<Link> contents) {
		_contents.addAll(contents); // should we clone each?
	}
	
	public Collection(Link [] contents) {
		if (contents != null) {
			for (int i=0; i < contents.length; ++i) {
				_contents.add(contents[i]);
			}
		}
	}
	
	/**
	 * Make a Collection containing Links which only specify names.
	 * @param nameContents The list of names to link to.
	 */
	public Collection(ArrayList<ContentName> nameContents) {
		if (null != nameContents) {
			for (ContentName name : nameContents) {
				_contents.add(new Link(name));
			}
		}
	}
	
	/**
	 * Make a Collection containing Links which only specify names and a single label.
	 * @param nameContents The list of names to link to.
	 */
	public Collection(String label, ArrayList<ContentName> nameContents) {
		if (null != nameContents) {
			for (ContentName name : nameContents) {
				_contents.add(new Link(name, label, null));
			}
		}
	}

	public LinkedList<Link> contents() { 
		return _contents; 
	}
		
	public Link get(int i) {
		return contents().get(i);
	}
	
	/**
	 * Return the first Link with matching label, if any.
	 * @param label
	 * @return
	 */
	public Link get(String label) {
		if (null == label)
			return null;
		
		for (Link link : _contents) {
			if (label.equals(link.targetLabel())) {
				return link;
			}
		}
		return null;
	}
	
	public void add(Link content) {
		_contents.add(content);
	}
	
	public void add(ArrayList<Link> contents) {
		_contents.addAll(contents);
	}
	
	public void add(String label, ArrayList<ContentName> nameContents) {
		if (null != nameContents) {
			for (ContentName name : nameContents) {
				_contents.add(new Link(name, label, null));
			}
		}
	}
	
	public void add(String label, ContentName target) {
		_contents.add(new Link(target, label, null));
	}

	public Link remove(int i) {
		return _contents.remove(i);
	}
	
	public boolean remove(Link content) {
		return _contents.remove(content);
	}

	public void removeAll() {
		_contents.clear();
	}
	
	public int size() { return _contents.size(); }
	
	/**
	 * Find all the elements in this Collection that match target on any of the 
	 * parameters it has set, and return them.
	 */
	public ArrayList<Link> find(Link target) {
		ArrayList<Link> results = new ArrayList<Link>();
		for (Link link : _contents) {
			if (target.approximates(link)) {
				results.add(link);
			}
		}
		return results;
	}
	
	public ArrayList<Link> find(ContentName targetName) {
		return find(new Link(targetName));
	}
	
	public ArrayList<Link> find(String targetLabel) {
		return find(new Link(null, targetLabel, null));
	}

	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		_contents.clear();
		
		decoder.readStartElement(getElementLabel());

		Link link = null;
		while (decoder.peekStartElement(NDNProtocolDTags.Link)) {
			link = new Link();
			link.decode(decoder);
			add(link);
		}
		decoder.readEndElement();
	}

	@Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		if (!validate()) {
			throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
		}
		encoder.writeStartElement(getElementLabel());
		Iterator<Link> linkIt = contents().iterator();
		while (linkIt.hasNext()) {
			Link link = linkIt.next();
			link.encode(encoder);
		}
		encoder.writeEndElement();   		
	}
	
	@Override
	public boolean validate() { 
		return (null != contents());
	}
	
	@Override
	public long getElementLabel() { return NDNProtocolDTags.Collection; }

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((_contents == null) ? 0 : _contents.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Collection other = (Collection) obj;
		if (_contents == null) {
			if (other._contents != null)
				return false;
		} else if (!_contents.equals(other._contents))
			return false;
		return true;
	}

	/**
	 * More concise toString.
	 */
	public String toString() {
		
		StringBuffer sbuf = new StringBuffer(getElementLabel() + ":\n");
		for (Link link : _contents) {
			sbuf.append("	" + link.toString() + "\n");
		}
		sbuf.append("\n");
		return sbuf.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Link> iterator() {
		return _contents.iterator();
	}
}
