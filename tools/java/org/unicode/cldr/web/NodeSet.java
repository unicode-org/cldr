//
//  NodeSet.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

// DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.lang.UCharacter;

import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;


import com.ibm.icu.lang.UCharacter;

public class NodeSet {
    public interface NodeSetTexter {
        abstract public String text(NodeSetEntry e);
    }
    
    public class NodeSetEntry {
        public String xpath = null;
        public String type;
        Vector otherAlts = new Vector();
        public Node main = null;
        public Node proposed = null;
        public NodeSetEntry(String t) {
            type = t;
        }
        public void add(Node node) {
            String alt = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALT);
            String testType = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            if(!testType.equals(type)) {
                throw new RuntimeException("Types don't match: " + type + ", " + testType);
            }
            if((alt!=null)&&(alt.length()>0)) {
                if(LDMLConstants.PROPOSED.equals(alt)) {
                    proposed = node;
                } else {
                    otherAlts.add(node);
                }
            } else {
                if(main != null) {
                    throw new RuntimeException("dup main nodes- " + type + ", " + node.toString());
                }
                main = node;
            }
        }
    }

    public NodeSet() { 
    }
    
    public void add(Node node) {
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            NodeSetEntry nse = (NodeSetEntry)map.get(type);
        if(nse == null) { 
            nse = new NodeSetEntry(type);
            map.put(type,nse);
        }
        nse.add(node);
    }
    
    public Iterator iterator() {
        return map.values().iterator();
    }
    
    static public NodeSet loadFromRoot(Node root) {
        NodeSet s = new NodeSet();
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            s.add(node);
        }
        return s;
    }
    
    int count() {
        return map.size();
    }
    
    /**
     * return the elements sorted according to the texter 
     */
    Map getSorted(NodeSetTexter nst) {
        TreeMap m = new TreeMap();
        for(Iterator e = iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            m.put(nst.text(f),f);
        }
        return m;
    }

    private HashMap map = new HashMap();
    private NodeSetTexter dispTexter = null;
}
