package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.CountryCodeConverter;
import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.ChainedMap.M3;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class Unlocode {

    private static final Charset LATIN1 = Charset.forName("ISO8859-1");

    public interface Mergeable<T> {
        T merge(T a);
    }

    public static class Iso3166_2Data implements Mergeable<Iso3166_2Data> {
        public final Set<String> names;

        public Iso3166_2Data(String... name) {
            this(Arrays.asList(name));
        }

        public Iso3166_2Data(Collection<String> names) {
            this.names = Collections.unmodifiableSet(new LinkedHashSet<String>(names));
        }

        @Override
        public String toString() {
            return names.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return names.equals((Iso3166_2Data) obj);
        }

        @Override
        public int hashCode() {
            return names.hashCode();
        }

        @Override
        public Iso3166_2Data merge(Iso3166_2Data b) {
            LinkedHashSet<String> set = new LinkedHashSet<String>(names);
            set.addAll(b.names);
            return new Iso3166_2Data(set);
        }
    }

    public static class LocodeData implements Mergeable<LocodeData>, Comparable<LocodeData> {
        public final String locode;
        public final Set<String> names;
        public final String subdivision;
        public final float north;
        public final float east;

        public LocodeData(String locode, String name, String subdivision, float north, float east) {
            this(locode, Arrays.asList(name), subdivision, north, east);
        }

        public LocodeData(String locode, Collection<String> names, String subdivision, float north, float east) {
            this.locode = locode;
            this.names = Collections.unmodifiableSet(new LinkedHashSet<String>(names));
            this.subdivision = subdivision;
            this.north = north;
            this.east = east;
        }

        @Override
        public String toString() {
            return names + ", " + locode + ", " + subdivision + ", " + north + ", " + east;
        }

        /**
         * Warning, must never have locode datas with the same locode and different other data.
         */
        @Override
        public int compareTo(LocodeData o) {
            // TODO Auto-generated method stub
            return locode.compareTo(o.locode);
        }

        /**
         * Warning, must never have locode datas with the same locode and different other data.
         */
        @Override
        public boolean equals(Object obj) {
            LocodeData other = (LocodeData) obj;
            return locode.equals(other.locode);
        }

        @Override
        public int hashCode() {
            return locode.hashCode();
        }

        @Override
        public LocodeData merge(LocodeData other) {
            if (locode.equals(other.locode)
                && subdivision.equals(other.subdivision)
                && north == other.north
                && east == other.east) {
                LinkedHashSet<String> set = new LinkedHashSet<String>(names);
                set.addAll(other.names);
                return new LocodeData(locode, set, subdivision, north, east);
            }
            throw new IllegalArgumentException("Can't merge " + this + " with " + other);
        }

    }

    static Map<String, LocodeData> locodeToData = new HashMap<String, LocodeData>();
    static Relation<String, LocodeData> nameToLocodeData = Relation.of(new HashMap<String, Set<LocodeData>>(), HashSet.class);
    static Map<String, Iso3166_2Data> iso3166_2Data = new HashMap<String, Iso3166_2Data>();
    static Relation<String, String> ERRORS = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);

    static {
        // read the data
        try {
            loadIso();
            iso3166_2Data = Collections.unmodifiableMap(iso3166_2Data);
            load(1);
            load(2);
            load(3);
            // load exceptions
            try {
                BufferedReader br = FileReaders.openFile(CldrUtility.class,
                    "data/external/alternate_locode_name.txt");
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    int hash = line.indexOf('#');
                    if (hash >= 0) {
                        line = line.substring(0, hash);
                    }
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (line.equals("EOF")) {
                        break;
                    }
                    String[] parts = line.split("\\s*;\\s*");
                    //System.out.println(Arrays.asList(parts));
                    String locode = parts[0].replace(" ", "");
                    if (locode.length() != 5) {
                        throw new IllegalArgumentException(line);
                    }
                    String alternateName = parts[1];
                    LocodeData locodeData = locodeToData.get(locode);
                    putCheckingDuplicate(locodeToData, locode, new LocodeData(
                        locode, alternateName, locodeData.subdivision, locodeData.north, locodeData.east));
                }
                br.close();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
            for (LocodeData s : locodeToData.values()) {
                for (String name : s.names) {
                    nameToLocodeData.put(name, s);
                }
            }
            nameToLocodeData.freeze();
            locodeToData = Collections.unmodifiableMap(locodeToData);
            ERRORS.freeze();
        } catch (IOException e) {
        }
    }

    /* http://www.unece.org/fileadmin/DAM/cefact/locode/unlocode_manual.pdf
    //
     * 0 ,
     * 1 "AD",
     * 2 "SJL",
     * 3 "Sant Julià de Lòria",
     * 4 "Sant Julia de Loria",
     * 5 ?,
     * 6 "--3-----",
     * 7 "RL",
     * 8 "1101",
     * 9 ,
     * 10 "4228N 00130E",""
            0 Column Change
            X Marked for deletion in the next issue
            1 Country code
                    "XZ" - no country
            2 Column LOCODE
            3 Column Name
            4 Column Name Without Diacritics
            5 Column Subdivision
            6 Column Function
            7 Column Status
            8 Column Date
            9 Column IATA
            10 Latitude/Longitude
            Torbay: 47°39′N 052°44′W "4739N 05244W"
     */

    //    public static class FieldData<K extends Enum<K>> {
    //        private List<EnumMap<K,String>> data;
    //        public FieldData(Class<K> classInstance, BufferedReader r, String filename) {
    //            data = new ArrayList<EnumMap<K,String>>();
    //            FileUtilities.FileProcessor myReader = new FileUtilities.FileProcessor() {
    //                @Override
    //                protected boolean handleLine(int lineCount, String line) {
    //                    // TODO Auto-generated method stub
    //                    return super.handleLine(lineCount, line);
    //                }
    //            };
    //            myReader.process(r, filename);
    //            //new EnumMap<K, String>(classInstance);
    //        }
    //    }

    enum SubdivisionFields {
        Subdivision_category, Code_3166_2, Subdivision_name, Language_code, Romanization_system, Parent_subdivision
    }

    public static void loadIso() throws IOException {
        BufferedReader br = FileReaders.openFile(CldrUtility.class,
            "data/external/subdivisionData.txt", CldrUtility.UTF8);
        while (true) {
            // Subdivision category TAB 3166-2 code TAB Subdivision name TAB Language code TAB Romanization system TAB Parent subdivision

            String line = br.readLine();
            if (line == null) {
                break;
            }
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] list = line.split("\t");
            String locode = list[SubdivisionFields.Code_3166_2.ordinal()].trim();
            if (locode.endsWith("*")) {
                locode = locode.substring(0, locode.length() - 1);
            }
            String bestName = list[SubdivisionFields.Subdivision_name.ordinal()].trim();
            //            if (!locode.contains("-")) {
            //                //System.out.println("*skipping: " + locode);
            //                continue;
            //            }
            //
            //            String names = list[5];
            //            String[] name = names.split("\\+");
            //            String bestName = null;
            //            for (String namePair : name) {
            //                if (bestName == null) {
            //                    bestName = namePair.split("=")[1];
            //                } else if (namePair.startsWith("en=")) {
            //                    bestName = namePair.split("=")[1];
            //                    break;
            //                }
            //            }
//            System.out.println("\t" + locode + "\t" + bestName + "\t\t\t");

            putCheckingDuplicate(iso3166_2Data, locode, new Iso3166_2Data(bestName));
        }
        br.close();
    }

    public static void load(int file) throws IOException {
        BufferedReader br =
            //CldrUtility.getUTF8Data(
            FileReaders.openFile(CldrUtility.class,
                "data/external/2013-1_UNLOCODE_CodeListPart" + file + ".csv",
                LATIN1);
        M3<String, String, Boolean> nameToAlternate = ChainedMap.of(new TreeMap<String, Object>(), new TreeMap<String, Object>(), Boolean.class);
        Output<String> tempOutput = new Output<String>();

        String oldCountryCode = null;
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] list = CldrUtility.splitCommaSeparated(line);
            String change = list[0];
            String locSuffix = list[2];
            if (change.equals("X")) {
                continue;
            }
            String countryCode = list[1];
            if (!countryCode.equals(oldCountryCode)) {
                nameToAlternate.clear();
                oldCountryCode = countryCode;
            }
            String name = list[3];
            String name2 = list[4];

            if (change.equals("=")) {
                String[] names = name.split("\\s*=\\s*");
                if (names.length != 2) {
                    throw new IllegalArgumentException();
                }
                nameToAlternate.put(names[1], names[0], Boolean.TRUE);
                if (!name.equals(name2)) {
                    names = name2.split("\\s*=\\s*");
                    if (names.length != 2) {
                        throw new IllegalArgumentException();
                    }
                    nameToAlternate.put(names[1], names[0], Boolean.TRUE);
                }
                continue;
            }
            if (locSuffix.isEmpty()) {
                if (!name.startsWith(".")) {
                    // System.out.println("*** Skipping " + line);
                }
                continue;
            }

            name = removeParens(name, tempOutput);
            String name3 = tempOutput.value;
            name2 = removeParens(name2, tempOutput);
            String name4 = tempOutput.value;

            String subdivision = list[5];
            if (!subdivision.isEmpty()) {
                subdivision = countryCode + "-" + subdivision;
                if (getIso3166_2Data(subdivision) == null) {
                    ERRORS.put(subdivision, "Missing subdivision " + subdivision + " on line " + line);
                }
            }
            String latLong = list[10];
            float latN = 0;
            float longE = 0;
            if (!latLong.isEmpty()) {
                String[] latlong = latLong.split(" ");
                latN = parse(latlong[0]);
                longE = parse(latlong[1]);
            }
            String locode = countryCode + locSuffix;
            LocodeData locodeData = new LocodeData(locode, name, subdivision, latN, longE);
            putCheckingDuplicate(locodeToData, locode, locodeData);
            Map<String, Boolean> alternates = nameToAlternate.get(name);
            if (alternates != null) {
                for (String alt : alternates.keySet()) {
                    putCheckingDuplicate(locodeToData, locode, new LocodeData(locode, alt, subdivision, latN, longE));
                }
            }
            if (!name2.equals(name)) {
                putCheckingDuplicate(locodeToData, locode, new LocodeData(locode, name2, subdivision, latN, longE));
                alternates = nameToAlternate.get(name2);
                if (alternates != null) {
                    for (String alt : alternates.keySet()) {
                        putCheckingDuplicate(locodeToData, locode, new LocodeData(locode, alt, subdivision, latN, longE));
                    }
                }
            }
            if (name3 != null) {
                putCheckingDuplicate(locodeToData, locode, new LocodeData(locode, name3, subdivision, latN, longE));
            }
            if (name4 != null && !name4.equals(name3)) {
                putCheckingDuplicate(locodeToData, locode, new LocodeData(locode, name4, subdivision, latN, longE));
            }
        }
        br.close();
    }

    public static String removeParens(String name, Output<String> tempOutput) {
        int paren = name.indexOf("(");
        tempOutput.value = null;
        if (paren > 0) {
            int paren2 = name.indexOf(")", paren);
            if (paren2 < 0) {
                paren2 = name.length();
            }
            // if the parens start with (ex, then it appears to be a safe alias.
            // if not, we don't know, since the UN format is ambiguous
            // sometimes yes: «Ras Zubbaya (Ras Dubayyah)»
            // sometimes no: «Challis Venture (oil terminal)»
            String temp = name.substring(paren + 1, paren2);
            if (temp.startsWith("ex ")) {
                tempOutput.value = temp.substring(3);
            }
            name = paren2 == name.length()
                ? name.substring(0, paren).trim()
                : (name.substring(0, paren) + name.substring(paren2 + 1)).replace("  ", " ").trim();
            //System.out.println("«" + orginal + "» => «" + name + "», «" + tempOutput.value + "»");
        }
        return name;
    }

    public static <K, V extends Mergeable<V>> void putCheckingDuplicate(Map<K, V> map, K key, V value) {
        V old = map.get(key);
        if (old != null && !old.equals(value)) {
            try {
                map.put(key, old.merge(value));
            } catch (Exception e) {
                ERRORS.put(key.toString(), "Can't merge records: " + key + "\t" + e.getMessage());
            }
        } else {
            map.put(key, value);
        }
    }

    public static LocodeData getLocodeData(String unlocode) {
        return locodeToData.get(unlocode);
    }

    public static Set<Entry<String, LocodeData>> entrySet() {
        return locodeToData.entrySet();
    }

    public static Set<String> getAvailable() {
        return locodeToData.keySet();
    }

    public static Iso3166_2Data getIso3166_2Data(String unlocode) {
        return iso3166_2Data.get(unlocode);
    }

    public static Set<Entry<String, Iso3166_2Data>> isoEntrySet() {
        return iso3166_2Data.entrySet();
    }

    public static Set<String> getAvailableIso3166_2() {
        return iso3166_2Data.keySet();
    }

    public static Relation<String, String> getLoadErrors() {
        return ERRORS;
    }

    private static float parse(String string) {
        int len = string.length();
        char dir = string.charAt(len - 1);
        int result0 = Integer.parseInt(string.substring(0, len - 1));
        float fract = (result0 % 100) / 60f;
        fract = ((int) (fract * 100 + 0.499999999f)) / 100f;
        float result = (result0 / 100) + fract;
        return dir == 'N' || dir == 'E' ? result : -result;
    }

    public static void main(String[] args) throws IOException {
        Relation<String, LocodeData> countryNameToCities = Relation.of(new TreeMap<String, Set<LocodeData>>(), TreeSet.class);
        Set<String> errors = new TreeSet<String>();
        loadCitiesCapitals(countryNameToCities, errors);
        loadCitiesOver1M(countryNameToCities, errors);
        SupplementalDataInfo supp = ToolConfig.getToolInstance().getSupplementalDataInfo();
        Set<String> missing = new TreeSet<String>(
            supp.getBcp47Keys().get("tz"));
        Set<String> already = new TreeSet<String>();

        for (Entry<String, LocodeData> entry : countryNameToCities.keyValueSet()) {
            String countryName = entry.getKey();
            LocodeData item = entry.getValue();
            String firstName = item.names.iterator().next();
            LinkedHashSet<String> remainingNames = new LinkedHashSet<String>(item.names);
            remainingNames.remove(firstName);
            String lowerLocode = item.locode.toLowerCase(Locale.ENGLISH);
            String info = countryName
                + "\t" + (remainingNames.isEmpty() ? "" : remainingNames)
                + "\t" + (item.subdivision.isEmpty() ? "" : "(" + item.subdivision + ")");

            if (missing.contains(lowerLocode)) {
                missing.remove(lowerLocode);
                already.add(lowerLocode);
                continue;
            }
            System.out.println("<location type=\"" + lowerLocode
                + "\">" + firstName
                + "</location>\t<!--" + info
                + "-->");
        }
        System.out.println();
        System.out.println(CollectionUtilities.join(errors, "\n"));
        System.out.println();
        showLocodes("In exemplars already:", already);
        System.out.println();
        showLocodes("In exemplars but not new cities:", missing);
        System.out.println();
        for (Entry<String, Set<String>> errorEntry : ERRORS.keyValuesSet()) {
            System.out.println(errorEntry.getKey() + "\t" + errorEntry.getValue());
        }
        if (true) return;

        int i = 0;
        //        for (String s : new TreeSet<String>(Unlocode.getAvailableIso3166_2())) {
        //            System.out.println((i++) + "\t" + s + "\t" + Unlocode.getIso3166_2Data(s));
        //            //if (i > 1000) break;
        //        }
        for (String s : new TreeSet<String>(Unlocode.getAvailable())) {
            if (!s.startsWith("GT")) {
                continue;
            }
            System.out.println((i++) + "\t" + s + "\t" + Unlocode.getLocodeData(s));
            //if (i > 1000) break;
        }

        //        Set<String> KNOWN_ERRORS = new HashSet<String>(Arrays.asList("AR-LA", "DE-BR"));
        //
        //        for (Entry<String, Set<String>> s : getLoadErrors().keyValuesSet()) {
        //            String key = s.getKey();
        //            Set<String> values = s.getValue();
        //            if (KNOWN_ERRORS.contains(key)) {
        //                System.out.println("# Known error\t" + key);
        //                continue;
        //            }
        //            String s2 = values.toString();
        //            System.out.println(key + "\t" + s2.substring(0,Math.min(256, s2.length())) + "…");
        //        }
    }

    public static void showLocodes(String title, Set<String> already) {
        Set<String> noData = new TreeSet<String>();
        Set<String> noData2 = new TreeSet<String>();
        for (String locode : already) {
            String upperLocode = locode.toUpperCase(Locale.ENGLISH);
            String countryName = ULocale.getDisplayCountry("und-" + upperLocode.substring(0, 2), ULocale.ENGLISH);
            LocodeData data = locodeToData.get(upperLocode);
            if (data == null) {
                if (locode.length() == 5) {
                    noData.add(locode);
                } else {
                    noData2.add(locode);
                }
            } else {
                System.out.println(title + "\t" + countryName + "\t" + data);
            }
        }
        System.out.println("* No locode data, len 5:\t" + noData);
        System.out.println("* No locode data:\t" + noData2);
    }

    public static int loadCitiesOver1M(Relation<String, LocodeData> countryNameToCities, Set<String> errors2) throws IOException {
        int i = 1;

        BufferedReader br = FileReaders.openFile(CldrUtility.class, "data/external/Cities-Over1M.txt");
        main: while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t");
            //System.out.println(Arrays.asList(parts));
            String cityName = parts[2];
            String subdivision = null;
            int bracket = cityName.indexOf('[');
            if (bracket > 0) {
                try {
                    subdivision = cityName.substring(bracket + 1, cityName.indexOf(']'));
                    cityName = cityName.substring(0, bracket);
                } catch (Exception e) {
                    throw new IllegalArgumentException(cityName);
                }
            }
            String countryName = parts[3];
            add(countryName, subdivision, cityName, countryNameToCities, errors2);

            //                String countryCode = CountryCodeConverter.getCodeFromName(countryName);
            //                if (countryCode == null) {
            //                    System.out.println("*** Couldn't find country " + countryName);
            //                    continue;
            //                }
            //                Set<LocodeData> locodeDatas = nameToLocodeData.get(cityName);
            //                if (locodeDatas == null) {
            //                    System.out.println((i++) + " Couldn't find city " + cityName + " in " + countryName);
            //                    continue;
            //                } else if (locodeDatas.size() == 1) {
            //                    add(countryNameToCities,locodeDatas.iterator().next());
            //                } else  {
            //                    Set<LocodeData> rem = new LinkedHashSet();
            //                    for (LocodeData x : locodeDatas) {
            //                        if (x.subdivision.equals(subdivision)) {
            //                            add(countryNameToCities, x);
            //                            continue main;
            //                        }
            //                        if (x.subdivision.startsWith(countryCode)) {
            //                            rem.add(x);
            //                        }
            //                    }
            //                    if (rem.size() != 1) {
            //                        System.out.println((i++) + " No single record for " + cityName + "\t" + rem);
            //                    } else {
            //                        add(countryNameToCities, rem.iterator().next());
            //                    }
            //                }
        }
        br.close();
        return i;
    }

    public static int loadCitiesCapitals(Relation<String, LocodeData> countryNameToCities, Set<String> errors2) throws IOException {
        int i = 1;
        BufferedReader br = FileReaders.openFile(CldrUtility.class, "data/external/Cities-CountryCapitals.txt");
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(" *\t *");
            //System.out.println(Arrays.asList(parts));
            String cityName = parts[0];
            String countryName = parts[1];
            add(countryName, null, cityName, countryNameToCities, errors2);
        }
        br.close();
        return i;
    }

    static final Set<String> noncountries = new HashSet<String>(Arrays.asList(
        "United States Virgin Islands", "Akrotiri and Dhekelia", "Easter Island", "Somaliland", "Northern Cyprus", "Nagorno-Karabakh Republic", "Abkhazia",
        "Transnistria", "South Ossetia"));

    static final Transform<String, String> REMOVE_ACCENTS = Transliterator.getInstance("nfd;[:mn:]remove");

    static void add(String countryName, String subdivision, String cityName, Relation<String, LocodeData> countryNameToCities, Set<String> errors2) {
        String countryCode = CountryCodeConverter.getCodeFromName(countryName);
        if (countryCode == null) {
            if (noncountries.contains(countryName)) {
                return; // skip
            }
            errors2.add("**Couldn't find country " + countryName);
            //continue;
        }
        countryName = ULocale.getDisplayCountry("und-" + countryCode, ULocale.ENGLISH);
        Set<LocodeData> locodeDatas = nameToLocodeData.get(cityName);
        if (locodeDatas == null) {
            // try again without accents
            String cityName2 = REMOVE_ACCENTS.transform(cityName);
            if (!cityName.equals(cityName2)) {
                locodeDatas = nameToLocodeData.get(cityName2);
            }
        }
        if (locodeDatas == null) {
            errors2.add("** No matching record for\t" + countryName + "\t" + countryCode + "\t" + cityName);
        } else {
            Set<LocodeData> rem = new LinkedHashSet<LocodeData>();
            for (LocodeData x : locodeDatas) {
                if (x.locode.startsWith(countryCode)) {
                    if (x.subdivision.equals(subdivision)) {
                        rem.clear();
                        rem.add(x);
                        break;
                    }
                    rem.add(x);
                }
            }
            if (rem.size() == 0) {
                errors2.add("** No matching country record for\t" + countryName + "\t" + countryCode + "\t" + cityName + "\t" + locodeDatas);
            } else if (rem.size() != 1) {
                errors2.add("** Multiple matching country records for\t" + countryName + "\t" + countryCode + "\t" + cityName + "\t" + rem);
            } else {
                LocodeData locodeData = rem.iterator().next();
                countryNameToCities.put(countryName, locodeData);
            }
        }
    }
}