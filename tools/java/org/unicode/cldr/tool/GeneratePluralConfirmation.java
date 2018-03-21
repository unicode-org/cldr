package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;

public class GeneratePluralConfirmation {
    private static final com.ibm.icu.text.PluralRules.PluralType ICU_ORDINAL = com.ibm.icu.text.PluralRules.PluralType.ORDINAL;

    private static final CLDRConfig testInfo = ToolConfig.getToolInstance();

    private static final StandardCodes STANDARD_CODES = testInfo.getStandardCodes();

    private static final SupplementalDataInfo SUPPLEMENTAL = testInfo.getSupplementalDataInfo();

    private static final PluralRulesFactory prf = PluralRulesFactory.getInstance(SUPPLEMENTAL);

    public static void main(String[] args) {
        for (String uLocale : new TreeSet<>(prf.getLocales())) {
            for (PluralRules.PluralType type : PluralRules.PluralType.values()) {
                for (Count count : Count.values()) {
                    String pattern = prf.getSamplePattern(uLocale, type, count);
                    if (pattern.contains("{no pattern available}")) {
                        continue;
                    }
                    System.out.println("locale="
                        + uLocale
                        + "; action=add ; new_path="
                        + "//ldml/numbers/minimalPairs/"
                        + (type == PluralRules.PluralType.CARDINAL ? "plural" : "ordinal")
                        + "MinimalPairs[@"
                        + (type == PluralRules.PluralType.CARDINAL ? "count" : "ordinal")
                        + "=\"" + count.toString().toLowerCase(Locale.ENGLISH) + "\"]"
                        + "; new_value="
                        + pattern);
                }
                System.out.println();
            }
        }
    }

    public static void mainOld2(String[] args) {
        Set<String> locales = STANDARD_CODES.getLocaleCoverageLocales(Organization.google);
        // SUPPLEMENTAL.getPluralLocales(PluralType.ordinal)
        LanguageTagParser ltp = new LanguageTagParser();
        for (String loc : locales) {
            ltp.set(loc);
            if (!ltp.getScript().isEmpty() || !ltp.getRegion().isEmpty()) {
                continue;
            }
            EnumSet<Count> counts = EnumSet.noneOf(Count.class);
            for (Count count : Count.VALUES) {
                String pat = prf.getSamplePattern(loc, ICU_ORDINAL, count);
                if (pat != null && !pat.contains("{no pattern available}")) {
                    counts.add(count);
                }
            }
            switch (counts.size()) {
            case 0:
                System.out.format("%s\t%s\t%s\t%s\n", loc, "missing", "n/a", "n/a");
                break;
            case 1: {
                String pat = prf.getSamplePattern(loc, ICU_ORDINAL, Count.other);
                System.out.format("%s\t%s\t%s\t%s\n", loc, "constant", Count.other, "n/a");
            }
                break;
            default:
                for (Count count : counts) {
                    String pat = prf.getSamplePattern(loc, ICU_ORDINAL, count);
                    System.out.format("%s\t%s\t%s\t%s\n", loc, "multiple", count, pat);
                }
                break;
            }
        }
    }

    public static void mainOld(String[] args) {
        Set<String> testLocales = new TreeSet(Arrays.asList(
            "az cy hy ka kk km ky lo mk mn my ne pa si sq uz eu my si sq vi zu"
                .split(" ")));
        // STANDARD_CODES.getLocaleCoverageLocales("google");
        System.out.println(testLocales);
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : testLocales) {
            // the only known case where plural rules depend on region or script is pt_PT
            if (locale.equals("root") || locale.equals("en_GB") || locale.equals("es_419") || locale.equals("*")) {
                continue;
            }
            //            if (!locale.equals("en")) {
            //                continue;
            //            }

            for (PluralType type : PluralType.values()) {
                // for now, just ordinals
                if (type == PluralType.cardinal) {
                    continue;
                }
                PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(type, locale, false);
                PluralRules rules;
                if (pluralInfo == null) {
                    rules = PluralRules.DEFAULT;
                } else {
                    rules = pluralInfo.getPluralRules();
                }
                Values values = new Values();
                values.locale = locale;
                values.type = type;

                for (int i = 0; i < 30; ++i) {
                    FixedDecimal fd = new FixedDecimal(i);
                    String keyword = rules.select(fd);
                    values.showValue(keyword, fd);
                }
                for (String keyword : rules.getKeywords()) {
                    FixedDecimalSamples samples;
                    samples = rules.getDecimalSamples(keyword, PluralRules.SampleType.DECIMAL);
                    values.showSamples(keyword, samples);
                    samples = rules.getDecimalSamples(keyword, PluralRules.SampleType.INTEGER);
                    values.showSamples(keyword, samples);
                }
                System.out.println(values);
            }
        }
    }

    static class Values {
        String locale;
        PluralType type;
        Relation<Count, FixedDecimal> soFar = Relation.of(new EnumMap(Count.class), TreeSet.class);
        Map<FixedDecimal, String> sorted = new TreeMap();

        private void showValue(String keyword, FixedDecimal fd) {
            Set<FixedDecimal> soFarSet = soFar.getAll(keyword);
            if (soFarSet != null && soFarSet.contains(fd)) {
                return;
            }
            soFar.put(Count.valueOf(keyword), fd);
            sorted.put(fd, keyword);
        }

        public void showSamples(String keyword, FixedDecimalSamples samples) {
            if (samples == null) {
                return;
            }
            for (FixedDecimalRange range : samples.getSamples()) {
                Set<FixedDecimal> soFarSet = soFar.getAll(keyword);
                if (soFarSet != null && soFarSet.size() > 10) {
                    break;
                }
                showValue(keyword, range.start);
                if (!range.end.equals(range.start)) {
                    soFarSet = soFar.getAll(keyword);
                    if (soFarSet != null && soFarSet.size() > 10) {
                        break;
                    }
                    showValue(keyword, range.end);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            for (Entry<Count, Set<FixedDecimal>> entry : soFar.keyValuesSet()) {
                Count count = entry.getKey();
                for (FixedDecimal fd : entry.getValue()) {
                    String pattern = prf.getSamplePattern(locale, type.standardType, count);
                    buffer.append(locale + "\t" + type + "\t" + count + "\t" + fd + "\t«" + pattern.replace("{0}", String.valueOf(fd)) + "»\n");
                }
                buffer.append("\n");
            }
            return buffer.toString();
        }
    }
}
