/*
**********************************************************************
* Copyright (c) 2002-2010, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons & Mark Davis
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.UnicodeSet;

/**
 * Class to generate POSIX format from CLDR. 
 * @author jcemmons/medavis
 */
public class GeneratePOSIX {

    private static final int 
        HELP1 = 0,
        HELP2 = 1,
        DESTDIR = 2,
        MATCH = 3,
        UNICODESET = 4,
        COLLATESET = 5,
        CHARSET = 6;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("destdir", 'd', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("match", 'm', UOption.REQUIRES_ARG),
        UOption.create("unicodeset", 'u', UOption.REQUIRES_ARG),
        UOption.create("collateset", 'x', UOption.REQUIRES_ARG),
        UOption.create("charset", 'c', UOption.REQUIRES_ARG).setDefault("UTF-8"),
    };

    public static void main(String[] args) throws Exception {
        UOption.parseArgs(args, options);
        if ( ! options[MATCH].doesOccur || options[HELP1].doesOccur || options[HELP2].doesOccur)
           Usage();

        String [] matchargs = options[MATCH].value.split("@",2);
        String locale = matchargs[0];
        POSIXVariant variant;

        if ( options[MATCH].value.indexOf("@") > 0 )
           variant = new POSIXVariant(matchargs[1]);
        else
           variant = new POSIXVariant();

        String codeset = options[CHARSET].value;
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

        if ( (!codeset.equals("UTF-8")) && ( options[COLLATESET].doesOccur || options[UNICODESET].doesOccur ))
        {
           System.out.println("Error: Specifying a non-UTF-8 codeset and repertoire or collation overrides are mutually exclusive.");
           Usage();
        }


    	POSIXLocale pl = new POSIXLocale(locale,repertoire,Charset.forName(options[CHARSET].value),codeset,collate_set,variant);
        PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value+File.separator,options[MATCH].value + "." + codeset + ".src");
        pl.write(out);
        out.close();
    }

    public static void Usage () {

    System.out.println("Usage: GeneratePOSIX [-d target_dir] -m locale_name[@variants]");
    System.out.println("                     { [-c codeset] | [-u repertoire_set][-x collation_set] }");
    System.out.println("where:");
    System.out.println("   -d target_dir is the directory where POSIX .src files will be written");
    System.out.println("   -m locale_name is the language/territory you want to generate");
    System.out.println("   -c codeset is the character set to use for the locale (Default = UTF-8)");
    System.out.println("   -u repertoire_set : Use to override the default repertoire set (UnicodeSet format)");
    System.out.println("   -x collation_set  : Use to override the default collation set (UnicodeSet format)");
    System.exit(-1);
    
    }
}
