package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Dictionary;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMapIterator;
import com.ibm.icu.dev.test.util.VariableReplacer;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.dev.test.util.Tabber.HTMLTabber;
import com.ibm.icu.impl.Punycode;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringPrep;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class IdnaLabelTester {

    private static boolean VERBOSE = false;

    enum Result {none, next, next2, fail};

    static class Rule {
        final Matcher before;
        final String beforeString;
        final Matcher at;
        final String atString;
        final Result result;
        final String title;
        final int lineNumber;
        transient int length;
        transient String label;

        public String toString() {
            return "{Rule "
            + (before == null ? "" : "before: " + beforeString + ", ")
            + "at: " + atString
            + ", result: " + result
            + ", line: " + lineNumber
            + ", title: " + title + "}";
        }

        public Rule(String before, String at, String result, String title, int lineNumber, VariableReplacer variables) {
            beforeString = before;
            if (before != null) {
                before = variables.replace(before.trim());
            }
            this.before = before == null || before == "" ? null 
                    : Pattern.compile(".*" + UnicodeRegex.fix(before), Pattern.COMMENTS).matcher(""); // hack, because Java doesn't have lookingBefore
            atString = at;
            at = variables.replace(at.trim());
            this.at = Pattern.compile(UnicodeRegex.fix(at), Pattern.COMMENTS).matcher("");
            this.result = Result.valueOf(result.toLowerCase().trim());
            this.title = title;
            this.lineNumber = lineNumber;
        }

        public Result match(int position) {
            if (before != null) {
                before.region(0, position);
                //before.reset(label.substring(0,position));
                if (!before.matches()) {
                    return Result.none;
                }
            }
            at.region(position, length);
            //at.reset(label.substring(position,length));
            if (!at.lookingAt()) {
                return Result.none;
            }

            return result;
        }

        public void setLabel(String label) {
            this.label = label;
            if (before != null) {
                before.reset(label);
            }
            at.reset(label);
            length = label.length();
        }
    }

    private List<Rule> rules = new ArrayList<Rule>();

    private static final UnicodeSet GRAPHIC = new UnicodeSet("[^[:cn:][:co:][:cs:][:cc:]]").freeze();
    private static final UnicodeSet NOT_NFKC_CASE_FOLD = computeNotNfkcCaseFold();
    VariableReplacer variables = new VariableReplacer();

    public IdnaLabelTester(String file) throws IOException {

        BufferedReader in = openFile(file);

        String title = "???";
        for (int lineCount = 1; ; ++lineCount) {
            String line = in.readLine();
            try {
                if (line == null) break;
                int commentPos = line.indexOf("#");
                if (commentPos >= 0) {
                    line = line.substring(0,commentPos);
                }
                line = line.trim();
                if (line.length() == 0) continue;

                // do debug

                if (startsWithIgnoreCase(line, "VERBOSE:")) {
                    VERBOSE = line.substring(8).trim().equalsIgnoreCase("true");
                    System.out.println("Verbose = " + VERBOSE);
                    continue;
                }

                // do title

                if (startsWithIgnoreCase(line, "Title:")) {
                    title = line.substring(6).trim();
                    continue;
                }

                // do variables

                if (startsWithIgnoreCase(line, "$")) {
                    int equals = line.indexOf("=");
                    if (equals >= 0) {
                        final String variable = line.substring(0,equals).trim();
                        final String value = variables.replace(line.substring(equals+1).trim());
                        if (VERBOSE && value.contains("$")) {
                            System.out.println("Warning: contains $ " + variable + "\t=\t" + value);
                        }
                        // small hack, because this property isn't in ICU until 5.2
                        UnicodeSet s = value.equals("[:^nfkc_casefolded:]")
                        ? NOT_NFKC_CASE_FOLD
                                : new UnicodeSet(value).complement().complement();
                        System.out.println(variable + "\tcontains 20000\t" + s.contains(0x20000));
                        if (VERBOSE) {
                            System.out.println("{Variable: " + variable + ", value: " + toPattern(s, true) + "}");
                        }
                        variables.add(variable, toPattern(s, false));
                        continue;
                    }
                }

                // do rules. This could be much more compact, but is broken out for debugging

                String[] pieces = line.split("\\s*;\\s*");
                //        if (DEBUG) {
                //          System.out.println(Arrays.asList(pieces));
                //        }
                String before, at, result;
                switch (pieces.length) {
                case 2: before = null; at = pieces[0]; result= pieces[1]; break;
                case 3: before = pieces[0]; at = pieces[1]; result= pieces[2]; break;
                default: throw new IllegalArgumentException(line + " => " + Arrays.asList(pieces));
                }
                Rule rule = new Rule(before, at, result, title, lineCount, variables);
                if (VERBOSE) {
                    System.out.println(rule);
                }
                rules.add(rule);
            } catch (Exception e) {
                throw (RuntimeException) new IllegalArgumentException("Error on line: " + lineCount + ".\t" + line).initCause(e);
            }
        }
        in.close();
    }
    // 248C ;   0035 002E ; MA  #* ( ⒌ → 5. ) DIGIT FIVE FULL STOP → DIGIT FIVE, FULL STOP  # {nfkc:9357}

    public static XEquivalenceClass<String,String> getConfusables() throws IOException {
        XEquivalenceClass<String,String> result = new XEquivalenceClass<String,String>();
        BufferedReader in = openFile("/Users/markdavis/Documents/workspace35/draft/reports/tr39/data/confusables.txt");
        String original = null;
        try {
            while (true) {
                String line = in.readLine();
                original = line;
                if (line == null) break;
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                int pos = line.indexOf('#');
                if (pos >= 0) line = line.substring(0,pos);
                line = line.trim();
                if (line.length() == 0) continue;
                String[] parts = line.split("\\s*;\\s*");
                if (parts[2].equals("MA")) continue;

                String cp = Utility.fromHex(parts[0], 4, SPACES);
                String s = Utility.fromHex(parts[1], 4, SPACES);
                result.add(cp, s);
            } 
        } catch (Exception e) {
            throw new IllegalArgumentException("Line:\t" + original, e);
        } finally {
            in.close();
        }
        return result;
    }

    static Pattern SPACES = Pattern.compile("\\s+");

    private static UnicodeSet computeNotNfkcCaseFold() {
        //    B: toNFKC(toCaseFold(toNFKC(cp))) != cp
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            // quick check to avoid extra processing
            if (i == 0x10000) {
                System.out.println("debug??");
            }
            int type = UCharacter.getType(i);
            if (type == UCharacter.UNASSIGNED || type == UCharacter.SURROGATE || type == UCharacter.PRIVATE_USE) {
                // result.add(i);
                continue;
            }
            String nfkc = Normalizer.normalize(i, Normalizer.NFKC);
            String case_nfkc = UCharacter.foldCase(nfkc, true);
            String nfkc_case_nfkc = Normalizer.normalize(case_nfkc, Normalizer.NFKC);
            if (!equals(nfkc_case_nfkc, i)) {
                result.add(i);
            }
        }
        return (UnicodeSet) result.freeze();
    }

    static String removals = new UnicodeSet("[\u1806[:di:]-[:cn:]]").complement().complement().toPattern(false);
    static Matcher rem = Pattern.compile(removals).matcher("");

    private static FrequencyData frequencies;

    private static String NFKC_CaseFold(int i, Normalizer.Mode mode, boolean onlyLower, boolean keepDI) {
        String nfkc = Normalizer.normalize(i, mode);
        String case_nfkc = onlyLower ? UCharacter.toLowerCase(ULocale.ROOT, nfkc) : UCharacter.foldCase(nfkc, true);
        String nfkc_case_nfkc = Normalizer.normalize(case_nfkc, mode);
        if (keepDI) return nfkc_case_nfkc;
        return rem.reset(nfkc_case_nfkc).replaceAll("");
    }

    private static boolean equals(String string, int codePoint) {
        switch(string.length()) {
        case 1: return codePoint == string.charAt(0);
        case 2: return codePoint >= 0x10000 && codePoint == string.codePointAt(0);
        default: return false;
        }
    }

    public static final Charset UTF8 = Charset.forName("utf-8");

    private static BufferedReader openFile(String file) throws IOException {
        try {
            File file1 = new File(file);
            //System.out.println("Reading:\t" + file1.getCanonicalPath());
            return new BufferedReader(new InputStreamReader(new FileInputStream(file1), UTF8),1024*64);
        } catch (Exception e) {
            File f = new File(file);
            throw new IllegalArgumentException("Bad file name: " + f.getCanonicalPath());
        }
    }

    private static boolean startsWithIgnoreCase(String line, final String string) {
        // we don't care about performance, and the arguments are only ASCII
        return line.toLowerCase(Locale.ENGLISH).startsWith(string.toLowerCase(Locale.ENGLISH));
    }

    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    public static final Transliterator UNESCAPER = Transliterator.getInstance("hex-any");
    public static final Transliterator ESCAPER = Transliterator.getInstance("any-hex");
    static {
        ESCAPER.setFilter(TO_QUOTE);
    }

    private static final PrettyPrinter PRETTY_PRINTER = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
    .setToQuote(TO_QUOTE)
    .setOrdering(null)
    .setSpaceComparator(null);

    private static String toPattern(UnicodeSet s, boolean escape) {
        return !escape 
        ? s.toPattern(false) 
                : PRETTY_PRINTER.format(s);
    }

    // ==================== Test Code =======================

    public static class TestStatus {
        public String title;
        public int position;
        int ruleLine;

        public TestStatus(int position, String title, int lineNumber) {
            this.position = position;
            this.title = title;
            this.ruleLine = lineNumber;
        }
    }

    /**
     * Test a label; null for success.
     * Later, return information.
     * @param label
     * @return
     */

    public TestStatus test(String label) {
        // initialize
        for (Rule rule : rules) {
            rule.setLabel(label);
        }
        boolean skipOverFail = false;
        boolean skipOverFailAndNext2 = false;
        // note: it doesn't matter if we test in the middle of a supplemental character
        for (int i= 0; i < label.length(); ++i) {
            for (Rule rule : rules) {

                // handle the skipping

                switch (rule.result) {
                case fail: if (skipOverFail || skipOverFailAndNext2) continue;
                break;
                case next2: if (skipOverFailAndNext2) continue;
                break;
                }
                skipOverFail = false;
                skipOverFailAndNext2 = false;

                // check the rule

                Result result = rule.match(i);
                switch (result) {
                case next: 
                    if (VERBOSE) {
                        rule.match(i);
                    }
                    skipOverFailAndNext2 = true;
                    break;
                case next2: 
                    if (VERBOSE) {
                        rule.match(i);
                    }
                    skipOverFail = true;
                    break;
                case fail: 
                    if (VERBOSE) {
                        rule.match(i);
                    }
                    return new TestStatus(i, rule.title, rule.lineNumber);
                default:
                    if (VERBOSE) {
                        rule.match(i);
                    }
                    break;
                }
            }
        }
        return null; // success!
    }

    public static void main(String[] args) throws Exception {
        //checkMapIterator();
        showPunycode("γιατρόσ");
        showPunycode("γιατρός");
        showPunycode("γιατρός".toUpperCase());
        showPunycode("weltfussball");
        showPunycode("weltfußball");
        showPunycode("weltfussball".toUpperCase());
        showPunycode("weltfuẞball".toUpperCase());
        System.out.println("γιατρός\t" + Punycode.encode(new StringBuffer("γιατρός"), null));
        System.out.println("weltfussball\t" + Punycode.encode(new StringBuffer("weltfussball"), null));

        String dir = "tools/java/org/unicode/cldr/draft/";
        IdnaLabelTester tester = new IdnaLabelTester(dir + "idnaContextRules.txt");
        BufferedReader in = openFile(dir + "idnaTestCases.txt");
        frequencyFile = org.unicode.cldr.util.CldrUtility.getProperty("frequency");

        boolean expectedSuccess = true;
        int failures = 0;
        int successes = 0;
        boolean firstTestLine = true;

        for (int lineCount = 1; ; ++lineCount) {
            String line = in.readLine();
            if (line == null) break;
            int commentPos = line.indexOf("#");
            if (commentPos >= 0) {
                line = line.substring(0,commentPos);
            }
            line = UNESCAPER.transform(line);

            line = line.trim();
            if (line.length() == 0) continue;

            if ("valid".equalsIgnoreCase(line)) {
                expectedSuccess = true;
                continue;
            }
            if ("invalid".equalsIgnoreCase(line)) {
                expectedSuccess = false;
                continue;
            }

            if (startsWithIgnoreCase(line, "VERBOSE:")) {
                VERBOSE = line.substring(8).trim().equalsIgnoreCase("true");
                System.out.println("Verbose = " + VERBOSE);
                continue;
            }

            if ("showmapping".equalsIgnoreCase(line)) {
                tester.showMapping();
                continue;
            }

            if ("checkPatrik".equalsIgnoreCase(line)) {
                tester.checkPatrik();
                continue;
            }


            if (firstTestLine) {
                if (VERBOSE) {
                    System.out.println("# Test lines are in the form <lineNumber>. <successOrFailure> <reason>;");
                }
                firstTestLine = false;
            }

            TestStatus result = tester.test(line);

            boolean showLine = false;
            if (result == null) {
                if (expectedSuccess) {
                    if (VERBOSE) {
                        System.out.print(lineCount + ". \tSuccess - expected and got Valid:\t");
                        showLine = true;
                    }
                    successes++;
                } else {
                    System.out.print(lineCount + ". \tFAILURE - expected Invalid, was valid:\t");
                    failures++;
                    showLine = true;
                }
                if (showLine) {
                    System.out.println(ESCAPER.transform(line));
                }
            } else {
                if (expectedSuccess) {
                    System.out.print(lineCount + ". \tFAILURE - expected Valid, was invalid:\t");
                    failures++;
                    showLine = true;
                } else {
                    if (VERBOSE) {
                        System.out.print(lineCount + ". \tSuccess - expected and got Invalid:\t");
                        showLine = true;
                    }
                    successes++;
                }
                if (showLine) {
                    System.out.println(ESCAPER.transform(line.substring(0, result.position)) 
                            + "\u2639" + ESCAPER.transform(line.substring(result.position)) 
                            + "\t\t" + result.title
                            + "; \tRuleLine: " + result.ruleLine);
                }
            }
        }
        System.out.println("Successes:\t" + successes);
        System.out.println("Failures:\t" + failures);
        in.close();
    }

    private static void showPunycode(String string) throws StringPrepParseException {
        System.out.println(string +
                "\t" + Punycode.encode(new StringBuffer(string), null));
    }

    private static void checkMapIterator() {
        UnicodeMap<String> foo = new UnicodeMap<String>();
        //foo.putAll(new UnicodeSet("[:cc:]"), " control");
        foo.putAll(new UnicodeSet("[:Lu:]"), " upper");
        foo.putAll(new UnicodeSet("[:Ll:]"), " lower");
        for (UnicodeMapIterator<String> it = new UnicodeMapIterator<String>(foo); it.nextRange();) {
            String codepointsHex = Utility.hex(it.codepoint, 4);
            if (it.codepoint != it.codepointEnd) {
                codepointsHex += ".." + Utility.hex(it.codepointEnd, 4);
            }
            System.out.println(codepointsHex + it.value);
        }
    }

    private void checkPatrik() throws IOException {

        UnicodeMap<IdnaStatus> mine = new UnicodeMap<IdnaStatus>();
        UnicodeSet contextj = new UnicodeSet(variables.replace("$JoinControl")).freeze();
        UnicodeSet contexto = new UnicodeSet(variables.replace("$Context")).removeAll(contextj).freeze();
        UnicodeSet unassigned = new UnicodeSet(variables.replace("$Unassigned")).removeAll(contextj).freeze();
        UnicodeSet valid = new UnicodeSet(variables.replace("$Valid")).freeze();
        UnicodeSet valid2 = new UnicodeSet(variables.replace("$Valid2")).freeze();
        boolean valid2ok = valid.equals(valid2);
        System.out.println("Valid=valid2? " + valid2ok);
        if (!valid2ok) {
            System.out.println("valid-valid2:" + new UnicodeSet(valid).removeAll(valid2));
            System.out.println("valid2-valid:" + new UnicodeSet(valid2).removeAll(valid));
        }
        UnicodeSet pvalid = new UnicodeSet(valid).removeAll(contexto).removeAll(contextj);
        UnicodeSet pvalidWithContexto = new UnicodeSet(valid).removeAll(contextj);
        UnicodeMap<Row.R5<IdnaStatus, String, IdnaStatus, String, Integer>> myLines = new UnicodeMap<Row.R5<IdnaStatus, String, IdnaStatus, String, Integer>>();

        mine.putAll(contextj, IdnaStatus.CONTEXTJ);
        mine.putAll(contexto, IdnaStatus.CONTEXTO);
        mine.putAll(new UnicodeSet(valid).removeAll(contexto).removeAll(contextj), IdnaStatus.PVALID);
        mine.putAll(unassigned, IdnaStatus.UNASSIGNED);
        mine.putAll(new UnicodeSet(variables.replace("$Invalid")).removeAll(unassigned), IdnaStatus.DISALLOWED);

        // $Context = [$ExceptionContexto $BackwardCompatibleContexto $JoinControl]
        // $ValidAlways = [$ExceptionPvalid $BackwardCompatiblePvalid $LDH]
        // $InvalidLetterDigits = [$ExceptionDisallowed $BackwardCompatibleDisallowed $Unassigned $Unstable $IgnorableProperties $IgnorableBlocks $OldHangulJamo]
        // $Valid = [$ValidAlways $Context [$LetterDigits - $InvalidLetterDigits]]
        
        countConfusables(pvalid, contexto);

        BufferedReader in = openFile("../DATA/IDN/idna-calculation.txt");
        /*
         * The table has the following format:
         * 
         * 000020; DISALLOWED # C : Zs : NOK : HOSTNAME # SPACE
         * Unicode codepoint
         * IDNA2008 property value
         * Rule(s) in the table document the codepoint matches 
         * General category
         * Is the codepoint OK or NOK as _output_ of IDNA2003
         * If NOK according to IDNA2003, why?
         * Name of the codepoint
         */
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            try {
                line = line.trim().replace('#', ';').replace(':', ';');
                String[] parts = line.split("\\s*;\\s*");
                int cp = Integer.parseInt(parts[0], 16);
                int gc = UCharacter.getType(cp);
                if (gc == UCharacter.UNASSIGNED) {
                    continue; // skip for now
                }
//                if (cp == 0x10000) {
//                    System.out.println("debug?");
//                }

                String s = UTF16.valueOf(cp);

                IdnaStatus idna2008 = IdnaStatus.valueOf(parts[1]);
                String rule2008 = parts[2];
                String gcPatrik = parts[3];
                IdnaStatus idna2003out = parts[4].equals("OK") ? IdnaStatus.PVALID : IdnaStatus.DISALLOWED;
                String idna2003why = parts[5];
                String cpName = parts[6];

                String diff = "";
                IdnaStatus my2008 = mine.get(cp);
                if (!my2008.equals(idna2008)) {
                    diff += "idna2008; ";
                }
                String myName = getName(cp);

                if (gc == UCharacter.UNASSIGNED || gc == UCharacter.PRIVATE_USE  || gc == UCharacter.SURROGATE  || gc == UCharacter.CONTROL) {
                    // do nothing
                } else {
                    if (!myName.equals(cpName) && !myName.startsWith("CJK UNIFIED IDEOGRAPH-") && !myName.startsWith("HANGUL SYLLABLE ")) {
                        diff += "name; ";
                    }
                }
                String myGc = getGc(cp);
                if (!myGc.equals(gcPatrik)) {
                    diff += "gc; ";
                }

                IdnaStatus myIdna2003 = getIdnaStatus(s);
                //                if ((myIdna2003a == IdnaStatus.PVALID) != idna2003out.equals("OK")) {
                //                    //diff += "idna2003; ";
                //                }

                String idna2008map = getIDNA2008Value(s, true);
                if (idna2008map.equals("")) {
                    idna2008map = s;
                }
                if (my2008 == IdnaStatus.DISALLOWED && valid.containsAll(idna2008map) && !idna2008map.equals(s)) {
                    my2008 = IdnaStatus.REMAP;
                } else {
                    idna2008map = "\uE000";
                }

                String idna2003map = getIDNAValue(s, StringPrep.ALLOW_UNASSIGNED, null);
                if (myIdna2003 != IdnaStatus.REMAP) {
                    idna2003map = "\uE000";
                }

                //                String predicate = ";\t" + my2008 + ";\t" + idna2008map + ";\t" + myIdna2003  + ";\t" + idna2003map;
                //                String myLine = hex4 + predicate + ";\t" + myGc + ";\t" + myName;
                R5<IdnaStatus, String, IdnaStatus, String, Integer> row = Row.of(my2008, idna2008map, myIdna2003, idna2003map, UScript.getScript(cp));
                myLines.put(cp, row);

                //                if (diff.length() != 0) {
                //                    System.out.println(line + "\n≠\t" + myLine + "\n#\tdiff:\t" + diff);
                //                }
            } catch (Exception e) {
                throw (RuntimeException) new IllegalArgumentException("EXCEPTION with:\t" + line).initCause(e);
            }
        }
        in.close();
        printFullComparison(myLines);
    }
    
    static class ConfusableData {
        long count;
        long countWeighted;
        long countWeightedIdna;
        UnicodeSet samples = new UnicodeSet();
        public void add(int codePoint, long weight, long weightIdna) {
            count++;
            countWeighted += weight;
            countWeightedIdna += weightIdna;
            samples.add(codePoint);
        }
        public String toString() {
            return count + "\t" + countWeighted + "\t" + countWeightedIdna + "\t" + samples;
        }
        /**
         * Creates new
         */
        ConfusableData minus(ConfusableData other) {
            ConfusableData result = new ConfusableData();
            result.count = count - other.count;
            result.countWeighted = countWeighted - other.countWeighted;
            result.countWeightedIdna = countWeightedIdna - other.countWeightedIdna;
            result.samples = new UnicodeSet(samples).removeAll(other.samples);
            return result;
        }
    }
    
    private void countConfusables(UnicodeSet pvalid, UnicodeSet contexto) throws IOException {
        Counter<Integer> idnaWeights = IdnaFrequency.getData(false);
        loadFrequencies();
        UnicodeSet idna2003Valid = new UnicodeSet();
        for (String s : GRAPHIC) {
            if (getIdnaStatus(s) == IdnaStatus.PVALID) {
                idna2003Valid.add(s);
            }
        }
        idna2003Valid.freeze();
        XEquivalenceClass<String, String> equivs = getConfusables();
//        int i = 0;
//        for (String sample : equivs) {
//            System.out.println((i++) + "\t" + Utility.hex(sample) + "\t" + equivs.getEquivalences(sample));
//        }
        
        ConfusableData valid = new ConfusableData();
        ConfusableData pvalid_pvalid = new ConfusableData();
        ConfusableData cvalid_cvalid = new ConfusableData();
        ConfusableData pvalid3_pvalid3 = new ConfusableData();
        ConfusableData pvalid_pvalid3 = new ConfusableData();

        UnicodeSet syntax = new UnicodeSet("[\\.\\/\\#\\?\\:\\-]");
        // fix to add syntax to pvalid
        pvalid = new UnicodeSet(pvalid).addAll(syntax).freeze();
        contexto = new UnicodeSet(contexto).removeAll(syntax).freeze();
        UnicodeSet pvalidWithContexto = new UnicodeSet(pvalid).addAll(contexto);
        UnicodeSet allTest = new UnicodeSet(pvalid).addAll(contexto).addAll(idna2003Valid).addAll(syntax).removeAll(new UnicodeSet("[:ideographic:]")).freeze();
        for (String item : allTest) {
            int codePoint = item.codePointAt(0);
            long weight = frequencies.getCount(codePoint);
            if (weight == 0) weight = 1;
            long weightIdna = idnaWeights.getCount(codePoint);
            if (weightIdna == 0) weightIdna = 1;

            valid.add(codePoint, weight, weightIdna);

            if (!equivs.hasEquivalences(item)) continue;
            boolean inSyntax = syntax.containsAll(item);

            Set<String> equivalentItems = equivs.getEquivalences(item);
            
            boolean has_pvalid_pvalid = false;
            boolean has_cvalid_cvalid = false;
            boolean has_pvalid3_pvalid3 = false;
            boolean has_pvalid_pvalid3 = false;
            
            for (String item2 : equivalentItems) {
                if ((pvalid.containsAll(item) || inSyntax) && pvalid.containsAll(item2)) {
                    has_pvalid_pvalid = true;
                }
                if ((pvalidWithContexto.containsAll(item) || inSyntax) && pvalidWithContexto.containsAll(item2)) {
                    has_cvalid_cvalid = true;
                }
                if ((idna2003Valid.containsAll(item) || inSyntax) && idna2003Valid.containsAll(item2)) {
                    has_pvalid3_pvalid3 = true;
                }
                if ((pvalid.containsAll(item) || inSyntax) && idna2003Valid.containsAll(item2)) {
                    has_pvalid_pvalid3 = true;
                }
            }
            if (has_pvalid_pvalid) {
                pvalid_pvalid.add(codePoint, weight, weightIdna);
            }
            if (has_cvalid_cvalid && !has_pvalid_pvalid) {
                cvalid_cvalid.add(codePoint, weight, weightIdna);
            }
            if (has_pvalid3_pvalid3 && !has_cvalid_cvalid && !has_pvalid_pvalid) {
                pvalid3_pvalid3.add(codePoint, weight, weightIdna);
            }
            if (has_pvalid_pvalid3 && !has_pvalid_pvalid) {
                pvalid_pvalid3.add(codePoint, weight, weightIdna);
            }
        }
        System.out.println("valid.count (non-Han):\t" + valid);
        System.out.println("pvalid_pvalid.count:\t" + pvalid_pvalid);
        System.out.println("cvalid_cvalid.count:\t" + cvalid_cvalid);
        System.out.println("pvalid3_pvalid3.count:\t" + pvalid3_pvalid3);
        System.out.println("pvalid_pvalid3.count:\t" + pvalid_pvalid3);
        //System.out.println('\u03C2' + "\t" + frequencies.getCount('\u03C2') + "\t" + idnaWeights.getCount((int)'\u03C2'));
        //System.out.println('\u00DF' + "\t" + frequencies.getCount('\u00DF') + "\t" + idnaWeights.getCount((int)'\u00DF'));
    }

    enum Diff {same, warn, bad};

    private void printFullComparison(UnicodeMap<R5<IdnaStatus, String, IdnaStatus, String, Integer>> myLines) throws IOException {
        Tabber tabber = new Tabber.MonoTabber()
        .add(12, Tabber.LEFT) // code
        .add(12, Tabber.CENTER) // chars
        .add(12, Tabber.LEFT) // idna2008
        .add(12, Tabber.LEFT) // map
        .add(12, Tabber.LEFT) // idna2003
        .add(12, Tabber.LEFT) // map
        .add(12, Tabber.LEFT) // gc
        .add(7, Tabber.LEFT) // name
        ;
        HTMLTabber htmlTabber = new Tabber.HTMLTabber();
        //003A..0040;DISALLOWED;;DISALLOWED;; Po Sm;COLON..COMMERCIAL AT
        //0041;REMAP; 0061;  REMAP; 0061;    Lu;  LATIN CAPITAL LETTER A

        PrintWriter out = BagFormatter.openUTF8Writer(org.unicode.cldr.util.CldrUtility.getProperty("out"), "idna-info.txt");
        PrintWriter out2 = BagFormatter.openUTF8Writer(org.unicode.cldr.util.CldrUtility.getProperty("out"), "idna-info-tab.txt");
        PrintWriter out3 = BagFormatter.openUTF8Writer(org.unicode.cldr.util.CldrUtility.getProperty("out"), "idna-info.html");
        out3.println("<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>IDNA Info</title>\n" +
                "<link rel='stylesheet' type='text/css' href='idna-info.css'>\n" +
                "</head>\n" +
                "<body>\n" +
                "<p><a href='http://www.macchiato.com/unicode/idna/idna-info-key'>Key</a></p>" +
        "<table>");
        String title = "D\tCode Points\tChars\tIdna2008\tMap\tIdna2003\tMap\tScript\tGCs\tDescription";
        out.println(tabber.process(title));
        out2.println(title);
        htmlTabber.setElement("th");
        out3.println(htmlTabber.process(title));
        htmlTabber.setElement("td");

        Set<String> gcs = new LinkedHashSet<String>();
        Set<String> scripts = new LinkedHashSet<String>();
        for (UnicodeMapIterator<R5<IdnaStatus, String, IdnaStatus, String, Integer>> it 
                = new UnicodeMapIterator<R5<IdnaStatus, String, IdnaStatus, String, Integer>>(myLines); it.nextRange();) {
            String codepointsHex = Utility.hex(it.codepoint, 4);
            String myName = getName(it.codepoint);
            String myGc = getGc(it.codepoint);
            String myScript = UScript.getShortName(UScript.getScript(it.codepoint));
            String charsHex = TransliteratorUtilities.toHTML.transform(UTF16.valueOf(it.codepoint));
            if (it.codepoint != it.codepointEnd) {
                codepointsHex += "\u200B.." + Utility.hex(it.codepointEnd, 4);
                myName += "\u200B.." + getName(it.codepointEnd);
                charsHex +=  "\u200B.." + TransliteratorUtilities.toHTML.transform(UTF16.valueOf(it.codepointEnd));
                gcs.clear();
                scripts.clear();
                for (int cp = it.codepoint; cp < it.codepointEnd; ++cp) {
                    gcs.add(getGc(cp));
                    scripts.add(UScript.getShortName(UScript.getScript(cp)));
                }
                myGc = shortStringForSet(gcs, myGc, 2);
                myScript = shortStringForSet(scripts, myScript, 2);
            }
            String myNameHtml = TransliteratorUtilities.toHTML.transform(myName);
            IdnaStatus valid2003 = it.value.get2();
            IdnaStatus valid2008 = it.value.get0();
            if (valid2008 == IdnaStatus.CONTEXTJ || valid2008 == IdnaStatus.CONTEXTO) {
                valid2008 = IdnaStatus.PVALID;
            }
            Diff diff = (valid2003 == IdnaStatus.REMAP || valid2003 == IdnaStatus.PVALID) 
            && (valid2008 == IdnaStatus.REMAP || valid2008 == IdnaStatus.PVALID)
            && !it.value.get1().equals(it.value.get3()) ? Diff.bad 
                    : valid2003 != IdnaStatus.UNASSIGNED && valid2008 != valid2003 ? Diff.warn 
                            : Diff.same;
            switch (diff) {
            case bad:
                htmlTabber.setParameters(0, "class='bad'");
                htmlTabber.setParameters(3, "class='bad'");
                htmlTabber.setParameters(5, "class='bad'");
                break;
            case warn:
                htmlTabber.setParameters(0, "class='warn'");
                htmlTabber.setParameters(3, "class='" + valid2008 + "'");
                htmlTabber.setParameters(5, "class='" + valid2003 + "'");
                break;
            case same:
                htmlTabber.setParameters(0, null);
                htmlTabber.setParameters(3, null);
                htmlTabber.setParameters(5, null);
            }
            out.println(tabber.process(getline(diff, codepointsHex, "", it.value, myScript, myGc, myName, false)));
            out2.println(getline(diff, codepointsHex, "", it.value, myGc, myScript, myName, true));
            out3.println(htmlTabber.process(getline(diff, codepointsHex, charsHex, it.value, myScript, myGc, myNameHtml, true)));
        }
        out3.println("</table>\n" +
                "</body>\n" +
        "</html>");
        out.close();
        out2.close();
        out3.close();
    }

    private String shortStringForSet(Set<String> gcs, String myGc, int limit) {
        if (gcs.size() != 1) {
            myGc = "";
            for (String item: gcs) {
                if (--limit < 0) {
                    myGc += "...";
                    break;
                }
                if (myGc.length() != 0) myGc += " ";
                myGc += item;
            }
            if (limit < gcs.size()) {

            }
            myGc = "{" + myGc + "}";
        }
        return myGc;
    }

    private String getline(Diff diff, String codepointsHex, String chars, R5<IdnaStatus, String, IdnaStatus, String, Integer> value, 
            String myScript, String myGc, String myName, boolean fixHex) {
        IdnaStatus v8 = value.get0();
        String m8 = value.get1();
        IdnaStatus v3 = value.get2();
        String m3 = value.get3();

        String sep = ";\t";  
        if (fixHex) {
            codepointsHex = "U+"+codepointsHex.replace("..", "..U+");
            sep = "\t";
        }

        String m8a = v8 != IdnaStatus.REMAP ? "n/a" : 
            m8.length() == 0 ? "delete" : 
                hex(m8, fixHex);
        String m3a = v3 != IdnaStatus.REMAP ? "n/a" :
            v8 == IdnaStatus.REMAP && m8.equals(m3) ? "~" : 
                m3.length() == 0 ? "delete" : 
                    hex(m3, fixHex);

        String v8a = v8.toString();
        String v3a = v8 == v3 ? "~" : v3.toString();
        String line = (diff == Diff.bad ? "X" : diff == Diff.warn ? "w" : "") + sep + codepointsHex + sep + chars + sep + v8a + sep + m8a + sep + v3a + sep + m3a + sep + myScript + sep + myGc + sep + myName;
        return line;
    }

    private String getGc(int cp) {
        return UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY,  UCharacter.getType(cp), UProperty.NameChoice.SHORT);
    }

    private String getName(int cp) {
        String myName = UCharacter.getExtendedName(cp);
        if (myName == null) myName = "<non-graphic>";
        return myName;
    }

    private String hex(String item, boolean fixHex) {
        String hex = Utility.hex(item, 4, " ");
        if (fixHex && item.length() != 0) hex = "U+"+hex.replace(" ", " U+");
        return hex;
    }
    
    public String getVariable(String variableName) {
     return variables.replace(variableName);
    }

    private void showMapping() {
        UnicodeSet valid = new UnicodeSet(variables.replace("$Valid"));
        UnicodeSet graphic = new UnicodeSet("[^[:cn:][:co:][:cs:][:cc:]]");
        Counter<String> counter = new Counter<String>();
        Map<String,R2<Long, Set<R2<Long, String>>>> examples = new HashMap<String,R2<Long, Set<R2<Long, String>>>>();
        double totalFrequency = 0;
        loadFrequencies();

        for (String s : graphic) {
            String idna2003 = getIDNAValue(s, StringPrep.ALLOW_UNASSIGNED, null); // StringPrep.ALLOW_UNASSIGNED
            String idna2008 = getIDNA2008Value(s, true);
            //String idna2008c = getIDNA2008Value(s, true);
            // a problem case is where they are not equal, and both mapped values are valid
            String eq = idna2008.equals(idna2003) ? "↔" : "⇹";
            String sourceStatus = getIdnaStatus(valid, s);
            String status3 = getIdnaStatus(valid, idna2003);
            String status8 = getIdnaStatus(valid, idna2008);
            //String status8c = getIdnaStatus(valid, idna2008c);
            String key = eq+"\t"+sourceStatus+"\t"+status3+"\t"+status8; // + (idna2008.equals(idna2008c) ? "" : " "+status8c);
            counter.add(key, 1);
            long newFrequency = frequencies.getCount(s.codePointAt(0));
            totalFrequency += newFrequency;

            R2<Long, Set<R2<Long, String>>> old = examples.get(key);
            if (old == null) {
                Set<R2<Long, String>> set = new TreeSet<R2<Long, String>>();
                old = Row.of(new Long(newFrequency), set);
                examples.put(key, old);
            }
            long oldFrequency = old.get0();
            old.set0(newFrequency + oldFrequency);
            if (true) {
                String example = getExample(s, idna2003, idna2008, eq, sourceStatus, status3, status8, null, null);
                Set<R2<Long, String>> set = old.get1();
                set.add(Row.of(new Long(-newFrequency), example));
            }
            if (!idna2008.equals(idna2003)) {
                System.out.println(getExample(s, idna2003, idna2008, eq, sourceStatus, status3, status8, null, null));
            }
        }
        System.out.println("==== Char count for groups");
        for (String s : counter) {
            R2<Long, Set<R2<Long, String>>> data  = examples.get(s);
            double freq = data.get0();
            R2<Long, String> freqSample = data.get1().iterator().next();
            System.out.println(freqSample.get1() + "\t" + counter.get(s) + "\t" + (freq/totalFrequency));
        }

        System.out.println("==== Samples for groups");
        for (String s : counter) {
            R2<Long, Set<R2<Long, String>>> data  = examples.get(s);
            double freq = data.get0();
            int max = 10;
            for (R2<Long, String> freqSample : data.get1()) {
                if (--max <= 0) break;
                System.out.println(freqSample.get1() + "\t" + freq + "\t" + (-freqSample.get0()/freq));
            }
        }

    }
    private void loadFrequencies() {
        if (frequencies == null && frequencyFile != null) {
            try {
                frequencies = new FrequencyData(frequencyFile, false);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static String getExample(String s, String idna2003, String idna2008, String eq, String sourceStatus, String status3, String status8, String idna2008c, String status8c) {
        return version(s) + "\t" + eq
        + "\t" + getCodeAndName(s) + "\t" + sourceStatus
        + "\t" + getCodeAndName(idna2003, s) + "\t" + status3
        + "\t" + getCodeAndName(idna2008, s) + "\t" + status8
        //        + (idna2008.equals(idna2008c) ? "" : "\t" + getCodeAndName(idna2008c, s) + "\t" + status8c)
        ;
    }

    private static String getCodeAndName(String mapped, String s) {
        if (mapped == null) {
            return "~";
        } else if (mapped.equals(s)) {
            return "~";
        } else {
            return getCodeAndName(mapped);
        }
    }

    private static String version(String s) {
        VersionInfo age = UCharacter.getAge(s.codePointAt(0));
        return "U" + age.getMajor() + "." + age.getMinor();
    }

    private static String getIdnaStatus(UnicodeSet valid, String s) {
        return (s != null && getIDNAValue(s, StringPrep.DEFAULT, null) != null ? "V" : "x") + "3"
        + "\t" + (s != null && valid.containsAll(s) ? "V" : "x") + 8;
    }

    private static String getCodeAndName(String string) {
        StringBuilder result = new StringBuilder();
        int cp;
        for (int i = 0; i < string.length(); ++i) {
            cp = string.codePointAt(i);
            if (i > 0) {
                result.append(" + ");
            }
            result.append(Utility.hex(cp) + " (" + UTF16.valueOf(cp) + ") " + UCharacter.getName(cp));
            if (cp > 0xFFFF) ++i;
        }
        return result.toString();
    }

    private static void addMapping(Map<String, UnicodeSet> mapping, int i, String mapped) {
        String s = UTF16.valueOf(i);
        if (!s.equals(mapped)) {
            UnicodeSet x = mapping.get(mapped);
            if (x == null) mapping.put(mapped, x= new UnicodeSet());
            x.add(i);
        }
    }

    static StringBuffer inbuffer = new StringBuffer();
    static StringBuffer intermediate = new StringBuffer();
    static StringBuffer outbuffer = new StringBuffer();
    static StringPrep namePrep = StringPrep.getInstance(StringPrep.RFC3491_NAMEPREP);

    enum IdnaStatus {PVALID, CONTEXTO, CONTEXTJ, REMAP, UNASSIGNED, DISALLOWED};

    IdnaStatus[] temp = new IdnaStatus[1];

    private IdnaStatus getIdnaStatus(String s) {
        getIDNAValue(s, StringPrep.ALLOW_UNASSIGNED, temp);
        return temp[0];
    }

    static public String getIDNAValue(String s, int namePrepOptions, IdnaStatus[] status) {
        if (status != null) {
            status[0] = IdnaStatus.PVALID;
        }
        if ("-".equals(s)) {
            return s;
        }
        String result;
        try {
            result = namePrep.prepare(s, namePrepOptions);
            if (status != null && !result.equals(s)) {
                status[0] = IdnaStatus.REMAP;
            }
        } catch (Exception e1) {
            if (status != null) {
                status[0] = IdnaStatus.DISALLOWED;
            }
            return null;
        }
        if (result.length() == 0) {
            return result;
        }
        try {
            IDNA.convertIDNToASCII(s, IDNA.USE_STD3_RULES + IDNA.ALLOW_UNASSIGNED); // just catch exception
        } catch (Exception e1) {
            if (status != null) {
                status[0] = IdnaStatus.DISALLOWED;
            }
            return null;
        }
        try {
            IDNA.convertIDNToASCII(s, IDNA.USE_STD3_RULES); // just catch exception
        } catch (Exception e1) {
            if (status != null) {
                status[0] = IdnaStatus.UNASSIGNED;
            }
            return null;
        }
        return result;
    }

    static UnicodeSet DT_WIDE_OR_NARROW = new UnicodeSet("[[:dt=wide:][:dt=narrow:]]").freeze();

    private static String frequencyFile;

    static public String getIDNA2008Value(String original, boolean lowerCase) {
        String source = original;
        for (int j = 0; j < 2; ++j) {
            String lower = lowerCase ? UCharacter.toLowerCase(ULocale.ROOT, source) : UCharacter.foldCase(source, true);
            //            String caseFoldNfc = Normalizer.normalize(caseFold, Normalizer.NFC);
            //            String lowerNfc = Normalizer.normalize(lower, Normalizer.NFC);
            //            if (!caseFoldNfc.equals(lowerNfc)) {
            //                System.out.println("fold difference:\t" + getCodeAndName(original)
            //                        + "\t" + getCodeAndName(lowerNfc)
            //                        + "\t" + getCodeAndName(caseFoldNfc));
            //            }
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lower.length(); ++i) {
                int cp = lower.codePointAt(i);
                int count = Character.charCount(cp); // 1 or 2 code units
                if (DT_WIDE_OR_NARROW.contains(cp)) {
                    String nfkc = Normalizer.normalize(cp, Normalizer.NFKC);
                    result.append(nfkc);
                } else {
                    result.appendCodePoint(cp);
                }
                i += count - 1; // offset if supplemental
            }
            String mapped = result.toString();
            if (source.equals(mapped)) { // idempotent result?
                return Normalizer.normalize(mapped, Normalizer.NFC);
            }
            if (j != 0) {
                System.out.println("Had to map more than once:\t" + getCodeAndName(original));
            }
            source = mapped;
        }
        throw new IllegalArgumentException("Can't get idempotence");
    }
}
