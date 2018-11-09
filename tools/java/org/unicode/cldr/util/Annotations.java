package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.unicode.cldr.test.EmojiSubdivisionNames;
import org.unicode.cldr.tool.ChartAnnotations;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.util.ICUUncheckedIOException;

public class Annotations {
    private static final boolean DEBUG = false;

    public static final String BAD_MARKER = "⊗";
    public static final String MISSING_MARKER = "⊖";
    public static final String ENGLISH_MARKER = "⊕";
    public static final String EQUIVALENT = "≣";

    public static final Splitter splitter = Splitter.on(Pattern.compile("[|;]")).trimResults().omitEmptyStrings();
    static final Splitter dotSplitter = Splitter.on(".").trimResults();

    static final Map<String, Map<String, AnnotationSet>> cache = new ConcurrentHashMap<>();
    static final Set<String> LOCALES;
    static final String DIR;
    private static final AnnotationSet ENGLISH_DATA;

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
                shortName.startsWith(".")
//                || shortName.contains("001") // skip world english for now
                ) continue; // skip dot files (backups, etc)
            temp.add(dotSplitter.split(shortName).iterator().next());
        }
        LOCALES = temp.build();
        ENGLISH_DATA = getDataSet("en");
    }

    static class MyHandler extends SimpleHandler {
        private final String locale;
        private final UnicodeMap<Annotations> localeData = new UnicodeMap<>();
        private final AnnotationSet parentData;
        private final Map<String, AnnotationSet> dirCache;

        public MyHandler(Map<String, AnnotationSet> dirCache, String locale, AnnotationSet parentData) {
            this.locale = locale;
            this.parentData = parentData;
            this.dirCache = dirCache;
        }

        public AnnotationSet cleanup() {
            // add parent data (may be overridden)
            UnicodeMap<Annotations> templocaleData = null;
            if (parentData != null) {
                templocaleData = new UnicodeMap<>();
                UnicodeSet keys = new UnicodeSet(parentData.baseData.keySet()).addAll(localeData.keySet());
                for (String key : keys) {
                    Annotations parentValue = parentData.baseData.get(key);
                    Annotations myValue = localeData.get(key);
                    if (parentValue == null) {
                        templocaleData.put(key, myValue);
                    } else if (myValue == null) {
                        templocaleData.put(key, parentValue);
                    } else { // need to combine
                        String tts = myValue.tts == null
                            ? parentValue.tts : myValue.tts;
                        Set<String> annotations = myValue.annotations == null || myValue.annotations.isEmpty()
                            ? parentValue.annotations : myValue.annotations;
                        templocaleData.put(key, new Annotations(annotations, tts));
                    }
                }
            }

            final AnnotationSet result = new AnnotationSet(locale, localeData, templocaleData);
            dirCache.put(locale, result);
            return result;
        }

        static final Pattern SPACES = Pattern.compile("\\s+");
        
        @Override
        public void handlePathValue(String path, String value) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String lastElement = parts.getElement(-1);
            if (!lastElement.equals("annotation")) {
                if (!"identity".equals(parts.getElement(1))) {
                    throw new IllegalArgumentException("Unexpected path");
                }
                return;
            }
            String usString = parts.getAttributeValue(-1, "cp");
            UnicodeSet us1 = usString.startsWith("[") && usString.endsWith("]") ? new UnicodeSet(usString) : new UnicodeSet().add(usString);
            UnicodeSet us = new UnicodeSet();
            for (String s : us1) {
                us.add(s.replace(EmojiConstants.EMOJI_VARIANT_STRING, ""));
            }
            String tts = parts.getAttributeValue(-1, "tts");
            String type = parts.getAttributeValue(-1, "type");
            String alt = parts.getAttributeValue(-1, "alt");

            // clean up value
            String value2 = SPACES.matcher(value).replaceAll(" ").trim();
            if (!value2.equals(value)) {
                value = value2;
            }
            if (alt != null) {
                // do nothing for now
            } else if ("tts".equals(type)) {
                addItems(localeData, us, Collections.<String> emptySet(), value);
            } else {
                Set<String> attributes = new TreeSet<>(splitter.splitToList(value));
                addItems(localeData, us, attributes, tts);
            }
        }

        private void addItems(UnicodeMap<Annotations> unicodeMap, UnicodeSet us, Set<String> attributes, String tts) {
            for (String entry : us) {
                addItems(unicodeMap, entry, attributes, tts);
            }
        }

        private void addItems(UnicodeMap<Annotations> unicodeMap, String entry, Set<String> attributes, String tts) {
            Annotations annotations = unicodeMap.get(entry);
            if (annotations == null) {
                unicodeMap.put(entry, new Annotations(attributes, tts));
            } else {
                unicodeMap.put(entry, annotations.add(attributes, tts)); // creates new item
            }
        }
    }

    public Annotations(Set<String> attributes, String tts2) {
        annotations = attributes == null ? Collections.<String> emptySet() : ImmutableSet.copyOf(attributes);
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

    public static final class AnnotationSet {

        private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

        static final Factory factory = CONFIG.getCldrFactory();
        static final CLDRFile ENGLISH = CONFIG.getEnglish();
        static final CLDRFile ENGLISH_ANNOTATIONS = null;
        static final Map<String,String> englishSubdivisionIdToName = EmojiSubdivisionNames.getSubdivisionIdToName("en");
        //CLDRConfig.getInstance().getAnnotationsFactory().make("en", false);

        private final String locale;
        private final UnicodeMap<Annotations> baseData;
        private final UnicodeMap<Annotations> unresolvedData;
        private final CLDRFile cldrFile;
        private final Map<String, String> subdivisionIdToName;
        private final SimpleFormatter initialPattern;
        private final Pattern initialRegexPattern;
        private final XListFormatter listPattern;
        private final Set<String> flagLabelSet;
        private final Set<String> keycapLabelSet;
        private final String keycapLabel;
        private final String flagLabel;
//        private final String maleLabel;
//        private final String femaleLabel;
        private final Map<String, Annotations> localeCache = new ConcurrentHashMap<>();

        static UnicodeSetSpanner uss = new UnicodeSetSpanner(EmojiConstants.COMPONENTS); // must be sync'ed

        private AnnotationSet(String locale, UnicodeMap<Annotations> source, UnicodeMap<Annotations> resolvedSource) {
            this.locale = locale;
            unresolvedData = source.freeze();
            this.baseData = resolvedSource == null ? unresolvedData : resolvedSource.freeze();
            cldrFile = factory.make(locale, true);
            subdivisionIdToName = EmojiSubdivisionNames.getSubdivisionIdToName(locale);
            listPattern = new XListFormatter(cldrFile, EmojiConstants.COMPOSED_NAME_LIST);
            final String initialPatternString = getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]");
            initialPattern = SimpleFormatter.compile(initialPatternString);
            final String regexPattern = ("\\Q" + initialPatternString.replace("{0}", "\\E.*\\Q").replace("{1}", "\\E.*\\Q") + "\\E")
                .replace("\\Q\\E", ""); // HACK to detect use of prefix pattern
            initialRegexPattern = Pattern.compile(regexPattern);
            flagLabelSet = getLabelSet("flag");
            flagLabel = flagLabelSet.isEmpty() ? null : flagLabelSet.iterator().next();
            keycapLabelSet = getLabelSet("keycap");
            keycapLabel = keycapLabelSet.isEmpty() ? null : keycapLabelSet.iterator().next();
//            maleLabel = getStringValue("//ldml/characterLabels/characterLabel[@type=\"male\"]");
//            femaleLabel = getStringValue("//ldml/characterLabels/characterLabel[@type=\"female\"]");
        }

        /**
         * @deprecated Use {@link #getLabelSet(String)} instead
         */
        private Set<String> getLabelSet() {
            return getLabelSet("flag");
        }

        private Set<String> getLabelSet(String typeAttributeValue) {
            String label = getStringValue("//ldml/characterLabels/characterLabel[@type=\"" + typeAttributeValue + "\"]");
            return label == null ? Collections.<String> emptySet() : Collections.singleton(label);
        }

        private String getStringValue(String xpath) {
            return getStringValue(xpath, cldrFile, ENGLISH);
        }

        private String getStringValue(String xpath, CLDRFile cldrFile2, CLDRFile english) {
            String result = cldrFile2.getStringValue(xpath);
            if (result == null) {
                return ENGLISH_MARKER + english.getStringValue(xpath);
            }
            String sourceLocale = cldrFile2.getSourceLocaleID(xpath, null);
            if (sourceLocale.equals(XMLSource.CODE_FALLBACK_ID) || sourceLocale.equals(XMLSource.ROOT_ID)) {
                return MISSING_MARKER + result;
            }
            return result;
        }

        public String getShortName(String code) {
            return getShortName(code, null);
        }

        public String getShortName(String code, Transform<String, String> otherSource) {
            if (code.equals("🧙‍♀️")) {
                int debug = 0;
            }

            code = code.replace(EmojiConstants.EMOJI_VARIANT_STRING, "");
            Annotations stock = baseData.get(code);
            if (stock != null && stock.tts != null) {
                return stock.tts;
            }
            stock = localeCache.get(code);
            if (stock != null) {
                return stock.tts;
            }
            stock = synthesize(code, otherSource);
            if (stock != null) {
                localeCache.put(code, stock);
                return stock.tts;
            }
            return null;
        }

        public Set<String> getKeywords(String code) {
            code = code.replace(EmojiConstants.EMOJI_VARIANT_STRING, "");
            Annotations stock = baseData.get(code);
            if (stock != null && stock.annotations != null) {
                return stock.annotations;
            }
            stock = localeCache.get(code);
            if (stock != null) {
                return stock.annotations;
            }
            stock = synthesize(code, null);
            if (stock != null) {
                localeCache.put(code, stock);
                return stock.annotations;
            }
            return Collections.<String> emptySet();
        }

        /** Returns the set of all keys for which annotations are available. WARNING: keys have the Emoji Presentation Selector removed!
         */
        public UnicodeSet keySet() {
            return baseData.keySet();
        }

        private Annotations synthesize(String code, Transform<String, String> otherSource) {
            if (code.equals("👱🏻‍♂")) {
                int debug = 0;
            }
            String shortName = null;
            int len = code.codePointCount(0, code.length());
            boolean isKeycap10 = code.equals("🔟");
            if (len == 1 && !isKeycap10) {
                String tempName = null;
                if (locale.equals("en")) {
                    if (otherSource != null) {
                        tempName = otherSource.transform(code);
                    }
                    if (tempName == null) {
                        return null;
                    }
                    return new Annotations(Collections.<String> emptySet(), tempName);
                } else { // fall back to English if possible, but mark it.
                    tempName = getDataSet("en").getShortName(code);
                    if (tempName == null) {
                        return null;
                    }
                    return new Annotations(Collections.<String> emptySet(), ENGLISH_MARKER + tempName);
                }
            } else if (EmojiConstants.REGIONAL_INDICATORS.containsAll(code)) {
                String countryCode = EmojiConstants.getFlagCode(code);
                String path = CLDRFile.getKey(CLDRFile.TERRITORY_NAME, countryCode);
                String regionName = getStringValue(path);
                if (regionName == null) {
                    regionName = ENGLISH_MARKER + ENGLISH.getStringValue(path);
                }
                String flagName = flagLabel == null ? regionName : initialPattern.format(flagLabel, regionName);
                return new Annotations(flagLabelSet, flagName);
            } else if (code.startsWith(EmojiConstants.BLACK_FLAG)
                && code.endsWith(EmojiConstants.TAG_TERM)) {
                String subdivisionCode = EmojiConstants.getTagSpec(code);
                String subdivisionName = subdivisionIdToName.get(subdivisionCode);
                if (subdivisionName == null) {
                    subdivisionName = englishSubdivisionIdToName.get(subdivisionCode);
                    if (subdivisionName != null) {
                        subdivisionName = ENGLISH_MARKER + subdivisionCode;
                    } else {
                        subdivisionName = MISSING_MARKER + subdivisionCode;
                    }
                }
                String flagName = flagLabel == null ? subdivisionName : initialPattern.format(flagLabel, subdivisionName);
                return new Annotations(flagLabelSet, flagName);
            } else if (isKeycap10 || code.contains(EmojiConstants.KEYCAP_MARK_STRING)) {
                final String rem = code.equals("🔟") ? "10" : UTF16.valueOf(code.charAt(0));
                shortName = initialPattern.format(keycapLabel, rem);
                return new Annotations(keycapLabelSet, shortName);
            }
            UnicodeSet skipSet = EmojiConstants.REM_SKIP_SET;
            String rem = "";
            SimpleFormatter startPattern = initialPattern;
            if (EmojiConstants.COMPONENTS.containsSome(code)) {
                synchronized (uss) {
                    rem = uss.deleteFrom(code, SpanCondition.NOT_CONTAINED);
                    code = uss.deleteFrom(code, SpanCondition.CONTAINED);
                }
            }
            if (code.contains(EmojiConstants.JOINER_STRING)) {
//                if (code.endsWith(EmojiConstants.JOINER_MALE_SIGN)){
//                    if (matchesInitialPattern(code)) { // "👮🏼‍♂️","police officer: man, medium-light skin tone"
//                        rem = EmojiConstants.MAN + rem;
//                        code = code.substring(0,code.length()-EmojiConstants.JOINER_MALE_SIGN.length());
//                    } // otherwise "🚴🏿‍♂️","man biking: dark skin tone"
//                } else if (code.endsWith(EmojiConstants.JOINER_FEMALE_SIGN)){
//                    if (matchesInitialPattern(code)) { // 
//                        rem = EmojiConstants.WOMAN + rem;
//                        code = code.substring(0,code.length()-EmojiConstants.JOINER_FEMALE_SIGN.length());
//                    }
//                } else 
                if (code.contains(EmojiConstants.KISS)) {
                    rem = code + rem;
                    code = "💏";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (code.contains(EmojiConstants.HEART)) {
                    rem = code + rem;
                    code = "💑";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (code.contains(EmojiConstants.HANDSHAKE)) {
                    code = code.startsWith(EmojiConstants.MAN) ? "👬"
                        : code.endsWith(EmojiConstants.MAN) ? "👫" 
                            : "👭";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (EmojiConstants.FAMILY_MARKERS.containsAll(code)) {
                    rem = code + rem;
                    code = "👪";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
//                } else {
//                    startPattern = listPattern;
                }
                // left over is "👨🏿‍⚖","judge: man, dark skin tone"
            }
            return getBasePlusRemainder(cldrFile, code, rem, skipSet, startPattern, otherSource);
        }

        private boolean matchesInitialPattern(String code) {
            Annotations baseAnnotation = baseData.get(code);
            String baseName = baseAnnotation == null ? null : baseAnnotation.getShortName();
            return baseName != null && initialRegexPattern.matcher(baseName).matches();
        }

        private Annotations getBasePlusRemainder(CLDRFile cldrFile, String base, String rem, UnicodeSet ignore, SimpleFormatter pattern,
            Transform<String, String> otherSource) {
            String shortName = null;
            Set<String> annotations = new LinkedHashSet<>();
            boolean needMarker = true;

            if (base != null) {
                needMarker = false;
                Annotations stock = baseData.get(base);
                if (stock != null) {
                    shortName = stock.getShortName();
                    annotations.addAll(stock.getKeywords());
                } else if (otherSource != null) {
                    shortName = otherSource.transform(base);
                    if (shortName == null) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            boolean hackBlond = EmojiConstants.HAIR_EXPLICIT.contains(base.codePointAt(0));
            Collection<String> arguments = new ArrayList<>();
            int lastSkin = -1;

            for (int mod : CharSequences.codePoints(rem)) {
                if (ignore.contains(mod)) {
                    continue;
                }
                if (EmojiConstants.MODIFIERS.contains(mod)) {
                    if (lastSkin == mod) {
                        continue;
                    }
                    lastSkin = mod; // collapse skin tones. TODO fix if we ever do multi-skin families
                }
                Annotations stock = baseData.get(mod);
                String modName = null;
                if (stock != null) {
                    modName = stock.getShortName();
                } else if (otherSource != null) {
                    modName = otherSource.transform(base);
                }
                if (modName == null) {
                    needMarker = true;
                    if (ENGLISH_DATA != null) {
                        Annotations engName = ENGLISH_DATA.baseData.get(mod);
                        if (engName != null) {
                            modName = engName.getShortName();
                        }
                    }
                    if (modName == null) {
                        modName = Utility.hex(mod); // ultimate fallback
                    }
                }
                if (hackBlond && shortName != null) { 
                    // HACK: make the blond names look like the other hair names
                    // Split the short name into pieces, if possible, and insert the modName first
                    String sep = initialPattern.format("", "");
                    int splitPoint = shortName.indexOf(sep);
                    if (splitPoint >= 0) {
                        String modName0 = shortName.substring(splitPoint+sep.length());
                        shortName = shortName.substring(0, splitPoint);
                        if (modName != null) {
                            arguments.add(modName);
                            annotations.add(modName);
                        }
                        modName = modName0;
                    }
                    hackBlond = false;
                }

                if (modName != null) {
                    arguments.add(modName);
                    annotations.add(modName);
                }
            }
            if (!arguments.isEmpty()) {
                shortName = pattern.format(shortName, listPattern.format(arguments));
            }
            Annotations result = new Annotations(annotations, (needMarker ? ENGLISH_MARKER : "") + shortName);
            return result;
        }

        /**
         * @deprecated Use {@link #toString(String,boolean,AnnotationSet)} instead
         */
        public String toString(String code, boolean html) {
            return toString(code, html, null);
        }

        public String toString(String code, boolean html, AnnotationSet parentAnnotations) {
            if (locale.equals("be") && code.equals("🤗")) {
                int debug = 0;
            }
            String shortName = getShortName(code);
            if (shortName == null || shortName.startsWith(BAD_MARKER) || shortName.startsWith(ENGLISH_MARKER)) {
                return MISSING_MARKER;
            }

            String parentShortName = parentAnnotations == null ? null : parentAnnotations.getShortName(code);
            if (shortName != null && Objects.equal(shortName, parentShortName)) {
                shortName = EQUIVALENT;
            }

            Set<String> keywords = getKeywordsMinus(code);
            Set<String> parentKeywords = parentAnnotations == null ? null : parentAnnotations.getKeywordsMinus(code);
            if (keywords != null && !keywords.isEmpty() && Objects.equal(keywords, parentKeywords)) {
                keywords = Collections.singleton(EQUIVALENT);
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

        public UnicodeMap<Annotations> getExplicitValues() {
            return baseData;
        }

        public UnicodeMap<Annotations> getUnresolvedExplicitValues() {
            return unresolvedData;
        }

        public Set<String> getKeywordsMinus(String code) {
            String shortName = getShortName(code);
            Set<String> keywords = getKeywords(code);
            if (shortName != null && keywords.contains(shortName)) {
                keywords = new LinkedHashSet<String>(keywords);
                keywords.remove(shortName);
            }
            return keywords;
        }
    }

    public static AnnotationSet getDataSet(String locale) {
        return getDataSet(DIR, locale);
    }

    public static AnnotationSet getDataSet(String dir, String locale) {
        if (dir == null) {
            dir = DIR;
        }
        Map<String, AnnotationSet> dirCache = cache.get(dir);
        if (dirCache == null) {
            cache.put(dir, dirCache = new ConcurrentHashMap<>());
        }
        AnnotationSet result = dirCache.get(locale);
        if (result != null) {
            return result;
        }
        if (!LOCALES.contains(locale)) {
            return null;
        }
        String parentString = LocaleIDParser.getSimpleParent(locale);
        AnnotationSet parentData = null;
        if (parentString != null && !parentString.equals("root")) {
            parentData = getDataSet(dir, parentString);
        }
        MyHandler myHandler = new MyHandler(dirCache, locale, parentData);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(dir + "/" + locale + ".xml", -1, true);
        return myHandler.cleanup();
    }

    public static UnicodeMap<Annotations> getData(String locale) {
        return getData(DIR, locale);
    }

    public static UnicodeMap<Annotations> getData(String dir, String locale) {
        AnnotationSet result = getDataSet(dir, locale);
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

    public static void main(String[] args) {
        if (true) {
            writeList();
        } else {
            writeEnglish();
        }
    }

    private static void writeList() {
        AnnotationSet eng = Annotations.getDataSet("en");
        Annotations an = eng.baseData.get("❤");
        final UnicodeMap<Annotations> map = eng.getUnresolvedExplicitValues();
        Set<String> keys = new TreeSet<>(ChartAnnotations.RBC);
        map.keySet().addAllTo(keys);
//        keys.add("👩🏻‍⚖");
        for (String key : keys) {
            System.out.println(Utility.hex(key, 4, "_").toLowerCase(Locale.ROOT)
                + "\t" + key
                + "\t" + map.get(key).getShortName()
                + "\t" + CollectionUtilities.join(map.get(key).getKeywords(), " | "));
        }
        for (String s : Arrays.asList(
            "💏", "👩‍❤️‍💋‍👩",
            "💑", "👩‍❤️‍👩",
            "👪", "👩‍👩‍👧",
            "👦🏻", "👩🏿",
            "👨‍⚖", "👨🏿‍⚖", "👩‍⚖", "👩🏼‍⚖",
            "👮", "👮‍♂️", "👮🏼‍♂️", "👮‍♀️", "👮🏿‍♀️",
            "🚴", "🚴🏿", "🚴‍♂️", "🚴🏿‍♂️", "🚴‍♀️", "🚴🏿‍♀️")) {
            final String shortName = eng.getShortName(s);
            final Set<String> keywords = eng.getKeywords(s);
            System.out.println("{\"" + s + "\",\"" + shortName + "\",\"" + CollectionUtilities.join(keywords, "|") + "\"},");
        }
    }

    private static void writeEnglish() {
        AnnotationSet eng = Annotations.getDataSet("en");
        System.out.println(Annotations.getAvailable());
        AnnotationSet eng100 = Annotations.getDataSet("en_001");
        UnicodeMap<Annotations> map100 = eng100.getUnresolvedExplicitValues();
        final UnicodeMap<Annotations> map = eng.getUnresolvedExplicitValues();
        Set<String> keys = new TreeSet<>(ChartAnnotations.RBC);
        map.keySet().addAllTo(keys);
        for (String key : keys) {
            Annotations value = map.get(key);
            Annotations value100 = map100.get(key);
            Set<String> keywords100 = (value100 == null ? null : value100.getKeywords());
            System.out.println(key + "\tname\t"
                + "\t" + value.getShortName()
                + "\t" + (value100 == null ? "" : value100.getShortName())
                + "\t" + CollectionUtilities.join(value.getKeywords(), " | ")
                + "\t" + (keywords100 == null ? "" : CollectionUtilities.join(keywords100, " | ")));
        }
    }
}
