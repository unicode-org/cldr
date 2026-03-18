package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.UnicodeRelation;

public class CompareExemplars {
    static final Set<String> Recommended =
            ImmutableSet.of(
                    "Zyyy", "Zinh", "Arab", "Armn", "Beng", "Bopo", "Cyrl", "Deva", "Ethi", "Geor",
                    "Grek", "Gujr", "Guru", "Hang", "Hani", "Hebr", "Hira", "Kana", "Knda", "Khmr",
                    "Laoo", "Latn", "Mlym", "Mymr", "Orya", "Sinh", "Taml", "Telu", "Thaa", "Thai",
                    "Tibt");

    public static void main(String[] args) {
        // [:Identifier_Status=Allowed:] doesn't work
        UnicodeSet skip = new UnicodeSet("[[:nfkcqc=n:][:Lu:][:^XID_Continue:]]");
        UnicodeSet IdAllowed =
                new UnicodeSet(
                                "['\\-.0-\\:A-Z_a-z·À-Ö Ø-öø-ıĴ-ľŁ-ňŊ-žƏƠơƯưǍ-ǜ Ǟ-ǣǦ-ǰǴǵǸ-țȞȟȦ-ȳəʻʼˬ̀-̄̆-̌̏-̛̣̑̓̔-̵̸̨̭̮̰̱̹͂ͅ ͵ͻ-ͽΆΈ-ΊΌΎ-ΡΣ-ώϼ-џҊ-ӿ Ԑ-ԩԮԯԱ-Ֆՙա-ֆ֊ִא-תׯ-״ؠ-ؿ ف-ٕ٠-٩ٰ-ٲٴٹ-ڍڏ-ڠڢ-ۓەۥ ۦۮ-ۿݐ-ޱࡰ-ࢇࢉ-ࢎࢠ-ࢬࢲࢵ-ࣉँ-्ॏ ॐॖॗॠ-ॣ०-९ॱ-ॷॹ-ॿঁ-ঃঅ-ঌ এঐও-নপ-রলশ-হ়-ৄেৈো-ৎৗ ৠ-ৣ০-ৱ৾ਁ-ਃਅ-ਊਏਐਓ-ਨਪ-ਰ ਲਵਸਹ਼ਾ-ੂੇੈੋ-੍ੜ੦-ੴઁ-ઃઅ-ઍ એ-ઑઓ-નપ-રલળવ-હ઼-ૅે-ૉો-્ ૐૠ-ૣ૦-૯ૺ-૿ଁ-ଃଅ-ଌଏଐଓ-ନ ପ-ରଲଳଵ-ହ଼-ୃେୈୋ-୍୕-ୗୟ-ୡ ୦-୯ୱஂஃஅ-ஊஎ-ஐஒ-கஙசஜஞடண தந-பம-ஹா-ூெ-ைொ-்ௐௗ௦-௯ఁ-ఌ ఎ-ఐఒ-నప-ళవ-హ఼-ౄె-ైొ-్ౕౖ ౝౠౡ౦-౯ಀಂಃಅ-ಌಎ-ಐಒ-ನಪ-ಳ ವ-ಹ಼-ೄೆ-ೈೊ-್ೕೖೝೠ-ೣ೦-೯ ೱ-ೳഀംഃഅ-ഌഎ-ഐഒ-ഺഽ-ൃെ-ൈൊ-ൎ ൔ-ൗൠൡ൦-൯ൺ-ൿංඃඅ-ඎඑ-ඖක-ඥ ට-නඳ-රලව-ෆ්ා-ුූෘ-ෞෲก-าิ-ฺ เ-๎๐-๙ກຂຄຆ-ຊຌ-ຣລວ-າິ-ຽ ເ-ໄໆ່-໎໐-໙ໞໟༀ་༠-༩༵༷༾-ག ང-ཇཉ-ཌཎ-དན-བམ-ཛཝ-ཨཪ-ཬཱིེུ-ྀྂ-྄྆-ྒྔ-ྗྙ-ྜྞ-ྡྣ-ྦྨ-ྫྭ-ྸྺ-ྼ࿆ က-၉ၐ-ႝჇჍა-ჰჷ-ჺჽ-ჿሀ-ቈቊ-ቍ ቐ-ቖቘቚ-ቝበ-ኈኊ-ኍነ-ኰኲ-ኵኸ-ኾ ዀዂ-ዅወ-ዖዘ-ጐጒ-ጕጘ-ፚ፝-፟ᎀ-ᎏ ក-អឥ-ឧឩ-ឳា-៍័្ៗៜ០-៩Ა-Ჺ Ჽ-ᲿḀ-ẙẞẠ-ỹἀ-ἕἘ-Ἕἠ-ὅὈ-Ὅ ὐ-ὗὙὛὝὟ-ὰὲὴὶὸὺὼᾀ-ᾴᾶ-Ὰ ᾼῂ-ῄῆ-ῈῊῌῐ-ῒῖ-Ὶῠ-ῢῤ-Ὺ Ῥῲ-ῴῶ-ῸῺῼ‐’‧ⴧⴭⶀ-ⶖⶠ-ⶦⶨ-ⶮ ⶰ-ⶶⶸ-ⶾⷀ-ⷆⷈ-ⷎⷐ-ⷖⷘ-ⷞ々-〇 ぁ-ゖ゙゚ゝゞ゠-ヾㄅ-ㄭㄯㆠ-ㆿ㐀-䶿一-鿿 ꙿꜗ-ꜟꞈꞍꞒꞓꞪꟀ-ꟊꟐꟑꟓꟕ-ꟙꧧ-ꧾ ꩠ-ꩶꩺ-ꩿꬁ-ꬆꬉ-ꬎꬑ-ꬖꬠ-ꬦꬨ-ꬮ ꭦꭧ가-힣﨎﨏﨑﨓﨔﨟﨡﨣﨤﨧-﨩𑌁𑌃𖿰𖿱𑌻𑌼 𛄟-𛄢𛄲𛅐-𛅒𛅕𛅤-𛅧𝼀-𝼞𝼥-𝼪𞂏𞟠-𞟦 𞟨-𞟫𞟭𞟮𞟰-𞟾𠀀-𪛟𪜀-𫜹𫝀-𫠝𫠠-𬺡𬺰-𮯠 𰀀-𱍊𱍐-𲎯]")
                        .removeAll(skip)
                        .freeze();
        UnicodeRelation<String> charactersToLocales = new UnicodeRelation<>();
        UnicodeSet allExemplars = new UnicodeSet();

        for (String subDir : ImmutableSet.of("common/main", "exemplars/main")) {
            gatherExemplars(subDir, charactersToLocales, allExemplars);
        }
        allExemplars.removeAll(skip);
        Map<Integer, UnicodeSet> scriptCodeToCharacters = new TreeMap<>();
        for (String s : allExemplars) {
            int scriptCode = UScript.getScript(s.codePointAt(0));
            if (scriptCode == UScript.INHERITED
                    || scriptCode == UScript.COMMON) { // skip Zyyy, Zinh
                continue;
            }
            UnicodeSet chars = scriptCodeToCharacters.get(scriptCode);
            if (chars == null) {
                scriptCodeToCharacters.put(scriptCode, chars = new UnicodeSet());
            }
            chars.add(s);
        }
        for (Entry<Integer, UnicodeSet> entry : scriptCodeToCharacters.entrySet()) {
            final Integer scriptCode = entry.getKey();
            String scriptName = UScript.getShortName(scriptCode);
            if (!Recommended.contains(scriptName)) {
                continue;
            }
            final UnicodeSet cldr = entry.getValue().freeze();
            final UnicodeSet scriptSet =
                    new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, scriptCode).freeze();
            UnicodeSet scriptAllowed = new UnicodeSet(scriptSet).retainAll(IdAllowed).freeze();

            show(
                    scriptName,
                    "➕CLDR\t➖Allowed",
                    new UnicodeSet(cldr).removeAll(scriptAllowed),
                    charactersToLocales);
            show(
                    scriptName,
                    "➖CLDR\t➕Allowed",
                    new UnicodeSet(scriptAllowed).removeAll(cldr),
                    null);
            show(
                    scriptName,
                    "➕CLDR\t➕Allowed",
                    new UnicodeSet(cldr).retainAll(scriptAllowed),
                    charactersToLocales);
            // show(scriptName, "➖CLDR\t➖Allowed", new UnicodeSet(scriptAllowed).retainAll(cldr),
            // null);
        }
    }

    public static void show(
            final String scriptName,
            String title,
            UnicodeSet allowed,
            UnicodeRelation<String> charactersToLocales) {
        if (!allowed.isEmpty()) {
            final String pattern = allowed.toPattern(false);
            final int allowedSize = allowed.size();
            System.out.println(
                    scriptName
                            + "\t"
                            + title
                            + "\t"
                            + allowedSize
                            + "\t"
                            + (allowedSize < 20000 ? pattern : pattern.substring(0, 500) + "…")
                            + "\t"
                            + showLocales(allowed, charactersToLocales));
        }
    }

    private static String showLocales(
            UnicodeSet allowed, UnicodeRelation<String> charactersToLocales) {
        StringBuilder temp = new StringBuilder();
        Map<String, UnicodeSet> compact = new LinkedHashMap<>();
        for (String s : allowed) {
            try {
                Set<String> locales = charactersToLocales.get(s);
                String set =
                        locales.size() > 10
                                ? "(" + locales.size() + ")"
                                : "[" + Joiner.on(" ").join(charactersToLocales.get(s)) + "]";
                UnicodeSet us = compact.get(set);
                if (us == null) {
                    compact.put(set, us = new UnicodeSet());
                }
                us.add(s);
            } catch (Exception e) {
                int debug = 0;
            }
        }
        for (Entry<String, UnicodeSet> entry : compact.entrySet()) {
            if (temp.length() != 0) {
                temp.append("; ");
            }
            temp.append(entry.getValue().toPattern(false) + "⬅︎" + entry.getKey());
        }
        return temp.toString();
    }

    private static void gatherExemplars(
            String subdir, UnicodeRelation<String> results, UnicodeSet allExemplars) {
        Path aPath = CLDRConfig.getInstance().getCldrBaseDirectory().toPath().resolve(subdir);
        Factory factory = Factory.make(aPath.toString(), ".*");
        char lastLetter = '@';
        for (String locale : factory.getAvailable()) {
            CLDRFile cldrFile = factory.make(locale, false);
            UnicodeSet exemplars =
                    cldrFile.getExemplarSet(ExemplarType.main, WinningChoice.WINNING);
            if (exemplars == null) {
                continue;
            }
            UnicodeSet aux = cldrFile.getExemplarSet(ExemplarType.auxiliary, WinningChoice.WINNING);
            if (aux != null && !aux.isEmpty()) {
                exemplars = new UnicodeSet(exemplars).addAll(aux);
            }
            if (locale.charAt(0) != lastLetter) {
                System.out.println(locale);
                lastLetter = locale.charAt(0);
            }
            results.addAll(exemplars, locale);
            allExemplars.addAll(flatten(exemplars));
        }
    }

    private static UnicodeSet flatten(UnicodeSet exemplars) {
        Collection<String> strings = exemplars.strings();
        if (strings == null || strings.isEmpty()) {
            return exemplars;
        }
        for (String s : ImmutableSet.copyOf(strings)) {
            exemplars.addAll(s);
        }
        return new UnicodeSet(exemplars).removeAll(strings);
    }
}
