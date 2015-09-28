package org.unicode.cldr.util;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * NodeLists as returned by DOM do not support Iterators; they can be accessed by index.
 * This is a small helper class that wraps a NodeList and makes it support the Iterator interface.
 *
 * The iterator does not support item removal.
 *
 * @author ribnitz
 *
 */
public class NodeListIterator implements Iterator<Node> {
    /**
     * The NodeList to work on
     */
    private NodeList nodeList;
    /**
     * Since node lists are indexed by position, the current
     * position
     */
    private int currentPos = 0;

    public NodeListIterator(NodeList aNodeList) {
        nodeList = aNodeList;
    }

    @Override
    public boolean hasNext() {
        return (currentPos < nodeList.getLength());
    }

    @Override
    public Node next() {
        return nodeList.item(currentPos++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not support item removal");
    }

}