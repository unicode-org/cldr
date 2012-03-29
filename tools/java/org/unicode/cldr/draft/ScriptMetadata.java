package org.unicode.cldr.draft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.lang.UScript;

public class ScriptMetadata {
    public enum Column {
        WR, SAMPLE, ID_USAGE("ID Usage (UAX31)"), RTL("RTL?"), LB_LETTERS("LB letters?"), SHAPING_REQ("Shaping Req?"), IME("IME?"),
        ORIGIN_COUNTRY("Origin Country"),
        DENSITY("~Density");
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
                    throw new IllegalArgumentException("Missing field for " + v + ", may need to add additional column alias");
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
        UNKNOWN("Other"), EXCLUSION("Historic"), LIMITED_USE("Limited Use"), ASPIRATIONAL("Aspirational"), RECOMMENDED("Major Use");
        public final String name;
        private IdUsage(String name) {
            this.name = name;
        }
    }

    public enum Trinary {UNKNOWN, NO, YES}

    public enum Shaping {UNKNOWN, NO, MIN, YES}

    static StandardCodes SC = StandardCodes.make();
    static HashMap<String,String> NAME_TO_REGION_CODE = new HashMap<String,String>();
    static EnumLookup<Shaping> shapingLookup = EnumLookup.of(Shaping.class, null, "n/a", Shaping.UNKNOWN);
    static EnumLookup<Trinary> trinaryLookup = EnumLookup.of(Trinary.class, null, "n/a", Trinary.UNKNOWN);
    static EnumLookup<IdUsage> idUsageLookup = EnumLookup.of(IdUsage.class, null, "n/a", IdUsage.UNKNOWN);
    static {
        for (String region : SC.getAvailableCodes("territory")) {
            String name = (String)(SC.getFullData("territory", region).get(0));
            NAME_TO_REGION_CODE.put(name.toUpperCase(Locale.ENGLISH), region);
        }
        NAME_TO_REGION_CODE.put("UNKNOWN", "ZZ");
        NAME_TO_REGION_CODE.put("", "ZZ");
        NAME_TO_REGION_CODE.put("N/A", "ZZ");
    }

    public static class Info {
        public final int rank;
        public final String sampleChar;
        public final IdUsage idUsage;
        public final Trinary rtl;
        public final Trinary lbLetters;
        public final Shaping shapingReq;
        public final Trinary ime;
        public final int density;
        public final String originCountry;
        private Info(String[] items) {
            // 3,Han,Hani,1.1,"74,519",字,5B57,East_Asian,Recommended,no,Yes,no,Yes
            rank = Column.WR.getInt(items, 999);
            sampleChar = Column.SAMPLE.getItem(items);
            idUsage = idUsageLookup.forString(Column.ID_USAGE.getItem(items));
            rtl = trinaryLookup.forString(Column.RTL.getItem(items));
            lbLetters = trinaryLookup.forString(Column.LB_LETTERS.getItem(items)); 
            shapingReq = shapingLookup.forString(Column.SHAPING_REQ.getItem(items));
            ime = trinaryLookup.forString(Column.IME.getItem(items));
            density = Column.DENSITY.getInt(items, -1);
            final String countryRaw = Column.ORIGIN_COUNTRY.getItem(items);
            String country = NAME_TO_REGION_CODE.get(countryRaw.toUpperCase(Locale.ENGLISH));
            if (country == null) {
                errors.add("Can't map " + countryRaw + " to country/region");
            }
            originCountry = country == null ? "ZZ" : country;
        }
//        public Trinary parseTrinary(Column title, String[] items) {
//            return Trinary.valueOf(fix(title.getItem(items)).toUpperCase(Locale.ENGLISH));
//        }
        String fix(String in) {
            return in.toUpperCase(Locale.ENGLISH).replace("N/A", "UNKNOWN").replace("?","UNKNOWN").replace("RTL","YES");
        }
        public String toString() {
            return rank 
            + "\t" + sampleChar
            + "\t" + SC.getFullData("territory", originCountry).get(0) + "\t(" + originCountry + ")"
            + "\t" + idUsage
            + "\t" + rtl
            + "\t" + lbLetters
            + "\t" + shapingReq
            + "\t" + ime
            + "\t" + density
            ;
        }
    }

    public static Set<String> errors = new LinkedHashSet<String>();
    static HashMap<String,Integer> titleToColumn = new HashMap<String,Integer>();

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
                    data.put(script2, info);
                }
            }
            return true;
        }
        @Override
        public MyFileReader process(Class classLocation, String fileName) {
            super.process(classLocation, fileName);
            return this;
        }

    }
    static Relation<String, String> EXTRAS = Relation.of(new HashMap<String,Set<String>>(), HashSet.class);
    static {
        EXTRAS.put("Hani", "Hans");
        EXTRAS.put("Hani", "Hant");
        EXTRAS.put("Hang", "Kore");
        EXTRAS.put("Hira", "Jpan");
    }
    static Map<String,Info> data = new MyFileReader().process(ScriptMetadata.class, "/org/unicode/cldr/util/data/Script_Metadata.csv").data;

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
