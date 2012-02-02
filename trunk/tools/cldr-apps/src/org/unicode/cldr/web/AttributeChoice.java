//
//  AttributeChoice.java
//  fivejay
//
//  Created by Steven R. Loomis on 10/03/2007.
//  Copyright 2007 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;



/**
* This class represents items which consist of multiple-choice attributes 
*/


public class AttributeChoice {

    static Hashtable<String,Pattern> allXpaths = new Hashtable<String,Pattern>();    // String -> matcher
    static Hashtable<String,List<String>> allValues = new Hashtable<String,List<String>>();  // String -> { true, false, ... }
    static Hashtable<String,String> allAttributes = new Hashtable<String,String>(); // commonyUsed -> used
    

    public static final String COMMONLY_USED = "//ldml/dates/timeZoneNames/metazone.*/commonlyUsed.*";
    public static final String INLIST = "//ldml/layout/inList.*";

    public static String[] setup = {  
        COMMONLY_USED, "true",
        COMMONLY_USED, "false",
        INLIST, "mixed",
        INLIST, "titlecase-firstword",
        INLIST, "titlecase-words",
    };
    
    static {
        allAttributes.put(COMMONLY_USED,"used");
        allAttributes.put(INLIST,"casing");

        for(int i=0;i<setup.length;i+=2) {
            String k = setup[i+0];
            String v = setup[i+1];
            
            List<String> valueList = null;
            if(!allXpaths.containsKey(k)) {
                allXpaths.put(k,Pattern.compile(k));
                valueList = new ArrayList<String>();
                allValues.put(k,valueList);
            } else {
                valueList = allValues.get(k);
            }
            valueList.add(v);
//System.err.println("Add " + k + "/"+v + " - " + valueList.size());
        }
        
    }
    
    public String baseXpath;
    String type;
    List<String> values;
    String attribute;
    public String[] valuesList;
    
    private AttributeChoice(String type, String baseXpath) {
        this.type = type;
        this.values = allValues.get(type);
        this.attribute = allAttributes.get(type);
        this.valuesList = (String[])values.toArray(new String[0]);
    
        // fix the baseXpath
        String value = valueOfXpath(baseXpath);
        if(value != null) {
            String attrString = attributeValueForChoice(value);
            if(baseXpath.endsWith(attrString)) {
                this.baseXpath = baseXpath.substring(0,baseXpath.length()-attrString.length());
            } else {
                throw new InternalError("Can't find "+attrString+" in " + baseXpath);
            }
        } else {
            this.baseXpath = baseXpath;
        }
//System.err.println("Initialized a " + type + " @ " + baseXpath + " ~= " + attribute);
    }
    
    public static AttributeChoice createChoice(String baseXpath) {
//    System.err.println("cc: " + baseXpath + " - matchers " + allXpaths.size());
        for(String type : allXpaths.keySet()) {
//            System.err.println("Comparing " + type + " to " + baseXpath);
            if(allXpaths.get(type).matcher(baseXpath).matches()) {
                return new AttributeChoice(type, baseXpath);
            }
        }
        return null;
    }
    
    public String valueOfXpath(String xpath) {
        for(String v : valuesList) {
            if(xpath.indexOf(attributeValueForChoice(v))!=-1) {
                return v;
            }
        }
        return null;
    }
    
    public String xpathForChoice(String choice) {
        if(!values.contains(choice)) {
            throw new IllegalArgumentException("Invalid choice for " + attribute + " - " + choice);
        }
        return baseXpath+attributeValueForChoice(choice);
    }
    
    public String attributeValueForChoice(String choice) {
        return "[@"+attribute+"=\""+choice+"\"]";
    }
}
