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
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class Annotations {
    private static final boolean DEBUG = false;

    public static final String BAD_MARKER = "⊗";
    public static final String MISSING_MARKER = "⊖";
    public static final String ENGLISH_MARKER = "⊕";
    
    static final Splitter splitter = Splitter.on(Pattern.compile("[|;]")).trimResults().omitEmptyStrings();
    static final Splitter dotSplitter = Splitter.on(".").trimResults();

    static final Map<String, AnnotationSet> cache = new ConcurrentHashMap<>();
    static final Set<String> LOCALES;
    static final String DIR;

    private final Set<String> annotations;
    private final String tts;

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

        public AnnotationSet cleanup() {
            final AnnotationSet result = new AnnotationSet(locale, localeData.freeze());
            cache.put(locale, result);
            return result;
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
                addItems(localeData, us, Collections.<String>emptySet(), value);
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
        annotations = attributes == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(attributes);
        tts = tts2;
    }

    public Annotations add(Set<String> attributes, String tts2) {
        return new Annotations(getKeywords() == null ? attributes : attributes == null ? getKeywords() : union(attributes, getKeywords()), 
            getShortName() == null ? tts2 : tts2 == null ? getShortName() : throwDup());
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

    static class EmojiConstants {
        public static final String EMOJI_VARIANT_STRING = "\uFE0F";
        static final int FIRST_REGIONAL = 0x1F1E6;
        static final int LAST_REGIONAL = 0x1F1FF;
        public static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(FIRST_REGIONAL, LAST_REGIONAL).freeze();
        public static final String KEYCAP_MARK_STRING = "\u20E3";
        public static final UnicodeSet MODIFIERS = new UnicodeSet(0x1F3FB, 0x1F3FF).freeze();
        public static final String JOINER_STRING = "\u200D";
        public static final String KISS = "💋";
        public static final String HEART = "❤";
        public static final String MALE_SIGN = "♂";
        public static final String FEMALE_SIGN = "♀";
        public static String getFlagCode(String s) {
            return String.valueOf((char)(s.codePointAt(0) - FIRST_REGIONAL + 'A')) + (char) (s.codePointAt(2) - FIRST_REGIONAL + 'A');
        }
        public static final UnicodeSet FAMILY_MARKERS = new UnicodeSet().add(0x1F466, 0x1F469).add(0x1F476).freeze(); // boy, girl, man, woman, baby
        public static final UnicodeSet SKIP_SET = new UnicodeSet().add(EmojiConstants.HEART).add(EmojiConstants.KISS).add(MALE_SIGN).add(FEMALE_SIGN)
            .freeze();
    }

    public static final class AnnotationSet {

        private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

        static Factory factory = CONFIG.getCldrFactory();
        static CLDRFile ENGLISH = CONFIG.getEnglish();

        private final String locale;
        private final UnicodeMap<Annotations> baseData;
        private final CLDRFile cldrFile;
        private final SimpleFormatter initialPattern;
        private final SimpleFormatter listPattern;
        private final String flagLabel;
        private final String keycapLabel;
        private final Map<String, Annotations> localeCache = new ConcurrentHashMap<>();

        public AnnotationSet(String locale, UnicodeMap<Annotations> source) {
            this.locale = locale;
            baseData = source;
            cldrFile = factory.make(locale, true);
            initialPattern = SimpleFormatter.compile(getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
            listPattern = SimpleFormatter.compile(getStringValue("//ldml/listPatterns/listPattern[@type=\"unit-short\"]/listPatternPart[@type=\"2\"]"));
            keycapLabel = getStringValue("//ldml/characterLabels/characterLabel[@type=\"keycap\"]");
            flagLabel = getStringValue("//ldml/characterLabels/characterLabel[@type=\"flag\"]");
        }
        private String getStringValue(String xpath) {
            String result = cldrFile.getStringValue(xpath);
            if (result == null) {
                return BAD_MARKER + ENGLISH_MARKER + ENGLISH.getStringValue(xpath);
            }
            String sourceLocale = cldrFile.getSourceLocaleID(xpath, null);
            if (sourceLocale.equals(XMLSource.CODE_FALLBACK_ID) || sourceLocale.equals(XMLSource.ROOT_ID)) {
                return BAD_MARKER + result;
            }
            return result;
        }
        public String getShortName(String code) {
            code = code.replace(EmojiConstants.EMOJI_VARIANT_STRING,"");
            Annotations stock = baseData.get(code);
            if (stock != null && stock.tts != null) {
                return stock.tts;
            }
            stock = localeCache.get(code);
            if (stock != null) {
                return stock.tts;
            }
            stock = synthesize(code);
            if (stock != null) {
                localeCache.put(code, stock);
                return stock.tts;
            }
            return null;
        }
        public Set<String> getKeywords(String code) {
            code = code.replace(EmojiConstants.EMOJI_VARIANT_STRING,"");
            Annotations stock = baseData.get(code);
            if (stock != null && stock.annotations != null) {
                return stock.annotations;
            }
            stock = localeCache.get(code);
            if (stock != null) {
                return stock.annotations;
            }
            stock = synthesize(code);
            if (stock != null) {
                localeCache.put(code, stock);
                return stock.annotations;
            }
            return Collections.<String>emptySet();
        }
        public UnicodeSet keySet() {
            return baseData.keySet();
        }

        private Annotations synthesize(String code) {
            String shortName = null;
            Set<String> annotations = null;
            int len = code.codePointCount(0, code.length());
            if (len == 1) {
                return new Annotations(Collections.<String>emptySet(), BAD_MARKER + getDataSet("en").getShortName(code) + "[en]");
            } else if (EmojiConstants.REGIONAL_INDICATORS.containsAll(code)) {
                String countryCode = EmojiConstants.getFlagCode(code);
                String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, countryCode);
                return new Annotations(Collections.singleton(flagLabel), getStringValue(path));
            } else if (code.contains(EmojiConstants.KEYCAP_MARK_STRING) || code.equals("🔟")) {
                if (locale.equals("ga")) {
                    int debug = 0;
                }
                final String rem = code.equals("🔟") ? "10" : UTF16.valueOf(code.charAt(0));
                shortName = initialPattern.format(keycapLabel, rem);
                return new Annotations(Collections.singleton(keycapLabel), shortName);
            } else if (EmojiConstants.MODIFIERS.containsSome(code)) {
                String rem = EmojiConstants.MODIFIERS.stripFrom(code, false);
                code = EmojiConstants.MODIFIERS.stripFrom(code, true);
                return getBasePlusRemainder(cldrFile, code, rem, UnicodeSet.EMPTY);
            } else if (code.contains(EmojiConstants.JOINER_STRING)) {
                code = code.replace(EmojiConstants.JOINER_STRING,"");
                if (code.contains(EmojiConstants.KISS)) {
                    return getBasePlusRemainder(cldrFile, "💏", code, EmojiConstants.SKIP_SET);
                } else if (code.contains(EmojiConstants.HEART)) {
                    return getBasePlusRemainder(cldrFile, "💑", code, EmojiConstants.SKIP_SET);
                } else if (EmojiConstants.FAMILY_MARKERS.containsAll(code)) {
                    return getBasePlusRemainder(cldrFile, "👪", code, UnicodeSet.EMPTY);
                } else if (code.contains(EmojiConstants.MALE_SIGN)){
                    return getBasePlusRemainder(cldrFile, BAD_MARKER, "👨", code, EmojiConstants.SKIP_SET);
                } else if (code.contains(EmojiConstants.FEMALE_SIGN)){
                    return getBasePlusRemainder(cldrFile, BAD_MARKER, "👩", code, EmojiConstants.SKIP_SET);
                }
            }
            return getBasePlusRemainder(cldrFile, BAD_MARKER, null, code, UnicodeSet.EMPTY);
        }

        private Annotations getBasePlusRemainder(CLDRFile cldrFile, String base, String rem, UnicodeSet ignore) {
            return getBasePlusRemainder(cldrFile, "", base, rem, ignore);
        }
        private Annotations getBasePlusRemainder(CLDRFile cldrFile, String marker, String base, String rem, UnicodeSet ignore) {
            String shortName = null;
            Set<String> annotations = new LinkedHashSet<>();
            SimpleFormatter pattern = listPattern;
            if (base != null) {
                pattern = initialPattern;
                shortName = getShortName(base);
                annotations.addAll(getKeywords(base));
            }
            for (int mod : CharSequences.codePoints(rem)) {
                if (ignore.contains(mod)) {
                    continue;
                }
                final String modStr = UTF16.valueOf(mod);
                String modName = getShortName(modStr);
                shortName = shortName == null ? modName : pattern.format(shortName, modName);
                annotations.addAll(getKeywords(modStr));
                pattern = listPattern;
            }
            Annotations result = new Annotations(annotations, marker + shortName);
            return result;
        }

        public String toString(String code, boolean html) {
            final String shortName = getShortName(code);
            if (shortName == null || shortName.startsWith(BAD_MARKER)) {
                return MISSING_MARKER;
            }
            Set<String> keywords = getKeywords(code);
            if (shortName != null && keywords.contains(shortName)) {
                keywords = new LinkedHashSet<String>(keywords);
                keywords.remove(shortName);
            }
            String result = CollectionUtilities.join(keywords, " |\u00a0");
            if (shortName != null) {
                String ttsString = (html ? "*<b>" : "*") + shortName + (html ? "</b>" : "*");
                if (result.isEmpty()) {
                    result = ttsString;
                } else {
                    result = ttsString + (html ? "<br>|\u00a0" : " |\u00a0") + result;
                }
            }
            return result;
        }
    }


    public static AnnotationSet getDataSet(String locale) {
        AnnotationSet result = cache.get(locale);
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

    public static UnicodeMap<Annotations> getData(String locale) {
        AnnotationSet result = getDataSet(locale);
        return result == null ? null : result.baseData;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean html) {
        Set<String> annotations2 = getKeywords();
        if (getShortName() != null && annotations2.contains(getShortName())) {
            annotations2 = new LinkedHashSet<String>(getKeywords());
            annotations2.remove(getShortName());
        }
        String result = CollectionUtilities.join(annotations2, " |\u00a0");
        if (getShortName() != null) {
            String ttsString = (html ? "*<b>" : "*") + getShortName() + (html ? "</b>" : "*");
            if (result.isEmpty()) {
                result = ttsString;
            } else {
                result = ttsString + (html ? "<br>|\u00a0" : " |\u00a0") + result;
            }
        }
        return result;
    }

    /**
     * @return the annotations
     */
    public Set<String> getKeywords() {
        return annotations;
    }

    /**
     * @return the tts
     */
    public String getShortName() {
        return tts;
    }
}
