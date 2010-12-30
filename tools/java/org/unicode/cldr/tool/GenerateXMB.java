package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transform;

public class GenerateXMB {
    final static Options myOptions = new Options()
    .add("target", ".*", CldrUtility.GEN_DIRECTORY + "/xmb/", "The target directory for building. Will generate an English .xmb file, and .xtb files for other languages.")
    .add("file", ".*", "^fr$", "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
    .add("path", ".*", "Filter the information based on path name, using a regex argument") // "dates.*(pattern|available)", 
    .add("content", ".*", "Filter the information based on content name, using a regex argument")
    .add("jason", ".*", "Generate JSON versions instead")
    ;

    static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
    static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // darn'd checked exceptions
        }
    }
    static Matcher contentMatcher;
    static Matcher pathMatcher;
    static PrettyPath prettyPath = new PrettyPath();

    //enum Handling {SKIP};
    static RegexLookup<String> pathHandling = new RegexLookup(GenerateXMB.class, "xmbHandling.txt", null, null);
    static final Matcher datePatternMatcher = Pattern.compile("dates.*(pattern|available)").matcher("");

    public static final boolean DEBUG = false;

    public static void main(String[] args) throws Exception {
        myOptions.parse(args, true);
        Option option;
        option = myOptions.get("file");
        String fileMatcherString = option.doesOccur() ? option.getValue() : ".*";
        option = myOptions.get("content");
        contentMatcher = option.doesOccur() ? Pattern.compile(option.getValue()).matcher("") : null;
        option = myOptions.get("path");
        pathMatcher = option.doesOccur() ? Pattern.compile(option.getValue()).matcher("") : null;

        String targetDir = myOptions.get("target").getValue();

        Factory cldrFactory1 = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory1.make("en", true);
        CLDRFile root = cldrFactory1.make("en", true);

        EnglishInfo englishInfo = new EnglishInfo(targetDir, english, root);
        writeFile(targetDir, englishInfo, "en", english, true);

        // TODO:
        // Replace {0}... with placeholders (Mostly done, but need better examples)
        // Replace datetime fields (MMM, L, ...) with placeholders
        // Skip items that we don't need translated (most language names, script names, deprecated region names, etc.
        // Add descriptions
        // Add pages with detailed descriptions, and links from the descriptions
        // Represent the items with count= as ICUSyntax
        // Filter items that we don't want to get translated, and add others that we need even if not in English
        // Rewire items that are in undistinguished attributes
        // Test each xml file for validity
        // Generate strings that let the user choose the placeholder style hh vs HH,...???

        Factory cldrFactory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, fileMatcherString);
        for (String file : cldrFactory2.getAvailable()) {
            CLDRFile cldrFile = cldrFactory2.make(file, true);
            if (file.equals("en")) {
                continue;
            }
            writeFile(targetDir, englishInfo, file, cldrFile, false);
        }
    }

    private static void writeFile(String targetDir, EnglishInfo englishInfo, String file, CLDRFile cldrFile, boolean isEnglish) throws IOException {
        String extension = "xml";
        PrintWriter out = BagFormatter.openUTF8Writer(targetDir, "xmb-" + file + "." + extension);
        out.println("<?xml version='1.0' encoding='UTF-8' ?>");
        out.println("<!DOCTYPE messagebundle SYSTEM 'xmb.dtd'>");
        out.println("<messagebundle class='cldr'>");

        for (PathInfo pathInfo : englishInfo) {
            String path = pathInfo.getPath();
            String value = cldrFile.getStringValue(path);
            if (value == null) {
                continue;
            }
            //String fullPath = cldrFile.getStringValue(path);
            out.println();
            if (isEnglish) {
                out.println("\t<!--\t" + prettyPath.getPrettyPath(pathInfo.getPath(), false) + " ;\t" + pathInfo.getPath() + "\t-->");
            } else {
                out.println("\t<!--\tEnglish: " + pathInfo.getEnglishValue() + "\t-->");
            }
            out.println("\t<msg id='" + pathInfo.getHexId() + "' desc='" + pathInfo.description + "'");
            out.println("\t >" + pathInfo.transformValue(path, value) + "</msg>");
            out.flush();
        }
        out.println();
        out.println("</messagebundle>");
        out.close();
    }


    static class PathInfo {
        final String path;
        final Long id;
        final String hexId;
        final String englishValue;
        final Map<String, String> placeholderReplacements;
        final String description;

        public PathInfo(String path, String englishValue, Map<String, String> placeholderReplacements, String description) {
            this.path = path;
            long id = getId(path);
            this.id = id;
            hexId = Long.toHexString(id).toUpperCase(Locale.ROOT);
            this.englishValue = englishValue;
            this.placeholderReplacements = placeholderReplacements;
            this.description = description;
        }

        public String getPath() {
            return path;
        }

        public Long getId() {
            return id;
        }

        public String getHexId() {
            return hexId;
        }

        public String getEnglishValue() {
            return englishValue;
        }

        private static long getId(String path) {
            byte[] hash = digest.digest(path.getBytes()); // safe, because it is all ascii
            long result = 0;
            for (int i = 0; i < 8; ++i) {
                result <<= 8;
                result ^= hash[i];
            }
            // mash the top bit to make things easier
            result &= 0x7FFFFFFFFFFFFFFFL;
            return result;
        }

        public Map<String, String> getPlaceholderReplacements() {
            return placeholderReplacements;
        }

        static DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();

        private String transformValue(String path, String value) {
            String result = TransliteratorUtilities.toHTML.transform(value);
            if (placeholderReplacements == null) {
                // skip
            } else if (result.contains("{0}")) {
                // TODO: fix for quoting
                result = replacePlaceholders(result, placeholderReplacements);
            } else {
                formatParser.set(value);
                StringBuilder buffer = new StringBuilder();
                for (Object item : formatParser.getItems()) {
                    if (item instanceof DateTimePatternGenerator.VariableField) {
                        String variable = item.toString();
                        String replacement = placeholderReplacements.get(variable);
                        if (replacement == null) {
                            throw new IllegalArgumentException("Missing placeholder for " + variable);
                        }
                        buffer.append(replacement);
                    } else {
                        buffer.append(item);
                    }
                }
                result = buffer.toString();
            }
            return result;
        }

        private String replacePlaceholders(String result, Map<String, String> argumentsFromPath) {
            for (Entry<String, String> entry : argumentsFromPath.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
            return result;
        }
    }

    static class EnglishInfo implements Iterable<PathInfo> {

        Map<String, PathInfo> pathToPathInfo = new TreeMap();
        Map<Long, PathInfo> longToPathInfo = new HashMap();

        EnglishInfo(String targetDir, CLDRFile english, CLDRFile root) throws Exception {
            // we don't want the fully resolved paths, but we do want the direct inheritance from root.
            Status status = new Status();
            Map<String, List<Set<String>>> starredPaths = new TreeMap<String,List<Set<String>>>();

            Merger<Map<String, String>> merger = new MyMerger();
            RegexLookup<Map<String,String>> patternPlaceholders = new RegexLookup(GenerateXMB.class, "xmbPlaceholders.txt", new MapTransform(), merger);

            Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(english).get();
            
            StandardCodes sc = StandardCodes.make();
            Map<String, String> zone_country = sc.getZoneToCounty();

            for (String path : sorted) {
                if (pathMatcher != null 
                        && !pathMatcher.reset(path).find()) {
                    continue;
                }
                String description = pathHandling.get(path);
                if (description == null) {
                    description = "***TBD***";
                } else if ("SKIP".equals(description)) {
                    continue;
                }
                String localeWhereFound = english.getSourceLocaleID(path, status);
                if (!status.pathWhereFound.equals(path)) {
                    continue;
                }
                String value = english.getStringValue(path);
                if (value.length() == 0 || contentMatcher != null && !contentMatcher.reset(value).find()) {
                    continue;
                }
                
                List<String> attributes = addStarredInfo(starredPaths, path);
                
                // In special cases, only use if there is a root value (languageNames, ...
                if (description.startsWith("ROOT")) {
                    int typeEnd = description.indexOf(';');
                    String type = description.substring(4,typeEnd).trim();
                    description = description.substring(typeEnd+1).trim();

                    Set<String> codes = sc.getSurveyToolDisplayCodes(type);
                    String code = attributes.get(0);
                    if (!codes.contains(code) && !code.contains("_")) {
                        System.out.println("Skipping code: " + code);
                        continue;
                    }
                    description = MessageFormat.format(description, new Object[]{code});
                }

                // Special case for exemplarCity
                if (path.contains("exemplarCity")) {
                    String regionCode = zone_country.get(attributes.get(0));
                    String englishRegionName = english.getName(CLDRFile.TERRITORY_NAME, regionCode);
                    description = MessageFormat.format(description, new Object[]{englishRegionName});
                }

                Map<String, String> placeholders = patternPlaceholders.get(path);
                PathInfo row = new PathInfo(path, value, placeholders, description);

                Long hash = row.getId();
                if (longToPathInfo.containsKey(hash)) {
                    throw new IllegalArgumentException("Id collision for "
                            + path + " and " + longToPathInfo.get(hash).getPath());
                }
                pathToPathInfo.put(path, row);
                longToPathInfo.put(hash, row);
                if (value.contains("{0}") && patternPlaceholders.get(path) == null) {
                    System.out.println("ERROR: " + path + " ; " + value);
                }
            }

            PrintWriter out = BagFormatter.openUTF8Writer(targetDir, "paths-base.txt");
            for (Entry<String, List<Set<String>>> starredPath : starredPaths.entrySet()) {
                out.println(starredPath.getKey() + "\t\t" + starredPath.getValue());
            }
            out.close();
        }

        static final Matcher starAttributeMatcher = Pattern.compile("=\"([^\"]*)\"").matcher("");

        private List<String> addStarredInfo(Map<String, List<Set<String>>> starredPaths, String path) {
            starAttributeMatcher.reset(path);
            StringBuilder starredPath = new StringBuilder();
            List<String> attributes = new ArrayList<String>();
            int lastEnd = 0;
            while (starAttributeMatcher.find()) {
                int start = starAttributeMatcher.start(1);
                int end = starAttributeMatcher.end(1);
                starredPath.append(path.substring(lastEnd, start));
                starredPath.append(".*");
                
                attributes.add(path.substring(start, end));
                lastEnd = end;
            }
            starredPath.append(path.substring(lastEnd));
            String starredPathString = starredPath.toString();
            List<Set<String>> attributeList = starredPaths.get(starredPathString);
            if (attributeList == null) {
                starredPaths.put(starredPathString, attributeList = new ArrayList<Set<String>>());
            }
            int i = 0;
            for (String attribute : attributes) {
                if (attributeList.size() <= i) {
                    TreeSet<String> subset = new TreeSet<String>();
                    subset.add(attribute);
                    attributeList.add(subset);
                } else {
                    Set<String> subset = attributeList.get(i);
                    subset.add(attribute);
                }
                ++i;
            }
            return attributes;
        }

        @Override
        public Iterator<PathInfo> iterator() {
            return pathToPathInfo.values().iterator();
        }
    }

    static final class MapTransform implements Transform<String, Map<String,String>> {

        @Override
        public Map<String, String> transform(String source) {
            Map<String, String> result = new LinkedHashMap<String, String>();
            try {
                String[] parts = source.split(";\\s+");
                for (String part : parts) {
                    int equalsPos = part.indexOf('=');
                    String id = part.substring(0, equalsPos).trim();
                    String name = part.substring(equalsPos+1).trim();
                    int spacePos = name.indexOf(' ');
                    String example;
                    if (spacePos >= 0) {
                        example = name.substring(spacePos+1).trim();
                        name = name.substring(0,spacePos).trim();
                    } else {
                        example = "";
                    }

                    String old = result.get(id);
                    if (old != null) {
                        throw new IllegalArgumentException("Key occurs twice: " + id + "=" + old + "!=" + name);
                    }
                    // <ph name='x'><ex>xxx</ex>yyy</ph>
                    result.put(id, "<ph name='" + name + "'><ex>" + example+ "</ex>" + id +  "</ph>");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse " + source, e);
            }
            for (Entry<String, String> entry : result.entrySet()) {
                if (DEBUG) System.out.println(entry);
            }
            return result;
        }

    }

    private static final class MyMerger implements Merger<Map<String, String>> {
        @Override
        public Map<String, String> merge(Map<String, String> a, Map<String, String> into) {
            // check unique
            for (String key : a.keySet()) {
                if (into.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate placeholder: " + key);
                }
            }
            into.putAll(a);
            return into;
        }
    }

//    static final class HandlingTransform implements Transform<String, Handling> {
//        @Override
//        public Handling transform(String source) {
//            return Handling.valueOf(source);
//        }
//    }

}
