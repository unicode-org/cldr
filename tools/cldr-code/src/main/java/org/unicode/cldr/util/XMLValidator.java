/*
 ******************************************************************************
 * Copyright (C) 2003-2014, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
/**
 * @author Ram Viswanadha
 *
 * This tool validates xml against DTD or valid XML ... IE 6 does not do a good job
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@CLDRTool(alias = "validate", description = "Check XML files for validity")
public class XMLValidator {
    public boolean quiet = false;
    public boolean parseonly = false;
    public boolean justCheckBom = false;

    public XMLValidator(boolean quiet, boolean parseonly, boolean justCheckBom) {
        this.quiet = quiet;
        this.parseonly = parseonly;
        this.justCheckBom = justCheckBom;
    }

    public static void main(String[] args) throws IOException {
        boolean quiet = false;
        boolean parseonly = false;
        boolean justCheckBom = false;
        if (args.length == 0) {
            System.out.println("No files specified. Validation failed. Use --help for help.");
            return;
        }
        List<File> toCheck = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-q") || args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--help")) {
                usage();
                return;
            } else if (args[i].equals("--parseonly")) {
                parseonly = true;
            } else if (args[i].equals("--justCheckBom")) {
                justCheckBom = true;
            } else {
                File f = new File(args[i]);
                if (f.isDirectory()) {
                    addDirectory(f, toCheck);
                } else if(f.canRead()) {
                    toCheck.add(f);
                } else {
                    throw(new IllegalArgumentException("Not a regular file: " + f.getAbsolutePath()));
                }
            }
        }
        if (parseonly) {
            System.err.println("# DTD Validation is disabled. Will only check for well-formed XML.");
        }
        if(toCheck.isEmpty()) {
            throw new IllegalArgumentException("No files specified to check.");
        }
        if(!quiet) {
            System.err.println("# " + toCheck.size() + " file(s) to check");
        }
        int failCount = new XMLValidator(quiet, parseonly, justCheckBom).check(toCheck);
        if(failCount != 0) {
            System.err.println("# FAIL: " + failCount + " of " + toCheck.size() + " file(s) had errors.");
            System.exit(1);
        } else if(!quiet) {
            System.err.println("# " + toCheck.size() + " file(s) OK");
        }
    }

    private static void addDirectory(File f, List<File> toCheck) throws IOException {
        // System.err.println("Parsing directory " + f.getAbsolutePath());
        for (final File s : f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                if (arg1.startsWith(".")) {
                    return false; // skip .git, .svn, ...
                }
                File n = new File(arg0, arg1);
                // System.err.println("Considering " + n.getAbsolutePath() );
                if (n.isDirectory()) {
                    try {
                        addDirectory(n, toCheck);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println("Error " + e.toString() + " parsing " + arg0.getPath());
                    }
                    return false;
                } else if (arg1.endsWith(".xml")) {
                    return true;
                } else {
                    return false;
                }
            }
        })) {
            toCheck.add(s);
        }
    }

    /**
     * Check a list of files, return the number of failures
     * @param toCheck
     * @return failure count, or 0 if all OK
     */
    public int check(List<File> toCheck) {
       return toCheck
            .parallelStream()
            .mapToInt(f -> parse(f))
            .sum();
    }

    private static void usage() {
        System.err.println("usage:  " + XMLValidator.class.getName() + " [ -q ] [ --help ] [ --parseonly ] [ --justCheckBom ] file ...");
        System.err.println("usage:  " + XMLValidator.class.getName()
            + " [ -q ] [ --help ] [ --parseonly ] [ --justCheckBom ] directory ...");
    }

    /**
     * Utility method to translate a String filename to URL.
     *
     * If the name is null, return null. If the name starts with a common URI
     * scheme (namely the ones found in the examples of RFC2396), then simply
     * return the name as-is (the assumption is that it's already a URL)
     * Otherwise we attempt (cheaply) to convert to a file:/// URL.
     *
     * @param filename
     *            a local path/filename of a file
     * @return a file:/// URL, the same string if it appears to already be a
     *         URL, or null if error
     * @throws MalformedURLException
     */
    public static String filenameToURL(String filename) throws MalformedURLException {
        // null begets null - something like the commutative property
        if (null == filename)
            return null;

        // Don't translate a string that already looks like a URL
        if (filename.startsWith("file:") || filename.startsWith("http:")
            || filename.startsWith("ftp:")
            || filename.startsWith("gopher:")
            || filename.startsWith("mailto:")
            || filename.startsWith("news:")
            || filename.startsWith("telnet:"))
            return filename;

        File f = new File(filename);
        return f.toURI().toURL().toString();
    }

    /**
     *
     * @param f file to parse
     * @return 1 if problems, 0 if OK
     */
    public int parse(File f) {

        if(checkForBOM(f)) return 1; // had BOM - fail

        if(justCheckBom) return 0; // short cut

        final String filename = PathUtilities.getNormalizedPathString(f);
        // Force filerefs to be URI's if needed: note this is independent of any
        // other files
        String docURI;
        try {
            docURI = filenameToURL(filename);
            parse(new InputSource(docURI), filename);
            return 0; // OK
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println(f.getPath() + " - fail - " + t);
            return 1; // fail
        }
    }

    Document parse(InputSource docSrc, String filename) {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        // Always set namespaces on
        if (!parseonly) {
            dfactory.setNamespaceAware(true);
            dfactory.setValidating(true);
        }
        // Set other attributes here as needed
        // applyAttributes(dfactory, attributes);

        // Local class: cheap non-printing ErrorHandler
        // This is used to suppress validation warnings
        final String filename2 = filename;
        ErrorHandler nullHandler = new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                System.err.println(filename2 + ": Warning: " + e.getMessage());

            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                int col = e.getColumnNumber();
                System.err.println(filename2 + ":" + e.getLineNumber() + (col >= 0 ? ":" + col : "")
                    + ": ERROR: Element " + e.getPublicId()
                    + " is not valid because " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                System.err.println(filename2 + ": ERROR ");
                throw e;
            }
        };

        Document doc = null;
        try {
            // First, attempt to parse as XML (preferred)...
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            // if(docBuilder.isValidating()){
            // System.out.println("The parser is a validating parser");
            // }
            doc = docBuilder.parse(docSrc);
        } catch (Throwable se) {
            // ... if we couldn't parse as XML, attempt parse as HTML...
            if (se instanceof SAXParseException) {
                SAXParseException pe = (SAXParseException) se;
                int col = pe.getColumnNumber();
                System.err.println(filename + ":" + pe.getLineNumber() + (col >= 0 ? ":" + col : "") + ": ERROR:"
                    + se.toString());
            } else {
                System.err.println(filename + ": ERROR:" + se.toString());
            }
            try {
                // @todo need to find an HTML to DOM parser we can use!!!
                // doc = someHTMLParser.parse(new InputSource(filename));
                throw new RuntimeException(filename + ": XMLComparator not HTML parser!");
            } catch (Exception e) {
                if (filename != null) {
                    // ... if we can't parse as HTML, then just parse the text
                    try {

                        // Parse as text, line by line
                        // Since we already know it should be text, this should
                        // work better than parsing by bytes.
                        FileReader fr = new FileReader(filename);
                        BufferedReader br = new BufferedReader(fr);
                        StringBuffer buffer = new StringBuffer();
                        for (;;) {
                            String tmp = br.readLine();

                            if (tmp == null) {
                                break;
                            }

                            buffer.append(tmp);
                            buffer.append("\n"); // Put in the newlines as well
                        }
                        br.close();
                        DocumentBuilder docBuilder = dfactory
                            .newDocumentBuilder();
                        doc = docBuilder.newDocument();
                        Element outElem = doc.createElement("out");
                        Text textNode = doc.createTextNode(buffer.toString());

                        // Note: will this always be a valid node? If we're
                        // parsing
                        // in as text, will there ever be cases where the diff that's
                        // done later on will fail becuase some really garbage-like
                        // text has been put into a node?
                        outElem.appendChild(textNode);
                        doc.appendChild(outElem);
                    } catch (Throwable throwable) {

                        // throwable.printStackTrace();
                    }
                }
            }
        }
        return doc;
    }

    /**
     * @return true if BOM found
     */
    private static boolean checkForBOM(File f) {
        // Check for BOM.
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                byte bytes[] = new byte[3];
                if (fis.read(bytes) == 3 &&
                    bytes[0] == (byte) 0xef &&
                    bytes[1] == (byte) 0xbb &&
                    bytes[2] == (byte) 0xbf) {
                    System.err.println(f.getPath() + ": ERROR: contains UTF-8 BOM (shouldn't happen in CLDR XML files)");
                    return true;
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (IOException ioe) { /* ignored- other branches will report an error. */
        }
        return false;
    }
}
