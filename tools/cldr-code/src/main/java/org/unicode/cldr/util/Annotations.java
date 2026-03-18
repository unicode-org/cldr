package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.ChartAnnotations;
import org.unicode.cldr.tool.SubdivisionNames;
import org.unicode.cldr.util.Factory.SourceTreeType;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

public class Annotations {
    private static final boolean DEBUG = false;

    public static final String BAD_MARKER = "⊗";
    public static final String MISSING_MARKER = "⊖";
    public static final String ENGLISH_MARKER = "⊕";
    public static final String EQUIVALENT = "≣";
    public static final String NEUTRAL_HOLDING = "🧑‍🤝‍🧑";

    public static final Splitter splitter =
            Splitter.on(Pattern.compile("[|;]")).trimResults().omitEmptyStrings();
    static final Splitter dotSplitter = Splitter.on(".").trimResults();

    static final Map<String, Map<String, AnnotationSet>> cache = new ConcurrentHashMap<>();
    static final Set<String> LOCALES;
    static final Set<String> ALL_LOCALES;
    static final Factory ANNOTATIONS_FACTORY;
    private static final AnnotationSet ENGLISH_DATA;

    private final Set<String> annotations;
    private final String tts;

    static final Splitter SPLIT_SPACE_OMIT = Splitter.on(" ").omitEmptyStrings();

    static {
        ANNOTATIONS_FACTORY = CLDRConfig.getInstance().getAnnotationsFactory();
        ALL_LOCALES = ANNOTATIONS_FACTORY.getAvailable();
        final Set<String> commonList = new HashSet<>();
        // calculate those in common
        for (final String loc : ALL_LOCALES) {
            final File f = getDirForLocale(loc);
            if (SimpleFactory.getSourceTreeType(f) == SourceTreeType.common) {
                commonList.add(loc);
            }
        }
        LOCALES = Collections.unmodifiableSet(commonList);
        ENGLISH_DATA = getDataSet("en");
    }

    static class MyHandler extends SimpleHandler {
        private final String locale;
        private final UnicodeMap<Annotations> localeData = new UnicodeMap<>();
        private final AnnotationSet parentData;
        private final Map<String, AnnotationSet> dirCache;

        public MyHandler(
                Map<String, AnnotationSet> dirCache, String locale, AnnotationSet parentData) {
            this.locale = locale;
            this.parentData = parentData;
            this.dirCache = dirCache;
        }

        public AnnotationSet cleanup() {
            // add parent data (may be overridden)
            UnicodeMap<Annotations> templocaleData = null;
            if (parentData != null) {
                templocaleData = new UnicodeMap<>();
                UnicodeSet keys =
                        new UnicodeSet(parentData.baseData.keySet()).addAll(localeData.keySet());
                for (String key : keys) {
                    Annotations parentValue = parentData.baseData.get(key);
                    Annotations myValue = localeData.get(key);
                    if (parentValue == null) {
                        templocaleData.put(key, myValue);
                    } else if (myValue == null) {
                        templocaleData.put(key, parentValue);
                    } else { // need to combine
                        String tts = myValue.tts == null ? parentValue.tts : myValue.tts;
                        Set<String> annotations =
                                myValue.annotations == null || myValue.annotations.isEmpty()
                                        ? parentValue.annotations
                                        : myValue.annotations;
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
            if (value.contains(CldrUtility.INHERITANCE_MARKER)) {
                return; // skip all ^^^
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String lastElement = parts.getElement(-1);
            if (!lastElement.equals("annotation")) {
                if (!"identity".equals(parts.getElement(1))) {
                    throw new IllegalArgumentException("Unexpected path");
                }
                return;
            }
            String usString = parts.getAttributeValue(-1, "cp");
            UnicodeSet us1 =
                    usString.startsWith("[") && usString.endsWith("]")
                            ? new UnicodeSet(usString)
                            : new UnicodeSet().add(usString);
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
                addItems(localeData, us, Collections.<String>emptySet(), value);
            } else {
                Set<String> attributes = new TreeSet<>(splitter.splitToList(value));
                addItems(localeData, us, attributes, tts);
            }
        }

        private void addItems(
                UnicodeMap<Annotations> unicodeMap,
                UnicodeSet us,
                Set<String> attributes,
                String tts) {
            for (String entry : us) {
                addItems(unicodeMap, entry, attributes, tts);
            }
        }

        private void addItems(
                UnicodeMap<Annotations> unicodeMap,
                String entry,
                Set<String> attributes,
                String tts) {
            Annotations annotations = unicodeMap.get(entry);
            if (annotations == null) {
                unicodeMap.put(entry, new Annotations(attributes, tts));
            } else {
                unicodeMap.put(entry, annotations.add(attributes, tts)); // creates new item
            }
        }
    }

    public Annotations(Set<String> attributes, String tts2) {
        annotations =
                attributes == null
                        ? Collections.<String>emptySet()
                        : ImmutableSet.copyOf(attributes);
        for (String attr : annotations) {
            if (attr.contains(CldrUtility.INHERITANCE_MARKER)) {
                throw new IllegalArgumentException(CldrUtility.INHERITANCE_MARKER);
            }
        }
        tts = tts2;
        if (tts != null && tts.contains(CldrUtility.INHERITANCE_MARKER)) {
            throw new IllegalArgumentException(CldrUtility.INHERITANCE_MARKER);
        }
    }

    public Annotations add(Set<String> attributes, String tts2) {
        return new Annotations(
                getKeywords() == null
                        ? attributes
                        : attributes == null ? getKeywords() : union(attributes, getKeywords()),
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

    /**
     * @return all common locales
     */
    public static Set<String> getAvailable() {
        return LOCALES;
    }

    /**
     * @return all common locales
     */
    public static Set<String> getAvailableLocales() {
        return LOCALES;
    }

    /**
     * @return all locales, including seed
     */
    public static Set<String> getAllAvailable() {
        return ALL_LOCALES;
    }

    public static final class AnnotationSet {

        private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

        static final Factory factory = CONFIG.getCldrFactory();
        static final CLDRFile ENGLISH = CONFIG.getEnglish();
        static final CLDRFile ENGLISH_ANNOTATIONS = null;
        static final SubdivisionNames englishSubdivisionIdToName =
                new SubdivisionNames("en", "main");

        private static final String BLACK_RIGHTWARDS_ARROW = "\u27A1";

        private static final String JOINER_RIGHTWARDS =
                EmojiConstants.JOINER_STRING + BLACK_RIGHTWARDS_ARROW;
        private static final String BLACK_LEFTWARDS_ARROW = "\u2B05";
        // CLDRConfig.getInstance().getAnnotationsFactory().make("en", false);

        private final String locale;
        private final UnicodeMap<Annotations> baseData;
        private final UnicodeMap<Annotations> unresolvedData;
        private final CLDRFile cldrFile;
        private final SubdivisionNames subdivisionIdToName;
        private final SimpleFormatter initialPattern;
        private final SimpleFormatter rightwardsArrowPattern;
        private final Pattern initialRegexPattern;
        private final XListFormatter listPattern;
        private final Set<String> flagLabelSet;
        private final Set<String> keycapLabelSet;
        private final String keycapLabel;
        private final String flagLabel;
        //        private final String maleLabel;
        //        private final String femaleLabel;
        private final Map<String, Annotations> localeCache = new ConcurrentHashMap<>();

        static UnicodeSetSpanner uss =
                new UnicodeSetSpanner(EmojiConstants.COMPONENTS); // must be sync'ed

        private AnnotationSet(
                String locale,
                UnicodeMap<Annotations> source,
                UnicodeMap<Annotations> resolvedSource) {
            this.locale = locale;
            unresolvedData = source.freeze();
            this.baseData = resolvedSource == null ? unresolvedData : resolvedSource.freeze();
            cldrFile = factory.make(locale, true);
            subdivisionIdToName = new SubdivisionNames(locale, "main", "subdivisions");
            // EmojiSubdivisionNames.getSubdivisionIdToName(locale);
            listPattern = new XListFormatter(cldrFile, EmojiConstants.COMPOSED_NAME_LIST);
            final String initialPatternString =
                    getStringValue(
                            "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]");
            initialPattern = SimpleFormatter.compile(initialPatternString);
            //      <characterLabelPattern type="facing-right">{0} facing
            // right</characterLabelPattern>
            final String facingRightPatternString =
                    getStringValue(
                            "//ldml/characterLabels/characterLabelPattern[@type=\"facing-right\"]");

            rightwardsArrowPattern =
                    facingRightPatternString == null
                            ? null
                            : SimpleFormatter.compile(facingRightPatternString);
            final String regexPattern =
                    ("\\Q"
                                    + initialPatternString
                                            .replace("{0}", "\\E.*\\Q")
                                            .replace("{1}", "\\E.*\\Q")
                                    + "\\E")
                            .replace("\\Q\\E", ""); // HACK to detect use of prefix pattern
            initialRegexPattern = Pattern.compile(regexPattern);
            flagLabelSet = getLabelSet("flag");
            flagLabel = flagLabelSet.isEmpty() ? null : flagLabelSet.iterator().next();
            keycapLabelSet = getLabelSet("keycap");
            keycapLabel = keycapLabelSet.isEmpty() ? null : keycapLabelSet.iterator().next();
            //            maleLabel =
            // getStringValue("//ldml/characterLabels/characterLabel[@type=\"male\"]");
            //            femaleLabel =
            // getStringValue("//ldml/characterLabels/characterLabel[@type=\"female\"]");
        }

        /**
         * @deprecated Use {@link #getLabelSet(String)} instead
         */
        @Deprecated
        private Set<String> getLabelSet() {
            return getLabelSet("flag");
        }

        private Set<String> getLabelSet(String typeAttributeValue) {
            String label =
                    getStringValue(
                            "//ldml/characterLabels/characterLabel[@type=\""
                                    + typeAttributeValue
                                    + "\"]");
            return label == null ? Collections.<String>emptySet() : Collections.singleton(label);
        }

        private String getStringValue(String xpath) {
            return getStringValue(xpath, cldrFile, ENGLISH);
        }

        private String getStringValue(String xpath, CLDRFile cldrFile2, CLDRFile english) {
            String result = cldrFile2.getStringValueWithBailey(xpath);
            if (result == null) {
                return ENGLISH_MARKER + english.getStringValueWithBailey(xpath);
            }
            String sourceLocale = cldrFile2.getSourceLocaleID(xpath, null);
            if (sourceLocale.equals(XMLSource.CODE_FALLBACK_ID)
                    || sourceLocale.equals(XMLSource.ROOT_ID)) {
                if (!xpath.equals(
                        "//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]")) {
                    return MISSING_MARKER + result;
                }
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
            return Collections.<String>emptySet();
        }

        /**
         * Returns the set of all keys for which annotations are available. WARNING: keys have the
         * Emoji Presentation Selector removed!
         */
        public UnicodeSet keySet() {
            return baseData.keySet();
        }

        /**
         * Public only for testing. This code needed to be modified when the Emoji subcommittee adds
         * new compound emoji. See also TestAnnotations.testCompleteness
         */
        public Annotations synthesize(final String code, Transform<String, String> otherSource) {
            if (code.equals("👱🏻‍♂")) {
                int debug = 0;
            }
            String shortName = null;
            final int len = code.codePointCount(0, code.length());
            String base = code.replace(EmojiConstants.EMOJI_VARIANT_STRING, "");
            boolean isKeycap10 = base.equals("🔟");
            if (len == 1 && !isKeycap10) {
                String tempName = null;
                if (locale.equals("en")) {
                    if (otherSource != null) {
                        tempName = otherSource.transform(base);
                    }
                    if (tempName == null) {
                        return null;
                    }
                    return new Annotations(Collections.<String>emptySet(), tempName);
                } else { // fall back to English if possible, but mark it.
                    tempName = getDataSet("en").getShortName(base);
                    if (tempName == null) {
                        return null;
                    }
                    return new Annotations(
                            Collections.<String>emptySet(), ENGLISH_MARKER + tempName);
                }
            } else if (EmojiConstants.REGIONAL_INDICATORS.containsAll(base)) {
                String countryCode = EmojiConstants.getFlagCode(base);
                String path = NameType.TERRITORY.getKeyPath(countryCode);
                String regionName = getStringValue(path);
                if (regionName == null) {
                    regionName = ENGLISH_MARKER + ENGLISH.getStringValueWithBailey(path);
                }
                String flagName =
                        flagLabel == null
                                ? regionName
                                : initialPattern.format(flagLabel, regionName);
                return new Annotations(flagLabelSet, flagName);
            } else if (base.startsWith(EmojiConstants.BLACK_FLAG)
                    && base.endsWith(EmojiConstants.TAG_TERM)) {
                String subdivisionCode = EmojiConstants.getTagSpec(base);
                String subdivisionName = subdivisionIdToName.get(subdivisionCode);
                if (subdivisionName == null) {
                    //                    subdivisionName =
                    // englishSubdivisionIdToName.get(subdivisionCode);
                    //                    if (subdivisionName != null) {
                    //                        subdivisionName = ENGLISH_MARKER + subdivisionCode;
                    //                    } else {
                    subdivisionName = MISSING_MARKER + subdivisionCode;
                    //                    }
                }
                String flagName =
                        flagLabel == null
                                ? subdivisionName
                                : initialPattern.format(flagLabel, subdivisionName);
                return new Annotations(flagLabelSet, flagName);
            } else if (isKeycap10 || base.contains(EmojiConstants.KEYCAP_MARK_STRING)) {
                final String rem = base.equals("🔟") ? "10" : UTF16.valueOf(base.charAt(0));
                shortName = initialPattern.format(keycapLabel, rem);
                return new Annotations(keycapLabelSet, shortName);
            }
            UnicodeSet skipSet = EmojiConstants.REM_SKIP_SET;
            String rem = "";
            SimpleFormatter startPattern = initialPattern;
            if (EmojiConstants.COMPONENTS.containsSome(base)) {
                synchronized (uss) {
                    rem = uss.deleteFrom(base, SpanCondition.NOT_CONTAINED);
                    base = uss.deleteFrom(base, SpanCondition.CONTAINED);
                }
            }
            boolean DEBUG = false;
            // This is typically the place to add new compound emoji

            if (base.contains(EmojiConstants.JOINER_STRING)) {
                if (base.contains(JOINER_RIGHTWARDS)) {
                    base = base.replace(JOINER_RIGHTWARDS, "");
                    rem += BLACK_RIGHTWARDS_ARROW;
                    // fall through because it might contain male/female sign
                }
                if (base.contains(EmojiConstants.KISS)) {
                    rem = base + rem;
                    base = "💏";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (base.contains(EmojiConstants.HEART)
                        && !base.startsWith(EmojiConstants.HEART)) {
                    rem = base + rem;
                    base = "💑";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (base.equals(EmojiConstants.COMPOSED_HANDSHAKE)) {
                    base = EmojiConstants.HANDSHAKE;
                } else if (base.contains(EmojiConstants.HANDSHAKE)) {
                    base = pickGender3(base, "👬", "👫", "👭", NEUTRAL_HOLDING);
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (base.contains("👯")) {
                    // Base is like the following
                    // 👯 E0.6 people with bunny ears
                    // 👯🏻 E17.0 people with bunny ears: light skin tone
                    // We have to fix the gender, because it is separated from the skintone
                    // 👯🏻‍♂️ E17.0 men with bunny ears: light skin tone
                    skipSet = EmojiConstants.REM_PERSON_SKIP_SET;
                } else if (base.contains("🐰")) { // fight-cloud
                    // Base is like the following
                    // 👯 E0.6 people with bunny ears
                    // 👯🏻 E17.0 people with bunny ears: light skin tone
                    // We have to map the substitute sequences, like
                    // 🧑🏻‍🐰‍🧑🏼 E17.0 people with bunny ears: light skin tone, medium-light skin
                    // tone
                    // 👨🏻‍🐰‍👨🏼 E17.0 men with bunny ears: light skin tone, medium-light skin
                    // tone
                    base = "👯" + pickGender2(base);
                    skipSet = EmojiConstants.REM_PERSON_SKIP_SET;
                } else if (base.startsWith("🤼")) { // wrestlers
                    // Base is like the following
                    // # 🤼 E3.0 people wrestling
                    // # 🤼‍♂️ E4.0 men wrestling
                    // We have to fix the gender, because it is separated from the skintone
                    // # 🤼🏻‍♂️ E17.0 men wrestling: light skin tone
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                } else if (base.contains("🫯")) { // fight-cloud
                    // Base is like the following
                    // # 🤼 E3.0 people wrestling
                    // # 🤼‍♂️ E4.0 men wrestling
                    // We have to map the substitute sequences, like
                    //  # 🧑🏻‍🫯‍🧑🏼 E17.0 people wrestling: light skin tone, medium-light skin
                    // tone
                    base = "🤼" + pickGender2(base);
                    skipSet = EmojiConstants.REM_PERSON_SKIP_SET;
                } else if (EmojiConstants.FAMILY_MARKERS.containsAll(base)) {
                    rem = base + rem;
                    base = "👪";
                    skipSet = EmojiConstants.REM_GROUP_SKIP_SET;
                    //                } else {
                    //                    startPattern = listPattern;
                }
                // left over is "👨🏿‍⚖","judge: man, dark skin tone"
            }
            // This composes a name from a base (code)
            // plus rem (the remaining items: skin modifiers and/or gender modifiers)
            // The skipSet are items to ignore in the rem.
            // The startPattern is constant (for the locale)
            // The otherSource is used by the unicodetools, and shouldn't be changed.
            if (DEBUG) {
                System.out.println(show(code, base, rem) + "\n" + skipSet.toPattern(false));
            }
            Annotations result =
                    getBasePlusRemainder(cldrFile, base, rem, skipSet, startPattern, otherSource);
            if (DEBUG) {
                System.out.println(result.tts);
            }
            return result;
        }

        private String show(String... strings) {
            return List.of(strings).stream()
                    .map(x -> Utility.hex(x) + " " + UCharacter.getName(x, ", "))
                    .collect(Collectors.joining("\n"));
        }

        private String pickGender2(String code) {
            return code.startsWith(EmojiConstants.MAN)
                    ? EmojiConstants.JOINER + EmojiConstants.MALE_SIGN
                    : code.startsWith(EmojiConstants.WOMAN)
                            ? EmojiConstants.JOINER + EmojiConstants.FEMALE_SIGN
                            : "";
        }

        private String pickGender3(
                String code, String manStart, String manEnd, String womanStart, String neutral) {
            return code.startsWith(EmojiConstants.MAN)
                    ? manStart
                    : code.endsWith(EmojiConstants.MAN)
                            ? manEnd
                            : code.startsWith(EmojiConstants.WOMAN) ? womanStart : neutral;
        }

        private boolean matchesInitialPattern(String code) {
            Annotations baseAnnotation = baseData.get(code);
            String baseName = baseAnnotation == null ? null : baseAnnotation.getShortName();
            return baseName != null && initialRegexPattern.matcher(baseName).matches();
        }

        /**
         * Constructs a name from pieces. There are lots of exceptions because the emoji structure
         * is very inconsistent.
         *
         * @param base Matches what should be in the annotations directory. For example, it might be
         *     WOMAN WITH BUNNY EARS, ZERO WIDTH JOINER, MALE SIGN.
         * @param rem Contains additional characters whose names are to be appended to the base's
         *     name, using the pattern.
         * @param ignore Contains characters that are ignored in the rem.
         * @param pattern The pattern used to combine base name plus rem names.
         * @param otherSource Used in Unicodetools to get data supplied from the UCD for English.
         * @return
         */
        private Annotations getBasePlusRemainder(
                CLDRFile cldrFile,
                String base,
                String rem,
                UnicodeSet ignore,
                SimpleFormatter pattern,
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
                } else {
                    return null;
                }
                if (shortName == null) {
                    return null;
                }
            }

            boolean hackBlond = EmojiConstants.HAIR_EXPLICIT.contains(base.codePointAt(0));
            Collection<String> arguments = new ArrayList<>();
            int lastSkin = -1;
            boolean addRightFacing = false;
            for (int mod : CharSequences.codePoints(rem)) {
                if (ignore.contains(mod)) {
                    continue;
                }
                if (mod == BLACK_RIGHTWARDS_ARROW.codePointAt(0)) {
                    addRightFacing = true;
                    continue;
                }
                if (EmojiConstants.MODIFIERS.contains(mod)) {
                    if (lastSkin == mod) {
                        continue;
                    }
                    lastSkin =
                            mod; // collapse skin tones. TODO fix if we ever do multi-skin families
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
                        String modName0 = shortName.substring(splitPoint + sep.length());
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
            if (addRightFacing) {
                final String rightFacing = rightwardsArrowPattern.format("").trim();
                arguments.add(rightFacing);
                annotations.addAll(SPLIT_SPACE_OMIT.splitToList(rightFacing));
            }
            if (!arguments.isEmpty()) {
                shortName = pattern.format(shortName, listPattern.format(arguments));
            }
            Annotations result =
                    new Annotations(annotations, (needMarker ? ENGLISH_MARKER : "") + shortName);
            return result;
        }

        /**
         * @deprecated Use {@link #toString(String,boolean,AnnotationSet)} instead
         */
        @Deprecated
        public String toString(String code, boolean html) {
            return toString(code, html, null);
        }

        public String toString(String code, boolean html, AnnotationSet parentAnnotations) {
            if (locale.equals("be") && code.equals("🤗")) {
                int debug = 0;
            }
            String shortName = getShortName(code);
            if (shortName == null
                    || shortName.startsWith(BAD_MARKER)
                    || shortName.startsWith(ENGLISH_MARKER)) {
                return MISSING_MARKER;
            }

            String parentShortName =
                    parentAnnotations == null ? null : parentAnnotations.getShortName(code);
            if (shortName != null && Objects.equal(shortName, parentShortName)) {
                shortName = EQUIVALENT;
            }

            Set<String> keywords = getKeywordsMinus(code);
            Set<String> parentKeywords =
                    parentAnnotations == null ? null : parentAnnotations.getKeywordsMinus(code);
            if (keywords != null
                    && !keywords.isEmpty()
                    && Objects.equal(keywords, parentKeywords)) {
                keywords = Collections.singleton(EQUIVALENT);
            }

            String result = Joiner.on(" |\u00a0").join(keywords);
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
                keywords = new LinkedHashSet<>(keywords);
                keywords.remove(shortName);
            }
            return keywords;
        }
    }

    public static AnnotationSet getDataSet(String locale) {
        final File theDir = getDirForLocale(locale);
        return getDataSet(theDir.getAbsolutePath(), locale);
    }

    private static File getDirForLocale(String locale) {
        // use the annotations Factory to find the XML file
        List<File> dirs = ANNOTATIONS_FACTORY.getSourceDirectoriesForLocale(locale);
        if (dirs == null || dirs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot find source annotation directory for locale " + locale);
        } else if (dirs.size() != 1) {
            throw new IllegalArgumentException(
                    "Did not find exactly one source directory for locale "
                            + locale
                            + " - "
                            + dirs);
        }
        final File theDir = dirs.get(0);
        return theDir;
    }

    public static AnnotationSet getDataSet(String dir, String locale) {
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
        String parentString = LocaleIDParser.getParent(locale);
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
        final File theDir = getDirForLocale(locale);
        return getData(theDir.getAbsolutePath(), locale);
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
            annotations2 = new LinkedHashSet<>(getKeywords());
            annotations2.remove(getShortName());
        }
        String result = Joiner.on(" |\u00a0").join(annotations2);
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
            System.out.println(
                    Utility.hex(key, 4, "_").toLowerCase(Locale.ROOT)
                            + "\t"
                            + key
                            + "\t"
                            + map.get(key).getShortName()
                            + "\t"
                            + Joiner.on(" | ").join(map.get(key).getKeywords()));
        }
        for (String s :
                Arrays.asList(
                        "💏",
                        "👩‍❤️‍💋‍👩",
                        "💑",
                        "👩‍❤️‍👩",
                        "👪",
                        "👩‍👩‍👧",
                        "👦🏻",
                        "👩🏿",
                        "👨‍⚖",
                        "👨🏿‍⚖",
                        "👩‍⚖",
                        "👩🏼‍⚖",
                        "👮",
                        "👮‍♂️",
                        "👮🏼‍♂️",
                        "👮‍♀️",
                        "👮🏿‍♀️",
                        "🚴",
                        "🚴🏿",
                        "🚴‍♂️",
                        "🚴🏿‍♂️",
                        "🚴‍♀️",
                        "🚴🏿‍♀️")) {
            final String shortName = eng.getShortName(s);
            final Set<String> keywords = eng.getKeywords(s);
            System.out.println(
                    "{\""
                            + s
                            + "\",\""
                            + shortName
                            + "\",\""
                            + Joiner.on("|").join(keywords)
                            + "\"},");
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
            System.out.println(
                    key
                            + "\tname\t"
                            + "\t"
                            + value.getShortName()
                            + "\t"
                            + (value100 == null ? "" : value100.getShortName())
                            + "\t"
                            + Joiner.on(" | ").join(value.getKeywords())
                            + "\t"
                            + (keywords100 == null ? "" : Joiner.on(" | ").join(keywords100)));
        }
    }
}
