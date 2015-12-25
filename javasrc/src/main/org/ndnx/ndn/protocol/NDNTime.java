/*
 * Part of the NDNx Java Library.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2008, 2009, 2011 Palo Alto Research Center, Inc.
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

package org.ndnx.ndn.protocol;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ndnx.ndn.impl.support.DataUtils;
import org.ndnx.ndn.profiles.VersioningProfile;



/**
 * NDN has a time representation for versions and signing times that
 * is quantized at a granularity of 2^-12 seconds (approximately 1/4 msec).
 * Because it does not quantize time evenly on millisecond boundaries,
 * this can lead to confusion -- t = System.currentTimeMillis(), when
 * used to set a version v, for example, then ends up with v != t as
 * v is quantized and t isn't. Simply providing quantization utility
 * routines to help developers turned out to be error-prone. So we
 * are moving all uses of time in NDN to pre-quantized NDNTime objects,
 * which encapsulates NDN time quantization (which could change),
 * and gives developers ways to deal with it without having to think
 * about it.
 * 
 * NDNTime, all though it implements methods like getNanos, only
 * represents time with a granularity equal to the underlying
 * NDN wire format -- i.e. ~.25 msec.
 */
public class NDNTime extends Timestamp implements ContentName.ComponentProvider {

	private static final long serialVersionUID = -1537142893653443100L;
	
	private byte [] _binarytime = null;
	
	/**
	 * This is the highest nanos value that doesn't quantize to over the ns limit for Timestamp of 999999999.
	 */
	public static final int NANOS_MAX = 999877929;

	/**
	 * Create a NDNTime.
	 * @param msec time in msec
	 */
	public NDNTime(long msec) {
		this((msec/1000) * 1000, (msec % 1000) * 1000000L);
	}

	/**
	 * Create a NDNTime
	 * @param timestamp source timestamp to initialize from, some precision will be lost
	 */
	public NDNTime(Timestamp timestamp) {
		this(timestamp.getTime(), timestamp.getNanos());
	}
	
	/**
	 * Create a NDNTime
	 * @param time source Date to initialize from, some precision will be lost
	 * as NDNTime does not round to unitary milliseconds
	 */
	public NDNTime(Date time) {
		this(time.getTime());
	}
	
	/**
	 * Create a NDNTime from its binary encoding
	 * @param binaryTime12 the binary representation of a NDNTime
	 */
	public NDNTime(byte [] binaryTime12) {	
		this(DataUtils.byteArrayToUnsignedLong(binaryTime12), true);
		if ((null == binaryTime12) || (binaryTime12.length == 0)) {
			throw new IllegalArgumentException("Invalid binary time!");
		} else if (binaryTime12.length > 6) {
			throw new IllegalArgumentException("Time unacceptably far in the future, can't decode: " + DataUtils.printHexBytes(binaryTime12));
		}
	}
	
	/**
	 * Creates a NDNTime initialized to the current time.
	 */
	public NDNTime() {
		this(System.currentTimeMillis());
	}
	
	/**
	 * Creates a NDNTime from a specification of msec and nanos, to ease implementing
	 * compatibility with Timestamp and Date. Note that there is redundant data here --
	 * the last 3 significant digits of msec are also represented as the top 3 of nanos.
	 * We take the version in the nanos. This is derived from Java's slightly odd Timestamp handling.
	 * @param msec milliseconds
	 * @param nanos nanoseconds
	 */
	protected NDNTime(long msec, long nanos) {
		this(toBinaryTimeAsLong(msec, nanos), true);
	}
	
	/**
	 * Creates a NDNTime from the internal long representation of the quantized time.
	 * Equivalent to setFromBinaryTimeAsLong.
	 * @param binaryTimeAsLong the time in our internal units
	 * @param unused a marker parameter to separate this from another constructor
	 */
	protected NDNTime(long binaryTimeAsLong, boolean unused) {
		super((binaryTimeAsLong / 4096L) * 1000L);
		super.setNanos((int)(((binaryTimeAsLong % 4096L) * 1000000000L) / 4096L));
	}
	
	/**
	 * Factory method to generate a NDNTime from our internal long time representation.
	 * Make this a static method to avoid confusion; should be little used
	 * @return
	 */
	public static NDNTime fromBinaryTimeAsLong(long binaryTimeAsLong) {
		return new NDNTime(binaryTimeAsLong, true);
	}
	
	/**
	 * Generate the binary representation of a NDNTime
	 * @return the binary representation we use for encoding
	 */
	public byte [] toBinaryTime() {
		if( null == _binarytime ) {
			byte [] b = DataUtils.unsignedLongToByteArray(toBinaryTimeAsLong());
			_binarytime = b;
		}
		return _binarytime;
	}
	
	/**
	 * Generate the internal long representation of a NDNTime, useful for comparisons
	 * and used internally
	 * @return the long representation of this time in our internal units
	 */
	public long toBinaryTimeAsLong() {
		return toBinaryTimeAsLong(getTime(), getNanos());
	}
	
	/**
	 * Static method to convert from milliseconds and nanoseconds to our
	 * internal long representation.
	 * Assumes that nanos also contains the integral milliseconds for this
	 * time. Ignores msec component in msec.
	 * @param msec milliseconds
	 * @param nanos nanoseconds
	 * @return
	 */
	public static long toBinaryTimeAsLong(long msec, long nanos) {
		long timeVal = (msec / 1000) * 4096L + (nanos * 4096L + 500000000L) / 1000000000L;
		return timeVal;		
	}
	
	protected void setFromBinaryTimeAsLong(long binaryTimeAsLong) {
		_binarytime = null;
		super.setTime((binaryTimeAsLong / 4096L) * 1000L);
		super.setNanos((int)(((binaryTimeAsLong % 4096L) * 1000000000L) / 4096L));
	}
	
	@Override
	public void setTime(long msec) {
		_binarytime = null;
		long binaryTimeAsLong = toBinaryTimeAsLong((msec/1000) * 1000, (msec % 1000) * 1000000L);
		super.setTime((binaryTimeAsLong / 4096L) * 1000L);
		super.setNanos((int)(((binaryTimeAsLong % 4096L) * 1000000000L) / 4096L));
	}

	@Override
	public void setNanos(int nanos) {
		_binarytime = null;
		int quantizedNanos = (int)(((((nanos * 4096L + 500000000L) / 1000000000L)) * 1000000000L) / 4096L);
		if ((quantizedNanos < 0) || (quantizedNanos > 999999999)) {
			System.out.println("Quantizing nanos " + nanos + " resulted in out of range value " + quantizedNanos + "!");
		}
	   	super.setNanos(quantizedNanos);
	}
	
	/**
	 * Note: you have to use a relatively high value of nanos before you get across a quantization
	 * unit and have an impact. Our units are 2^-12 seconds, or ~250 msec. So 250000 nanos.
	 * @param nanos
	 */
	public void addNanos(int nanos) {
		_binarytime = null;
		setNanos(nanos + getNanos());
	}
	
	/**
	 * A helper method to increment to avoid collisions. Add a number
	 * of NDNx quantized time units to the time. Synchronize if modifications can
	 * be performed from multiple threads.
	 */
	public void increment(int timeUnits) {
		_binarytime = null;
		long binaryTimeAsLong = toBinaryTimeAsLong();
		binaryTimeAsLong += timeUnits;
		setFromBinaryTimeAsLong(binaryTimeAsLong);
	}
	
	/**
	 * We handle all comparison functions by quantizing the thing
	 * we are being compared to, then comparing. The only thing
	 * this won't catch is if a normal Timestamp or Date's comparison
	 * method is called with a NDNTime as an argument. This is a 
	 * small risk, and worth it given the convenience of subclassing
	 * Timestamp and using it for most functionality.
	 */
	@Override
	public boolean equals(Timestamp ts) {
		return super.equals(new NDNTime(ts));
	}

	@Override
	public int compareTo(Date o) {
		return super.compareTo(new NDNTime(o));
	}

	@Override
	public int compareTo(Timestamp ts) {
		return super.compareTo(new NDNTime(ts));
	}

	@Override
	public boolean before(Timestamp ts) {
		return super.before(new NDNTime(ts));
	}

	@Override
	public boolean after(Timestamp ts) {
		return super.after(new NDNTime(ts));
	}

	@Override
	public boolean before(Date when) {
		return super.before(new NDNTime(when));
	}
	
	@Override
	public boolean after(Date when) {
		return super.after(new NDNTime(when));
	}

	/**
	 * Create a NDNTime initialized to the current date/time.
	 * @return the new NDNTime
	 */
	public static NDNTime now() {
		return new NDNTime();
	}
	
	/**
	 * Generate a string representation of a NDNTime containing only date and HH:MM:SS,
	 * not milliseconds or nanoseconds.
	 */
	public String toShortString() {
		// use . instead of : as URI printer will make it look nicer in the logs
		SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd-HH.mm.ss");
		return df.format(this);
	}

	/**
	 * Convert the time into a version component, so NDNTime objects can be used
	 * directly in a ContentName builder.
	 */
	public final byte[] getComponent() {
		return VersioningProfile.timeToVersionComponent(this);
	}
}
