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

package org.ndnx.ndn.io.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.ndnx.ndn.NDNHandle;
import org.ndnx.ndn.impl.NDNFlowControl;
import org.ndnx.ndn.impl.NDNFlowControl.SaveType;
import org.ndnx.ndn.impl.support.Log;
import org.ndnx.ndn.io.ErrorStateException;
import org.ndnx.ndn.protocol.ContentName;
import org.ndnx.ndn.protocol.ContentObject;
import org.ndnx.ndn.protocol.KeyLocator;
import org.ndnx.ndn.protocol.PublisherPublicKeyDigest;
import org.ndnx.ndn.protocol.SignedInfo.ContentType;


/**
 * A NDNNetworkObject subclass specialized for reading and writing X509Certificates.
 * X509Certificates are Serializable. So we could use a subclass of NDNSerializableObject
 * to serialize them to NDN. But, we want to control their on-the-wire data format --
 * using their serialization interface, the output will contain metadata only
 * readable via the Java serialization interface. We want to write raw encoded
 * certificates. So have to override the serialization behavior.
 * 
 * While traditional X.509 certificates are somewhat odd in a NDNx context, sometimes
 * we need to interface with protocols that will only accept those as a credential
 * format. Think of them as a particular way of packaging keys.
 * 
 * This class also serves as an example of how to write a NDNNetworkObject
 * subclass that needs to implement its own serialization.
 */
public class X509CertificateObject extends NDNNetworkObject<X509Certificate> {

	/**
	 * Write constructor.
	 * @param name
	 * @param data
	 * @param handle
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, X509Certificate data, SaveType saveType, NDNHandle handle) throws IOException {
		super(X509Certificate.class, false, name, data, saveType, handle);
	}
	
	/**
	 * Write constructor.
	 * @param name
	 * @param data
	 * @param publisher
	 * @param locator
	 * @param handle
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, X509Certificate data, SaveType saveType,
							PublisherPublicKeyDigest publisher, 
							KeyLocator locator, NDNHandle handle) throws IOException {
		super(X509Certificate.class, false, name, data, saveType, publisher, locator, handle);
	}

	/**
	 * Read constructor.
	 * @param name
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(X509Certificate.class, false, name, (PublisherPublicKeyDigest)null, handle);
	}
	
	/**
	 * Read constructor.
	 * @param name
	 * @param publisher
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, PublisherPublicKeyDigest publisher, 
							NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(X509Certificate.class, false, name, publisher, handle);
	}
	
	/**
	 * Read constructor if you already have a block.
	 * @param firstBlock
	 * @param handle
	 * @throws ContentDecodingException
	 * @throws IOException
	 */
	public X509CertificateObject(ContentObject firstBlock, NDNHandle handle) 
			throws ContentDecodingException, IOException {
		super(X509Certificate.class, false, firstBlock, handle);
	}
	
	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, X509Certificate data, 
			PublisherPublicKeyDigest publisher, 
			KeyLocator locator,
			NDNFlowControl flowControl) throws IOException {
		super(X509Certificate.class, false, name, data, publisher, locator, flowControl);
	}
		
	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public X509CertificateObject(ContentName name, PublisherPublicKeyDigest publisher,
						   NDNFlowControl flowControl) throws ContentDecodingException, IOException {
		super(X509Certificate.class, false, name, publisher, flowControl);
	}

	/**
	 * Internal constructor used by low-level network operations. Don't use unless you know what 
	 * you are doing.
	 * @param name name under which to save data
	 * @param data data to save when save() is called; or null if the next call will be updateInBackground()
	 * @param publisher key (identity) to use to sign the content (null for default)
	 * @param locator key locator to use to tell people where to find our key, should match publisher, (null for default for key)
	 * @param flowControl flow controller to use for network output
	 * @throws IOException
	 */
	public X509CertificateObject(ContentObject firstSegment, NDNFlowControl flowControl) 
					throws ContentDecodingException, IOException {
		super(X509Certificate.class, false, firstSegment, flowControl);
	}

	/**
	 * Copy constructor.
	 */
	public X509CertificateObject(NDNNetworkObject<? extends X509Certificate> other) {
		super(X509Certificate.class, other);
	}

				
	@Override
	public ContentType contentType() { return ContentType.DATA; }

	public X509Certificate certificate() throws ContentNotReadyException, ContentGoneException, ErrorStateException { return data(); }
	
	public PublisherPublicKeyDigest publicKeyDigest() throws ContentNotReadyException, ContentGoneException, ErrorStateException {
		X509Certificate cert = certificate();
		if (null != cert) {
			return new PublisherPublicKeyDigest(cert.getPublicKey());
		}
		return null; // don't expect to get here.
	}

	@Override
	protected X509Certificate readObjectImpl(InputStream input) throws ContentDecodingException, IOException {
		// assume we read until we have all the bytes, then decode.
		// Doesn't give us a good opportunity to check whether it's of type KEY. TODO
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			 X509Certificate certificate = (X509Certificate)cf.generateCertificate(input);
			return certificate;
		} catch (CertificateEncodingException e) {
			Log.warning("Cannot decode certificate " + e.getClass().getName() + ": " + e.getMessage());
			throw new IOException("Cannot decode certificate " + e.getClass().getName() + ": " + e.getMessage());
		} catch (CertificateException e) {
			Log.warning("Cannot decode certificate " + e.getClass().getName() + ": " + e.getMessage());
			throw new IOException("Cannot decode certificate " + e.getClass().getName() + ": " + e.getMessage());
		} 
	}

	@Override
	protected void writeObjectImpl(OutputStream output) throws ContentEncodingException, IOException {
		if (null == data())
			throw new ContentNotReadyException("No content available to save for object " + getBaseName());
		byte[] encoded;
		try {
			encoded = certificate().getEncoded();
		} catch (CertificateEncodingException e) {
			throw new ContentEncodingException("Cannot encode certificate: " + e.getMessage(), e);
		}
		output.write(encoded);
	}
	
	/**
	 * Many cryptographic providers don't implement equals() correctly.
	 * @throws ContentGoneException 
	 * @throws ContentNotReadyException 
	 * @throws ErrorStateException 
	 * @throws CertificateEncodingException 
	 */
	public boolean equalsCertificate(X509Certificate otherCertificate) throws ContentNotReadyException, ContentGoneException, ErrorStateException, CertificateEncodingException {
		if (!available())
			throw new ContentNotReadyException("No data available to compare!");
		if (certificate().equals(otherCertificate))
			return true;
		// might be that the provider doesn't implement equals()
		return Arrays.equals(certificate().getEncoded(), otherCertificate.getEncoded());
	}
	
	public boolean equalsCertificate(X509CertificateObject otherCertificateObject) throws ContentNotReadyException, ContentGoneException, ErrorStateException, CertificateEncodingException {
		return this.equalsCertificate(otherCertificateObject.certificate());
	}
}
