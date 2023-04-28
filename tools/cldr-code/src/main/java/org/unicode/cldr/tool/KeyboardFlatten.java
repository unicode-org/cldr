package org.unicode.cldr.tool;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.XMLValidator;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Read a Keyboard and write it out with no import statements */
public class KeyboardFlatten {
    public static void flatten(String path, OutputStream stream)
            throws MalformedURLException, SAXException, TransformerConfigurationException,
                    TransformerException, TransformerFactoryConfigurationError {
        final String filename = PathUtilities.getNormalizedPathString(path);
        // Force filerefs to be URI's if needed: note this is independent of any
        // other files
        String docURI;
        docURI = XMLValidator.filenameToURL(filename);
        flatten(new InputSource(docURI), filename, stream);
    }

    public static void flatten(InputSource inputSource, String filename, OutputStream stream)
            throws SAXException, TransformerConfigurationException, TransformerException,
                    TransformerFactoryConfigurationError, MalformedURLException {
        final DocumentBuilderFactory dfactory = getKeyboardDocFactory();
        final ErrorHandler nullHandler = getNullHandler(filename);
        // Parse
        Document doc = parseDocument(inputSource, filename, dfactory, nullHandler);

        // do the flatten
        flattenDoc(dfactory, doc);

        // Write out
        write(doc, stream);
    }

    private static void flattenDoc(final DocumentBuilderFactory dfactory, Document doc)
            throws MalformedURLException {
        // Now, flatten it
        NodeList imports = doc.getElementsByTagName("import");

        if (imports.getLength() == 0) {
            System.err.println("No imports");
        } else {
            for (int i = 0; i < imports.getLength(); i++) {
                Node item = imports.item(i);
                flattenImport(dfactory, doc, item);
            }
            // now remove the import nodes
            while (imports.getLength() > 0) {
                Node item = imports.item(0);
                item.getParentNode().removeChild(item);
                // item is no longer in list
            }
        }
    }

    private static void flattenImport(
            final DocumentBuilderFactory dfactory, Document doc, Node item)
            throws MalformedURLException {
        final String base = getBase(item);
        final String path = getPath(item);
        System.err.println("Import: " + base + ":" + path);
        if (base.equals("cldr")) {
            if (path.startsWith("techpreview/")) {
                final String subpath = path.replaceFirst("techpreview/", "");
                final File importDir =
                        new File(
                                CLDRConfig.getInstance().getCldrBaseDirectory(),
                                "keyboards/import");
                final File importFile = new File(importDir, subpath);
                applyImportFile(dfactory, doc, item, path, importFile);
            } else {
                throw new IllegalArgumentException("Unknown cldr base: " + path);
            }
        } else {
            throw new IllegalArgumentException("Unknown base: " + base);
        }
    }

    private static void applyImportFile(
            final DocumentBuilderFactory dfactory,
            Document doc,
            Node item,
            final String path,
            final File importFile)
            throws MalformedURLException {
        if (!importFile.exists()) {
            throw new IllegalArgumentException("File " + importFile + " does not exist");
        }
        System.err.println("Importing: " + importFile.getAbsolutePath());
        final String ifilename =
                PathUtilities.getNormalizedPathString(importFile.getAbsolutePath());
        // Force filerefs to be URI's if needed: note this is independent of any
        // other files
        String docURI;
        docURI = XMLValidator.filenameToURL(ifilename);

        Document importDoc =
                parseDocument(
                        new InputSource(docURI), ifilename, dfactory, getNullHandler(ifilename));
        System.err.println("Parsed import OK");
        // Now perform the import

        // Validate the root element
        final Element importedRoot = importDoc.getDocumentElement();
        final Node importParentNode = item.getParentNode();
        if (importParentNode.getNodeType() != Node.ELEMENT_NODE) {
            throw new IllegalArgumentException("import parent is not an element");
        }
        final Element importParent = (Element) importParentNode;
        // Elements must be same name
        if (!importParent.getTagName().equals(importedRoot.getTagName())) {
            throw new IllegalArgumentException(
                    "trying to import "
                            + importedRoot.getTagName()
                            + " root into child of "
                            + importParent.getTagName());
        }
        System.err.println("Importing into " + importParent.getTagName());

        Comment preComment = doc.createComment("Begin Imports from " + path);
        Comment postComment = doc.createComment("End Imports from " + path);

        // OK here we go
        NodeList moveChildren = importedRoot.getChildNodes();
        importParent.insertBefore(preComment, item);
        for (int j = 0; j < moveChildren.getLength(); j++) {
            final Node child = moveChildren.item(j);
            final Node clone = doc.importNode(child, true);
            importParent.insertBefore(clone, item);
        }
        System.err.println("Moved " + moveChildren.getLength() + " children");
        // Add a comment
        importParent.insertBefore(postComment, item);

        // done
    }

    private static ErrorHandler getNullHandler(final String filename2) {
        ErrorHandler nullHandler =
                new ErrorHandler() {
                    @Override
                    public void warning(SAXParseException e) throws SAXException {
                        System.err.println(filename2 + ": Warning: " + e.getMessage());
                    }

                    @Override
                    public void error(SAXParseException e) throws SAXException {
                        int col = e.getColumnNumber();
                        System.err.println(
                                filename2
                                        + ":"
                                        + e.getLineNumber()
                                        + (col >= 0 ? ":" + col : "")
                                        + ": ERROR: Element "
                                        + e.getPublicId()
                                        + " is not valid because "
                                        + e.getMessage());
                    }

                    @Override
                    public void fatalError(SAXParseException e) throws SAXException {
                        System.err.println(filename2 + ": ERROR ");
                        throw e;
                    }
                };
        return nullHandler;
    }

    private static DocumentBuilderFactory getKeyboardDocFactory() throws SAXException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        // Always set namespaces on
        dfactory.setNamespaceAware(true);
        dfactory.setValidating(true);
        SchemaFactory sfac = SchemaFactory.newDefaultInstance();
        Schema schema =
                sfac.newSchema(
                        new File(
                                CLDRConfig.getInstance().getCldrBaseDirectory(),
                                DtdType.keyboard.getXsdPath()));
        dfactory.setSchema(schema);
        // Set other attributes here as needed
        // applyAttributes(dfactory, attributes);
        return dfactory;
    }

    private static String getBase(Node item) {
        final String attrName = "base";
        return getAttributeValue(item, attrName);
    }

    private static String getPath(Node item) {
        final String attrName = "path";
        return getAttributeValue(item, attrName);
    }

    private static String getAttributeValue(Node item, final String attrName) {
        return item.getAttributes().getNamedItem(attrName).getTextContent();
    }

    private static Document parseDocument(
            InputSource inputSource,
            String filename,
            DocumentBuilderFactory dfactory,
            ErrorHandler nullHandler) {
        Document doc = null;
        try {
            // First, attempt to parse as XML (preferred)...
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setErrorHandler(nullHandler);
            // if(docBuilder.isValidating()){
            // System.out.println("The parser is a validating parser");
            // }
            doc = docBuilder.parse(inputSource);
        } catch (Throwable se) {
            if (se instanceof SAXParseException) {
                SAXParseException pe = (SAXParseException) se;
                int col = pe.getColumnNumber();
                System.err.println(
                        filename
                                + ":"
                                + pe.getLineNumber()
                                + (col >= 0 ? ":" + col : "")
                                + ": ERROR:"
                                + se.toString());
            } else {
                System.err.println(filename + ": ERROR:" + se.toString());
            }
        }
        System.err.println("Doc parse OK");
        return doc;
    }

    /**
     * Serialize XML out to stream
     *
     * @param doc
     */
    private static void write(Document doc, OutputStream stream)
            throws TransformerConfigurationException, TransformerException,
                    TransformerFactoryConfigurationError {
        TransformerFactory.newInstance()
                .newTransformer()
                .transform(new DOMSource(doc), new StreamResult(stream));
    }
}
