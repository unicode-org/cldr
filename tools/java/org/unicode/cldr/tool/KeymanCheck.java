package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Map;
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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class KeymanCheck {
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig.getInstance().getSupplementalDataInfo();
    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(FileUtilities.openFile("/Users/markdavis/Google Drive/workspace/DATA/cldr/", "keyman.json"));
        final StringWriter stringWriter = new StringWriter();
        JsonWriter writer = gson.newJsonWriter(stringWriter);
        writer.setIndent("  ");
        prettyprint(reader,writer);
        reader.close();
        writer.close();
        //System.out.println(stringWriter);
    }
    static         LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer();

    static void prettyprint(JsonReader reader, JsonWriter writer) throws IOException {
        boolean afterId = false;
        boolean afterName = false;
        boolean afterLanguage = false;
        String lastId = null;
        Multimap<String,String> languageIdToName = TreeMultimap.create();

        main:
            while (true) {
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
                    switch(name) {
                    case "id": afterId = afterLanguage; break;
                    case "name": afterName = afterLanguage; break;
                    case "languages": afterLanguage = true; break;
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
        TreeSet<String> langs = new TreeSet<>();
        langs.addAll(keyboardLangs.keySet());
        langs.addAll(languageIdToName.keySet());
        for (String lang : langs) {
            PopulationData pop = getPopulationData(lang);
            System.out.println(++count 
                + "\t" + lang 
                + "\t" + (keyboardLangs.containsKey(lang) ? "CLDR" : "")
                + "\t" + (languageIdToName.containsKey(lang) ? "SIL" : "")
                + "\t" + en.getName(lang) 
                + "\t" + (pop != null ? (long) pop.getLiteratePopulation() : "-1")
                );
        }
    }
    
    static LanguageTagParser ltp = new LanguageTagParser();
    static LikelySubtags ls = new LikelySubtags();
    static Map<String,String> unfixedData = new TreeMap<>();
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

