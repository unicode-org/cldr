package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.draft.Keyboard;
import org.unicode.cldr.draft.Keyboard.KeyMap;
import org.unicode.cldr.draft.Keyboard.Platform;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CLDRFile.WinningChoice;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ShowKeyboards {

    static final TestInfo testInfo = TestInfo.getInstance();
    static final Factory factory = testInfo.getCldrFactory();

    // *********************************************
    // Temporary, for some simple testing
    // *********************************************
    public static void main(String[] args) {
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
        Set<String> totalErrors = new LinkedHashSet<String>();
        Set<String> errors = new LinkedHashSet<String>();
        UnicodeSet controls = new UnicodeSet("[:Cc:]").freeze();
        // check what the characters are, excluding controls.
        Map<Id,UnicodeSet> id2unicodeset = new TreeMap<Id,UnicodeSet>();
        Set<String> totalModifiers = new LinkedHashSet<String>();
        Relation<String, Id> locale2ids = Relation.of(new TreeMap<String,Set<Id>>(), TreeSet.class);
        LanguageTagCanonicalizer canonicalizer = new LanguageTagCanonicalizer();
        IdInfo idInfo = new IdInfo();

        for (String platformId : Keyboard.getPlatformIDs()) {
            Platform p = Keyboard.getPlatform(platformId);
            //            System.out.println(platformId + "\t" + p.getHardwareMap());
            for (String keyboardId : Keyboard.getKeyboardIDs(platformId)) {
                Keyboard keyboard = Keyboard.getKeyboard(platformId, keyboardId, errors);
                for (String error : errors) {
                    totalErrors.add(keyboardId + " " + error);                    
                }
                UnicodeSet unicodeSet = keyboard.getPossibleResults().removeAll(controls);
                final Id id = new Id(keyboardId, keyboard.getPlatformVersion());
                idInfo.add(id, unicodeSet);
                String canonicalLocale = canonicalizer.transform(id.locale).replace('_', '-');
                if (!id.locale.equals(canonicalLocale)) {
                    totalErrors.add("Non-canonical id: " + id.locale + "\t=>\t" + canonicalLocale);
                }
                id2unicodeset.put(id, unicodeSet);
                locale2ids.put(id.locale, id);
                System.out.println(id.toString().replace('/','\t') + "\t" + keyboard.getNames());
                for (KeyMap keymap : keyboard.getKeyMaps()) {
                    totalModifiers.add(keymap.getModifiers().toString());
                }
            }
        }
        if (totalErrors.size() != 0) {
            System.out.println("Errors\t" + CollectionUtilities.join(totalErrors, "\n\t"));
        }
        for (String item : totalModifiers) {
            System.out.println(item);
        }
        try {
            PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "../beta-charts/keyboards/", "chars2keyboards.html");
            printTop("Characters → Keyboards", out);
            idInfo.print(out);
            printBottom(out);
            out.close();
            
            out = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY + "../beta-charts/keyboards/", "keyboards2chars.html");
            printTop("Keyboards → Characters", out);
            showLocaleToCharacters(out, id2unicodeset, locale2ids);
            printBottom(out);
            out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void printTop(String title, PrintWriter out) {
        out.println(
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n" +
                "<style>\n" +
                "table {border-collapse:collapse}\n" +
                "td,th {border:1px solid blue; vertical-align:top}\n" +
                ".s {background-color:#DDD}\n" +
                ".ch {text-align:center; font-size:150%}\n" +
                ".c {text-align:center; font-family:monospace}\n" +
                ".n {font-size:75%}\n" +
                ".k {width:30%;font-size:75%}\n" +
                "</style>\n" +
                "<title>" + title + "</title>\n" + 
                "</head>\n" +
                "<body>\n" +
                "<h1>DRAFT " +
                title +
                "</h1>\n"
        );
    }
    public static void printBottom(PrintWriter pw) {
        pw.println(
                "</body>\n" +
                "</html>"
        );
    }

    public static void showLocaleToCharacters(PrintWriter out, Map<Id, UnicodeSet> id2unicodeset,
            Relation<String, Id> locale2ids) {

        TablePrinter t = new TablePrinter()
        .addColumn("Name").setSpanRows(true).setBreakSpans(true)
        .addColumn("Locale").setSpanRows(true).setBreakSpans(true)
        .addColumn("Platform").setSpanRows(true)
        .addColumn("Variant")
        .addColumn("Script")
        .addColumn("Statistics")
        .addColumn("Characters").setSpanRows(true);

        for (Entry<String, Set<Id>> localeAndIds : locale2ids.keyValuesSet()) {
            final String key = localeAndIds.getKey();
            final Set<Id> keyboardIds = localeAndIds.getValue();

            //System.out.println();
            final String localeName = testInfo.getEnglish().getName(key);
            final ULocale uLocale = ULocale.forLanguageTag(key);
            String script = uLocale.getScript();
            String writtenLanguage = uLocale.getLanguage() + (script.isEmpty() ? "" : "_" + script);
            CLDRFile cldrFile  = null;
            try {
                cldrFile = factory.make(writtenLanguage, false);
            } catch (Exception e) {}

            //            final String heading = uLocale.getDisplayName(ULocale.ENGLISH)
            //            + "\t" + ULocale.addLikelySubtags(uLocale).getScript() 
            //            + "\t";
            UnicodeSet common = UnicodeSet.EMPTY;
            if (keyboardIds.size() > 1) {
                common = UnicodeSet.EMPTY;
                String locale = null;
                for (Id keyboardId : keyboardIds) {
                    locale = keyboardId.locale;
                    if (common == UnicodeSet.EMPTY) {
                        common = id2unicodeset.get(keyboardId);
                    } else {
                        common.retainAll(id2unicodeset.get(keyboardId));
                    }
                }
                common.freeze();
                t.addRow()
                .addCell(localeName) // name
                .addCell(key) // locale
                .addCell("COMMON") // platform
                .addCell("") // variant
                .addCell(ULocale.addLikelySubtags(uLocale).getScript()) // script
                .addCell(getInfo(common, cldrFile)) // stats
                .addCell(safeUnicodeSet(common)) // characters
                .finishRow();

                //                System.out.println(
                //                        locale + "\tCOMMON\t\t-"
                //                        + "\t" + heading + getInfo(common, cldrFile)
                //                        + "\t" + common.toPattern(false));
            }
            for (Id keyboardId : keyboardIds) {
                final UnicodeSet current2 = id2unicodeset.get(keyboardId);
                final UnicodeSet remainder = new UnicodeSet(current2).removeAll(common);

                t.addRow()
                .addCell(localeName) // name
                .addCell(key) // locale
                .addCell(keyboardId.platform) // platform
                .addCell(keyboardId.variant) // variant
                .addCell(ULocale.addLikelySubtags(uLocale).getScript()) // script
                .addCell(getInfo(current2, cldrFile)) // stats
                .addCell(safeUnicodeSet(remainder)) // characters
                .finishRow();
                //                System.out.println(
                //                        keyboardId.toString().replace('/','\t')
                //                        + "\t" + keyboardId.platformVersion
                //                        + "\t" + heading + getInfo(current2, cldrFile)
                //                        + "\t" + remainder.toPattern(false));
            }
        }
        out.println(t.toTable());
    }

    static PrettyPrinter prettyPrinter = new PrettyPrinter()
    .setOrdering(Collator.getInstance(ULocale.ROOT))
    .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY)
    );

    public static String safeUnicodeSet(UnicodeSet unicodeSet) {
        return TransliteratorUtilities.toHTML.transform(prettyPrinter.format(unicodeSet));
    }

    static class IdInfo {
        final Collator collator = Collator.getInstance(ULocale.ENGLISH);
        BitSet bitset = new BitSet();
        BitSet bitset2 = new BitSet();
        TreeMap<String,IdSet>[] charToKeyboards = new TreeMap[UScript.CODE_LIMIT];
        {
            collator.setStrength(Collator.IDENTICAL);
            for (int i = 0; i < charToKeyboards.length; ++i) {
                charToKeyboards[i] = new TreeMap<String,IdSet>(collator);
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
                    for (int script2 = bitset.nextSetBit(0); script2 >= 0; script2 = bitset.nextSetBit(script2+1)) {
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
                UnicodeSet inScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, script).removeAll(notNFKC);

                //                UnicodeSet fullScript = new UnicodeSet(inScript);
                //                int fullScriptSize = inScript.size();
                if (inScript.size() == 0) {
                    continue;
                }
                final TreeMap<String, IdSet> charToKeyboard = charToKeyboards[script];
                final String scriptName = UScript.getName(script);
                if (charToKeyboard.size() == 0) {
                    missingScripts.add(scriptName);
                    continue;
                }

                // also check to see that at least one item is not all common
                check:
                if (script != UScript.COMMON && script != UScript.INHERITED) {
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
                        t.addRow()
                        .addCell(scriptName)
                        .addCell(TransliteratorUtilities.toHTML.transform(s))
                        .addCell(Utility.hex(s, 4, " + "))
                        .addCell(name)
                        .addCell(keyboardsString)
                        .finishRow();
                    }
                    inScript.remove(s);
                    last = s;
                }
                if (inScript.size() != 0 && script != UScript.UNKNOWN) {
                    //String pattern;
                    //                    if (inScript.size() < 255 || inScript.size()*4 < fullScriptSize) {
                    //                    } else {
                    //                        fullScript.removeAll(inScript);
                    //                        inScript = new UnicodeSet("[[:sc=" + UScript.getShortName(script) + ":]-" + fullScript.toPattern(false) + "]");
                    //                    }
                    t.addRow()
                    .addCell(scriptName)
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

    private static String getInfo(UnicodeSet common, CLDRFile cldrFile) {
        Counter<String> results = new Counter<String>();
        for (String s : common) {
            int first = s.codePointAt(0); // first char is good enough
            results.add(UScript.getShortName(UScript.getScript(first)), 1);
        }
        results.remove("Zyyy");
        results.remove("Zinh");
        results.remove("Zzzz");

        if (cldrFile != null) {
            UnicodeSet mainExemplars = cldrFile.getExemplarSet("", WinningChoice.WINNING);
            UnicodeSet auxExemplars = cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING);
            if (auxExemplars != null && auxExemplars.size() != 0) {
                addComparison("aux", common, new UnicodeSet(auxExemplars).addAll(mainExemplars), results);
            } else {
                addComparison("main", common, mainExemplars, results);
            }
        }

        return results.toString();
    }

    private static void addComparison(String title, UnicodeSet keyboard, UnicodeSet exemplars,
            Counter<String> results) {
        UnicodeSet common = new UnicodeSet(keyboard).retainAll(exemplars);
        if (common.size() != 0) results.add("k=" + title, common.size());
        common = new UnicodeSet(keyboard).removeAll(exemplars);
        if (common.size() != 0) results.add("k−" + title, common.size());
        common = new UnicodeSet(exemplars).removeAll(keyboard);
        if (common.size() != 0) results.add(title + "−k", common.size());
    }

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
            int pos2 = input.indexOf('-',pos);
            if (pos2 > 0) {
                platform = input.substring(pos, pos2);
                variant = input.substring(pos2+1);
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
    }

    static class IdSet {
        Map<String,Relation<String,String>> data = new TreeMap();
        public void add(Id id) {
            Relation<String, String> platform2variant = data.get(id.platform);
            if (platform2variant == null) {
                data.put(id.platform, platform2variant = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class));
            }
            platform2variant.put(id.locale, id.variant);
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
        private void appendLocaleAndVariants(StringBuilder b, Set<Entry<String, Set<String>>> set, Relation<String, String> relation) {
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
            return data.equals(((IdSet)other).data);
        }
        public int hashCode() {
            return data.hashCode();
        }
    }
    //    public static class Key {
    //        Iso iso;
    //        ModifierSet modifierSet;
    //    }
    //    /**
    //     * Return all possible results. Could be external utility. WARNING: doesn't account for transform='no' or failure='omit'.
    //     */
    //    public Map<String,List<Key>> getPossibleSource()  {
    //        Map<String,List<Key>> results = new HashMap<String,List<Key>>();
    //        UnicodeSet results = new UnicodeSet();
    //        addOutput(getBaseMap().iso2output.values(), results);
    //        for (KeyMap keymap : getKeyMaps()) {
    //            addOutput(keymap.string2output.values(), results);
    //        }
    //        for (Transforms transforms : getTransforms().values()) {
    //            // loop, to catch empty case
    //            for (String result : transforms.string2string.values()) {
    //                if (!result.isEmpty()) {
    //                    results.add(result);
    //                }
    //            }
    //        }
    //        return results;
    //    }
}
