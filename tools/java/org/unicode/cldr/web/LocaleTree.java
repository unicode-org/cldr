package org.unicode.cldr.web;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.text.RuleBasedCollator;

import org.unicode.cldr.util.CLDRLocale;

import com.ibm.icu.util.ULocale;

public class LocaleTree {
    ULocale displayLocale;
    
    public LocaleTree(ULocale displayLocale) {
        this.displayLocale = displayLocale;
    }
    public void add(CLDRLocale localeName) {
        localeNameToCode.put(getLocaleDisplayName(localeName), (localeName));
        addLocaleToListMap((localeName));
    }

    //TODO: object
    /**
    * TreeMap of all locales. 
     *
     * localeListMap =  TreeMap
     *     [  (String  langScriptDisplayName)  ,    (String localecode) ]  
     *  subLocales = Hashtable
     *       [ localecode, TreeMap ]
     *         -->   TreeMap [ langScriptDisplayName,   String localeCode ]
     *  example
     *  
     *   localeListMap
     *     English  -> en
     *     Serbian  -> sr
     *     Serbian (Cyrillic) -> sr_Cyrl
     *    sublocales
     *       en ->  
     *           [  "English (US)" -> en_US ],   [ "English (Australia)" -> en_AU ] ...
     *      sr ->
     *           "Serbian (Yugoslavia)" -> sr_YU
     */
    
    Map<String,CLDRLocale> localeListMap = new TreeMap<String,CLDRLocale>(RuleBasedCollator.getInstance());
    Map<String,CLDRLocale> localeNameToCode = new HashMap<String,CLDRLocale>();
    Map<CLDRLocale, Map<String,CLDRLocale>> subLocales = new HashMap<CLDRLocale,Map<String,CLDRLocale>>();
    
    private void addLocaleToListMap(CLDRLocale localeName)
    {
        String l = localeName.getLanguage();
        if((l!=null)&&(l.length()==0)) {
            l = null;
        }
        String s = localeName.getScript();
        if((s!=null)&&(s.length()==0)) {
            s = null;
        }
        String t = localeName.getCountry();
        if((t!=null)&&(t.length()==0)) {
            t = null;
        }
        String v = localeName.getVariant();
        if((v!=null)&&(v.length()==0)) {
            v = null;
        }
        
        if(l==null) {
            return; // no language?? 
        }
        
        String ls = ((s==null)?l:(l+"_"+s)); // language and script
        CLDRLocale lsl = CLDRLocale.getInstance(ls);
        
        localeListMap.put(lsl.getDisplayName(displayLocale),lsl);
        
        Map<String, CLDRLocale> lm = subLocales.get(lsl);
        if(lm == null) {
            lm = new TreeMap<String, CLDRLocale>();
            subLocales.put(lsl, lm); 
        }
        
        if(t != null || v!= null) {
            if(v == null) {
                lm.put(localeName.toULocale().getDisplayCountry(displayLocale), localeName);
            } else {
                lm.put(localeName.toULocale().getDisplayCountry(displayLocale) + " (" + localeName.toULocale().getDisplayVariant(displayLocale) + ")", localeName);
            }
        }
    }
    
    public CLDRLocale getLocaleCode(String localeName) {
        return localeNameToCode.get(localeName);
    }
    public  String getLocaleDisplayName(CLDRLocale locale) {
        return locale.getDisplayName(displayLocale);
    }
    public Map<String, CLDRLocale> getMap() {
        // TODO Auto-generated method stub
        return localeListMap;
    }
    /**
     * Return a list of the 'top' locales (language, or language_script)
     * @return
     */
    public Set<String> getTopLocales() {
        return localeListMap.keySet();
    }
    public Map<String, CLDRLocale> getSubLocales(CLDRLocale locale) {
        return subLocales.get(locale);
    }
    
    
}
