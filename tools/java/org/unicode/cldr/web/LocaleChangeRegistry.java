//
//  LocaleChangeRegistry.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.util.*;

public class LocaleChangeRegistry {

    private static int keyn = 0;
    /**
     * mint a new key for use with the registry
     * @return key string
     */
    public synchronized static final String newKey() {
//        synchronized(this) {
            return CookieSession.cheapEncode(++keyn)+":LCR";
//        }
    }
    
    Hashtable tables = new Hashtable();
    
    /**
     * fetch the appropriate hash table. Private, assumes lock.
     * @param locale locale to hash for
     * @return the hash table, new if needed
     */
    private Hashtable internalGetHash(String locale) {
        Hashtable r = (Hashtable)tables.get(locale);
        if(r == null) {
            r = new Hashtable();
            tables.put(locale, r);
        }
        return r;
    }
    
    /**
     * register an object
     * @param locale - the locale to register. Note, parent locales will automatically be registered.
     */
    public void register(/* other params: tree, etc.., */ String locale, String key, Object what) {
    System.out.println("presynch " + what.toString());
        synchronized(this) {
    System.out.println("insynch " + what.toString());
            while(locale != null ) {
    System.out.println("putting  " + what.toString() + " into " + locale);
                internalGetHash(locale).put(key, what);
                locale = WebContext.getParent(locale);
    System.out.println("trying  " + what.toString() + " into " + locale);
            }
        }
    }

    /** 
     * Is the object still valid under this key?
     * @param locale - locale and all parents will be checked
     * @param key
     * @return true if valid, false if stale.
     */
    public boolean isKeyValid(/* other params: tree, etc.. */ String locale, String key) {
       synchronized(this) {
            while(locale != null ) {
                Object what = internalGetHash(locale).get(key);
                if(what == null) {
                    return false;
                }
                locale = WebContext.getParent(locale);
            }
        }
        return true;
    }
    
    /**
     * invalidate a locale - does NOT invalidate parents or children.
     * @param locale locale to invalidate
     */
    public void invalidateLocale(/* other params: tree, etc.. */ String locale)
    {
        synchronized(this) {
            internalGetHash(locale).clear();
            // notify objects?
        }
    }
}
