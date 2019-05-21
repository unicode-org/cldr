/*
 **********************************************************************
 * Copyright (c) 2002-2012, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.icu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Extract ICU transform data and convert to CLDR format.<br>
 * With the option -Dfile=xxxx, will convert a single file. For example:<br>
 *
 * <pre>
 * -DSHOW_FILES -Dfile=c:/downloads/zh_Hans-zh_Hant.txt
 * </pre>
 *
 * The option -Dtarget=yyy will specify an output directory; otherwise it is Utility.GEN_DIRECTORY + "/translit/gen/"
 *
 * @author markdavis
 *
 */
public class ExtractICUData {
    public static void main(String[] args) throws Exception {
        String file = CldrUtility.getProperty("file", null);
        if (file != null) {
            String targetDirectory = CldrUtility.getProperty("target", CLDRPaths.GEN_DIRECTORY + "/translit/gen/");
            convertFile(file, targetDirectory);
        } else {
            generateTransliterators();
        }
        System.out.println("Done");
    }

    static Set<String> skipLines = new HashSet<String>(Arrays.asList(new String[] {
        "#--------------------------------------------------------------------",
        "# Copyright (c) 1999-2005, International Business Machines",
        "# Copyright (c) 1999-2004, International Business Machines",
        "# Corporation and others. All Rights Reserved.",
        "#--------------------------------------------------------------------"
    }));
    static Set<String> skipFiles = new HashSet<String>(Arrays.asList(new String[] {
        // "Any_Accents",
        "el",
        "en",
        "root"
    }));

    static void generateTransliterators() throws IOException {
        Matcher fileFilter = PatternCache.get(".*").matcher("");

        CLDRFile accumulatedItems = SimpleFactory.makeSupplemental("allItems");
        getTranslitIndex(accumulatedItems);

        File translitSource = new File("C:\\cvsdata\\icu\\icu\\source\\data\\translit\\");
        System.out.println("Source: " + translitSource.getCanonicalPath());
        File[] fileArray = translitSource.listFiles();
        List<Object> list = new ArrayList<Object>(Arrays.asList(fileArray));

//        List<String> extras = Arrays.asList(new String[] {
//            "Arabic_Latin.txt",
//            "CanadianAboriginal_Latin.txt",
//            "Cyrillic_Latin.txt",
//            "Georgian_Latin.txt",
//            // "Khmer_Latin.txt", "Lao_Latin.txt", "Tibetan_Latin.txt"
//            "Latin_Armenian.txt",
//            "Latin_Ethiopic.txt",
//            "Syriac_Latin.txt", "Thaana_Latin.txt", });
//        list.addAll(extras);

        String[] attributesOut = new String[1];
        for (Object file : list) {
            String fileName = (file instanceof File) ? ((File) file).getName() : (String) file;
//            if (file instanceof File && extras.contains(fileName)) {
//                System.out.println("Skipping old version: " + fileName);
//            }
            if (!fileName.endsWith(".txt")) continue;
            String coreName = fileName.substring(0, fileName.length() - 4);
            if (skipFiles.contains(coreName)) continue;
            String id = fixTransID(coreName, attributesOut);
            String outName = id.replace('/', '-');
            String attributes = attributesOut[0];
            attributes += "[@direction=\"both\"]";

            System.out.println(coreName + "\t=>\t" + outName + " => " + attributes);

            if (!fileFilter.reset(fileName).matches()) continue;

            BufferedReader input;
            if (file instanceof File) {
                input = FileUtilities.openUTF8Reader(((File) file).getParent() + File.separator, fileName);
            } else {
                input = CldrUtility.getUTF8Data(fileName);
            }
            {
                CLDRFile outFile = SimpleFactory.makeSupplemental(fileName);
                int count = 0;
                String prefixBase = "//supplementalData[@version=\"" + CLDRFile.GEN_VERSION + "\"]/transforms/transform"
                    + attributes;
                String rulePrefix = prefixBase + "/tRule[@_q=\"";
                String commentPrefix = prefixBase + "/comment[@_q=\"";

                StringBuffer accumulatedLines = new StringBuffer();
                while (true) {
                    String line = input.readLine();
                    if (line == null) break;
                    if (line.startsWith("\uFEFF")) line = line.substring(1); // remove BOM
                    line = line.trim();
                    if (skipLines.contains(line)) continue;
                    if (line.length() == 0) continue;
                    String fixedLine = fixTransRule(line);
                    // if (accumulatedLines.length() == 0)
                    accumulatedLines.append("\n\t\t");
                    accumulatedLines.append(fixedLine);
                    String prefix = (line.startsWith("#")) ? commentPrefix : rulePrefix;
                    addInTwo(outFile, accumulatedItems, prefix + (++count) + "\"]", fixedLine);
                }

                PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "/translit/gen/", outName + ".xml");
                outFile.write(pw);
                pw.close();
            }
        }
        PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "/translit/gen/", "All" + ".xml");
        accumulatedItems.write(pw);
        pw.close();
    }

    static void convertFile(String fileName, String targetDirectory) throws IOException {
        // Get the ID
        String coreName = new File(fileName).getName();
        if (coreName.endsWith(".txt")) {
            coreName = coreName.substring(0, coreName.length() - 4);
        }
        String[] attributesOut = new String[1];
        attributesOut[0] = "";
        String id = fixTransID(coreName, attributesOut);
        String outName = id.replace('/', '-');
        String attributes = attributesOut[0];
        attributes += "[@direction=\"both\"]";

        System.out.println(coreName + "\t=>\t" + outName + " => " + attributes);

        BufferedReader input = FileUtilities.openUTF8Reader("", fileName);
        CLDRFile outFile = SimpleFactory.makeSupplemental(coreName);
        int count = 0;
        String prefixBase = "//supplementalData[@version=\"" + CLDRFile.GEN_VERSION + "\"]/transforms/transform"
            + attributes;
        String rulePrefix = prefixBase + "/tRule[@_q=\"";
        String commentPrefix = prefixBase + "/comment[@_q=\"";

        StringBuffer accumulatedLines = new StringBuffer();
        while (true) {
            String line = input.readLine();
            if (line == null) break;
            if (line.startsWith("\uFEFF")) line = line.substring(1); // remove BOM
            line = line.trim();
            if (skipLines.contains(line)) continue;
            if (line.length() == 0) continue;
            String fixedLine = fixTransRule(line);
            // if (accumulatedLines.length() == 0)
            accumulatedLines.append("\n\t\t");
            accumulatedLines.append(fixedLine);
            String prefix = (line.startsWith("#")) ? commentPrefix : rulePrefix;
            addInTwo(outFile, null, prefix + (++count) + "\"]", fixedLine);
        }

        PrintWriter pw = FileUtilities.openUTF8Writer(targetDirectory, outName + ".xml");
        outFile.write(pw);
        pw.close();

    }

    private static void addInTwo(CLDRFile outFile, CLDRFile accumulatedItems, String path, String value) {
        // System.out.println("Adding: " + path + "\t\t" + value);
        outFile.add(path, value);
        if (accumulatedItems != null) {
            accumulatedItems.add(path, value);
        }
    }

    private static String fixTransRule(String line) {
        int hashPos = line.indexOf('#');
        // quick hack to separate comment, and check for quoted '#'
        if (hashPos >= 0 && line.indexOf('\'', hashPos) < 0) {
            String core = line.substring(0, hashPos).trim();
            String comment = line.substring(hashPos + 1).trim();
            if (comment.length() != 0) {
                comment = "# " + comment;
            } else if (core.length() == 0) {
                return "#";
            }
            line = (core.length() == 0 ? "" : core + " ") + comment;
        }
        // fixedLine = fixedLine.replaceAll("<>", "\u2194");
        // fixedLine = fixedLine.replaceAll("<", "\u2190");
        // fixedLine = fixedLine.replaceAll(">", "\u2192");
        // fixedLine = fixedLine.replaceAll("&", "\u00A7");
        String fixedLine = fixLine.transliterate(line);
        return fixedLine;
    }

    static String fixLineRules = "'<>' > '\u2194';" +
        "'<' > '\u2190';" +
        "'>' > '\u2192';" +
        "'&' > '\u00A7';" +
        "('\\u00'[0-7][0-9A-Fa-f]) > $1;" + // leave ASCII alone
        "('\\u'[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]) > |@&hex-any/java($1);" +
        "([[:whitespace:][:Default_Ignorable_Code_Point:][:C:]-[\\u0020\\u200E\\0009]]) > &any-hex/java($1);"

    ;
    static Transliterator fixLine = Transliterator.createFromRules("foo", fixLineRules, Transliterator.FORWARD);

    private static final String INDEX = "index",
        RB_RULE_BASED_IDS = "RuleBasedTransliteratorIDs";

    private static void getTranslitIndex(CLDRFile accumulatedItems) throws IOException {

        UResourceBundle bundle, transIDs, colBund;
        bundle = UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, INDEX);
        transIDs = bundle.get(RB_RULE_BASED_IDS);

        String[] attributesOut = new String[1];
        int count = 0;

        int maxRows = transIDs.getSize();
        for (int row = 0; row < maxRows; row++) {
            colBund = transIDs.get(row);
            String ID = colBund.getKey();
            UResourceBundle res = colBund.get(0);
            String type = res.getKey();
            if (type.equals("file") || type.equals("internal")) {
                // // Rest of line is <resource>:<encoding>:<direction>
                // // pos colon c2
                // String resString = res.getString("resource");
                // String direction = res.getString("direction");
                // result.add(Arrays.asList(new Object[]{ID,
                // resString, // resource
                // "UTF-16", // encoding
                // direction,
                // type}));
            } else if (type.equals("alias")) {
                CLDRFile outFile = SimpleFactory.makeSupplemental("transformAliases");
                // 'alias'; row[2]=createInstance argument
                ID = fixTransID(ID, attributesOut);
                String outName = ID.replace('/', '-');
                String attributes = attributesOut[0];
                attributes += "[@direction=\"forward\"]";
                System.out.println(ID + " => " + attributes);
                String prefix = "//supplementalData[@version=\"" + CLDRFile.GEN_VERSION + "\"]/transforms/transform"
                    + attributes + "/tRule[@_q=\"";
                String resString = res.getString();
                if (!instanceMatcher.reset(resString).matches()) {
                    System.out.println("Doesn't match id: " + resString);
                } else {
                    String filter = instanceMatcher.group(1);
                    if (filter != null) {
                        filter = fixTransRule(filter);
                        outFile.add(prefix + (++count) + "\"]", "::" + filter + ";");
                        accumulatedItems.add(prefix + (++count) + "\"]", "::" + filter + ";");
                    }
                    String rest = instanceMatcher.group(2);
                    String[] pieces = rest.split(";");
                    for (int i = 0; i < pieces.length; ++i) {
                        String piece = pieces[i].trim();
                        if (piece.length() == 0) continue;
                        piece = fixTransID(piece, null);
                        outFile.add(prefix + (++count) + "\"]", "::" + piece + ";");
                        accumulatedItems.add(prefix + (++count) + "\"]", "::" + piece + ";");
                    }
                }
                PrintWriter pw = FileUtilities.openUTF8Writer(
                    CLDRPaths.GEN_DIRECTORY + "/translit/gen/", outName + ".xml");
                outFile.write(pw);
                pw.close();
            } else {
                // Unknown type
                throw new RuntimeException("Unknown type: " + type);
            }
        }
    }

    private static String fixTransID(String id, String[] attributesOut) {
        if (!idMatcher.reset(id).matches()) {
            System.out.println("Doesn't match id:: " + id);
        } else {
            String source = fixTransIDPart(idMatcher.group(1));
            String target = fixTransIDPart(idMatcher.group(2));
            String variant = fixTransIDPart(idMatcher.group(3));

            if (attributesOut != null) {
                attributesOut[0] = "[@source=\"" + source + "\"]"
                    + "[@target=\"" + target + "\"]"
                    + (variant == null ? "" : "[@variant=\"" + variant + "\"]");
                if (privateFiles.reset(id).matches()) attributesOut[0] += "[@visibility=\"internal\"]";
            }

            if (target == null)
                target = "";
            else
                target = "-" + target;
            if (variant == null)
                variant = "";
            else
                variant = "/" + variant;
            id = source + target + variant;
        }
        return id;
    }

    static String idPattern = "\\s*(\\p{L}+)(?:[_-](\\p{L}+))?(?:\\[_/](\\p{L}+))?";
    static Matcher idMatcher = PatternCache.get(idPattern).matcher("");
    static Matcher instanceMatcher = PatternCache.get("\\s*(\\[.*\\]\\s*)?(.*)").matcher("");

    // private static String fixTransName(String name, String[] attributesOut, String separator) {
    // String[] pieces = name.split(separator);
    // String source = fixTransIDPart(pieces[0]);
    // String target = fixTransIDPart(pieces[1]);
    // String variant = null;
    // if (pieces.length > 2) {
    // variant = pieces[2].toUpperCase();
    // }
    // attributesOut[0] = "[@source=\"" + source + "\"]"
    // + "[@target=\"" + target + "\"]"
    // + (variant == null ? "" : "[@variant=\"" + variant + "\"]");
    // if (privateFiles.reset(name).matches()) attributesOut[0] += "[@visibility=\"internal\"]";
    // return source + (target == null ? "" : "-") + target + (variant == null ? "" : "/" + variant);
    // }

    static Matcher privateFiles = PatternCache.get(".*(Spacedhan|InterIndic|ThaiLogical|ThaiSemi).*").matcher("");
    static Matcher allowNames = PatternCache.get("(Fullwidth|Halfwidth|NumericPinyin|Publishing)").matcher("");

    static Set<String> collectedNames = new TreeSet<String>();

    private static String fixTransIDPart(String name) {
        if (name == null) return name;
        try {
            UCharacter.getPropertyValueEnum(UProperty.SCRIPT, name);
        } catch (IllegalArgumentException e) {
            collectedNames.add(name);
        }

        if (name.equals("Tone")) return "Pinyin";
        if (name.equals("Digit")) return "NumericPinyin";
        if (name.equals("Jamo")) return "ConjoiningJamo";
        if (name.equals("LowerLatin")) return "Latin";

        return name;
    }

    static void testProps() {
        int[][] ranges = { { UProperty.BINARY_START, UProperty.BINARY_LIMIT },
            { UProperty.INT_START, UProperty.INT_LIMIT },
            { UProperty.DOUBLE_START, UProperty.DOUBLE_START },
            { UProperty.STRING_START, UProperty.STRING_LIMIT },
        };
        Collator col = Collator.getInstance(ULocale.ROOT);
        ((RuleBasedCollator) col).setNumericCollation(true);
        Map<String, Set<String>> alpha = new TreeMap<String, Set<String>>(col);

        for (int range = 0; range < ranges.length; ++range) {
            for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
                String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
                String shortPropName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.SHORT);
                propName = getName(propIndex, propName, shortPropName);
                Set<String> valueOrder = new TreeSet<String>(col);
                alpha.put(propName, valueOrder);
                switch (range) {
                case 0:
                    valueOrder.add("[binary]");
                    break;
                case 2:
                    valueOrder.add("[double]");
                    break;
                case 3:
                    valueOrder.add("[string]");
                    break;
                case 1:
                    for (int valueIndex = 0; valueIndex < 256; ++valueIndex) {
                        try {
                            String valueName = UCharacter.getPropertyValueName(propIndex, valueIndex,
                                UProperty.NameChoice.LONG);
                            String shortValueName = UCharacter.getPropertyValueName(propIndex, valueIndex,
                                UProperty.NameChoice.SHORT);
                            valueName = getName(valueIndex, valueName, shortValueName);
                            valueOrder.add(valueName);
                        } catch (RuntimeException e) {
                            // just skip
                        }
                    }
                    break;
                }
            }
        }
        PrintStream out = System.out;

        for (Iterator<String> it = alpha.keySet().iterator(); it.hasNext();) {
            String propName = it.next();
            Set<String> values = alpha.get(propName);
            out.println("<tr><td>" + propName + "</td>");
            out.println("<td><table>");
            for (Iterator<String> it2 = values.iterator(); it2.hasNext();) {
                String propValue = it2.next();
                System.out.println("<tr><td>" + propValue + "</td></tr>");
            }
            out.println("</table></td></tr>");
        }
        Collator c = Collator.getInstance(ULocale.ENGLISH);
        ((RuleBasedCollator) c).setNumericCollation(true);

        // int enumValue = UCharacter.getIntPropertyValue(codePoint, propEnum);
        // return UCharacter.getPropertyValueName(propEnum,enumValue, (int)nameChoice);

    }

    private static String getName(int index, String valueName, String shortValueName) {
        if (valueName == null) {
            if (shortValueName == null) return String.valueOf(index);
            return shortValueName;
        }
        if (shortValueName == null) return valueName;
        if (valueName.equals(shortValueName)) return valueName;
        return valueName + "\u00A0(" + shortValueName + ")";
    }
}