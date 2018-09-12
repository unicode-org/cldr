package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PluralSnapshot;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class Misc {
    public static void main(String[] args) throws IOException {
        showDefaultContent("bn", "sw", "mr", "ta", "ms", "am", "af", "zu", "et", "is", "ur", "te", "gu", "kn", "ml",
            "gl", "eu");
        showSortKey();
        showNumberSamples();
        showDateSamples();
        showExemplarSize();

        doNFC();
        showPlurals();

        String[] locales = "zh en es hi fr ar pt ru id bn ur ja de fil sw pa jv ko tr vi it te mr th fa ta pl lah gu my ms uk zh_Hant kn su ml nl az or ro uz bho ps ha ku mad yo ig si mg sd hu am om kk el ne be mai sr cs km as sv mag mwr sn ny ca bg hne tg bgc ii he dcc ug fuv qu rw min af zu mn bjn so ki hr ak tk fi sq da bya sk gn bal nb lua xh bs ht syl ka bjj ban sat hy za luy rn bug bem luo wtm st lo gl ti shn ceb ks mfa ace lt ky bm lg shi tn bcl glk war kok bew kln kam umb bo suk ee kmb ay pam bhk sas bbc swv nso tpi rjb gbm lmn ff kab sl ts ba cv kri gon ndc guz wo tzm mak kfy ln ljp mk efi ibb doi awa mos nyn vmw mer kru lv sid pag gno sck tcy wbq nd lrc ss cgg brh xog nn sg xnr dyu rmt teo kxm mdh hno lu eu khn wbr tsg rej rif brx ilo kbd et ce kg fy hil kj cy ast av ve udm ga tt sah myv tet gaa ady mt dv fj nr is mdf kum kha sm kpv lez pap krc inh oc se tyv zdj dz bi gag to koi lbe mi ab os ty kl gil iu ch fo rm mh chk haw pon lb pau tvl sa kos na ho yap gd uli niu la tkl eo kl"
            .split(" ");
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> scripts = new LinkedHashSet<String>();
        for (String locale : locales) {
            Set<BasicLanguageData> items = sdi.getBasicLanguageData(locale);
            if (items == null) {
                System.out.println(locale + "\t?");
                continue;
            }
            scripts.clear();
            for (BasicLanguageData item : items) {
                if (item.getType() == BasicLanguageData.Type.secondary) {
                    continue;
                }
                Set<String> script2 = item.getScripts();
                if (script2 != null) {
                    scripts.addAll(script2);
                }
            }
            if (scripts.size() == 0) {
                System.out.println(locale + "\t?");
                continue;
            }
            if (locale.equals("zh")) {
                scripts.remove("Hant");
            } else if (locale.equals("zh_Hant")) {
                scripts.add("Hant");
            }
            System.out.println(locale + "\t" + CollectionUtilities.join(scripts, " "));
        }

        StringTransform unicode = Transliterator.getInstance("hex/unicode");
        UnicodeSet exclude = new UnicodeSet("[:bidimirrored:]");
        for (int i = 0; i < 0x110000; ++i) {
            if (exclude.contains(i))
                continue;
            String name = UCharacter.getExtendedName(i);
            if (name == null)
                continue;
            String reverse = name.replaceAll("RIGHT", "LEFT");
            if (reverse.equals(name)) {
                reverse = name.replaceAll("REVERSED ", "");
                if (reverse.equals(name))
                    continue;
            }
            int rev = UCharacter.getCharFromName(reverse);
            if (rev == -1)
                continue;
            System.out.println(
                unicode.transform(UTF16.valueOf(i))
                    + "\t" + UTF16.valueOf(i)
                    + "\t" + name
                    + "\t" + UTF16.valueOf(rev)
                    + "\t" + unicode.transform(UTF16.valueOf(rev))
                    + "\t" + reverse);
        }
        System.out.println(Locale.SIMPLIFIED_CHINESE);
        System.out.println(Locale.TRADITIONAL_CHINESE);
        for (String s : StandardCodes.make().getGoodCountries()) {
            System.out.println(s + "\t" + ULocale.getDisplayCountry("und-" + s, ULocale.ENGLISH));
        }
    }

    private static void showDefaultContent(String... strings) {
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        final CLDRConfig info = ToolConfig.getToolInstance();
        CLDRFile english = info.getEnglish();
        Set<String> defaultContents = sdi.getDefaultContentLocales();
        for (String string : strings) {
            String defCon = null;
            for (String dc : defaultContents) {
                if (string.equals(LocaleIDParser.getParent(dc))) {
                    defCon = dc;
                    break;
                }
            }
            System.out.println(string + "\t" + defCon + "\t" + english.getName(defCon));
        }
    }

    private static void showSortKey() {
        String[] tests = "a ä A ぁ あ ァ ｧ ア ｱ ㋐".split(" ");
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
        c.setStrength(RuleBasedCollator.QUATERNARY);
        c.setCaseLevel(true);
        c.setHiraganaQuaternary(true);
        for (String test : tests) {
            for (boolean caseLevel : new boolean[] { false, true }) {
                c.setCaseLevel(caseLevel);
                for (boolean hiraganaQuaternary : new boolean[] { false, true }) {
                    c.setHiraganaQuaternary(hiraganaQuaternary);
                    System.out.print((caseLevel ? "Cl\t" : "\t"));
                    System.out.print((hiraganaQuaternary ? "Hl\t" : "\t"));
                    System.out.print(test + "\t");
                    RawCollationKey key = c.getRawCollationKey(test, null);
                    for (byte item : key.bytes) {
                        System.out.print(Integer.toHexString(0xFF & item) + "\t");
                    }
                    System.out.println();
                }
            }
        }
    }

    private static void showNumberSamples() {
        String[] tests = { "a$b", "abcd_defg-hi", "abcd-defg$xy", "ab-d$efg-419", "root", "", "und" };
        for (String test : tests) {
            ULocale locale = ULocale.forLanguageTag(test);
            System.out.println(test + " -> " + locale);
        }
        DecimalFormat df = new DecimalFormat("***");
        for (int i = 10; i > -10; --i) {
            String sample = df.format(1.23456789 * Math.pow(10, i));
            System.out.println(sample);
        }
    }

    private static void showDateSamples() {
        Map<String, Row.R2<Integer, Integer>> specials = Builder.with(new TreeMap<String, Row.R2<Integer, Integer>>())
            .put("full-date", Row.of(DateFormat.FULL, DateFormat.NONE))
            .put("long-date", Row.of(DateFormat.LONG, DateFormat.NONE))
            .put("medium-date", Row.of(DateFormat.MEDIUM, DateFormat.NONE))
            .put("short-date", Row.of(DateFormat.SHORT, DateFormat.NONE))
            .put("full-time", Row.of(DateFormat.NONE, DateFormat.FULL))
            .put("long-time", Row.of(DateFormat.NONE, DateFormat.LONG))
            .put("medium-time", Row.of(DateFormat.NONE, DateFormat.MEDIUM))
            .put("short-time", Row.of(DateFormat.NONE, DateFormat.SHORT))
            .freeze();
        Date sample = new Date(2011 - 1900, 12 - 1, 30, 14, 45, 59);
        final ULocale english = ULocale.ENGLISH;
        final ULocale otherLocale = new ULocale("el");
        DateTimePatternGenerator englishGenerator = DateTimePatternGenerator.getInstance(english);
        DateTimePatternGenerator otherGenerator = DateTimePatternGenerator.getInstance(otherLocale);
        for (String dp : new String[] { "d", "h", "H", "hm", "Hm", "Hms", "hms", "hmv", "Hmv", "hv", "Hv", "M", "Md",
            "MEd", "MMM", "MMMd",
            "MMMEd", "ms", "y", "yM", "yMd", "yMEd", "yMMM", "yMMMd", "yMMMEd", "yMMMM", "yQ", "yQQQ", "EEEd",
            "full-date", "long-date", "medium-date", "short-date", "full-time", "long-time", "medium-time",
            "short-time",
            "MMMM", "MMMMd", "E", "Ed", "GGGGyMd", "GGGGyMMMMEEEEdd", "GGGGyyyyMMMMd", "HHmm", "HHmmss", "HHmmZ",
            "Hmm", "MMd", "MMdd", "MMMdd", "MMMEEEd", "MMMMdd", "MMMMEd", "MMMMEEEd", "mmss", "yMMMMccccd", "yyMM",
            "yyMMdd", "yyMMM", "yyMMMd", "yyMMMEEEd", "yyQ", "yyQQQQ", "yyyy", "yyyyLLLL", "yyyyM", "yyyyMEEEd",
            "yyyyMM", "yyyyMMM", "yyyyMMMM", "yyyyMMMMEEEEd", "yyyyQQQQ",
            "hmz", "hz", "LLL", "LLLL", "MMMMEEEEd", "yMMMMd", "yMMMMEEEEd"
        }) {
            final String formattedEnglish = getFormatted(specials, sample, dp, english, englishGenerator);
            final String formattedOther = getFormatted(specials, sample, dp, otherLocale, otherGenerator);
            System.out.println(dp + "\t«" + formattedEnglish + "»\t«" + formattedOther + "»");
        }
    }

    private static String getFormatted(Map<String, Row.R2<Integer, Integer>> specials, Date sample, String dp,
        ULocale ulocale,
        DateTimePatternGenerator generator) {
        Row.R2<Integer, Integer> special = specials.get(dp);
        DateFormat df;
        if (special != null) {
            df = DateFormat.getDateTimeInstance(special.get0(), special.get1(), ulocale);
        } else {
            String pat = generator.getBestPattern(dp);
            df = new SimpleDateFormat(pat, ulocale);
        }
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String formatted = df.format(sample);
        return formatted;
    }

    private static void showExemplarSize() {
        final CLDRConfig info = ToolConfig.getToolInstance();
        CLDRFile english = info.getEnglish();
        Factory factory = info.getCldrFactory();
        SupplementalDataInfo dataInfo = info.getSupplementalDataInfo();
        Map<String, Map<String, R2<List<String>, String>>> type_tag_replacement = dataInfo.getLocaleAliasInfo();
        Map<String, R2<List<String>, String>> lang2replacement = type_tag_replacement.get("language");

        LanguageTagParser ltp = new LanguageTagParser();
        String[] locales = "en ru nl en-GB fr de it pl pt-BR es tr th ja zh-CN zh-TW ko ar bg sr uk ca hr cs da fil fi hu id lv lt no pt-PT ro sk sl es-419 sv vi el iw fa hi am af et is ms sw zu bn mr ta eu fr-CA gl zh-HK ur gu kn ml te"
            .split(" ");
        Set<String> nameAndInfo = new TreeSet<String>(info.getCollator());
        for (String localeCode : locales) {
            String baseLanguage = ltp.set(localeCode).getLanguage();
            R2<List<String>, String> temp = lang2replacement.get(baseLanguage);
            if (temp != null) {
                baseLanguage = temp.get0().get(0);
            }
            String englishName = english.getName(baseLanguage);
            CLDRFile cldrFile = factory.make(baseLanguage, false);
            UnicodeSet set = cldrFile.getExemplarSet("", WinningChoice.WINNING);
            int script = -1;
            for (String s : set) {
                int cp = s.codePointAt(0);
                script = UScript.getScript(cp);
                if (script != UScript.COMMON && script != UScript.INHERITED) {
                    break;
                }
            }
            String nativeName = cldrFile.getName(baseLanguage);
            nameAndInfo
                .add(englishName + "\t" + nativeName + "\t" + baseLanguage + "\t" + UScript.getShortName(script));
        }

        for (String item : nameAndInfo) {
            System.out.println(item);
        }
        // for (String localeCode : locales) {
        // String baseLanguage = ltp.set(localeCode).getLanguage();
        // R2<List<String>, String> temp = lang2replacement.get(baseLanguage);
        // if (temp != null) {
        // baseLanguage = temp.get0().get(0);
        // }
        // int size = -1;
        //
        // try {
        // CLDRFile cldrFile = factory.make(baseLanguage, false);
        // UnicodeSet set = cldrFile.getExemplarSet("", WinningChoice.WINNING);
        // size = set.size();
        // } catch (Exception e) {
        // }
        //
        // System.out.println(localeCode + "\t" + size);
        // }
    }

    static final Normalizer2 nfc = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
    static final Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.DECOMPOSE);

    private static void doNFC() {

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 0x110000; ++i) {
            b.setLength(0);
            b.appendCodePoint(i);
            boolean isNfd = nfd.isNormalized(b);
            boolean isNfdNew = IsNfd.isNormalizedUpTo(b) < 0;
            if (isNfd != isNfdNew) {
                IsNfd.isNormalizedUpTo(b);
                throw new IllegalArgumentException();
            }
        }
        String[] tests = { "Mark", "Μάρκος", nfd.normalize("Μάρκος") };
        long[] times = new long[2];
        // warmup
        for (String test : tests) {
            times[0] = times[1] = Long.MIN_VALUE;
            time(nfc, test, 10000000, "NFC", times);
            time(nfd, test, 10000000, "NFD", times);
            time(test, 10000000, "NFDx", times);
        }
        System.out.println();
        for (String test : tests) {
            times[0] = times[1] = Long.MIN_VALUE;
            time(nfc, test, 100000000, "NFC", times);
            time(nfd, test, 100000000, "NFD", times);
            time(test, 100000000, "NFDx", times);
        }
    }

    // static class ByteTrie {
    // static class Block {
    // byte[] values = new byte[128];
    // }
    // int[] index;
    // byte[][] blocks;
    // static class Builder {
    // Map<Block,Integer> backIndex = new HashMap<Block, Integer>();
    // Block block = new Block();
    // int pos = 0;
    // void append(byte item) {
    // if (pos >= 128) {
    // // if block is in backIndex, use the index, otherwise add and create new
    // Block
    // } else {
    // block.values[pos++] = item;
    // }
    // }
    // }
    // }

    static class IsNfd {
        static final byte[] info = new byte[0x110000];
        static {
            for (int i = 0; i < 0x110000; ++i) {
                int nfdqc = UCharacter.getIntPropertyValue(i, UProperty.NFD_QUICK_CHECK);
                if (nfdqc == 0) {
                    info[i] = (byte) 0xFF;
                    continue;
                }
                int ccc = UCharacter.getIntPropertyValue(i, UProperty.CANONICAL_COMBINING_CLASS);
                info[i] = (byte) ccc;

                // if (ccc != 0) {
                // info[i] = (byte) ccc;
                // continue;
                // }
                // int gc = UCharacter.getIntPropertyValue(i,
                // UProperty.GENERAL_CATEGORY);
                // if (gc != UCharacter.UNASSIGNED) {
                // info[i] = (byte) 0;
                // continue;
                // }
                // int nc = UCharacter.getIntPropertyValue(i,
                // UProperty.NONCHARACTER_CODE_POINT);
                // if (nc == yes) {
                // info[i] = (byte) 0;
                // continue;
                // }
                // info[i] = (byte) 0xFF;
            }
        }

        public static String normalize(CharSequence s) {
            int normalizedUpTo = isNormalizedUpTo(s);
            if (normalizedUpTo < 0) {
                return s.toString();
            }
            return nfd.normalizeSecondAndAppend(
                new StringBuilder(s.subSequence(0, normalizedUpTo)),
                s.subSequence(normalizedUpTo, s.length()))
                .toString();
        }

        public static int isNormalizedUpTo(CharSequence s) {
            final int length = s.length();
            int lastNonStarterIndex = 0;
            int lastByte = 0;
            int i;
            for (i = 0; i < length; ++i) {
                int cp = s.charAt(i);
                if (cp >= 0xD800 && cp < 0xDC00) {
                    cp = Character.codePointAt(s, i);
                }
                int b = info[cp] & 0xFF;
                if (b == 0) {
                    lastNonStarterIndex = i;
                    lastByte = b;
                } else if (b == lastByte) {
                    // do nothing, common case
                } else if (b < lastByte || b == 0xFF) {
                    return lastNonStarterIndex; // failure
                } else {
                    lastByte = b; // increasing CCC, ok
                }
                if (cp > 0xFFFF) {
                    ++i;
                }
            }
            return -1;
        }
    }

    private static void time(String test, int iterations, String name, long[] times) {
        System.out.println(test);
        System.gc();
        System.gc();
        System.gc();

        Timer t = new Timer();
        t.start();
        for (int i = iterations; i > 0; --i) {
            IsNfd.isNormalizedUpTo(test);
        }
        long isNfc = t.getDuration();
        if (times[0] != Long.MIN_VALUE) {
            System.out.println("\tis" + name + ":\t" + t.toString(iterations, times[0]));
        } else {
            System.out.println("\tis" + name + ":\t" + t.toString(iterations));
        }
        times[0] = isNfc;

        System.gc();
        System.gc();
        System.gc();
        t.start();
        for (int i = iterations; i > 0; --i) {
            IsNfd.normalize(test);
        }
        long toNfc = t.getDuration();
        if (times[1] != Long.MIN_VALUE) {
            System.out.println("\tto" + name + ":\t" + t.toString(iterations, times[1]));
        } else {
            System.out.println("\tto" + name + ":\t" + t.toString(iterations));
        }
        times[1] = toNfc;
    }

    private static void time(Normalizer2 nfx, String test, int iterations, String name, long[] times) {
        System.out.println(test);
        System.gc();
        System.gc();
        System.gc();

        Timer t = new Timer();
        t.start();
        for (int i = iterations; i > 0; --i) {
            nfx.isNormalized(test);
        }
        long isNfc = t.getDuration();
        if (times[0] != Long.MIN_VALUE) {
            System.out.println("\tis" + name + ":\t" + t.toString(iterations, times[0]));
        } else {
            System.out.println("\tis" + name + ":\t" + t.toString(iterations));
        }
        times[0] = isNfc;

        System.gc();
        System.gc();
        System.gc();
        t.start();
        for (int i = iterations; i > 0; --i) {
            nfx.normalize(test);
        }
        long toNfc = t.getDuration();
        if (times[1] != Long.MIN_VALUE) {
            System.out.println("\tto" + name + ":\t" + t.toString(iterations, times[1]));
        } else {
            System.out.println("\tto" + name + ":\t" + t.toString(iterations));
        }
        times[1] = toNfc;
    }

    private static void showPlurals() throws IOException {
        CLDRConfig testInfo = org.unicode.cldr.tool.ToolConfig.getToolInstance();
        // for (Entry<PluralSnapshot, String> ruleEntry : info) {
        // PluralSnapshot ss = ruleEntry.getKey();
        // String rules = ruleEntry.getValue();
        // Set<String> locales = info.getLocales(rules);
        // System.out.println(ss + "\nRules:\t" + rules + "\nLocales:\t" +
        // locales + "\n");
        // }

        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "pluralTest.html");

        System.out.println(PluralSnapshot.getDefaultStyles());

        out.println("<html><head>" + PluralSnapshot.getDefaultStyles() + "</style><body>");

        PluralSnapshot.writeTables(testInfo.getEnglish(), out);
        out.println("</body></html>");
        out.close();

    }

}
