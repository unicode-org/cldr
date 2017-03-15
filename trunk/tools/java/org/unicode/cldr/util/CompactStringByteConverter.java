/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.IOException;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.ICUUncheckedIOException;

public class CompactStringByteConverter extends StringByteConverter {
    static public final boolean DEBUG = false;
    private boolean deltaEncoded;
    private int last;

    public CompactStringByteConverter(boolean deltaEncoded) {
        this.deltaEncoded = deltaEncoded;
    }

    public boolean isDeltaEncoded() {
        return deltaEncoded;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.StringByteConverter#clear()
     */
    public void clear() {
        last = 0x40;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.StringByteConverter#toBytes(char, byte[])
     */
    public int toBytes(char cp, byte[] output, int bytePosition) {
        if (deltaEncoded) {
            // get the delta from the previous
            int delta = cp - last;
            bytePosition = writeInt(delta, output, bytePosition);
            last = cp;
            last = (last & ~0x7F) | 0x40; // position in middle of 128 block
            return bytePosition;
        } else {
            return writeUnsignedInt(cp, output, bytePosition);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.StringByteConverter#toBytes(java.lang.CharSequence, int, byte[])
     */
    public int toBytes(CharSequence source, byte[] output, int bytePosition) {
        if (deltaEncoded) {
            int last = 0x40;
            for (int i = 0; i < source.length(); ++i) {
                int cp = source.charAt(i);
                // get the delta from the previous
                int delta = cp - last;
                bytePosition = writeInt(delta, output, bytePosition);
                last = cp;
                last = (last & ~0x7F) | 0x40; // position in middle of 128 block
            }
        } else {
            for (int i = 0; i < source.length(); ++i) {
                int cp = source.charAt(i);
                bytePosition = writeUnsignedInt(cp, output, bytePosition);
            }
        }
        return bytePosition;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.StringByteConverter#fromBytes(byte[], int, int, java.lang.Appendable)
     */
    public Appendable fromBytes(byte[] input, int byteStart, int byteLength, Appendable result) {
        try {
            int[] ioBytePosition = new int[1];
            ioBytePosition[0] = 0;
            if (deltaEncoded) {
                int last = 0x40;
                while (ioBytePosition[0] < byteLength) {
                    int delta = readInt(input, ioBytePosition);
                    last += delta;
                    if (DEBUG) {
                        System.out.println("\t\t" + Utility.hex(last));
                    }
                    result.append((char) last);
                    last = (last & ~0x7F) | 0x40; // position in middle of 128 block
                }
            } else {
                while (ioBytePosition[0] < byteLength) {
                    int last = readUnsignedInt(input, ioBytePosition);
                    if (DEBUG) {
                        System.out.println("\t\t" + Utility.hex(last));
                    }
                    result.append((char) last);
                }
            }
            return result;
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal error", e);
        }
    }

    public static int writeInt(int source, byte[] output, int bytePosition) {
        // grab sign bit
        int sign = 0;
        if (source < 0) {
            sign = 0x40;
            source = ~source;
        }
        // now output into bytes, 7 bits at a time
        // stop when we only have 6 bits left
        if (DEBUG) {
            System.out.println(Utility.hex(source));
        }
        // find the first non-zero byte. We have 6 bits in the bottom byte, otherwise seven per
        // so that leave 32 - 6 + 3*7 = 5
        int mask = ~0x3F;
        int offset = -1;
        while ((source & mask) != 0) {
            offset += 7;
            mask <<= 7;
        }
        for (; offset > 0; offset -= 7) {
            output[bytePosition++] = (byte) ((source >> offset) & 0x7F);
            if (DEBUG) {
                System.out.println(Utility.hex(output[bytePosition - 1] & 0xFF, 2));
            }
        }
        // last byte is signed, with real sign in bit 6
        output[bytePosition++] = (byte) (0x80 | sign | (source & 0x3F));
        if (DEBUG) {
            System.out.println(Utility.hex(output[bytePosition - 1] & 0xFF, 2));
        }
        return bytePosition;
    }

    public static int readInt(byte[] input, int[] ioBytePosition) {
        int result = 0;
        int bytePosition = ioBytePosition[0];
        while (true) {
            // add byte
            int nextByte = input[bytePosition++];
            if (nextByte >= 0) {
                result <<= 7;
                result |= nextByte;
                if (DEBUG) {
                    System.out.println(Utility.hex(nextByte & 0xFF, 2) + ", " + Utility.hex(result));
                }
            } else { // < 0
                result <<= 6;
                result |= nextByte & 0x3F;
                if ((nextByte & 0x40) != 0) {
                    result = ~result;
                }
                if (DEBUG) {
                    System.out.println(Utility.hex(nextByte & 0xFF, 2) + ", " + Utility.hex(result));
                }
                ioBytePosition[0] = bytePosition;
                return result;
            }
        }
    }

    public static int writeUnsignedInt(int source, byte[] output, int bytePosition) {
        if (DEBUG) {
            System.out.println(Utility.hex(source));
        }
        // find the first non-zero byte. We have 6 bits in the bottom byte, otherwise seven per
        // so that leave 32 - 6 + 3*7 = 5
        int mask = ~0x7F;
        int offset = 0;
        while ((source & mask) != 0) {
            offset += 7;
            mask <<= 7;
        }
        for (; offset > 0; offset -= 7) {
            output[bytePosition++] = (byte) ((source >> offset) & 0x7F);
            if (DEBUG) {
                System.out.println(Utility.hex(output[bytePosition - 1] & 0xFF, 2));
            }
        }
        output[bytePosition++] = (byte) (0x80 | source);
        if (DEBUG) {
            System.out.println(Utility.hex(output[bytePosition - 1] & 0xFF, 2));
        }
        return bytePosition;
    }

    public static int readUnsignedInt(byte[] input, int[] ioBytePosition) {
        int result = 0;
        int bytePosition = ioBytePosition[0];
        while (true) {
            // add byte
            int nextByte = input[bytePosition++];
            if (nextByte >= 0) {
                result <<= 7;
                result |= nextByte;
                if (DEBUG) {
                    System.out.println(Utility.hex(nextByte & 0xFF, 2) + ", " + Utility.hex(result));
                }
            } else { // < 0
                result <<= 7;
                result |= nextByte & 0x7F;
                if (DEBUG) {
                    System.out.println(Utility.hex(nextByte & 0xFF, 2) + ", " + Utility.hex(result));
                }
                ioBytePosition[0] = bytePosition;
                return result;
            }
        }
    }

    @Override
    public int getMaxBytesPerChar() {
        return 4;
    }
}