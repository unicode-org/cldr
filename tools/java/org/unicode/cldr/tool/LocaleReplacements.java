package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.IsoRegionData;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.Output;

public class LocaleReplacements {
    public static final Pattern WHITESPACE = PatternCache.get("\\s+");

    /**
     * eg language, eng, <overlong,en>
     */
    static Map<String, Map<String, Row.R2<Set<String>, String>>> type2item2replacementAndReason = new HashMap<String, Map<String, Row.R2<Set<String>, String>>>();
    static Map<String, Relation<String, Row.R2<String, Set<String>>>> type2reason2itemAndreplacement = new TreeMap<String, Relation<String, Row.R2<String, Set<String>>>>();
    static Relation<String, String> fixed = Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);

    public String get(String old, Output<String> reason) {
        reason.value = null;
        return old;
    }

    static {
        Map<String, Map<String, Map<String, String>>> lstreg = StandardCodes.getLStreg();
        for (Entry<String, Map<String, Map<String, String>>> entry : lstreg.entrySet()) {
            String type = entry.getKey();
            Map<String, Map<String, String>> subtype2data = entry.getValue();

            for (Entry<String, Map<String, String>> itemAndData : subtype2data.entrySet()) {
                final Map<String, String> value = itemAndData.getValue();
                String deprecated = value.get("Deprecated");
                if (deprecated != null) {
                    String preferredValue = value.get("Preferred-Value");
                    if (preferredValue == null) {
                        preferredValue = "";
                    }
                    final String key = itemAndData.getKey();
                    String type2 = type.equals("region") ? "territory" : type;
                    addType2item2reasonNreplacement(type2, key, preferredValue, "deprecated", false);
                }
            }
        }

        for (String lang : Iso639Data.getAvailable()) {
            if (lang.length() != 2) continue;
            String alpha3 = Iso639Data.toAlpha3(lang);
            addType2item2reasonNreplacement("language", alpha3, lang, "overlong", false);
        }
        /*
         * return IsoRegionData.get_alpha3(region);
         * }
         * });
         * addRegions(english, territories, "AC,CP,DG,EA,EU,IC,TA".split(","), new Transform<String,String>() {
         * public String transform(String region) {
         * return IsoRegionData.getNumeric(region);
         */
        //Set<String> available2 = IsoRegionData.getAvailable();

        for (String region : IsoRegionData.getAvailable()) {
            String alpha3 = IsoRegionData.get_alpha3(region);
            addType2item2reasonNreplacement("territory", alpha3, region, "overlong", false);
            String numeric = IsoRegionData.getNumeric(region);
            addType2item2reasonNreplacement("territory", numeric, region, "overlong", false);
        }

        // Add overrides
        FileProcessor myReader = new FileProcessor() {
            @Override
            protected boolean handleLine(int lineCount, String line) {
                addType2item2reasonNreplacement(line);
                return true;
            }
        };

        myReader.process(CldrUtility.class, "data/localeReplacements.txt");

        // fix up the data by recursing

        for (Entry<String, Map<String, R2<Set<String>, String>>> entry : type2item2replacementAndReason.entrySet()) {
            //String type = entry.getKey();
            final Map<String, R2<Set<String>, String>> item2replacementAndReason = entry.getValue();
            while (true) {
                boolean keepGoing = false;
                for (Entry<String, R2<Set<String>, String>> entry2 : item2replacementAndReason.entrySet()) {
                    String item = entry2.getKey();
                    R2<Set<String>, String> replacementAndReason = entry2.getValue();
                    Set<String> replacements = replacementAndReason.get0();
                    //String reason = replacementAndReason.get1();
                    Set<String> newReplacements = new LinkedHashSet<String>(replacements.size());
                    boolean gotChange = false;
                    for (String oldRep : replacements) {
                        R2<Set<String>, String> newRepAndReason = item2replacementAndReason.get(oldRep);
                        if (newRepAndReason != null) {
                            fixed.put(item, oldRep + "\t-->\t" + newRepAndReason);
                            newReplacements.addAll(newRepAndReason.get0());
                            gotChange = true;
                        } else {
                            newReplacements.add(oldRep);
                        }
                    }
                    if (gotChange) {
                        replacementAndReason.set0(newReplacements);
                        keepGoing = true;
                    }
                }
                if (!keepGoing) {
                    break;
                }
            }
        }

        for (Entry<String, Map<String, R2<Set<String>, String>>> entry : type2item2replacementAndReason.entrySet()) {
            String type = entry.getKey();
            final Map<String, R2<Set<String>, String>> item2replacementAndReason = entry.getValue();
            for (Entry<String, R2<Set<String>, String>> entry2 : item2replacementAndReason.entrySet()) {
                String item = entry2.getKey();
                R2<Set<String>, String> replacementAndReason = entry2.getValue();
                Set<String> replacements = replacementAndReason.get0();
                String reason = replacementAndReason.get1();

                Relation<String, R2<String, Set<String>>> reason2item2replacement = type2reason2itemAndreplacement
                    .get(type);
                if (reason2item2replacement == null) {
                    type2reason2itemAndreplacement.put(
                        type,
                        reason2item2replacement = Relation.of(new TreeMap<String, Set<R2<String, Set<String>>>>(),
                            TreeSet.class));
                }
                reason2item2replacement.put(reason, Row.of(item, replacements));
            }
        }
    }

    private static void addType2item2reasonNreplacement(String line) {
        String[] parts = WHITESPACE.split(line);
        if (parts.length < 4) {
            addType2item2reasonNreplacement(parts[0], parts[2], parts[1], "", true);
            return;
        }
        // language macrolanguage bxk luy
        for (int i = 3; i < parts.length; ++i) {
            addType2item2reasonNreplacement(parts[0], parts[2], parts[i], parts[1], true);
        }
    }

    private static void addType2item2reasonNreplacement(String type, String key, String preferredValue, String reason,
        boolean ignoreDuplicates) {
        if (key == null) {
            return;
        }
        if (type.equals("grandfathered") || type.equals("redundant")) {
            type = "language";
        }

        key = key.replace('-', '_');
        if (type.equals("variant")) {
            key = key.toUpperCase(Locale.US);
            preferredValue = preferredValue.toUpperCase(Locale.US);
        }

        Map<String, R2<Set<String>, String>> item2replacementAndReason = type2item2replacementAndReason.get(type);
        if (item2replacementAndReason == null) {
            type2item2replacementAndReason.put(type, item2replacementAndReason = new HashMap<String, R2<Set<String>, String>>());
        }

        R2<Set<String>, String> oldReplacementAndReason = item2replacementAndReason.get(key);
        if (oldReplacementAndReason != null) {
            final String message = "duplicateReplacement\t" + type + "\t" + key + "\told: "
                + oldReplacementAndReason + "\tnew:" + preferredValue + ", " + reason;
            if (!ignoreDuplicates) {
                throw new IllegalArgumentException(message);
            } else {
                fixed.put(key, message);
                Set<String> list = oldReplacementAndReason.get0();
                list.add(preferredValue);
                return;
            }
        }
        Set<String> list = new LinkedHashSet<String>(1);
        if (!preferredValue.isEmpty()) {
            list.add(preferredValue);
        }
        item2replacementAndReason.put(key, Row.of(list, reason));
    }

    public static void main(String[] args) {
        Map<String, Map<String, R2<List<String>, String>>> localeAliasInfo = SupplementalDataInfo.getInstance()
            .getLocaleAliasInfo();

        Set<String> newStuff = new TreeSet<String>();
        Set<String> oldStuff = new TreeSet<String>();
        for (Entry<String, Relation<String, R2<String, Set<String>>>> entry : type2reason2itemAndreplacement.entrySet()) {
            String type = entry.getKey();
            for (Entry<String, R2<String, Set<String>>> entry2 : entry.getValue().entrySet()) {
                String reason = entry2.getKey();
                R2<String, Set<String>> replacementAndReason = entry2.getValue();
                String key = replacementAndReason.get0();
                Set<String> replacements = replacementAndReason.get1();
                final String message = type + "\t" + reason + "\t" + key + "\t"
                    + CollectionUtilities.join(replacements, " ");
                // System.out.println(message);
                newStuff.add(message);
            }
        }
        for (Entry<String, String> entry : fixed.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
        // Returns type -> tag -> , like "language" -> "sh" -> <{"sr_Latn"}, reason>
        for (Entry<String, Map<String, R2<List<String>, String>>> entry : localeAliasInfo.entrySet()) {
            String type = entry.getKey();
            for (Entry<String, R2<List<String>, String>> entry2 : entry.getValue().entrySet()) {
                String item = entry2.getKey();
                R2<List<String>, String> replacementAndReason = entry2.getValue();
                List<String> replacements = replacementAndReason.get0();
                String reason = replacementAndReason.get1();
                oldStuff.add(type + "\t" + reason + "\t" + item
                    + "\t" + (replacements == null ? "" : CollectionUtilities.join(replacements, " ")));
            }
        }
        Set<Row.R2<String, String>> merged = new TreeSet<Row.R2<String, String>>();

        Set<String> oldNotNew = Builder.with(new TreeSet<String>(oldStuff)).removeAll(newStuff).get();
        Set<String> newNotOld = Builder.with(new TreeSet<String>(newStuff)).removeAll(oldStuff).get();
        //Set<String> shared = Builder.with(new TreeSet<String>(oldStuff)).retainAll(newStuff).get();
        // for (String s : shared) {
        // merged.add(Row.of(s,"\tSAME"));
        // }
        for (String s : oldNotNew) {
            merged.add(Row.of(s, "\tOLD"));
        }
        for (String s : newNotOld) {
            merged.add(Row.of(s, "\tNEW"));
        }
        int i = 0;
        for (R2<String, String> s : merged) {
            System.out.println(++i + "\t" + s.get1() + "\t" + s.get0());
        }
        System.out.println("DONE");
    }
}