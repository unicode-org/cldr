package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.unittest.TestAll;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Row.R5;

public class LanguageCodeConverter {
    static Map<String,String> languageNameToCode = new TreeMap<String,String>();
    static Set<String> exceptionCodes = new TreeSet<String>();

    static TestInfo testInfo = TestAll.TestInfo.getInstance();
    static SupplementalDataInfo supplementalInfo = testInfo.getSupplementalDataInfo();
    static Map<String, R2<List<String>, String>> languageAliases = supplementalInfo.getLocaleAliasInfo().get("language");

    static final Map<String,String> GOOGLE_CLDR = 
        Builder.with(new LinkedHashMap<String,String>()) // preserve order
        .put("iw", "he")
        .put("no", "nb")
        .put("tl", "fil")
        .put("pt-BR", "pt")
        .put("xx-bork", "x_bork")
        .put("xx-elmer", "x_elmer")
        .put("xx-hacker", "x_hacker")
        .put("xx-pirate", "x_pirate")
        .put("xx-klingon", "tlh")
        .put("zh-CN", "zh")
        .put("zh-TW", "zh_Hant")
        .put("zh-HK", "zh_Hant_HK")
        .put("sit-NP", "lif")
        .put("ut", "und")
        .put("un", "und")
        .put("xx", "und")
        
        //.put("sh", "fil")
        .freeze();

    static final Map<String,String> CLDR_GOOGLE = 
        Builder.with(new HashMap<String,String>())
        .putAllTransposed(GOOGLE_CLDR)
        .freeze();
    
    static final Map<String,String> EXTRA_SCRIPTS = 
        Builder.with(new HashMap<String,String>())
        .on("crs", "pcm", "tlh").put("Latn")
        .freeze();

    static {

        Map<String, Map<String, Map<String, String>>> lstreg = StandardCodes.getLStreg();
        Map<String, Map<String, String>> languages = lstreg.get("language");
        Set<String> validCodes = new HashSet<String>();

        for (Entry<String, Map<String, String>> codeInfo : languages.entrySet()) {
            final String code = codeInfo.getKey();
            if (languageAliases.containsKey(code)) {
                continue;
            }
            final Map<String, String> info = codeInfo.getValue();
            String deprecated = info.get("Deprecated");
            if (deprecated != null) {
                continue;
            }
            String name = info.get("Description");
            if (name.equals("Private use")) {
                continue;
            }
            validCodes.add(code);
            if (name.contains(StandardCodes.DESCRIPTION_SEPARATOR)) {
                for (String namePart : name.split(StandardCodes.DESCRIPTION_SEPARATOR)) {
                    addNameToCode("lstr", code, namePart);
                }
            } else {
                addNameToCode("lstr", code, name);
            }
        }

        CLDRFile english = testInfo.getEnglish();
        for (String code : validCodes) {
            if (languageAliases.containsKey(code)) {
                continue;
            }
            String cldrName = english.getName("language", code);
            if (cldrName != null && !cldrName.equals("private-use")) {
                addNameToCode("cldr", code, cldrName);
            }
        }
        // add exceptions
        LanguageTagParser ltp = new LanguageTagParser();
        for (String line : FileUtilities.in(CldrUtility.UTIL_DATA_DIR + "/external/", "alternate_language_names.txt")) {
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null || parts.length == 0) continue;
            String code = parts[0];
            if (!validCodes.contains(code)) {
                if (code.equals("*OMIT")) {
                    System.out.println("Skipping " + line);
                    continue;
                }
                String base = ltp.set(code).getLanguage();
                if (!validCodes.contains(base)) {
                    R2<List<String>, String> alias = languageAliases.get(base);
                    if (alias != null) {
                        code = alias.get0().get(0);
                    } else {
                        System.out.println("Skipping " + line);
                        continue;
                    }
                }
            }
            exceptionCodes.add(toUnderbarLocale(code));
            if (parts.length < 2) {
                continue;
            }
            String name = parts[1];
            if (parts.length > 2) {
                name += ";" + parts[2]; // HACK
            }
            addNameToCode("exception", code, name);
        }
        for (String cldr : GOOGLE_CLDR.values()) {
            String goodCode = toUnderbarLocale(cldr);
            exceptionCodes.add(goodCode);
        }
    }

    private static void addNameToCode(final String type, final String code, String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        String oldCode = languageNameToCode.get(name);
        if (oldCode != null) {
            if (!oldCode.equals(code)) {
                System.out.println("Name Collision! " + type + ": " + name + " <" + oldCode + ", " + code + ">");
            } else {
                return;
            }
        }
        languageNameToCode.put(name, code);
    }

    public static String toGoogleLocaleId(String localeId) {
        // TODO fix to do languages, etc. field by field
        localeId = localeId.replace("-", "_");
        String result = CLDR_GOOGLE.get(localeId);
        result = result == null ? localeId : result;
        return result.replace("_", "-");
    }

    public static String fromGoogleLocaleId(String localeId) {
        localeId = localeId.replace("_", "-");
        // TODO fix to do languages, etc. field by field
        String result = GOOGLE_CLDR.get(localeId);
        result = result == null ? localeId : result;
        return result.replace("-", "_");
    }

    public static String toUnderbarLocale(String localeId) {
        return localeId.replace("-", "_");
    }

    public static String toHyphenLocale(String localeId) {
        return localeId.replace("_", "-");
    }
    
    static class LanguageName extends Row.R2<String,String> {

        public LanguageName(String a, String b) {
            super(a, b);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    public static String getCodeForName(String languageName) {
        return languageNameToCode.get(languageName.toLowerCase(Locale.ENGLISH));
    }

    public static void main(String[] args) {
        CLDRFile english = testInfo.getEnglish();
        System.out.println("Input Name" + "\t" + "Std Code" + "\t" + "Std Name");
        Set<LanguageName> names = new TreeSet();
        for (Entry<String, String> codeName : languageNameToCode.entrySet()) {
            String name = codeName.getKey();
            String code = codeName.getValue();
            if (exceptionCodes.contains(code)) {
                String cldrName = getName(english, code);
                names.add(new LanguageName(code, cldrName));
                if (!name.equalsIgnoreCase(cldrName)) {
                    names.add(new LanguageName(code,name));
                }
            }
        }
        for (LanguageName item : names) {
            String code = item.get0();
            String name = item.get1();
            String cldrName = getName(english, code);
            System.out.println(name.toLowerCase(Locale.ENGLISH) + "\t" + code + "\t" + cldrName);
        }

        System.out.println();
        System.out.println("Input Code" + "\t" + "Bcp47 Code" + "\t" + "CLDR Code" + "\t" + "Google Code" + "\t" + "Std Name");

        Set<LanguageLine> lines = new TreeSet();
        for (Entry<String, R2<List<String>, String>> languageAlias : languageAliases.entrySet()) {
            String badCode = languageAlias.getKey();
            R2<List<String>, String> alias = languageAlias.getValue();

            String goodCode = alias.get0() == null ? "?" : alias.get0().get(0);

            if (exceptionCodes.contains(goodCode)) {
                String cldrName = getName(english, goodCode);
                String googleCode = toGoogleLocaleId(goodCode);
                addLine(lines, badCode, goodCode, googleCode, cldrName);
            }
        }
        for (Entry<String, String> entry : GOOGLE_CLDR.entrySet()) {
            String googleCode = entry.getKey();
            String goodCode = toHyphenLocale(entry.getValue());
            String cldrName = getName(english, goodCode);
            addLine(lines, googleCode, goodCode, googleCode, cldrName);
        }
        for (String goodCode : exceptionCodes) {
            String cldrName = getName(english, goodCode);
            String googleCode = toGoogleLocaleId(goodCode);
            addLine(lines, googleCode, goodCode, googleCode, cldrName);
        }
        for (String cldr : CLDR_GOOGLE.keySet()) {
            String goodCode = toUnderbarLocale(cldr);
            String googleCode = toGoogleLocaleId(goodCode);
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

        LikelySubtags likely = new LikelySubtags(supplementalInfo);
        LanguageTagParser ltp = new LanguageTagParser();
        // get targets of language aliases for macros
        Map<String,String> macroToEncompassed = new HashMap();
        for (Entry<String, R2<List<String>, String>> languageAlias : languageAliases.entrySet()) {
            String reason = languageAlias.getValue().get1();
            if ("macrolanguage".equals(reason)) {
                macroToEncompassed.put(languageAlias.getValue().get0().get(0), languageAlias.getKey());
            }
        }

        System.out.println();
        System.out.println("Bcp47 Code" + "\t" + "Name" + "\t" + "Script\tEncomp.Lang?\tName");

        for (String code : exceptionCodes) {
            String max = likely.maximize(code);

            String script = "?";
            if (max != null) {
                script = ltp.set(max).getScript();
            } else {
                Set<BasicLanguageData> data = supplementalInfo.getBasicLanguageData(code);
                if (data != null) {
                    for (BasicLanguageData item : data) {
                        Set<String> scripts = item.getScripts();
                        if (scripts == null || scripts.size() == 0) continue;
                        script = scripts.iterator().next();
                        Type type = item.getType();
                        if (type == Type.primary) {
                            break;
                        }
                    }
                }
                if (script.equals("?")) {
                    script = EXTRA_SCRIPTS.get(code);
                }
            }

            String encompassed = macroToEncompassed.get(code);
            if (encompassed == null) {
                encompassed = "";
            } else {
                encompassed = "\t" + encompassed + "\t" + getName(english, encompassed);
            }
            System.out.println(toHyphenLocale(code) + "\t" + getName(english, code) + "\t" + script + encompassed);
        }
    }

    public static String getName(CLDRFile english, String goodCode) {
        if (goodCode.startsWith("x_")) {
            return "Private use: " + goodCode.substring(2);
        }
        return english.getName(goodCode);
    }

    public static void printLine(LanguageLine entry) {
        System.out.println(
                entry.get1() // reverse the order: bad
                + "\t" + entry.get0() // bcp47
                + "\t" + entry.get2() // cldr
                + "\t" + entry.get3() // google
                + "\t" + entry.get4()
        );
    }

    private static class LanguageLine extends R5<String, String, String, String, String> {
        public LanguageLine(String a, String b, String c, String d) {
            super(toHyphenLocale(a), b, toUnderbarLocale(a), c, d);
        }
        boolean isStandard() {
            return get0().equals(get2()) && get0().equals(get3());
        }
    }

    public static void addLine(Set<LanguageLine> lines, 
            String badCode,
            String goodCode, 
            String googleCode, 
            String cldrName) {
        // add the various combinations
        lines.add(new LanguageLine(goodCode, goodCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, badCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, googleCode, googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toUnderbarLocale(goodCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toUnderbarLocale(badCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toUnderbarLocale(googleCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toHyphenLocale(goodCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toHyphenLocale(badCode), googleCode, cldrName));
        lines.add(new LanguageLine(goodCode, toHyphenLocale(googleCode), googleCode, cldrName));
    }
}
