package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.Keyboard;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class KeymanCheck {
    static Splitter SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final List<String> board = SPACE.splitToList(
        "aa-ET ab-GE an-ES apa-US ar-AE ar-DZ ar-IL ar-IQ ar-JO ar-KW ar-LB ar-LY ar-QA ar-SA ar-SD ar-SY ar-TN ar-YE arn-CL ast-ES av-RU awa-IN ay-BO az-AZ az-IR ba-RU bal-PK bar-AT bfy-IN bgc-IN bgq-IN bgq-PK bh-IN bho-IN bho-MU bho-NP bi-VU bm-ML bn-IN bns-IN bo-CN br-FR brx-Beng-IN brx-Deva-IN brx-Latn-IN bs-BA bug-ID ca-AD ca-ES ce-RU ceb-PH ch-GU cho-US chr-US cmn-Hans-SG cmn-Hant-HK co-FR cr-Cans-CA cr-Latn-CA cv-RU cy-GB dak-US de-AT de-CH de-LI de-LU dhd-IN din-SD doi-Arab-IN doi-Deva-IN dv-MV dz-BT ee-GH en-BD en-BM en-HK en-IE en-MY en-NZ en-PK en-SD en-SG en-TT en-TZ en-UG eo-001 es-419 es-BO es-CL es-CO es-CR es-CU es-DO es-EC es-GQ es-GT es-HN es-NI es-PA es-PE es-PR es-PY es-SV es-UY es-VE esu-US fa-AF ff-011 fj-FJ fo-FO fr-BE fr-CH fr-CI fr-LU fr-SN fy-NL ga-IE gbm-IN gd-GB gn-PG gv-IM gyn-GY ha-NG haw-US hi-Latn-IN hil-PH hmn-CN hne-IN ho-PG hoj-IN hsb-DE hu-SK hy-AM hz-NA ia-001 ig-NG ii-CN ik-US ilo-PH it-CH it-MT iu-Cans-CA iu-Latn-CA jbo-001 kfy-IN kg-CD ki-KE kj-AO kl-GL kok-Deva-IN kok-Knda-IN kok-Latn-IN kr-NG kri-SL ks-Arab-IN ks-Deva-IN ktu-CD ku-Arab-IQ ku-Latn-IQ kv-RU ky-KG la-001 lb-LU lg-UG li-NL lis-CN lkt-US lmn-IN lmo-IT ln-CD lu-CD mad-ID mag-IN mag-NP mai-NP mg-MG mh-MH mi-NZ min-ID ml-IN mn-CN mni-Beng-IN mrj-RU ms-SG mt-MT mtr-IN mup-IN mus-US mwr-IN na-NR nah-MX nap-IT nd-ZW nds-DE ne-IN new-NP nl-BE nn-NO noe-IN nr-ZA nso-ZA nv-US ny-MW oc-FR oj-CA om-ET os-RU pa-Arab-PK pap-CW pms-IT ps-AF pt-AO pt-TL qu-BO quc-GT raj-PK rm-CH rn-BI ro-MD ru-BY ru-KZ ru-UA sa-IN sah-RU sat-Beng-IN sat-Deva-IN sat-Latn-IN sat-Olck-IN sc-IT sck-IN scn-IT sco-GB sd-Arab-IN sd-Deva-IN sd-PK se-NO see-US sg-CF sgs-LT sjp-BD sm-WS sn-ZW sr-Cyrl-ME sr-Cyrl-RS sr-Latn-ME ss-ZA st-ZA sv-FI sw-KE sw-TZ sw-UG syl-BD ta-LK ta-SG tcy-IN tet-TL ti-ET tk-TM tn-BW tn-ZA to-TO tpi-PG ts-ZA tt-RU tw-GH ty-PF ug-CN ur-IN var-IN ve-ZA vec-IT wa-BE war-PH wen-DE wo-SN xh-ZA xnr-IN yi-US yo-NG");

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(FileUtilities.openFile("/Users/markdavis/Google Drive/workspace/DATA/cldr/", "keyman.json"));
        final StringWriter stringWriter = new StringWriter();
        JsonWriter writer = gson.newJsonWriter(stringWriter);
        writer.setIndent("  ");
        prettyprint(reader, writer);
        reader.close();
        writer.close();
        //System.out.println(stringWriter);
    }

    static LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer();

    static void prettyprint(JsonReader reader, JsonWriter writer) throws IOException {
        boolean afterId = false;
        boolean afterName = false;
        boolean afterLanguage = false;
        String lastId = null;
        Multimap<String, String> languageIdToName = TreeMultimap.create();

        main: while (true) {
            JsonToken token = reader.peek();
            switch (token) {
            case BEGIN_ARRAY:
                reader.beginArray();
                writer.beginArray();
                break;
            case END_ARRAY:
                reader.endArray();
                writer.endArray();
                afterLanguage = false;
                break;
            case BEGIN_OBJECT:
                reader.beginObject();
                writer.beginObject();
                break;
            case END_OBJECT:
                reader.endObject();
                writer.endObject();
                break;
            case NAME:
                String name = reader.nextName();
                switch (name) {
                case "id":
                    afterId = afterLanguage;
                    break;
                case "name":
                    afterName = afterLanguage;
                    break;
                case "languages":
                    afterLanguage = true;
                    break;
                }
                writer.name(name);
                break;
            case STRING:
                String s = reader.nextString();
                writer.value(s);
                if (afterId) {
                    lastId = ltc.transform(s);
                    afterId = false;
                } else if (afterName) {
                    languageIdToName.put(lastId, s);
                    afterName = false;
                }
                break;
            case NUMBER:
                String n = reader.nextString();
                writer.value(new BigDecimal(n));
                break;
            case BOOLEAN:
                boolean b = reader.nextBoolean();
                writer.value(b);
                break;
            case NULL:
                reader.nextNull();
                writer.nullValue();
                break;
            case END_DOCUMENT:
                break main;
            }
        }
        int count = 0;
        CLDRFile en = CLDRConfig.getInstance().getEnglish();
        TreeMultimap<String, String> keyboardLangs = TreeMultimap.create();
        for (String kpid : Keyboard.getPlatformIDs()) {
            for (String kid : Keyboard.getKeyboardIDs(kpid)) {
                keyboardLangs.put(ltp.set(kid).getLanguageScript(), kid);
            }
        }

        LikelySubtags likely = new LikelySubtags();
        LanguageTagParser ltp = new LanguageTagParser();

        Set<String> minBoard = new HashSet<>();
        for (String boardId : board) {
            ltp.set(boardId);
            ltp.setRegion("");
            String min = ltc.transform(ltp.toString());
            minBoard.add(min == null ? boardId : min);
        }

        TreeSet<String> langs = new TreeSet<>();
        langs.addAll(keyboardLangs.keySet());
        langs.addAll(languageIdToName.keySet());
        langs.addAll(minBoard);
        for (String lang : langs) {
            PopulationData pop = getPopulationData(lang);
            System.out.println(
//                ++count 
//                 + "\t" + 
                en.getName(lang)
                    + "\t" + lang
                    + "\t" + (pop != null ? (long) pop.getLiteratePopulation() : "-1")
                    + "\t" + (keyboardLangs.containsKey(lang) ? "CLDR" : "")
                    + "\t" + (languageIdToName.containsKey(lang) ? "SIL" : "")
                    + "\t" + (minBoard.contains(lang) ? "GB" : ""));
        }
    }

    static LanguageTagParser ltp = new LanguageTagParser();
    static LikelySubtags ls = new LikelySubtags();
    static Map<String, String> unfixedData = new TreeMap<>();
    static {
        for (String s : SUPPLEMENTAL_DATA_INFO.getLanguagesForTerritoriesPopulationData()) {
            String fixed = ltc.transform(s);
            if (!fixed.equals(s)) {
                unfixedData.put(fixed, s);
                System.out.println(s + " => " + fixed);
            }
        }
    }

    private static PopulationData getPopulationData(String lang) {
        PopulationData pop = SUPPLEMENTAL_DATA_INFO.getLanguagePopulationData(lang);
        if (pop == null) {
            String unfixed = unfixedData.get(lang);
            if (unfixed != null) {
                pop = SUPPLEMENTAL_DATA_INFO.getLanguagePopulationData(unfixed);
            }
        }
//        if (pop == null) {
//            final String maximize = ls.maximize(lang);
//            if (maximize != null) {
//                ltp.set(maximize);
//                SUPPLEMENTAL_DATA_INFO.getLanguagePopulationData(ltp.getLanguageScript());
//            }
//        }
        return pop;
    }
}
