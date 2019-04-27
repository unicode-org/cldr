/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

public class GenerateAttributeList {
    XPathParts parts = new XPathParts();
    Map<String, Map<String, Set<String>[]>> element_attribute_valueSet = new TreeMap<String, Map<String, Set<String>[]>>();
    Set<String> allElements = new TreeSet<String>();
    Map<String, String> defaults = new HashMap<String, String>();

    public GenerateAttributeList(Factory cldrFactory) throws IOException {
        addFromStandardCodes();
        addFromDTD(CLDRPaths.COMMON_DIRECTORY + "main/en.xml");
        addFromDTD(CLDRPaths.COMMON_DIRECTORY + "supplemental/characters.xml");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "collation/");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "main/");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "rbnf/");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "segments/");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "supplemental/");
        addFromDirectory(CLDRPaths.COMMON_DIRECTORY + "transforms/");
    }

    /**
     *
     */
    private void addFromStandardCodes() {
        StandardCodes sc = StandardCodes.make();
        addFromStandardCodes(sc, "language");
        addFromStandardCodes(sc, "territory");
        addFromStandardCodes(sc, "script");
        addFromStandardCodes(sc, "variant");
        addFromStandardCodes(sc, "currency");
        addFromStandardCodes(sc, "tzid");
    }

    /**
     *
     */
    private void addFromStandardCodes(StandardCodes sc, String cat) {
        Collection<String> c = sc.getGoodAvailableCodes(cat);
        String target = cat.equals("tzid") ? "zone" : cat;
        for (Iterator<String> it = c.iterator(); it.hasNext();) {
            String item = it.next();
            add(target, "type", item, true);
        }
    }

    /**
     * @throws IOException
     *
     */
    private void addFromDTD(String filename) throws IOException {
        // StringBufferInputStream fis = new StringBufferInputStream(
        // "<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\"><ldml></ldml>");
        FileInputStream fis = new FileInputStream(filename);
        try {
            XMLReader xmlReader = CLDRFile.createXMLReader(true);
            MyDeclHandler me = new MyDeclHandler();
            xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", me);
            InputSource is = new InputSource(fis);
            is.setSystemId(filename);
            // xmlReader.setContentHandler(me);
            // xmlReader.setErrorHandler(me);
            xmlReader.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fis.close();
        }
    }

    private void addFromDirectory(String directory) throws IOException {
        File dir = new File(directory);
        directory = dir.getCanonicalPath();
        String[] files = dir.list();
        for (int i = 0; i < files.length; ++i) {
            if (files[i].startsWith(".#")) continue;
            if (!files[i].endsWith(".xml")) continue;
            String file = directory + File.separatorChar + files[i];
            if (new File(file).isDirectory()) continue;
            addFromFiles(file);
        }
    }

    private void addFromFiles(String file) throws IOException {
        // StringBufferInputStream fis = new StringBufferInputStream(
        // "<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\"><ldml></ldml>");
        System.out.println(file);
        FileInputStream fis = new FileInputStream(file);
        try {
            XMLReader xmlReader = CLDRFile.createXMLReader(true);
            xmlReader.setContentHandler(new MyContentHandler());
            InputSource is = new InputSource(fis);
            is.setSystemId(file);

            // xmlReader.setErrorHandler(me);
            // xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", me);
            xmlReader.parse(is);
        } catch (Exception e) {
            System.err.println("Failure in " + file);
            e.printStackTrace();
        } finally {
            fis.close();
        }
    }

    void add(String element, String attribute, String attributeValue, boolean dtd) {
        // fiddle the fields
        if (element.equals("generation") && attribute.equals("date"))
            attributeValue = "[date]";
        else if (element.equals("version") && attribute.equals("number"))
            attributeValue = "[revision]";
        else if (attribute.equals("draft")
            || attribute.equals("validSubLocales")
            || attribute.equals("standard")
            || attribute.equals("references"))
            element = "[common]";
        else if (attribute.equals("alt")) {
            int pos = attributeValue.indexOf("proposed");
            if (pos == 0) return;
            if (pos > 0) attributeValue = attributeValue.substring(0, pos - 1);
            element = "[common]";
        }
        // now add
        Map<String, Set<String>[]> attribute_valueSet = element_attribute_valueSet.get(element);
        if (attribute_valueSet == null) element_attribute_valueSet.put(element, attribute_valueSet = new TreeMap<String, Set<String>[]>());
        Set<String>[] valueSets = attribute_valueSet.get(attribute);
        if (valueSets == null) {
            Comparator<String> c = DtdData.getAttributeValueComparator(element, attribute);
            valueSets = new Set[2];
            valueSets[0] = new TreeSet<String>(c);
            valueSets[1] = new TreeSet<String>();
            attribute_valueSet.put(attribute, valueSets);
        }
        try {
            valueSets[dtd ? 1 : 0].add(attributeValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error with " + element
                + ", " + attribute + ", " + attributeValue + ", " + dtd, e);
        }
    }

    void show(PrintWriter pw) {
        pw.println("<html><head>");
        pw.println("<style>td,th { border-style: solid; border-width: 1; vertical-align: top }</style>");
        pw.println("</head><body>");
        pw.println("<p>Date: $" +
            "Date$</p>");
        pw.println("<p>Version: " + ToolConstants.CHART_DISPLAY_VERSION + "</p>");
        pw.println("<table>");
        pw.println("<tr><th>Element</th><th>Attribute</th><th>Actual Attribute Values</th><th>Other DTD Attribute Values</th></tr>");

        // show those with no attributes
        /*
         * Set hasNoAttributes = new TreeSet(allElements);
         * hasNoAttributes.removeAll(element_attribute_valueSet.keySet());
         * pw.print("<tr><td>");
         * pw.print(toString(hasNoAttributes));
         * pw.println("</td><td>{none}</td><td>{none}</td></tr>");
         */

        for (Iterator<String> it = element_attribute_valueSet.keySet().iterator(); it.hasNext();) {
            String element = it.next();
            Map<String, Set<String>[]> attribute_valueSet = element_attribute_valueSet.get(element);
            int size = attribute_valueSet.size();
            if (size == 0) continue;
            boolean first = true;
            for (Iterator<String> it2 = attribute_valueSet.keySet().iterator(); it2.hasNext();) {
                String attribute = it2.next();
                Set<String>[] valueSets = attribute_valueSet.get(attribute);
                pw.print("<tr>");
                if (first) {
                    first = false;
                    pw.print("<td" + (size == 1 ? "" : " rowSpan='" + attribute_valueSet.size() + "'") + ">" + element
                        + "</td>");
                }
                pw.print("<td>" + attribute + "</td><td>");
                String defaultKey = element + "|" + attribute;
                pw.print(toString(valueSets[0], defaultKey));
                pw.println("</td><td>");
                Set<String> toRemove = new TreeSet<String>(valueSets[0]);
                Set<String> remainder = new TreeSet<String>(valueSets[1]);
                remainder.removeAll(toRemove);
                pw.print(toString(remainder, defaultKey));
                pw.println("</td></tr>");
            }
        }
        pw.println("</table>");
        pw.println(CldrUtility.ANALYTICS);
        pw.println("</body></html>");
    }

    /**
     *
     */
    private String toString(Collection<String> source, String defaultKey) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator<String> it = source.iterator(); it.hasNext();) {
            String value = it.next();
            if (first)
                first = false;
            else
                result.append(", ");
            final boolean isDefault = value.equals(defaults.get(defaultKey));
            if (isDefault) {
                result.append("<b>");
            }
            result.append(TransliteratorUtilities.toHTML.transform(value));
            if (isDefault) {
                result.append("</b>");
            }
        }
        return result.toString();
    }

    class MyDeclHandler implements DeclHandler {

        Matcher idmatcher = PatternCache.get("[a-zA-Z][-_a-zA-Z0-9]*").matcher("");

        public void attributeDecl(String eName, String aName, String type, String mode, String value)
            throws SAXException {
            // System.out.println("Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
            if (value != null) {
                add(eName, aName, value, true);
                defaults.put(eName + "|" + aName, value);
            }
            idmatcher.reset(type);
            while (idmatcher.find()) {
                add(eName, aName, idmatcher.group(), true);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.DeclHandler#elementDecl(java.lang.String, java.lang.String)
         */
        public void elementDecl(String name, String model) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.DeclHandler#internalEntityDecl(java.lang.String, java.lang.String)
         */
        public void internalEntityDecl(String name, String value) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.DeclHandler#externalEntityDecl(java.lang.String, java.lang.String, java.lang.String)
         */
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
            // TODO Auto-generated method stub

        }
    }

    class MyContentHandler implements ContentHandler {

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#startDocument()
         */
        public void startDocument() throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
         */
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
         */
        public void skippedEntity(String name) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        public void setDocumentLocator(Locator locator) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
         */
        public void processingInstruction(String target, String data) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
         */
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         * org.xml.sax.Attributes)
         */
        public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
            throws SAXException {
            if (attributes == null) return;
            for (int i = 0; i < attributes.getLength(); ++i) {
                String attribute = attributes.getQName(i);
                String value = attributes.getValue(i);
                add(qName, attribute, value, false);
            }
        }
    }

    public Map<String, Map<String, Set<String>[]>> getElement_attribute_valueSet() {
        return element_attribute_valueSet;
    }
}