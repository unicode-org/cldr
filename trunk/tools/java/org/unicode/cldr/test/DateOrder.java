package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.text.DateTimePatternGenerator.VariableField;

class DateOrder implements Comparable<DateOrder> {
    private int etype1;
    private int etype2;

    public void set(int a, int b) {
        etype1 = a;
        etype2 = b;
    }
    @Override
    public boolean equals(Object obj) {
        DateOrder that = (DateOrder)obj;
        return that.etype1 == etype1 && that.etype2 == etype2;
    }
    @Override
    public int hashCode() {
        return etype1*37 + etype2;
    }
    @Override
    public String toString() {
        return "<" + toString2(etype1) + "," + toString2(etype2) + ">";
    }
    private String toString2(int etype) {
        char lead;
        switch (etype>>1) {

        }
        return (VariableField.getCanonicalCode(etype>>1)) + ((etype&1) == 0 ? "" : "ⁿ");
    }
    @Override
    public int compareTo(DateOrder that) {
        int diff;
        if (0 != (diff = etype1 - that.etype1)) {
            return diff;
        }
        return etype2 - that.etype2;
    }

    public static Map<String, Map<DateOrder, String>> getOrderingInfo(CLDRFile plain, CLDRFile resolved, FlexibleDateFromCLDR flexInfo) {
        Map<String, Map<DateOrder,String>> pathsWithConflictingOrder2sample = new HashMap<String, Map<DateOrder,String>>();
        Status status = new Status();
        try {
            Map<String,Map<DateOrder, Set<String>>> type2order2set = new HashMap<String,Map<DateOrder, Set<String>>>();
            Matcher typeMatcher = Pattern.compile("\\[@type=\"([^\"]*)\"]").matcher("");
            int[] soFar = new int[50];
            int lenSoFar = 0;
            DateOrder order = new DateOrder();
            for (String path : resolved) {
                if (DisplayAndInputProcessor.hasDatetimePattern(path)) {
                    String locale = resolved.getSourceLocaleID(path, status);
                    if (!path.equals(status.pathWhereFound)) {
                        continue;
                    }
                    typeMatcher.reset(path).find();
                    String type = typeMatcher.group(1);
                    Map<DateOrder, Set<String>> pairCount = type2order2set.get(type);
                    if (pairCount == null) {
                        type2order2set.put(type, pairCount = new HashMap());
                    }
                    boolean isInterval = path.contains("intervalFormatItem");
                    lenSoFar = 0;
                    String value = resolved.getStringValue(path);
                    boolean test = value.equals("EEE d.") || value.equals("E, dd.MM. - E, dd.MM.");
                    test = test;
                    // register a comparison for all of the items so far
                    for (Object item : flexInfo.fp.set(value).getItems()) {
                        if (item instanceof VariableField) {
                            VariableField variable = (VariableField) item;
                            int eType = variable.getType()*2 + (variable.isNumeric() ? 1 : 0);
                            if (isInterval && find(eType, soFar, lenSoFar)) {
                                lenSoFar = 0; // restart the clock
                                soFar[lenSoFar++] = eType;
                                continue;
                            }
                            for (int i = 0; i < lenSoFar; ++i) {
                                order.set(soFar[i], eType);
                                Set<String> paths = pairCount.get(order);
                                if (paths == null) {
                                    pairCount.put(order, paths = new HashSet<String>());
                                    order = new DateOrder();
                                }
                                paths.add(path);
                            }
                            soFar[lenSoFar++] = eType;
                        }
                    }
                }
            }
            // determine conflicts, and mark
            HashSet<Set<String>> alreadySeen = new HashSet<Set<String>>();
            DateOrder reverseOrder = new DateOrder();
            for (Entry<String, Map<DateOrder, Set<String>>> typeAndOrder2set : type2order2set.entrySet()) {
                Map<DateOrder, Set<String>> pairCount = typeAndOrder2set.getValue();
                for (Entry<DateOrder, Set<String>> entry : pairCount.entrySet()) {
                    Set<String> thisPaths = entry.getValue();
                    if (alreadySeen.contains(thisPaths)) {
                        continue;
                    }
                    DateOrder thisOrder = entry.getKey();
                    reverseOrder.etype1 = thisOrder.etype2;
                    reverseOrder.etype2 = thisOrder.etype1;
                    Set<String> reverseSet = pairCount.get(reverseOrder);
                    DateOrder sample = thisOrder.compareTo(reverseOrder) < 0 ? thisOrder : reverseOrder;

                    if (reverseSet != null) {
                        String otherPath = reverseSet.iterator().next();
                        //String otherValue = resolved.getStringValue(otherPath);
                        for (String first : thisPaths) {
                            addItem(plain, first, sample, otherPath, pathsWithConflictingOrder2sample);
                        }
                        otherPath = thisPaths.iterator().next();
                        // otherValue = resolved.getStringValue(otherPath);
                        for (String first : reverseSet) {
                            addItem(plain, first, sample, otherPath, pathsWithConflictingOrder2sample);
                        }
                        alreadySeen.add(reverseSet);
                    }
                }
            }
            // for debugging, show conflicts
            if (CheckDates.GREGORIAN_ONLY) {
                for (Entry<String, Map<DateOrder, String>> entry : pathsWithConflictingOrder2sample.entrySet()) {
                    String path1 = entry.getKey();
                    String locale1 = resolved.getSourceLocaleID(path1, status);
                    String value1 = resolved.getStringValue(path1);
                    Map<DateOrder, String> orderString = entry.getValue();
                    for (Entry<DateOrder, String> entry2 : orderString.entrySet()) {
                        DateOrder order2 = entry2.getKey();
                        String path2 = entry2.getValue();
                        String locale2 = resolved.getSourceLocaleID(path2, status);
                        String value2 = resolved.getStringValue(path2);
                        System.out.println(order2 + "\t" + value1 + "\t" + value2 + "\t" + locale1 + "\t" + locale2 + "\t" + path1 + "\t" + path2);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        }
        return pathsWithConflictingOrder2sample;
    }

    private static boolean find(int eType, int[] soFar, int lenSoFar) {
        for (int i = 0; i < lenSoFar; ++i) {
            if (eType == soFar[i]) {
                return true;
            }
        }
        return false;
    }

    private static void addItem(CLDRFile plain, String path, DateOrder sample, String conflictingPath, Map<String, Map<DateOrder, String>> pathsWithConflictingOrder2sample) {
        String value = plain.getStringValue(path);
        if (value == null) {
            return;
        }
        Map<DateOrder, String> order2path = pathsWithConflictingOrder2sample.get(path);
        if (order2path == null) {
            pathsWithConflictingOrder2sample.put(path, order2path = new TreeMap());
        }
        order2path.put(sample, conflictingPath);
    }

}