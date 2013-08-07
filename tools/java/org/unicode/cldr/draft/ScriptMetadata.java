package org.unicode.cldr.draft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.tool.CountryCodeConverter;
import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.lang.UScript;

public class ScriptMetadata {
    private static final String DATA_FILE = "/org/unicode/cldr/util/data/Script_Metadata.csv";

    // To get the data, go do the Script MetaData spreadsheet, and Download As Comma Separated Items into DATA_FILE
    // Then you'll run GenerateScriptMetadata
    public enum Column {
        // must match the spreadshee header (caseless compare) or have the alternate header as an argument.
        // doesn't have to be in order
        WR, SAMPLE, ID_USAGE("ID Usage (UAX31)"), RTL("RTL?"), LB_LETTERS("LB letters?"), SHAPING_REQ("Shaping Req?"), IME(
            "IME?"),
        ORIGIN_COUNTRY("Origin Country"),
        DENSITY("~Density"),
        LIKELY_LANGUAGE("Likely Language"),
        HAS_CASE("Has Case?"), ;
        int columnNumber = -1;
        final Set<String> names = new HashSet<String>();

        Column(String... alternateNames) {
            names.add(this.name());
            for (String name : alternateNames) {
                names.add(name.toUpperCase(Locale.ENGLISH));
            }
        }

        static void setColumns(String[] headers) {
            for (int i = 0; i < headers.length; ++i) {
                String header = headers[i].toUpperCase(Locale.ENGLISH);
                for (Column v : values()) {
                    if (v.names.contains(header)) {
                        v.columnNumber = i;
                    }
                }
            }
            for (Column v : values()) {
                if (v.columnNumber == -1) {
                    throw new IllegalArgumentException("Missing field for " + v
                        + ", may need to add additional column alias");
                }
            }
        }

        String getItem(String[] items) {
            return items[columnNumber];
        }

        int getInt(String[] items, int defaultValue) {
            final String item = getItem(items);
            return item.isEmpty() || item.equalsIgnoreCase("n/a") ? defaultValue : Integer.parseInt(item);
        }
    }

    public enum IdUsage {
        UNKNOWN("Other"), EXCLUSION("Historic"), LIMITED_USE("Limited Use"), ASPIRATIONAL("Aspirational"), RECOMMENDED(
            "Major Use");
        public final String name;

        private IdUsage(String name) {
            this.name = name;
        }
    }

    public enum Trinary {
        UNKNOWN, NO, YES
    }

    public enum Shaping {
        UNKNOWN, NO, MIN, YES
    }

    static StandardCodes SC = StandardCodes.make();
    // static HashMap<String,String> NAME_TO_REGION_CODE = new HashMap<String,String>();
    // static HashMap<String,String> NAME_TO_LANGUAGE_CODE = new HashMap<String,String>();
    static EnumLookup<Shaping> shapingLookup = EnumLookup.of(Shaping.class, null, "n/a", Shaping.UNKNOWN);
    static EnumLookup<Trinary> trinaryLookup = EnumLookup.of(Trinary.class, null, "n/a", Trinary.UNKNOWN);
    static EnumLookup<IdUsage> idUsageLookup = EnumLookup.of(IdUsage.class, null, "n/a", IdUsage.UNKNOWN);
    static {
        // addNameToCode("language", NAME_TO_LANGUAGE_CODE);
        // // NAME_TO_LANGUAGE_CODE.put("", "und");
        // NAME_TO_LANGUAGE_CODE.put("N/A", "und");
        // addSynonym(NAME_TO_LANGUAGE_CODE, "Ancient Greek", "Ancient Greek (to 1453)");
        // //addSynonym(NAME_TO_LANGUAGE_CODE, "Khmer", "Cambodian");
        // addSynonym(NAME_TO_LANGUAGE_CODE, "Old Irish", "Old Irish (to 900)");

        // addNameToCode("region", NAME_TO_REGION_CODE);
        // // NAME_TO_REGION_CODE.put("UNKNOWN", "ZZ");
        // // NAME_TO_REGION_CODE.put("", "ZZ");
        // NAME_TO_REGION_CODE.put("N/A", "ZZ");
        // addSynonym(NAME_TO_REGION_CODE, "Laos", "Lao People's Democratic Republic");
    }

    public static void addNameToCode(String type, Map<String, String> hashMap) {
        for (String language : SC.getAvailableCodes(type)) {
            Map<String, String> fullData = SC.getLStreg().get(type).get(language);
            String name = (String) (fullData.get("Description"));
            hashMap.put(name.toUpperCase(Locale.ENGLISH), language);
        }
    }

    public static void addSynonym(Map<String, String> map, String newTerm, String oldTerm) {
        String code = map.get(oldTerm.toUpperCase(Locale.ENGLISH));
        map.put(newTerm.toUpperCase(Locale.ENGLISH), code);
    }

    public static class Info implements Comparable<Info> {
        public final int rank;
        public final String sampleChar;
        public final IdUsage idUsage;
        public final Trinary rtl;
        public final Trinary lbLetters;
        public final Trinary hasCase;
        public final Shaping shapingReq;
        public final Trinary ime;
        public final int density;
        public final String originCountry;
        public final String likelyLanguage;

        private Info(String[] items) {
            // 3,Han,Hani,1.1,"74,519",字,5B57,East_Asian,Recommended,no,Yes,no,Yes
            rank = Column.WR.getInt(items, 999);
            sampleChar = Column.SAMPLE.getItem(items);
            idUsage = idUsageLookup.forString(Column.ID_USAGE.getItem(items));
            rtl = trinaryLookup.forString(Column.RTL.getItem(items));
            lbLetters = trinaryLookup.forString(Column.LB_LETTERS.getItem(items));
            shapingReq = shapingLookup.forString(Column.SHAPING_REQ.getItem(items));
            ime = trinaryLookup.forString(Column.IME.getItem(items));
            hasCase = trinaryLookup.forString(Column.HAS_CASE.getItem(items));
            density = Column.DENSITY.getInt(items, -1);

            final String countryRaw = Column.ORIGIN_COUNTRY.getItem(items);
            String country = CountryCodeConverter.getCodeFromName(countryRaw);
            // NAME_TO_REGION_CODE.get(countryRaw.toUpperCase(Locale.ENGLISH));
            if (country == null) {
                errors.add("Can't map " + countryRaw + " to country/region");
            }
            originCountry = country == null ? "ZZ" : country;

            final String likelyLanguageRaw = Column.LIKELY_LANGUAGE.getItem(items);
            String language = LanguageCodeConverter.getCodeForName(likelyLanguageRaw);
            if (language == null && !likelyLanguageRaw.equals("n/a")) {
                errors.add("Can't map " + likelyLanguageRaw + " to language");
                LanguageCodeConverter.getCodeForName(likelyLanguageRaw); // for debugging
            }
            likelyLanguage = language == null ? "und" : language;
        }

        public Info(Info other, String string) {
            rank = other.rank;
            sampleChar = other.sampleChar;
            idUsage = other.idUsage;
            rtl = other.rtl;
            lbLetters = other.lbLetters;
            hasCase = other.hasCase;
            shapingReq = other.shapingReq;
            ime = string.equals("IME:YES") ? Trinary.YES : other.ime;
            density = other.density;
            originCountry = other.originCountry;
            likelyLanguage = other.likelyLanguage;
        }

        // public Trinary parseTrinary(Column title, String[] items) {
        // return Trinary.valueOf(fix(title.getItem(items)).toUpperCase(Locale.ENGLISH));
        // }
        String fix(String in) {
            return in.toUpperCase(Locale.ENGLISH).replace("N/A", "UNKNOWN").replace("?", "UNKNOWN")
                .replace("RTL", "YES");
        }

        public String toString() {
            return rank
                + "\tSample: " + sampleChar
                + "\tCountry: " + getName("territory", originCountry) + " (" + originCountry + ")"
                + "\tLanguage: " + getName("language", likelyLanguage) + " (" + likelyLanguage + ")"
                + "\tId: " + idUsage
                + "\tRtl: " + rtl
                + "\tLb: " + lbLetters
                + "\tShape: " + shapingReq
                + "\tIme: " + ime
                + "\tCase: " + hasCase
                + "\tDensity: " + density;
        }

        public Object getName(String type, String code) {
            List fullData = SC.getFullData(type, code);
            if (fullData == null) {
                return "unavailable";
            }
            return fullData.get(0);
        }

        @Override
        public int compareTo(Info o) {
            // we don't actually care what the comparison value is, as long as it is transitive and consistent with equals.
            return toString().compareTo(o.toString());
        }
    }

    public static Set<String> errors = new LinkedHashSet<String>();
    static HashMap<String, Integer> titleToColumn = new HashMap<String, Integer>();

    private static class MyFileReader extends FileUtilities.SemiFileReader {
        public Map<String, Info> data = new HashMap<String, Info>();

        @Override
        protected boolean isCodePoint() {
            return false;
        }

        @Override
        protected String[] splitLine(String line) {
            return FileUtilities.splitCommaSeparated(line);
        };

        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            if (items[0].startsWith("For help")) {
                return true; // header lines
            }
            if (items[0].equals("WR")) {
                Column.setColumns(items);
                return true;
            }
            Info info;
            try {
                info = new Info(items);
            } catch (Exception e) {
                errors.add(e.getClass().getName() + "\t" + e.getMessage() + "\t" + Arrays.asList(items));
                return true;
            }

            String script = items[2];
            data.put(script, info);
            Set<String> extras = EXTRAS.get(script);
            if (extras != null) {
                for (String script2 : extras) {
                    Info info2 = info;
                    if (script2.equals("Jpan")) {
                        // HACK
                        info2 = new Info(info, "IME:YES");
                    }
                    data.put(script2, info2);
                }
            }
            return true;
        }

        @Override
        public MyFileReader process(Class<?> classLocation, String fileName) {
            super.process(classLocation, fileName);
            return this;
        }

    }

    static Relation<String, String> EXTRAS = Relation.of(new HashMap<String, Set<String>>(), HashSet.class);
    static {
        EXTRAS.put("Hani", "Hans");
        EXTRAS.put("Hani", "Hant");
        EXTRAS.put("Hang", "Kore");
        EXTRAS.put("Hira", "Jpan");
    }
    static Map<String, Info> data = new MyFileReader().process(ScriptMetadata.class, DATA_FILE).data;

    public static Info getInfo(String s) {
        return data.get(s);
    }

    public static Set<String> getScripts() {
        return data.keySet();
    }

    public static Info getInfo(int i) {
        return data.get(UScript.getShortName(i));
    }
}
