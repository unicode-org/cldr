//
//  Registerable.java
//  fivejay
//
//  Created by Steven R. Loomis on 03/05/2006.
//  Copyright 2006 IBM. All rights reserved.
//

package org.unicode.cldr.web;

public abstract class Registerable {
    private boolean valid = true;
    private LocaleChangeRegistry lcr;

    public String locale;
    
    protected Registerable(LocaleChangeRegistry lcr, String locale) {
        this.lcr = lcr;
        this.locale = locale;
    }
    
    public boolean isValid() {
        if(valid) { 
            if(!lcr.isKeyValid(locale, key)) {
                //lcr.unregister();
                valid=false;
            }
        }
        return valid;
    }
    /** Check if pod is valid. Do not unregister if invalid. **/
    public boolean peekIsValid() {
        if(valid) { 
            if(!lcr.isKeyValid(locale, key)) {
                //lcr.unregister();
                //valid=false;
                return false;
            }
        }
        return valid;
    }
    protected void setValid() {
        valid = true;
    }
    
    public void register() {
        lcr.register(locale, key, this);
    }
    private String key = LocaleChangeRegistry.newKey(); // key for this item
    public String toString () {
        return "{Registerable "+key+" @ "+locale+", valid:"+peekIsValid()+"}";
    }
}
