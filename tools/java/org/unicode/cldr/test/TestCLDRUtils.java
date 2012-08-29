/*
 **********************************************************************
 * Copyright (c) 2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Steven R. Loomis
 **********************************************************************
 */
package org.unicode.cldr.test;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;
import org.unicode.cldr.util.CLDRLocale.FormatBehavior;
import org.unicode.cldr.util.SimpleFactory;

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
        CLDRLocale aloc = CLDRLocale.getInstance("tlh");
        logln("Testing CLDRFile.make("+aloc.toString()+").write()");
        CLDRFile emptyFile = SimpleFactory.makeFile(aloc.getBaseName());
        StringWriter outStream = new StringWriter();
        try {
            emptyFile.write(new PrintWriter(outStream));
        } finally {
            System.out.println(aloc.getBaseName()+".xml: "+outStream.toString().replaceAll("\n","\\\\n").replaceAll("\t","\\\\t"));
        }
        if(outStream.toString().length() == 0) {
            errln("Error: empty CLDRFile of " + aloc + " turned into a 0-length string.");
        }
    }
    
    
    public void TestCLDRLocale() {
        logln("Tests for CLDRLocale:");
        CLDRLocale.setDefaultFormatter(new CLDRFormatter(FormatBehavior.replace));
        String tests_str[] = { "", "root", "el__POLYTON", "el_POLYTON", "__UND", "en","en_GB","en_Shav","en_Shav_GB","en_Latn_GB_POLYTON","nnh"};
        for(String s:tests_str) {
            CLDRLocale loc = CLDRLocale.getInstance(s);
            String str = loc.toString();
            ULocale uloc = loc.toULocale();
            String fromloc = new ULocale(s).toString();
            String format = loc.getDisplayName();
            CLDRLocale parent = loc.getParent();
            logln(s+":  tostring:"+str+", uloc:"+uloc+", fromloc:"+fromloc + ", format: " + format + ", parent:"+parent);
        }
        
        {
            CLDRLocale ml = CLDRLocale.getInstance("ml");
            logln("Testing descendants of " + ml + " " + ml.getDisplayName());
            CLDRLocale areSub[] = {  //  isChild return strue
                CLDRLocale.getInstance("ml"), 
                CLDRLocale.getInstance("ml_IN"), 
                CLDRLocale.getInstance("ml_MLym_IN")
            };
            for(CLDRLocale l : areSub) {
                final boolean expect = true;
                boolean got = l.childOf(ml);
                if(got==expect) {
                    logln( l + " " + l.getDisplayName() + ".childOf(ml) = " + got);
                } else {
                    errln( l + " " + l.getDisplayName() + ".childOf(ml) = " + got + " expected " + expect);
                }
            }
            CLDRLocale notSub[] = { // isCHild returns false
                CLDRLocale.getInstance("mli"),
                CLDRLocale.getInstance("root"),
                CLDRLocale.ROOT
            };
            for(CLDRLocale l : notSub) {
                final boolean expect = false;
                boolean got = l.childOf(ml);
                if(got==expect) {
                    logln( l + " " + l.getDisplayName() + ".childOf(ml) = " + got);
                } else {
                    errln( l + " " + l.getDisplayName() + ".childOf(ml) = " + got + " expected " + expect);
                }
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
        System.out.println("Seconds: " + deltaTime/1000);
    }

}
