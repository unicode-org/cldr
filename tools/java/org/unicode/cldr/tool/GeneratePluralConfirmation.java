package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.util.ULocale;

public class GeneratePluralConfirmation {
    private static final CLDRConfig testInfo = ToolConfig.getToolInstance();

    private static final StandardCodes STANDARD_CODES = testInfo.getStandardCodes();

    private static final SupplementalDataInfo SUPPLEMENTAL = testInfo.getSupplementalDataInfo();

    private static final PluralRulesFactory prf = PluralRulesFactory.getInstance(SUPPLEMENTAL);

    public static void main(String[] args) {
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

            ULocale ulocale = new ULocale(locale);

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
                values.ulocale = ulocale;
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
        ULocale ulocale;
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
                    String pattern = prf.getSamplePattern(ulocale, type.standardType, count);
                    buffer.append(ulocale + "\t" + type + "\t" + count + "\t" + fd + "\t«" + pattern.replace("{0}", String.valueOf(fd)) + "»\n");
                }
                buffer.append("\n");
            }
            return buffer.toString();
        }
    }
}
