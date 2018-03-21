package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.Keyboard;
import org.unicode.cldr.draft.Keyboard.Gesture;
import org.unicode.cldr.draft.Keyboard.Iso;
import org.unicode.cldr.draft.Keyboard.KeyMap;
import org.unicode.cldr.draft.Keyboard.Output;
import org.unicode.cldr.draft.Keyboard.TransformStatus;
import org.unicode.cldr.draft.Keyboard.TransformType;
import org.unicode.cldr.draft.Keyboard.Transforms;
import org.unicode.cldr.draft.KeyboardModifierSet;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

@CLDRTool(alias = "showkeyboards", description = "Generate keyboard charts")
public class ShowKeyboards {
    // TODO - fix ' > xxx
    // TODO - check for bad locale ids

    private static final String ABOUT_KEYBOARD_CHARTS = "<p>For more information, see <a target='ABOUT_KB' href='http://cldr.unicode.org/index/charts/keyboards'>About Keyboard Charts</a>.</p>";
    private static String keyboardChartDir;
    private static String keyboardChartLayoutsDir;
    static final CLDRConfig testInfo = ToolConfig.getToolInstance();
    static final Factory factory = testInfo.getCldrFactory();

    static final boolean SHOW_BACKGROUND = false;

    final static Options myOptions = new Options();

    enum MyOptions {
        idFilter(".+", ".*", "Filter the information based on id, using a regex argument."), sourceDirectory(".+", CLDRPaths.BASE_DIRECTORY + "keyboards/",
            "The source directory. CURRENTLY CAN’T BE CHANGED!!"), targetDirectory(".+", CLDRPaths.CHART_DIRECTORY + "keyboards/",
                "The target directory."), layouts(null, null,
                    "Only create html files for keyboard layouts"), repertoire(null, null, "Only create html files for repertoire"),;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();

    // *********************************************
    // Temporary, for some simple testing
    // *********************************************
    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.idFilter, args, true);
        String idPattern = MyOptions.idFilter.option.getValue();
        keyboardChartDir = MyOptions.targetDirectory.option.getValue();
        keyboardChartLayoutsDir = keyboardChartDir + "/layouts/";

        FileCopier.ensureDirectoryExists(keyboardChartDir);
        FileCopier.copy(ShowKeyboards.class, "keyboards-index.html", keyboardChartDir, "index.html");

        Matcher idMatcher = PatternCache.get(idPattern).matcher("");
        try {
            Log.setLog(CLDRPaths.LOG_DIRECTORY + "keyboard-log.txt");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        boolean layoutsOnly = MyOptions.layouts.option.doesOccur();
        boolean repertoireOnly = MyOptions.repertoire.option.doesOccur();

        if (!repertoireOnly) {
            showHtml(idMatcher);
        }
        if (!layoutsOnly) {
            showRepertoire(idMatcher);
        }
    }

    public static void showRepertoire(Matcher idMatcher) {
        Set<Exception> totalErrors = new LinkedHashSet<Exception>();
        Set<Exception> errors = new LinkedHashSet<Exception>();
        UnicodeSet controls = new UnicodeSet("[:Cc:]").freeze();
        // check what the characters are, excluding controls.
        Map<Id, UnicodeSet> id2unicodeset = new TreeMap<Id, UnicodeSet>();
        Set<String> totalModifiers = new LinkedHashSet<String>();
        Relation<String, Id> locale2ids = Relation.of(new TreeMap<String, Set<Id>>(), TreeSet.class);
        LanguageTagCanonicalizer canonicalizer = new LanguageTagCanonicalizer();
        IdInfo idInfo = new IdInfo();
        for (String platformId : Keyboard.getPlatformIDs()) {
            //Platform p = Keyboard.getPlatform(platformId);
            // System.out.println(platformId + "\t" + p.getHardwareMap());
            for (String keyboardId : Keyboard.getKeyboardIDs(platformId)) {
                if (!idMatcher.reset(keyboardId).matches()) {
                    continue;
                }
                Keyboard keyboard = Keyboard.getKeyboard(platformId, keyboardId, errors);
                for (Exception error : errors) {
                    totalErrors.add(new IllegalArgumentException(keyboardId, error));
                }
                UnicodeSet unicodeSet = keyboard.getPossibleResults().removeAll(controls);
                final Id id = new Id(keyboardId, keyboard.getPlatformVersion());
                idInfo.add(id, unicodeSet);
                String canonicalLocale = canonicalizer.transform(id.locale).replace('_', '-');
                if (!id.locale.equals(canonicalLocale)) {
                    totalErrors.add(new IllegalArgumentException("Non-canonical id: " + id.locale + "\t=>\t" + canonicalLocale));
                }
                id2unicodeset.put(id, unicodeSet.freeze());
                locale2ids.put(id.locale, id);
                System.out.println(id.toString().replace('/', '\t') + "\t" + keyboard.getNames());
                for (KeyMap keymap : keyboard.getKeyMaps()) {
                    totalModifiers.add(keymap.getModifiers().toString());
                }
            }
        }
        if (totalErrors.size() != 0) {
            System.out.println("Errors\t" + CollectionUtilities.join(totalErrors, System.lineSeparator() + "\t"));
        }
        for (String item : totalModifiers) {
            System.out.println(item);
        }
        // logInfo.put(Row.of("k-cldr",common), keyboardId);
        try {
            FileCopier.copy(ShowKeyboards.class, "keyboards.css", keyboardChartDir, "index.css");
            PrintWriter out = FileUtilities.openUTF8Writer(keyboardChartDir, "chars2keyboards.html");
            String[] headerAndFooter = new String[2];

            ShowData.getChartTemplate(
                "Characters → Keyboards",
                ToolConstants.CHART_DISPLAY_VERSION,
                "",
                headerAndFooter, null, false);
            out.println(headerAndFooter[0] + ABOUT_KEYBOARD_CHARTS);

            // printTop("Characters → Keyboards", out);
            idInfo.print(out);
            // printBottom(out);
            out.println(headerAndFooter[1]);
            out.close();

            out = FileUtilities.openUTF8Writer(keyboardChartDir, "keyboards2chars.html");
            ShowData.getChartTemplate(
                "Keyboards → Characters",
                ToolConstants.CHART_DISPLAY_VERSION,
                "",
                headerAndFooter, null, false);
            out.println(headerAndFooter[0]
                + ABOUT_KEYBOARD_CHARTS);
            // printTop("Keyboards → Characters", out);
            showLocaleToCharacters(out, id2unicodeset, locale2ids);
            // printBottom(out);
            out.println(headerAndFooter[1]);
            out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        for (Entry<R2<String, UnicodeSet>, Set<Id>> entry : logInfo.keyValuesSet()) {
            IdSet idSet = new IdSet();
            idSet.addAll(entry.getValue());
            Log.logln(entry.getKey().get0() + "\t" + entry.getKey().get1().toPattern(false) + "\t"
                + idSet.toString(idInfo.allIds));
        }
        Log.close();
    }

    private static void showHtml(Matcher idMatcher) throws IOException {
        Set<Exception> errors = new LinkedHashSet<Exception>();
        Relation<String, Row.R3<String, String, String>> locale2keyboards = Relation.of(
            new TreeMap<String, Set<Row.R3<String, String, String>>>(), TreeSet.class);
        Map<String, String> localeIndex = new TreeMap<String, String>();

        for (String platformId : Keyboard.getPlatformIDs()) {
            //Platform p = Keyboard.getPlatform(platformId);
            // System.out.println(platformId + "\t" + p.getHardwareMap());
            for (String keyboardId : Keyboard.getKeyboardIDs(platformId)) {
                if (!idMatcher.reset(keyboardId).matches()) {
                    continue;
                }
                String baseLocale = keyboardId.substring(0, keyboardId.indexOf('-'));
                String locale = keyboardId.substring(0, keyboardId.indexOf("-t-"));
                locale2keyboards.put(baseLocale, Row.of(platformId, locale, keyboardId));

                final String localeName = testInfo.getEnglish().getName(baseLocale, true);
                localeIndex.put(localeName, baseLocale);
            }
        }

        FileCopier.ensureDirectoryExists(keyboardChartLayoutsDir);
        FileCopier.copy(ShowKeyboards.class, "keyboards.css", keyboardChartLayoutsDir, "index.css");
        PrintWriter index = FileUtilities.openUTF8Writer(keyboardChartLayoutsDir, "index.html");
        String[] headerAndFooter = new String[2];
        ShowData.getChartTemplate(
            "Keyboard Layout Index",
            ToolConstants.CHART_DISPLAY_VERSION,
            "",
            headerAndFooter, "Keyboard Index", false);
        index
            .println(headerAndFooter[0] + ABOUT_KEYBOARD_CHARTS);
        // printTop("Keyboard Layout Index", index);
        index.println("<ol>");
        for (Entry<String, String> entry : localeIndex.entrySet()) {
            index.println("<li><a href='" + entry.getValue() + ".html'>"
                + entry.getKey() + "</a>"
                + " [" + entry.getValue() + "]" +
                "</li>");
        }
        index.println("</ol>");
        index.println(headerAndFooter[1]);
        // printBottom(index);
        index.close();
        // FileUtilities.copyFile(ShowKeyboards.class, "keyboards.css", keyboardChartLayoutsDir);

        for (Entry<String, Set<R3<String, String, String>>> localeKeyboards : locale2keyboards.keyValuesSet()) {
            String locale = localeKeyboards.getKey();
            final String localeName = testInfo.getEnglish().getName(locale);

            // String localeNameString = localeName.replace(' ', '_').toLowerCase(Locale.ENGLISH);
            PrintWriter out = FileUtilities.openUTF8Writer(keyboardChartLayoutsDir, locale + ".html");
            ShowData.getChartTemplate(
                "Layouts: " + localeName + " (" + locale + ")",
                ToolConstants.CHART_DISPLAY_VERSION,
                "",
                headerAndFooter, null, false);
            out.println(headerAndFooter[0] + ABOUT_KEYBOARD_CHARTS);
            // printTop("Layouts: " + localeName + " (" + locale + ")", out);
            Set<R3<String, String, String>> keyboards = localeKeyboards.getValue();
            for (R3<String, String, String> platformKeyboard : keyboards) {
                String platformId = platformKeyboard.get0();
                String keyboardId = platformKeyboard.get2();
                // System.out.println(platformId + "\t" + p.getHardwareMap());
                Keyboard keyboard = Keyboard.getKeyboard(platformId, keyboardId, errors);
                showErrors(errors);
                Set<String> names = keyboard.getNames();
                String platformFromKeyboardId = Keyboard.getPlatformId(keyboardId);
                String printId = platformId.equals(platformFromKeyboardId) ? keyboardId : keyboardId + "/und";
                out.println("<h2>" + CldrUtility.getDoubleLinkedText(printId, printId)
                    + (names.size() == 0 ? "" : " " + names)
                    + "</h2>");

                Transforms transforms = keyboard.getTransforms().get(TransformType.SIMPLE);

                out.println("<table class='keyboards'><tr>");
                for (KeyMap map : keyboard.getKeyMaps()) {
                    KeyboardModifierSet mods = map.getModifiers();
                    out.println("<td class='keyboardTD'><table class='keyboard'>");
                    // KeyboardModifierSet modifiers = map.getModifiers();
                    Map<Iso, Output> isoMap = map.getIso2Output();
                    for (Keyboard.IsoRow row : Keyboard.IsoRow.values()) {
                        out.println("<tr>");
                        for (Iso isoValue : Iso.values()) {
                            if (isoValue.isoRow != row) {
                                continue;
                            }
                            Output output = isoMap.get(isoValue);
                            if (output == null) {
                                out.println("<td class='x'>&nbsp;</td>");
                                continue;
                            }
                            String chars = output.getOutput();
                            TransformStatus transformStatus = output.getTransformStatus();
                            StringBuilder hover = new StringBuilder();
                            if (transformStatus == TransformStatus.DEFAULT && transforms != null) {
                                Map<String, String> map2 = transforms.getMatch(chars);
                                add(map2, hover);
                            }
                            Map<Gesture, List<String>> gestures = output.getGestures();
                            if (!gestures.isEmpty()) {
                                add(gestures, hover);
                            }
                            final String longPress = hover.length() == 0 ? ""
                                : " title='" + hover + "'";
                            out.println("<td class='" + (hover.length() == 0 ? 'm' : 'h') +
                                "'" + longPress + ">"
                                + toSafeHtml(chars) + "</td>");
                        }
                        out.println("</tr>");
                    }
                    String modsString = mods.getShortInput();
                    if (modsString.isEmpty()) {
                        modsString = "\u00A0";
                    } else if (modsString.length() > 20) {
                        modsString = modsString.substring(0, 20) + "…";
                    }
                    out.println("</table><span class='modifiers'>"
                        + TransliteratorUtilities.toHTML.transform(modsString) +
                        "</span></td>");
                }
                out.println("</tr></table>");
            }
            index.println(headerAndFooter[1]);
            // printBottom(out);
            out.close();
        }
        System.out.println("Failing Invisibles: " + FAILING_INVISIBLE.retainAll(INVISIBLE));
    }

    private static void showErrors(Set<Exception> errors) {
        for (Exception error : errors) {
            String title = error.getMessage().contains("No minimal data for") ? "Warning" : "Error";
            System.out.println("\t*" + title + ":\t" + error);
        }
    }

    static Transliterator TO_SAFE_HTML;
    static {
        StringBuilder rules = new StringBuilder(TransliteratorUtilities.toHTML.toRules(false));
        for (char i = 0; i < 0x20; ++i) {
            addRule(String.valueOf(i), "^" + String.valueOf((char) (i + 0x40)), rules);
        }
        String[][] map = {
            // {"\u0020","sp"},
            { "\u007F", "del" },
            { "\u00A0", "nbsp" },
            { "\u00AD", "shy" },
            { "\u200B", "zwsp" },
            { "\u200C", "zwnj" },
            { "\u200D", "zwj" },
            { "\u200E", "lrm" },
            { "\u200F", "rlm" },
            { "\u202F", "nnbs" },
            { "\uFEFF", "bom" },
            { "\u180B", "mvs1" },
            { "\u180C", "mvs2" },
            { "\u180D", "mvs3" },
            { "\u180E", "mvs" },
            // {"\uF8FF","appl"},
        };
        for (String[] items : map) {
            final String fromItem = items[0];
            final String toItem = items[1];
            addRule(fromItem, toItem, rules);
        }
        TO_SAFE_HTML = Transliterator.createFromRules("none", rules.toString(), Transliterator.FORWARD);
    }

    public static void addRule(final String fromItem, final String toItem, StringBuilder rules) {
        rules.append("'"
            + fromItem
            + "'>"
            + "'<span class=\"cc\">"
            + toItem
            + "</span>'"
            + ";"
            + System.lineSeparator());
    }

    static UnicodeSet INVISIBLE = new UnicodeSet("[[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]").freeze();
    static UnicodeSet FAILING_INVISIBLE = new UnicodeSet();

    public static String toSafeHtml(Object hover) {
        String result = TO_SAFE_HTML.transform(hover.toString());
        if (INVISIBLE.containsSome(result)) {
            FAILING_INVISIBLE.addAll(result);
        }
        return result;
    }

    private static <K, V> void add(Map<K, V> map2, StringBuilder hover) {
        if (!map2.isEmpty()) {
            for (Entry<K, V> entry : map2.entrySet()) {
                if (hover.length() != 0) {
                    hover.append("; ");
                }
                final K key = entry.getKey();
                String keyString = key == Gesture.LONGPRESS ? "LP" : key.toString();
                final V value = entry.getValue();
                String valueString = value instanceof Collection
                    ? CollectionUtilities.join((Collection) value, " ")
                    : value.toString();
                hover.append(TransliteratorUtilities.toHTML.transform(keyString)).append("→")
                    .append(TransliteratorUtilities.toHTML.transform(valueString));
            }
        }
    }

    // public static void printTop(String title, PrintWriter out) {
    // out.println(
    // "<html>\n" +
    // "<head>\n" +
    // "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n" +
    // "<link rel='stylesheet' type='text/css' href='keyboards.css'>" +
    // "<title>" + title + "</title>\n" +
    // "</head>\n" +
    // "<body>\n" +
    // "<h1>DRAFT " +
    // title +
    // "</h1>\n" +
    // "<p>For more information, see <a href='http://cldr.unicode.org/index/charts/keyboards'>Keyboard Charts</a>.</p>"
    // );
    // }
    //
    // public static void printBottom(PrintWriter pw) {
    // pw.println(
    // "</body>\n" +
    // "</html>"
    // );
    // }

    public static void showLocaleToCharacters(PrintWriter out, Map<Id, UnicodeSet> id2unicodeset,
        Relation<String, Id> locale2ids) {

        TablePrinter t = new TablePrinter()
            .addColumn("Name").setSpanRows(true).setBreakSpans(true).setSortPriority(0)
            .setCellAttributes("class='cell'")
            .addColumn("Locale").setSpanRows(true).setBreakSpans(true).setCellAttributes("class='cell'")
            .addColumn("Platform").setSpanRows(true).setCellAttributes("class='cell'")
            .addColumn("Variant").setCellAttributes("class='cell'")
            .addColumn("Script").setCellAttributes("class='cell'")
            .addColumn("Statistics").setCellAttributes("class='cell'")
            .addColumn("Characters").setSpanRows(true).setCellAttributes("class='cell'");

        Map<String, UnicodeSet> commonSets = new HashMap<String, UnicodeSet>();
        Counter<String> commonCount = new Counter<String>();
        Set<String> commonDone = new HashSet<String>();

        for (Entry<String, Set<Id>> localeAndIds : locale2ids.keyValuesSet()) {
            final String key = localeAndIds.getKey();
            final Set<Id> keyboardIds = localeAndIds.getValue();

            // System.out.println();
            final String localeName = testInfo.getEnglish().getName(key, true);
            final String linkedLocaleName = CldrUtility.getDoubleLinkedText(key, localeName);
            final ULocale uLocale = ULocale.forLanguageTag(key);
            String script = uLocale.getScript();
            String writtenLanguage = uLocale.getLanguage() + (script.isEmpty() ? "" : "_" + script);
            CLDRFile cldrFile = null;
            try {
                cldrFile = factory.make(writtenLanguage, true);
            } catch (Exception e) {
            }

            // final String heading = uLocale.getDisplayName(ULocale.ENGLISH)
            // + "\t" + ULocale.addLikelySubtags(uLocale).getScript()
            // + "\t";
            UnicodeSet common = UnicodeSet.EMPTY;
            final String likelyScript = ULocale.addLikelySubtags(uLocale).getScript();
            commonCount.clear();
            for (String platform : Keyboard.getPlatformIDs()) {
                commonSets.put(platform, UnicodeSet.EMPTY);
            }
            if (keyboardIds.size() > 1) {
                common = UnicodeSet.EMPTY;
                for (Id keyboardId : keyboardIds) {
                    final UnicodeSet keyboardSet = id2unicodeset.get(keyboardId);
                    if (common == UnicodeSet.EMPTY) {
                        common = new UnicodeSet(keyboardSet);
                    } else {
                        common.retainAll(keyboardSet);
                    }
                    UnicodeSet platformCommon = commonSets.get(keyboardId.platform);
                    commonCount.add(keyboardId.platform, 1);
                    if (platformCommon == UnicodeSet.EMPTY) {
                        commonSets.put(keyboardId.platform, new UnicodeSet(keyboardSet));
                    } else {
                        platformCommon.retainAll(keyboardSet);
                    }
                }
                common.freeze();
                t.addRow()
                    .addCell(linkedLocaleName) // name
                    .addCell(key) // locale
                    .addCell("ALL") // platform
                    .addCell("COMMON") // variant
                    .addCell(likelyScript) // script
                    .addCell(getInfo(null, common, cldrFile)) // stats
                    .addCell(safeUnicodeSet(common)) // characters
                    .finishRow();

                // System.out.println(
                // locale + "\tCOMMON\t\t-"
                // + "\t" + heading + getInfo(common, cldrFile)
                // + "\t" + common.toPattern(false));
            }
            commonDone.clear();
            for (Id keyboardId : keyboardIds) {
                UnicodeSet platformCommon = commonSets.get(keyboardId.platform);
                if (!commonDone.contains(keyboardId.platform)) {
                    commonDone.add(keyboardId.platform);
                    if (commonCount.get(keyboardId.platform) <= 1) {
                        platformCommon = UnicodeSet.EMPTY;
                        commonSets.put(keyboardId.platform, platformCommon);
                    } else if (platformCommon.size() > 0) {
                        // get stats for all, but otherwise remove common.
                        final String stats = getInfo(null, platformCommon, cldrFile);
                        platformCommon.removeAll(common).freeze();
                        commonSets.put(keyboardId.platform, platformCommon);
                        t.addRow()
                            .addCell(linkedLocaleName) // name
                            .addCell(key) // locale
                            .addCell(keyboardId.platform) // platform
                            .addCell("COMMON") // variant
                            .addCell(likelyScript) // script
                            .addCell(stats) // stats
                            .addCell(safeUnicodeSet(platformCommon)) // characters
                            .finishRow();
                    }
                }
                final UnicodeSet current2 = id2unicodeset.get(keyboardId);
                final UnicodeSet remainder = new UnicodeSet(current2)
                    .removeAll(common)
                    .removeAll(platformCommon);

                t.addRow()
                    .addCell(linkedLocaleName) // name
                    .addCell(key) // locale
                    .addCell(keyboardId.platform) // platform
                    .addCell(keyboardId.variant) // variant
                    .addCell(likelyScript) // script
                    .addCell(getInfo(keyboardId, current2, cldrFile)) // stats
                    .addCell(safeUnicodeSet(remainder)) // characters
                    .finishRow();
                // System.out.println(
                // keyboardId.toString().replace('/','\t')
                // + "\t" + keyboardId.platformVersion
                // + "\t" + heading + getInfo(current2, cldrFile)
                // + "\t" + remainder.toPattern(false));
            }
        }
        out.println(t.toTable());
    }

    static UnicodeSetPrettyPrinter prettyPrinter = new UnicodeSetPrettyPrinter()
        .setOrdering(Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY));

    public static String safeUnicodeSet(UnicodeSet unicodeSet) {
        return TransliteratorUtilities.toHTML.transform(prettyPrinter.format(unicodeSet));
    }

    static class IdInfo {
        final Collator collator = Collator.getInstance(ULocale.ENGLISH);
        BitSet bitset = new BitSet();
        BitSet bitset2 = new BitSet();
        @SuppressWarnings("unchecked")
        TreeMap<String, IdSet>[] charToKeyboards = new TreeMap[UScript.CODE_LIMIT];
        {
            collator.setStrength(Collator.IDENTICAL);
            for (int i = 0; i < charToKeyboards.length; ++i) {
                charToKeyboards[i] = new TreeMap<String, IdSet>(collator);
            }
        }
        IdSet allIds = new IdSet();

        public void add(Id id, UnicodeSet unicodeSet) {
            allIds.add(id);
            for (String s : unicodeSet) {
                int script = getScriptExtensions(s, bitset);
                if (script >= 0) {
                    addToScript(script, id, s);
                } else {
                    for (int script2 = bitset.nextSetBit(0); script2 >= 0; script2 = bitset.nextSetBit(script2 + 1)) {
                        addToScript(script2, id, s);
                    }
                }
            }
        }

        public int getScriptExtensions(String s, BitSet outputBitset) {
            final int firstCodePoint = s.codePointAt(0);
            int result = UScript.getScriptExtensions(firstCodePoint, outputBitset);
            final int firstCodePointCount = Character.charCount(firstCodePoint);
            if (s.length() == firstCodePointCount) {
                return result;
            }
            for (int i = firstCodePointCount; i < s.length();) {
                int ch = s.codePointAt(i);
                UScript.getScriptExtensions(ch, bitset2);
                outputBitset.or(bitset2);
                i += Character.charCount(ch);
            }
            // remove inherited, if there is anything else; then remove common if there is anything else
            int cardinality = outputBitset.cardinality();
            if (cardinality > 1) {
                if (outputBitset.get(UScript.INHERITED)) {
                    outputBitset.clear(UScript.INHERITED);
                    --cardinality;
                }
                if (cardinality > 1) {
                    if (outputBitset.get(UScript.COMMON)) {
                        outputBitset.clear(UScript.COMMON);
                        --cardinality;
                    }
                }
            }
            if (cardinality == 1) {
                return outputBitset.nextSetBit(0);
            } else {
                return -cardinality;
            }
        }

        public void addToScript(int script, Id id, String s) {
            TreeMap<String, IdSet> charToKeyboard = charToKeyboards[script];
            IdSet idSet = charToKeyboard.get(s);
            if (idSet == null) {
                charToKeyboard.put(s, idSet = new IdSet());
            }
            idSet.add(id);
        }

        public void print(PrintWriter pw) {

            TablePrinter t = new TablePrinter()
                .addColumn("Script").setSpanRows(true).setCellAttributes("class='s'")
                .addColumn("Char").setCellAttributes("class='ch'")
                .addColumn("Code").setCellAttributes("class='c'")
                .addColumn("Name").setCellAttributes("class='n'")
                .addColumn("Keyboards").setSpanRows(true).setCellAttributes("class='k'");
            Set<String> missingScripts = new TreeSet<String>();
            UnicodeSet notNFKC = new UnicodeSet("[:nfkcqc=n:]");
            UnicodeSet COMMONINHERITED = new UnicodeSet("[[:sc=common:][:sc=inherited:]]");

            for (int script = 0; script < charToKeyboards.length; ++script) {
                UnicodeSet inScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, script).removeAll(
                    notNFKC);

                // UnicodeSet fullScript = new UnicodeSet(inScript);
                // int fullScriptSize = inScript.size();
                if (inScript.size() == 0) {
                    continue;
                }
                final TreeMap<String, IdSet> charToKeyboard = charToKeyboards[script];
                final String scriptName = UScript.getName(script);
                final String linkedScriptName = CldrUtility.getDoubleLinkedText(UScript.getShortName(script),
                    scriptName);
                if (charToKeyboard.size() == 0) {
                    missingScripts.add(scriptName);
                    continue;
                }

                // also check to see that at least one item is not all common
                check: if (script != UScript.COMMON && script != UScript.INHERITED) {
                    for (String s : charToKeyboard.keySet()) {
                        if (!COMMONINHERITED.containsAll(s)) {
                            break check;
                        }
                    }
                    missingScripts.add(scriptName);
                    continue;
                }

                String last = "";
                for (Entry<String, IdSet> entry : charToKeyboard.entrySet()) {
                    String s = entry.getKey();
                    IdSet value = entry.getValue();
                    final String keyboardsString = value.toString(allIds);
                    if (!s.equalsIgnoreCase(last)) {
                        if (s.equals("\u094D\u200C")) { // Hack, because the browsers width is way off
                            s = "\u094D";
                        }
                        String name = UCharacter.getName(s, " + ");
                        if (name == null) {
                            name = "[no name]";
                        }
                        String ch = s.equals("\u0F39") ? "\uFFFD" : s;
                        t.addRow()
                            .addCell(linkedScriptName)
                            .addCell((SHOW_BACKGROUND ? "<span class='ybg'>" : "") +
                                TransliteratorUtilities.toHTML.transform(ch)
                                + (SHOW_BACKGROUND ? "</span>" : ""))
                            .addCell(Utility.hex(s, 4, " + "))
                            .addCell(name)
                            .addCell(keyboardsString)
                            .finishRow();
                    }
                    inScript.remove(s);
                    last = s;
                }
                if (inScript.size() != 0 && script != UScript.UNKNOWN) {
                    // String pattern;
                    // if (inScript.size() < 255 || inScript.size()*4 < fullScriptSize) {
                    // } else {
                    // fullScript.removeAll(inScript);
                    // inScript = new UnicodeSet("[[:sc=" + UScript.getShortName(script) + ":]-" +
                    // fullScript.toPattern(false) + "]");
                    // }
                    t.addRow()
                        .addCell(linkedScriptName)
                        .addCell("")
                        .addCell(String.valueOf(inScript.size()))
                        .addCell("missing (NFKC)!")
                        .addCell(safeUnicodeSet(inScript))
                        .finishRow();
                }
            }
            t.addRow()
                .addCell("")
                .addCell("")
                .addCell(String.valueOf(missingScripts.size()))
                .addCell("missing scripts!")
                .addCell(missingScripts.toString())
                .finishRow();
            pw.println(t.toTable());
        }
    }

    private static String getInfo(Id keyboardId, UnicodeSet common, CLDRFile cldrFile) {
        Counter<String> results = new Counter<String>();
        for (String s : common) {
            int first = s.codePointAt(0); // first char is good enough
            results.add(UScript.getShortName(UScript.getScript(first)), 1);
        }
        results.remove("Zyyy");
        results.remove("Zinh");
        results.remove("Zzzz");

        if (cldrFile != null) {
            UnicodeSet exemplars = new UnicodeSet(cldrFile.getExemplarSet("", WinningChoice.WINNING));
            UnicodeSet auxExemplars = cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING);
            if (auxExemplars != null) {
                exemplars.addAll(auxExemplars);
            }
            UnicodeSet punctuationExemplars = cldrFile.getExemplarSet("punctuation", WinningChoice.WINNING);
            if (punctuationExemplars != null) {
                exemplars.addAll(punctuationExemplars);
            }
            exemplars.addAll(getNumericExemplars(cldrFile));
            exemplars.addAll(getQuotationMarks(cldrFile));
            exemplars.add(" ");
            addComparison(keyboardId, common, exemplars, results);
        }
        StringBuilder b = new StringBuilder();
        for (String entry : results.keySet()) {
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append(entry).append(":").append(results.get(entry));
        }
        return b.toString();
    }

    private static void addComparison(Id keyboardId, UnicodeSet keyboard, UnicodeSet exemplars,
        Counter<String> results) {
        UnicodeSet common = new UnicodeSet(keyboard).retainAll(exemplars);
        if (common.size() != 0) {
            results.add("k∩cldr", common.size());
        }
        common = new UnicodeSet(keyboard).removeAll(exemplars);
        if (common.size() != 0) {
            results.add("k‑cldr", common.size());
            if (keyboardId != null) {
                common.remove(0, 0x7F); // don't care much about ASCII.
                logInfo.put(Row.of("k-cldr\t" + keyboardId.getBaseLanguage(), common), keyboardId);
                // Log.logln(keyboardId + "\tk-cldr\t" + common.toPattern(false));
            }
        }
        common = new UnicodeSet(exemplars).removeAll(keyboard).remove("ss");
        if (common.size() != 0) {
            results.add("cldr‑k", common.size());
            if (keyboardId != null && SKIP_LOG.containsNone(common)) {
                logInfo.put(Row.of("cldr‑k\t" + keyboardId.getBaseLanguage(), common), keyboardId);
                // Log.logln(keyboardId + "\tcldr‑k\t" + common.toPattern(false));
            }
        }
    }

    static final UnicodeSet SKIP_LOG = new UnicodeSet("[가一]").freeze();
    static Relation<Row.R2<String, UnicodeSet>, Id> logInfo = Relation.of(new TreeMap<Row.R2<String, UnicodeSet>, Set<Id>>(), TreeSet.class);

    static class Id implements Comparable<Id> {
        final String locale;
        final String platform;
        final String variant;
        final String platformVersion;

        Id(String input, String platformVersion) {
            int pos = input.indexOf("-t-k0-");
            String localeTemp = input.substring(0, pos);
            locale = ULocale.minimizeSubtags(ULocale.forLanguageTag(localeTemp)).toLanguageTag();
            pos += 6;
            int pos2 = input.indexOf('-', pos);
            if (pos2 > 0) {
                platform = input.substring(pos, pos2);
                variant = input.substring(pos2 + 1);
            } else {
                platform = input.substring(pos);
                variant = "";
            }
            this.platformVersion = platformVersion;
        }

        @Override
        public int compareTo(Id other) {
            int result;
            if (0 != (result = locale.compareTo(other.locale))) {
                return result;
            }
            if (0 != (result = platform.compareTo(other.platform))) {
                return result;
            }
            if (0 != (result = variant.compareTo(other.variant))) {
                return result;
            }
            return 0;
        }

        @Override
        public String toString() {
            return locale + "/" + platform + "/" + variant;
        }

        public String getBaseLanguage() {
            int pos = locale.indexOf('-');
            return pos < 0 ? locale : locale.substring(0, pos);
        }
    }

    static class IdSet {
        Map<String, Relation<String, String>> data = new TreeMap<String, Relation<String, String>>();

        public void add(Id id) {
            Relation<String, String> platform2variant = data.get(id.platform);
            if (platform2variant == null) {
                data.put(id.platform, platform2variant = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            platform2variant.put(id.locale, id.variant);
        }

        public void addAll(Collection<Id> idSet) {
            for (Id id : idSet) {
                add(id);
            }
        }

        public String toString(IdSet allIds) {
            if (this.equals(allIds)) {
                return "*";
            }
            StringBuilder b = new StringBuilder();
            final Set<Entry<String, Relation<String, String>>> entrySet = data.entrySet();
            boolean first = true;
            for (Entry<String, Relation<String, String>> entry : entrySet) {
                if (first) {
                    first = false;
                } else {
                    b.append(" ");
                }
                String key = entry.getKey();
                Set<Entry<String, Set<String>>> valueSet = entry.getValue().keyValuesSet();
                b.append(key).append(":");
                appendLocaleAndVariants(b, valueSet, allIds.data.get(key));
            }
            return b.toString();
        }

        private void appendLocaleAndVariants(StringBuilder b, Set<Entry<String, Set<String>>> set,
            Relation<String, String> relation) {
            if (set.equals(relation.keyValuesSet())) {
                b.append("*");
                return;
            }
            final int setSize = set.size();
            if (setSize > 9) {
                b.append(setSize).append("/").append(relation.size());
                return;
            }
            final boolean isSingle = setSize == 1;
            if (!isSingle) b.append("(");
            boolean first = true;
            for (Entry<String, Set<String>> item : set) {
                if (first) {
                    first = false;
                } else {
                    b.append("|");
                }
                final String key = item.getKey();
                b.append(key);
                final Set<String> variants = item.getValue();
                final int size = variants.size();
                if (size != 0) {
                    if (size == 1) {
                        String firstOne = variants.iterator().next();
                        if (firstOne.isEmpty()) {
                            continue; // fr-CA/∅ => fr-CA
                        }
                    }
                    b.append("/");
                    appendVariant(b, variants, relation.get(key));
                }
            }
            if (!isSingle) b.append(")");
        }

        private void appendVariant(StringBuilder b, Set<String> set, Set<String> set2) {
            if (set.equals(set2)) {
                b.append("*");
                return;
            }
            final boolean isSingle = set.size() == 1;
            if (!isSingle) b.append("(");
            boolean first = true;
            for (String item : set) {
                if (first) {
                    first = false;
                } else {
                    b.append("|");
                }
                b.append(item.isEmpty() ? "∅" : item);
            }
            if (!isSingle) b.append(")");
        }

        public boolean isEquals(Object other) {
            return data.equals(((IdSet) other).data);
        }

        public int hashCode() {
            return data.hashCode();
        }
    }

    // public static class Key {
    // Iso iso;
    // ModifierSet modifierSet;
    // }
    // /**
    // * Return all possible results. Could be external utility. WARNING: doesn't account for transform='no' or
    // failure='omit'.
    // */
    // public Map<String,List<Key>> getPossibleSource() {
    // Map<String,List<Key>> results = new HashMap<String,List<Key>>();
    // UnicodeSet results = new UnicodeSet();
    // addOutput(getBaseMap().iso2output.values(), results);
    // for (KeyMap keymap : getKeyMaps()) {
    // addOutput(keymap.string2output.values(), results);
    // }
    // for (Transforms transforms : getTransforms().values()) {
    // // loop, to catch empty case
    // for (String result : transforms.string2string.values()) {
    // if (!result.isEmpty()) {
    // results.add(result);
    // }
    // }
    // }
    // return results;
    // }

    static UnicodeSet getQuotationMarks(CLDRFile file) {
        UnicodeSet results = new UnicodeSet();
        // TODO should have a test to make sure these are in exemplars.
        results.add(file.getStringValue("//ldml/delimiters/quotationEnd"));
        results.add(file.getStringValue("//ldml/delimiters/quotationStart"));
        results.add(file.getStringValue("//ldml/delimiters/alternateQuotationEnd"));
        results.add(file.getStringValue("//ldml/delimiters/alternateQuotationStart"));
        return results;
    }

    // TODO Add as utility to CLDRFile
    static UnicodeSet getNumericExemplars(CLDRFile file) {
        UnicodeSet results = new UnicodeSet();
        String defaultNumberingSystem = file.getStringValue("//ldml/numbers/defaultNumberingSystem");
        String nativeNumberingSystem = file.getStringValue("//ldml/numbers/otherNumberingSystems/native");
        // "//ldml/numbers/otherNumberingSystems/native"
        addNumberingSystem(file, results, "latn");
        if (!defaultNumberingSystem.equals("latn")) {
            addNumberingSystem(file, results, defaultNumberingSystem);
        }
        if (!nativeNumberingSystem.equals("latn") && !nativeNumberingSystem.equals(defaultNumberingSystem)) {
            addNumberingSystem(file, results, nativeNumberingSystem);
        }
        return results;
    }

    public static void addNumberingSystem(CLDRFile file, UnicodeSet results, String numberingSystem) {
        String digits = supplementalDataInfo.getDigits(numberingSystem);
        results.addAll(digits);
        addSymbol(file, numberingSystem, "decimal", results);
        addSymbol(file, numberingSystem, "group", results);
        addSymbol(file, numberingSystem, "minusSign", results);
        addSymbol(file, numberingSystem, "percentSign", results);
        addSymbol(file, numberingSystem, "plusSign", results);
    }

    public static void addSymbol(CLDRFile file, String numberingSystem, String key, UnicodeSet results) {
        String symbol = file.getStringValue("//ldml/numbers/symbols[@numberSystem=\"" + numberingSystem + "\"]/" +
            key);
        results.add(symbol);
    }
}
