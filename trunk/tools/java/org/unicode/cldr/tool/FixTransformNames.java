package org.unicode.cldr.tool;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.Direction;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.CLDRTransforms.Visibility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes.CodeType;
import org.unicode.cldr.util.With;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;

public class FixTransformNames {
    CLDRConfig testInfo = ToolConfig.getToolInstance();

    public static void main(String[] args) {
        new FixTransformNames().run(args);
    }

    Map<String, String> fieldToCode = new HashMap<String, String>();
    Map<String, String> oldToNewVariant = new HashMap<String, String>();
    Map<String, String> fieldToVariant = new HashMap<String, String>();
    Map<String, String> targetToCode = new HashMap<String, String>();

    Set<String> languageCodes = new HashSet<String>();

    private void run(String[] args) {
        CLDRFile file = testInfo.getEnglish();
        for (String lang : testInfo.getStandardCodes().getAvailableCodes(CodeType.language)) {
            String name = file.getName(lang);
            if (!name.equals(lang)) {
                fieldToCode.put(name, lang);
                languageCodes.add(lang);
            }
        }
        fieldToCode.put("Maldivian", "dv");
        fieldToCode.put("JapaneseKana", "und_Kana");
        fieldToCode.put("Kirghiz", "ky");
        fieldToCode.put("ASCII", "und-Qaaa");
        fieldToCode.put("zh_Latn_PINYIN", "zh_Latn");
        fieldToCode.put("zh_Latn_PINYIN", "zh_Latn");
        fieldToCode.put("IPA", "und-fonipa");
        fieldToCode.put("XSampa", "und-fonxsamp");
        fieldToCode.put("Simplified", "und-Hans");
        fieldToCode.put("Traditional", "und-Hant");
        fieldToCode.put("ConjoiningJamo", "und-Qaaj");
        oldToNewVariant.put("UNGEGN", "-m0-ungegn");
        oldToNewVariant.put("BGN", "-m0-bgn");
        addX(oldToNewVariant, "-x0-", "hex", "C Java Perl, Plain Unicode XML XML10");
        addX(fieldToVariant, "-x0-", "", "CaseFold Lower Title Upper");
        addX(fieldToVariant, "-x0-", "", "NFC NFD NFKC NFKD FCC FCD FullWidth Halfwidth");
        addX(fieldToVariant, "-x0-", "", "Null Remove");
        addX(fieldToVariant, "-x0-", "", "Accents Publishing Name");
        //exceptions.put("Latin-ConjoiningJamo", "und-t-und-Latn-m0-conjamo"); // Conjoining Jamo - internal
        /*
            <transformName type="BGN">BGN</transformName>
            <transformName type="Numeric">Numeric</transformName>
            <transformName type="Tone">Tone</transformName>
            <transformName type="UNGEGN">UNGEGN</transformName>
            <transformName type="x-Accents">Accents</transformName>
            <transformName type="x-Fullwidth">Fullwidth</transformName>
            <transformName type="x-Halfwidth">Halfwidth</transformName>
            <transformName type="x-Jamo">Jamo</transformName>
            <transformName type="x-Pinyin">Pinyin</transformName>
            <transformName type="x-Publishing">Publishing</transformName>
        
        ??Accents   [Any-Accents]
        ??ConjoiningJamo    [Latin-ConjoiningJamo]
        ??Fullwidth [Fullwidth-Halfwidth]
        ??Halfwidth [Fullwidth-Halfwidth]
        ??InterIndic    [Bengali-InterIndic, Devanagari-InterIndic, Gujarati-InterIndic, Gurmukhi-InterIndic, InterIndic-Bengali, InterIndic-Devanagari, InterIndic-Gujarati, InterIndic-Gurmukhi, InterIndic-Kannada, InterIndic-Latin, InterIndic-Malayalam, InterIndic-Oriya, InterIndic-Tamil, InterIndic-Telugu, Kannada-InterIndic, Latin-InterIndic, Malayalam-InterIndic, Oriya-InterIndic, Tamil-InterIndic, Telugu-InterIndic]
        ??Jamo  [Jamo-Latin, Latin-Jamo]
        ??Latin-Names   [Han-Latin-Names]
        ??Lower [az-Lower, el-Lower, lt-Lower, tr-Lower]
        ??NumericPinyin [Latin-NumericPinyin, Pinyin-NumericPinyin]
        ??Publishing    [Any-Publishing]
        ??Simplified    [Simplified-Traditional]
        ??Spacedhan [Han-Spacedhan]
        ??ThaiLogical   [Thai-ThaiLogical, ThaiLogical-Latin]
        ??ThaiSemi  [Thai-ThaiSemi]
        ??Title [az-Title, el-Title, lt-Title, nl-Title, tr-Title]
        ??Traditional   [Simplified-Traditional]
        ??Upper [az-Upper, el-Upper, lt-Upper, tr-Upper]
        
         */

        //CLDRTransforms transforms = CLDRTransforms.getInstance();
        Relation<String, String> missing = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> found = new TreeSet<String>();
        Map<String, String> allFields = new TreeMap<String, String>();
        Map<String, String> specialFields = new TreeMap<String, String>();
        Map<String, String> allVariants = new TreeMap<String, String>();

        Set<String> internal = new TreeSet<String>();
        Set<String> cldrIds = getCldrIds(internal);

        for (String id : CLDRTransforms.getAvailableIds()) {
            if (id.endsWith(".xml")) {
                id = id.substring(0, id.length() - 4);
            }
            int first = id.indexOf('-');
            int second = id.indexOf('-', first + 1);
            String id2 = second < 0 ? id : id.substring(0, second) + "/" + id.substring(second + 1);
            if (internal.contains(id2)) {
                System.out.println("*Internal:\t" + id);
            } else if (!cldrIds.contains(id2)) {
                System.out.println("*Missing:\t" + id);
            }
        }
        Set<String> icuOnlyIds = new TreeSet<String>();
        for (Enumeration<String> x = Transliterator.getAvailableIDs(); x.hasMoreElements();) {
            String icuId = x.nextElement();
            if (!cldrIds.contains(icuId)) {
                icuOnlyIds.add(icuId);
            }
        }

        for (String id : With.in(cldrIds, icuOnlyIds)) {
            String original = id;

            ParsedTransformID ptd = new ParsedTransformID().set(id);
            if (!id.equals(ptd.toString())) {
                missing.put("ERROR\t" + id, ptd.toString());
                continue;
            }
            // und-Latn-t-und-cyrl
            // und-Hebr-t-und-latn-m0-ungegn-1977

            String variantSource = ptd.variant;
            String variant = getFixedVariant(variantSource);
            if (variant.contains("?")) {
                missing.put(variantSource, id);
            } else {
                allVariants.put(variant, variantSource);
            }

            String source = getFixedName(ptd.source);
            if (source.contains("?")) {
                if (variantSource == null) {
                    String temp = fieldToVariant.get(ptd.source);
                    if (temp != null) {
                        source = "";
                        variant = temp;
                        specialFields.put(source + "/" + variant, ptd.source);
                    } else {
                        missing.put(ptd.source, id);
                    }
                } else {
                    missing.put(ptd.source, id);
                }
            } else {
                allFields.put(source, ptd.source);
            }
            String target = getFixedName(ptd.target);
            if (target.contains("?")) {
                if (variantSource == null) {
                    String temp = fieldToVariant.get(ptd.target);
                    if (temp != null) {
                        target = "und";
                        variant = temp;
                        specialFields.put(target + "/" + variant, ptd.target);
                    } else {
                        missing.put(ptd.target, id);
                    }
                } else {
                    missing.put(ptd.target, id);
                }
            } else {
                allFields.put(target, ptd.target);
            }
            String bcp47 = target + "-t" + (source.isEmpty() ? "" : "-" + source) + variant;

            if (bcp47.contains("?")) {
                continue;
            }
            found.add(bcp47 + "\t" + getName(target) + "\t" + getName(source) + "\t" + variant + "\t" + original);
        }

        System.out.println("\nAll Fields");
        for (Entry<String, String> s : allFields.entrySet()) {
            System.out.println(s.getKey() + "\t" + getName(s.getKey()) + "\t" + s.getValue());
        }
        System.out.println("\nSpecial Fields");
        for (Entry<String, String> s : specialFields.entrySet()) {
            System.out.println(s.getKey() + "\t" + s.getValue());
        }
        System.out.println("\nAll Variants");
        for (Entry<String, String> s : allVariants.entrySet()) {
            System.out.println(s.getKey() + "\t" + s.getValue());
        }
        System.out.println("\nFound IDs");
        for (String s : found) {
            System.out.println(s);
        }
        System.out.println("\nUnconverted");
        for (Entry<String, Set<String>> s : missing.keyValuesSet()) {
            System.out.println(s.getKey() + "\t" + s.getValue());
        }
    }

    private void addX(Map<String, String> oldToNewVariant2, String type, String prefix, String items) {
        for (String part : items.split("\\s+")) {
            String target = prefix + part.toLowerCase(Locale.ENGLISH);
            if (target.length() > 8) {
                target = target.substring(0, 8);
            }
            oldToNewVariant2.put(part, type + target);
        }
    }

    LanguageTagParser ltp = new LanguageTagParser();
    CLDRFile english = testInfo.getEnglish();

    private String getName(String target) {
        if (target.equals("und")) {
            return "Any";
        }
        ltp.set(target);
        if (ltp.getLanguage().equals("und")) {
            String result = "";
            result = add(result, CLDRFile.SCRIPT_NAME, ltp.getScript());
            result = add(result, CLDRFile.TERRITORY_NAME, ltp.getRegion());
            for (String v : ltp.getVariants()) {
                result = add(result, CLDRFile.VARIANT_NAME, v);
            }
            return result;
        }
        return english.getName(target.replace('-', '_'));
    }

    private String add(String result, int type, String code) {
        if (code.isEmpty()) {
            return result;
        }
        if (result.length() != 0) {
            result += ", ";
        }
        String temp = english.getName(type, code);
        if (type == CLDRFile.SCRIPT_NAME && fieldToCode.containsKey(temp)) {
            temp += "*";
        }
        return result + (temp == null ? code : temp);
    }

    private String getFixedVariant(String variant) {
        if (variant == null || variant.isEmpty()) {
            return "";
        }
        String fixedVariant = oldToNewVariant.get(variant);
        if (fixedVariant != null) {
            return fixedVariant;
        }
        return "??" + variant;
    }

    private Set<String> getCldrIds(Set<String> internal) {
        Set<String> result = new LinkedHashSet<String>();
        for (String s : CLDRTransforms.getAvailableIds()) {
            //String dir;
            ParsedTransformID directionInfo = new ParsedTransformID();
            //String rules = CLDRTransforms.getIcuRulesFromXmlFile(CLDRTransforms.TRANSFORM_DIR, s, directionInfo);
            Set<String> store = directionInfo.getVisibility() == Visibility.external ? result : internal;
            if (directionInfo.getDirection() != Direction.backward) {
                store.add(directionInfo.getId());
            }
            if (directionInfo.getDirection() != Direction.forward) {
                store.add(directionInfo.getBackwardId());
            }
        }
        return result;
    }

    private String getFixedName(String field) {
        String variant = "";
        if (field.equals("Any")) {
            return "und";
        }
        if (field.contains("_FONIPA")) {
            field = field.replace("_FONIPA", "");
            variant = "-fonipa";
        }
        if (field.equals("es_419")
            || field.equals("ja_Latn")
            || field.equals("zh_Latn")
            || field.equals("und-Latn")) {
            return field.replace("_", "-");
        }
        int source = UScript.getCodeFromName(field);
        if (languageCodes.contains(field)) {
            return field + variant;
        }
        String name;
        try {
            name = UScript.getShortName(source);
            return "und-" + name + variant;
        } catch (Exception e) {
            name = fieldToCode.get(field);
            if (name != null) {
                return name + variant;
            }
        }
        return "??" + field;
    }

}
