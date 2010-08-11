//
//  SupplementalData.java
//  fivejay
//
//  Created by Steven R. Loomis on 16/01/2007.
//  Copyright 2007 IBM. All rights reserved.
//
//
// TODO: replace string literals with constants

package org.unicode.cldr.util;

import org.unicode.cldr.icu.LDMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Vector;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.*;

/**
 * A class that abstracts out some of the supplementalData  
 * and parses it into more readily usable form. It is certinly not complete, 
 * and the interface should not be considered stable.
 * 
 * Generally, it tries to only parse data as it is used.
 *
 */
public class SupplementalData {

    String pathName = null;
    private Document supplementalDocument = null;
    private Document supplementalMetaDocument = null;

    static final String SPLIT_PATTERN = "[, \t\u00a0\\s]+"; // whitespace
    
    public static String[] split(String list) {
        if((list == null)||((list=list.trim()).length()==0)) {
            return new String[0];
        }
        return list.trim().split(SPLIT_PATTERN);
    }


    /**
     * Construct a new SupplementalData from the specified fileName, i.e. "...../supplemental"
     */
    public SupplementalData(String pathName) {
        this.pathName = pathName;
        supplementalDocument = LDMLUtilities.parse(pathName+"/supplementalData.xml", false);
        if(supplementalDocument == null) {
            throw new InternalError("Can't parse supplemental: " + pathName+"/supplementalData.xml");
        }
        supplementalMetaDocument = LDMLUtilities.parse(pathName+"/supplementalMetadata.xml", false);
        if(supplementalMetaDocument == null) {
            throw new InternalError("Can't parse metadata: " + pathName+"/supplementalMetadata.xml");
        }
    }
    
    /**
     * some supplemental data parsing stuff
     */
    public Document getSupplemental() {
        return supplementalDocument;
    }
    public Document getMetadata() {
        return supplementalMetaDocument;
    }


    /**
     * Territory alias parsing
     */
    Hashtable territoryAlias = null;
    
    public synchronized Hashtable getTerritoryAliases() {
        if(territoryAlias == null) {
            Hashtable ta = new Hashtable();
            NodeList territoryAliases = 
                        LDMLUtilities.getNodeList(getMetadata(), 
                        "//supplementalData/metadata/alias/territoryAlias");
            if(territoryAliases.getLength() == 0) {
                System.err.println("no territoryAliases found");
            }
            for(int i=0;i<territoryAliases.getLength();i++) {
                Node item = territoryAliases.item(i);
                
                String type = LDMLUtilities.getAttributeValue(item, LDMLConstants.TYPE);
                String replacement = LDMLUtilities.getAttributeValue(item, LDMLConstants.REPLACEMENT);
                String[] replacementList = split(replacement);
                
                ta.put(type,replacementList);
                //System.err.println(type + " -> " + replacement);
            }
            territoryAlias = ta;
        }
        return territoryAlias;
    }
    
    
    public Set getObsoleteTerritories() {
        if(territoryAlias == null) {
            getTerritoryAliases();
        }
        return territoryAlias.keySet();
    }
    
    /**
     * some containment parsing stuff
     */
     
    private Hashtable tcDown = null;  // String -> String[]
    private Hashtable tcUp = null;    // String -> String (parent)

    public String[] getContainedTerritories(String territory) {
        if(tcUp==null) {    
            parseTerritoryContainment();
        }
        return (String[])tcDown.get(territory);
    }

    public String getContainingTerritory(String territory) {
        if(tcUp==null) {    
            parseTerritoryContainment();
        }
        return (String)tcUp.get(territory);
    }

    void findParents(Hashtable u, Hashtable d, String w, int n)
    {   
        if(n==0){
            throw new InternalError("SupplementalData:findParents() recursed too deep, at "+w);
        }
        String[] children = (String[])d.get(w);
        if(children == null) {
            return;
        }
        for(int i=0;i<children.length;i++) {
            u.put(children[i],w);
            findParents(u,d,children[i],n-1);
        }
    }

    private synchronized void parseTerritoryContainment()
    {
        Set ot = getObsoleteTerritories();
        if(tcDown == null) {
            Hashtable d = new Hashtable();
            Hashtable u = new Hashtable();
            
            NodeList territoryContainment = 
                        LDMLUtilities.getNodeList(getSupplemental(), 
                        "//supplementalData/territoryContainment/group");
            for(int i=0;i<territoryContainment.getLength();i++) {
                Node item = territoryContainment.item(i);
                
                String type = LDMLUtilities.getAttributeValue(item, LDMLConstants.TYPE);
                String contains = LDMLUtilities.getAttributeValue(item, LDMLConstants.CONTAINS);
                String[] containsList = split(contains);
             
                // now, add them
                d.put(type, containsList);                
            }
            
            // link the children to the parents
            findParents(u,d,"001",15);
            
            tcUp = u;
            tcDown = d;
        }
    }

    /**
     * parse the list of used zones
     */
    private Hashtable territoryToZones = null;
    private Hashtable zoneToTerritory = null;
    
    String olsonVersion = null;
    String multiZone[] = null;
    Set multiZoneSet = null;
    
    public String getOlsonVersion() {
        if(olsonVersion==null) {
            getTerritoryToZones();
        }
        return olsonVersion;
    }

    public synchronized Hashtable getZoneToTerritory() {
        if(zoneToTerritory == null) {
            getTerritoryToZones();
        }
        return zoneToTerritory;
    }

    public synchronized Hashtable getTerritoryToZones() {
        if(territoryToZones == null) {
            Hashtable u = new Hashtable();
            Hashtable z = new Hashtable();
            NodeList zoneFormatting = 
                        LDMLUtilities.getNodeList(getSupplemental(), 
                        "//supplementalData/timezoneData/zoneFormatting");
                        
            Node zfItem = zoneFormatting.item(0);
            
            olsonVersion = LDMLUtilities.getAttributeValue(zfItem,"tzidVersion");
            multiZone = split(LDMLUtilities.getAttributeValue(zfItem,"multizone"));
            multiZoneSet = new HashSet();
            for(int i=0;i<multiZone.length;i++) {
                multiZoneSet.add(multiZone[i]);
            }
            
            NodeList terrToZones = 
                        LDMLUtilities.getNodeList(getSupplemental(), 
                        "//supplementalData/timezoneData/zoneFormatting/zoneItem");
            for(int i=0;i<terrToZones.getLength();i++) {
                Node item = terrToZones.item(i);
                
                String type = LDMLUtilities.getAttributeValue(item, LDMLConstants.TYPE);
                String territory = LDMLUtilities.getAttributeValue(item, LDMLConstants.TERRITORY);
                // do we care about alias?
                
                //String[] containsList = split(contains);
                Vector v = (Vector)u.get(territory);
                if(v==null) { 
                    v = new Vector();
                    u.put(territory,v);
                }
                v.add(type);
                z.put(type,territory);
            }
            
            // devectorize?        
            zoneToTerritory=z;
            territoryToZones = u;
        }
        return territoryToZones;
    }
    
    public String resolveParsedMetazone ( String metazone, String territory )
    {
       Node mzLookup = null; 
       String result = "Etc/GMT";

       mzLookup = LDMLUtilities.getNode(getSupplemental(), "//supplementalData/timezoneData/mapTimezones[@type=\"metazones\"]/mapZone[@other=\""+metazone+"\"][@territory=\""+territory+"\"]");

       if (mzLookup == null ) {
          mzLookup = LDMLUtilities.getNode(getSupplemental(),"//supplementalData/timezoneData/mapTimezones[@type=\"metazones\"]/mapZone[@other=\""+metazone+"\"][@territory=\"001\"]");
 
          if (mzLookup == null ) {
             return result;
          }
       }

       result = LDMLUtilities.getAttributeValue(mzLookup,LDMLConstants.TYPE);
       return result;
    }

    // Default Content Locales
    private Hashtable defaultContentToParentHash = null; // list of default content locale IDs -> 'parent ID'
    private Hashtable defaultContentToChildHash = null; // list of default content source IDs -> 'which default content locale ID'
    
    public String defaultContentToParent(String child) {
        if(defaultContentToParentHash == null) {
            defaultContentToChild(""); // so that it will initialize
        }
        return (String)defaultContentToParentHash.get(child);
    }
    
    public synchronized String defaultContentToChild(String parent) {
        if(defaultContentToChildHash == null) {
            Hashtable c2p = new Hashtable(); // child to parent
            Hashtable p2c = new Hashtable(); // parent to child
            
            NodeList defaultContent = 
                        LDMLUtilities.getNodeList(getMetadata(), 
                        "//supplementalData/metadata/defaultContent");
                        
            Node defaultContentItem = defaultContent.item(0);
            
            String localesList = LDMLUtilities.getAttributeValue(defaultContentItem,"locales");
            
            // we assume locales is sorted, with the later entries being more leaf nodes.
            String[] locales = split(localesList);
            
            Set localeSet = new HashSet(); // for containment purposes
            for(int i=0;i<locales.length;i++) {
               localeSet.add(locales[i]);
            }
            
            for(int i=0;i<locales.length;i++) {
                String child = locales[i];

                // Find a parent of this locale which is NOT itself also a defaultContent
                String nextParent=LDMLUtilities.getParent(child);
                while(nextParent!=null) {
                    if (!localeSet.contains(nextParent)) { // Did we find a parent that's also not itself a defaultContent?
                        break;
                    }
                    nextParent=LDMLUtilities.getParent(nextParent);  // loop over parent localse
                }
                // parent 
                if(nextParent == null) {
                    throw new InternalError("SupplementalData.defaultContentToChild(): No valid parent for " + child);
                }
                
                c2p.put(child, nextParent);  //  wo_Arab_SN -> wo
                String oldChild = (String)p2c.get(nextParent);
                if(oldChild != null) {
                    String childParent = LDMLUtilities.getParent(child);
                    if(!childParent.equals(oldChild)) {
                        throw new InternalError("SupplementalData.defaultContentToChild(): defaultContent list in wrong order? Tried to map "+nextParent + " -> " + child + ", replacing " + oldChild + " (should have been " + childParent+")");
                    }
                }
                p2c.put(nextParent, child); // wo -> wo_Arab_SN
            }
            // done, save the hashtables..
            defaultContentToChildHash = p2c;
            defaultContentToParentHash = c2p;
        }
        // Normal case: return the lookedup value
        return (String)defaultContentToChildHash.get(parent);
    }
    
    Hashtable validityVariables = new Hashtable();
    
    // variables 
    synchronized String getValidityVariable(String id, String type) {
        String key = id+"/"+type;
        String ret = (String)validityVariables.get(key);
        if(ret != null) {
            return ret;
        }
    
        try {
            NodeList defaultContent = 
                    LDMLUtilities.getNodeList(getMetadata(), 
                    "//supplementalData/metadata/validity/variable");
                    
            int len = defaultContent.getLength();
            for(int i=0;i<len;i++) {
                Node defaultContentItem = defaultContent.item(i);
                
                String nodeId = LDMLUtilities.getAttributeValue(defaultContentItem,"id");
                if(!id.equals(nodeId)) {
                    continue;
                }
                String nodeType = LDMLUtilities.getAttributeValue(defaultContentItem,"type");
                if(!type.equals(nodeType)) {
                    continue;
                }
                
                ret = LDMLUtilities.getNodeValue(defaultContentItem);

                validityVariables.put(key, ret);
                return ret;
            }
        } catch ( Throwable t ) {
            System.err.println("Looking for validity variable " + id + "/"+type + " -> " + t.toString());
            t.printStackTrace();
            throw new InternalError("Looking for validity variable " + id + "/"+type + " -> " + t.toString());
        }
        return null;
    }
    
    public String[] getValidityChoice(String id) {
        return split(getValidityVariable(id, "choice"));
    }
    
    public Pattern getValidityRegex(String id) {
        String regex = getValidityVariable(id, "regex");
        if(regex == null) {
            return null;
        } else {
            return Pattern.compile(regex);
        }
    }
}
