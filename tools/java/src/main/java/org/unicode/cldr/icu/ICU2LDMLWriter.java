/*
 ******************************************************************************
 * Copyright (C) 2004-2008 International Business Machines Corporation and    *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.icu;

//CLDR imports
import org.unicode.cldr.ant.CLDRConverterTool;

import com.ibm.icu.dev.tool.UOption;

/**
 * This class is a user runnable class intended to create LDML documents by
 * calling the appropriate ICU API functions and retrieving the data.
 *
 * @author Brian Rower - IBM - August 2009
 *
 */
@SuppressWarnings("deprecation")
public class ICU2LDMLWriter extends CLDRConverterTool {
    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.DESTDIR(),
        UOption.VERBOSE(),
        UOption.create("main", 'm', UOption.NO_ARG),
        UOption.create("trans", 't', UOption.NO_ARG),
        UOption.create("coll", 'c', UOption.NO_ARG)
    };

    // ******************** ENUMS *************************

    // For command line options
    private static final int OP_HELP1 = 0;
    private static final int OP_HELP2 = 1;
    private static final int OP_DESTDIR = 2;
    private static final int OP_VERBOSE = 3;
    private static final int OP_MAIN = 4;
    private static final int OP_TRANS = 5;
    private static final int OP_COLL = 6;

    private String currentLocaleName = null;

    public static void main(String[] args) {
        ICU2LDMLWriter w = new ICU2LDMLWriter();
        w.processArgs(args);
    }

    public void processArgs(String[] args) {
        int remainingArgc = 0;
        // for some reason when
        // Class classDefinition = Class.forName(className);
        // object = classDefinition.newInstance();
        // is done then the options are not reset!!
        for (int i = 0; i < options.length; i++) {
            options[i].doesOccur = false;
        }
        try {
            remainingArgc = UOption.parseArgs(args, options);
        } catch (Exception e) {
            printError("(parsing args): " + e.toString());
            e.printStackTrace();
            usage();
        }

        if (options[OP_HELP1].doesOccur || options[OP_HELP2].doesOccur || args.length == 0) {
            usage();
        }

        printError("** Note: ICU2LDMLWriter is no longer supported or functional. **");

    }

    private void usage() {
        System.out
            .println("\nUsage: ICU2LDMLWriter [OPTIONS] -d OUTPUTFOLDER [FILES]\n"
                +
                "This program generates LDML documents which represent the data currently inside of your ICU installation\n"
                +
                "If no files are given, a file for each supported locale will be produced.\n"
                +
                "If no destination folder is given, the file structure will be created in the current working directory.\n"
                +
                "Options:\n" +
                "-m or --main			Generate the main locale files.\n" +
                "-t or --trans			Generate the transliteration locale files.\n" +
                "-c or --coll			Generate the collation files.\n" +
                "If none of the above options are given, all three types of files will be generated.\n" +
                "\n" +
                "** Note: ICU2LDMLWriter is no longer supported or functional. **\n");
        System.exit(-1);
    }

    // **********************Printing Methods *********************************

    private void printError(String message) {
        System.err.println("ERROR : " + currentLocaleName + ": " + message);
    }
}
