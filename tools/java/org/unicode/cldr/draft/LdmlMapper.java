package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.Merger;
import org.unicode.cldr.util.RegexLookup.RegexFinder;

import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.Transform;

public abstract class LdmlMapper {
    private static final Pattern SEMI = Pattern.compile("\\s*+;\\s*+");

    private String converterFile;

    protected static class FullMatcher extends RegexFinder {
        public FullMatcher(String pattern) {
            super(pattern);
        }
        
        public boolean find(String item, Object context) {
            return matcher.reset(item).matches();
        }
    }

    /**
     * A wrapper class for storing and working with the unprocessed values of a RegexResult.
     */
    public static class PathValueInfo {
        private static final Pattern QUOTES = Pattern.compile("\"([^\"]++)\"");

        private String rbPath;
        private String[] rawValues;
        private boolean isArray;

        public PathValueInfo(String rbPath, String[] rawValues, boolean isArray) {
            this.rbPath = rbPath;
            this.rawValues = rawValues;
            this.isArray = isArray;
        }

        public static PathValueInfo make(String rbPath, String valueArg, boolean isArray) {
            if (valueArg == null) return new PathValueInfo(rbPath, null, isArray);
            // Split up values using spaces unless enclosed by non-escaped quotes,
            // e.g. a "b \" c" would become {"a", "b \" c"}
            List<String> args = new ArrayList<String>();
            StringBuffer valueBuffer = new StringBuffer();
            boolean shouldSplit = true;
            char lastChar = ' ';
            for (char c : valueArg.toCharArray()) {
                if (c == '"' && lastChar != '\\') {
                    shouldSplit = !shouldSplit;
                } else if (c == ' ' && shouldSplit) {
                    args.add(valueBuffer.toString());
                    valueBuffer.setLength(0);
                } else {
                    valueBuffer.append(c);
                }
                lastChar = c;
            }
            args.add(valueBuffer.toString());
            String[] rawValues = new String[args.size()];
            args.toArray(rawValues);
            return new PathValueInfo(rbPath, rawValues, isArray);
        }

        /**
         * @param arguments the arguments retrieved from the regex corresponding to this PathValueInfo
         * @return the processed rb path
         */
        public String processRbPath(String[] arguments) {
            String path = processString(rbPath, arguments);
            // Replace slashes in metazone names,
            // e.g. "America/Argentina/La_Rioja"
            Matcher matcher = QUOTES.matcher(path);
            if (matcher.find()) {
                path = path.substring(0, matcher.start(1))
                    + matcher.group(1).replace('/', ':')
                    + path.substring(matcher.end(1)); 
            }
            return path;
        }
        
        public String[] processValues(String[] arguments, CLDRFile cldrFile, String xpath) {
            if (rawValues == null) {
                return new String[] { getStringValue(cldrFile, xpath) };
            }
            // Split single args using spaces.
            if (rawValues.length == 1) {
                return processValue(rawValues[0], xpath, cldrFile, arguments).split("\\s++");
            }
            String[] values = new String[rawValues.length];
            for (int i = 0; i < rawValues.length; i++) {
                values[i] = processValue(rawValues[i], xpath, cldrFile, arguments);
            }
            return values;
        }
        
        private String processValue(String value, String xpath, CLDRFile cldrFile, String[] arguments) {
            value = processString(value, arguments);
            if (value.equals("{value}")) {
                value = getStringValue(cldrFile, xpath);
            }
            return value;
        }
        
        @Override
        public String toString() { return rbPath + "=" + rawValues; }
        
        public boolean isArray() { return isArray; }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PathValueInfo) {
                PathValueInfo otherInfo = (PathValueInfo)o;
                return rbPath.equals(otherInfo.rbPath)
                        && rawValues.equals(otherInfo.rawValues);
            } else {
                return false;
            }
        }
    }
    
    private static String processString(String value, String[] arguments) {
        if (value == null) {
            return null;
        }
        try {
            return RegexLookup.replace(value, arguments);
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Error while filling out arguments in " + value + " with " + Arrays.asList(arguments), e);
        }
    }

    static class PathValuePair {
        String path;
        String[] values;
        boolean isArray;
        
        public PathValuePair(String path, String[] values, boolean isArray) {
            // TODO: merge with CldrValue/IcuData
            this.path = path;
            this.values = values;
            this.isArray = isArray;
        }
    }

    private static class Argument {
        private int argNum;
        private boolean shouldSplit = true;
        public Argument(int argNum, boolean shouldSplit) {
            this.argNum = argNum;
            this.shouldSplit = shouldSplit;
        }
    }

    static class RegexResult implements Iterable<PathValueInfo> {
        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^\\$(\\d+)=(//.*)");
        // Matches arguments with or without enclosing quotes.
        private static final Pattern ARGUMENT = Pattern.compile("\"?\\$(\\d)\"?");

        private Set<PathValueInfo> unprocessed;
        private Map<Integer, String> requiredArgs;
        private Argument[] rbArgs;

        public RegexResult(String rbPath, String rawValues,
                    Map<Integer, String> requiredArgs, Argument[] rbArgs,
                    boolean isArray) {
            unprocessed = new HashSet<PathValueInfo>();
            unprocessed.add(PathValueInfo.make(rbPath, rawValues, isArray));
            this.requiredArgs = requiredArgs;
            this.rbArgs = rbArgs;
        }

        /**
         * Merges this result with another RegexResult.
         * @param otherResult
         */
        public void merge(RegexResult otherResult) {
            for (PathValueInfo struct : otherResult.unprocessed) {
                unprocessed.add(struct);
            }
        }

        /**
         * Each RegexResult is only accessible if its corresponding regex
         * matched. However, there may be additional requirements imposed in order
         * for it to be a valid match, i.e. the arguments retrieved from the regex
         * must also match. This method checks that specified arguments match
         * the requirements for this RegexResult.
         * NOTE: LocaleMapper only
         * @param file
         * @param arguments
         * @return true if the arguments matched
         */
        public boolean argumentsMatch(CLDRFile file, String[] arguments) {
            for (int argNum : requiredArgs.keySet()) {
                if (arguments.length <= argNum) {
                    throw new IllegalArgumentException("Argument " + argNum + " missing");
                }
                String argFromCldr = getStringValue(file, requiredArgs.get(argNum));
                if (argFromCldr != null && !arguments[argNum].equals(argFromCldr)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Iterator<PathValueInfo> iterator() {
            return unprocessed.iterator();
        }

        // NOTE: only needed by SupplementalMapper
        public List<PathValuePair> processResult(CLDRFile cldrFile, String xpath,
                String[] arguments) {
            List<PathValuePair> processed = new ArrayList<PathValuePair>(unprocessed.size());
            for (PathValueInfo struct : unprocessed) {
                String[] values = struct.processValues(arguments, cldrFile, xpath);
                // Check if there are any arguments that need splitting for the rbPath.
                String[] newArgs = arguments.clone();
                boolean splitNeeded = false;
                for (Argument arg : rbArgs) {
                    if (arg.shouldSplit) {
                        int argIndex = arg.argNum;
                        String[] splitArgs = arguments[argIndex].split("\\s++");
                        // Only split the first splittable argument needed for each rbPath.
                        if (splitArgs.length > 1) {
                            for (String splitArg : splitArgs) {
                                newArgs[argIndex] = splitArg;
                                String rbPath = struct.processRbPath(newArgs);
                                processed.add(new PathValuePair(rbPath, values, struct.isArray));
                            }
                            splitNeeded = true;
                            break;
                        }
                    }
                }
                // No splitting required, process as per normal.
                if (!splitNeeded) {
                    String rbPath = struct.processRbPath(arguments);
                    processed.add(new PathValuePair(rbPath, values, struct.isArray));
                }
             }
            return processed;
        }
    }

    /**
     * Wrapper class for the interim form of a CLDR value before it is added
     * into an IcuData object.
     */
    protected class CldrValue {
        private String xpath;
        private String value;
        private boolean isArray;

        public CldrValue(String xpath, String value, boolean isArray) {
            this.xpath = xpath;
            this.value = value;
            this.isArray = isArray;
        }

        public String getValue() { return value; }
        
        public String getXpath() { return xpath; }

        public boolean isArray() { return isArray; }
    }

    private static Merger<RegexResult> RegexValueMerger = new Merger<RegexResult>() {
        @Override
        public RegexResult merge(RegexResult a, RegexResult into) {
            into.merge(a);
            return into;
        }
    };

    private static Transform<String, Finder> regexTransform = new Transform<String, Finder>() {
        @Override
        public Finder transform(String source) {
            return new FullMatcher(source);
        }
    };

    /**
     * Checks if two strings match a specified pattern.
     * @param pattern the pattern to be matched against
     * @param arg0
     * @param arg1
     * @param matchers a 2-element array to contain the two matchers. The
     *        array is not guaranteed to be filled if matches() returns false
     * @return true if both strings successfully matched the pattern
     */
    protected static boolean matches(Pattern pattern, String arg0, String arg1, Matcher[] matchers) {
        return (matchers[0] = pattern.matcher(arg0)).matches()
                && (matchers[1] = pattern.matcher(arg1)).matches();
    }

    /**
     * @param cldrFile
     * @param xpath
     * @return the value of the specified xpath (fallback or otherwise)
     */
    protected static String getStringValue(CLDRFile cldrFile, String xpath) {
        String value = cldrFile.getStringValue(xpath);
        return value;
    }

    RegexLookup<FallbackInfo> fallbackConverter;
    RegexLookup<RegexResult> xpathConverter;

    // One FallbackInfo object for every type of rbPath.
    protected class FallbackInfo implements Iterable<R3<Finder, String, List<String>>> {
        private List<R3<Finder, String, List<String>>> fallbackItems;

        public FallbackInfo(Finder xpathMatcher, String fallbackXpath, String fallbackValue) {
            fallbackItems = new ArrayList<R3<Finder, String, List<String>>>();
            List<String> values = new ArrayList<String>();
            values.add(fallbackValue);
            fallbackItems.add(new R3<Finder, String, List<String>>(xpathMatcher, fallbackXpath, values));
        }

        private R3<Finder, String, List<String>> getItem(Finder finder) {
            for (R3<Finder, String, List<String>> item : fallbackItems) {
                if (item.get0().equals(finder)) return item;
            }
            return null;
        }

        public FallbackInfo merge(FallbackInfo newInfo) {
            for (R3<Finder, String, List<String>> newItem : newInfo.fallbackItems) {
                R3<Finder, String, List<String>> item = getItem(newItem.get0());
                if (item == null) fallbackItems.add(newItem);
            }
            return this;
        }
        
        @Override
        public Iterator<R3<Finder, String, List<String>>> iterator() {
            return fallbackItems.iterator();
        }
    }

    /**
     * All child classes should call this constructor.
     * @param converterFile
     */
    protected LdmlMapper(String converterFile) {
        this.converterFile = converterFile;
    }

    /**
     * @return a RegexLookup for matching rb paths that may require fallback
     * values.
     */
    protected RegexLookup<FallbackInfo> getFallbackConverter() {
        if (fallbackConverter == null) {
            loadConverters();
        }
        return fallbackConverter;
    }
 
    /**
     * @return a RegexLookup for matching xpaths
     */
    protected RegexLookup<RegexResult> getPathConverter() {
        if (xpathConverter == null) {
            loadConverters();
        }
        return xpathConverter;
    }
    
    private void loadConverters() {
        xpathConverter = new RegexLookup<RegexResult>()
            .setValueMerger(RegexValueMerger)
            .setPatternTransform(regexTransform);
        fallbackConverter = new RegexLookup<FallbackInfo>()
            .setValueMerger(new Merger<FallbackInfo>() {
                @Override
                public FallbackInfo merge(FallbackInfo a, FallbackInfo into) {
                    return into.merge(a);
                }
            });
        BufferedReader reader = FileUtilities.openFile(LDMLConverter.class, converterFile);
        VariableReplacer variables = new VariableReplacer();
        String line;
        int lineNum = 0;
        try {
            while((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) continue;
                // Read variables.
                if (line.charAt(0) == '%') {
                    int pos = line.indexOf("=");
                    if (pos < 0) {
                        throw new IllegalArgumentException(
                            "Failed to read RegexLookup File " + converterFile +
                            "\t\t(" + lineNum + ") " + line);
                    }
                    variables.add(line.substring(0,pos).trim(), line.substring(pos+1).trim());
                    continue;
                }
                if (line.contains("%")) {
                    line = variables.replace(line);
                }
                processLine(line);
            }
        } catch(IOException e) {
            System.err.println("Error reading " + converterFile + " at line " + lineNum);
            e.printStackTrace();
        }
    }
    
    /**
     * Processes a line in the input file for xpath conversion.
     * @param line
     */
    private void processLine(String line) {
        String[] content = line.split(SEMI.toString());
        // xpath ; rbPath ; value
        // Create a reverse lookup for rbPaths to xpaths.
        Finder xpathMatcher = new RegexFinder(content[0].replace("[@", "\\[@"));
        String rbPath = content[1];
        
        // Find arguments in rbPath.
        Matcher matcher = RegexResult.ARGUMENT.matcher(rbPath);
        List<Argument> argList = new ArrayList<Argument>();
        while (matcher.find()) {
            boolean shouldSplit = !(rbPath.charAt(matcher.start()) == '"' &&
                    rbPath.charAt(matcher.end() - 1) == '"');
            argList.add(new Argument(Integer.parseInt(matcher.group(1)), shouldSplit));
        }
        Argument[] rbArgs = new Argument[argList.size()];
        argList.toArray(rbArgs);

        // Parse special instructions.
        String value = null;
        Map<Integer, String> requiredArgs = new HashMap<Integer, String>();
        boolean isArray = false;
        for (int i = 2; i < content.length; i++) {
            String instruction = content[i];
            Matcher argMatcher;
            if (instruction.startsWith("values=")) {
                value = instruction.substring(7);
            } else if (instruction.equals("array")) {
                isArray = true;
            } else if (instruction.startsWith("fallback=")) {
                // WARNING: fallback might backfire if more than one type of xpath for the same rbpath
                String fallbackValue = instruction.substring(9);
                addFallback(xpathMatcher, rbPath, fallbackValue);
            } else if ((argMatcher = RegexResult.ARGUMENT_PATTERN.matcher(instruction)).matches()) {
                requiredArgs.put(Integer.parseInt(argMatcher.group(1)),
                        argMatcher.group(2));
            }
        }
        xpathConverter.add(xpathMatcher, new RegexResult(rbPath, value, requiredArgs, rbArgs, isArray));
    }

    /**
     * Adds an entry to the fallback converter.
     * @param xpathMatcher the xpath matcher that determines if a fallback value
     *        is necessary
     * @param rbPath the rb path that the fallback value is for
     * @param fallbackValue the fallback value
     */
    private void addFallback(Finder xpathMatcher, String rbPath, String fallbackValue) {
        ArrayList<StringBuffer> args = new ArrayList<StringBuffer>();
        int numBraces = 0;
        int argNum = 0;
        // Create RB path matcher and xpath replacement.
        // WARNING: doesn't currently take lookaround groups into account.
        StringBuffer xpathReplacement = new StringBuffer();
        for (char c : xpathMatcher.toString().toCharArray()) {
            boolean isBrace = false;
            if (c == '(') {
                numBraces++;
                argNum++;
                args.add(new StringBuffer());
                isBrace = true;
                if (numBraces == 1) {
                    xpathReplacement.append('$').append(argNum);
                }
            }
            if (numBraces > 0) {
                for (int i = args.size() - numBraces; i < args.size(); i++) {
                    args.get(i).append(c);
                }
            }
            if (c == ')') {
                numBraces--;
                isBrace = true;
            }
            if (!isBrace && numBraces == 0) {
                xpathReplacement.append(c);
            }
        }
        String fallbackXpath = xpathReplacement.toString().replaceAll("\\\\\\[", "[");
        if (fallbackXpath.contains("(")) {
            System.err.println("Warning: malformed xpath " + fallbackXpath);
        }

        // Create rb matcher.
        Matcher matcher = RegexResult.ARGUMENT.matcher(rbPath);
        StringBuffer rbPattern = new StringBuffer();
        int lastIndex = 0;
        while (matcher.find()) {
            rbPattern.append(rbPath.substring(lastIndex, matcher.start()));
            argNum = Integer.parseInt(matcher.group(1)) - 1;
            rbPattern.append(args.get(argNum));
            lastIndex = matcher.end();
        }
        rbPattern.append(rbPath.substring(lastIndex));
        fallbackConverter.add(rbPattern.toString(), new FallbackInfo(xpathMatcher, fallbackXpath, fallbackValue));
    }

    /**
     * Checks if any fallback values are required and adds them to the specified
     * map.
     * @param pathValueMap
     */
    protected void addFallbackValues(Map<String, List<CldrValue>> pathValueMap) {
        RegexLookup<FallbackInfo> fallbackConverter = getFallbackConverter();
        for (String rbPath : pathValueMap.keySet()) {
            Output<String[]> arguments = new Output<String[]>();
            FallbackInfo fallbackInfo = fallbackConverter.get(rbPath, null, arguments);
            if (fallbackInfo == null) continue;
            List<CldrValue> values = pathValueMap.get(rbPath);
            for (R3<Finder, String, List<String>> info : fallbackInfo) {
                boolean fallbackNeeded = true;
                Finder finder = info.get0();
                for (CldrValue value : values) {
                    if (finder.find(value.getXpath(), null)) {
                        fallbackNeeded = false;
                        break;
                    }
                }
                if (fallbackNeeded) {
                    // The fallback xpath is just for sorting purposes.
                    String fallbackXpath = processString(info.get1(), arguments.value);
                    // Sanity check.
                    if (fallbackXpath.contains("$")) {
                        System.err.println("Warning: " + fallbackXpath + " for " + rbPath +
                            " still contains unreplaced arguments.");
                    }
                    List<String> fallbackValues = info.get2();
                    for (String value : fallbackValues) {
                        value = processString(value, arguments.value);
                        values.add(new CldrValue(fallbackXpath, value, false));
                    }
                }
            }
        }
    }
}
