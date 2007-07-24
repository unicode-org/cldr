/**
 * 
 */
package org.unicode.cldr.util;


import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CLDRTransforms {
    private static Object mutex = new Object();
  
    private static CLDRTransforms INSTANCE = null;
    
    /**
     * 
     * @param showProgress null if no progress needed
     * @return
     */

    public static CLDRTransforms getinstance(TestFmwk showProgress) {
      synchronized (mutex) {
        if (INSTANCE == null) {
          INSTANCE = new CLDRTransforms(showProgress);
        }
        return INSTANCE;
      }
    }

    
    Map id_instance = new HashMap();

    static Transliterator fixup = Transliterator
            .getInstance("[:Mn:]any-hex/java");

    Set available = new HashSet();
    String[] doFirst = {"Latin-ConjoiningJamo"};
    
    Factory cldrFactory = CLDRFile.Factory.make(
        Utility.COMMON_DIRECTORY + "transforms/", ".*");
    
    private CLDRTransforms(TestFmwk showProgress)  {        
        // reorder to preload some 
        Set ordered = new LinkedHashSet();
        ordered.addAll(Arrays.asList(doFirst));
        ordered.addAll(cldrFactory.getAvailable());
        
        for (Iterator it = ordered.iterator(); it.hasNext();) {
            String cldrFileName = (String) it.next();
            if (cldrFileName.contains("Ethiopic")) {
              System.out.println("Skipping Ethiopic");
              continue;
            }
            CLDRFile file = cldrFactory.make(cldrFileName, false);
            cache(file, showProgress);
        }
        available = Collections.unmodifiableSet(id_instance.keySet());
    }

    public  Set getAvailableTransforms() {
        return available;
    }

    public  Transliterator getInstance(String id) {
        Transliterator result = (Transliterator) id_instance.get(id);
        if (result == null) {
            throw new IllegalArgumentException("No transform for " + id);
        }
        return result;
    }

    private  void cache(CLDRFile cldrFile, TestFmwk showProgress) {
        boolean first = true;
        StringBuffer rules = new StringBuffer();
        XPathParts parts = new XPathParts();
        String source = null;
        String target = null;
        String variant = null;
        String direction = null;

        for (Iterator it = cldrFile.iterator("", CLDRFile.ldmlComparator); it
                .hasNext();) {
            String path = (String) it.next();
            String value = cldrFile.getStringValue(path);
            if (first) {
                parts.set(path);
                Map attributes = parts.findAttributes("transform");
                if (attributes == null)
                    return; // error, not a transform file
                source = (String) attributes.get("source");
                target = (String) attributes.get("target");
                variant = (String) attributes.get("variant");
                direction = (String) attributes.get("direction");
                first = false;
            }
            if (path.indexOf("/comment") >= 0) {
                // skip
            } else if (path.indexOf("/tRule") >= 0) {
                // value = replaceUnquoted(value,"\u00A7", "&");
                value = value.replace('\u2192', '>');
                value = value.replace('\u2190', '<');
                value = value.replaceAll("\u2194", "<>");
                value = fixup.transliterate(value);
                rules.append(value).append("\r\n");
            } else {
                throw new IllegalArgumentException("Unknown element: "
                        + path + "\t " + value);
            }
        }
        String ruleString = rules.toString();
        if (direction.equals("both") || direction.equals("forward")) {
            String id = source + "-" + target + (variant == null ? "" : "/" + variant);
            internalRegister(id, ruleString, Transliterator.FORWARD, showProgress);
        }
        if (direction.equals("both") || direction.equals("backward")) {
            String id = target + "-" + source + (variant == null ? "" : "/" + variant);
            internalRegister(id, ruleString, Transliterator.REVERSE, showProgress);
        }
    }

    private  void internalRegister(String id, String ruleString, int direction, TestFmwk showProgress) {
        try {
            Transliterator t = Transliterator.createFromRules(id,
                    ruleString, direction);
            id_instance.put(id, t);
            Transliterator.unregister(id);
            Transliterator.registerInstance(t);
            if (showProgress != null) showProgress.logln("Registered new Transliterator: " + id);
        } catch (RuntimeException e) {
          if (showProgress != null) showProgress.errln("couldn't register new Transliterator: " + id + "\t" + e.getMessage());
        }
    }
}