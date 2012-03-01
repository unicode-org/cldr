/**
 * 
 */
package org.unicode.cldr.test;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.SublocaleProvider;
import org.unicode.cldr.util.LruMap;

import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * @author srl
 *
 * Factory must implement CLDRLocale.SublocaleProvider
 */
public class SimpleTestCache extends TestCache {
    /**
     * Hash the options map
     * @param o
     * @return
     */
    private static String optionsToHash(Map<String,String> o) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> k : o.entrySet()) {
            sb.append(k.getKey()).append("=").append(k.getValue()).append("\n");
        }
        return sb.toString();
    }
    
    LruMap<CLDRLocale,Map<String,TestResultBundle>> map = new LruMap<CLDRLocale, Map<String, TestResultBundle>>(4);
    
    /* (non-Javadoc)
     * @see org.unicode.cldr.test.TestCache#notifyChange(org.unicode.cldr.util.CLDRLocale, java.lang.String)
     */
    @Override
    public void notifyChange(CLDRLocale locale, String xpath) {
        for(CLDRLocale sub : ((SublocaleProvider)getFactory()).subLocalesOf(locale)) {
            notifyChange(sub,xpath);
        }
        map.remove(locale); // remove all
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.test.TestCache#getBundle(org.unicode.cldr.util.CLDRLocale, java.util.Map)
     */
    @Override
    public TestResultBundle getBundle(CLDRLocale locale, Map<String, String> options) {
        Map<String, TestResultBundle> r = map.get(locale);
        if(r==null) {
            r = new TreeMap<String, TestResultBundle>();
            map.put(locale, r);
        }
        String k = optionsToHash(options);
        TestResultBundle b = r.get(k);
        System.err.println("Bundle " + b + " for " + k);
        if(b==null) {
            ElapsedTimer et = new ElapsedTimer("New test bundle " + locale + " opt " + options);
            b = new TestResultBundle(locale, options);
            System.err.println(et.toString());
            r.put(k, b);
        }
        return b;
    }

}
