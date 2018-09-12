package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ICUUncheckedIOException;

public class FindPluralDifferences {

    public static void main(String[] args) {
        diff();
    }

    public static void diff() {

        BitSet x = new BitSet();
        x.set(3, 6);
        x.set(9);
        x.set(11, 13);
        Map<String, BitSet> foo = new TreeMap<String, BitSet>();
        foo.put("x", x);
        FindPluralDifferences.show(foo);

        SupplementalDataInfo supplementalNew = null;
        String newVersion = null;

        List<String> items = new ArrayList<>(ToolConstants.CLDR_VERSIONS);
        items.add("trunk");

        for (String version : items) {
            if (version.compareTo("28.0") < 0) {
                continue; // old versions don't handle various items
            }
            String oldVersion = newVersion;
            newVersion = version;
            SupplementalDataInfo supplementalOld = supplementalNew;

            if (supplementalNew == null) {
                try {
                    supplementalNew = SupplementalDataInfo.getInstance(
                        CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + version + "/common/supplemental/");
                } catch (ICUUncheckedIOException e) {
                    System.out.println(e.getMessage());
                }
                continue;
            }

            supplementalNew = newVersion.equals("trunk") ? SupplementalDataInfo.getInstance()
                : SupplementalDataInfo.getInstance(
                    CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + newVersion + "/common/supplemental/");
            System.out.println("# " + oldVersion + "➞" + newVersion);

            for (PluralType pluralType : PluralType.values()) {
                Set<String> oldLocales = supplementalOld.getPluralLocales(pluralType);
                Set<String> newLocales = supplementalNew.getPluralLocales(pluralType);

                TreeSet justOldLocales = new TreeSet(oldLocales);
                justOldLocales.removeAll(newLocales);
                if (!justOldLocales.isEmpty()) {
                    System.err.println("Old locales REMOVED:\t" + justOldLocales.size() + "\t" + justOldLocales);
                }

                TreeSet justNewLocales = new TreeSet(newLocales);
                justNewLocales.removeAll(oldLocales);
                System.out.println("\nNew locales for " + pluralType + "s:\t" + justNewLocales.size() + "\t" + justNewLocales);
                System.out.println("Modifications:");

                for (String locale : oldLocales) {
                    Map<String, BitSet> results = new TreeMap<String, BitSet>();
                    PluralInfo oldPluralInfo = supplementalOld.getPlurals(pluralType, locale);
                    PluralRules oldRules = oldPluralInfo.getPluralRules();
                    PluralInfo newPluralInfo = supplementalNew.getPlurals(pluralType, locale);
                    PluralRules newRules = newPluralInfo.getPluralRules();
                    for (int i = 0; i < 101; ++i) {
                        String oldKeyword = oldRules.select(i);
                        String newKeyword = newRules.select(i);
                        if (!oldKeyword.equals(newKeyword)) {
                            String key = oldKeyword + "➞" + newKeyword;
                            BitSet diff = results.get(key);
                            if (diff == null) {
                                results.put(key, diff = new BitSet());
                            }
                            diff.set(i);
                        }
                    }
                    Set<String> oldKeywords = oldRules.getKeywords();
                    Set<String> newKeywords = newRules.getKeywords();
                    if (results.size() == 0 && oldKeywords.equals(newKeywords)) {
                        continue;
                    }
                    String type = null;
                    if (oldKeywords.equals(newKeywords)) {
                        type = "EQUAL TO";
                    } else if (oldKeywords.containsAll(newKeywords)) {
                        type = "MERGED TO";
                    } else if (newKeywords.containsAll(oldKeywords)) {
                        type = "SPLIT TO";
                    } else {
                        type = "DISJOINT FROM";
                    }
                    System.out.println(pluralType
                        + "\t" + oldVersion + "➞" + newVersion
                        + "\t" + ToolConfig.getToolInstance().getEnglish().getName(locale)
                        + "\t" + locale
                        + "\t" + oldKeywords + "\t" + type + "\t" + newKeywords
                        + "\t" + FindPluralDifferences.show(results));
                }
            }
        }
        Set<String> pluralRangesLocales = supplementalNew.getPluralRangesLocales();
        System.out.println("\nLocales for plural ranges: " + pluralRangesLocales.size()
            + "\t" + new TreeSet(pluralRangesLocales));
    }

    static String show(Map<String, BitSet> results) {
        StringBuilder result = new StringBuilder();
        for (Entry<String, BitSet> entry : results.entrySet()) {
            String key = entry.getKey();
            BitSet value = entry.getValue();
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(key).append(" for {");
            int start = 0;
            boolean first = true;
            while (true) {
                start = value.nextSetBit(start);
                if (start < 0) {
                    break;
                }
                int limit = value.nextClearBit(start);
                if (limit < 0) {
                    limit = value.size();
                }
                int end = limit - 1;
                if (first) {
                    first = false;
                } else {
                    result.append(",");
                }
                result.append(start);
                if (end != start) {
                    result.append("–")
                        .append(end);
                }
                start = limit;
            }
            return result.append("}").toString();
        }
        return null;
    }

}
