/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public abstract class PluralRulesFactory extends PluralRules.Factory {

    private final SupplementalDataInfo supplementalDataInfo;

    public abstract boolean hasOverride(ULocale locale);

    public enum Type {
        NORMAL, ALTERNATE
    };

    public static PluralRulesFactory getInstance(SupplementalDataInfo supplementalDataInfo) {
        return getInstance(supplementalDataInfo, Type.NORMAL);
    }

    private static ConcurrentHashMap<Pair<Type, String>, PluralRulesFactory> singletons = new ConcurrentHashMap<Pair<Type, String>, PluralRulesFactory>();

    public static PluralRulesFactory getInstance(SupplementalDataInfo supplementalDataInfo, Type type) {
        Pair<Type, String> key = new Pair<Type, String>(type, supplementalDataInfo.getDirectory().getAbsolutePath());
        PluralRulesFactory prf = singletons.get(key);
        if (prf == null) {
            switch (type) {
            case NORMAL:
                prf = new PluralRulesFactoryVanilla(supplementalDataInfo);
                break;
//            case ALTERNATE:
//                prf = new PluralRulesFactoryWithOverrides(supplementalDataInfo);
//                break;
            default:
                throw new InternalError("Illegal type value: " + type);
            }
            singletons.put(key, prf);
        }
        return prf;
    }

//    static final PluralRulesFactory NORMAL = new PluralRulesFactoryVanilla();
//    static final PluralRulesFactory ALTERNATE = new PluralRulesFactoryWithOverrides();

    private PluralRulesFactory(SupplementalDataInfo supplementalDataInfo) {
        this.supplementalDataInfo = supplementalDataInfo;
    }

    static class PluralRulesFactoryVanilla extends PluralRulesFactory {
        private PluralRulesFactoryVanilla(SupplementalDataInfo supplementalDataInfo) {
            super(supplementalDataInfo);
        }

        @Override
        public boolean hasOverride(ULocale locale) {
            return false;
        }

        @Override
        public PluralRules forLocale(ULocale locale, PluralType ordinal) {
            return PluralRules.forLocale(locale, ordinal);
        }

        @Override
        public ULocale[] getAvailableULocales() {
            return PluralRules.getAvailableULocales();
        }

        @Override
        public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
            return PluralRules.getFunctionalEquivalent(locale, isAvailable);
        }
    }

//    static class PluralRulesFactoryWithOverrides extends PluralRulesFactory {
//        private PluralRulesFactoryWithOverrides(SupplementalDataInfo supplementalDataInfo) {
//            super(supplementalDataInfo);
//        }
//
//        @Override
//        public boolean hasOverride(ULocale locale) {
//            return getPluralOverrides().containsKey(locale);
//        }
//
//        @Override
//        public PluralRules forLocale(ULocale locale, PluralType ordinal) {
//            PluralRules override = ordinal != PluralType.CARDINAL
//                ? null
//                    : getPluralOverrides().get(locale);
//            return override != null
//                ? override
//                    : PluralRules.forLocale(locale, ordinal);
//        }
//
//        @Override
//        public ULocale[] getAvailableULocales() {
//            return PluralRules.getAvailableULocales(); // TODO fix if we add more locales
//        }
//
//        static final Map<String, ULocale> rulesToULocale = new HashMap<String, ULocale>();
//
//        @Override
//        public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
//            if (rulesToULocale.isEmpty()) {
//                for (ULocale locale2 : getAvailableULocales()) {
//                    String rules = forLocale(locale2).toString();
//                    ULocale old = rulesToULocale.get(rules);
//                    if (old == null) {
//                        rulesToULocale.put(rules, locale2);
//                    }
//                }
//            }
//            String rules = forLocale(locale).toString();
//            ULocale result = rulesToULocale.get(rules);
//            return result == null ? ULocale.ROOT : result;
//        }
//    };

//    private Map<ULocale, PluralMinimalPairs> getLocaleToSamplePatterns() {
//        if (LOCALE_TO_SAMPLE_PATTERNS == null) {
//            loadData();
//        }
//        return LOCALE_TO_SAMPLE_PATTERNS;
//    }

    public Set<String> getLocales() {
        return supplementalDataInfo.getPluralLocales(SupplementalDataInfo.PluralType.cardinal);
    }

    public Set<Count> getSampleCounts(String locale, PluralType type) {
        PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(locale);
        return samplePatterns == null ? null : samplePatterns.getCounts(type);
    }

    public static String getSamplePattern(String uLocale, PluralType type, Count count) {
        PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(uLocale);
        if (samplePatterns != null) {
            String result = samplePatterns.get(type, count);
            if (result != null) {
                return result;
            }
        }
        return "{0} {no pattern available}";
    }

//    public Map<ULocale, PluralRules> getPluralOverrides() {
//        if (OVERRIDES == null) {
//            loadData();
//        }
//        return OVERRIDES;
//    }

    public Relation<ULocale, FixedDecimal> getExtraSamples() {
        if (EXTRA_SAMPLES == null) {
            loadData();
        }
        return EXTRA_SAMPLES;
    }

    //private Map<ULocale, PluralMinimalPairs> LOCALE_TO_SAMPLE_PATTERNS = null;
    //private Map<ULocale, PluralRules> OVERRIDES = null;
    private Relation<ULocale, FixedDecimal> EXTRA_SAMPLES = null;

    private void loadData() {
//        LinkedHashMap<ULocale, PluralMinimalPairs> temp = new LinkedHashMap<ULocale, PluralMinimalPairs>();
//        HashMap<ULocale, PluralRules> tempOverrides = new HashMap<ULocale, PluralRules>();
        Relation<ULocale, FixedDecimal> tempSamples = Relation.of(new HashMap<ULocale, Set<FixedDecimal>>(), HashSet.class);
//        Factory factory = CLDRConfig.getInstance().getFullCldrFactory();
//        for (String localeId : supplementalDataInfo.getPluralLocales()) {
//            ULocale ulocale = new ULocale(localeId);
//            PluralMinimalPairs samplePatterns = new PluralMinimalPairs();
//            CLDRFile cldrFile = factory.make(localeId, true);
//            for (Iterator<String> it = cldrFile.iterator("//ldml/numbers/minimalPairs/"); it.hasNext();) {
//                String path = it.next();
//                XPathParts parts = XPathParts.getFrozenInstance(path);
//                String sample = cldrFile.getStringValue(path);
//                String element = parts.getElement(-1);
//                PluralType type = "pluralMinimalPairs".equals(element) ? PluralType.CARDINAL 
//                    : "ordinalMinimalPairs".equals(element) ? PluralType.ORDINAL
//                        : null;
//                PluralInfo.Count category = PluralInfo.Count.valueOf(
//                    parts.getAttributeValue(-1, type == PluralType.CARDINAL ? "count" : "ordinal"));
//                if (category == null || type == null) {
//                    throw new IllegalArgumentException("Bad plural info");
//                }
//                samplePatterns.put(ulocale, type, category, sample);
//            }
//            samplePatterns.freeze();
//            temp.put(ulocale, samplePatterns);

        /*
         *      <minimalPairs>
        <pluralMinimalPairs count="one">{0} day</pluralMinimalPairs>
        <pluralMinimalPairs count="other">{0} days</pluralMinimalPairs>
        <ordinalMinimalPairs ordinal="few">Take the {0}rd right.</ordinalMinimalPairs>
        <ordinalMinimalPairs ordinal="one">Take the {0}st right.</ordinalMinimalPairs>
        <ordinalMinimalPairs ordinal="other">Take the {0}th right.</ordinalMinimalPairs>
        <ordinalMinimalPairs ordinal="two">Take the {0}nd right.</ordinalMinimalPairs>
        </minimalPairs>
        </numbers>
        
         */
//        }

//        for (String[] row : SAMPLE_PATTERNS) {
//            ULocale locale = new ULocale(row[0]);
//            String keyword = row[1];
//            String sample = row[2];
//            SamplePatterns samplePatterns = temp.get(locale);
//            if (samplePatterns == null) {
//                temp.put(locale, samplePatterns = new SamplePatterns());
//            }
//            //System.out.println("*Adding sample:\t" + locale + "\t" + keyword + "\t" + sample);
//            samplePatterns.put(locale, PluralType.CARDINAL, Count.valueOf(keyword), sample);
//        }
//        for (String[] row : ORDINAL_SAMPLES) {
//            ULocale locale = new ULocale(row[0]);
//            PluralInfo pluralInfo = supplementalDataInfo
//                .getPlurals(SupplementalDataInfo.PluralType.ordinal, row[0]);
//            if (pluralInfo == null) {
//                throw new IllegalArgumentException("Can't get plural info for " + row[0]);
//            }
//            Count count;
//            try {
//                int integerValue = Integer.parseInt(row[2]);
//                count = pluralInfo.getCount(integerValue);
//            } catch (NumberFormatException e) {
//                count = Count.valueOf(row[2]);
//            }
//
//            String sample = row[1];
//            SamplePatterns samplePatterns = temp.get(locale);
//            if (samplePatterns == null) {
//                temp.put(locale, samplePatterns = new SamplePatterns());
//            }
//            // { "af", "one", "{0} dag" },
//            samplePatterns.put(locale, PluralType.ORDINAL, count, sample);
//            //System.out.println("*Adding ordinal sample:\t" + locale + "\t" + count + "\t" + sample + "\t" + integerValue);
////            try {
////                samplePatterns.put(locale, PluralType.ORDINAL, count, sample);
////            } catch (Exception e) {
////                System.out.println("***" + e.getMessage());
////            }
//        }

//        for (String[] pair : overrides) {
//            for (String locale : pair[0].split("\\s*,\\s*")) {
//                ULocale uLocale = new ULocale(locale);
//                if (tempOverrides.containsKey(uLocale)) {
//                    throw new IllegalArgumentException("Duplicate locale: " + uLocale);
//                }
//                try {
//                    PluralRules rules = PluralRules.parseDescription(pair[1]);
//                    tempOverrides.put(uLocale, rules);
//                } catch (Exception e) {
//                    throw new IllegalArgumentException(locale + "\t" + pair[1], e);
//                }
//            }
//        }
        for (String[] pair : EXTRA_SAMPLE_SOURCE) {
            for (String locale : pair[0].split("\\s*,\\s*")) {
                ULocale uLocale = new ULocale(locale);
                if (tempSamples.containsKey(uLocale)) {
                    throw new IllegalArgumentException("Duplicate locale: " + uLocale);
                }
                for (String item : pair[1].split("\\s*,\\s*")) {
                    tempSamples.put(uLocale, new PluralRules.FixedDecimal(item));
                }
            }
        }
//        LOCALE_TO_SAMPLE_PATTERNS = Collections.unmodifiableMap(temp);
//        OVERRIDES = Collections.unmodifiableMap(tempOverrides);
        EXTRA_SAMPLES = (Relation<ULocale, FixedDecimal>) tempSamples.freeze();
    }

    //    static String[][] OLDRULES = {
    //        {"af", "one: n is 1"},
    //        {"am", "one: n in 0..1"},
    //        {"ar", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n mod 100 in 3..10;  many: n mod 100 in 11..99"},
    //        {"az", "other: null"},
    //        {"bg", "one: n is 1"},
    //        {"bn", "one: n is 1"},
    //        {"ca", "one: n is 1"},
    //        {"cs", "one: n is 1;  few: n in 2..4"},
    //        {"cy", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
    //        {"da", "one: n is 1"},
    //        {"de", "one: n is 1"},
    //        {"el", "one: n is 1"},
    //        {"en", "one: n is 1"},
    //        {"es", "one: n is 1"},
    //        {"et", "one: n is 1"},
    //        {"eu", "one: n is 1"},
    //        {"fa", "other: null"},
    //        {"fi", "one: n is 1"},
    //        {"fil", "one: n in 0..1"},
    //        {"fr", "one: n within 0..2 and n is not 2"},
    //        {"gl", "one: n is 1"},
    //        {"gu", "one: n is 1"},
    //        {"hi", "one: n in 0..1"},
    //        {"hr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
    //        {"hu", "other: null"},
    //        {"hy", "one: n is 1"},
    //        {"id", "other: null"},
    //        {"is", "one: n is 1"},
    //        {"it", "one: n is 1"},
    //        {"he", "one: n is 1;  two: n is 2;  many: n is not 0 and n mod 10 is 0"},
    //        {"ja", "other: null"},
    //        {"ka", "other: null"},
    //        {"kk", "one: n is 1"},
    //        {"km", "other: null"},
    //        {"kn", "other: null"},
    //        {"ko", "other: null"},
    //        {"ky", "one: n is 1"},
    //        {"lo", "other: null"},
    //        {"lt", "one: n mod 10 is 1 and n mod 100 not in 11..19;  few: n mod 10 in 2..9 and n mod 100 not in 11..19"},
    //        {"lv", "zero: n is 0;  one: n mod 10 is 1 and n mod 100 is not 11"},
    //        {"mk", "one: n mod 10 is 1 and n is not 11"},
    //        {"ml", "one: n is 1"},
    //        {"mn", "one: n is 1"},
    //        {"mr", "one: n is 1"},
    //        {"ms", "other: null"},
    //        {"my", "other: null"},
    //        {"ne", "one: n is 1"},
    //        {"nl", "one: n is 1"},
    //        {"nb", "one: n is 1"},
    //        {"pa", "one: n is 1"},
    //        {"pl", "one: n is 1;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n is not 1 and n mod 10 in 0..1 or n mod 10 in 5..9 or n mod 100 in 12..14"},
    //        {"ps", "one: n is 1"},
    //        {"pt", "one: n is 1"},
    //        {"ro", "one: n is 1;  few: n is 0 or n is not 1 and n mod 100 in 1..19"},
    //        {"ru", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
    //        {"si", "other: null"},
    //        {"sk", "one: n is 1;  few: n in 2..4"},
    //        {"sl", "one: n mod 100 is 1;  two: n mod 100 is 2;  few: n mod 100 in 3..4"},
    //        {"sq", "one: n is 1"},
    //        {"sr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
    //        {"sv", "one: n is 1"},
    //        {"sw", "one: n is 1"},
    //        {"ta", "one: n is 1"},
    //        {"te", "one: n is 1"},
    //        {"th", "other: null"},
    //        {"tr", "other: null"},
    //        {"uk", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
    //        {"ur", "one: n is 1"},
    //        {"uz", "other: null"},
    //        {"vi", "other: null"},
    //        {"zh", "other: null"},
    //        {"zu", "one: n is 1"},
    //    };

//    static String[][] SAMPLE_PATTERNS = {
//        { "und", "zero", "{0} ADD-SAMPLE-ZERO" },
//        { "und", "one", "{0} ADD-SAMPLE-ONE" },
//        { "und", "two", "{0} ADD-SAMPLE-TWO" },
//        { "und", "few", "{0} ADD-SAMPLE-FEW" },
//        { "und", "many", "{0} ADD-SAMPLE-MANY" },
//        { "und", "other", "{0} ADD-SAMPLE-OTHER" },
//        { "af", "one", "{0} dag" },
//        { "af", "other", "{0} dae" },
//        { "am", "one", "{0} ቀን" },
//        { "am", "other", "{0} ቀናት" }, // fixed to 'other'
//        { "ar", "few", "{0} أولاد حضروا" },
//        { "ar", "many", "{0} ولدًا حضروا" },
//        { "ar", "one", "ولد واحد حضر" },
//        { "ar", "other", "{0} ولد حضروا" },
//        { "ar", "two", "ولدان حضرا" },
//        { "ar", "zero", "{0} كتاب" },
//        { "az", "one", "Alış-veriş katınızda {0} X var. Almaq istəyirsiniz?" },
//        { "az", "other", "Alış-veriş kartınızda {0} X var. Almaq istəyirsiniz?" },
//        { "ast", "one", "{0} día" },
//        { "ast", "other", "{0} díes" },
//        { "be", "one", "з {0} кнігі за {0} дзень" },
//        { "be", "few", "з {0} кніг за {0} дні" },
//        { "be", "many", "з {0} кніг за {0} дзён" },
//        { "be", "other", "з {0} кніги за {0} дні" },
//        { "bg", "one", "{0} ден" },
//        { "bg", "other", "{0} дена" },
//        { "bn", "one", "সসে {0}টি আপেল খেল, সেটা ভাল" },
//        { "bn", "other", "সসে {0}টি আপেল খেল, সেগুলি ভাল" },
//        { "br", "few", "{0} deiz" },
//        { "br", "many", "{0} a zeizioù" },
//        { "br", "one", "{0} deiz" },
//        { "br", "other", "{0} deiz" },
//        { "br", "two", "{0} zeiz" },
//        { "bs", "few", "za {0} mjeseca" },
//        { "bs", "many", "za {0} mjeseci" },
//        { "bs", "one", "za {0} mjesec" },
//        { "bs", "other", "za {0} mjeseci" },
//        { "ca", "one", "{0} dia" },
//        { "ca", "other", "{0} dies" },
//        { "cs", "few", "{0} dny" },
//        { "cs", "one", "{0} den" },
//        { "cs", "other", "{0} dní" },
//        { "cs", "many", "{0} dne" }, // added from spreadsheet
//        { "cy", "zero", "{0} cŵn, {0} cathod" },
//        { "cy", "one", "{0} ci, {0} gath" },
//        { "cy", "two", "{0} gi, {0} gath" },
//        { "cy", "few", "{0} chi, {0} cath" },
//        { "cy", "many", "{0} chi, {0} chath" },
//        { "cy", "other", "{0} ci, {0} cath" },
//        { "da", "one", "{0} dag" },
//        { "da", "other", "{0} dage" },
//        { "de", "one", "{0} Tag" },
//        { "de", "other", "{0} Tage" },
//        { "dz", "other", "ཉིནམ་ {0} " },
//        { "el", "one", "{0} ημέρα" },
//        { "el", "other", "{0} ημέρες" },
//        { "es", "one", "{0} día" },
//        { "es", "other", "{0} días" },
//        { "et", "one", "{0} ööpäev" },
//        { "et", "other", "{0} ööpäeva" },
//        { "eu", "one", "Nire {0} lagunarekin nago" },
//        { "eu", "other", "Nire {0} lagunekin nago" },
//        { "fa", "one", "او {0} فیلم در هفته می‌بیند که کمدی است." },
//        { "fa", "other", "او {0} فیلم در هفته می‌بیند که کمدی هستند." },
//        { "fi", "one", "{0} päivä" },
//        { "fi", "other", "{0} päivää" },
//        { "fil", "one", "{0} mansanas" },
//        { "fil", "other", "{0} na mansanas" },
//        { "fr", "one", "{0} jour" },
//        { "fr", "other", "{0} jours" },
//        { "gl", "one", "{0} día" },
//        { "gl", "other", "{0} días" },
//        { "gsw", "one", "{0} Tag" },
//        { "gsw", "other", "{0} Tage" },
//        { "gu", "one", "{0} કિલોગ્રામ" },
//        { "gu", "other", "{0} કિલોગ્રામ્સ" },
//        { "gv", "one", "{0} thunnag/vuc/ooyl" },
//        { "gv", "two", "{0} hunnag/vuc/ooyl" },
//        { "gv", "few", "{0} thunnag/muc/ooyl" },
//        { "gv", "many", "{0} dy hunnagyn/dy vucyn/dy ooylyn" },
//        { "gv", "other", "{0} thunnagyn/mucyn/ooylyn" },
//        { "he", "many", "{0} שנה" },
//        { "he", "one", "שנה" },
//        { "he", "other", "{0} שנים" },
//        { "he", "two", "שנתיים" },
//        { "hi", "one", "{0} घंटा" },
//        { "hi", "other", "{0} घंटे" },
//        { "hr", "few", "za {0} mjeseca" },
//        { "hr", "many", "za {0} mjeseci" },
//        { "hr", "one", "za {0} mjesec" },
//        { "hr", "other", "za {0} mjeseci" },
//        { "hu", "one", "A kosár tartalma: {0} X. Megveszi?" },
//        { "hu", "other", "A kosár tartalma: {0} X. Megveszi őket?" },
//        { "hy", "one", "այդ {0} ժամը" },
//        { "hy", "other", "այդ {0} ժամերը" },
//        { "id", "other", "{0} hari" },
//        { "is", "one", "{0} dagur" },
//        { "is", "other", "{0} dagar" },
//        { "it", "one", "{0} giorno" },
//        { "it", "other", "{0} giorni" },
//        { "ja", "other", "{0}日" },
//        { "ka", "one", "კალათში {0} X-ია. შეიძენთ მას?" }, //
//        { "ka", "other", "კალათში {0} X-ია. შეიძენთ მათ?" }, //
//        { "kk", "one", "Cебетте {0} Х бар. Ол сіздікі ме?" }, //
//        { "kk", "other", "Себетте {0} Х бар. Олар сіздікі ме?" }, //
//        { "kl", "one", "{0} Ulloq" },
//        { "kl", "other", "{0} Ullut" },
//        { "km", "other", "{0} ថ្ងៃ" },
//        { "kn", "one", "{0} ದಿನ" },
//        { "kn", "other", "{0} ದಿನಗಳು" },
//        { "ko", "other", "{0}일" },
//        { "ky", "one", "Себетте {0} Х бар. Аны аласызбы?" },
//        { "ky", "other", "Себетте {0} Х бар. Аларды аласызбы?" },
//        { "lo", "other", "{0} ມື້" },
//        { "lt", "one", "{0} obuolys" },
//        { "lt", "few", "{0} obuoliai" },
//        { "lt", "many", "{0} obuolio" },
//        { "lt", "other", "{0} obuolių" },
//        { "lv", "one", "{0} diennakts" },
//        { "lv", "other", "{0} diennaktis" },
//        { "lv", "zero", "{0} diennakšu" },
//        { "mk", "one", "{0} ден" },
//        { "mk", "other", "{0} дена" },
//        { "ml", "one", "{0} വ്യക്തി" },
//        { "ml", "other", "{0} വ്യക്തികൾ" },
//        { "mn", "one", "Картанд {0} Х байна. Үүнийг авах уу?" },
//        { "mn", "other", "Картанд {0} Х байна. Тэднийг авах уу?" },
//        { "mr", "one", "{0} घर" },
//        { "mr", "other", "{0} घरे" },
//        { "ms", "other", "{0} hari" },
//        { "my", "other", "{0}ရက္" },
//        { "nb", "one", "{0} dag" },
//        { "nb", "other", "{0} dager" },
//        { "ne", "one", "तपाईँसँग {0} निम्तो छ" },
//        { "ne", "other", "तपाईँसँग {0} निम्ता छन््" },
//        //        {"ne", "", "{0} दिन बाँकी छ ।"},
//        //        {"ne", "", "{0} दिन बाँकी छ ।"},
//        //        {"ne", "", "{0} दिन बाँकी छ ।"},
//        //        {"ne", "", "{0} जनाहरू पाहुना बाँकी छ ।"},
//        { "nl", "one", "{0} dag" },
//        { "nl", "other", "{0} dagen" },
//        { "pa", "one", "{0} ਘੰਟਾ" },
//        { "pa", "other", "{0} ਘੰਟੇ" },
//        { "pl", "few", "{0} miesiące" },
//        { "pl", "many", "{0} miesięcy" },
//        { "pl", "one", "{0} miesiąc" },
//        { "pl", "other", "{0} miesiąca" },
//        { "pt", "one", "{0} ponto" },
//        { "pt", "other", "{0} pontos" },
//        //        {"pt_PT", "one", "{0} dia"},
//        //        {"pt_PT", "other", "{0} dias"},
//        { "ro", "few", "{0} zile" },
//        { "ro", "one", "{0} zi" },
//        { "ro", "other", "{0} de zile" },
//        { "ru", "few", "из {0} книг за {0} дня" },
//        { "ru", "many", "из {0} книг за {0} дней" },
//        { "ru", "one", "из {0} книги за {0} день" },
//        { "ru", "other", "из {0} книги за {0} дня" },
//        { "si", "one", "{0} පොතක් ඇත. එය කියවීමි." },
//        { "si", "other", "පොත් {0}ක් ඇත. ඒවා කියවීමි." },
//        { "sk", "few", "{0} dni" },
//        { "sk", "one", "{0} deň" },
//        { "sk", "other", "{0} dní" },
//        { "sk", "many", "{0} dňa" }, // added from spreadsheet
//        { "sl", "few", "{0} ure" },
//        { "sl", "one", "{0} ura" },
//        { "sl", "other", "{0} ur" },
//        { "sl", "two", "{0} uri" },
//        { "sq", "one", "{0} libër" },
//        { "sq", "other", "{0} libra" },
//        { "sr", "few", "{0} сата" },
//        { "sr", "many", "{0} сати" },
//        { "sr", "one", "{0} сат" },
//        { "sr", "other", "{0} сати" },
//        { "sv", "one", "om {0} dag" },
//        { "sv", "other", "om {0} dagar" },
//        { "sw", "one", "siku {0} iliyopita" },
//        { "sw", "other", "siku {0} zilizopita" },
//        { "ta", "one", "{0} நாள்" },
//        { "ta", "other", "{0} நாட்கள்" },
//        { "te", "one", "{0} రోజు" },
//        { "te", "other", "{0} రోజులు" },
//        { "th", "other", "{0} วัน" },
//        { "tr", "one", "Sepetinizde {0} X var. Bunu almak istiyor musunuz?" },
//        { "tr", "other", "Sepetinizde {0} X var. Bunları almak istiyor musunuz?" },
//        { "ug", "one", "{0}  كىتاب" },
//        { "ug", "other", "{0} بېلىق كۆردۈم ۋە ئۇنى يەۋەتتىم." },
//        { "uk", "few", "{0} дні" },
//        { "uk", "many", "{0} днів" },
//        { "uk", "one", "{0} день" },
//        { "uk", "other", "{0} дня" },
//        { "ur", "one", "{0} گھنٹہ" },
//        { "ur", "other", "{0} گھنٹے" },
//        { "uz", "one", "Savatingizda {0}X bor. Uni sotib olasizmi?" },
//        { "uz", "other", "Savatingizda {0}X bor. Ularni sotib olasizmi?" },
//        { "vi", "one", "Rẽ vào lối rẽ thứ nhất bên phải." },
//        { "vi", "other", "Rẽ vào lối rẽ thứ {0} bên phải." },
//        { "yue", "other", "{0} 本書" },
//        { "zh", "other", "{0} 天" },
//        { "zh_Hant", "other", "{0} 日" },
//        { "en", "one", "{0} day" }, // added from spreadsheet
//        { "en", "other", "{0} days" }, // added from spreadsheet
//        { "zu", "one", "{0} usuku" }, // added from spreadsheet
//        { "zu", "other", "{0} izinsuku" }, // added from spreadsheet
//
//        { "ga", "one", "{0} ci, {0} gath" },
//        { "ga", "two", "{0} gi, {0} gath" },
//        { "ga", "few", "{0} chi, {0} cath" },
//        { "ga", "many", "{0} chi, {0} chath" },
//        { "ga", "other", "{0} ci, {0} cath" },
//    };

    static String[][] EXTRA_SAMPLE_SOURCE = {
        { "he,iw", "10,20" },
        { "und,az,ka,kk,ky,mk,mn,my,pa,sq,uz", "0,0.0,0.1,1,1.0,1.1,2.0,2.1,3,4,5,10,11,1.2,1.121" },
    };

//    static String[][] overrides = {
//        { "gu,mr,kn,am,fa", "one: n within 0..1" },
//        { "ta,te,uz,ky,hu,az,ka,mn,tr", "one: n is 1" },
//        { "bn", "one: n within 0..1" },
//        { "kk", "one: n is 1" },
//        { "en,ca,de,et,fi,gl,it,nl,sw,ur", "one: j is 1" },
//        { "sv", "one: j is 1 or f is 1" },
//        { "pt", "one: n is 1 or f is 1" },
//        { "si", "one: n in 0,1 or i is 0 and f is 1" },
//        { "cs,sk", "one: j is 1;  few: j in 2..4; many: v is not 0" },
//        //{"cy", "one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
//        //{"el", "one: j is 1 or i is 0 and f is 1"},
//        { "da", "one: j is 1 or f is 1" },
//        { "is", "one: j mod 10 is 1 and j mod 100 is not 11 or f mod 10 is 1 and f mod 100 is not 11" },
//        { "fil,tl", "one: j in 0..1" },
//        { "he,iw", "one: j is 1;  two: j is 2; many: j not in 0..10 and j mod 10 is 0", "10,20" },
//        { "hi", "one: n within 0..1" },
//        { "hy", "one: n within 0..2 and n is not 2" },
//        //                    {"hr", "one: j mod 10 is 1 and j mod 100 is not 11;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
//        { "lv", "zero: n mod 10 is 0" +
//            " or n mod 100 in 11..19" +
//            " or v is 2 and f mod 100 in 11..19;" +
//            "one: n mod 10 is 1 and n mod 100 is not 11" +
//            " or v is 2 and f mod 10 is 1 and f mod 100 is not 11" +
//        " or v is not 2 and f mod 10 is 1" },
//        //                    {"lv", "zero: n mod 10 is 0" +
//        //                            " or n mod 10 in 11..19" +
//        //                            " or v in 1..6 and f is not 0 and f mod 10 is 0" +
//        //                            " or v in 1..6 and f mod 10 in 11..19;" +
//        //                            "one: n mod 10 is 1 and n mod 100 is not 11" +
//        //                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
//        //                            " or v not in 0..6 and f mod 10 is 1"},
//        {
//            "pl",
//        "one: j is 1;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j is not 1 and j mod 10 in 0..1 or j mod 10 in 5..9 or j mod 100 in 12..14" },
//        { "sl", "one: j mod 100 is 1;  two: j mod 100 is 2;  few: j mod 100 in 3..4 or v is not 0" },
//        //                    {"sr", "one: j mod 10 is 1 and j mod 100 is not 11" +
//        //                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
//        //                            " or v not in 0..6 and f mod 10 is 1;" +
//        //                            "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
//        //                            " or v in 1..6 and f mod 10 in 2..4 and f mod 100 not in 12..14" +
//        //                            " or v not in 0..6 and f mod 10 in 2..4"
//        //                    },
//        { "sr,hr,sh,bs", "one: j mod 10 is 1 and j mod 100 is not 11" +
//            " or f mod 10 is 1 and f mod 100 is not 11;" +
//            "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
//            " or f mod 10 in 2..4 and f mod 100 not in 12..14"
//        },
//        // +
//        //                            " ; many: j mod 10 is 0 " +
//        //                            " or j mod 10 in 5..9 " +
//        //                            " or j mod 100 in 11..14" +
//        //                            " or v in 1..6 and f mod 10 is 0" +
//        //                            " or v in 1..6 and f mod 10 in 5..9" +
//        //                            " or v in 1..6 and f mod 100 in 11..14" +
//        //                    " or v not in 0..6 and f mod 10 in 5..9"
//        { "mo,ro", "one: j is 1; few: v is not 0 or n is 0 or n is not 1 and n mod 100 in 1..19" },
//        { "ru", "one: j mod 10 is 1 and j mod 100 is not 11;" +
//            " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
//            //                            + "; many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
//        },
//        { "uk", "one: j mod 10 is 1 and j mod 100 is not 11;  " +
//            "few: j mod 10 in 2..4 and j mod 100 not in 12..14;  " +
//        "many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14" },
//        { "zu", "one: n within 0..1" },
//        { "mk", "one: j mod 10 is 1 or f mod 10 is 1" },
//        { "pa", "one: n in 0..1" },
//        { "lt", "one: n mod 10 is 1 and n mod 100 not in 11..19; " +
//            "few: n mod 10 in 2..9 and n mod 100 not in 11..19; " +
//        "many: f is not 0" },
//    };
//    static String[][] ORDINAL_SAMPLES = {
//        { "af", "Neem die {0}e afdraai na regs.", "1" },
//        { "am", "በቀኝ በኩል ባለው በ{0}ኛው መታጠፊያ ግባ።", "1" },
//        { "ar", "اتجه إلى المنعطف الـ {0} يمينًا.", "1" },
//        { "az", "{0}-ci sağ döngəni seçin.", "one" },
//        { "az", "{0}-cı sağ döngəni seçin.", "many" },
//        { "az", "{0}-cü sağ döngəni seçin.", "few" },
//        { "az", "{0}-cu sağ döngəni seçin.", "other" },
//        { "be", "{0}-і дом злева", "few" },
//        { "be", "{0}-ы дом злева", "other" },
//        { "bg", "Завийте надясно по {0}-ата пресечка.", "1" },
//        { "bn", "ডান দিকে {0}ম বাঁকটি নিন।", "1" },
//        { "bn", "ডান দিকে {0}য় বাঁকটি নিন।", "2" },
//        { "bn", "ডান দিকে {0}র্থ বাঁকটি নিন।", "4" },
//        { "bn", "ডান দিকে {0}ষ্ঠ বাঁকটি নিন।", "6" },
//        { "bn", "ডান দিকে {0}তম বাঁকটি নিন।", "11" },
//        { "bs", "Skrenite na {0}. križanju desno.", "1" },
//        { "ca", "Agafa el {0}r a la dreta.", "1" },
//        { "ca", "Agafa el {0}n a la dreta.", "2" },
//        { "ca", "Agafa el {0}t a la dreta.", "4" },
//        { "ca", "Agafa el {0}è a la dreta.", "5" },
//        { "cs", "Na {0}. křižovatce odbočte vpravo.", "1" },
//        { "da", "Tag den {0}. vej til højre.", "1" },
//        { "de", "{0}. Abzweigung nach rechts nehmen", "1" },
//        { "en", "Take the {0}st right.", "1" },
//        { "en", "Take the {0}nd right.", "2" },
//        { "en", "Take the {0}rd right.", "3" },
//        { "en", "Take the {0}th right.", "4" },
//        { "el", "Στρίψτε στην {0}η γωνία δεξιά.", "1" },
//        { "es", "Toma la {0}.ª a la derecha.", "1" },
//        { "et", "Tehke {0}. parempööre.", "1" },
//        { "eu", "{0}. bira eskuinetara", "other" },
//        { "fa", "در پیچ {0}ام سمت راست بپیچید.", "1" },
//        { "fi", "Käänny {0}. risteyksestä oikealle.", "1" },
//        { "fil", "Lumiko sa unang kanan.", "1" },
//        { "fil", "Lumiko sa ika-{0} kanan.", "2" },
//        { "fr", "Prenez la {0}re à droite.", "1" },
//        { "fr", "Prenez la {0}e à droite.", "2" },
//        { "ga", "Glac an {0}ú chasadh ar dheis.", "1" },
//        { "ga", "Glac an {0}ú casadh ar dheis.", "2" },
//        { "gl", "Colle a {0}.ª curva á dereita.", "1" },
//        { "gsw", "{0}. Abzweigung nach rechts nehmen", "1" },
//        { "gu", "જમણી બાજુએ {0}લો વળાંક લો.", "1" },
//        { "gu", "જમણી બાજુએ {0}જો વળાંક લો.", "2" },
//        { "gu", "જમણી બાજુએ {0}થો વળાંક લો.", "4" },
//        { "gu", "જમણી બાજુએ {0}મો વળાંક લો.", "5" },
//        { "gu", "જમણી બાજુએ {0}ઠો વળાંક લો.", "6" },
//        { "hi", "{0}ला दाहिना मोड़ लें.", "1" },
//        { "hi", "{0}रा दाहिना मोड़ लें.", "2" },
//        { "hi", "{0}था दाहिना मोड़ लें.", "4" },
//        { "hi", "{0}वां दाहिना मोड़ लें.", "5" },
//        { "hi", "{0}ठा दाहिना मोड़ लें.", "6" },
//        { "hr", "Skrenite na {0}. križanju desno.", "1" },
//        { "hu", "Az {0}. lehetőségnél forduljon jobbra.", "1" },
//        { "hu", "A {0}. lehetőségnél forduljon jobbra.", "2" },
//        { "hy", "Թեքվեք աջ {0}-ին խաչմերուկից:", "one" },
//        { "hy", "Թեքվեք աջ {0}-րդ խաչմերուկից:", "other" },
//        { "id", "Ambil belokan kanan ke-{0}.", "1" },
//        { "is", "Taktu {0}. beygju til hægri.", "1" },
//        { "it", "Prendi la {0}° a destra.", "1" },
//        { "it", "Prendi l'{0}° a destra.", "8" },
//        { "he", "פנה ימינה בפנייה ה-{0}", "1" },
//        { "ja", "{0} 番目の角を右折します。", "1" },
//        { "ka", "{0}-ლი", "one" },
//        { "ka", "მე-{0}", "many" },
//        { "ka", "{0}-ე", "other" },
//        { "kk", "{0}-ші бұрылыстан оңға бұрылыңыз.", "many" },
//        { "kk", "{0}-шы бұрылыстан оңға бұрылыңыз.", "other" },
//        { "km", "បត់​ស្តាំ​លើក​ទី​ {0}", "1" },
//        { "kn", "{0}ನೇ ಬಲತಿರುವನ್ನು ತೆಗೆದುಕೊಳ್ಳಿ.", "1" },
//        { "ko", "{0}번째 길목에서 우회전하세요.", "1" },
//        { "ky", "{0}-бурулуштан оңго бурулуңуз.", "other" },
//        { "lo", "ລ້ຽວຂວາທຳອິດ.", "1" },
//        { "lo", "ລ້ຽວຂວາທີ {0}.", "23" },
//        { "lt", "{0}-ame posūkyje sukite į dešinę.", "1" },
//        { "lv", "Dodieties {0}. pagriezienā pa labi.", "1" },
//        { "mk", "Сврти на {0}-вата улица десно.", "one" },
//        { "mk", "Сврти на {0}-рата улица десно.", "two" },
//        { "mk", "Сврти на {0}-мата улица десно.", "many" },
//        { "mk", "Сврти на {0}-тата улица десно.", "other" },
//        { "ml", "{0}-ാമത്തെ വലത്തേക്ക് തിരിയുക.", "1" },
//        { "mn", "{0}-р баруун эргэлтээр орно уу", "1" },
//        { "mr", "{0}ले उजवे वळण घ्या.", "1" },
//        { "mr", "{0}रे उजवे वळण घ्या.", "2" },
//        { "mr", "{0}थे उजवे वळण घ्या.", "4" },
//        { "mr", "{0}वे उजवे वळण घ्या.", "5" },
//        { "ms", "Ambil belokan kanan yang pertama.", "1" },
//        { "ms", "Ambil belokan kanan yang ke-{0}.", "2" },
//        { "my", "{0} အုပ်မြောက်", "15" },
//        { "ne", "{0} ओ दायाँ घुम्ति लिनुहोस्", "1" },
//        { "ne", "{0} औं दायाँ घुम्ति लिनुहोस्", "5" },
//        { "nl", "Neem de {0}e afslag rechts.", "1" },
//        { "nb", "Ta {0}. svingen til høyre.", "1" },
//        { "pa", "ਸਜੇ ਪਾਸੇ {0} ਮੋੜ ਲਵੋ", "1" },
//        { "pl", "Skręć w {0} w prawo.", "1" },
//        { "pt", "{0}º livro", "15" },
//        { "ro", "Faceţi virajul nr. {0} la dreapta.", "1" },
//        { "ro", "Faceţi virajul al {0}-lea la dreapta.", "2" },
//        { "ru", "Сверните направо на {0}-м перекрестке.", "1" },
//        { "si", "{0} වන හැරවුම දකුණට", "other" },
//        { "sk", "Na {0}. križovatke odbočte doprava.", "1" },
//        { "sl", "V {0}. križišču zavijte desno.", "1" },
//        { "sq", "Merrni kthesën e {0}-rë në të djathtë.", "1" },
//        { "sq", "Merrni kthesën e {0}-t në të djathtë.", "4" },
//        { "sq", "Merrni kthesën e {0}-të në të djathtë.", "2" },
//        { "sr", "Скрените у {0}. десно.", "1" },
//        { "sv", "Ta {0}:a svängen till höger", "1" },
//        { "sv", "Ta {0}:e svängen till höger", "3" },
//        { "sw", "Chukua mpinduko wa {0} kulia.", "1" },
//        { "ta", "{0}வது வலது திருப்பத்தை எடு.", "1" },
//        { "te", "{0}వ కుడి మలుపు తీసుకోండి.", "1" },
//        { "th", "เลี้ยวขวาที่ทางเลี้ยวที่ {0}", "1" },
//        { "tr", "{0}. sağdan dönün.", "2" },
//        //        { "uk", "Поверніть праворуч на {0}-му повороті.", "1" },
//        { "uk", "{0}-а дивізія, {0}-е коло", "1" },
//        { "uk", "{0}-я дивізія, {0}-є коло", "3" },
//        { "ur", "دایاں موڑ نمبر {0} مڑیں۔", "1" },
//        { "uz", "{0}chi chorraxada o'ngga buriling.", "1" },
//        { "vi", "Rẽ vào lối rẽ thứ nhất bên phải.", "1" },
//        { "vi", "Rẽ vào lối rẽ thứ {0} bên phải.", "2" },
//        { "zh_Hant", "在第 {0} 個路口右轉。", "1" },
//        { "zu", "Thatha indlela ejikela kwesokudla engu-{0}", "other" },
//        { "cy", "{0}fed ci", "7" },
//        { "cy", "ci {0}af", "1" },
//        { "cy", "{0}il gi", "2" },
//        { "cy", "{0}ydd ci", "3" },
//        { "cy", "{0}ed ci", "5" },
//        { "cy", "ci rhif {0}", "10" },
//        { "yue", "第 {0} 本書", "15" },
//        { "zh", "在第 {0} 个路口右转。", "15" },
//    };
}
