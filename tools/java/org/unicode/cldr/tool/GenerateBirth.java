package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.GenerateBirth.Births;
import org.unicode.cldr.tool.GenerateBirth.Versions;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.LanguageTagParser;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;

public class GenerateBirth {
    enum Versions {
        v2_0_0β, v1_9_0, v1_8_1, v1_7_2, v1_6_1, v1_5_1, v1_4_1, v1_3_0, v1_2_0, v1_1_1;
        public String toString() {
            return name().substring(1).replace('_', '.');
        };
    }
    static final Versions[] VERSIONS = Versions.values();
    static final Factory[] factories;
    static {
        ArrayList<Factory> list = new ArrayList<Factory>();
        for (Versions version : VERSIONS) {
            Factory aFactory = Factory.make(CldrUtility.BASE_DIRECTORY 
                    + (version == Versions.v2_0_0β ? "" : "../cldr-" + version) 
                    + "/common/main/", ".*");
            list.add(aFactory);
        }
        factories = list.toArray(new Factory[list.size()]);
    }

    public static void main(String[] args) {
        Births english = new Births("en");
        english.writeBirth();
        System.out.println("\nFrench\n");
        Births fr = new Births("fr");
        fr.writeBirth(english);

        if (true) return;
        LanguageTagParser ltp = new LanguageTagParser();
        for (String file : factories[0].getAvailable()) {
            if (!ltp.set(file).getRegion().isEmpty()) {
                continue; // skip region locales
            }
            // TODO skip default content locales
            Births other = new Births(file);
        }
    }

    static class Births {
        final Relation<Versions, String> birthToPaths;
        final Map<String, Row.R3<Versions,String,String>> pathToBirthCurrentPrevious;
        final String locale;

        Births(String file) {
            locale = file;
            CLDRFile[] files = new CLDRFile[factories.length];
            for (int i = 0; i < factories.length; ++i) {
                files[i] = factories[i].make(file, false);
            }
            birthToPaths = Relation.of(new TreeMap<Versions, Set<String>>(), TreeSet.class);
            pathToBirthCurrentPrevious = new HashMap();
            for (String xpath : files[0]) {
                xpath = xpath.intern();
                String base = files[0].getStringValue(xpath);
                String previousValue = null;
                int i;
                for (i = 1; i < files.length; ++i) {
                    String previous = files[i].getStringValue(xpath);
                    if (!CharSequences.equals(base, previous)) {
                        if (previous != null) {
                            previousValue = previous;
                        }
                        break;
                    }
                }
                Versions version = VERSIONS[i-1];
                birthToPaths.put(version, xpath);
                pathToBirthCurrentPrevious.put(xpath, Row.of(version, base, previousValue));
            }
        }

        void writeBirth(Births onlyNewer) {
            Versions onlyNewerVersion = Versions.v2_0_0β;
            String otherValue = "";
            String olderOtherValue = "";
            for (Entry<Versions, Set<String>> entry2 : birthToPaths.keyValuesSet()) {
                Versions version = entry2.getKey();
                for (String xpath : entry2.getValue()) {
                    R3<Versions, String, String> info = pathToBirthCurrentPrevious.get(xpath);
                    if (onlyNewer != null) {
                        R3<Versions, String, String> otherInfo = onlyNewer.pathToBirthCurrentPrevious.get(xpath);
                        if (otherInfo == null) {
                            continue;
                        }
                        // skip if older or same
                        onlyNewerVersion = otherInfo.get0();
                        if (version.compareTo(onlyNewerVersion) <= 0) {
                            continue;
                        }
                        otherValue = fixNull(otherInfo.get1());   
                        olderOtherValue = fixNull(otherInfo.get2());
                    }
                    String value = fixNull(info.get1());
                    String olderValue = fixNull(info.get2());
                    System.out.println(locale + "\t" + version + "\t" + onlyNewerVersion 
                            + "\t" + xpath 
                            + "\t" + value
                            + "\t" + olderValue
                            + "\t" + otherValue 
                            + "\t" + olderOtherValue 
                            );
                }
            }
        }

        private String fixNull(String value) {
            if (value == null) {
                value = "∅";
            }
            return value;
        }

        void writeBirth() {
            writeBirth(null);
        }
    }
}
