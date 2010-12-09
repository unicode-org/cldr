//
//  LocaleChangeRegistry.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2007 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.util.Hashtable;

import org.unicode.cldr.util.CLDRLocale;

/**
 * This class implements a class that tracks changes to locale-based data. 
 * @see Registerable
 */
public class LocaleChangeRegistry {

    /**
     * Last key ID .. unique across all LCRs
     */
    private static int keyn = 0;
    
    /**
     * mint a new key for use with the registry
     * @return key string
     */
    public synchronized static final String newKey() {
        return CookieSession.cheapEncode(++keyn)+":LCR";
    }
    
    /**
     * hash of all lcrs
     */
    Hashtable tables = new Hashtable();
    
    /**
     * fetch the appropriate hash table. Private, assumes lock.
     * @param locale locale to hash for
     * @return the hash table, new if needed
     */
    private Hashtable internalGetHash(CLDRLocale locale) {
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
     * @param key key to register under.
     * @param what what is being registered (ignored)
     */
    public void register(/* other params: tree, etc.., */ CLDRLocale locale, String key, Object /*notused */what) {
        synchronized(this) {
            while(locale != null ) {
                internalGetHash(locale).put(key, "what"); // Register as string
                locale = locale.getParent();
            }
        }
    }

    /** 
     * Is the object still valid under this key?
     * @param locale - locale and all parents will be checked
     * @param key
     * @return true if valid, false if stale.
     */
    public boolean isKeyValid(/* other params: tree, etc.. */ CLDRLocale locale, String key) {
       synchronized(this) {
            while(locale != null ) {
                Object what = internalGetHash(locale).get(key);
                if(what == null) {
                    return false;
                }
                locale = locale.getParent();
            }
        }
        return true;
    }
    
    /**
     * invalidate a locale - does NOT invalidate parents or children.
     * @param locale locale to invalidate
     */
    public void invalidateLocale(/* other params: tree, etc.. */ CLDRLocale locale)
    {
System.err.println("#*#LCR invalidate " + locale);
        synchronized(this) {
            internalGetHash(locale).clear();
        }
    }
}
