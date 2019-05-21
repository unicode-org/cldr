/*
 **********************************************************************
 * Copyright (c) 2009, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 **********************************************************************
 */
package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.ibm.icu.text.Normalizer;

/**
 * Tool to help determine if
 *
 * @author John C. Emmons
 */

public class FilterCharacterFallbacks {

    public static void main(String[] args) throws IOException {

        Document fb;
        Node n;
        fb = LDMLUtilities.parse(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY + File.separator + "characters.xml", true);
        if (fb != null) {
            PrintWriter out = FileUtilities.openUTF8Writer(".", "report");
            n = LDMLUtilities.getNode(fb, "//supplementalData/characters/character-fallback");
            for (Node cf = n.getFirstChild(); cf != null; cf = cf.getNextSibling()) {
                String srcChar = LDMLUtilities.getAttributeValue(cf, "value");
                if (srcChar != null) {
                    for (Node sb = cf.getFirstChild(); sb != null; sb = sb.getNextSibling()) {
                        String subChars = LDMLUtilities.getNodeValue(sb);
                        if (subChars != null) {
                            boolean canonicallyEquivalent = (Normalizer.compare(srcChar, subChars, 0) == 0);
                            if (canonicallyEquivalent) {
                                out.println("Remove Character \"" + srcChar + "\" ("
                                    + com.ibm.icu.impl.Utility.escape(srcChar) + ")    Substitute \"" + subChars
                                    + "\" (" + com.ibm.icu.impl.Utility.escape(subChars)
                                    + ") - Canonically equivalent.");
                            }
                            String toNFKC = Normalizer.normalize(srcChar, Normalizer.NFKC);
                            if (subChars.equals(toNFKC)) {
                                out.println("Remove Character \"" + srcChar + "\" ("
                                    + com.ibm.icu.impl.Utility.escape(srcChar) + ")    Substitute \"" + subChars
                                    + "\" (" + com.ibm.icu.impl.Utility.escape(subChars) + ") - a toNFKC form.");
                            } else {
                                out.println("OK - Character \"" + srcChar + "\" ("
                                    + com.ibm.icu.impl.Utility.escape(srcChar) + ")    Substitute \"" + subChars
                                    + "\" (" + com.ibm.icu.impl.Utility.escape(subChars) + ")");
                            }
                        }
                    }
                }
            }
            out.close();
        } else
            System.out.println("Couldn't open characters.xml...");

    }
}
