package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PluralSnapshot;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.PluralSnapshot.Integral;
import org.unicode.cldr.util.PluralSnapshot.SnapshotInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Trie2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class Misc {
    public static void main(String[] args) throws IOException {
        showExemplarSize();
        if (true)
            return;

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
        if (true)
            return;

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
        for (String s : ULocale.getISOCountries()) {
            System.out.println(s + "\t" + ULocale.getDisplayCountry("und-" + s, ULocale.ENGLISH));
        }
    }

    private static void showExemplarSize() {
        final TestInfo info = TestAll.TestInfo.getInstance();
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
            nameAndInfo.add(englishName + "\t" + nativeName + "\t" + baseLanguage + "\t" + UScript.getShortName(script));
        }
        
        for (String item : nameAndInfo) {
            System.out.println(item);
        }
//        for (String localeCode : locales) {
//            String baseLanguage = ltp.set(localeCode).getLanguage();
//            R2<List<String>, String> temp = lang2replacement.get(baseLanguage);
//            if (temp != null) {
//                baseLanguage = temp.get0().get(0);
//            }
//            int size = -1;
//
//            try {
//                CLDRFile cldrFile = factory.make(baseLanguage, false);
//                UnicodeSet set = cldrFile.getExemplarSet("", WinningChoice.WINNING);
//                size = set.size();
//            } catch (Exception e) {
//            }
//
//            System.out.println(localeCode + "\t" + size);
//        }
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
        TestInfo testInfo = org.unicode.cldr.unittest.TestAll.TestInfo.getInstance();
        // for (Entry<PluralSnapshot, String> ruleEntry : info) {
        // PluralSnapshot ss = ruleEntry.getKey();
        // String rules = ruleEntry.getValue();
        // Set<String> locales = info.getLocales(rules);
        // System.out.println(ss + "\nRules:\t" + rules + "\nLocales:\t" +
        // locales + "\n");
        // }

        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY, "pluralTest.html");

        System.out.println(PluralSnapshot.getDefaultStyles());

        out.println("<html><head>" + PluralSnapshot.getDefaultStyles() + "</style><body>");

        PluralSnapshot.writeTables(testInfo.getEnglish(), out);
        out.println("</body></html>");
        out.close();

    }

}
