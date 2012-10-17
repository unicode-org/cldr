package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.PatternFixer.Target;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class RegexTransformBuilder {
    static final boolean DEBUG = false;
    private static final boolean SKIP_BAD = true;

    // initially just very rough rule parser, for proof-of-concept
    public static StringTransform createFromRules(String string) {
        List<StringTransform> compound = new ArrayList<StringTransform>();

        List<Rule> rules = new ArrayList<Rule>();
        String[] ruleSet = string.split(";");
        Matcher m = RULE_PATTERN.matcher("");
        List<String> results = new ArrayList<String>();
        Matcher variable = VARIABLE.matcher("");
        UnicodeSet filter = null;

        if (DEBUG) System.out.println();

        for (String ruleString : ruleSet) {
            ruleString = ruleString.trim();
            if (DEBUG) System.out.print(ruleString + "\t=>\t");

            if (ruleString.startsWith("::")) {
                if (rules.size() != 0) {
                    compound.add(new RegexTransform(rules));
                    rules.clear();
                }
                final String body = ruleString.substring(2).trim();
                if (body.equalsIgnoreCase("NULL")) {
                    // nothing
                    if (DEBUG) System.out.println();
                } else if (UnicodeSet.resemblesPattern(body, 0)) {
                    filter = new UnicodeSet(body);
                    if (DEBUG) System.out.println(":: " + filter + " ;");
                } else {
                    // if we didn't find a filter, it is a Transliterator
                    final Transliterator translit = Transliterator.getInstance(body.trim());
                    compound.add(translit);
                    if (DEBUG) System.out.println(":: " + translit + " ;");
                }
                continue;
            }
            if (!m.reset(ruleString).matches()) {
                if (SKIP_BAD) {
                    System.out.println("BAD RULE");
                    continue;
                } else {
                    throw new IllegalArgumentException("Bad rule: {" + Utility.escape(ruleString) + "} ;");
                }
            }

            String pre = m.group(1);
            if (pre == null) {
                pre = "";
            } else {
                pre = fix(pre);
            }

            String main = fix(m.group(2));
            if (m.group(3) != null) {
                main += "(?=" + fix(m.group(3)) + ")";
            }

            results.clear();
            String result = m.group(4).trim();
            variable.reset(result);
            int last = 0;
            while (true) {
                if (!variable.find()) {
                    results.add(result.substring(last));
                    break;
                } else {
                    results.add(result.substring(last, variable.start()));
                    results.add(variable.group());
                    last = variable.end();
                }
            }
            try {
                Rule rule = new Rule(pre, main, results);
                if (DEBUG) System.out.println(rule);
                rules.add(rule);
            } catch (Exception e) {
                System.out.println("BAD:\t" + e.getMessage());
            }
        }

        // add any trailing rules
        if (rules.size() != 0) {
            compound.add(new RegexTransform(rules));
            rules.clear();
        }

        // generate final result
        StringTransform result = compound.size() == 1 ? compound.get(0) : new CompoundTransform(compound);
        if (filter != null) {
            return new UnicodeSetFilteredTransform(filter, result);
        }
        return result;
    }

    private static String fix(String pattern) {
        pattern = pattern.trim();
        // TODO fix pattern to not have anything but NFD in patterns
        PATTERN_FIXER.fix(pattern);
        pattern = Normalizer.decompose(pattern, false);
        // pre = pre.replace("[:", "\\p{");
        // pre = pre.replace(":]", "}");
        return pattern;
    }

    private static final PatternFixer PATTERN_FIXER = new PatternFixer(Target.JAVA);

    static Pattern RULE_PATTERN = Pattern.compile(
        "(?:([^{}>]*) \\{)?" +
            "([^}<>]*)" +
            "(?:\\} ([^<>]*))?" +
            "<?> (.*)",
        Pattern.COMMENTS);
    static Pattern VARIABLE = Pattern.compile(
        "\\$[0-9]",
        Pattern.COMMENTS);
}
