package org.unicode.cldr.tool;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.util.ULocale;
public class DumpCoverage {

/**
  * @param args
  */
    public static void main(String[] args) throws IOException {

        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        Set<String> languages = cldrFactory.getAvailableLanguages();
        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,"coverageDump.txt");
        for ( String lang : languages ) {
            ULocale loc = new ULocale(lang);
            CLDRFile cf = cldrFactory.makeWithFallback(lang);
            Set<String> paths = new TreeSet<String>();
            cf.getPaths("//ldml", null, paths);
            System.out.println("Dumping coverage for locale --> " + loc);
            for ( String path : paths) {
                int cov = sdi.getCoverageValue(path, loc);
                out.println(lang + " [" + cov + "] --> " + path);
                
            }
        }
        out.close();
    }
}
