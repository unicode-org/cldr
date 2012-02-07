package org.unicode.cldr.icu;

/**
 * This Class is the Java representation of the ICU4C structure UDataInfo which can 
 * be found in <I>$icu4c_root</I>/source/common/unicode/udata.h
 * 
 * <p>This class is used by LDML2ICUBinaryWriter to store information that must be written
 * in the ICU Binary format.
 * 
 * Note that if this data structure ever grows, the getSize() method must be updated.
 * 
 * @author Brian Rower - June 2008
 *
 */
public class UDataInfo
{
	
	/**
	 * Use to signify that this data is in Big Endian form.
	 * Currently the only mode supported in Java is Big Endian.
	 */
	public static final byte BIGENDIAN = 1;
	
	/**
	 * charsetFamily is equal to this value when the platform is an ASCII based platform.
	 * Currently the only mode supported in Java is ASCII
	 * This mirrors the ICU4C version in <I>$icu4c_root</I>/source/common/unicode/utypes.h
	 */
	public static final byte ASCII_FAMILY = 0;
	
	/**
	 * This is the value for setting sizeofUChar. Currently it is 16 bits (2 bytes).
	 * UChar is currently defined in <I>$icu4c_root</I>/source/common/unicode/umachine.h
	 */
	public static final byte SIZE_OF_UCHAR = 2;
		
	/**
	 * This field stores the size of this data structure in memory. 
	 * Add up the size of each part of it.
	 */
	public short size;
	
	/**
	 * This field is currently unused, set it to zero.
	 */
	public short reservedWord;
	
	/**
	 *   This field is used to signify the Endian mode of a system.
	 *   Choose from the static final int's provided in this class.
	 *   In Java, there is only one possibility: Big Endian.
	 */
	public byte isBigEndian;
	
	/**
	 * This field stores the character set which is being used.
	 */
	public byte charsetFamily;
	
	/**
	 * Size of the UChar structure in C.
	 */
	public byte sizeofUChar;
	
	/**
	 * This field is currently unused, set it to zero.
	 */
	public byte reservedByte;
	
	/**
	 * This field stores an identifier for the data format.
	 * Array should be of length 4. 
	 */
	public byte[] dataFormat;
	
	/**
	 * This field stores the Format version. Array should be of length 4.<br>
	 * [0] = major<br>
	 * [1] = minor<br>
	 * [2] = milli<br>
	 * [3] = micro<br>
	 */
	public byte[] formatVersion;
	
	/**
	 * This field stores the data version. Array should be of length 4.<br>
	 * [0] = major<br>
	 * [1] = minor<br>
	 * [2] = milli<br>
	 * [3] = micro<br>
	 */
	public byte[] dataVersion;
	
	class IncorrectArrayLengthException extends Exception
	{
		IncorrectArrayLengthException(String message)
		{
			super(message);
		}
	}
	
	public UDataInfo(short size, short reservedWord, byte isBigEndian, byte charsetFamily, byte sizeofUChar, 
						byte reservedByte, byte[] dataFormat, byte[] formatVersion, byte[] dataVersion) throws IncorrectArrayLengthException
	{
		if(dataFormat.length != 4)
		{
			throw new IncorrectArrayLengthException("The byte array 'dataFormat' must be of length 4.");
		}
		if(formatVersion.length != 4)
		{
			throw new IncorrectArrayLengthException("The byte array 'formatVersion' must be of length 4.");
		}
		if(dataVersion.length != 4)
		{
			throw new IncorrectArrayLengthException("The byte array 'dataVersion' must be of length 4.");
		}
		this.size = size;
		this.reservedWord = reservedWord;
		this.isBigEndian = isBigEndian;
		this.charsetFamily = charsetFamily;
		this.sizeofUChar = sizeofUChar;
		this.reservedByte = reservedByte;
		this.dataFormat = dataFormat;
		this.formatVersion = formatVersion;
		this.dataVersion = dataVersion;
	}
	
	/**
	 * This method returns the size that this structure will occupy when written to binary file.
	 *  byte = 1 byte <Br>
	 *  short = 2 bytes<Br>
	 *  int = 4 bytes<Br>
	 *  long = 8 bytes<Br>
	 *  float = 4 bytes<Br>
	 *  double = 8 bytes<br>
	 *  char = 2 bytes<br>
	 *  
	 *  @return The number of bytes that UDataInfo occupies
	 */
	public static short getSize()
	{
		/*
		 * number of short elements = 2
		 * number of byte elements = 4
		 * number of byte array elements of length 4 = 3
		 * 2*2 + 4*1 + 3*4 = 4 + 4 + 12 = 20 bytes
		 */	
		return 20;
	}
	
	/**
	 * Returns a byte array representing the UDataStructure so that it can be written byte by byte. 
	 * @returns a byte array of the contents of this UDataStructure.
	 */
	public byte[] getByteArray()
	{
		//This size may change, see get size method above.
		byte[] b = new byte[20]; 
		byte[] sizeBytes = shortToBytes(size);
		
		//write the size
		b[0] = sizeBytes[0];
		b[1] = sizeBytes[1];
		
		//write the reserved word (a bunch of zeros)
		b[2] = 0;
		b[3] = 0;
		
		//write isBigEndian
		b[4] = isBigEndian;
		
		//write charsetFamily
		b[5] = charsetFamily;
		
		//write sizeofUChar
		b[6] = sizeofUChar;
		
		//write reserved byte (some zeros)
		b[7] = 0;
		
		//write the dataFormat
		b[8] = dataFormat[0];
		b[9] = dataFormat[1];
		b[10] = dataFormat[2];
		b[11] = dataFormat[3];
		
		//write the formatVersion
		b[12] = formatVersion[0];
		b[13] = formatVersion[1];
		b[14] = formatVersion[2];
		b[15] = formatVersion[3];
		
		//write the dataVersion
		b[16] = dataVersion[0];
		b[17] = dataVersion[1];
		b[18] = dataVersion[2];
		b[19] = dataVersion[3];
		 		
		return b;
	}
	
	/**
	 * Takes a 16 bit number and returns a two byte array. 0th element is lower byte, 1st element is upper byte.
	 * Ex: x = 28,000. In binary: 0110 1101 0110 0000. This method will return:
	 * [0] = 0110 0000 or 0x60
	 * [1] = 0110 1101 or 0x6D
	 */
	private static byte[] shortToBytes(short x)
	{
		byte[] b = new byte[2];
		byte mask = (byte)0xFF;
		b[1] = (byte)(x & mask); //bitwise and with the lower byte
		b[0] = (byte)((x >>> 8) & mask); //shift four bits to the right and fill with zeros, and then bitwise and with the lower byte
		return b;
	}
}

