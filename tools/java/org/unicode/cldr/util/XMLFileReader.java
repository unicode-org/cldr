/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.base.Function;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * Convenience class to make reading XML data files easier. The main method is read();
 * This is meant for XML data files, so the contents of elements must either be all other elements, or
 * just text. It is thus not suitable for XML files with MIXED content;
 * all text content in a mixed element is discarded.
 *
 * @author davis
 */
public class XMLFileReader {
    static final boolean SHOW_ALL = false;
    /**
     * Handlers to use in read()
     */
    public static int CONTENT_HANDLER = 1, ERROR_HANDLER = 2, LEXICAL_HANDLER = 4, DECLARATION_HANDLER = 8;

    private MyContentHandler DEFAULT_DECLHANDLER = new MyContentHandler();
    // TODO Add way to skip gathering value contents
    // private ElementOnlyContentHandler ELEMENT_ONLY_DECLHANDLER = new ElementOnlyContentHandler();
    private SimpleHandler simpleHandler;

    public static class SimpleHandler {
        public void handlePathValue(String path, String value) {
        }

        public void handleComment(String path, String comment) {
        }

        public void handleElementDecl(String name, String model) {
        }

        public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
        }

        public void handleEndDtd() {
        }

        public void handleStartDtd(String name, String publicId, String systemId) {
        }
    }

    public XMLFileReader setHandler(SimpleHandler simpleHandler) {
        this.simpleHandler = simpleHandler;
        return this;
    }

    /**
     * Read an XML file. The order of the elements matches what was in the file.
     *
     * @param fileName
     *            file to open
     * @param handlers
     *            a set of values for the handlers to use, eg CONTENT_HANDLER | ERROR_HANDLER
     * @param validating
     *            if a validating parse is requested
     * @return list of alternating values.
     */
    public XMLFileReader read(String fileName, int handlers, boolean validating) {
        try (InputStream fis = new FileInputStream(fileName);
            ) {
            return read(fileName, new InputSource(fis), handlers, validating);
        } catch (IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + fileName).initCause(e);
        }
    }


    /**
     * read from a CLDR resource
     * @param fileName
     * @param handlers
     * @param validating
     * @param fis
     * @see CldrUtility#getInputStream(String)
     * @return
     */
    public XMLFileReader readCLDRResource(String resName, int handlers, boolean validating) {
        try (InputStream inputStream = CldrUtility.getInputStream(resName)) {
            return read(resName, new InputSource(inputStream), handlers, validating);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * read from an arbitrary
     * @param fileName
     * @param handlers
     * @param validating
     * @param fis
     * @see CldrUtility#getInputStream(String)
     * @return
     */
    public XMLFileReader read(String resName, Class<?> callingClass, int handlers, boolean validating) {
        try (InputStream inputStream = CldrUtility.getInputStream(callingClass, resName)) {
            return read(resName, new InputSource(inputStream), handlers, validating);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public XMLFileReader read(String systemID, Reader reader, int handlers, boolean validating) {
        read(systemID, reader, handlers, validating, DEFAULT_DECLHANDLER.reset());
        return this;
    }

    public XMLFileReader read(String systemID, InputSource insrc, int handlers, boolean validating) {
        read(systemID, insrc, handlers, validating, DEFAULT_DECLHANDLER.reset());
        return this;
    }

    public static void read(String systemID, InputStream instr, int handlers, boolean validating, AllHandler allHandler) {
        InputSource is = new InputSource(instr);
        read(systemID, is, handlers, validating, allHandler);
    }

    public static void read(String systemID, Reader reader, int handlers, boolean validating, AllHandler allHandler) {
        InputSource is = new InputSource(reader);
        read(systemID, is, handlers, validating, allHandler);
    }

    public static void read(String systemID, InputSource is, int handlers, boolean validating, AllHandler allHandler) {
        try {
            XMLReader xmlReader = createXMLReader(handlers, validating, allHandler);
            is.setSystemId(systemID);
            try {
                xmlReader.parse(is);
            } catch (AbortException e) {
            } // ok
        } catch (SAXParseException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + systemID + "\tline:\t"
                + e.getLineNumber()).initCause(e);
        } catch (SAXException | IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + systemID).initCause(e);
        }
    }

    private static final XMLReader createXMLReader(int handlers, boolean validating, AllHandler allHandler) throws SAXNotRecognizedException, SAXNotSupportedException {
        XMLReader xmlReader = createXMLReader(validating);
        if ((handlers & CONTENT_HANDLER) != 0) {
            xmlReader.setContentHandler(allHandler);
        }
        if ((handlers & ERROR_HANDLER) != 0) {
            xmlReader.setErrorHandler(allHandler);
        }
        if ((handlers & LEXICAL_HANDLER) != 0) {
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", allHandler);
        }
        if ((handlers & DECLARATION_HANDLER) != 0) {
            xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", allHandler);
        }
        return xmlReader;
    }

    public interface AllHandler extends ContentHandler, LexicalHandler, DeclHandler, ErrorHandler {

    }


    /** Basis for handlers that provides for logging, with no actions on methods
     */
    static public class LoggingHandler implements AllHandler {
        @Override
        public void startDocument() throws SAXException {
            if (SHOW_ALL) Log.logln("startDocument");
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL) Log.logln("characters");
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
            throws SAXException {
            if (SHOW_ALL) Log.logln("startElement");
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (SHOW_ALL) Log.logln("endElement");
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if (SHOW_ALL) Log.logln("startDTD");
        }

        @Override
        public void endDTD() throws SAXException {
            if (SHOW_ALL) Log.logln("endDTD");
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL) Log.logln(" comment " + new String(ch, start, length));
        }

        @Override
        public void elementDecl(String name, String model) throws SAXException {
            if (SHOW_ALL) Log.logln("elementDecl");
        }

        @Override
        public void attributeDecl(String eName, String aName, String type, String mode, String value)
            throws SAXException {
            if (SHOW_ALL) Log.logln("attributeDecl");
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL) Log.logln("ignorableWhitespace length: " + length);
        }

        @Override
        public void endDocument() throws SAXException {
            if (SHOW_ALL) Log.logln("endDocument");
        }

        @Override
        public void internalEntityDecl(String name, String value) throws SAXException {
            if (SHOW_ALL) Log.logln("Internal Entity\t" + name + "\t" + value);
        }

        @Override
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
            if (SHOW_ALL) Log.logln("Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void notationDecl(String name, String publicId, String systemId) {
            if (SHOW_ALL) Log.logln("notationDecl: " + name
                + ", " + publicId
                + ", " + systemId);
        }

        @Override
        public void processingInstruction(String target, String data)
            throws SAXException {
            if (SHOW_ALL) Log.logln("processingInstruction: " + target + ", " + data);
        }

        @Override
        public void skippedEntity(String name)
            throws SAXException {
            if (SHOW_ALL) Log.logln("skippedEntity: " + name);
        }

        public void unparsedEntityDecl(String name, String publicId,
            String systemId, String notationName) {
            if (SHOW_ALL) Log.logln("unparsedEntityDecl: " + name
                + ", " + publicId
                + ", " + systemId
                + ", " + notationName);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            if (SHOW_ALL) Log.logln("setDocumentLocator Locator " + locator);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (SHOW_ALL) Log.logln("startPrefixMapping prefix: " + prefix +
                ", uri: " + uri);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            if (SHOW_ALL) Log.logln("endPrefixMapping prefix: " + prefix);
        }

        @Override
        public void startEntity(String name) throws SAXException {
            if (SHOW_ALL) Log.logln("startEntity name: " + name);
        }

        @Override
        public void endEntity(String name) throws SAXException {
            if (SHOW_ALL) Log.logln("endEntity name: " + name);
        }

        @Override
        public void startCDATA() throws SAXException {
            if (SHOW_ALL) Log.logln("startCDATA");
        }

        @Override
        public void endCDATA() throws SAXException {
            if (SHOW_ALL) Log.logln("endCDATA");
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error(SAXParseException exception) throws SAXException {
            if (SHOW_ALL) Log.logln("error: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            if (SHOW_ALL) Log.logln("fatalError: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            if (SHOW_ALL) Log.logln("warning: " + showSAX(exception));
            throw exception;
        }

    }

    public class MyContentHandler extends LoggingHandler {
        StringBuffer chars = new StringBuffer();
        StringBuffer commentChars = new StringBuffer();
        Stack<String> startElements = new Stack<>();
        StringBuffer tempPath = new StringBuffer();
        boolean lastIsStart = false;

        public MyContentHandler reset() {
            chars.setLength(0);
            tempPath = new StringBuffer("/");
            startElements.clear();
            startElements.push("/");
            return this;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (lastIsStart) chars.append(ch, start, length);
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
            throws SAXException {
            tempPath.setLength(0);
            tempPath.append(startElements.peek()).append('/').append(qName);
            for (int i = 0; i < atts.getLength(); ++i) {
                tempPath.append("[@").append(atts.getQName(i)).append("=\"").append(atts.getValue(i).replace('"', '\'')).append("\"]");
            }
            startElements.push(tempPath.toString());
            chars.setLength(0); // clear garbage
            lastIsStart = true;
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            String startElement = startElements.pop();
            if (lastIsStart) {
                // System.out.println(startElement + ":" + chars);
                simpleHandler.handlePathValue(startElement, chars.toString());
            }
            chars.setLength(0);
            lastIsStart = false;
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if (SHOW_ALL) Log.logln("startDTD name: " + name
                + ", publicId: " + publicId
                + ", systemId: " + systemId);
            simpleHandler.handleStartDtd(name, publicId, systemId);
        }

        @Override
        public void endDTD() throws SAXException {
            if (SHOW_ALL) Log.logln("endDTD");
            simpleHandler.handleEndDtd();
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL) Log.logln(" comment " + new String(ch, start, length));
            commentChars.append(ch, start, length);
            simpleHandler.handleComment(startElements.peek(), commentChars.toString());
            commentChars.setLength(0);
        }

        @Override
        public void elementDecl(String name, String model) throws SAXException {
            simpleHandler.handleElementDecl(name, model);
        }

        @Override
        public void attributeDecl(String eName, String aName, String type, String mode, String value)
            throws SAXException {
            simpleHandler.handleAttributeDecl(eName, aName, type, mode, value);
        }

    }

    static final class AbortException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Show a SAX exception in a readable form.
     */
    public static String showSAX(SAXParseException exception) {
        return exception.getMessage()
            + ";\t SystemID: " + exception.getSystemId()
            + ";\t PublicID: " + exception.getPublicId()
            + ";\t LineNumber: " + exception.getLineNumber()
            + ";\t ColumnNumber: " + exception.getColumnNumber();
    }

    public static XMLReader createXMLReader(boolean validating) {
        // weiv 07/20/2007: The laundry list below is somewhat obsolete
        // I have moved the system's default parser (instantiated when "" is
        // passed) to the top, so that we will always use that. I have also
        // removed "org.apache.crimson.parser.XMLReaderImpl" as this one gets
        // confused regarding UTF-8 encoding name.
        String[] testList = {
            System.getProperty("CLDR_DEFAULT_SAX_PARSER", ""), // defaults to "", system default.
            "org.apache.xerces.parsers.SAXParser",
            "gnu.xml.aelfred2.XmlReader",
            "com.bluecast.xml.Piccolo",
            "oracle.xml.parser.v2.SAXParser"
        };
        XMLReader result = null;
        for (int i = 0; i < testList.length; ++i) {
            try {
                result = (testList[i].length() != 0)
                    ? XMLReaderFactory.createXMLReader(testList[i])
                        : XMLReaderFactory.createXMLReader();
                    result.setFeature("http://xml.org/sax/features/validation", validating);
                    break;
            } catch (SAXException e1) {
            }
        }
        if (result == null)
            throw new NoClassDefFoundError("No SAX parser is available, or unable to set validation correctly");
        return result;
    }

    static final class DebuggingInputStream extends InputStream {
        InputStream contents;

        @Override
        public void close() throws IOException {
            contents.close();
        }

        public DebuggingInputStream(InputStream fis) {
            contents = fis;
        }

        @Override
        public int read() throws IOException {
            int x = contents.read();
            System.out.println(Integer.toHexString(x) + ",");
            return x;
        }
    }

    public static List<Pair<String, String>> loadPathValues(String filename, List<Pair<String, String>> data, boolean validating) {
        return loadPathValues(filename, data, validating, false);
    }

    public static List<Pair<String, String>> loadPathValues(String filename, List<Pair<String, String>> data, boolean validating, boolean full) {
        return loadPathValues(filename, data, validating, full, null);
    }

    public static List<Pair<String, String>> loadPathValues(String filename, List<Pair<String, String>> data, boolean validating, boolean full,
        Function<String, String> valueFilter) {
        try {
            new XMLFileReader()
            .setHandler(new PathValueListHandler(data, full, valueFilter))
            .read(filename, -1, validating);
            return data;
        } catch (Exception e) {
            throw new ICUException(filename, e);
        }
    }

    public static void processPathValues(String filename, boolean validating, SimpleHandler simpleHandler) {
        try {
            new XMLFileReader()
            .setHandler(simpleHandler)
            .read(filename, -1, validating);
        } catch (Exception e) {
            throw new ICUException(filename, e);
        }
    }

    static final class PathValueListHandler extends SimpleHandler {
        List<Pair<String, String>> data;
        boolean full;
        private Function<String, String> valueFilter;

        public PathValueListHandler(List<Pair<String, String>> data, boolean full, Function<String, String> valueFilter) {
            super();
            this.data = data != null ? data : new ArrayList<>();
            this.full = full;
            this.valueFilter = valueFilter;
        }

        @Override
        public void handlePathValue(String path, String value) {
            if (valueFilter == null) {
                data.add(Pair.of(path, value));
            } else {
                String filteredValue = valueFilter.apply(value);
                if (filteredValue != null) {
                    data.add(Pair.of(path, filteredValue));
                }
            }
        }

        @Override
        public void handleComment(String path, String comment) {
            if (!full || path.equals("/")) {
                return;
            }
            data.add(Pair.of("!", comment));
        }
    }
}
