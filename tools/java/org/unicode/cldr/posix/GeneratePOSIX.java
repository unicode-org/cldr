/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons & Mark Davis
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
 

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.SortedBag;
import com.ibm.icu.dev.tool.UOption;

/**
 * Class to generate POSIX format from CLDR. 
 * @author jcemmons/medavis
 */
public class GeneratePOSIX {

    private static final int 
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        UNICODESET = 5,
        COLLATESET = 6,
        CHARSET = 7;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("sourcedir", 's', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("destdir", 'd', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault("root"),
        UOption.create("unicodeset", 'u', UOption.REQUIRES_ARG),
        UOption.create("collateset", 'x', UOption.REQUIRES_ARG),
        UOption.create("charset", 'c', UOption.REQUIRES_ARG).setDefault("UTF-8"),
    };

    public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        String locale = options[MATCH].value;
        String codeset = options[CHARSET].value;
        String cldr_data_location = options[SOURCEDIR].value;
        UnicodeSet collate_set;
        UnicodeSet repertoire;
        if ( options[COLLATESET].doesOccur )
           collate_set = new UnicodeSet(options[COLLATESET].value);
        else
           collate_set = new UnicodeSet();

        if ( options[UNICODESET].doesOccur )
           repertoire = new UnicodeSet(options[UNICODESET].value);
        else
           repertoire = new UnicodeSet();

    	POSIXLocale pl = new POSIXLocale(locale,cldr_data_location,repertoire,Charset.forName(options[CHARSET].value),codeset,collate_set);
        PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value+File.separator,locale + "." + codeset + ".src");
        pl.write(out);
        out.close();
    }

}
