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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNNetworkManager.NetworkProtocol;
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
import org.ndnx.ndn.protocol.Component;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;

/**
 *
 */
public class FaceManager extends NDNDaemonHandle /* extends GenericXMLEncodable implements XMLEncodable */ {

	
	public enum ActionType {
		NewFace ("newface"), DestroyFace ("destroyface"), QueryFace ("queryface");
		ActionType(String st) { this.st = st; }
		private final String st;
		public String value() { return st; }
	}

	public static final Component NDNX = new Component("ndnx");
	
public static class FaceInstance extends GenericXMLEncodable implements XMLEncodable {
	/* extends NDNEncodableObject<PolicyXML>  */
	
	/**
	 * From the XML definitions:
	 * <xs:element name="Action" type="xs:string" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="PublisherPublicKeyDigest" type="DigestType" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="FaceID" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="IPProto" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="Host" type="xs:string" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="Port" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="MulticastInterface" type="xs:string" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="MulticastTTL" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
	 * <xs:element name="FreshnessSeconds" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
	 */

	protected String		_action;
	protected PublisherPublicKeyDigest _ndndID;
	protected Integer		_faceID;
	protected NetworkProtocol		_ipProto;
	protected String		_host;
	protected Integer		_port;
	protected String		_multicastInterface;
	protected Integer		_multicastTTL;
	protected Integer 		_lifetime;


	public FaceInstance(ActionType action, PublisherPublicKeyDigest ndndID, NetworkProtocol ipProto, String host, Integer port) {
		_action = action.value();
		_ndndID = ndndID;
		_ipProto = ipProto;
		_host = host;
		_port = port;
	}

	public FaceInstance(ActionType action, PublisherPublicKeyDigest ndndID, Integer faceID) {
		_action = action.value();
		_ndndID = ndndID;
		_faceID = faceID;
	}

	public FaceInstance(ActionType action, PublisherPublicKeyDigest ndndID, NetworkProtocol ipProto, String host, Integer port,
			String multicastInterface, Integer multicastTTL, Integer lifetime) {
		_action = action.value();
		_ndndID = ndndID;
		_ipProto = ipProto;
		_host = host;
		_port = port;
		_multicastInterface = multicastInterface;
		_multicastTTL = multicastTTL;
		_lifetime = lifetime;
	}

	public FaceInstance(byte[] raw) {
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		XMLDecoder decoder = XMLCodecFactory.getDecoder(BinaryXMLCodec.CODEC_NAME);
		try {
			decoder.beginDecoding(bais);
			decode(decoder);
			decoder.endDecoding();	
		} catch (ContentDecodingException e) {
			String reason = e.getMessage();
			Log.fine("Unexpected error decoding FaceInstance from bytes.  reason: " + reason + "\n");
			Log.warningStackTrace(e);
			throw new IllegalArgumentException("Unexpected error decoding FaceInstance from bytes.  reason: " + reason);
		}
	}
	
	public FaceInstance() {
	}

	public Integer faceID() { return _faceID; }
	public void setFaceID(Integer faceID) { _faceID = faceID; }

	public String action() { return _action; }

	
	public String toFormattedString() {
		StringBuilder out = new StringBuilder(128);
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
		if (null != _host) {
			out.append("Host: "+ _host + "\n");
		} else {
			out.append("Host: not present\n");
		}
		if (null != _port) {
			out.append("Port: "+ _port.toString() + "\n");
		} else {
			out.append("Port: not present\n");
		}
		return out.toString();
	}	

	public boolean validateAction(String action) {
		if (action != null){
			if (action.equals(ActionType.NewFace.value()) || 
					action.equals(ActionType.DestroyFace.value()) || 
					action.equals(ActionType.QueryFace.value())) {
				return true;
			}
		}
		return false;
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
		if (decoder.peekStartElement(NDNProtocolDTags.PublisherPublicKeyDigest)) {
			_ndndID = new PublisherPublicKeyDigest();
			_ndndID.decode(decoder);
		}
		if (decoder.peekStartElement(NDNProtocolDTags.FaceID)) {
			_faceID = decoder.readIntegerElement(NDNProtocolDTags.FaceID); 
		}
		if (decoder.peekStartElement(NDNProtocolDTags.IPProto)) {
			int pI = decoder.readIntegerElement(NDNProtocolDTags.IPProto);
			_ipProto = null;
			if (NetworkProtocol.TCP.value().intValue() == pI) {
				_ipProto = NetworkProtocol.TCP;
			} else if (NetworkProtocol.UDP.value().intValue() == pI) {
				_ipProto = NetworkProtocol.UDP;
			} else {
				throw new ContentDecodingException("FaceInstance.decoder.  Invalid " + 
						NDNProtocolDTags.tagToString(NDNProtocolDTags.IPProto) + " field: " + pI);
			}
		}
		if (decoder.peekStartElement(NDNProtocolDTags.Host)) {
			_host = decoder.readUTF8Element(NDNProtocolDTags.Host); 
		}
		if (decoder.peekStartElement(NDNProtocolDTags.Port)) {
			 _port = decoder.readIntegerElement(NDNProtocolDTags.Port); 
		}
		if (decoder.peekStartElement(NDNProtocolDTags.MulticastInterface)) {
			_multicastInterface = decoder.readUTF8Element(NDNProtocolDTags.MulticastInterface); 
		}
		if (decoder.peekStartElement(NDNProtocolDTags.MulticastTTL)) {
			_multicastTTL = decoder.readIntegerElement(NDNProtocolDTags.MulticastTTL); 
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
		if (null != _ndndID) {
			_ndndID.encode(encoder);
		}
		if (null != _faceID) {
			encoder.writeElement(NDNProtocolDTags.FaceID, _faceID);
		}
		if (null != _ipProto) {
			encoder.writeElement(NDNProtocolDTags.IPProto, _ipProto.value());
		}
		if (null != _host && _host.length() != 0) {
			encoder.writeElement(NDNProtocolDTags.Host, _host);	
		}
		if (null != _port) {
			encoder.writeElement(NDNProtocolDTags.Port, _port);
		}
		if (null != _multicastInterface && _multicastInterface.length() != 0) {
			encoder.writeElement(NDNProtocolDTags.MulticastInterface, _multicastInterface);
		}
		if (null != _multicastTTL) {
			encoder.writeElement(NDNProtocolDTags.MulticastTTL, _multicastTTL);
		}
		if (null != _lifetime) {
			encoder.writeElement(NDNProtocolDTags.FreshnessSeconds, _lifetime);
		}
		encoder.writeEndElement();   			
	}

	@Override
	public long getElementLabel() { return NDNProtocolDTags.FaceInstance; }

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
		result = prime * result + ((_ndndID == null) ? 0 : _ndndID.hashCode());
		result = prime * result + ((_faceID == null) ? 0 : _faceID.hashCode());
		result = prime * result + ((_ipProto == null) ? 0 : _ipProto.hashCode());
		result = prime * result + ((_host == null) ? 0 : _host.hashCode());
		result = prime * result + ((_port == null) ? 0 : _port.hashCode());
		result = prime * result + ((_multicastInterface == null) ? 0 : _multicastInterface.hashCode());
		result = prime * result + ((_multicastTTL == null) ? 0 : _multicastTTL.hashCode());
		result = prime * result + ((_lifetime == null) ? 0 : _lifetime.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		FaceInstance other = (FaceInstance) obj;
		if (_action == null) {
			if (other._action != null) return false;
		} else if (!_action.equals(other._action)) return false;
		if (_ndndID == null) {
			if (other._ndndID != null) return false;
		} else if (!_ndndID.equals(other._ndndID)) return false;
		if (_faceID == null) {
			if (other._faceID != null) return false;
		} else if (!_faceID.equals(other._faceID)) return false;
		if (_ipProto == null) {
			if (other._ipProto != null) return false;
		} else if (!_ipProto.equals(other._ipProto)) return false;
		if (_host == null) {
			if (other._host != null) return false;
		} else if (!_host.equals(other._host)) return false;
		if (_port == null) {
			if (other._port != null) return false;
		} else if (!_port.equals(other._port)) return false;
		if (_multicastInterface == null) {
			if (other._multicastInterface != null) return false;
		} else if (!_multicastInterface.equals(other._multicastInterface)) return false;
		if (_multicastTTL == null) {
			if (other._multicastTTL != null) return false;
		} else if (!_multicastTTL.equals(other._multicastTTL)) return false;
		if (_lifetime == null) {
			if (other._lifetime != null) return false;
		} else if (!_lifetime.equals(other._lifetime)) return false;
		return true;
	}

} /* FaceInstance */

/*************************************************************************************/
/*************************************************************************************/

	public FaceManager(NDNHandle handle) throws NDNDaemonException {
		super(handle);
	}
	
	public FaceManager() { }
	
	private PublisherPublicKeyDigest getId() throws NDNDaemonException {
		PublisherPublicKeyDigest id = null;
		try {
			id = _manager.getNDNDId();
		} catch (IOException e) {
			throw new NDNDaemonException(e.getMessage());
		}
		return id;
	}
	
	public Integer createFace(NetworkProtocol ipProto, String host, Integer port) 
							throws NDNDaemonException {
		return createFace(ipProto, host, port, null, null, null);
	}
	
	public Integer createFace(NetworkProtocol ipProto, String host, Integer port, Integer lifetime) 
							throws NDNDaemonException {
		return createFace(ipProto, host, port, null, null, lifetime);
	}

	public Integer createFace(NetworkProtocol ipProto, String host, Integer port,
			String multicastInterface, Integer multicastTTL, Integer lifetime) 
							throws NDNDaemonException {
		FaceInstance face = new FaceInstance(ActionType.NewFace, getId(), ipProto, host, port, 
											multicastInterface, multicastTTL, lifetime);
		FaceInstance returned = sendIt(face);
		return returned.faceID();
	}
		
	public void deleteFace(Integer faceID) throws NDNDaemonException {
		FaceInstance face = new FaceInstance(ActionType.DestroyFace, getId(), faceID);
		sendIt(face);
	}
	
	public FaceInstance queryFace(Integer faceID) throws NDNDaemonException {
		FaceInstance face = new FaceInstance(ActionType.QueryFace, getId(), faceID);
		FaceInstance returned = sendIt(face);
		return returned;
	}
	
	private FaceInstance sendIt(FaceInstance face) throws NDNDaemonException {
		ContentName interestName = new ContentName(NDNX, getId().digest(), face.action());

		byte[] payloadBack = super.sendIt(interestName, face, null, true);
		FaceInstance faceBack = new FaceInstance(payloadBack);

		String formattedFace = faceBack.toFormattedString();
		Log.fine(formattedFace);
		return faceBack; 
	} /* private FaceInstance sendIt(FaceInstance face) throws NDNDaemonException */
	

}
