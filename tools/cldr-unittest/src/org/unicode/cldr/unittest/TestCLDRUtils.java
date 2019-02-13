/*
 **********************************************************************
 * Copyright (c) 2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Steven R. Loomis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;
import org.unicode.cldr.util.CLDRLocale.FormatBehavior;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class TestCLDRUtils extends TestFmwk {

    /**
     *
     */
    public TestCLDRUtils() {
        // TODO Auto-generated constructor stub
    }

    static Transform<String, String> SHORT_ALT_PICKER = new Transform<String, String>() {
        public String transform(String source) {
            return "short";
        }
    };

    public void TestVariantName() {
        CLDRFile english = CLDRConfig.getInstance().getEnglish();

        checkNames(english, "en_US_POSIX", 
            "American English (Computer)",
            "US English (Computer)", 
            "English (United States, Computer)",
            "English (US, Computer)");

        checkNames(english, new ULocale("en_US_POSIX").toLanguageTag(),
            "American English (POSIX Compliant Locale)",
            "US English (POSIX Compliant Locale)", 
            "English (United States, POSIX Compliant Locale)",
            "English (US, POSIX Compliant Locale)");

        checkNames(english, "en_HK", "English (Hong Kong SAR China)",
            "English (Hong Kong)", "English (Hong Kong SAR China)",
            "English (Hong Kong)");

        checkNames(english, "en_GB", "British English", "UK English",
            "English (United Kingdom)", "English (UK)");

        checkNames(english, "eo_001", "Esperanto (World)");

        checkNames(english, "el_POLYTON", "Greek (Polytonic)");

        checkNames(english, new ULocale("el__POLYTON").toLanguageTag(),
            "Greek (Polytonic)");

        CLDRFile french = CLDRConfig.getInstance().getCldrFactory()
            .make("fr", true);

        checkNames(french, "en_US_POSIX", "anglais américain (informatique)",
            "anglais [É.-U.] (informatique)",
            "anglais (États-Unis, informatique)",
            "anglais (É.-U., informatique)");
    }

    /**
     *
     * @param french
     * @param locale
     * @param combinedLong
     * @param otherNames
     *            : combinedShort, uncombinedLong, uncombinedShort
     */
    private void checkNames(CLDRFile french, String locale,
        String combinedLong, String... otherNames) {
        assertEquals("Test variant formatting combinedLong " + locale,
            combinedLong, french.getName(locale));
        String combinedShort = otherNames.length > 0 ? otherNames[0]
            : combinedLong;
        String uncombinedLong = otherNames.length > 1 ? otherNames[1]
            : combinedLong;
        String uncombinedShort = otherNames.length > 2 ? otherNames[2]
            : uncombinedLong;

        assertEquals("Test variant formatting combinedShort " + locale,
            combinedShort, french.getName(locale, false, SHORT_ALT_PICKER));
        assertEquals("Test variant formatting uncombinedLong " + locale,
            uncombinedLong, french.getName(locale, true));
        assertEquals("Test variant formatting uncombinedShort " + locale,
            uncombinedShort, french.getName(locale, true, SHORT_ALT_PICKER));
    }

    public void TestEmptyCLDRFile() {
        CLDRLocale aloc = CLDRLocale.getInstance("und_AQ_NONEXISTENT");
        logln("Testing CLDRFile.make(" + aloc.toString() + ").write()");
        CLDRFile emptyFile = SimpleFactory.makeFile(aloc.getBaseName());
        StringWriter outStream = new StringWriter();
        try {
            emptyFile.write(new PrintWriter(outStream));
        } finally {
            logln(aloc.getBaseName()
                + ".xml: "
                + outStream.toString().replaceAll("\n", "\\\\n")
                    .replaceAll("\t", "\\\\t"));
        }
        if (outStream.toString().length() == 0) {
            errln("Error: empty CLDRFile of " + aloc
                + " turned into a 0-length string.");
        }
    }

    public void TestCLDRLocaleFormatNoFile() {
        logln("Tests for CLDRLocale:");
        CLDRLocale
            .setDefaultFormatter(new CLDRFormatter(FormatBehavior.replace));
        String tests_str[] = { "", "root", "el__POLYTON", "el_POLYTON",
            "__UND", "en", "en_GB", "en_Shav", "en_Shav_GB",
            "en_Latn_GB_POLYTON", "nnh", "sq_XK" };
        for (String s : tests_str) {
            CLDRLocale loc = CLDRLocale.getInstance(s);
            String str = loc.toString();
            ULocale uloc = loc.toULocale();
            String fromloc = new ULocale(s).toString();
            String format = loc.getDisplayName();
            CLDRLocale parent = loc.getParent();
            logln(s + ":  tostring:" + str + ", uloc:" + uloc + ", fromloc:"
                + fromloc + ", format: " + format + ", parent:" + parent);
        }

        CLDRLocale.setDefaultFormatter(CLDRLocale.getSimpleFormatterFor(ULocale
            .getDefault()));
    }

    // Disable this test, because we now require known DtdTypes
    public void oldTestCLDRLocaleDataDriven() throws IOException {
        XMLFileReader myReader = new XMLFileReader();
        final Factory cldrFactory = Factory
            .make(CLDRPaths.MAIN_DIRECTORY, ".*");
        final CLDRFile engFile = cldrFactory.make("en", true);
        final CLDRFormatter engFormat = new CLDRFormatter(engFile);
        final XPathParts xpp = new XPathParts(null, null);
        final Map<String, String> attrs = new TreeMap<String, String>();
        myReader.setHandler(new XMLFileReader.SimpleHandler() {
            public void handlePathValue(String path, String value) {
                xpp.clear();
                xpp.initialize(path);
                attrs.clear();
                for (String k : xpp.getAttributeKeys(-1)) {
                    attrs.put(k, xpp.getAttributeValue(-1, k));
                }
                String elem = xpp.getElement(-1);
                logln("* <" + elem + " " + attrs.toString() + ">" + value
                    + "</" + elem + ">");
                String loc = attrs.get("locale");
                CLDRLocale locale = CLDRLocale.getInstance(loc);
                if (elem.equals("format")) {
                    String type = attrs.get("type");
                    String result = null;
                    boolean combined = Boolean.parseBoolean(attrs
                        .get("combined"));
                    Transform<String, String> picker = attrs.get("alt")
                        .equalsIgnoreCase("short") ? SHORT_ALT_PICKER
                            : null;
                    if (type.equals("region")) {
                        result = locale.getDisplayCountry(engFormat);
                    } else if (type.equals("all")) {
                        result = locale.getDisplayName(engFormat, combined,
                            picker);
                    } else {
                        errln("Unknown test type: " + type);
                        return;
                    }

                    if (result == null) {
                        errln("Null result!");
                        return;
                    }
                    logln("  result=" + result);
                    if (!result.equals(value)) {
                        errln("For format test " + attrs.toString()
                            + " expected '" + value + "' got '" + result
                            + "'");
                    }
                } else if (elem.equals("echo")) {
                    logln("*** \"" + value.trim() + "\"");
                } else {
                    throw new IllegalArgumentException(
                        "Unknown test element type " + elem);
                }
            };
            // public void handleComment(String path, String comment) {};
            // public void handleElementDecl(String name, String model) {};
            // public void handleAttributeDecl(String eName, String aName,
            // String type, String mode, String value) {};
        });
        String fileName = "TestCLDRLocale" + ".xml";
        logln("Reading" + fileName);
        myReader.read(TestCLDRUtils.class.getResource("data/" + fileName)
            .toString(), FileReaders.openFile(TestCLDRUtils.class, "data/"
                + fileName),
            -1, true);
    }

    public void TestCLDRLocaleInheritance() {
        CLDRLocale ml = CLDRLocale.getInstance("ml");
        CLDRLocale ml_IN = CLDRLocale.getInstance("ml_IN");
        CLDRLocale ml_Mlym = CLDRLocale.getInstance("ml_Mlym");
        CLDRLocale ml_Mlym_IN = CLDRLocale.getInstance("ml_Mlym_IN");

        logln("Testing descendants of " + ml + " " + ml.getDisplayName());
        CLDRLocale areSub[][] = { // isChild returns true for i!=0
            { ml, ml_IN, ml_Mlym, ml_Mlym_IN }, { ml_Mlym, ml_Mlym_IN }, };
        for (CLDRLocale[] row : areSub) {
            CLDRLocale parent = row[0];
            for (CLDRLocale child : row) {
                // TODO move the first checkChild here if the meaning changes.
                if (child == parent) {
                    continue;
                }
                checkChild(child, parent, false);
                checkChild(parent, child, true);
            }
        }
        CLDRLocale notSub[] = { // isChild returns false
            CLDRLocale.getInstance("mli"), CLDRLocale.getInstance("root"),
            CLDRLocale.ROOT };
        for (CLDRLocale child : notSub) {
            checkChild(ml, child, false);
        }
    }

    public void TestCLDRLocaleEquivalence() {
        assertEquals("root is caseless", CLDRLocale.getInstance("root"), CLDRLocale.getInstance("RoOt"));
        assertEquals("root = empty", CLDRLocale.getInstance("root"), CLDRLocale.getInstance(""));
        String test = "zh-TW-u-co-pinyin";
        assertEquals(test, test, CLDRLocale.getInstance(test).toLanguageTag());
    }

    private boolean checkChild(CLDRLocale parent, CLDRLocale child,
        boolean expected) {
        boolean got = child.childOf(parent);
        String message = child + ".childOf(" + parent + ") " + "["
            + child.getDisplayName() + ", " + parent.getDisplayName()
            + "] " + "= " + got;
        if (got == expected) {
            logln(message);
        } else {
            errln(message + ", but expected " + expected);
        }
        return got;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        double deltaTime = System.currentTimeMillis();
        new TestCLDRUtils().run(args);
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Seconds: " + deltaTime / 1000);
    }

}
