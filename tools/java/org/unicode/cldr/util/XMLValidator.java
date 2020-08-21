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
    public static boolean quiet = false;
    public static boolean parseonly = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("No files specified. Validation failed. Use --help for help.");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-q") || args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--help")) {
                usage();
                return;
            } else if (args[i].equals("--parseonly")) {
                System.err.println("# DTD Validation is disabled. Will only check for well formed XML.");
                parseonly = true;
            } else {
                File f = new File(args[i]);
                if (f.isDirectory()) {
                    parseDirectory(f);
                } else {
                    if (!quiet) System.out.println("Processing file " + args[i]);
                    new fileParserThread(args[i]).run();
                }
            }
        }
        if (parseonly) {
            System.err.println("# DTD Validation is disabled. Only checked for well formed XML.");
        }
    }

    private static void parseDirectory(File f) throws IOException {
        // System.err.println("Parsing directory " + f.getAbsolutePath());
        for (File s : f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                if (arg1.startsWith(".")) {
                    return false; // skip .git, .svn, ...
                }
                File n = new File(arg0, arg1);
                // System.err.println("Considering " + n.getAbsolutePath() );
                if (n.isDirectory()) {
                    try {
                        parseDirectory(n);
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
            if (!quiet) System.out.println("Processing file " + s.getPath());
            new fileParserThread(PathUtilities.getNormalizedPathString(s)).run();
        }
    }

    private static void usage() {
        System.err.println("usage:  " + XMLValidator.class.getName() + " [ -q ] [ --help ] [ --parseonly ] file ...");
        System.err.println("usage:  " + XMLValidator.class.getName()
            + " [ -q ] [ --help ] [ --parseonly ] directory ...");
    }

    /**
     * Utility method to translate a String filename to URL.
     *
     * Note: This method is not necessarily proven to get the correct URL for
     * every possible kind of filename; it should be improved. It handles the
     * most common cases that we've encountered when running Conformance tests
     * on Xalan. Also note, this method does not handle other non-file: flavors
     * of URLs at all.
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
     */
    public static String filenameToURL(String filename) {
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
        String tmp = PathUtilities.getNormalizedPathString(f);

        // URLs must explicitly use only forward slashes
        if (File.separatorChar == '\\') {
            tmp = tmp.replace('\\', '/');
        }
        // Note the presumption that it's a file reference
        // Ensure we have the correct number of slashes at the
        // start: we always want 3 /// if it's absolute
        // (which we should have forced above)
        if (tmp.startsWith("/"))
            return "file://" + tmp;
        else
            return "file:///" + tmp;

    }

    public static class fileParserThread extends Thread {
        String filename;

        fileParserThread(String _filename) {
            filename = _filename;
        }

        @Override
        public void run() {
            // Force filerefs to be URI's if needed: note this is independent of any
            // other files
            String docURI = filenameToURL(filename);
            parse(new InputSource(docURI), filename);
        }
    }

    static Document parse(InputSource docSrc, String filename) {

        // Check for BOM.
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                byte bytes[] = new byte[3];
                if (fis.read(bytes) == 3 &&
                    bytes[0] == (byte) 0xef &&
                    bytes[1] == (byte) 0xbb &&
                    bytes[2] == (byte) 0xbf) {
                    System.err.println(filename + ": ERROR: contains UTF-8 BOM (shouldn't happen in CLDR XML files)");
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (IOException ioe) { /* ignored- other branches will report an error. */
        }

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
}
