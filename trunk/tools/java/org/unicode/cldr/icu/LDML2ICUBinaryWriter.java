/*
 *******************************************************************************
 * Copyright (C) 2003-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package org.unicode.cldr.icu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterEnums.ECharacterCategory;
import com.ibm.icu.text.UTF16;

/**
 * The LDML2ICUBinaryWriter class is a set of methods which can be used
 * to generate Binary (.res) files in the ICU Binary format.
 *
 * @author Brian Rower - June 2008
 *
 */
public class LDML2ICUBinaryWriter {
    /**
     * This string is the copyright to be written into the file.
     * In the C version, can be found in <I>icu4c_root</I>/source/common/unicode/uversion.h
     */
    private static final String COPYRIGHT = " Copyright (C) 2012, International Business Machines Corporation and others. All Rights Reserved. ";

    public static int written = 0;

    /**
     * Magic numbers!!!!
     */
    private static final byte MAGIC1 = (byte) 0xda;
    private static final byte MAGIC2 = 0x27;

    private static boolean INCLUDE_COPYRIGHT = false;
    /**
     * The number of bytes it takes to write magic number 1.
     */
    private static final short BYTES_TAKEN_BY_MAGIC1 = 1;

    /**
     * The number of bytes it takes to write magic number 2;
     */
    private static final short BYTES_TAKEN_BY_MAGIC2 = 1;

    /**
     * The number of bytes that it takes to write the size of the header.
     */
    private static final short BYTES_TAKEN_BY_HEADER_SIZE = 2;

    /**
     * The charsets to be used when encoding strings.
     */
    public static final String CHARSET8 = "UTF-8";
    public static final String CHARSET16 = "UTF-16BE";

    /**
     * The number of bytes that each character takes up. This is dependant on the encoding (see CHARSET above).
     */
    private static final int BYTES_PER_UTF8_CHAR = 1;

    /**
     * Numeric constants for special elements.
     */
    private static final int SPECIAL_NONE = 0;
    private static final int SPECIAL_COLLATIONS = 1;
    private static final int SPECIAL_COLLATIONELEMENTS = 2;
    private static final int SPECIAL_DEPENDENCY = 3;
    private static final int SPECIAL_TRANSLITERATOR = 4;

    /**
     * Numeric constants for types of resource items.
     *
     * @see ures_getType
     * @stable ICU 2.0
     */

    // **************************** ENUM Below is ported from C. See ures.h ***********************

    /** Resource type constant for "no resource". @stable ICU 2.6 */
    public static final int URES_NONE = -1;

    /** Resource type constant for 16-bit Unicode strings. @stable ICU 2.6 */
    public static final int URES_STRING = 0;

    /** Resource type constant for binary data. @stable ICU 2.6 */
    public static final int URES_BINARY = 1;

    /** Resource type constant for tables of key-value pairs. @stable ICU 2.6 */
    public static final int URES_TABLE = 2;

    /**
     * Resource type constant for aliases;
     * internally stores a string which identifies the actual resource
     * storing the data (can be in a different resource bundle).
     * Resolved internally before delivering the actual resource through the API.
     *
     * @stable ICU 2.6
     */
    public static final int URES_ALIAS = 3;

    /**
     * Internal use only.
     * Alternative resource type constant for tables of key-value pairs.
     * Never returned by ures_getType().
     *
     * @internal
     */
    public static final int URES_TABLE32 = 4;

    /**
     * Resource type constant for a single 28-bit integer, interpreted as
     * signed or unsigned by the ures_getInt() or ures_getUInt() function.
     *
     * @see ures_getInt
     * @see ures_getUInt
     * @stable ICU 2.6
     */
    public static final int URES_INT = 7;

    /** Resource type constant for arrays of resources. @stable ICU 2.6 */
    public static final int URES_ARRAY = 8;

    /**
     * Resource type constant for vectors of 32-bit integers.
     *
     * @see ures_getIntVector
     * @stable ICU 2.6
     */
    public static final int URES_INT_VECTOR = 14;

    public static final int URES_LIMIT = 16;

    /*
     * The enum below is ported from C. See uresdata.h
     *
     * It is used as index references for the array which will be written.
     */
    /* [0] contains URES_INDEX_TOP==the length of indexes[] */
    private static final int URES_INDEX_LENGTH = 0;
    /* [1] contains the top of the strings, same as the bottom of resources, rounded up */
    private static final int URES_INDEX_STRINGS_TOP = 1;
    /* [2] contains the top of all resources */
    private static final int URES_INDEX_RESOURCES_TOP = 2;
    /* [3] contains the top of the bundle, in case it were ever different from [2] */
    private static final int URES_INDEX_BUNDLE_TOP = 3;
    /* [4] max. length of any table */
    private static final int URES_INDEX_MAX_TABLE_LENGTH = 4;
    /* [5] attributes bit set, see URES_ATT_* (new in formatVersion 1.2) */
    // private static final int URES_INDEX_ATTRIBUTES = 5;
    /* This one is the length of the array */
    private static final int URES_INDEX_TOP = 6;

    // must be set if writing transliteration
    private static Hashtable<String, String> ruleStringsHash = null;

    public static void main() {

    }

    /**
     * This method is called upon the top of an ICUResourceWriter.Resource
     * in order to write the whole Resource tree into binary format.
     *
     * @param resTop
     *            The top of the resource tree that you would like written to file. This
     *            object should be a ICUResourceWriter.ResourceTable.
     * @param outDir
     *            A string pointing to the path of the output directory.
     * @param outFile
     *            The name of the output file. If filename has an extension other than .res
     *            (ex: .txt) this method will strip that extention and replace with .res.
     */
    public static void writeBinaryFile(ICUResourceWriter.Resource resTop, String outDir, String outFile) {
        String fileName = "";
        int usedOffset = 0;
        String directoryPath = "";
        FileOutputStream out;
        UDataInfo info;
        byte[] dataFormat;
        byte[] formatVersion;
        byte[] dataVersion;
        byte[] padding;

        // Do some checks on the file name
        // if it has a period in it...get rid of everything after the period
        if (outFile.indexOf('.') > -1) {
            fileName = outFile.substring(0, outFile.indexOf('.'));
            if (fileName.length() == 0) {
                printError(outFile + " is not a valid file name.");
                System.exit(1);
            }
            fileName = fileName + ".res";
        } else {
            fileName = outFile + ".res";
        }
        // add the .res part to the file name

        // do some checks on the directory path
        // replace all backslashes with forward slashes
        directoryPath = outDir.replace('\\', '/');

        // if the path does not end in a slash, then we'll add one
        if (directoryPath.charAt(directoryPath.length() - 1) != '/') {
            directoryPath = directoryPath + "/";
        }

        // create UDataInfo
        // Data format is "ResB"
        dataFormat = new byte[4];
        dataFormat[0] = 0x52; // R
        dataFormat[1] = 0x65; // e
        dataFormat[2] = 0x73; // s
        dataFormat[3] = 0x42; // B

        // Format version is 1.2.0.0
        formatVersion = new byte[4];
        formatVersion[0] = 1;
        formatVersion[1] = 2;
        formatVersion[2] = 0;
        formatVersion[3] = 0;

        // data version is 1.4.0.0
        dataVersion = new byte[4];
        dataVersion[0] = 1;
        dataVersion[1] = 4;
        dataVersion[2] = 0;
        dataVersion[3] = 0;

        // now that the file and directory name are formatted, lets try to create an output stream
        try {
            System.out.println("Creating file: " + directoryPath + fileName);
            File f = new File(directoryPath, fileName);
            out = new FileOutputStream(f);

            info = new UDataInfo(UDataInfo.getSize(), (short) 0, UDataInfo.BIGENDIAN, UDataInfo.ASCII_FAMILY,
                UDataInfo.SIZE_OF_UCHAR, (byte) 0, dataFormat, formatVersion, dataVersion);

            // this method goes through the tree and looks for a table named CollationElements or Collations, and adds
            // the
            // appropriate data to the tree
            dealWithSpecialElements(resTop, outDir);

            // before we do anything with the resources, sort them
            resTop.sort();

            // call writeBinaryHeader.
            writeBinaryHeader(out, info, COPYRIGHT);

            usedOffset = writeKeyString(out, resTop);

            // Call writeBinary on the top of the Resource tree

            usedOffset = resTop.writeBinary(out, usedOffset);
            padding = createPadding(pad32(usedOffset));
            if (padding != null) {
                out.write(padding);
                written += padding.length;
            }
            out.close();
            System.out.println("Finished writing binary.");
        } catch (FileNotFoundException e) {
            printError(directoryPath + fileName + " could not be opened, please ensure the correct path is given.");
            e.printStackTrace();
            System.exit(1);
        } catch (SecurityException e) {
            printError("access denied: " + directoryPath + fileName);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            printError(e.getMessage());
            System.exit(1);
        }
    }

    private static int getSpecialType(ICUResourceWriter.Resource res) {
        if (!res.hasKey) {
            return SPECIAL_NONE;
        }

        if (res.name.equals("CollationElements") && res instanceof ICUResourceWriter.ResourceTable) {
            return SPECIAL_COLLATIONELEMENTS;
        }

        if (res.name.equals("collations") && res instanceof ICUResourceWriter.ResourceTable) {
            return SPECIAL_COLLATIONS;
        }

        if (res.name.equals("depends") && res instanceof ICUResourceWriter.ResourceProcess) {
            return SPECIAL_DEPENDENCY;
        }

        if (res instanceof ICUResourceWriter.ResourceProcess) {
            if (((ICUResourceWriter.ResourceProcess) res).ext.equals(ICUResourceWriter.TRANSLITERATOR)) {
                return SPECIAL_TRANSLITERATOR;
            }
        }

        return SPECIAL_NONE;
    }

    /**
     *
     * Goes through the resource tree recursively and looks for a table named
     * CollationElements, collations, dependency, or transliterator and adds the appropriate data
     *
     * @param top
     *            The top of the Resource Tree
     */
    private static void dealWithSpecialElements(ICUResourceWriter.Resource top, String outDir) {
        // if it's a table
        if (top instanceof ICUResourceWriter.ResourceTable) {
            // loop through all it's elements and check if they're anything specialCollationElements or Collation
            ICUResourceWriter.Resource cur = top.first;
            while (cur != null) {
                switch (getSpecialType(cur)) {
                case SPECIAL_COLLATIONELEMENTS:
                    addCollation(cur);
                    break;
                case SPECIAL_COLLATIONS:
                    addCollationElements(cur);
                    break;
                case SPECIAL_DEPENDENCY:
                    addDependency((ICUResourceWriter.ResourceTable) top, (ICUResourceWriter.ResourceProcess) cur,
                        outDir);
                    break;
                case SPECIAL_TRANSLITERATOR:
                    addTransliteration((ICUResourceWriter.ResourceTable) top, (ICUResourceWriter.ResourceProcess) cur);
                    break;
                case SPECIAL_NONE:
                default:
                    dealWithSpecialElements(cur, outDir);
                }

                cur = cur.next;
            }
        }
        // if it's not a table...don't do anything...
    }

    public static void setRulesHash(Hashtable<String, String> hash) {
        ruleStringsHash = hash;
    }

    // Parallels the C function for parseTransliterator in parse.c of genrb
    private static void addTransliteration(ICUResourceWriter.ResourceTable parent,
        ICUResourceWriter.ResourceProcess trans) {
        if (ruleStringsHash == null) {
            System.err.println("If you are processing transliteration, you must set the Rules Hashtable.");
            System.exit(-1);
        }

        String dataString = ruleStringsHash.get(trans.val);

        if (dataString == null) {
            System.err.println("Could not find data for: " + trans.val);
            System.exit(-1);
        }

        // strip out the unneeded stuff from the buffer (like comments and spaces and line breaks
        dataString = stripRules(dataString);

        // create a string resource containing the data and add it to the resource tree
        // remove the ResourceProcess and add the String

        ICUResourceWriter.ResourceString replacement = new ICUResourceWriter.ResourceString("Resource", dataString);

        ICUResourceWriter.Resource current = parent.first;

        // yes, we're using an address comparison below...because they should both be pointing the the same object when
        // we find it.
        if (current != trans) {
            while (current != null && current.next != trans) {
                current = current.next;
            }
            if (current != null) {
                replacement.next = trans.next;
                current.next = replacement;
            } else {
                System.err.println("An unexpected error has occured: Could not find Transliteration resource.");
                System.exit(-1);
            }
        } else {
            replacement.next = trans.next;
            parent.first = replacement;
        }

    }

    private static boolean isUWhiteSpace(char c) {
        return (c >= 0x0009 && c <= 0x2029 && (c <= 0x000D || c == 0x0020 || c == 0x0085 ||
            c == 0x200E || c == 0x200F || c >= 0x2028));
    }

    private static boolean isNewLine(char c) {
        if (c == 0x000d || c == 0x000a) {
            return true;
        }
        return false;
    }

    private static boolean isPunctuation(char c) {
        int x = UCharacter.getType(c);
        switch (x) {
        case ECharacterCategory.CONNECTOR_PUNCTUATION:
        case ECharacterCategory.DASH_PUNCTUATION:
        case ECharacterCategory.END_PUNCTUATION:
        case ECharacterCategory.FINAL_PUNCTUATION:
        case ECharacterCategory.INITIAL_PUNCTUATION:
        case ECharacterCategory.OTHER_PUNCTUATION:
        case ECharacterCategory.START_PUNCTUATION:
            return true;
        default:
            return false;
        }
    }

    private static boolean isControl(char c) {
        int x = UCharacter.getType(c);
        switch (x) {
        case ECharacterCategory.CONTROL:
            return true;
        default:
            return false;
        }
    }

    // parallels the C++ function utrans_stripRules in rbt_pars.cpp in i18n project
    private static String stripRules(String data) {
        String newData = "";
        int currentIndex = 0;
        char curChar;
        char curChar2 = '0';
        boolean needChar2 = false;
        boolean quoted = false;

        try {

            while (currentIndex < data.length()) {
                needChar2 = false;
                curChar = data.charAt(currentIndex);
                // if it's a quote, set the flag
                if (curChar == '\'') {
                    quoted = !quoted;
                }
                // otherwise...if the quote flag is NOT set.
                else if (!quoted) {
                    // IF comment... ignore comment lines ...starting with #....and until a carriage return or line feed
                    if (curChar == '#') {
                        // if the preceeding characters were whitepace or new lines, go back and get rid of them

                        while (newData.length() > 0
                            && (isNewLine(newData.charAt(newData.length() - 1)) || isUWhiteSpace(newData.charAt(newData
                                .length() - 1)))) {
                            if (newData.length() == 1) {
                                newData = "";
                            } else {
                                newData = newData.substring(0, newData.length() - 2);
                            }

                        }

                        // move to the end of the line
                        while (!isNewLine(curChar) && currentIndex < data.length()) {
                            currentIndex++;
                            if (currentIndex < data.length()) {
                                curChar = data.charAt(currentIndex);
                            }
                        }
                        // grab the first character of this new line (no longer part of the comment
                        currentIndex++;
                        if (currentIndex < data.length()) {
                            curChar = data.charAt(currentIndex);
                        }

                    } else if (curChar == '\\') // OR if its an escape char //((UChar)0x005C) - \
                    {
                        // skip over the \ and then skip any line breaks that may follow
                        do {
                            currentIndex++;
                            if (currentIndex < data.length()) {
                                curChar = data.charAt(currentIndex);
                            }
                        } while (isNewLine(curChar) && currentIndex < data.length());

                        // if it's a u and there are 4 more characters after it
                        if (curChar == 'u' && (data.length() - currentIndex) >= 4) {
                            // convert it to a character from a codepoint (String)UTF16.valueOf(int)

                            String hexString = data.substring(currentIndex + 1, currentIndex + 5);
                            int codeNum = Integer.parseInt(hexString, 16);
                            String temp = UTF16.valueOf(codeNum);
                            char tempChar;

                            tempChar = temp.charAt(0);

                            // if its 0xFFFFFFFF
                            if (tempChar == 0xFFFFFFFF) {
                                System.err.println("Invalid character found while processing file.");
                                System.exit(-1);
                            }
                            // if NOT whitespace(isUWhiteSpace) && NOT a control character? && not punctuation
                            if (!isUWhiteSpace(tempChar) && !isPunctuation(tempChar) && !isControl(tempChar)) {
                                // set the current character to this character
                                curChar = tempChar;
                                currentIndex += 4; // the 4 numbers...will add one more for the u, already did one for
                                // the slash
                                if (temp.length() > 1) {
                                    curChar2 = temp.charAt(1);
                                    needChar2 = true;
                                }
                            }

                        }

                    } else if (curChar == '\'')// OR if it's a quote
                    {
                        quoted = !quoted;
                    }
                } // end not quoted

                if (isNewLine(curChar)) {
                    quoted = false;
                    // while we're not hitting the end of the string
                    while (currentIndex < data.length()) {
                        if (!isNewLine(curChar)) {
                            break;
                        }
                        currentIndex++;
                        if (currentIndex < data.length()) {
                            curChar = data.charAt(currentIndex);
                        }
                    }
                    continue;
                }

                // append the character to the new string, because we've decided it's ok
                newData += curChar;
                currentIndex++;
                if (needChar2) {
                    newData += curChar2;
                }
            } // end loop

        } catch (Exception e) {
            System.err.println("Had a problem...");
        }
        if (newData.length() > data.length()) {
            return null;
        }
        return newData;
    }

    private static void addDependency(ICUResourceWriter.ResourceTable parent, ICUResourceWriter.ResourceProcess dep,
        String outDir) {
        String filename;
        File f;

        filename = outDir;
        if (!(outDir.charAt(outDir.length() - 1) == '/' || outDir.charAt(outDir.length() - 1) == '\\')) {
            filename += "/";
        }

        filename += dep.val;

        f = new File(filename);
        if (!f.exists()) {
            System.err.println("WARNING: Could not find dependancy: " + filename);
        }
        // create the %%DEPENDENCY array with a string containing the path, add it to the table.
        ICUResourceWriter.ResourceArray a = new ICUResourceWriter.ResourceArray();
        a.name = "%%DEPENDENCY";
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString(null, dep.val);
        a.first = str;
        dep.addAfter(a);

        // Remove the ResourceProcess object and replace it with a ResourceString object.
        ICUResourceWriter.ResourceString replacement = new ICUResourceWriter.ResourceString(dep.name, dep.val);

        ICUResourceWriter.Resource current = parent.first;

        // yes, we're using an address comparison below...because they should both be pointing the the same object when
        // we find it.
        while (current != null && current.next != dep) {
            current = current.next;
        }
        replacement.next = dep.next;
        current.next = replacement;

    }

    private static void addCollationElements(ICUResourceWriter.Resource elementTable) {
        // Element table name is "Collation"
        // loops through sub tables of Collation and adds CollationBinary as nessisary
        ICUResourceWriter.Resource cur = elementTable.first;

        while (cur != null) {
            addCollation(cur);
            cur = cur.next;
        }
    }

    private static void addCollation(ICUResourceWriter.Resource element) {
        ICUResourceWriter.Resource cur = element.first;

        while (cur != null) {
            if (cur.hasKey && (cur instanceof ICUResourceWriter.ResourceString)) {
                ICUResourceWriter.ResourceString strElement = (ICUResourceWriter.ResourceString) cur;

                if (strElement.name.equals("Sequence")) {
                    try {
                        // RuleBasedCollator rbc = new RuleBasedCollator(strElement.val);
                        // TODO Generate proper binary data for Collator
                        /*
                         * currently CollatorWriter does not work properly
                         * Need to write something to generate proper bytes,
                         * bytes do not seem to exist at this time
                         * CollatorWriter was not committed to the ICU4J trunk, it currently lives in the bdrower
                         * subdirectory of icu4j in the IBM local cvs
                         */
                        // byte[] bytes = CollatorWriter.writeRBC(rbc);
                        // ICUResourceWriter.ResourceBinary b = new ICUResourceWriter.ResourceBinary();
                        // b.data = bytes;
                        // b.name = "%%CollationBin";
                        // element.addAfter(b);

                    } catch (Exception e) {
                        System.err.println("Could not create Collation Binary");
                    }
                }
            }
            cur = cur.next;
        }
    }

    /**
     * Write the header section of the file. This section of the file currently contains:<br>
     * -A 2 byte number containing the length (in bytes) of the header.<br>
     * -Two "magic numbers" each 1 byte in size.<br>
     * -The UDataInfo structure
     * -The null terminated copyright string (if it should be written)
     *
     * @param out
     * @param info
     * @param copyright
     */
    private static void writeBinaryHeader(FileOutputStream out, UDataInfo info, String copyright) {
        short headSize = 0;
        byte[] magics = new byte[2];
        int pad = 0;
        byte[] padding;
        /*
         * The header includes a 2 byte number containing the size of the header,
         * two magic numbers each 1 byte in size, the UDataInfo structure, and the
         * copyright plus null terminator. Subject to change.
         */
        headSize += info.size + BYTES_TAKEN_BY_HEADER_SIZE + BYTES_TAKEN_BY_MAGIC1 + BYTES_TAKEN_BY_MAGIC2;
        if (copyright != null && INCLUDE_COPYRIGHT) {
            headSize += copyright.length() + 1;
        }
        if ((pad = pad16Bytes(headSize)) != 0) {
            headSize += pad;
        }

        magics[0] = MAGIC1;
        magics[1] = MAGIC2;

        try {
            // write the size of the header
            out.write(shortToBytes(headSize));
            written += (shortToBytes(headSize)).length;

            // write the two magic numbers
            out.write(magics);
            written += magics.length;

            // write the UDataInfo structure
            out.write(info.getByteArray());
            written += info.getByteArray().length;

            // write the copyright and null terminating byte(s) if writing it
            if (copyright != null && INCLUDE_COPYRIGHT) {
                out.write((copyright + "\0").getBytes(CHARSET8));
                written += ((copyright + "\0").getBytes(CHARSET8)).length;

            }

            if (pad != 0) {
                padding = new byte[pad];
                for (int i = 0; i < padding.length; i++) {
                    padding[i] = 0;
                }
                out.write(padding);
                written += padding.length;
            }

        } catch (IOException e) {
            printError(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Write some information about the key string and then write a chunk of bytes which mirrors the
     * SRBRoot->fkeys character buffer. This will be a list of null
     * terminated strings. Each string pertains to a certain resource. This method also modifies the resources in
     * 'resTop' by setting the keyStringOffset variable. The keyStringOffset variable is the number of bytes from
     * the start of the key string that the resources key starts. For example:
     *
     * <p>
     * In the 'en_PK' locale, you may have a Table resource with the key "Version." The Table contains a string resource
     * with the key "1.31."
     * </p>
     * <p>
     * If this were the whole of the locale data, the key string would be an encoded version of this:
     * </p>
     *
     * "Version\01.31\0"
     *
     * <br>
     * <br>
     * In UTF-16 encoding, each character will take 2 bytes. <br>
     * keyStringOffset for the table object would be 0. <br>
     * keyStringOffset for the string resource would be = "Version".length() + 2 = 16
     *
     *
     * @param out
     *            The output stream to write this to.
     * @param resTop
     *            The top of the resource tree whose keys shall be written
     */
    private static int writeKeyString(FileOutputStream out, ICUResourceWriter.Resource resTop) {
        String keyList = "";
        byte[] padding = null;
        int padBytes = 0;
        int end;
        int root;
        byte[] rootBytes;
        int[] indexes = new int[URES_INDEX_TOP];
        byte[] indexBytes = new byte[URES_INDEX_TOP * 4];
        byte[] keyBytes;
        int usedOffset;
        int sizeOfIndexes;
        int sizeOfIndexesAndKeys;
        int tableID;

        // set flag so that we know which resource is the top of the tree
        resTop.isTop = true;

        sizeOfIndexes = (1 + URES_INDEX_TOP) * ICUResourceWriter.SIZE_OF_INT;

        usedOffset = sizeOfIndexes;

        // Build the String of keys
        keyList = buildKeyList(keyList, resTop, usedOffset);

        sizeOfIndexesAndKeys = sizeOfIndexes + keyList.length();
        usedOffset = sizeOfIndexesAndKeys + pad32(sizeOfIndexesAndKeys);

        end = sizeOfIndexesAndKeys + resTop.sizeOfChildren;

        // if it is not 16 byte aligned
        if ((padBytes = pad32(sizeOfIndexesAndKeys)) != 0) {
            padding = createPadding(padBytes);
            if (padding != null) {
                usedOffset += padding.length;
                end += padding.length;
            }

        }

        // build a set of 32 bits (in C this variable is called 'root' in reslist.c)
        // the number of bytes included in the keyList, keyList padding, all the children

        if (((ICUResourceWriter.ResourceTable) resTop).is32Bit()) {
            tableID = (URES_TABLE32 << 28);
        } else {
            tableID = (URES_TABLE << 28);
        }
        root = (end >>> 2) | (tableID);

        rootBytes = intToBytes(root);

        end += resTop.size;

        end += pad32(end);

        indexes[URES_INDEX_LENGTH] = URES_INDEX_TOP;
        indexes[URES_INDEX_STRINGS_TOP] = usedOffset >>> 2;
        indexes[URES_INDEX_RESOURCES_TOP] = (end) >> 2;
        indexes[URES_INDEX_BUNDLE_TOP] = indexes[URES_INDEX_RESOURCES_TOP];
        indexes[URES_INDEX_MAX_TABLE_LENGTH] = ICUResourceWriter.maxTableLength;

        indexBytes = intArrayToBytes(indexes);

        try {
            // write the "root" object
            out.write(rootBytes);
            written += rootBytes.length;

            // write the indexes array
            out.write(indexBytes);
            written += indexBytes.length;

            // write the keyList and padding if nessicary
            keyBytes = keyList.getBytes(CHARSET8);
            out.write(keyBytes);
            written += keyBytes.length;

            if (padding != null) {
                out.write(padding);
                written += padding.length;
            }
        } catch (IOException e) {
            printError("Could not write key string to file. " + e.getMessage());
            System.exit(1);
        }

        return usedOffset;
    }

    /**
     * Recursively go through the whole tree and continue to add to the keyList. As this is done,
     * set the keyStringOffset, numChildren, sizeOfChildren, and size variables.
     *
     * @param keyList
     *            The current string of keys.
     * @param resTop
     *            The resource whose keys shall be written to the keyList.
     * @return
     */
    private static String buildKeyList(String keyList, ICUResourceWriter.Resource resTop, int usedOffset) {
        ICUResourceWriter.Resource current = resTop.first;
        int x = 0;

        // add this resources key to the list unless it is the top resource or doesn't have a key
        if (!resTop.isTop && resTop.hasKey) {
            // clean up quotes if any
            if (resTop.name.indexOf("\"") >= 0) {
                resTop.name = removeQuotes(resTop.name);
            }
            // set the keyStringOffset
            resTop.keyStringOffset = usedOffset + (keyList.length() * BYTES_PER_UTF8_CHAR);
            keyList += (resTop.name + "\0");

        }

        // if it has children, call this method on them too
        while (current != null) {
            if (resTop instanceof ICUResourceWriter.ResourceArray
                || resTop instanceof ICUResourceWriter.ResourceIntVector) {
                current.hasKey = false;
            }

            keyList = buildKeyList(keyList, current, usedOffset);
            x++;

            // add the size of the current child to the parents sizeOfChildren

            current = current.next;
        }

        // set the size of this object
        resTop.setSize();

        resTop.numChildren = x;
        return keyList;
    }

    /**
     * Takes a 16 bit number and returns a two byte array. 0th element is lower byte, 1st element is upper byte.
     * Ex: x = 28,000. In binary: 0110 1101 0110 0000. This method will return:
     * [0] = 0110 0000 or 0x60
     * [1] = 0110 1101 or 0x6D
     */
    private static byte[] shortToBytes(short x) {
        byte[] b = new byte[2];
        b[1] = (byte) (x); // bitwise AND with the lower byte
        b[0] = (byte) (x >>> 8); // shift four bits to the right and fill with zeros, and then bitwise and with the
        // lower byte
        return b;
    }

    /**
     * Takes a 32 bit integer and returns an array of 4 bytes.
     *
     */
    private static byte[] intToBytes(int x) {
        byte[] b = new byte[4];
        b[3] = (byte) (x); // just the last byte

        x = x >>> 8; // shift each byte over one spot.
        b[2] = (byte) (x); // just the last byte

        x = x >>> 8; // shift each byte over one spot.
        b[1] = (byte) (x); // just the last byte

        x = x >>> 8; // shift each byte over one spot.
        b[0] = (byte) (x); // just the last byte

        return b;
    }

    /**
     * Takes an array of integers and returns a byte array of the memory representation.
     *
     * @param x
     * @return
     */
    private static byte[] intArrayToBytes(int[] x) {
        byte[] b = new byte[x.length * 4];
        byte[] temp;
        int i, z;

        for (i = 0; i < x.length; i++) {
            temp = intToBytes(x[i]);
            for (z = 0; z < 4; z++) {
                b[(i * 4) + z] = temp[z];
            }
        }
        return b;
    }

    /**
     * calculate the padding to make things align with 32 bits (aka 4 bytes)
     *
     * @param x
     * @return
     */
    private static int pad32(int x) {
        return ((x % 4) == 0) ? 0 : (4 - (x % 4));
    }

    private static int pad16Bytes(int x) {
        return ((x % 16) == 0) ? 0 : (16 - (x % 16));
    }

    /**
     * for printing errors.
     */
    private static void printError(String message) {

        System.err.println("LDML2ICUBinaryWriter : ERROR : " + message);
    }

    private static byte[] createPadding(int length) {
        byte x = (byte) 0x00;
        byte[] b = new byte[length];
        if (length == 0) {
            return null;
        }
        for (int z = 0; z < b.length; z++) {
            b[z] = x;
        }

        return b;
    }

    public static String removeQuotes(String s) {
        String temp = s;
        String temp2;
        int x;
        while (temp.indexOf("\"") >= 0) {
            x = temp.indexOf("\"");
            temp2 = temp.substring(0, x);
            temp2 += temp.substring(x + 1, temp.length());
            temp = temp2;
        }

        return temp;
    }

}