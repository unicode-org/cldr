package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LsrvCanonicalizer;
import org.unicode.cldr.util.LsrvCanonicalizer.ReplacementRule;
import org.unicode.cldr.util.LsrvCanonicalizer.TestDataTypes;
import org.unicode.cldr.util.LsrvCanonicalizer.XLanguageTag;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R2;

/**
 * TestLsrvCanonicalizer is used to verify the correctness of the specification algorithm,
 * sanity-check the supplementalMetadata.xml alias data,
 * and generate test files for use by implementations.
 */
public class TestLsrvCanonicalizer extends TestFmwk {

    static final LsrvCanonicalizer rrs = LsrvCanonicalizer.getInstance();
    private static final boolean DEBUG = false;

    public static void main(String[] args) {
        final TestLsrvCanonicalizer testLocaleCanonicalizer = new TestLsrvCanonicalizer();
        testLocaleCanonicalizer.run(args);
    }

    public void testAgainstData() {
        if (DEBUG) {
            System.out.println(Joiner.on('\n').join(rrs.filter(LstrType.variant, null)));
            System.out.println(Joiner.on('\n').join(rrs.filter(LstrType.language, "no")));
        }

        List<ReplacementRule> rules = new ArrayList<>();
        for (Entry<TestDataTypes, Map<String, String>> mainEntry : rrs.getTestData(null).entrySet()) {
            TestDataTypes type = mainEntry.getKey();
            for (Entry<String, String> entry : mainEntry.getValue().entrySet()) {
                String toTest = entry.getKey();
                String expected = entry.getValue();
                final XLanguageTag source2 = XLanguageTag.fromTag(LstrType.language, toTest);
                XLanguageTag newTag = rrs.canonicalizeToX(source2, rules);
                String actual = newTag.toLocaleString();
                if (DEBUG && rules.size() > 1) {
                    System.out.println(
                        "source: " + toTest
                        + ", expected: " + expected
                        + ", actual: " + actual
                        + ", rules: " + rules
                        );
                }
                if (!Objects.equal(expected, actual)) {
                    errln("Error: "
                        + "source: " + toTest
                        + ", expected: " + expected
                        + ", actual: " + actual
                        + ", rules: " + rules
                        );
                }
            }
        }
    }

    public void TestAgainstLanguageSubtagRegistry() {
        Map<String,String> exceptions = ImmutableMap.<String, String>builder()
            .put("drh", "mn") // Darkhat => Halh Mongolian
            .put("drw", "fa_af") // Darwazi => Dari
            .put("tnf", "fa_af") // Tangshewi => Dari
            .put("AN", "CW SX BQ") // Netherlands Antilles => list
            .put("CS", "RS ME") // Serbia and Montenegro => list
            .put("NT", "SA IQ") // Neutral Zone => ???
            .put("SU", "RU AM AZ BY EE GE KZ KG LV LT MD TJ TM UA UZ") // Union of Soviet Socialist Republics => list
            .put("YU", "RS ME") // Yugoslavia => list
            .put("cel-gaulish", "xtg")
            .put("i-enochian", "und_x_i_enochian")
            .put("zh-guoyu", "zh") // Mandarin Chinese
            .put("zh-min", "nan_x_zh_min") // special code
            .put("sgn-NO", "nsi") // Norwegian Sign Language
            .put("zh-cmn", "zh") // Chinese
            .put("zh-cmn-Hans", "zh_Hans") // Simplified Chinese
            .put("zh-cmn-Hant", "zh_Hant") // Traditional Chinese
            .put("agp", "apf") // Paranan => ???
            .put("ais", "ami") // Nataoran Amis => ???
            .put("baz", "nvo") // Tunen => ???
            .put("bhk", "fbl") // Albay Bicolano => ???
            .put("bjq", "bzc") // Southern Betsimisaraka Malagasy => ???
            .put("bkb", "ebk") // Finallig => ???
            .put("btb", "beb") // Beti (Cameroon) => ???
            .put("daf", "dnj") // Dan => ???
            .put("dap", "njz") // Nisi (India) => ???
            .put("djl", "dze") // Djiwarli => ???
            .put("dkl", "aqd") // Kolum So Dogon => ???
            .put("dud", "uth") // Hun-Saare => ???
            .put("duj", "dwu") // Dhuwal => ???
            .put("dwl", "dbt") // Walo Kumbe Dogon => ???
            .put("elp", "amq") // Elpaputih => ???
            .put("gbc", "wny") // Garawa => ???
            .put("ggo", "esg") // Southern Gondi => ???
            .put("ggr", "gtu") // Aghu Tharnggalu => ???
            .put("gio", "aou") // Gelao => ???
            .put("ill", "ilm") // Iranun => ???
            .put("izi", "eza") // Izi-Ezaa-Ikwo-Mgbo => ???
            .put("jar", "jgk") // Jarawa (Nigeria) => ???
            .put("kdv", "zkd") // Kado => ???
            .put("kgd", "ncq") // Kataang => ???
            .put("kpp", "jkm") // Paku Karen => ???
            .put("kzh", "dgl") // Kenuzi-Dongola => ???
            .put("leg", "enl") // Lengua => ???
            .put("mgx", "jbk") // Omati => ???
            .put("mnt", "wnn") // Maykulan => ???
            .put("mof", "xnt") // Mohegan-Montauk-Narragansett => ???
            .put("mwd", "dmw") // Mudbura => ???
            .put("nbf", "nru") // Naxi => ???
            .put("nbx", "ekc") // Ngura => ???
            .put("nln", "azd") // Durango Nahuatl => ???
            .put("nlr", "nrk") // Ngarla => ???
            .put("noo", "dtd") // Nootka => ???
            .put("rmr", "emx") // Caló => ???
            .put("sap", "aqt") // Sanapaná => ???
            .put("sgl", "isk") // Sanglechi-Ishkashimi => ???
            .put("sul", "sgd") // Surigaonon => ???
            .put("sum", "ulw") // Sumo-Mayangna => ???
            .put("tgg", "bjp") // Tangga => ???
            .put("tid", "itd") // Tidong => ???
            .put("unp", "wro") // Worora => ???
            .put("wgw", "wgb") // Wagawaga => ???
            .put("wit", "nol") // Wintu => ???
            .put("wiw", "nwo") // Wirangu => ???
            .put("yen", "ynq") // Yendang => ???
            .put("yiy", "yrm") // Yir Yoront => ???
            .build();
        Set<String> SKIP = ImmutableSet.of(
            // handled via languageAliases
            "arevela", "arevmda",
            // newly added
            "bmy", "btl", "bxx", "byy", "cbe", "cbh", "cum", "dha", "dzd", "emo", "iap", "ime", "kbf", "kox", "lba", "lsg", "mhh", "mja", "mld", "mwx",
            "mwy", "myi", "myq", "ome", "pbz", "pgy", "pod", "prb", "puk", "rie", "rna", "rsi", "sgo", "snh", "svr", "toe", "xbx", "xip", "yds", "ynh", "yri"
            );

//        Error: (TestLsrvCanonicalizer.java:110) : drh: expected "khk", got "mn"
//        Error: (TestLsrvCanonicalizer.java:110) : drw: expected "prs", got "fa_af"
//        Error: (TestLsrvCanonicalizer.java:110) : tnf: expected "prs", got "fa_af"
//        Error: (TestLsrvCanonicalizer.java:110) : AN: expected null, got "CW SX BQ"
//        Error: (TestLsrvCanonicalizer.java:110) : CS: expected null, got "RS ME"
//        Error: (TestLsrvCanonicalizer.java:110) : NT: expected null, got "SA IQ"
//        Error: (TestLsrvCanonicalizer.java:110) : SU: expected null, got "RU AM AZ BY EE GE KZ KG LV LT MD TJ TM UA UZ"
//        Error: (TestLsrvCanonicalizer.java:110) : YU: expected null, got "RS ME"
//        Error: (TestLsrvCanonicalizer.java:110) : cel_gaulish: expected null, got "xtg"
//        Error: (TestLsrvCanonicalizer.java:110) : i_enochian: expected null, got "und_x_i_enochian"
//        Error: (TestLsrvCanonicalizer.java:110) : zh_guoyu: expected "cmn", got "zh"
//        Error: (TestLsrvCanonicalizer.java:110) : zh_min: expected null, got "nan_x_zh_min"
//        Error: (TestLsrvCanonicalizer.java:110) : sgn_NO: expected "nsl", got "nsi"
//        Error: (TestLsrvCanonicalizer.java:119) Can't access aliasInfo for the following. Suggested additions are:

        SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = SDI.getLocaleAliasInfo();
        Set<String> shouldHave = new LinkedHashSet<>();
        Set<String> shouldSkip = new LinkedHashSet<>();
        Set<String> addExceptions = new LinkedHashSet<>();

        for ( Entry<LstrType, Map<String, Map<LstrField, String>>> entry1 : StandardCodes.getEnumLstreg().entrySet()) {
            LstrType type = entry1.getKey();
            if (type == LstrType.extlang) {
                continue;
            }
            final String typeCompat = type.toCompatString();
            Map<String, R2<List<String>, String>> aliasInfo2 = aliasInfo.get(typeCompat);
            if (aliasInfo2 == null) {
                errln("Can't access aliasInfo for: " + type);
                continue;
            }

            for (Entry<String, Map<LstrField, String>> entry2 : entry1.getValue().entrySet()) {
                String subtag = entry2.getKey();
                final Map<LstrField, String> subtagInfo = entry2.getValue();
                String deprecated = subtagInfo.get(LstrField.Deprecated);
                if (deprecated == null) {
                    continue;
                }
                String preferredValue = subtagInfo.get(LstrField.Preferred_Value);
                final String preferredValueCompat = preferredValue == null ? null : preferredValue.replace('-', '_');
                final String subtagCompat = subtag.replace('-', '_');
                R2<List<String>, String> aliasInfo3 = aliasInfo2.get(subtagCompat);
                if (aliasInfo3 == null) {
                    if (SKIP.contains(subtag)) {
                        continue;
                    }
                    String possibleReplacement = preferredValueCompat != null ? preferredValueCompat : subtagInfo.get(LstrField.Comments);
                    // <languageAlias type="sgn_BR" replacement="bzs" reason="deprecated"/>
                    if (possibleReplacement != null) {
                        shouldHave.add("<" + typeCompat + "Alias"
                            + " type=\"" + subtagCompat + "\""
                            + (possibleReplacement == null || possibleReplacement.isEmpty() ? "" : " replacement=\"" + possibleReplacement + "\"")
                            + " reason=\"" + (type == LstrType.legacy || type == LstrType.redundant ? type.toString() : "deprecated") + "\""
                            + "/>  <!-- " + subtagInfo.get(LstrField.Description) + " -->");
                    } else {
                        shouldSkip.add(subtag);
                    }
                    continue;
                }
                List<String> replacement = aliasInfo3.get0();
                String reason = aliasInfo3.get1();
                final String replacementString = Joiner.on(' ').join(replacement);
                if (!Objects.equal(preferredValueCompat, replacementString)) {
                    String exception = exceptions.get(subtag);
                    if (Objects.equal(exception, replacementString)) {
                        continue;
                    }
                    CLDRFile english = CLDRConfig.getInstance().getEnglish();
                    String typeName = english.getName(typeCompat, subtag);
                    String replacementName = preferredValueCompat == null ? "???" : english.getName(typeCompat, replacementString);
                    addExceptions.add(".put(\"" + subtag + "\", \"" + replacementString + "\")" + " // " + typeName + " => " + replacementName);
                }

//                for (Entry<LstrField, String> entry3 : entry2.getValue().entrySet()) {
//                    LstrField field = entry3.getKey();
//                    String data = entry3.getValue();
//                }
            }
        }
        if (!addExceptions.isEmpty()) {
            errln("The following have different replacements.\n"
                + "    Here are suggested exception values to add to the test code, but research each one:");
            System.out.println(Joiner.on('\n').join(addExceptions));
            System.out.println();
        }

        if (!shouldHave.isEmpty()) {
            errln("Can't access aliasInfo for the following.\n"
                + "    Here are suggested additions to supplementalMetadata , but check them:");
            System.out.println(Joiner.on('\n').join(shouldHave));
            System.out.println();
        }
        if (!shouldSkip.isEmpty()) {
            errln("No replacement.\n"
                + "    Here are suggested additions to SKIP in the test code, but check them:");
            System.out.println("\"" + Joiner.on("\", \"").join(shouldSkip) + "\"");
            System.out.println();
        }
    }
}