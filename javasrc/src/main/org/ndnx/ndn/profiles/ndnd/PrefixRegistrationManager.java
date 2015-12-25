/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009-2012 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.profiles.ndnd;

import static org.ndnx.ndn.profiles.ndnd.FaceManager.NDNX;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNNetworkManager;
import org.ndnx.ndn.impl.NDNNetworkManager.RegisteredPrefix;
import org.ndnx.ndn.impl.encoding.BinaryXMLCodec;
import org.ndnx.ndn.impl.encoding.NDNProtocolDTags;
import org.ndnx.ndn.impl.encoding.GenericXMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLCodecFactory;
import org.ndnx.ndn.impl.encoding.XMLDecoder;
import org.ndnx.ndn.impl.encoding.XMLEncodable;
import org.ndnx.ndn.impl.encoding.XMLEncoder;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.content.ContentDecodingException;
import org.ndnx.ndn.io.content.ContentEncodingException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.MalformedContentNameStringException;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;

public class PrefixRegistrationManager extends NDNDaemonHandle {

	public enum ActionType {
		Register ("prefixreg"), SelfRegister("selfreg"), UnRegister("unreg");
		ActionType(String st) { this.st = st; }
		private final String st;
		public String value() { return st; }
	}
	
	// Forwarding flags - refer to doc/technical/Registration.txt for the meaning of these
	public static final int NDN_FORW_ACTIVE = 1;
	public static final int NDN_FORW_CHILD_INHERIT = 2;	// This entry may be used even if there is a longer
														// match available
	public static final int NDN_FORW_ADVERTISE = 4;		// Prefix may be advertised to other nodes	
	public static final int NDN_FORW_LAST = 8;			// Entry should be used last if nothing else worked
	public static final int NDN_FORW_CAPTURE = 16;		// No shorter prefix may be used, overriding
														// child-inherit bits that would otherwise make the
														// shorter entries usable.

	public static final int NDN_FORW_LOCAL = 32;		// Restricts namespace to use by applications on the
														// local machine
	public static final int NDN_FORW_TAP = 64;			// Causes the entry to be used right away - intended
														// for debugging and monitoring purposes.
	public static final int NDN_FORW_CAPTURE_OK = 128;	// Use this with NDN_FORW_CHILD_INHERIT to make it eligible for capture.
	public static final int NDN_FORW_PUBMASK = 	NDN_FORW_ACTIVE |
            									NDN_FORW_CHILD_INHERIT |
            									NDN_FORW_ADVERTISE     |
            									NDN_FORW_LAST          |
            									NDN_FORW_CAPTURE       |
            									NDN_FORW_LOCAL         |
            									NDN_FORW_TAP           |
										NDN_FORW_CAPTURE_OK;

	
	public static final Integer DEFAULT_SELF_REG_FLAGS = Integer.valueOf(NDN_FORW_ACTIVE + NDN_FORW_CHILD_INHERIT);

	/*
	 * 	#define NDN_FORW_ACTIVE         1
	 *	#define NDN_FORW_CHILD_INHERIT  2
	 *	#define NDN_FORW_ADVERTISE      4
	 *	#define NDN_FORW_LAST           8
	 */
		
	public static class ForwardingEntry extends GenericXMLEncodable implements XMLEncodable {
		/* extends NDNEncodableObject<PolicyXML>  */
		
		/**
		 * From the XML definitions:
		 * <xs:element name="ForwardingEntry" type="ForwardingEntryType"/>
		 * <xs:complexType name="ForwardingEntryType">
  		 *		<xs:sequence>
      	 *		<xs:element name="Action" type="xs:string" minOccurs="0" maxOccurs="1"/>
      	 * 		<xs:element name="Name" type="NameType" minOccurs="0" maxOccurs="1"/>
      	 * 		<xs:element name="PublisherPublicKeyDigest" type="DigestType" minOccurs="0" maxOccurs="1"/>
         * 		<xs:element name="FaceID" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
       	 * 	 	<xs:element name="ForwardingFlags" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
       	 * 		<xs:element name="FreshnessSeconds" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
      	 * 		</xs:sequence>
      	 * 	</xs:complexType>
		 */

		protected String		_action;
		protected ContentName	_prefixName;
		protected PublisherPublicKeyDigest _ndndId;
		protected Integer		_faceID;
		protected Integer		_flags;
		protected Integer 		_lifetime = Integer.MAX_VALUE;  // in seconds


		public ForwardingEntry(ContentName prefixName, Integer faceID, Integer flags) {
			_action = ActionType.Register.value();
			_prefixName = new ContentName(prefixName); // in case ContentName gets subclassed
			_faceID = faceID;
			_flags = flags;
		}

		public ForwardingEntry(ActionType action, ContentName prefixName, PublisherPublicKeyDigest ndndId, 
								Integer faceID, Integer flags, Integer lifetime) {
			_action = action.value();
			_ndndId = ndndId;
			_prefixName = new ContentName(prefixName); // in case ContentName gets subclassed
			_faceID = faceID;
			_flags = flags;
			_lifetime = lifetime;
		}

		public ForwardingEntry(byte[] raw) {
			ByteArrayInputStream bais = new ByteArrayInputStream(raw);
			XMLDecoder decoder = XMLCodecFactory.getDecoder(BinaryXMLCodec.CODEC_NAME);
			try {
				decoder.beginDecoding(bais);
				decode(decoder);
				decoder.endDecoding();	
			} catch (ContentDecodingException e) {
				String reason = e.getMessage();
				Log.warning(Log.FAC_NETMANAGER, "Unexpected error decoding ForwardingEntry from bytes.  reason: " + reason + "\n");
				Log.warningStackTrace(e);
				throw new IllegalArgumentException("Unexpected error decoding ForwardingEntry from bytes.  reason: " + reason);
			}
		}
		
		public ForwardingEntry() {
		}

		public ContentName getPrefixName() { return _prefixName; }
		
		public Integer getFaceID() { return _faceID; }
		public void setFaceID(Integer faceID) { _faceID = faceID; }

		public String action() { return _action; }
		
		public PublisherPublicKeyDigest getndndId() { return _ndndId; }
		public void setndndId(PublisherPublicKeyDigest id) { _ndndId = id; }
		
		/**
		 * 
		 * @return lifetime of registration in seconds
		 */
		public Integer getLifetime() { return Integer.valueOf(_lifetime.intValue()); }
		

		public String toFormattedString() {
			StringBuilder out = new StringBuilder(256);
			if (null != _action) {
				out.append("Action: "+ _action + "\n");
			} else {
				out.append("Action: not present\n");
			}
			if (null != _faceID) {
				out.append("FaceID: "+ _faceID.toString() + "\n");
			} else {
				out.append("FaceID: not present\n");
			}
			if (null != _prefixName) {
				out.append("Prefix Name: "+ _prefixName + "\n");
			} else {
				out.append("Prefix Name: not present\n");
			}
			if (null != _flags) {
				out.append("Flags: "+ _flags.toString() + "\n");
			} else {
				out.append("Flags: not present\n");
			}
			if (null != _lifetime) {
				out.append("Lifetime: "+ _lifetime.toString() + "\n");
			} else {
				out.append("Lifetime: not present\n");
			}
			return out.toString();
		}	

		public boolean validateAction(String action) {
			if (action != null && action.length() != 0){
				if (action.equalsIgnoreCase(ActionType.Register.value()) ||
						action.equalsIgnoreCase(ActionType.SelfRegister.value()) ||
						action.equalsIgnoreCase(ActionType.UnRegister.value())) {
					return true;
				}
				return false;
			}
			return true; 	// Responses don't have actions
		}
		/**
		 * Used by NetworkObject to decode the object from a network stream.
		 * @see org.ndnx.ndn.impl.encoding.XMLEncodable
		 */
		public void decode(XMLDecoder decoder) throws ContentDecodingException {
			decoder.readStartElement(getElementLabel());
			if (decoder.peekStartElement(NDNProtocolDTags.Action)) {
				_action = decoder.readUTF8Element(NDNProtocolDTags.Action); 
			}
			if (decoder.peekStartElement(NDNProtocolDTags.Name)) {
				_prefixName = new ContentName();
				_prefixName.decode(decoder) ;
			}
			if (decoder.peekStartElement(NDNProtocolDTags.PublisherPublicKeyDigest)) {
				_ndndId = new PublisherPublicKeyDigest();
				_ndndId.decode(decoder);
			}
			if (decoder.peekStartElement(NDNProtocolDTags.FaceID)) {
				_faceID = decoder.readIntegerElement(NDNProtocolDTags.FaceID); 
			}
			if (decoder.peekStartElement(NDNProtocolDTags.ForwardingFlags)) {
				_flags = decoder.readIntegerElement(NDNProtocolDTags.ForwardingFlags); 
			}
			if (decoder.peekStartElement(NDNProtocolDTags.FreshnessSeconds)) {
				_lifetime = decoder.readIntegerElement(NDNProtocolDTags.FreshnessSeconds); 
			}
			decoder.readEndElement();
		}

		/**
		 * Used by NetworkObject to encode the object to a network stream.
		 * @see org.ndnx.ndn.impl.encoding.XMLEncodable
		 */
		public void encode(XMLEncoder encoder) throws ContentEncodingException {
			if (!validate()) {
				throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
			}
			encoder.writeStartElement(getElementLabel());
			if (null != _action && _action.length() != 0)
				encoder.writeElement(NDNProtocolDTags.Action, _action);	
			if (null != _prefixName) {
				_prefixName.encode(encoder);
			}
			if (null != _ndndId) {
				_ndndId.encode(encoder);
			}
			if (null != _faceID) {
				encoder.writeElement(NDNProtocolDTags.FaceID, _faceID);
			}
			if (null != _flags) {
				encoder.writeElement(NDNProtocolDTags.ForwardingFlags, _flags);
			}
			if (null != _lifetime) {
				encoder.writeElement(NDNProtocolDTags.FreshnessSeconds, _lifetime);
			}
			encoder.writeEndElement();   			
		}

		@Override
		public long getElementLabel() { return NDNProtocolDTags.ForwardingEntry; }

		@Override
		public boolean validate() {
			if (validateAction(_action)){
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_action == null) ? 0 : _action.hashCode());
			result = prime * result + ((_prefixName == null) ? 0 : _prefixName.hashCode());
			result = prime * result + ((_ndndId == null) ? 0 : _ndndId.hashCode());
			result = prime * result + ((_faceID == null) ? 0 : _faceID.hashCode());
			result = prime * result + ((_flags == null) ? 0 : _flags.hashCode());
			result = prime * result + ((_lifetime == null) ? 0 : _lifetime.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ForwardingEntry other = (ForwardingEntry) obj;
			if (_action == null) {
				if (other._action != null) return false;
			} else if (!_action.equalsIgnoreCase(other._action)) return false;
			if (_prefixName == null) {
				if (other._prefixName != null) return false;
			} else if (!_prefixName.equals(other._prefixName)) return false;
			if (_ndndId == null) {
				if (other._ndndId != null) return false;
			} else if (!_ndndId.equals(other._ndndId)) return false;
			if (_faceID == null) {
				if (other._faceID != null) return false;
			} else if (!_faceID.equals(other._faceID)) return false;
			if (_flags == null) {
				if (other._flags != null) return false;
			} else if (!_flags.equals(other._flags)) return false;
			if (_lifetime == null) {
				if (other._lifetime != null) return false;
			} else if (!_lifetime.equals(other._lifetime)) return false;
			return true;
		}

	} /* ForwardingEntry */

	/*************************************************************************************/
	/*************************************************************************************/

	public PrefixRegistrationManager(NDNHandle handle) throws NDNDaemonException {
		super(handle);
	}

	public PrefixRegistrationManager(NDNNetworkManager networkManager) throws NDNDaemonException {
		super(networkManager);
	}

	public PrefixRegistrationManager() {
	}
	
	public void registerPrefix(ContentName prefix, Integer faceID, Integer flags) throws NDNDaemonException {
		this.registerPrefix(prefix, null, faceID, flags, Integer.MAX_VALUE);
	}

	public void registerPrefix(String uri, Integer faceID, Integer flags) throws NDNDaemonException {
		this.registerPrefix(uri, null, faceID, flags, Integer.MAX_VALUE);
	}
	
	public void registerPrefix(String uri, PublisherPublicKeyDigest publisher, Integer faceID, Integer flags, 
			Integer lifetime) throws NDNDaemonException {
		try {
			this.registerPrefix(ContentName.fromURI(uri), null, faceID, flags, Integer.MAX_VALUE);
		} catch (MalformedContentNameStringException e) {
			String reason = e.getMessage();
			String msg = ("MalformedContentName (" + uri + ") , reason: " + reason);
			Log.warning(Log.FAC_NETMANAGER, msg);
			Log.warningStackTrace(e);
			throw new NDNDaemonException(msg);
		}
	}
	
	public void registerPrefix(ContentName prefixToRegister, PublisherPublicKeyDigest publisher, Integer faceID, Integer flags, 
							Integer lifetime) throws NDNDaemonException {
		if (null == publisher) {
			try {
				publisher = _manager.getNDNDId();
			} catch (IOException e1) {
				Log.warning(Log.FAC_NETMANAGER, "Unable to get ndnd id");
				Log.warningStackTrace(e1);
				throw new NDNDaemonException(e1.getMessage());
			}
		}
		
		ForwardingEntry forward = new ForwardingEntry(ActionType.Register, prefixToRegister, publisher, faceID, flags, lifetime);
		// byte[] entryBits = super.getBinaryEncoding(forward);

		/*
		 * First create a name that looks like 'ndn:/ndnx/NDNDId/action/ContentObjectWithForwardInIt'
		 */
		ContentName interestName = null;
		try {
			interestName = new ContentName(NDNX, _manager.getNDNDId().digest(), ActionType.Register.value());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NDNDaemonException(e.getMessage());
		}
		super.sendIt(interestName, forward, null, true);
	}

	
	public ForwardingEntry selfRegisterPrefix(String uri) throws NDNDaemonException {
		ContentName prefixToRegister;
		try {
			prefixToRegister = ContentName.fromURI(uri);
		} catch (MalformedContentNameStringException e) {
			String reason = e.getMessage();
			String msg = ("MalformedContentNameStringException for prefix to register (" + uri + ") , reason: " + reason);
			Log.warning(Log.FAC_NETMANAGER, msg);
			Log.warningStackTrace(e);
			throw new NDNDaemonException(msg);
		}
		return selfRegisterPrefix(prefixToRegister, null, DEFAULT_SELF_REG_FLAGS, Integer.MAX_VALUE);
	}
	
	public ForwardingEntry selfRegisterPrefix(ContentName prefixToRegister) throws NDNDaemonException {
		return selfRegisterPrefix(prefixToRegister, null, DEFAULT_SELF_REG_FLAGS, Integer.MAX_VALUE);
	}
	
	public ForwardingEntry selfRegisterPrefix(ContentName prefixToRegister, Integer faceID) throws NDNDaemonException {
		return selfRegisterPrefix(prefixToRegister, faceID, DEFAULT_SELF_REG_FLAGS, Integer.MAX_VALUE);
	}
	
	public ForwardingEntry selfRegisterPrefix(ContentName prefixToRegister, Integer faceID, Integer flags) throws NDNDaemonException {
		return selfRegisterPrefix(prefixToRegister, faceID, flags, Integer.MAX_VALUE);
	}
	
	public ForwardingEntry selfRegisterPrefix(ContentName prefixToRegister, Integer faceID, Integer flags, Integer lifetime) throws NDNDaemonException {
		PublisherPublicKeyDigest ndndId;
		try {
			ndndId = _manager.getNDNDId();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new NDNDaemonException(e1.getMessage());
		}
		ContentName interestName;
		interestName = new ContentName(NDNX, ndndId.digest(), ActionType.SelfRegister.value());
		ForwardingEntry forward = new ForwardingEntry(ActionType.SelfRegister, prefixToRegister, ndndId, faceID, flags, lifetime);

		byte[] payloadBack = super.sendIt(interestName, forward, null, true);
		ForwardingEntry entryBack = new ForwardingEntry(payloadBack);
		Log.fine(Log.FAC_NETMANAGER, "registerPrefix: returned {0}", entryBack);
		return entryBack; 
	}
	
	public void unRegisterPrefix(ContentName prefixName, Integer faceID) throws NDNDaemonException {
		unRegisterPrefix(prefixName, null, faceID);
	}
	
	/**
	 * Unregister a prefix with ndnd
	 * 
	 * @param prefixName ContentName of prefix
	 * @param prefix has callback for completion
	 * @param faceID faceId that has the prefix registered
	 * @throws NDNDaemonException
	 */
	public void unRegisterPrefix(ContentName prefixName, RegisteredPrefix prefix, Integer faceID) throws NDNDaemonException {
		PublisherPublicKeyDigest ndndId;
		try {
			ndndId = _manager.getNDNDId();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new NDNDaemonException(e1.getMessage());
		}
		ContentName interestName = new ContentName(NDNX, ndndId.digest(), ActionType.UnRegister.value());
		ForwardingEntry forward = new ForwardingEntry(ActionType.UnRegister, prefixName, ndndId, faceID, null, null);

		super.sendIt(interestName, forward, prefix, false);
	}
}
