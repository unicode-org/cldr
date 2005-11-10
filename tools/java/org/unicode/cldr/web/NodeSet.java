//
//  NodeSet.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.util.*;

// DOM imports
import org.w3c.dom.Node;

import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;


public class NodeSet {
    public interface NodeSetTexter {
        abstract public String text(NodeSetEntry e);
    }
    public interface XpathFilter {
        abstract public boolean okay(String path);
    }
    
    /**
     * This class represents a single data item relative to a single locale 
     */
    public class NodeSetEntry {
        public String xpath = null;
        public String type;
        public String key; // for types
        boolean mainDraft = false;
        Hashtable alts = null;
        boolean isAlias = false;
        public Node main = null; // Main node. May be missing or draft.
        public Node fallback = null; // fallback from somewhere
        public String fallbackLocale = null; // fallback info
        public Object fallbackObject = null; // In case there's no real node
        boolean hasWarning = false;
        
        public NodeSetEntry(String t) {
            type = SurveyMain.pool(t);
            key = null;
        }
        public NodeSetEntry(String t, String k) {
            type = SurveyMain.pool(t);
            key = SurveyMain.pool(k);
        }
        
        public void addFallback(String locale) {
            fallbackLocale = locale;
        }
        
        public void add(Node node, String locale) {
            if(locale == null) {
                add(node);
            } else {
                fallback = node;
                fallbackLocale = locale;
            }
        }

/*        public void add(Object obj, String locale) {
            fallbackObject = obj;
            fallbackLocale = locale;
        }
*/
        public void add(Node node, boolean draft, String alt)
        {
            if((alt!=null)&&(alt.length()>0)) {
                if(alts == null) {
                    alts = new Hashtable();
                }
                alts.put(SurveyMain.pool(alt),node);
            } else {
                if(main != null) {
                    throw new RuntimeException("dup main nodes- " + type + ", " + node.toString());
                }
                main = node;
                mainDraft = draft;                
            }
        }
        
        public void add(Node node) {
            String alt = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALT);
            String testType = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            //String testKey = LDMLUtilities.getAttributeValue(node, LDMLConstants.KEY);
            boolean draft = LDMLUtilities.isNodeDraft(node);
            
            if(!testType.equals(type)) {
                throw new RuntimeException("Types don't match: " + type + ", " + testType);
            }
            add(node, draft, alt);
        }
    }

    /**
     * create an empty NodeSet
     */
    public NodeSet() { 
    }

    /**
     * add a fallback
     */
    public void add(String l, boolean isAlias, String locale) {
        NodeSetEntry nse = new NodeSetEntry(l);
        nse.isAlias = isAlias;
        nse.fallbackLocale = locale;
        add(nse);
    }
    public void add(String l) {
        add(new NodeSetEntry(l));
    }
    
    public void add(NodeSetEntry nse) {
        map.put(nse.type,nse);
    }
    
    // add an empty for this xpath
    public void addXpath(WebContext ctx, String xpath, String type) {
        NodeSetEntry nse = (NodeSetEntry)map.get(xpath);
        if(nse == null) {
            nse = new NodeSetEntry((type!=null)?type:"");
//            nse.xpath = ctx.xpt.poolx(xpath);
            nse.xpath = xpath; // TODO: replace..
            map.put(nse.xpath,nse);
        }
    }
    
    public void add(Node node) {
        add(node, null);
    }
    public void add(Node node, String locale) {
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        String key = LDMLUtilities.getAttributeValue(node, LDMLConstants.KEY);
        NodeSetEntry nse = (NodeSetEntry)map.get(type);
        if(nse == null) {
            nse = new NodeSetEntry(type, key);
            map.put(nse.type,nse);
        }
        nse.add(node, locale);            
    }
    
    /**
     * @param ctx (for debugging)
     * @param xpath xpath of the node
     * @param node the node
     * @param u A locale - null if this is the main (active) locale 
     */
    public void addFromXpath(WebContext ctx, ULocale u, String xpath, Node node, boolean draft, String alt, String type) {
        NodeSetEntry nse = (NodeSetEntry)map.get(xpath);
        if(nse == null) {
            nse = new NodeSetEntry((type!=null)?type:"");
//            nse.xpath = ctx.xpt.poolx(xpath);
            nse.xpath = xpath; // TODO: replace
            map.put(nse.xpath, nse);
            // calculate warning here??
        }
        if(u != null) {
            if((draft==false)&&(alt==null)) {
                nse.add(node, u.toString());
            } else {
                nse.addFallback(u.toString()); // mark that some fallback change will occur
            }
        } else {
            nse.add(node, draft, alt);
        }
    }
    
    public Iterator iterator() {
        return map.values().iterator();
    }

    /**
     * @param ctx (for debugging)
     */
    static void traverseTree(WebContext ctx, NodeSet s, ULocale u, Node root, String xpath,
            boolean draft, String alt, String type, XpathFilter filter) {
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String nodeName = node.getNodeName();
            String newPath = xpath + "/" + nodeName;
            String newAlt = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALT);
            if(newAlt != null) {
                alt = newAlt;
            }
            String newType = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            if(newType != null) {
                if(type != null) {
                    newType = type + "/" + newType; // a space. to allow breaks.
                }
            } else {
                newType = type;
            }
            String newDraft = LDMLUtilities.getAttributeValue(node, LDMLConstants.DRAFT);
            if((newDraft != null)&&(newDraft.equals("true"))) {
                draft = true;
            }
            for(int i=0;i<SurveyMain.distinguishingAttributes.length;i++) {
                String da = SurveyMain.distinguishingAttributes[i];
                String nodeAtt = LDMLUtilities.getAttributeValue(node,da);
                if((nodeAtt != null) && !(da.equals(LDMLConstants.ALT))) {
                    newPath = newPath + "[@"+da+"='" + nodeAtt + "']";
                }
            }
            // any values here?
            String value = LDMLUtilities.getNodeValue(node);
            if((value != null) && (value.length()>0)) {
                if(filter.okay(newPath)) {
                    if(value.startsWith(" ") || value.startsWith("\n") || value.startsWith("\t") || 
                        value.startsWith("\r")) {
    ///*srl*/            ctx.println("<tt>O " + newPath + " " + (draft?"<b>draft</b>":"") + " " + ((alt==null)?"":("<b>alt="+alt +"</b>")) + "</tt><br/>");
    ///*srl*/            ctx.println("<tt>+ (" + value + ") #" + value.length() + "</tt><br/>");
                    } else {
                        s.addFromXpath(ctx, u, newPath, node, draft, alt, newType);
                    }
                }
            }
            
            traverseTree(ctx, s, u, node, newPath, draft, alt, newType, filter);
        }
    }

    static public NodeSet loadFromXpaths(WebContext ctx, TreeMap allXpaths, XpathFilter filter) {
        NodeSet s = new NodeSet();
        if(ctx.doc.length == 0) {
            System.out.println("ctx.doc.length = 0!!");
        }
        Node roots[] = new Node[ctx.doc.length];
        int i;
        // Load the doc root for each item. 
        // This is starting to sound suspiciously like a fuil resolver.
        for(i=0;i<ctx.doc.length;i++) {
            roots[i] = ctx.doc[i];
        }

        // add all the items from the document
        if(roots[0] != null) {
            traverseTree(ctx, s, null, roots[0], "/", false, null, null, filter);
        }

        for(i=(roots.length)-1;i>0;i--) {
            if(roots[i] != null) {
                traverseTree(ctx, s, new ULocale(ctx.docLocale[i]), roots[i], "/", false, null, null, filter);
            }
        }
        
        // load all xpaths .. as dummies

        return s;
    }
    
    /**
     * load a NodeSet from the associated WebContext, given an Xpath and an optional set of full items
     * @param ctx the WebContext
     * @param xpath xpath to the requested items
     * @param fullSet optional Set listing the keys expected to be found, otherwise null
     */
    static public NodeSet loadFromPath(WebContext ctx, String xpath, Set fullSet) {
        NodeSet s = new NodeSet();
        Node roots[] = new Node[ctx.doc.length];
        int i;
        // Load the doc root for each item. 
        // This is starting to sound suspiciously like a fuil resolver.
        for(i=0;i<ctx.doc.length;i++) {
            roots[i] = LDMLUtilities.getNode(ctx.doc[i], xpath);
        }
        
        // add all the items from the document
        if(roots[0] != null) {
            for(Node node=roots[0].getFirstChild(); node!=null; node=node.getNextSibling()){
                if(node.getNodeType()!=Node.ELEMENT_NODE){
                    continue;
                }
                if(node.getNodeName().equals(LDMLConstants.ALIAS)) {
                    s.add(" aliased to " + LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE), true, LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE));
                } else {
                    s.add(node); // add regular item
                }
            }
        }

        // Sigh.  May as well do this.
        for(i=(roots.length)-1;i>0;i--) {
            //ctx.println("Loading from: " + ctx.docLocale[i] + "<br/>");
            if(roots[i] != null) for(Node node=roots[i].getFirstChild(); node!=null; node=node.getNextSibling()){
                if(node.getNodeType()!=Node.ELEMENT_NODE){
                    continue;
                }
                if(LDMLUtilities.isNodeDraft(node)) {
                    continue;
                }
                if(node.getNodeName().equals(LDMLConstants.ALIAS)) {
                    s.add(ctx.docLocale[i] + ": aliased to " + LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE), true, LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE));
                } else {
                    s.add(node, ctx.docLocale[i]); // add an item as a fallback ( w/ a locale )
                }
            }
        }

        if(fullSet != null) {
            for(Iterator e = fullSet.iterator();e.hasNext();) {
                String f = (String)e.next();
                NodeSetEntry nse = s.get(f);
                if(nse == null) {
                    s.add(f); // adds an empty item, no data.
                }
            }
        }
        return s;
    }
    
    NodeSetEntry get(String type) {
        return (NodeSetEntry)map.get(type);
    }
    
    int count() {
        return map.size();
    }
    
    /**
     * return the elements sorted according to the texter 
     */
    Map getSorted(NodeSetTexter nst) {
        if(nst == null) {
            nst = new NullTexter();
        }
        com.ibm.icu.text.Collator myCollator = com.ibm.icu.text.Collator.getInstance(new ULocale("en"));
        ((com.ibm.icu.text.RuleBasedCollator)myCollator).setNumericCollation(true);
        TreeMap m = new TreeMap(myCollator);
        for(Iterator e = iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            m.put(nst.text(f),f); // don't pool this - it's ephemeral
        }
        return m;
    }

    private HashMap map = new HashMap();
    private NodeSetTexter dispTexter = null;
}
