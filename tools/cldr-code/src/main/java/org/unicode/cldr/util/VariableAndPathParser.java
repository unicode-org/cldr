package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathException;

import org.unicode.cldr.util.XPathExpressionParser.NodeHandlingInterface;
import org.w3c.dom.Node;

/**
 * Small helper class; given a file with a specified syntax, extract variables and paths,
 * with associated information
 *
 * @author ribnitz
 *
 */
public class VariableAndPathParser<E, F> {

    /**
     * Interface that is used to extract a Key-Value-Pair from a Node
     * @author ribnitz
     *
     */
    public static interface ExtractableVariable {
        /**
         * Given a node, extract a variable definition, and return it as a Map.Entry
         * @param aNode - the node to process
         * @return a Map.Entry with the variable name as key, and its value as value
         */
        Map.Entry<String, String> extractVariable(Node aNode);
    }

    /**
     * Interface that is used to get a pair of a Paths with associated info, given a node.
     * @author ribnitz
     *
     * @param <E>
     * @param <F>
     */

    public static interface ExtractablePath<E, F> {
        /**
         * Given a DOM Node, extract the information contained therein, and return it as a Key-Value pair
         *
         * @param aNode the DOM Node to extract from
         * @return a Key/Value pair with the key and the data
         */
        Map.Entry<E, F> extractPath(Node aNode);
    }

    /**
     * XPathExpressionParser, used internally
     */
    private final XPathExpressionParser xpExpParser;
    /**
     * pointer to the implementation of the interface called for variable extraction
     */
    private ExtractableVariable variableExtractor;

    /**
     * pointer to the implementation of the interface called for path extraction
     */
    private ExtractablePath<E, F> pathExtractor;

    public ExtractableVariable getVariableExtractor() {
        return variableExtractor;
    }

    public void setVariableExtractor(ExtractableVariable variableExtractor) {
        this.variableExtractor = variableExtractor;
    }

    public ExtractablePath<E, F> getPathExtractor() {
        return pathExtractor;
    }

    public void setPathExtractor(ExtractablePath<E, F> pathExtractor) {
        this.pathExtractor = pathExtractor;
    }

    public VariableAndPathParser(File f) throws IOException {
        xpExpParser = new XPathExpressionParser(f);
    }

    public VariableAndPathParser(Reader in) throws IOException {
        xpExpParser = new XPathExpressionParser(in);
    }

    public Map<String, String> getVariables(String varsXPath) {
        final Map<String, String> variables = new HashMap<>();
        if (variableExtractor != null) {
            try {
                xpExpParser.iterateThroughNodeSet(varsXPath, new NodeHandlingInterface() {

                    @Override
                    public void handle(Node aNode) {
                        Map.Entry<String, String> curVar = variableExtractor.extractVariable(aNode);
                        if (curVar != null) {
                            variables.put(curVar.getKey(), curVar.getValue());
                        }
                    }
                });
            } catch (XPathException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return variables;
    }

    /**
     * Assemble a list of paths starting from the given position
     * @param pathsXPath
     * @return
     */
    public Map<E, F> getPaths(String pathsXPath) {
        final Map<E, F> pathsReadFromFile = new HashMap<>();
        if (pathExtractor != null) {
            try {
                xpExpParser.iterateThroughNodeSet(pathsXPath, new NodeHandlingInterface() {

                    @Override
                    public void handle(Node aNode) {
                        Map.Entry<E, F> p = pathExtractor.extractPath(aNode);
                        if (p != null) {
                            pathsReadFromFile.put(p.getKey(), p.getValue());
                        }
                    }
                });
            } catch (XPathException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return pathsReadFromFile;
    }

}