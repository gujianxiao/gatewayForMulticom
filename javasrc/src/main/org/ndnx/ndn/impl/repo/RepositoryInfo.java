/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008-2012 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.impl.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.io.content.NDNEncodableObject;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.io.content.ContentGoneException;
import org.ndnx.ndn.io.content.ContentNotReadyException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;

/**
 * Object to return information associated with a Repository to a repository client
 */

public class RepositoryInfo extends GenericXMLEncodable implements XMLEncodable{

	protected static double _version = 1.1;

	protected String _repoVersion = null;
	protected String _localName = null;
	protected GlobalPrefix _globalPrefix = null;
	protected ArrayList<ContentName> _names = new ArrayList<ContentName>();
	protected ContentName _policyName;
	protected String _infoString = null;
	protected RepoInfoType _type = RepoInfoType.INFO;

	/**
	 * The two possible types.
	 * INFO is used for most acknowledgements
	 * DATA is used for positive responses (no need for data transfer) to checked write requests
	 */
	public enum RepoInfoType {
		INFO ("INFO"),
		DATA ("DATA");

		private String _stringValue = null;

		RepoInfoType() {}

		RepoInfoType(String stringValue) {
			this._stringValue = stringValue;
		}

		static RepoInfoType valueFromString(String value) {
			for (RepoInfoType pv : RepoInfoType.values()) {
				if (pv._stringValue != null) {
					if (pv._stringValue.equals(value.toUpperCase()))
						return pv;
				}
			}
			return null;
		}
	}

	private static class GlobalPrefix extends ContentName {

		private static final long serialVersionUID = -6669511721290965466L;

		public GlobalPrefix(ContentName cn) {
			super(cn);
		}

		public GlobalPrefix() {
			super();
		}

		@Override
		public long getElementLabel() { return NDNProtocolDTags.GlobalPrefixName; }
	}

	protected static final HashMap<RepoInfoType, String> _InfoTypeNames = new HashMap<RepoInfoType, String>();

	/**
	 * A NDNNetworkObject wrapper around RepositoryInfo, used for easily saving and retrieving
	 * versioned RepositoryInfos to NDN. A typical pattern for using network objects to save
	 * objects that happen to be encodable or serializable is to incorporate such a static
	 * member wrapper class subclassing NDNEncodableObject, NDNSerializableObject, or
	 * NDNNetworkObject itself inside the main class definition.
	 */
	public static class RepositoryInfoObject extends NDNEncodableObject<RepositoryInfo> {

		/**
		 * Read constructor. We have separated constructors into "write" and "read";
		 * basically if you are only going to read with an object, use a read constructor
		 * unless you do not want update() to be called to read data in the constructor
		 * (for example, because you know no data has been written yet to this name).
		 * In this case, use a write constructor, set the data argument to null, and
		 * later call update or one of the forms of updateInBackground to read data
		 * into the object.
		 *
		 * If the first thing you are going to do with an object is write data, use a
		 * write constructor, with a SaveType chosen to reflect where you are going to
		 * write data to (e.g. directly to the network or to a repository). The SaveType
		 * just controls the choice of flow controller used by the object, so constructors
		 * that explicitly specify a flow controller do not need a SaveType.
		 *
		 * If you want to use an object to both read and write data, and you create it
		 * with a read constructor, you must call setupSave on the object prior to writing
		 * to set its SaveType, unless you specified a flow controller in the constructor.
		 */
		public RepositoryInfoObject(ContentName name, NDNHandle handle)
		throws ContentDecodingException, IOException {
			super(RepositoryInfo.class, true, name, (PublisherPublicKeyDigest)null, handle);
		}

		/**
		 * Read constructor.
		 */
		public RepositoryInfoObject(ContentName name,
				PublisherPublicKeyDigest publisher, NDNHandle handle)
				throws ContentDecodingException, IOException {
			super(RepositoryInfo.class, true, name, publisher, handle);
		}

		/**
		 * Read constructor.
		 */
		public RepositoryInfoObject(ContentName name,
				PublisherPublicKeyDigest publisher, NDNFlowControl flowControl)
				throws ContentDecodingException, IOException {
			super(RepositoryInfo.class, true, name, publisher, flowControl);
		}

		/**
		 * Read constructor.
		 */
		public RepositoryInfoObject(ContentObject firstBlock,
				NDNHandle handle) throws ContentDecodingException, IOException {
			super(RepositoryInfo.class, true, firstBlock, handle);
		}

		/**
		 * Read constructor.
		 */
		public RepositoryInfoObject(ContentObject firstBlock,
				NDNFlowControl flowControl) throws ContentDecodingException,
				IOException {
			super(RepositoryInfo.class, true, firstBlock, flowControl);
		}

		/**
		 * Write constructor.
		 */
		public RepositoryInfoObject(ContentName name,
				RepositoryInfo data, SaveType saveType, NDNHandle handle)
				throws IOException {
			super(RepositoryInfo.class, true, name, data, saveType, handle);
		}

		/**
		 * Write constructor.
		 */
		public RepositoryInfoObject(ContentName name,
				RepositoryInfo data, SaveType saveType,
				PublisherPublicKeyDigest publisher, KeyLocator keyLocator,
				NDNHandle handle) throws IOException {
			super(RepositoryInfo.class, true, name, data, saveType, publisher, keyLocator,
					handle);
		}

		/**
		 * Write constructor.
		 */
		public RepositoryInfoObject(ContentName name,
				RepositoryInfo data, PublisherPublicKeyDigest publisher,
				KeyLocator keyLocator, NDNFlowControl flowControl)
				throws IOException {
			super(RepositoryInfo.class, true, name, data, publisher, keyLocator, flowControl);
		}

		public RepositoryInfo repositoryInfo() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }
	}

	/**
	 * The main constructor used to create the information returned by a repository during initialization
	 *
	 * @param version
	 * @param globalPrefix
	 * @param localName
	 * @throws MalformedContentNameStringException
	 */
	public RepositoryInfo(String version, String globalPrefix, String localName) throws MalformedContentNameStringException {
		_localName = localName;
		_repoVersion = version;
		if (!globalPrefix.startsWith("/"))
			globalPrefix = "/" + _globalPrefix;
		_globalPrefix = new GlobalPrefix(ContentName.fromNative(globalPrefix));
	}

	/**
	 * Create String info to return
	 *
	 * @param version
	 * @param globalPrefix
	 * @param localName
	 * @param info - a string with info to return
	 * @throws MalformedContentNameStringException
	 */
	public RepositoryInfo(String version, ContentName globalPrefix, String localName, String info) throws MalformedContentNameStringException {
		_localName = localName;
		_repoVersion = version;
		_infoString = info;
		_globalPrefix = new GlobalPrefix(globalPrefix);
	}

	/**
	 * Currently used only to test decoding/encoding
	 *
	 * @param version
	 * @param globalPrefix
	 * @param localName
	 * @param names
	 * @throws MalformedContentNameStringException
	 */
	public RepositoryInfo(String version, String globalPrefix, String localName, ArrayList<ContentName> names) throws MalformedContentNameStringException {
		this(localName, globalPrefix, version);
		for (ContentName name : names) {
			_names.add(name);
		}
		_type = RepoInfoType.DATA;
	}

	/**
	 * Constructor for a DATA packet using names
	 *
	 * @param version
	 * @param globalPrefix
	 * @param localName
	 * @param names
	 * @throws MalformedContentNameStringException
	 */
	public RepositoryInfo(String version, ContentName globalPrefix, String localName, ArrayList<ContentName> names) throws MalformedContentNameStringException {
		_localName = localName;
		_repoVersion = version;
		_globalPrefix = new GlobalPrefix(globalPrefix);
		for (ContentName name : names) {
			_names.add(name);
		}
		_type = RepoInfoType.DATA;
	}

	public RepositoryInfo() {}	// For decoding

	/**
	 * Gets the current local name as a slash separated String
	 * @return the local name
	 */
	public String getLocalName() {
		return _localName;
	}

	/**
	 * Gets the current global prefix as a ContentName
	 * @return the prefix
	 */
	public ContentName getGlobalPrefix() {
		return _globalPrefix;
	}

	/**
	 * Gets the ContentName that would be used to change policy for this repository
	 * @return the name
	 */
	public synchronized ContentName getPolicyName() {
		if (null == _policyName) {
			_policyName = BasicPolicy.getPolicyName(_globalPrefix);
			if (Log.isLoggable(Log.FAC_REPO, Level.INFO)) {
				Log.info(Log.FAC_REPO, "REPO: Policy name for repository: {0}", _policyName);
			}
		}
		return _policyName;
	}

	/**
	 * Gets String info returned by the repo
	 * @return
	 */
	public String getInfo() {
		return _infoString;
	}

	/**
	 * Get the type of information encapsulated in this object (INFO or DATA).
	 * @return
	 */
	public RepoInfoType getType() {
		return _type;
	}

	/**
	 * Gets the repository store version as a String
	 * @return the version
	 */
	public String getRepositoryVersion() {
		return _repoVersion;
	}

	/**
	 * Get the repository store version as a Double
	 * @return the version
	 */
	public String getVersion() {
		return Double.toString(_version);
	}

	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		decoder.readStartElement(getElementLabel());
		_version = Double.valueOf(decoder.readUTF8Element(NDNProtocolDTags.Version));
		_type = RepoInfoType.valueFromString(decoder.readUTF8Element(NDNProtocolDTags.Type));
		_repoVersion = decoder.readUTF8Element(NDNProtocolDTags.RepositoryVersion);

		_globalPrefix = new GlobalPrefix();
		_globalPrefix.decode(decoder);

		_localName = decoder.readUTF8Element(NDNProtocolDTags.LocalName);
		while (decoder.peekStartElement(NDNProtocolDTags.Name)) {
			ContentName name = new ContentName();
			name.decode(decoder);
			_names.add(name);
		}
		if (decoder.peekStartElement(NDNProtocolDTags.InfoString))
			_infoString = decoder.readUTF8Element(NDNProtocolDTags.InfoString);
		decoder.readEndElement();
	}

	@Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		if (!validate()) {
			throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
		}
		encoder.writeStartElement(getElementLabel());
		encoder.writeElement(NDNProtocolDTags.Version, Double.toString(_version));
		encoder.writeElement(NDNProtocolDTags.Type, getType().toString());
		encoder.writeElement(NDNProtocolDTags.RepositoryVersion, _repoVersion);
		_globalPrefix.encode(encoder);
		encoder.writeElement(NDNProtocolDTags.LocalName, _localName);
		if (_names.size() > 0) {
			for (ContentName name : _names)
				name.encode(encoder);
		}
		if (null != _infoString)
			encoder.writeElement(NDNProtocolDTags.InfoString, _infoString);
		encoder.writeEndElement();
	}

	@Override
	public long getElementLabel() { return NDNProtocolDTags.RepositoryInfo; }

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((_globalPrefix == null) ? 0 : _globalPrefix.hashCode());
		result = prime * result
				+ ((_localName == null) ? 0 : _localName.hashCode());
		result = prime * result + ((_names == null) ? 0 : _names.hashCode());
		result = prime * result
				+ ((_policyName == null) ? 0 : _policyName.hashCode());
		result = prime * result
				+ ((_repoVersion == null) ? 0 : _repoVersion.hashCode());
		result = prime * result + ((_type == null) ? 0 : _type.hashCode());
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
		RepositoryInfo other = (RepositoryInfo) obj;
		if (_globalPrefix == null) {
			if (other._globalPrefix != null)
				return false;
		} else if (!_globalPrefix.equals(other._globalPrefix))
			return false;
		if (_localName == null) {
			if (other._localName != null)
				return false;
		} else if (!_localName.equals(other._localName))
			return false;
		if (_names == null) {
			if (other._names != null)
				return false;
		} else if (!_names.equals(other._names))
			return false;
		if (_policyName == null) {
			if (other._policyName != null)
				return false;
		} else if (!_policyName.equals(other._policyName))
			return false;
		if (_repoVersion == null) {
			if (other._repoVersion != null)
				return false;
		} else if (!_repoVersion.equals(other._repoVersion))
			return false;
		if (_type == null) {
			if (other._type != null)
				return false;
		} else if (!_type.equals(other._type))
			return false;
		if (_infoString == null) {
			if (other._infoString != null)
				return false;
		} else if (!_infoString.equals(other._infoString))
			return false;
		return true;
	}
}
