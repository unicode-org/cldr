package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class Annotations {
    private static final boolean DEBUG = false;

    static final Splitter splitter = Splitter.on(Pattern.compile("[|;]")).trimResults().omitEmptyStrings();
    static final Splitter dotSplitter = Splitter.on(".").trimResults();

    public final Set<String> annotations;
    public final String tts;

    static final Map<String, UnicodeMap<Annotations>> cache = new ConcurrentHashMap<>();
    static final Set<String> LOCALES;
    static final String DIR;

    static {
        File directory = new File(CLDRPaths.COMMON_DIRECTORY, "annotations");
        try {
            DIR = directory.getCanonicalPath();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        if (DEBUG) {
            System.out.println(DIR);
        }
        Builder<String> temp = ImmutableSet.builder();
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
                shortName.startsWith(".") ||
                shortName.contains("001") // skip world english for now
                ) continue; // skip dot files (backups, etc)
            temp.add(dotSplitter.split(shortName).iterator().next());
        }
        LOCALES = temp.build();
    }

    static class MyHandler extends SimpleHandler {
        String locale;
        UnicodeMap<Annotations> localeData = new UnicodeMap<>();
        XPathParts parts = new XPathParts();

        public MyHandler(String locale) {
            this.locale = locale;
        }

        public UnicodeMap<Annotations> cleanup() {
            cache.put(locale, localeData.freeze());
            return localeData;
        }

        @Override
        public void handlePathValue(String path, String value) {
            parts.set(path);
//  <ldml>
//    <annotations>
//      <annotation cp='[🐦🕊]'>bird</annotation>
//             or
//      <annotation cp="😀">gesig; grinnik</annotation>
//      <annotation cp="😀" type="tts">grinnikende gesig</annotation>
//             or
//      <annotation cp="[😁]" tts="grinnikende gesig met glimlaggende oë">oog; gesig; grinnik; glimlag</annotation>

            String lastElement = parts.getElement(-1);
            if (!lastElement.equals("annotation")) {
                if (!"identity".equals(parts.getElement(1))) {
                    throw new IllegalArgumentException("Unexpected path");
                }
                return;
            }
            String usString = parts.getAttributeValue(-1, "cp");
            UnicodeSet us = usString.startsWith("[") && usString.endsWith("]") ? new UnicodeSet(usString) : new UnicodeSet().add(usString);
            String tts = parts.getAttributeValue(-1, "tts");
            String type = parts.getAttributeValue(-1, "type");

            if ("tts".equals(type)) {
                addItems(localeData, us, Collections.EMPTY_SET, value);
            } else {
                Set<String> attributes = new TreeSet<>(splitter.splitToList(value));
                addItems(localeData, us, attributes, tts);
            }
        }

        private void addItems(UnicodeMap<Annotations> unicodeMap, UnicodeSet us, Set<String> attributes, String tts) {
            for (String entry : us) {
                Annotations annotations = unicodeMap.get(entry);
                if (annotations == null) {
                    unicodeMap.put(entry, new Annotations(attributes, tts));
                } else {
                    unicodeMap.put(entry, annotations.add(attributes, tts)); // creates new item
                }
            }
        }
    }

    public Annotations(Set<String> attributes, String tts2) {
        annotations = attributes == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(attributes);
        tts = tts2;
    }

    public Annotations add(Set<String> attributes, String tts2) {
        return new Annotations(annotations == null ? attributes : attributes == null ? annotations : union(attributes, annotations), 
            tts == null ? tts2 : tts2 == null ? tts : throwDup());
    }

    private String throwDup() {
        throw new IllegalArgumentException("Duplicate tts");
    }

    private Set<String> union(Set<String> a, Set<String> b) {
        TreeSet<String> result = new TreeSet<>(a);
        result.addAll(b);
        return result;
    }

    public static Set<String> getAvailable() {
        return LOCALES;
    }

    public static Set<String> getAvailableLocales() {
        return LOCALES;
    }

    public static UnicodeMap<Annotations> getData(String locale) {
        UnicodeMap<Annotations> result = cache.get(locale);
        if (result != null) {
            return result;
        }
        if (!LOCALES.contains(locale)) {
            return null;
        }
        MyHandler myHandler = new MyHandler(locale);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(DIR + "/" + locale + ".xml", -1, true);
        return myHandler.cleanup();
    }

    @Override
    public String toString() {
        return toString(false);
    }
    
    public String toString(boolean html) {
        Set<String> annotations2 = annotations;
        if (tts != null && annotations2.contains(tts)) {
            annotations2 = new LinkedHashSet<String>(annotations);
            annotations2.remove(tts);
        }
        String result = CollectionUtilities.join(annotations2, " |\u00a0");
        if (tts != null) {
            String ttsString = (html ? "*<b>" : "*") + tts + (html ? "</b>" : "*");
            if (result.isEmpty()) {
                result = ttsString;
            } else {
                result = ttsString + (html ? "<br>|\u00a0" : " |\u00a0") + result;
            }
        }
        return result;
    }
}
