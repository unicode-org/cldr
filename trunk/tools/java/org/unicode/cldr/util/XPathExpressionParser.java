package org.unicode.cldr.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class that offers different methods of "evaluating" an XPath expression against a document provided and of
 * iterating through the result of the evaluation
 *
 * @author ribnitz
 *
 * @param <E>
 * @param <F>
 */
public class XPathExpressionParser {

    /**
     * Buffer which holds the contents to work on, initialized once, when it is read from file. Preferred over
     * private final File f, to prevent re-reading the file on every request
     */
    private final byte[] buf;

    /**
     * Interface for handling 'simple' content types, that are not Nodes, or for processing Nodes/NodeSets oneself
     * @author ribnitz
     *
     * @param <G>
     */
    public static interface SimpleContentHandlingInterface<G> {

        void handle(G result);
    }

    /**
     * Interface for handling Nodes/NodeSets; in the case of NodeSets call will be made for each node separately
     * @author ribnitz
     *
     */

    public static interface NodeHandlingInterface extends SimpleContentHandlingInterface<Node> {

    }

    private Document getDocument(InputStream is) throws SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            return dbf.newDocumentBuilder().parse(is);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initialize by reading the file specified
     * @param f
     * @throws IOException
     */
    public XPathExpressionParser(File f) throws IOException {
        buf = Files.readAllBytes(f.toPath());
    }

    /**
     * Create an expression parser using the Reader given
     * @param rdr
     * @throws IOException
     */
    public XPathExpressionParser(Reader rdr) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(rdr)) {
            String s = null;
            while ((s = br.readLine()) != null) {
                sb.append(s);
            }
        }
        buf = sb.toString().getBytes();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void evaluateWithXPathFixture(String xPathString, QName expectedResult, boolean iterate, SimpleContentHandlingInterface handler)
        throws XPathExpressionException {
        if (handler != null) {
            try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(buf))) {
                Document doc = getDocument(is);
                XPathFactory xpFact = XPathFactory.newInstance();
                XPath xp = xpFact.newXPath();
                Object result = xp.compile(xPathString).evaluate(doc, expectedResult);
                if (expectedResult == XPathConstants.NODESET && iterate) {
                    if (result instanceof NodeList) {
                        NodeList nl = (NodeList) result;
                        Iterator<Node> nlIter = new NodeListIterator(nl);
                        while (nlIter.hasNext()) {
                            handler.handle(nlIter.next());
                        }
                    }
                } else {
                    handler.handle(result);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Evaluate the xPathString with the expected result type, and pass the result to handler.
     * @param xPathString
     * @param expectedResult
     * @param handler
     * @throws XPathException
     */
    public void evaluate(String xPathString, QName expectedResult, SimpleContentHandlingInterface<?> handler) throws XPathException {
        evaluateWithXPathFixture(xPathString, expectedResult, false, handler);
    }

    /**
     * Evaluate this xPathString, and feed the result to the handler. The result is assumed to be a Node.
     * @param xPathString
     * @param handler
     * @throws XPathException
     */
    public void evaluateToNode(String xPathString, NodeHandlingInterface handler) throws XPathException {
        iterate(xPathString, XPathConstants.NODE, handler);
    }

    /**
     * Internal method that gets the ResultSet identified by the xPathString, and that calls the
     * handler for each node.
     * @param xPathString
     * @param handler
     * @throws XPathException
     */
    public void iterate(String xPathString, QName expectedReturnType, NodeHandlingInterface handler) throws XPathException {
        evaluateWithXPathFixture(xPathString, expectedReturnType, true, handler);
    }

    /**
     * Evaluate the expression which is expected to return a NodeSet and iterate through the result
     * the handler will be called for each Node encountered.
     *
     * @param xPathExpression
     * @param handler
     * @throws XPathException
     */
    public void iterateThroughNodeSet(String xPathExpression, NodeHandlingInterface handler) throws XPathException {
        iterate(xPathExpression, XPathConstants.NODESET, handler);
    }
}