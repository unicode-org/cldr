package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

/**
 * This tool is manually run to generate *part* of the ar.xml
 * collation tailorings: the mappings from presentation forms to
 * identical and tertiary equivalents of the normal forms.
 *
 * To generate ar.xml, it is used with default options.
 *
 * By Steven R. Loomis (srl) thx Markus Scherer
 *
 */
@CLDRTool(alias = "generatedecompcollrules",
    description = "based on decomposition, generate identical/tertiary collation rules. Used to generate collation/ar.xml.",
    hidden = "Run manually to generate collation/ar.xml - not general purpose.")
public class GenerateDecompCollationRules {

    private static final char SINGLEQUOTE = '\'';

    private final static UnicodeSet isWord = new UnicodeSet("[\\uFDF0-\\uFDFF]");

    private final static String RESET = "\u200E&";
    private final static String IDENTICAL = "\u200E=";
    private final static String TERTIARY = "\u200E<<<";
    private final static String COMMENT = "# ";
    private final static String NL = "\n";

    private static final Options myOptions = new Options(GenerateDecompCollationRules.class);

    enum MyOptions {
        unicodeset(".*", "[[:dt=init:][:dt=med:][:dt=fin:][:dt=iso:]]", "UnicodeSet of input chars"), verbose(null, null, "verbose debugging messages");

        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    final static Transliterator hex = Transliterator.getInstance("any-hex");
    final static Transliterator hexForComment = Transliterator.getInstance("[^ ] any-hex");
    final static Transliterator name = Transliterator.getInstance("any-name");
    final static Transliterator escapeRules = Transliterator.getInstance("nfc;[[:Mn:]] any-hex");

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.verbose, args, true);
        final boolean verbose = myOptions.get(MyOptions.verbose).doesOccur();
        final CLDRConfig cldrConfig = CLDRConfig.getInstance();
        final Normalizer2 nfkd = Normalizer2.getNFKDInstance();
        final Normalizer2 nfc = Normalizer2.getNFCInstance();

        if (false) {
            final String astr = "\uFE70";
            final String astr_nfkd = nfkd.normalize(astr);
            final String astr_nfkd_nfc = nfc.normalize(astr_nfkd);
            System.out.println("'" + astr + "'=" + hex.transform(astr) + ", NFKD: '" + astr_nfkd + "'=" + hex.transform(astr_nfkd));
            System.out.println(" NFC: '" + astr_nfkd_nfc + "'=" + hex.transform(astr_nfkd_nfc));
            System.out.println(" escapeRules(astr): '" + escapeRules.transform(astr));
            System.out.println(" escapeRules(astr_nfkd): '" + escapeRules.transform(astr_nfkd));
        }

        UnicodeSet uSet;
        Option uSetOption = myOptions.get(MyOptions.unicodeset);
        final String uSetRules = uSetOption.doesOccur() ? uSetOption.getValue() : uSetOption.getDefaultArgument();
        System.out.println("UnicodeSet rules: " + uSetRules);
        try {
            uSet = new UnicodeSet(uSetRules);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Failed to construct UnicodeSet from \"" + uSetRules + "\" - see http://unicode.org/cldr/utility/list-unicodeset.jsp");
            return;
        }
        System.out.println("UnicodeSet size: " + uSet.size());

        final Relation<String, String> reg2pres = new Relation(new TreeMap<String, Set<String>>(), TreeSet.class);

        for (final String presForm : uSet) {
            final String regForm = nfkd.normalize(presForm).trim();
            if (verbose) System.out.println("# >" + presForm + "< = " + hex.transliterate(presForm) + "... ->" +
                regForm + "=" + hex.transliterate(regForm));
            if (regForm.length() > 31 || presForm.length() > 31) {
                System.out.println("!! Skipping, TOO LONG: " + presForm + " -> " + regForm);
            } else {
                reg2pres.put(regForm, presForm);
            }
        }
        System.out.println("Relation size: " + reg2pres.size());

        StringBuilder rules = new StringBuilder();

        rules.append(COMMENT)
            .append("Generated by " + GenerateDecompCollationRules.class.getSimpleName() + NL +
                COMMENT + "ICU v" + VersionInfo.ICU_VERSION + ", Unicode v" +
                UCharacter.getUnicodeVersion() + NL +
                COMMENT + "from rules " + uSetRules + NL + COMMENT + NL);

        for (final String regForm : reg2pres.keySet()) {
            final Set<String> presForms = reg2pres.get(regForm);

            final String relation = (presForms.size() == 1) &&
                isWord.containsAll(presForms.iterator().next()) ? TERTIARY : // only pres form is a word.
                    IDENTICAL; // all other cases.

            // COMMENT
            rules.append(COMMENT)
                .append(RESET)
                .append(hexForComment.transliterate(regForm));

            for (final String presForm : presForms) {
                rules.append(relation)
                    .append(hexForComment.transliterate(presForm));
            }
            rules.append(NL);

            // ACTUAL RULE
            rules.append(RESET)
                .append(toRule(regForm));

            for (final String presForm : presForms) {
                rules.append(relation)
                    .append(toRule(presForm));
            }
            rules.append(NL);
        }

        if (verbose) {
            System.out.println(rules);
        }

        // now, generate the output file
        XPathParts xpp = new XPathParts()
            .addElements(LDMLConstants.LDML,
                LDMLConstants.COLLATIONS,
                LDMLConstants.COLLATION,
                "cr");
        // The following crashes. Bug #XXXX
        //xpp.setAttribute(-1, LDMLConstants.COLLATION, LDMLConstants.STANDARD);
        SimpleXMLSource xmlSource = new SimpleXMLSource("ar");
        CLDRFile newFile = new CLDRFile(xmlSource);
        newFile.add(xpp.toString(), "xyzzy");
        newFile.addComment(xpp.toString(), "Generated by " + GenerateDecompCollationRules.class.getSimpleName() + " " + new java.util.Date() + "\n" +
            "from rules " + uSetRules + "\n", CommentType.PREBLOCK);
        final String filename = newFile.getLocaleID() + ".xml";
        StringWriter sw = new StringWriter();
        newFile.write(new PrintWriter(sw));
        sw.close();
        try (PrintWriter w = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, filename)) {
            w.print(sw.toString().replace("xyzzy",
                "<![CDATA[\n" +
                    rules.toString().replaceAll("\\\\u0020", "\\\\\\\\u0020") +
                    "\n" + "]]>"));
            //newFile.write(w);
            System.out.println("Wrote to " + CLDRPaths.GEN_DIRECTORY + "/" + filename);
        }

    }

    /**
     * convert a rule to the right form for escaping.
     * @param rule
     * @return
     */
    private static String toRule(String rule) {
        final String asHex = escapeRules.transform(rule);
        // quote any strings with spaces
        if (asHex.contains(" ")) {
            final StringBuilder sb = new StringBuilder(rule.length());
            sb.append(SINGLEQUOTE)
                .append(asHex)
                .append(SINGLEQUOTE);
            return sb.toString();
        } else {
            return asHex;
        }
    }
}
