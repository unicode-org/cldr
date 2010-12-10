/**
 * 
 */
package org.unicode.cldr.web;

import java.util.Hashtable;

/**
 * a UserSettings object that simply stores its settings in memory
 * @author srl
 *
 */
public class EphemeralSettings extends UserSettings {

    private Hashtable<String,String> hash = new Hashtable<String,String>();
    /**
     * 
     */
    public EphemeralSettings() {
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(UserSettings o) {
        if(o == this) {
            return 0;
        } else {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.web.UserSettings#get(java.lang.String, java.lang.String)
     */
    @Override
    public String get(String name, String defaultValue) {
        String v = hash.get(name);
        if(v==null) {
            return defaultValue;
        } else {
            return v;
        }
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.web.UserSettings#set(java.lang.String, java.lang.String)
     */
    @Override
    public void set(String name, String value) {
        hash.put(name,value);
    }

}
