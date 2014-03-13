/**
 * 
 */
package org.unicode.cldr.test;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map.Entry;
import java.util.Vector;

import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.SublocaleProvider;
import org.unicode.cldr.util.LruMap;
import org.unicode.cldr.util.XMLSource;

/**
 * @author srl
 * 
 *         Factory must implement CLDRLocale.SublocaleProvider
 */
public class SimpleTestCache extends TestCache {
    private static final boolean DEBUG = false;
    /**
     * Hash the options map
     * 
     * @param o
     * @return
     */
    private LruMap<CheckCLDR.Options, Reference<TestResultBundle>> map = new LruMap<CheckCLDR.Options, Reference<TestResultBundle>>(CLDRConfig.getInstance()
        .getProperty("CLDR_TESTCACHE_SIZE", 12));

    /*
     * (non-Javadoc)
     * 
     * @see org.unicode.cldr.test.TestCache#notifyChange(org.unicode.cldr.util.CLDRLocale, java.lang.String)
     */
    @Override
    public void valueChanged(String xpath, XMLSource source) {
        CLDRLocale locale = CLDRLocale.getInstance(source.getLocaleID());
        valueChanged(xpath, locale);
    }

    private void valueChanged(String xpath, CLDRLocale locale) {
        if (DEBUG) System.err.println("BundDelLoc " + locale + " @ " + xpath);
        for (CLDRLocale sub : ((SublocaleProvider) getFactory()).subLocalesOf(locale)) {
            valueChanged(xpath, sub);
        }
        Vector<CheckCLDR.Options> toRemove = new Vector<CheckCLDR.Options>();
        for (CheckCLDR.Options k : map.keySet()) {
            if (k.getLocale() == locale) {
                toRemove.add(k);
            }
        }
        // avoid concurrent remove
        for (CheckCLDR.Options k : toRemove) {
            map.remove(k);
            if (DEBUG) System.err.println("BundDel " + k);
        }
    }

    @Override
    public String toString() {
        StringBuilder stats = new StringBuilder();
        stats.append("{" + this.getClass().getSimpleName() + super.toString() + " Size: " + map.size() + " (");
        int good = 0;
        int total = 0;
        for (Entry<Options, Reference<TestResultBundle>> k : map.entrySet()) {
            if (k.getValue().get() != null) good++;
            if (DEBUG && true) stats.append("," + k.getKey() + "=" + k.getValue().get());
            total++;
        }
        stats.append(" " + good + "/" + total + "}");
        return stats.toString();
    }

    @Override
    public TestResultBundle getBundle(CheckCLDR.Options options) {
        Reference<TestResultBundle> ref = map.get(options);
        if (DEBUG && ref != null) System.err.println("Bundle refvalid: " + options + " -> " + (ref.get() != null));
        TestResultBundle b = (ref != null ? ref.get() : null);
        if (DEBUG) System.err.println("Bundle " + b + " for " + options + " in " + this.toString());
        if (b == null) {
            // ElapsedTimer et = new ElapsedTimer("New test bundle " + locale + " opt " + options);
            b = new TestResultBundle(options);
            // System.err.println(et.toString());
            map.put(options, new SoftReference<TestResultBundle>(b));
        }
        return b;
    }
}
