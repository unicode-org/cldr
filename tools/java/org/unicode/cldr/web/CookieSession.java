/* Copyright (C) 2004, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               */
//
//  CookieJar.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/17/2005.
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


import com.fastcgi.FCGIInterface;
import com.fastcgi.FCGIGlobalDefs;
import com.ibm.icu.lang.UCharacter;

public class CookieSession {
    public String id;
    public long last;
    public Hashtable stuff = new Hashtable();  // user data
    public Hashtable prefs = new Hashtable(); // user prefs
    public UserRegistry.User user = null;
    
    private CookieSession(String s) {
        id = s;
    }
    
    static Hashtable gHash = new Hashtable();
    static Hashtable uHash = new Hashtable();
    
    public static CookieSession retrieve(String s) {
        CookieSession c = (CookieSession)gHash.get(s);
        if(c != null) {
            c.touch();
        }
        return c;
    }
    
    public static CookieSession retrieveUser(String id) {
        CookieSession c = (CookieSession)uHash.get(id);
        if(c != null) {
            c.touch();
        }
        return c;
    }
    
    public void setUser(UserRegistry.User u) {
        user = u;
        uHash.put(user.id, this); // replaces any existing session by this user.
    }
    
    public CookieSession(boolean isGuest) {
        id = newId(isGuest);
        touch();
        gHash.put(id,this);
    }
    
    protected void touch() {
        last = System.currentTimeMillis();
    }
    
    public void remove() {
        if(user != null) {
            uHash.remove(user.id);
        }
        gHash.remove(id);
    }
    
    protected long age() {
        return (System.currentTimeMillis()-last);
    }
    
    static int n = 4000;
    static int g = 8000;
    static int h = 90;
    static String j = cheapEncode(System.currentTimeMillis());
    
    protected String newId(boolean isGuest) {  
        if(isGuest) {
            // no reason, just a different set of hashes
            return cheapEncode(h+=2)+"w"+cheapEncode(g++);
        } else {
            return cheapEncode((n+=(j.hashCode()%444))+n) +"y" + j;
        }
    }
    
    // convenience functions
    Object get(String key) { 
        return stuff.get(key);
    }
    
    void put(String key, Object value) {
        stuff.put(key,value);
    }

    boolean prefGetBool(String key) { 
        Boolean b = (Boolean)prefs.get(key);
        if(b == null) {
            return false;
        } else {
            return b.booleanValue();
        }
    }
    
    void prefPut(String key, boolean value) {
        prefs.put(key,new Boolean(value));
    }
    
    public Hashtable getLocales() {
        Hashtable l = (Hashtable)get("locales");
        if(l == null) {
            l = new Hashtable();
            put("locales",l);
        }
        return l;
    }
    
    static String cheapEncode(long l) {
        String out = "";
        if(l < 0) {
            l = 0 - l;
        } else if (l == 0) {
            return "0";
        }
        while(l > 0) {
            char c = (char)(l%(26));
            char o;
            c += 'a';
            o = c;
            out = out + o;
            l /= 26;
        }
        return out;
    }
}
