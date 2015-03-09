package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class Annotations {
    
    private static final boolean DEBUG = false;
    
    static Map<String, UnicodeMap<Set<String>>> data = new HashMap<>();
    static {
        File directory = new File(CLDRPaths.COMMON_DIRECTORY, "annotations");
        if (DEBUG) {
            try {
                System.out.println(directory.getCanonicalPath());
            } catch (IOException e) {
            }
        }
        for (File file : directory.listFiles()) {
            if (DEBUG) {
                try {
                    System.out.println(file.getCanonicalPath());
                } catch (IOException e) {
                }
            }
            String name = file.toString();
            String shortName = file.getName();
            if (!shortName.endsWith(".xml") || // skip non-XML
                shortName.startsWith("#") || // skip other junk files
                shortName.startsWith(".")) continue; // skip dot files (backups, etc)
            MyHandler myHandler = new MyHandler(shortName);
            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            xfr.read(name, -1, true);
            myHandler.cleanup();
        }
    }
    
    static class MyHandler extends SimpleHandler {
        static final Splitter splitter = Splitter.on(";").trimResults();
        static final Splitter dotSplitter = Splitter.on(".").trimResults();
        String locale;
        UnicodeMap<Set<String>> localeData = new UnicodeMap<>();
        XPathParts parts = new XPathParts();
        
        public MyHandler(String locale) {
            this.locale = dotSplitter.split(locale).iterator().next();
        }

        public void cleanup() {
            data.put(locale, localeData.freeze());
        }
        
        @Override
        public void handlePathValue(String path, String value) {
            parts.set(path);
            // <ldml>
            // <annotations>
            // <annotation cp='[🐦🕊]'>bird</annotation>
            String lastElement = parts.getElement(-1);
            if (!lastElement.equals("annotation")) {
                if (!"identity".equals(parts.getElement(1))) {
                    throw new IllegalArgumentException("Unexpected path");
                }
                return;
            }
            UnicodeSet us = new UnicodeSet(parts.getAttributeValue(-1, "cp"));
            Set<String> attributes = Collections.unmodifiableSet(new TreeSet<>(splitter.splitToList(value)));
            for (String entry : us) {
                Set<String> usOld = localeData.get(entry);
                Set<String> usNew = attributes;
                if (usOld != null) {
                    usNew = new TreeSet<>(usOld);
                    usNew.addAll(attributes);
                    usNew = Collections.unmodifiableSet(usNew);
                }
                localeData.put(entry, usNew);
            }
        }
    }
    
    public static Set<String> getAvailable() {
        return data.keySet();
    }
    
    public static UnicodeMap<Set<String>> getData(String locale) {
        return data.get(locale);
    }
}
