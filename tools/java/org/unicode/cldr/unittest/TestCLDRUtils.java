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

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;
import org.unicode.cldr.util.CLDRLocale.FormatBehavior;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
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

    public void TestEmptyCLDRFile() {
        CLDRLocale aloc = CLDRLocale.getInstance("und_AQ_NONEXISTENT");
        logln("Testing CLDRFile.make(" + aloc.toString() + ").write()");
        CLDRFile emptyFile = SimpleFactory.makeFile(aloc.getBaseName());
        StringWriter outStream = new StringWriter();
        try {
            emptyFile.write(new PrintWriter(outStream));
        } finally {
            logln(aloc.getBaseName() + ".xml: "
                + outStream.toString().replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t"));
        }
        if (outStream.toString().length() == 0) {
            errln("Error: empty CLDRFile of " + aloc + " turned into a 0-length string.");
        }
    }

    public void TestCLDRLocaleFormatNoFile() {
        logln("Tests for CLDRLocale:");
        CLDRLocale.setDefaultFormatter(new CLDRFormatter(FormatBehavior.replace));
        String tests_str[] = { "", "root", "el__POLYTON", "el_POLYTON", "__UND", "en", "en_GB", "en_Shav",
            "en_Shav_GB", "en_Latn_GB_POLYTON", "nnh", "sq_XK" };
        for (String s : tests_str) {
            CLDRLocale loc = CLDRLocale.getInstance(s);
            String str = loc.toString();
            ULocale uloc = loc.toULocale();
            String fromloc = new ULocale(s).toString();
            String format = loc.getDisplayName();
            CLDRLocale parent = loc.getParent();
            logln(s + ":  tostring:" + str + ", uloc:" + uloc + ", fromloc:" + fromloc + ", format: " + format
                + ", parent:" + parent);
        }

        CLDRLocale.setDefaultFormatter(CLDRLocale.getSimpleFormatterFor(ULocale.getDefault()));
    }
    
    public void TestCLDRLocaleDataDriven() throws IOException {
        XMLFileReader myReader = new XMLFileReader();
        final Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
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
                logln("* <" + elem + " " + attrs.toString() + ">" + value + "</" + elem + ">");
                String loc = attrs.get("locale");
                CLDRLocale locale = CLDRLocale.getInstance(loc);
                if (elem.equals("format")) {
                    String type = attrs.get("type");
                    String result = null;
                    if(type.equals("region")) {
                        result = locale.getDisplayCountry(engFormat);
                    } else if(type.equals("all")) {
                            result = locale.getDisplayName(engFormat);
                    } else {
                        errln("Unknown test type: " + type);
                        return;
                    }                    
                    
                    if(result==null) {
                        errln("Null result!");
                        return;
                    }
                    logln("  result="+result);
                    if(!result.equals(value)) {
                        errln("For format test " + attrs.toString() + " expected '"+value+"' got '"+result+"'");
                    }
                } else if (elem.equals("echo")) {
                    logln("*** \"" + value.trim() + "\"");
                } else {
                    throw new IllegalArgumentException("Unknown test element type " + elem);
                }
            };
            // public void handleComment(String path, String comment) {};
            // public void handleElementDecl(String name, String model) {};
            // public void handleAttributeDecl(String eName, String aName,
            // String type, String mode, String value) {};
        });
        String fileName = "TestCLDRLocale" + ".xml";
        logln("Reading" + fileName);
        myReader.read(TestCLDRUtils.class.getResource( "data/" +  fileName).toString(),
                FileUtilities.openFile(TestCLDRUtils.class,  "data/" +  fileName), -1, true);
    }    
    public void TestCLDRLocaleInheritance() {
        CLDRLocale ml = CLDRLocale.getInstance("ml");
        logln("Testing descendants of " + ml + " " + ml.getDisplayName());
        CLDRLocale areSub[] = { // isChild return strue
                CLDRLocale.getInstance("ml"),
                CLDRLocale.getInstance("ml_IN"),
                CLDRLocale.getInstance("ml_MLym_IN")
        };
        for (CLDRLocale l : areSub) {
            final boolean expect = true;
            boolean got = l.childOf(ml);
            if (got == expect) {
                logln(l + " " + l.getDisplayName() + ".childOf(ml) = " + got);
            } else {
                errln(l + " " + l.getDisplayName() + ".childOf(ml) = " + got + " expected " + expect);
            }
        }
        CLDRLocale notSub[] = { // isCHild returns false
                CLDRLocale.getInstance("mli"),
                CLDRLocale.getInstance("root"),
                CLDRLocale.ROOT
        };
        for (CLDRLocale l : notSub) {
            final boolean expect = false;
            boolean got = l.childOf(ml);
            if (got == expect) {
                logln(l + " " + l.getDisplayName() + ".childOf(ml) = " + got);
            } else {
                errln(l + " " + l.getDisplayName() + ".childOf(ml) = " + got + " expected " + expect);
            }
        }
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
