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

import com.ibm.icu.dev.test.TestFmwk;

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
        CLDRFile emptyFile = CLDRFile.make(aloc.getBaseName());
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
