package org.unicode.cldr.unittest;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XListFormatter;
import org.unicode.cldr.util.XListFormatter.ListTypeLength;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class TestAnnotations extends TestFmwkPlus {
    private static final boolean DEBUG = false;
    private static final boolean TEST_ONLY_ENGLISH_UNIQUENESS = false;

    public static void main(String[] args) {
        new TestAnnotations().run(args);
    }

    enum Containment {
        contains,
        empty,
        not_contains
    }

    public void TestBasic() {
        String[][] tests = {
            {"en", "[\u2650]", "contains", "sagitarius", "zodiac"},
            {"en", "[\u0020]", "empty"},
            {"en", "[\u2651]", "not_contains", "foobar"},
        };
        for (String[] test : tests) {
            UnicodeMap<Annotations> data = Annotations.getData(test[0]);
            UnicodeSet us = new UnicodeSet(test[1]);
            Set<String> annotations = new LinkedHashSet<>();
            Containment contains = Containment.valueOf(test[2]);
            for (int i = 3; i < test.length; ++i) {
                annotations.add(test[i]);
            }
            for (String s : us) {
                Set<String> set = data.get(s).getKeywords();
                if (set == null) {
                    set = Collections.emptySet();
                }
                switch (contains) {
                    case contains:
                        if (Collections.disjoint(set, annotations)) {
                            LinkedHashSet<String> temp = new LinkedHashSet<>(annotations);
                            temp.removeAll(set);
                            assertEquals("Missing items", Collections.EMPTY_SET, temp);
                        }
                        break;
                    case not_contains:
                        if (!Collections.disjoint(set, annotations)) {
                            LinkedHashSet<String> temp = new LinkedHashSet<>(annotations);
                            temp.retainAll(set);
                            assertEquals("Extra items", Collections.EMPTY_SET, temp);
                        }
                        break;
                    case empty:
                        assertEquals("mismatch", Collections.emptySet(), set);
                        break;
                }
            }
        }
    }

    final AnnotationSet eng = Annotations.getDataSet("en");

    public void TestNames() {
        if (true) return; // Skip this test until the English annotations settle down.
        String[][] tests = { // the expected value for keywords can use , as well as |.
            {"👨🏻", "man: light skin tone", "adult | man | light skin tone"},
            {"👱‍♂️", "man: blond hair", "blond, blond-haired man, hair, man, man: blond hair"},
            {
                "👱🏻‍♂️",
                "man: light skin tone, blond hair",
                "blond, blond-haired man, hair, man, man: blond hair, light skin tone, blond hair"
            },
            {"👨‍🦰", "man: red hair", "adult | man | red hair"},
            {
                "👨🏻‍🦰",
                "man: light skin tone, red hair",
                "adult | man | light skin tone| red hair"
            },
            {"🇪🇺", "flag: European Union", "flag"},
            {"#️⃣", "keycap: #", "keycap"},
            {"9️⃣", "keycap: 9", "keycap"},
            {"💏", "kiss", "couple | kiss"},
            {"👩‍❤️‍💋‍👩", "kiss: woman, woman", "couple | kiss | woman"},
            {"💑", "couple with heart", "couple | couple with heart | love"},
            {
                "👩‍❤️‍👩",
                "couple with heart: woman, woman",
                "couple | couple with heart | love | woman"
            },
            {"👪", "family", "family"},
            {"👩‍👩‍👧", "family: woman, woman, girl", "family | woman | girl"},
            {"👦🏻", "boy: light skin tone", "boy | young | light skin tone"},
            {"👩🏿", "woman: dark skin tone", "adult | woman | dark skin tone"},
            {"👨‍⚖", "man judge", "judge | justice | law | man | scales"},
            {
                "👨🏿‍⚖",
                "man judge: dark skin tone",
                "judge | justice | law | man | scales | dark skin tone"
            },
            {"👩‍⚖", "woman judge", "judge | justice | law | scales | woman"},
            {
                "👩🏼‍⚖",
                "woman judge: medium-light skin tone",
                "judge | justice | law | scales | woman | medium-light skin tone"
            },
            {"👮", "police officer", "cop | officer | police"},
            {"👮🏿", "police officer: dark skin tone", "cop | officer | police | dark skin tone"},
            {"👮‍♂️", "man police officer", "cop | man | officer | police"},
            {
                "👮🏼‍♂️",
                "man police officer: medium-light skin tone",
                "cop | man | officer | police | medium-light skin tone"
            },
            {"👮‍♀️", "woman police officer", "cop | officer | police | woman"},
            {
                "👮🏿‍♀️",
                "woman police officer: dark skin tone",
                "cop | officer | police | woman | dark skin tone"
            },
            {"🚴", "person biking", "bicycle | biking | cyclist | person biking"},
            {
                "🚴🏿",
                "person biking: dark skin tone",
                "bicycle | biking | cyclist | person biking | dark skin tone"
            },
            {"🚴‍♂️", "man biking", "bicycle | biking | cyclist | man"},
            {
                "🚴🏿‍♂️",
                "man biking: dark skin tone",
                "bicycle | biking | cyclist | man | dark skin tone"
            },
            {"🚴‍♀️", "woman biking", "bicycle | biking | cyclist | woman"},
            {
                "🚴🏿‍♀️",
                "woman biking: dark skin tone",
                "bicycle | biking | cyclist | woman | dark skin tone"
            },
        };

        Splitter BAR = Splitter.on(CharMatcher.anyOf("|,")).trimResults();
        boolean ok = true;
        for (String[] test : tests) {
            String emoji = test[0];
            String expectedName = test[1];
            Set<String> expectedKeywords = new HashSet<>(BAR.splitToList(test[2]));
            final String shortName = eng.getShortName(emoji);
            final Set<String> keywords = eng.getKeywords(emoji);
            ok &= assertEquals("short name for " + emoji, expectedName, shortName);
            ok &= assertEquals("keywords for " + emoji, expectedKeywords, keywords);
        }
        if (!ok) {
            System.out.println("Possible replacement, but check");
            for (String[] test : tests) {
                String emoji = test[0];
                final String shortName = eng.getShortName(emoji);
                final Set<String> keywords = eng.getKeywords(emoji);
                System.out.println(
                        "{\""
                                + emoji
                                + "\",\""
                                + shortName
                                + "\",\""
                                + Joiner.on(" | ").join(keywords)
                                + "\"},");
            }
        }
    }

    static final UnicodeSet symbols =
            new UnicodeSet(Emoji.EXTRA_SYMBOL_MINOR_CATEGORIES.keySet()).freeze();

    /** The English name should line up with the emoji-test.txt file */
    public void TestNamesVsEmojiData() {
        for (Entry<String, Annotations> s : eng.getExplicitValues().entrySet()) {
            String emoji = s.getKey();
            Annotations annotations = s.getValue();
            String name = Emoji.getName(emoji);
            String annotationName = annotations.getShortName();
            if (!symbols.contains(emoji)
                    && !emoji.contains("👲")
                    && !emoji.contains("🧑")
                    && !emoji.contains("\u20E3")) {
                assertEquals(emoji + " (en.xml vs. emoji-test.txt)", name, annotationName);
            }
        }
    }

    public void TestCategories() {
        if (DEBUG) System.out.println();

        TreeSet<R4<PageId, Long, String, R3<String, String, String>>> sorted = new TreeSet<>();
        for (Entry<String, Annotations> s : eng.getExplicitValues().entrySet()) {
            String emoji = s.getKey();
            Annotations annotations = s.getValue();
            final String rawCategory = Emoji.getMajorCategory(emoji);
            // Note: this call to PageId.forString possibly assumes it throws an exception if
            // rawCategory isn't recognized as a page ID.
            PageId majorCategory = PageId.forString(rawCategory);
            if (majorCategory == PageId.Symbols) {
                majorCategory = PageId.EmojiSymbols;
            }
            String minorCategory = Emoji.getMinorCategory(emoji);
            long emojiOrder = Emoji.getEmojiToOrder(emoji);
            R3<String, String, String> row2 =
                    Row.of(
                            emoji,
                            annotations.getShortName(),
                            Joiner.on(" | ").join(annotations.getKeywords()));
            R4<PageId, Long, String, R3<String, String, String>> row =
                    Row.of(majorCategory, emojiOrder, minorCategory, row2);
            sorted.add(row);
        }
        for (R4<PageId, Long, String, R3<String, String, String>> row : sorted) {
            PageId majorCategory = row.get0();
            Long emojiOrder = row.get1();
            String minorCategory = row.get2();
            R3<String, String, String> row2 = row.get3();
            String emoji = row2.get0();
            String shortName = row2.get1();
            String keywords = row2.get2();
            if (DEBUG)
                System.out.println(
                        majorCategory
                                + "\t"
                                + emojiOrder
                                + "\t"
                                + minorCategory
                                + "\t"
                                + emoji
                                + "\t"
                                + shortName
                                + "\t"
                                + keywords);
        }
    }

    public void TestUniqueness() {
        //        if (logKnownIssue(
        //                "CLDR-16947", "skip duplicate TestUniqueness in favor of
        // CheckDisplayCollisions")) {
        //            return;
        //        }
        Set<String> locales = new TreeSet<>();
        locales.add("en");
        if (!TEST_ONLY_ENGLISH_UNIQUENESS) {
            locales.addAll(Annotations.getAvailable());
            locales.remove("root");
        }
        /*
         * Note: "problems" here is a work-around for what appears to be a deficiency
         * in the function sourceLocation, involving the call stack. Seemingly sourceLocation
         * can't handle the "->" notation used for parallelStream().forEach() if
         * uniquePerLocale calls errln directly.
         */
        Set<String> problems = new HashSet<>();
        locales.parallelStream().forEach(locale -> uniquePerLocale(locale, problems));
        if (!problems.isEmpty()) {
            problems.forEach(s -> errln(s));
        }
    }

    private void uniquePerLocale(String locale, Set<String> problems) {
        logln("uniqueness: " + locale);
        Multimap<String, String> nameToEmoji = TreeMultimap.create();
        AnnotationSet data = Annotations.getDataSet(locale);
        for (String emoji : Emoji.getAllRgi()) {
            String name = data.getShortName(emoji);
            if (name == null) {
                continue;
            }
            if (name.contains(CldrUtility.INHERITANCE_MARKER)) {
                throw new IllegalArgumentException(
                        CldrUtility.INHERITANCE_MARKER + " in name of " + emoji + " in " + locale);
            }
            nameToEmoji.put(name, emoji);
        }
        Multimap<String, String> duplicateNameToEmoji = null;
        for (Entry<String, Collection<String>> entry : nameToEmoji.asMap().entrySet()) {
            String name = entry.getKey();
            Collection<String> emojis = entry.getValue();
            if (emojis.size() > 1) {
                synchronized (problems) {
                    if (problems.add(
                            "Duplicate name in "
                                    + locale
                                    + ": “"
                                    + name
                                    + "” for "
                                    + Joiner.on(" & ").join(emojis))) {
                        int debug = 0;
                    }
                }
                if (duplicateNameToEmoji == null) {
                    duplicateNameToEmoji = TreeMultimap.create();
                }
                duplicateNameToEmoji.putAll(name, emojis);
            }
        }
        if (isVerbose() && duplicateNameToEmoji != null && !duplicateNameToEmoji.isEmpty()) {
            System.out.println("\nCollisions");
            for (Entry<String, String> entry : duplicateNameToEmoji.entries()) {
                String emoji = entry.getValue();
                System.out.println(locale + "\t" + eng.getShortName(emoji) + "\t" + emoji);
            }
        }
    }

    public void testAnnotationPaths() {
        assertTrue("", Emoji.getNonConstructed().contains("®"));
        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        for (String locale : Arrays.asList("en", "root")) {
            CLDRFile enAnnotations = factoryAnnotations.make(locale, false);
            //               //ldml/annotations/annotation[@cp="🧜"][@type="tts"]
            Set<String> annotationPaths =
                    enAnnotations.getPaths(
                            "//ldml/anno",
                            Pattern.compile("//ldml/annotations/annotation.*tts.*").matcher(""),
                            new TreeSet<>());
            Set<String> annotationPathsExpected = Emoji.getNamePaths();
            if (!checkAMinusBIsC(
                    "(" + locale + ".xml - Emoji.getNamePaths)",
                    annotationPaths,
                    annotationPathsExpected,
                    Collections.<String>emptySet())) {
                System.out.println("Check Emoji.SPECIALS");
            }
            checkAMinusBIsC(
                    "(Emoji.getNamePaths - " + locale + ".xml)",
                    annotationPathsExpected,
                    annotationPaths,
                    Collections.<String>emptySet());
        }
    }

    /** Check that the order info, categories, and collation are consistent. */
    public void testEmojiOrdering() {
        // load an array for sorting
        // and test that every order value maps to exactly one emoji
        Map<String, String> minorToMajor = new HashMap<>();
        Map<Long, String> orderToEmoji = new TreeMap<>();
        Collator col = CLDRConfig.getInstance().getCollatorRoot();

        for (String emoji : Emoji.getNonConstructed()) {
            Long emojiOrder = Emoji.getEmojiToOrder(emoji);
            if (DEBUG) {
                String minor = Emoji.getMinorCategory(emoji);
                System.out.println(emojiOrder + "\t" + emoji + "\t" + minor);
            }
            String oldEmoji = orderToEmoji.get(emojiOrder);
            if (oldEmoji == null) {
                orderToEmoji.put(emojiOrder, emoji);
            } else {
                errln("single order value with different emoji" + emoji + " ≠ " + oldEmoji);
            }
        }
        Set<String> majorsSoFar = new TreeSet<>();
        String lastMajor = "";
        Set<String> minorsSoFar = new TreeSet<>();
        String lastMinor = "";
        Set<String> lastMajorGroup = new LinkedHashSet<>();
        Set<String> lastMinorGroup = new LinkedHashSet<>();
        String lastEmoji = "";
        long lastEmojiOrdering = -1L;
        for (Entry<Long, String> entry : orderToEmoji.entrySet()) {
            String emoji = entry.getValue();
            Long emojiOrdering = entry.getKey();
            // check against collation
            if (col.compare(emoji, lastEmoji) <= 0) {
                String name = eng.getShortName(emoji);
                String lastName = eng.getShortName(lastEmoji);
                int errorType = ERR;
                if (logKnownIssue("CLDR-16394", "slightly out of order")) {
                    errorType = WARN;
                }
                msg(
                        "Out of order: "
                                + lastEmoji
                                + " ("
                                + lastEmojiOrdering
                                + ") "
                                + lastName
                                + " > "
                                + emoji
                                + " ("
                                + emojiOrdering
                                + ") "
                                + name,
                        errorType,
                        true,
                        true);
            }

            String major = Emoji.getMajorCategory(emoji);
            String minor = Emoji.getMinorCategory(emoji);
            if (isVerbose()) {
                System.out.println(major + "\t" + minor + "\t" + emoji);
            }
            String oldMajor = minorToMajor.get(minor);
            // never get major1:minor1 and major2:minor1
            if (oldMajor == null) {
                minorToMajor.put(minor, major);
            } else {
                assertEquals(
                        minor + " maps to different majors for " + Utility.hex(emoji),
                        oldMajor,
                        major);
            }
            // never get major1 < major2 < major1
            if (!major.equals(lastMajor)) {
                // System.out.println(lastMajor + "\t" + lastMajorGroup);

                //                if (majorsSoFar.contains(major)) {
                //                    errln("Non-contiguous majors: " + major + " <… " + lastMajor +
                // " < " + major);
                //                }
                majorsSoFar.add(major);
                lastMajor = major;
                lastMajorGroup.clear();
                lastMajorGroup.add(emoji); // add emoji with different cat
            } else {
                lastMajorGroup.add(emoji);
            }
            // never get minor1 < minor2 < minor1
            if (!minor.equals(lastMinor)) {
                if (DEBUG) System.out.println(lastMinor + "\t" + lastMinorGroup);
                if (minorsSoFar.contains(minor)) {
                    errln("Non-contiguous minors: " + minor + " <… " + lastMinor + " < " + minor);
                }
                minorsSoFar.add(minor);
                lastMinor = minor;
                lastMinorGroup.clear();
                lastMinorGroup.add(emoji); // add emoji with different cat
            } else {
                lastMinorGroup.add(emoji);
            }
            lastEmoji = emoji;
            lastEmojiOrdering = emojiOrdering;
        }
        if (DEBUG) System.out.println(lastMinor + "\t" + lastMinorGroup);
    }

    public void testSuperfluousAnnotationPaths() {
        if (CLDRPaths.ANNOTATIONS_DIRECTORY.contains("cldr-staging/production/")) {
            return; // don't bother checking production for this: root is empty
        }
        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        ImmutableSet<String> rootPaths =
                ImmutableSortedSet.copyOf(
                        factoryAnnotations.make("root", false).iterator("//ldml/annotations/"));

        CLDRFile englishAnnotations = factoryAnnotations.make("en", false);
        ImmutableSet<String> englishPaths =
                ImmutableSortedSet.copyOf(englishAnnotations.iterator("//ldml/annotations/"));

        Set<String> superfluous2 = setDifference(rootPaths, englishPaths);
        assertTrue("en contains root", superfluous2.isEmpty());
        if (!superfluous2.isEmpty()) {
            for (String path : superfluous2) {
                //              XPathParts parts = XPathParts.getFrozenInstance(path);
                //              String emoji = parts.getAttributeValue(-1, "cp");
                System.out.println("locale=en; action=add; path=" + path + "; value=XXX");
            }
        }

        Set<String> allSuperfluous = new TreeSet<>();
        for (String locale : factoryAnnotations.getAvailable()) {
            ImmutableSet<String> currentPaths =
                    ImmutableSortedSet.copyOf(
                            factoryAnnotations.make(locale, false).iterator("//ldml/annotations/"));
            Set<String> superfluous = setDifference(currentPaths, rootPaths);
            if (!assertTrue("root contains " + locale, superfluous.isEmpty())) {
                int debug = 0;
            }
            allSuperfluous.addAll(superfluous);
            for (String s : currentPaths) {
                if (s.contains("\uFE0F")) {
                    errln("Contains FE0F: " + s);
                    break;
                }
            }
        }
        // get items to fix
        if (!allSuperfluous.isEmpty()) {
            for (String path : allSuperfluous) {
                //                XPathParts parts = XPathParts.getFrozenInstance(path);
                //                String emoji = parts.getAttributeValue(-1, "cp");
                System.out.println("locale=/.*/; action=delete; path=" + path);
            }
        }
    }

    private Set<String> setDifference(ImmutableSet<String> a, ImmutableSet<String> b) {
        Set<String> superfluous = new LinkedHashSet<>(a);
        superfluous.removeAll(b);
        return superfluous;
    }

    private boolean checkAMinusBIsC(String title, Set<String> a, Set<String> b, Set<String> c) {
        Set<String> aMb = new TreeSet<>(a);
        aMb.removeAll(b);
        for (Iterator<String> it = aMb.iterator(); it.hasNext(); ) {
            String item = it.next();
            if (symbols.containsSome(item)) {
                it.remove();
            }
        }
        return assertEquals(title + " (" + aMb.size() + ")", c, aMb);
    }

    public void testListFormatter() {
        Object[][] tests = {
            {"en", ListTypeLength.NORMAL, "ABC", "A, B, and C"},
            {"en", ListTypeLength.AND_SHORT, "ABC", "A, B, & C"},
            {"en", ListTypeLength.AND_NARROW, "ABC", "A, B, C"},
            {"en", ListTypeLength.OR_WIDE, "ABC", "A, B, or C"}
        };
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        for (Object[] test : tests) {
            CLDRFile cldrFile = factory.make((String) (test[0]), true);
            ListTypeLength listTypeLength = (ListTypeLength) (test[1]);
            String expected = (String) test[3];
            XListFormatter xlistFormatter = new XListFormatter(cldrFile, listTypeLength);
            String source = (String) test[2];
            String actual = xlistFormatter.formatCodePoints(source);
            assertEquals(test[0] + ", " + listTypeLength + ", " + source, expected, actual);
        }
    }

    public void testCoverage() {
        UnicodeMap<Level> levels = new UnicodeMap<>();
        for (String minorCategory : Emoji.getMinorCategoriesWithExtras()) {
            for (String s : Emoji.getEmojiInMinorCategoriesWithExtras(minorCategory)) {
                if (s.contentEquals("‾")) {
                    int debug = 0;
                }
                CoverageLevel2 coverageLevel =
                        CoverageLevel2.getInstance(SupplementalDataInfo.getInstance(), "en");
                final String pathKeyword = "//ldml/annotations/annotation[@cp=\"" + s + "\"]";
                final String pathName = pathKeyword + "[@type=\"tts\"]";
                Level levelKeyword = coverageLevel.getLevel(pathKeyword);
                Level levelName = coverageLevel.getLevel(pathName);
                assertEquals(s, levelName, levelKeyword);
                levels.put(s, levelName);
            }
        }
        for (Level level : Level.values()) {
            UnicodeSet us = levels.getSet(level);
            getLogger().fine(level + "\t" + us.size());
            switch (level) {
                case MODERN:
                    assertNotEquals(level.toString(), 0, us.size());
                    break;
                default:
                    assertEquals(level.toString(), 0, us.size());
                    break;
            }
        }
    }

    static final UnicodeSet allRgiNoES = Emoji.getAllRgiNoES();
    static final UnicodeSet punctuation = new UnicodeSet("[:P:]").freeze();
    static final UnicodeSet mathSymbols = new UnicodeSet("[:Sm:]").freeze();
    static final UnicodeSet otherSymbols = new UnicodeSet("[^[:Sm:][:P:]]").freeze();

    public void testSymbols() {
        CLDRFile root = CLDRConfig.getInstance().getAnnotationsFactory().make("root", false);
        UnicodeMap<String> expectedMap =
                new UnicodeMap<String>()
                        .putAll(punctuation, "Punctuation")
                        .putAll(mathSymbols, "Math Symbols")
                        .putAll(otherSymbols, "Other Symbols")
                        .freeze();
        Set<String> nonEmojiPages = expectedMap.values();
        UnicodeMap<Pair<String, String>> failures = new UnicodeMap<>();
        PathHeader.Factory phf = PathHeader.getFactory();
        for (String path : root) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String cp = parts.getAttributeValue(-1, "cp");
            if (cp == null) {
                continue; // non-annotation line
            }
            PathHeader ph = phf.fromPath(path);
            PathHeader.SectionId sectionId = ph.getSectionId();
            assertEquals("Section for " + cp, PathHeader.SectionId.Characters, sectionId);
            PageId pageId = ph.getPageId();
            final String actual = pageId.toString();

            // collect all the failures rather than having a long list of errors

            if (allRgiNoES.contains(cp)) { // check emoji
                if (nonEmojiPages.contains(actual)) {
                    failures.put(cp, Pair.of("«Emoji-Page»", actual));
                } else if (actual.equals("Symbols2")) {
                    failures.put(cp, Pair.of("Emoji Symbols", actual));
                }
            } else {
                String expected = expectedMap.get(cp);
                if (!actual.equals(expected)) {
                    failures.put(cp, Pair.of(expected, actual));
                }
            }
        }
        if (!failures.isEmpty()) {
            for (Pair<String, String> value : ImmutableSortedSet.copyOf(failures.values())) {
                UnicodeSet uset = failures.getSet(value);
                errln(
                        "Mismatch in "
                                + uset.size()
                                + " cases: expected="
                                + value.getFirst()
                                + " actual="
                                + value.getSecond()
                                + "\n"
                                + uset.toPattern(false));
            }
        }
    }

    final UnicodeSet TEMPORARY_SKIP_COMPOUNDS = UnicodeSet.EMPTY;

    // For any new Unicode release with emoji, put any ones that need to be derived into the
    // following list (uncommented), replacing what was there (left just for comparison.
    // After the submission (there is a BRS item for this) modify the algorithm in Annotations to
    // generate the names for special compounds,
    // and set the above to UnicodeSet.EMPTY to test, and comment out the new UnicodeSet.
    //
    //     new UnicodeSet(
    // "[{👨🏻‍🐰‍👨🏼}{👨🏻‍🐰‍👨🏽}{👨🏻‍🐰‍👨🏾}{👨🏻‍🐰‍👨🏿}{👨🏻‍🫯‍👨🏼}{👨🏻‍🫯‍👨🏽}{👨🏻‍🫯‍👨🏾}{👨🏻‍🫯‍👨🏿}{👨🏼‍🐰‍👨🏻}{👨🏼‍🐰‍👨🏽}{👨🏼‍🐰‍👨🏾}{👨🏼‍🐰‍👨🏿}{👨🏼‍🫯‍👨🏻}{👨🏼‍🫯‍👨🏽}{👨🏼‍🫯‍👨🏾}{👨🏼‍🫯‍👨🏿}
    // {👨🏽‍🐰‍👨🏻}{👨🏽‍🐰‍👨🏼}{👨🏽‍🐰‍👨🏾}{👨🏽‍🐰‍👨🏿}{👨🏽‍🫯‍👨🏻}{👨🏽‍🫯‍👨🏼}{👨🏽‍🫯‍👨🏾}{👨🏽‍🫯‍👨🏿}{👨🏾‍🐰‍👨🏻}{👨🏾‍🐰‍👨🏼}{👨🏾‍🐰‍👨🏽}{👨🏾‍🐰‍👨🏿}{👨🏾‍🫯‍👨🏻}{👨🏾‍🫯‍👨🏼}{👨🏾‍🫯‍👨🏽}{👨🏾‍🫯‍👨🏿}{👨🏿‍🐰‍👨🏻}
    // {👨🏿‍🐰‍👨🏼}{👨🏿‍🐰‍👨🏽}{👨🏿‍🐰‍👨🏾}{👨🏿‍🫯‍👨🏻}{👨🏿‍🫯‍👨🏼}{👨🏿‍🫯‍👨🏽}{👨🏿‍🫯‍👨🏾}{👩🏻‍🐰‍👩🏼}{👩🏻‍🐰‍👩🏽}{👩🏻‍🐰‍👩🏾}{👩🏻‍🐰‍👩🏿}{👩🏻‍🫯‍👩🏼}{👩🏻‍🫯‍👩🏽}{👩🏻‍🫯‍👩🏾}{👩🏻‍🫯‍👩🏿}{👩🏼‍🐰‍👩🏻}
    // {👩🏼‍🐰‍👩🏽}{👩🏼‍🐰‍👩🏾}{👩🏼‍🐰‍👩🏿}{👩🏼‍🫯‍👩🏻}{👩🏼‍🫯‍👩🏽}{👩🏼‍🫯‍👩🏾}{👩🏼‍🫯‍👩🏿}{👩🏽‍🐰‍👩🏻}{👩🏽‍🐰‍👩🏼}{👩🏽‍🐰‍👩🏾}{👩🏽‍🐰‍👩🏿}{👩🏽‍🫯‍👩🏻}{👩🏽‍🫯‍👩🏼}{👩🏽‍🫯‍👩🏾}{👩🏽‍🫯‍👩🏿}{👩🏾‍🐰‍👩🏻}
    // {👩🏾‍🐰‍👩🏼}{👩🏾‍🐰‍👩🏽}{👩🏾‍🐰‍👩🏿}{👩🏾‍🫯‍👩🏻}{👩🏾‍🫯‍👩🏼}{👩🏾‍🫯‍👩🏽}{👩🏾‍🫯‍👩🏿}{👩🏿‍🐰‍👩🏻}{👩🏿‍🐰‍👩🏼}{👩🏿‍🐰‍👩🏽}{👩🏿‍🐰‍👩🏾}{👩🏿‍🫯‍👩🏻}{👩🏿‍🫯‍👩🏼}{👩🏿‍🫯‍👩🏽}{👩🏿‍🫯‍👩🏾}{👯🏻}{👯🏻‍♀}
    // {👯🏻‍♂}{👯🏼}{👯🏼‍♀}{👯🏼‍♂}{👯🏽}{👯🏽‍♀}{👯🏽‍♂}{👯🏾}{👯🏾‍♀}{👯🏾‍♂}{👯🏿}{👯🏿‍♀}{👯🏿‍♂}{🤼🏻}{🤼🏻‍♀}{🤼🏻‍♂}{🤼🏼}{🤼🏼‍♀}{🤼🏼‍♂}
    // {🤼🏽}{🤼🏽‍♀}{🤼🏽‍♂}{🤼🏾}{🤼🏾‍♀}{🤼🏾‍♂}{🤼🏿}{🤼🏿‍♀}{🤼🏿‍♂}{🧑🏻‍🐰‍🧑🏼}{🧑🏻‍🐰‍🧑🏽}{🧑🏻‍🐰‍🧑🏾}{🧑🏻‍🐰‍🧑🏿}{🧑🏻‍🩰}{🧑🏻‍🫯‍🧑🏼}{🧑🏻‍🫯‍🧑🏽}{🧑🏻‍🫯‍🧑🏾}{🧑🏻‍🫯‍🧑🏿}
    // {🧑🏼‍🐰‍🧑🏻}{🧑🏼‍🐰‍🧑🏽}{🧑🏼‍🐰‍🧑🏾}{🧑🏼‍🐰‍🧑🏿}{🧑🏼‍🩰}{🧑🏼‍🫯‍🧑🏻}{🧑🏼‍🫯‍🧑🏽}{🧑🏼‍🫯‍🧑🏾}{🧑🏼‍🫯‍🧑🏿}{🧑🏽‍🐰‍🧑🏻}{🧑🏽‍🐰‍🧑🏼}{🧑🏽‍🐰‍🧑🏾}{🧑🏽‍🐰‍🧑🏿}{🧑🏽‍🩰}{🧑🏽‍🫯‍🧑🏻}{🧑🏽‍🫯‍🧑🏼}{🧑🏽‍🫯‍🧑🏾}
    // {🧑🏽‍🫯‍🧑🏿}{🧑🏾‍🐰‍🧑🏻}{🧑🏾‍🐰‍🧑🏼}{🧑🏾‍🐰‍🧑🏽}{🧑🏾‍🐰‍🧑🏿}{🧑🏾‍🩰}{🧑🏾‍🫯‍🧑🏻}{🧑🏾‍🫯‍🧑🏼}{🧑🏾‍🫯‍🧑🏽}{🧑🏾‍🫯‍🧑🏿}{🧑🏿‍🐰‍🧑🏻}{🧑🏿‍🐰‍🧑🏼}{🧑🏿‍🐰‍🧑🏽}{🧑🏿‍🐰‍🧑🏾}{🧑🏿‍🩰}{🧑🏿‍🫯‍🧑🏻}{🧑🏿‍🫯‍🧑🏼}{🧑🏿‍🫯‍🧑🏽}{🧑🏿‍🫯‍🧑🏾}]")
    //                        .freeze();

    /**
     * We test that all emoji have English annotations. This may fail when the emoji are updated for
     * a new version of Unicode, if the algorithm for computing derived annotations needs updating.
     *
     * <pre>
     * <annotation cp="👋🏻">bye | cya | g2g | greetings | gtg | hand | hello</annotation>
     * <annotation cp="👋🏻" type="tts">waving hand: light skin tone</annotation>
     * </pre>
     */
    public void testCompleteness() {
        UnicodeSet allRgiNoEs = Emoji.getAllRgiNoES();

        // get both regular and derived emoji
        for (String file :
                List.of("en.xml")) { // for testing, can add others like "de.xml", "fr.xml"
            UnicodeSet namesFound = new UnicodeSet();
            UnicodeSet searchKeywordsFound = new UnicodeSet();
            fillNamesAndSearchKeywords(
                    file, namesFound, searchKeywordsFound); // freezes the results

            warnln(
                    Joiner.on('\t')
                            .join(
                                    "FYI, RGI:",
                                    allRgiNoEs.size(),
                                    "namesFound:",
                                    namesFound.size(),
                                    "searchKeywordsFound:",
                                    searchKeywordsFound.size()));

            UnicodeSet missingNames = new UnicodeSet(allRgiNoEs).removeAll(namesFound).freeze();

            UnicodeSet missingKeywords =
                    new UnicodeSet(allRgiNoEs).removeAll(searchKeywordsFound).freeze();

            // If one of the following fails, it is likely due to code needed in DerivedAnnotations
            // to handle special sequences.
            // See instructions below.

            if (!assertEquals(
                    file + " RGI name annotations",
                    "[]",
                    new UnicodeSet(missingNames)
                            .removeAll(TEMPORARY_SKIP_COMPOUNDS)
                            .toPattern(false))) {
                break;
            }
            if (!assertEquals(
                    file + " RGI search key annotations",
                    "[]",
                    new UnicodeSet(missingKeywords)
                            .removeAll(TEMPORARY_SKIP_COMPOUNDS)
                            .toPattern(false))) {
                break;
            }
            UnicodeSet onlyAllowedBecauseOfTEMPORARY_SKIP_COMPOUNDS =
                    new UnicodeSet(missingNames)
                            .addAll(missingKeywords)
                            .retainAll(TEMPORARY_SKIP_COMPOUNDS)
                            .freeze();
            if (!onlyAllowedBecauseOfTEMPORARY_SKIP_COMPOUNDS.isEmpty()) {
                // Normally the following exception is used.
                throw new IllegalArgumentException(
                        "This is probably due to new emoji being added. See instructions for fixing.");
                // When there are new emoji that cause a failure in the derived annotations, do the
                // following:
                //
                // file a ticket,
                // comment out the above exception
                // uncomment the logKnownIssue below
                // replace the ticket number by the new ticket number
                // and populate the TEMPORARY_SKIP_COMPOUNDS.

                // The extra code to handled the special derived forms must be added well before
                // Alpha (ideally before submission).
                // that code will go into

                //      public Annotations synthesize(String code, Transform<String, String>
                // otherSource) {

                //  logKnownIssue("CLDR-18462", file
                //   + " Update Annotations.java for new compounds: "
                //   +  onlyAllowedBecauseOfTEMPORARY_SKIP_COMPOUNDS.toPattern(false));
            }
        }
    }

    private void fillNamesAndSearchKeywords(
            String file, UnicodeSet namesFound, UnicodeSet searchKeywordsFound) {
        List<Pair<String, String>> listXmlEmoji = new ArrayList<>();
        XMLFileReader.loadPathValues(
                CLDRPaths.ANNOTATIONS_DERIVED_DIRECTORY + file, listXmlEmoji, false);
        XMLFileReader.loadPathValues(CLDRPaths.ANNOTATIONS_DIRECTORY + file, listXmlEmoji, false);

        // pull out the ones that are handled by English

        for (Pair<String, String> pathAndValue : listXmlEmoji) {
            XPathParts parts = XPathParts.getFrozenInstance(pathAndValue.getFirst());
            if (!"annotation".equals(parts.getElement(-1))) {
                continue;
            }
            String cp = parts.getAttributeValue(-1, "cp");
            boolean isTts = parts.getAttributeValue(-1, "type") != null;
            UnicodeSet set = isTts ? namesFound : searchKeywordsFound;
            set.add(cp);
        }
        namesFound.freeze();
        searchKeywordsFound.freeze();
    }

    public void testRightFacing() {
        AnnotationSet aset = Annotations.getDataSet("en");

        for (String item : new UnicodeSet("[{🚶🏻‍♂️‍➡️}{🚶🏼‍♂️‍➡️}{🚶🏽‍♂️‍➡️}{🚶🏾‍♂️‍➡️}]")) {
            Annotations annotations = aset.synthesize(item, null);
            Set<String> keywords = annotations.getKeywords();
            String shortName = annotations.getShortName();

            assertTrue(
                    item + ", " + keywords,
                    contains(keywords, "skin") && contains(keywords, "facing"));
            assertTrue(
                    item + ", " + shortName,
                    shortName.contains("skin") && shortName.contains("facing"));
        }
    }

    private boolean contains(Set<String> keywords, String string) {
        return keywords.stream().anyMatch(x -> x.contains(string));
    }
}
