package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.With;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;

/**
 * Prototype simpler mechanism for converting to ICU Resource Bundles.
 * The format is almost entirely data-driven instead of having lots of special-case code.
 * @author markdavis
 */
public class LDMLConverter {
    private static final boolean DEBUG = false;
    /**
     * What we use as ID characters (actually ASCII would suffice).
     */
    static final UnicodeSet ID_CHARACTERS = new UnicodeSet("[-:[:xid_continue:]]").freeze();
    static final UnicodeSet ID_START = new UnicodeSet("[:xid_start:]").freeze();


    // TODO Handle more than just the main directory.
    static Factory factory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");

    /**
     * The source for path regexes is much simpler if we automatically quote the [ character in front of @.
     */
    public static Transform<String, RegexFinder> RegexFinderTransform = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            return new RegexFinder("^" + source.replace("[@", "\\[@") + "$");
        }
    };

    static final Pattern SEMI = Pattern.compile("\\s*;\\s*");

    /**
     * The value for the regex is a pair, with the directory and the path.
     */
    public static Transform<String, String[]> PairValueTransform = new Transform<String, String[]>() {
        public String[] transform(String source) {
            String[] pair = SEMI.split(source);
            if (pair.length == 3) {
                pair[2] = pair[2].replace("[@", "\\[@");
            } else if (pair.length != 2) {
                throw new IllegalArgumentException("Must be of form directory ; path: " + source);
            }
            return pair;
        }
    };
    
    static final Matcher DATE_OR_TIME_FORMAT = Pattern.compile("/(date|time)Formats/").matcher("");
    
    /**
     * Special hack comparator, so that RB strings come out in the right order. This is only important for the order of items in arrays.
     */
    public static Comparator<String> SpecialLDMLComparator = new Comparator<String>() {

        @Override
        /**
         * Reverse the ordering of the following:
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/displayName ; curr ; /Currencies/$1
         * //ldml/numbers/currencies/currency[@type="([^"]*)"]/symbol ; curr ; /Currencies/$1
         * and the following (time/date)
         * //ldml/dates/calendars/calendar[@type="([^"]*)"]/(dateFormats|dateTimeFormats|timeFormats)/(?:[^/\[]*)[@type="([^"]*)"]/(?:[^/\[]*)[@type="([^"]*)"]/.* ; locales ; /calendar/$1/DateTimePatterns
         */
        public int compare(String arg0, String arg1) {
            if (arg0.startsWith("//ldml/numbers/currencies/currency") 
                    && arg1.startsWith("//ldml/numbers/currencies/currency")) {
                int last0 = arg0.lastIndexOf('/');
                int last1 = arg1.lastIndexOf('/');
                if (last0 == last1 && arg0.regionMatches(0, arg1, 0, last1)) {
                    return - arg0.substring(last0, arg0.length()).compareTo(arg1.substring(last1, arg1.length()));
                }
            }
            if (arg0.startsWith("//ldml/dates/calendars/calendar") 
                    && arg1.startsWith("//ldml/dates/calendars/calendar") 
                    ) {
               if (DATE_OR_TIME_FORMAT.reset(arg0).find()) {
                   int start0 = DATE_OR_TIME_FORMAT.start();
                   int end0 = DATE_OR_TIME_FORMAT.end();
                   if (DATE_OR_TIME_FORMAT.reset(arg1).find()) {
                       int start1 = DATE_OR_TIME_FORMAT.start();
                       int end1 = DATE_OR_TIME_FORMAT.end();
                       if (start0 == start1 && arg0.regionMatches(0, arg1, 0, start1) && !arg0.regionMatches(0, arg1, 0, end1)) {
                           return - arg0.substring(start0, arg0.length()).compareTo(arg1.substring(start1, arg1.length()));
                       }
                   }
               }
            }
            return CLDRFile.ldmlComparator.compare((String)arg0, (String)arg1);
        }
    };

    /**
     * Loads the data in from a file. That file is of the form cldrPath ; rbPath
     */
    static RegexLookup<String[]> pathConverter =
        new RegexLookup<String[]>()
        .setPatternTransform(RegexFinderTransform)
        .setValueTransform(PairValueTransform)
        .loadFromFile(LDMLConverter.class, "ldml2icu.txt");

        /**
         * ICU paths have a simple comparison, alphabetical within a level. We do have to catch the / so that it is lower than everything.
         */
        public static final Comparator<String> PATH_COMPARATOR = new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                int min = Math.min(arg0.length(), arg1.length());
                for (int i = 0; i < min; ++i) {
                    int ch0 = arg0.charAt(i);
                    int ch1 = arg1.charAt(i);
                    int diff = ch0 - ch1;
                    if (diff == 0) {
                        continue;
                    }
                    if (ch0 == '/') {
                        return -1;
                    } else if (ch1 == '/') {
                        return 1;
                    } else {
                        return diff;
                    }
                }
                return arg0.length() - arg1.length();
            }
        };

        static class MultiFileOutput {
            //Map<String,Map<String,List<String>>> multiOutput = new HashMap<String,Map<String,List<String>>>();
            Map<String,Map<String,List<String>>> dir2path2values = new HashMap<String,Map<String,List<String>>>();
            
            public void clear() {
                dir2path2values.clear();
            }
            // PATH_COMPARATOR

            /**
             * The RB path,value pair actually has an array as the value. So when we add to it, add to a list.
             * @param path
             * @param value
             * @param path2values
             * @return
             */
            void add(String directory, String file, String path, String value) {
                path = "/" + file + path;
                if (DEBUG) System.out.println("+++\t" + path + "\t" + value);
                boolean newDirectory = false;
                Map<String, List<String>> path2values = dir2path2values.get(directory);
                if (path2values == null) {
                    dir2path2values.put(directory, path2values = new TreeMap<String, List<String>>(PATH_COMPARATOR));
                    newDirectory = true;
                }
                List<String> list = path2values.get(path);
                if (list == null) {
                    path2values.put(path, list = new ArrayList<String>(1));
                }
                list.add(value);
                if (newDirectory) {
                    // Special-case the version
                    // TODO figure out where to get this from
                    add(directory, file, "/Version", "2.0.58.6");
                }
            }
            /**
             * Write a file in ICU format. LDML2ICUConverter currently has some funny formatting in a few cases; don't try to match everything.
             * @param output2
             * @param path2values
             */
            void writeRB(String dirPath, String file) {
                try {
                    boolean wasSingular = false;
                    String[] replacements = { "%file%", file };
                    for (Entry<String, Map<String, List<String>>> dirAndPath2values : dir2path2values.entrySet()) {
                        String dir = dirAndPath2values.getKey();
                        PrintWriter out = BagFormatter.openUTF8Writer(dirPath + "/" + dir, file + ".txt");
                        FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
                        Map<String, List<String>> path2values = dirAndPath2values.getValue();
                        String[] lastLabels= new String[] {};

                        for (Entry<String, List<String>> entry : path2values.entrySet()) {
                            String path = entry.getKey();
                            List<String> values = entry.getValue();
                            String[] labels = path.split("/");
                            int common = getCommon(lastLabels, labels);
                            for (int i = lastLabels.length - 1; i > common; --i) {
                                if (wasSingular) {
                                    wasSingular = false;
                                } else {
                                    out.append(Utility.repeat(TAB, i-1));
                                }
                                out.append("}\n");
                            }
                            for (int i = common + 1; i < labels.length; ++i) {
                                final String pad = Utility.repeat(TAB, i-1);
                                out.append(pad);
                                out.append(quoteIfNeeded(labels[i]) + "{");
                                if (i != labels.length-1) {
                                    out.append('\n');
                                }
                            }
                            int maxWidth = 76;
                            if (values.size() == 1) {
                                String value = values.iterator().next();
                                boolean quote = !path.endsWith(":int");
                                if (quote) {
                                    value = quoteInside(value);
                                }
                                if (value.length() <= maxWidth) {
                                    appendQuoted(value, quote, out);
                                    wasSingular = true;
                                } else {
                                    final String pad = Utility.repeat(TAB, labels.length-1);
                                    out.append('\n');
                                    int end;
                                    for (int i = 0; i < value.length(); i = end) {
                                        end = goodBreak(value, i + maxWidth);
                                        String part = value.substring(i, end);
                                        out.append(pad);
                                        appendQuoted(part, quote, out).append('\n');
                                    }
                                    wasSingular = false;
                                }
                            } else {
                                out.append('\n');
                                final String pad = Utility.repeat(TAB, labels.length-1);
                                for (String item : values) {
                                    out.append(pad);
                                    out.append('"');
                                    out.append(quoteInside(item));
                                    out.append("\",\n");
                                }
                                wasSingular = false;
                            }
                            out.flush();
                            lastLabels = labels;
                        }
                        // finish last
                        for (int i = lastLabels.length - 1; i > 0; --i) {
                            if (wasSingular) {
                                wasSingular = false;
                            } else {
                                out.append(Utility.repeat(TAB, i-1));
                            }
                            out.append("}\n");
                        }
                        out.flush();
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            private PrintWriter appendQuoted(String value, boolean quote, PrintWriter out) {
                if (quote) {
                    return out.append('"').append(value).append('"');
                } else {
                    return out.append(value);
                }
            }
            /**
             * Can a string be broken here? If not, backup until we can.
             * @param quoted
             * @param end
             * @return
             */
            private static int goodBreak(String quoted, int end) {
                if (end > quoted.length()) {
                    return quoted.length();
                }
                while (end > 0) {
                    char ch = quoted.charAt(end-1);
                    if (ch != '\\' && (ch < '\uD800' || ch > '\uDFFF')) {
                        break;
                    }
                    --end;
                }
                return end;
            }

            /**
             * Get items
             * @return
             */
            public Set<Entry<String, Map<String, List<String>>>> entrySet() {
                return dir2path2values.entrySet();
            }
        }

        /**
         * In this prototype, just convert one file.
         * 
         * @param args
         * @throws IOException
         */
        public static void main(String[] args) throws IOException {

            MultiFileOutput output = new MultiFileOutput();
            Status status = new Status();

            String[] files = {
                    "en", 
                    "fr"};
            Set<Matcher> fullMatches = new HashSet<Matcher>();
            Set<String> allPaths = new TreeSet<String>(SpecialLDMLComparator);
            for (String file : files) {
                CLDRFile cldr = factory.make(file, false);
                Output<String[]> arguments = new Output<String[]>();
                output.clear();
                // copy the relevant path/data to the output, changing as required.

                // first find all the items that need to be 'fleshed out'
                fullMatches.clear();
                allPaths.clear();
                for (String path : cldr) {
                    String[] dirAndRbPath = getConvertedValues(cldr, path, arguments);
                    if (dirAndRbPath == null) {
                        continue;
                    }
                    allPaths.add(path);
                    if (dirAndRbPath.length >= 3) {
                        String pathRegex = pathConverter.replace(dirAndRbPath[2], arguments.value);
                        fullMatches.add(Pattern.compile(pathRegex).matcher(""));
                    }
                }
                
                // now get all the resolved items that we need to flesh out with
                CLDRFile cldrResolved = factory.make(file, true);

                for (String path : cldrResolved) {
                    for (Matcher matcher : fullMatches) {
                        if (matcher.reset(path).matches()) {
                            allPaths.add(path);
                            break;
                        }
                    }
                }
                
                // now convert to ICU format
                
                for (String path : allPaths) {
                    String value = cldrResolved.getStringValue(path);
                    addPath(cldrResolved, file, path, value, arguments, output);
                }

                output.writeRB(CldrUtility.GEN_DIRECTORY + "rb/", file);
            }
        }

        private static void addPath(CLDRFile cldr, String file, String path, String value, Output<String[]> arguments, MultiFileOutput output) {            
            String[] dirAndRbPath = getConvertedValues(cldr, path, arguments);
            if (dirAndRbPath == null) {
                return;
            }
            String rbPath = pathConverter.replace(dirAndRbPath[1], arguments.value);
            // special hack for commonlyUsed
            if (rbPath.contains("cu:int")) {
                value = value.equals("false") ? "0" : "1";
            }
            output.add(dirAndRbPath[0], file, rbPath, value);            
        }

        private static String[] getConvertedValues(CLDRFile cldr, String path, Output<String[]> arguments) {
            String fullPath = cldr.getFullXPath(path);
            if (fullPath.contains("[@draft")) {
                if (!fullPath.contains("[@draft=\"contributed\"]")) {
                    return null;
                }
            }
            String[] dirAndRbPath = pathConverter.get(path, null, arguments);
            if (dirAndRbPath == null) {
                System.out.println("Can't convert " + path);
                return null;
            }
            return dirAndRbPath;
        }
        
        /**
         * The default tab indent (actually spaces)
         */
        final static String TAB = "    ";

        /**
         * Quote numeric identifiers (because LDML2ICUConverter does).
         * @param item
         * @return
         */
        private static String quoteIfNeeded(String item) {
            return item.contains(":") && !item.contains("cu:int") ? quote(item) : item;
            //item.isEmpty() || (ID_CHARACTERS.containsAll(item) && ID_START.contains(item.codePointAt(0))) ? item : quote(item); 
        }

        /**
         * Fix characters inside strings.
         * @param item
         * @return
         */
        private static String quote(String item) {
            return new StringBuilder().append('"').append(quoteInside(item)).append('"').toString();
        }

        /**
         * Fix characters inside strings.
         * @param item
         * @return
         */
        private static String quoteInside(String item) {
            if (item.contains("\"")) {
                item = item.replace("\"", "\\\"");
            }
            return item;
        }

        /**
         * find the initial labels (from a path) that are identical.
         * @param item
         * @return
         */
        private static int getCommon(String[] lastLabels, String[] labels) {
            int min = Math.min(lastLabels.length, labels.length);
            int i;
            for (i = 0; i < min; ++i) {
                if (!lastLabels[i].equals(labels[i])) {
                    return i-1;
                }
            }
            return i;
        }

}
