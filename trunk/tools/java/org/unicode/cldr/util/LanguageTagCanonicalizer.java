package org.unicode.cldr.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.StringTransform;

public class LanguageTagCanonicalizer implements StringTransform {
    static SupplementalDataInfo info = SupplementalDataInfo.getInstance();
    static Map<String, Map<String, R2<List<String>, String>>> aliases = info.getLocaleAliasInfo();

    // instance variables, since they can change.
    LanguageTagParser ltp1 = new LanguageTagParser();
    LanguageTagParser ltp2 = new LanguageTagParser();

    /**
     * Convert a locale (language tag) into the canonical form.
     */
    // TODO, handle variants
    public String transform(String locale) {
        ltp1.set(locale);
        copyFields(LanguageTagField.language);
        copyFields(LanguageTagField.script);
        copyFields(LanguageTagField.region);
        
        // special code for variants
        
        List<String> originalVariants = ltp1.getVariants();
        if (originalVariants.size() != 0) {
            Set<String> newVariants = new TreeSet<String>();
            for (String item : originalVariants) {
                String replacement = getReplacement(LanguageTagField.variant, item);
                if (replacement == null) {
                    newVariants.add(item);
                } else {
                    copyFields(LanguageTagField.variant, replacement);
                    List<String> otherVariants = ltp2.getVariants();
                    newVariants.addAll(otherVariants);
                }
            }
            ltp1.setVariants(newVariants);
        }
        return ltp1.toString();
    }

    enum LanguageTagField {
        language("language"), script("script"), region("territory"), variant("variant");

        private Map<String, R2<List<String>, String>> replacements;

        private LanguageTagField(String replacementName) {
            this.replacements = aliases.get(replacementName);
        }

        private String get(LanguageTagParser parser) {
            switch (this) {
            case language: return parser.getLanguage();
            case script: return parser.getScript();
            case region: return parser.getRegion();
            default: throw new UnsupportedOperationException();
            //case variant: return parser.getVariants();
            }
        }
    }

    private String getReplacement(LanguageTagField tagField) {
        return getReplacement(tagField, tagField.get(ltp1));
    }

    private String getReplacement(LanguageTagField tagField, String field) {
        String newField = null;
        R2<List<String>, String> otherLanguage = tagField.replacements.get(field);
        if (otherLanguage != null) {
            List<String> list = otherLanguage.get0();
            if (list.size() != 0) {
                newField = list.get(0);
            }
        }
        return newField;
    }

    /**
     * Copy fields from one language tag into another. 
     * @param otherField
     * @param mainField - for this field, force a copy. For other fields, only copy if target is empty
     */
    private void copyFields(LanguageTagField mainField) {
        String otherField = getReplacement(mainField);
        copyFields(mainField, otherField);
    }

    private void copyFields(LanguageTagField mainField, String otherField) {
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
}
