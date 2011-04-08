package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PluralSnapshot;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.PluralSnapshot.Integral;
import org.unicode.cldr.util.PluralSnapshot.SnapshotInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class Misc {
    public static void main(String[] args) throws IOException {

        showPlurals();
        if (true) return;

        String[] locales = "zh en es hi fr ar pt ru id bn ur ja de fil sw pa jv ko tr vi it te mr th fa ta pl lah gu my ms uk zh_Hant kn su ml nl az or ro uz bho ps ha ku mad yo ig si mg sd hu am om kk el ne be mai sr cs km as sv mag mwr sn ny ca bg hne tg bgc ii he dcc ug fuv qu rw min af zu mn bjn so ki hr ak tk fi sq da bya sk gn bal nb lua xh bs ht syl ka bjj ban sat hy za luy rn bug bem luo wtm st lo gl ti shn ceb ks mfa ace lt ky bm lg shi tn bcl glk war kok bew kln kam umb bo suk ee kmb ay pam bhk sas bbc swv nso tpi rjb gbm lmn ff kab sl ts ba cv kri gon ndc guz wo tzm mak kfy ln ljp mk efi ibb doi awa mos nyn vmw mer kru lv sid pag gno sck tcy wbq nd lrc ss cgg brh xog nn sg xnr dyu rmt teo kxm mdh hno lu eu khn wbr tsg rej rif brx ilo kbd et ce kg fy hil kj cy ast av ve udm ga tt sah myv tet gaa ady mt dv fj nr is mdf kum kha sm kpv lez pap krc inh oc se tyv zdj dz bi gag to koi lbe mi ab os ty kl gil iu ch fo rm mh chk haw pon lb pau tvl sa kos na ho yap gd uli niu la tkl eo kl".split(" ");
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
        if (true) return;



        StringTransform unicode = Transliterator.getInstance("hex/unicode");
        UnicodeSet exclude = new UnicodeSet("[:bidimirrored:]");
        for (int i = 0; i < 0x110000; ++i) {
            if (exclude.contains(i)) continue;
            String name = UCharacter.getExtendedName(i);
            if (name == null) continue;
            String reverse = name.replaceAll("RIGHT", "LEFT");
            if (reverse.equals(name)) {
                reverse = name.replaceAll("REVERSED ", "");
                if (reverse.equals(name)) continue;
            }
            int rev = UCharacter.getCharFromName(reverse);
            if (rev == -1) continue;
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

    private static void showPlurals() throws IOException {
        TestInfo testInfo = org.unicode.cldr.unittest.TestAll.TestInfo.getInstance();
//        for (Entry<PluralSnapshot, String> ruleEntry : info) {
//            PluralSnapshot ss = ruleEntry.getKey();
//            String rules = ruleEntry.getValue();
//            Set<String> locales = info.getLocales(rules);
//            System.out.println(ss + "\nRules:\t" + rules + "\nLocales:\t" + locales + "\n");
//        }

        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY, "pluralTest.html");

        System.out.println(PluralSnapshot.getDefaultStyles());
        
        out.println("<html><head>" + PluralSnapshot.getDefaultStyles() + "</style><body>");

        PluralSnapshot.writeTables(testInfo.getEnglish(), out);
        out.println("</body></html>");
        out.close();

    }


}
