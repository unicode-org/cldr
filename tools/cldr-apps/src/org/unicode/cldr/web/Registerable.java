//
//  Registerable.java
//  fivejay
//
//  Created by Steven R. Loomis on 03/05/2006.
//  Copyright 2006-2007 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import org.unicode.cldr.util.CLDRLocale;


/**
 * This class defines a type which can be registered in a LocaleChangeRegistry to
 * receive notification of any changes by locale.
 * As locales are inherited, changes in 'en' will be reflected to 'en_US' etc. 
 * a change to 'root' will be reflected in all sub locales.
 */

public class Registerable {

    /**
     * Is this object still 'valid'?  Becomes invalid if a change is recotded 
     */
    private boolean valid = true; 
    
    /**
     * An alias to the LCR we are registered with.
     */
    private LocaleChangeRegistry lcr;

    /**
     * the locale of this object
     */
    public CLDRLocale locale;
    
    /**
     * protected constructor. Does not register, call register()
     * @param lcr the LCR to eventually register with
     * @param locale the locale to register as.
     * @see #register
     */
    protected Registerable(LocaleChangeRegistry lcr, CLDRLocale locale) {
        this.lcr = lcr;
        this.locale = locale;
    }
    
    /**
     * is this object still valid?  Object becomes invalid if a change is recorded
     * by the LCR.
     * Does not unregister the object if invalid.
     */
    public boolean isValid() {
        if(valid) { 
            if(!lcr.isKeyValid(locale, key)) {
                valid=false;
            }
        }
        return valid;
    }
    
    /**
     * Check if item is valid. Do not unregister if invalid. 
     * Does not modify the object.
     **/
    public boolean peekIsValid() {
        if(valid) { 
            if(!lcr.isKeyValid(locale, key)) {
                return false;
            }
        }
        return valid;
    }
    
    /**
     * Make the item valid again.
     * Useful if the item has responded to the change (reloaded) and is to be considered valid again.
     * does not cause a registration if not registered.
     */
    protected void setValid() {
        valid = true;
    }
    
    /**
     * Register the item with its appropriate change registry.
     */
    public void register() {
        lcr.register(locale, key, this);
    }
    
    /**
     * The key which uniquely identifies this item to the LCR.
     */
    private String key = LocaleChangeRegistry.newKey();
    
    /**
     * convert this item to a string..
     *
     * @return this item as a string
     */
    public String toString () {
        return "{Registerable "+key+" @ "+locale.toString()+", valid:"+peekIsValid()+"}";
    }
    
    public CLDRLocale locale() {
        return locale;
    }
}
