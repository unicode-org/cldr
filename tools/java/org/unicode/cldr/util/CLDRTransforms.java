/**
 * 
 */
package org.unicode.cldr.util;


import org.unicode.cldr.util.CLDRFile.Factory;

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
    static Map id_instance = new HashMap();

    static Transliterator fixup = Transliterator
            .getInstance("[:Mn:]any-hex/java");

    static Set available = new HashSet();
    static String[] doFirst = {"Latin-ConjoiningJamo"};
    // avoid opening
    static {
        Factory cldrFactory = CLDRFile.Factory.make(
                Utility.COMMON_DIRECTORY + "transforms/", ".*");
        // reorder to preload some 
        Set ordered = new LinkedHashSet();
        ordered.addAll(Arrays.asList(doFirst));
        ordered.addAll(cldrFactory.getAvailable());
        
        for (Iterator it = ordered.iterator(); it.hasNext();) {
            String cldrFileName = (String) it.next();
            CLDRFile file = cldrFactory.make(cldrFileName, false);
            cache(file);
        }
        available = Collections.unmodifiableSet(id_instance.keySet());
    }

    public static Set getAvailableTransforms() {
        return available;
    }

    public static Transliterator getInstance(String id) {
        Transliterator result = (Transliterator) id_instance.get(id);
        if (result == null) {
            throw new IllegalArgumentException("No transform for " + id);
        }
        return result;
    }

    private static void cache(CLDRFile cldrFile) {
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
            internalRegister(id, ruleString, Transliterator.FORWARD);
        }
        if (direction.equals("both") || direction.equals("backward")) {
            String id = target + "-" + source + (variant == null ? "" : "/" + variant);
            internalRegister(id, ruleString, Transliterator.REVERSE);
        }
    }

    private static void internalRegister(String id, String ruleString, int direction) {
        try {
            Transliterator t = Transliterator.createFromRules(id,
                    ruleString, direction);
            id_instance.put(id, t);
            Transliterator.unregister(id);
            Transliterator.registerInstance(t);
            if (false) System.out.println("Registered new Transliterator: " + id);
        } catch (RuntimeException e) {
            System.out.println("ERROR: couldn't register new Transliterator: " + id + "\t" + e.getMessage());
        }
    }
}