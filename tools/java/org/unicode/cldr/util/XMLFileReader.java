/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Convenience class to make reading XML data files easier. The main method is read();
 * This is meant for XML data files, so the contents of elements must either be all other elements, or
 * just text. It is thus not suitable for XML files with MIXED content; 
 * all text content in a mixed element is discarded.
 * @author davis
 */
public class XMLFileReader {
  static final boolean SHOW_ALL = false;
  /**
   * Handlers to use in read()
   */
  public static int CONTENT_HANDLER = 1, ERROR_HANDLER = 2, LEXICAL_HANDLER = 4, DECLARATION_HANDLER = 8;

  private MyContentHandler DEFAULT_DECLHANDLER = new MyContentHandler();
  private SimpleHandler simpleHandler;

  public static class SimpleHandler {
    public void handlePathValue(String path, String value) {};
    public void handleComment(String path, String comment) {};
    public void handleElementDecl(String name, String model) {};
    public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {};
  }

  public XMLFileReader setHandler(SimpleHandler simpleHandler) {
    this.simpleHandler = simpleHandler;
    return this;
  }

  /**
   * Read an XML file. Return a list of alternating items, where the even items are the paths,
   * and the odd ones are values. The order of the elements matches what was in the file.
   * @param fileName file to open
   * @param handlers a set of values for the handlers to use, eg CONTENT_HANDLER | ERROR_HANDLER
   * @param validating if a validating parse is requested
   * @return list of alternating values.
   */
  public XMLFileReader read(String fileName, int handlers, boolean validating) {
    try {
      InputStream fis = new FileInputStream(fileName);
      //fis = new DebuggingInputStream(fis);
      return read(fileName, new InputStreamReader(fis), handlers, validating);
    } catch (IOException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + fileName).initCause(e);
    }    	 
  }

  public XMLFileReader read(String systemID, Reader reader, int handlers, boolean validating) {
    try {
      XMLReader xmlReader = createXMLReader(validating);
      DEFAULT_DECLHANDLER.reset();
      if ((handlers & CONTENT_HANDLER) != 0) {
        xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
      }
      if ((handlers & ERROR_HANDLER) != 0) {
        xmlReader.setErrorHandler(DEFAULT_DECLHANDLER);
      }
      if ((handlers & LEXICAL_HANDLER) != 0) {
        xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
      }
      if ((handlers & DECLARATION_HANDLER) != 0) {
        xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
      }
      InputSource is = new InputSource(reader);
      is.setSystemId(systemID);
      xmlReader.parse(is);
      reader.close();
      return this;
    } catch (SAXParseException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + systemID + "\tline:\t" + e.getLineNumber()).initCause(e);
    } catch (SAXException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + systemID).initCause(e);
    } catch (IOException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + systemID).initCause(e);
    }      
  }

  private class MyContentHandler implements ContentHandler, ErrorHandler, LexicalHandler, DeclHandler {
    StringBuffer chars = new StringBuffer();
    StringBuffer commentChars = new StringBuffer();
    Stack startElements = new Stack();
    StringBuffer tempPath = new StringBuffer();
    boolean lastIsStart = false;

    public void reset() {
      chars.setLength(0);
      tempPath = new StringBuffer("/");
      startElements.clear();
      startElements.push("/");
    }
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (lastIsStart) chars.append(ch, start, length);
    }
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      tempPath.setLength(0);
      tempPath.append(startElements.peek()).append('/').append(qName);
      for (int i = 0; i < atts.getLength(); ++i) {
        tempPath.append("[@").append(atts.getQName(i)).append("=\"").append(atts.getValue(i)).append("\"]");
      }
      startElements.push(tempPath.toString());
      chars.setLength(0); // clear garbage
      lastIsStart = true;
    }
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      String startElement = (String) startElements.pop();
      if (lastIsStart) {
        //System.out.println(startElement + ":" + chars);
        simpleHandler.handlePathValue(startElement, chars.toString());
      }
      chars.setLength(0);
      lastIsStart = false;
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
      if (SHOW_ALL) Log.logln("startDTD name: " + name
              + ", publicId: " + publicId
              + ", systemId: " + systemId
      );
    }

    public void endDTD() throws SAXException {
      if (SHOW_ALL) Log.logln("endDTD");
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
      if (SHOW_ALL) Log.logln(" comment " + new String(ch, start,length));
      commentChars.append(ch, start, length);
      simpleHandler.handleComment((String) startElements.peek(), commentChars.toString());
      commentChars.setLength(0);
    }

    public void elementDecl(String name, String model) throws SAXException {
      simpleHandler.handleElementDecl(name, model);
    }
    public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
      simpleHandler.handleAttributeDecl(eName, aName, type, mode, value);
    }

    // ==== The following are just for debuggin =====

    public void startDocument() throws SAXException {
      if (SHOW_ALL) Log.logln("startDocument");
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      if (SHOW_ALL) Log.logln("ignorableWhitespace length: " + length);
    }

    public void endDocument() throws SAXException {
      if (SHOW_ALL) Log.logln("endDocument");
    }

    public void internalEntityDecl(String name, String value) throws SAXException {
      if (SHOW_ALL) Log.logln("Internal Entity\t" + name + "\t" + value);
    }
    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
      if (SHOW_ALL) Log.logln("Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
    }

    public void notationDecl (String name, String publicId, String systemId){
      if (SHOW_ALL) Log.logln("notationDecl: " + name
              + ", " + publicId
              + ", " + systemId
      );
    }

    public void processingInstruction (String target, String data)
    throws SAXException {
      if (SHOW_ALL) Log.logln("processingInstruction: " + target + ", " + data);
    }

    public void skippedEntity (String name)
    throws SAXException {
      if (SHOW_ALL) Log.logln("skippedEntity: " + name);
    }

    public void unparsedEntityDecl (String name, String publicId,
            String systemId, String notationName) {
      if (SHOW_ALL) Log.logln("unparsedEntityDecl: " + name
              + ", " + publicId
              + ", " + systemId
              + ", " + notationName
      );
    }

    public void setDocumentLocator(Locator locator) {
      if (SHOW_ALL) Log.logln("setDocumentLocator Locator " + locator);
    }
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      if (SHOW_ALL) Log.logln("startPrefixMapping prefix: " + prefix +
              ", uri: " + uri);
    }
    public void endPrefixMapping(String prefix) throws SAXException {
      if (SHOW_ALL) Log.logln("endPrefixMapping prefix: " + prefix);
    }
    public void startEntity(String name) throws SAXException {
      if (SHOW_ALL) Log.logln("startEntity name: " + name);
    }
    public void endEntity(String name) throws SAXException {
      if (SHOW_ALL) Log.logln("endEntity name: " + name);
    }
    public void startCDATA() throws SAXException {
      if (SHOW_ALL) Log.logln("startCDATA");
    }
    public void endCDATA() throws SAXException {
      if (SHOW_ALL) Log.logln("endCDATA");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException exception) throws SAXException {
      if (SHOW_ALL) Log.logln("error: " + showSAX(exception));
      throw exception;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException exception) throws SAXException {
      if (SHOW_ALL) Log.logln("fatalError: " + showSAX(exception));
      throw exception;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException exception) throws SAXException {
      if (SHOW_ALL) Log.logln("warning: " + showSAX(exception));
      throw exception;
    }
  }

  /**
   * Show a SAX exception in a readable form.
   */
  public static String showSAX(SAXParseException exception) {
    return exception.getMessage() 
    + ";\t SystemID: " + exception.getSystemId() 
    + ";\t PublicID: " + exception.getPublicId() 
    + ";\t LineNumber: " + exception.getLineNumber() 
    + ";\t ColumnNumber: " + exception.getColumnNumber() 
    ;
  }

  public static XMLReader createXMLReader(boolean validating) {
    // weiv 07/20/2007: The laundry list below is somewhat obsolete
    // I have moved the system's default parser (instantiated when "" is
    // passed) to the top, so that we will always use that. I have also
    // removed "org.apache.crimson.parser.XMLReaderImpl" as this one gets
    // confused regarding UTF-8 encoding name.
    String[] testList = {
    		System.getProperty("CLDR_DEFAULT_SAX_PARSER", ""),  // defaults to "", system default.
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
      } catch (SAXException e1) { }
    }
    if (result == null) throw new NoClassDefFoundError("No SAX parser is available, or unable to set validation correctly");
    try {
      result.setEntityResolver(new CachingEntityResolver());
    } catch (Throwable e) {
      System.err
      .println("WARNING: Can't set caching entity resolver  -  error "
              + e.toString());
      e.printStackTrace();
    }
    return result;
  }
  static final class DebuggingInputStream extends InputStream {
    InputStream contents;

    public void close() throws IOException {
      contents.close();
    }

    public DebuggingInputStream(InputStream fis) {
      contents = fis;
    }

    public int read() throws IOException {
      int x = contents.read();
      System.out.println(Integer.toHexString(x) + ",");
      return x;
    }

  }
}