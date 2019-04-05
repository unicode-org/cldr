package org.unicode.cldr.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.LanguageTagParser.OutputOption;
import org.unicode.cldr.util.StandardCodes.LstrType;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.StringTransform;

public class LanguageTagCanonicalizer implements StringTransform {

    private static final SupplementalDataInfo info = SupplementalDataInfo.getInstance();
    private static final LikelySubtags LIKELY_FAVOR_SCRIPT = new LikelySubtags(info.getLikelySubtags());
    private static final LikelySubtags LIKELY_FAVOR_REGION = new LikelySubtags(info.getLikelySubtags()).setFavorRegion(true);
    private static final Map<String, Map<String, R2<List<String>, String>>> ALIASES = info.getLocaleAliasInfo();

    private final LikelySubtags likely;

    // instance variables, since they can change. The transform call is synchronized, however.

    private final LanguageTagParser ltp1 = new LanguageTagParser();
    private final LanguageTagParser ltp2 = new LanguageTagParser();

    public LanguageTagCanonicalizer() {
        this(LstrType.script);
    }

    public LanguageTagCanonicalizer(boolean favorRegion) {
        this(favorRegion ? LstrType.region : LstrType.script);
    }

    public LanguageTagCanonicalizer(LstrType lstrType) {
        switch(lstrType) {
        case region: likely = LIKELY_FAVOR_REGION; break;
        case script: likely = LIKELY_FAVOR_SCRIPT; break;
        default: likely = null;
        }
    }

    /**
     * Convert a locale (language tag) into the canonical form.
     * <br>Any invalid variant code is removed.
     * <br>TODO: map invalid language tags to <unknown>, eg ZZ; drop invalid U or T extensions, convert ICU locale extensions to BCP47
     */
    // TODO, handle variants
    public synchronized String transform(String locale) {
        ltp1.set(locale);

        copyFields2(LanguageTagField.language, getReplacement(LanguageTagField.language, LanguageTagField.language.get(ltp1), locale));
        copyFields2(LanguageTagField.script, getReplacement(LanguageTagField.script, LanguageTagField.script.get(ltp1), locale));
        copyFields2(LanguageTagField.region, getReplacement(LanguageTagField.region, LanguageTagField.region.get(ltp1), locale));

        // special code for variants

        List<String> originalVariants = ltp1.getVariants();
        if (originalVariants.size() != 0) {
            Set<String> newVariants = new TreeSet<String>();
            for (String item : originalVariants) {
                String replacement = getReplacement(LanguageTagField.variant, item, locale);
                if (replacement == null) {
                    newVariants.add(item);
                } else {
                    copyFields2(LanguageTagField.variant, replacement);
                    List<String> otherVariants = ltp2.getVariants();
                    newVariants.addAll(otherVariants);
                }
            }
            ltp1.setVariants(newVariants);
        }
        final String result = ltp1.toString();
        if ("und".equals(ltp1.getLanguage())) return result;
        if (likely == null) {
            return result;
        }
        // TODO: make more efficient, since likely will do the parsing again.
        String likelyMin = likely.minimize(result);
        return likelyMin == null ? result : likelyMin;
    }

    private enum LanguageTagField {
        language("language"), script("script"), region("territory"), variant("variant");

        private final Map<String, R2<List<String>, String>> replacements;

        private LanguageTagField(String replacementName) {
            this.replacements = ALIASES.get(replacementName);
        }

        private String get(LanguageTagParser parser) {
            switch (this) {
            case language:
                return parser.getLanguage();
            case script:
                return parser.getScript();
            case region:
                return parser.getRegion();
            default:
                throw new UnsupportedOperationException();
                // case variant: return parser.getVariants();
            }
        }

        /**
         * Get the replacements, or the empty list if there are none.
         */
        private List<String> getReplacements(String field) {
            final R2<List<String>, String> data = replacements.get(this == variant ? field.toUpperCase(Locale.ROOT) : field);
            return data == null ? Collections.emptyList() : data.get0();
        }
    }

    /**
     * Return the replacement value for the tagField (eg territory) in the field (eg "en") in languageTag.
     * It special-cases cases where regions split, using likely subtags to get the best answer.
     */
    private String getReplacement(LanguageTagField tagField, String field, String languageTag) {
        String newField = null;
        List<String> list = tagField.getReplacements(field);
        if (list.size() != 0) {
            newField = list.get(0);
            // special case region, where there are multiple values
            // we look for the most likely region given the language, if there is one.
            if (list.size() > 1 && tagField == LanguageTagField.region) {
                LanguageTagParser x = new LanguageTagParser().set(languageTag).setRegion("");
                String max = LIKELY_FAVOR_REGION.maximize(x.toString());
                String region = x.set(max).getRegion();
                if (list.contains(region)) {
                    newField = region;
                }
            }
        }
        return newField;
    }

    /**
     * Copy fields from one language tag into another.
     *
     * @param otherField
     * @param mainField
     *            - for this field, force a copy. For other fields, only copy if target is empty
     */
    private void copyFields2(LanguageTagField mainField, String otherField) {
        if (otherField == null) {
            return;
        }
        // Note: could be optimized to only parts if there is an "_" in the replacement.
        ltp2.set(mainField == LanguageTagField.language ? otherField : "und-" + otherField);
        if (mainField == LanguageTagField.language || ltp1.getLanguage().length() == 0) {
            ltp1.setLanguage(ltp2.getLanguage());
        }
        if (mainField == LanguageTagField.script || ltp1.getScript().length() == 0) {
            ltp1.setScript(ltp2.getScript());
        }
        if (mainField == LanguageTagField.region || ltp1.getRegion().length() == 0) {
            ltp1.setRegion(ltp2.getRegion());
        }
    }

    public void transform(LanguageTagParser ltp1) {
        String locale = ltp1.toString(OutputOption.BCP47);
        copyFields2(LanguageTagField.language, getReplacement(LanguageTagField.language, LanguageTagField.language.get(ltp1), locale));
        copyFields2(LanguageTagField.script, getReplacement(LanguageTagField.script, LanguageTagField.script.get(ltp1), locale));
        copyFields2(LanguageTagField.region, getReplacement(LanguageTagField.region, LanguageTagField.region.get(ltp1), locale));

        // special code for variants

        List<String> originalVariants = ltp1.getVariants();
        if (originalVariants.size() != 0) {
            Set<String> newVariants = new TreeSet<String>();
            for (String item : originalVariants) {
                String replacement = getReplacement(LanguageTagField.variant, item, locale);
                if (replacement == null) {
                    newVariants.add(item);
                } else {
                    copyFields2(LanguageTagField.variant, replacement);
                    List<String> otherVariants = ltp2.getVariants();
                    newVariants.addAll(otherVariants);
                }
            }
            ltp1.setVariants(newVariants);
        }        
    }
}
