package org.unicode.cldr.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R5;

public class CheckLanguageCodeConverter {
    public static void main(String[] args) {
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        System.out
            .println("Input Name" + "\t" + "Std Code" + "\t" + "Std Name");
        Set<LanguageName> names = new TreeSet<LanguageName>();
        for (Entry<String, String> codeName : LanguageCodeConverter
            .getLanguageNameToCode().entrySet()) {
            String name = codeName.getKey();
            String code = codeName.getValue();
            if (LanguageCodeConverter.getExceptionCodes().contains(code)) {
                String cldrName = getName(english, code);
                names.add(new LanguageName(code, cldrName));
                if (!name.equalsIgnoreCase(cldrName)) {
                    names.add(new LanguageName(code, name));
                }
            }
        }
        for (LanguageName item : names) {
            String code = item.get0();
            String name = item.get1();
            String cldrName = getName(english, code);
            System.out.println(name.toLowerCase(Locale.ENGLISH) + "\t" + code
                + "\t" + cldrName);
        }

        System.out.println();
        System.out.println("Input Code" + "\t" + "Bcp47 Code" + "\t"
            + "CLDR Code" + "\t" + "Google Code" + "\t" + "Std Name");

        Set<LanguageLine> lines = new TreeSet<LanguageLine>();
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo
            .getInstance();
        Map<String, R2<List<String>, String>> languageAliases = supplementalDataInfo
            .getLocaleAliasInfo().get("language");

        for (Entry<String, R2<List<String>, String>> languageAlias : languageAliases
            .entrySet()) {
            String badCode = languageAlias.getKey();
            R2<List<String>, String> alias = languageAlias.getValue();

            String goodCode = alias.get0() == null ? "?" : alias.get0().get(0);

            if (LanguageCodeConverter.getExceptionCodes().contains(goodCode)) {
                String cldrName = getName(english, goodCode);
                String googleCode = LanguageCodeConverter
                    .toGoogleLocaleId(goodCode);
                addLine(lines, badCode, goodCode, googleCode, cldrName);
            }
        }
        for (Entry<String, String> entry : LanguageCodeConverter.GOOGLE_CLDR
            .entrySet()) {
            String googleCode = entry.getKey();
            String goodCode = LanguageCodeConverter.toHyphenLocale(entry
                .getValue());
            String cldrName = getName(english, goodCode);
            addLine(lines, googleCode, goodCode, googleCode, cldrName);
        }
        for (String goodCode : LanguageCodeConverter.getExceptionCodes()) {
            String cldrName = getName(english, goodCode);
            String googleCode = LanguageCodeConverter
                .toGoogleLocaleId(goodCode);
            addLine(lines, googleCode, goodCode, googleCode, cldrName);
        }
        for (String cldr : LanguageCodeConverter.CLDR_GOOGLE.keySet()) {
            String goodCode = LanguageCodeConverter.toUnderbarLocale(cldr);
            String googleCode = LanguageCodeConverter
                .toGoogleLocaleId(goodCode);
            String cldrName = getName(english, goodCode);
            addLine(lines, googleCode, goodCode, googleCode, cldrName);
        }
        for (LanguageLine entry : lines) {
            if (entry.isStandard()) {
                printLine(entry);
            }
        }
        for (LanguageLine entry : lines) {
            if (!entry.isStandard()) {
                printLine(entry);
            }
        }

        LikelySubtags likely = new LikelySubtags(supplementalDataInfo);
        LanguageTagParser ltp = new LanguageTagParser();
        // get targets of language aliases for macros
        Map<String, String> macroToEncompassed = new HashMap<String, String>();
        for (Entry<String, R2<List<String>, String>> languageAlias : languageAliases
            .entrySet()) {
            String reason = languageAlias.getValue().get1();
            if ("macrolanguage".equals(reason)) {
                macroToEncompassed.put(languageAlias.getValue().get0().get(0),
                    languageAlias.getKey());
            }
        }

        System.out.println();
        System.out.println("Bcp47 Code" + "\t" + "Name" + "\t"
            + "Script\tEncomp.Lang?\tName");

        for (String code : LanguageCodeConverter.getExceptionCodes()) {
            String max = likely.maximize(code);

            String script = "?";
            if (max != null) {
                script = ltp.set(max).getScript();
            } else {
                Set<BasicLanguageData> data = supplementalDataInfo
                    .getBasicLanguageData(code);
                if (data != null) {
                    for (BasicLanguageData item : data) {
                        Set<String> scripts = item.getScripts();
                        if (scripts == null || scripts.size() == 0)
                            continue;
                        script = scripts.iterator().next();
                        Type type = item.getType();
                        if (type == Type.primary) {
                            break;
                        }
                    }
                }
                if (script.equals("?")) {
                    script = LanguageCodeConverter.EXTRA_SCRIPTS.get(code);
                }
            }

            String encompassed = macroToEncompassed.get(code);
            if (encompassed == null) {
                encompassed = "";
            } else {
                encompassed = "\t" + encompassed + "\t"
                    + getName(english, encompassed);
            }
            System.out.println(LanguageCodeConverter.toHyphenLocale(code)
                + "\t" + getName(english, code) + "\t" + script
                + encompassed);
        }
    }

    public static String getName(CLDRFile english, String goodCode) {
        if (goodCode.startsWith("x_")) {
            return "Private use: " + goodCode.substring(2);
        }
        return english.getName(goodCode);
    }

    public static void printLine(LanguageLine entry) {
        System.out.println(entry.get1() // reverse the order: bad
            + "\t" + entry.get0() // bcp47
            + "\t" + entry.get2() // cldr
            + "\t" + entry.get3() // google
            + "\t" + entry.get4());
    }

    private static class LanguageLine extends
        R5<String, String, String, String, String> {
        public LanguageLine(String a, String b, String c, String d) {
            super(LanguageCodeConverter.toHyphenLocale(a), b,
                LanguageCodeConverter.toUnderbarLocale(a), c, d);
        }

        boolean isStandard() {
            return get0().equals(get2()) && get0().equals(get3());
        }
    }

    public static void addLine(Set<LanguageLine> lines, String badCode,
        String goodCode, String googleCode, String cldrName) {
        // add the various combinations
        lines.add(new LanguageLine(goodCode, goodCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, badCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, googleCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toUnderbarLocale(goodCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toUnderbarLocale(badCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toUnderbarLocale(googleCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toHyphenLocale(goodCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toHyphenLocale(badCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, LanguageCodeConverter
            .toHyphenLocale(googleCode), googleCode, cldrName));
    }

    private static class LanguageName extends Row.R2<String, String> {

        public LanguageName(String a, String b) {
            super(a, b);
            // TODO Auto-generated constructor stub
        }
    }
}
