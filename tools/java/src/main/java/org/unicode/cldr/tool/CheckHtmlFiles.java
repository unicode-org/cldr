package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.TransliteratorUtilities;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

@CLDRTool(alias = "checkhtmlfiles", description = "Look for errors in CLDR documentation tools", hidden = "Used for CLDR process")
public class CheckHtmlFiles {

    static final Set<String> NOPOP = new HashSet<>(Arrays.asList("br", "img", "link", "meta", "!doctype", "hr", "col", "input"));

    static final EnumSet<Type> SUPPRESS = EnumSet.of(
        Type.ELEMENT, Type.ELEMENT_START, Type.ELEMENT_END, Type.ELEMENT_POP,
        Type.ATTRIBUTE, Type.ATTRIBUTE_CONTENT);

    final static Options myOptions = new Options();
    final static Writer LOG = new OutputStreamWriter(System.out);
    static Pattern WELLFORMED_HEADER = PatternCache.get("\\s*(\\d+(\\.\\d+)*\\s*).*");
    static Pattern SUPPRESS_SECTION_NUMBER = PatternCache.get(
        "(Annex [A-Z]: .*)" +
            "|(Appendix [A-Z].*)" +
            "|(.*Migrati(on|ng).*)" +
            "|Step \\d+.*" +
            "|Example \\d+.*" +
            "|D\\d+\\.\\s.*" +
            "|References" +
            "|Acknowledge?ments" +
            "|Rights to .*Images" +
            "|Modifications" +
            "|(Revision \\d+\\.?)");
    static Pattern SUPPRESS_REVISION = PatternCache.get("Revision \\d+\\.?");
    static Pattern SPACES = PatternCache.get("\\s+");

    enum MyOptions {
//        old(".*", Settings.OTHER_WORKSPACE_DIRECTORY + "cldr-archive/cldr-22.1/specs/ldml/tr35\\.html", "source data (regex)"),
        target(".*", CLDRPaths.BASE_DIRECTORY + "specs" + File.separator + "ldml" + File.separator +
            "tr35(-.*)?\\.html", "target data (regex); ucd for Unicode docs; "
                + "for others use the format -t ${workspace_loc}/unicode-draft/reports/tr51/tr51.html"), verbose(".*", "none", "verbose debugging messages"),
//        contents(".*", CLDRPaths.BASE_DIRECTORY + "specs/ldml/tr35(-.*)?\\.html", "generate contents"),
        // /cldr-archive
        ;

        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    enum Verbosity {
        none, element, all;
        static Verbosity of(String input) {
            return input == null ? Verbosity.none : Verbosity.valueOf(input.toLowerCase(Locale.ROOT));
        }
    }

    static Verbosity verbose;
    static boolean doContents;
    static boolean isLdml;

    public static void main(String[] args) throws IOException {
        System.out.println("First do a replace of <a\\s+name=\"([^\"]*)\"\\s*> by <a name=\"$1\" href=\"#$1\">");
        System.out.println("Then check for all links with no anchors: <a([^>]*)></a>");
        System.out.println("Then check for all links that don't start with name or href <a (?!href|name)");

        myOptions.parse(MyOptions.target, args, true);
        verbose = Verbosity.of(MyOptions.verbose.option.getValue());

        String targetString = MyOptions.target.option.getValue();
        if (targetString.contains("ldml")) {
            isLdml = true;
        }
        if (targetString.equalsIgnoreCase("ucd")) {
            targetString = CLDRPaths.BASE_DIRECTORY + "../unicode-draft/reports/tr(\\d+)/tr(\\d+).html";
        } else if (targetString.equalsIgnoreCase("security")) {
            targetString = CLDRPaths.BASE_DIRECTORY + "../unicode-draft/reports/tr(3[69])/tr(3[69]).html";
        }
        Data target = new Data().getSentences(targetString);
        if (target.count == 0) {
            throw new IllegalArgumentException("No files matched with " + targetString);
        }

        if (isLdml) {
            checkForDtd(target);
        }

        System.out.println("*TOTAL COUNTS*  files:" + target.count + ", fatal errors:" + target.totalFatalCount + ", nonfatal errors:"
            + target.totalErrorCount);
        if (target.totalFatalCount > 0 || target.totalErrorCount > 0) {
            System.exit(1); // give an error status
        }

        System.exit(0);

//        Data source = new Data().getSentences(MyOptions.old.option.getValue());
//        String file = MyOptions.target.option.getValue();
//
//        Data target = new Data().getSentences(file);
//
//        int missingCount = 0, extraCount = 0;
//        int line = 0;
//        for (String sentence : source) {
//            ++line;
//            long sourceCount = source.getCount(sentence);
//            long targetCount = target.getCount(sentence);
//            if (targetCount == 0) {
//                System.out.println(line + "\tMISSING:\t" + sourceCount + "≠" + targetCount + "\t" + sentence);
//                ++missingCount;
//            }
//        }
//        line = 0;
//        for (String sentence : target) {
//            ++line;
//            long sourceCount = source.getCount(sentence);
//            long targetCount = target.getCount(sentence);
//            if (sourceCount == 0) {
//                System.out.println(line + "\tEXTRA:\t" + targetCount + "≠" + sourceCount + "\t" + sentence);
//                ++extraCount;
//            }
//        }
//        System.out.println("Missing:\t" + missingCount);
//        System.out.println("Extra:\t" + extraCount);
    }

    private static final Set<String> SKIP_ATTR = ImmutableSet.of("draft", "alt", "references", "cldrVersion", "unicodeVersion");

    private static void checkForDtd(Data target) {
        M4<String, String, DtdType, Boolean> typeToElements = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), Boolean.class);
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) continue;
            DtdData dtdData = DtdData.getInstance(type);
            Set<Element> elements = dtdData.getElements();
            for (Element element : elements) {
                if (element.isDeprecated()
                    || element.equals(dtdData.PCDATA)
                    || element.equals(dtdData.ANY)) continue;
                typeToElements.put(element.name, element.toDtdString(), type, Boolean.TRUE);
            }
            Set<Attribute> attributes = dtdData.getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute.isDeprecated()) continue;
                if (SKIP_ATTR.contains(attribute.name)) {
                    continue;
                }
                typeToElements.put(attribute.element.name, attribute.appendDtdString(new StringBuilder()).toString(), type, Boolean.TRUE);
            }
        }
        final Map<String, String> skeletonToInFile = new HashMap<>();
        Relation<String, String> extra = new Relation(new TreeMap(), TreeSet.class);
        for (R4<String, String, String, Boolean> elementItem : target.dtdItems.rows()) {
            String file = elementItem.get0();
            String element = elementItem.get1();
            String item = elementItem.get2();
            extra.put(element, item);
            skeletonToInFile.put(item.replace(" ", ""), item);
        }
        ChainedMap.M4<String, String, DtdType, Comparison> status = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), Comparison.class);
        for (R4<String, String, DtdType, Boolean> entry : typeToElements.rows()) {
            final String element = entry.get0();
            final String key = entry.get1();
            final DtdType dtdType = entry.get2();
            String spaceless = key.replace(" ", "");
            String realKey = skeletonToInFile.get(spaceless);
            if (realKey == null) {
                status.put(element, key, dtdType, Comparison.missing);
            } else {
                boolean found = extra.remove(element, realKey);
                if (!found) {
                    status.put(element, key, dtdType, Comparison.no_rem);
                }
            }
        }
        for (Entry<String, String> extraItem : extra.entrySet()) {
            status.put(extraItem.getKey(), extraItem.getValue(), DtdType.ldmlICU, Comparison.extra);
        }
        TreeSet<String> reverse = new TreeSet<>(Collections.reverseOrder());
        for (Entry<String, Map<String, Map<DtdType, Comparison>>> entry1 : status) {
            String element = entry1.getKey();
            reverse.clear();
            final Map<String, Map<DtdType, Comparison>> itemToDtdTypeToComparison = entry1.getValue();
            reverse.addAll(itemToDtdTypeToComparison.keySet());
            for (String item : reverse) {
                Map<DtdType, Comparison> typeToComparison = itemToDtdTypeToComparison.get(item);
                for (Entry<DtdType, Comparison> entry2 : typeToComparison.entrySet()) {
                    System.out.println(element
                        + "\t" + entry2.getValue()
                        + "\t" + CldrUtility.ifSame(entry2.getKey(), DtdType.ldmlICU, "")
                        + "\t" + item);
                }
            }
        }
    }

    enum Comparison {
        missing, extra, no_rem
    }

    static Pattern WHITESPACE = PatternCache.get("[\\s]+");
    static Pattern BADSECTION = PatternCache.get("^\\s*(\\d+\\s*)?Section\\s*\\d+\\s*[-:]\\s*");

    static final Set<String> FORCEBREAK = new HashSet<String>(Arrays.asList(
        "table", "div", "blockquote",
        "p", "br", "td", "th", "h1", "h2", "h3", "h4", "h5", "li"));

//    enum ContentsElements {h1, h2, h3, h4, h5, caption}

    static final Set<String> DO_CONTENTS = new HashSet<String>(Arrays.asList(
        "h1", "h2", "h3", "h4", "h5", "caption"));

    static class Levels implements Comparable<Levels> {
        final int[] levels = new int[10];
        final int h2_start;

        public Levels(int h2_start) {
            levels[0] = h2_start; // special adjustment of starting header level
            this.h2_start = h2_start;
        }

        public Levels() {
            this(0);
        }

        /**
         * h2 = level 0, h3 is level 1, etc.
         * @param level
         * @return
         */
        Levels next(int level, Output<Boolean> missingLevel) {
            level -= 2; // h2 = level 0
            missingLevel.value = false;
            if (levels[0] < h2_start) {
                missingLevel.value = true;
            }
            for (int i = 1; i < level; ++i) {
                if (levels[i] == 0) {
                    missingLevel.value = true;
                }
            }
            levels[level]++;
            for (int i = level + 1; i < levels.length; ++i) {
                levels[i] = 0;
            }
            return this;
        }

        public int getDepth() {
            for (int i = 0;; ++i) {
                int level = levels[i];
                if (level == 0) {
                    return i - 1;
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (int i = 0;; ++i) {
                int level = levels[i];
                if (level == 0) {
                    return b.toString();
                }
                if (b.length() != 0) {
                    b.append('.');
                }
                b.append(level);
            }
        }

        public static Levels parse(String group) {
            Levels result = new Levels();
            int currentLevel = 0;
            for (int i = 0; i < group.length(); ++i) {
                char ch = group.charAt(i);
                if (ch == '.') {
                    currentLevel++;
                } else {
                    ch -= '0';
                    if (ch > '9') {
                        break;
                    }
                    result.levels[currentLevel] = result.levels[currentLevel] * 10 + ch;
                }
            }
            return result;
        }

        @Override
        public int compareTo(Levels other) {
            for (int i = 0; i < levels.length; ++i) {
                if (levels[i] != other.levels[i]) {
                    return levels[i] < other.levels[i] ? -1 : 1;
                }
            }
            return 0;
        }

        public void set(Levels other) {
            for (int i = 0; i < levels.length; ++i) {
                levels[i] = other.levels[i];
            }
        }
    }

    static class HeadingInfo {
        private Levels levels = new Levels();
        private String text = "";
        private Set<String> ids = new LinkedHashSet<String>();
        private boolean suppressSection;
        private boolean isHeader;

        // temporary
        private int level;

        public void setLevel(String headingLabel, HeadingInfo lastHeading) {
            isHeader = !headingLabel.equals("caption");
            level = isHeader ? headingLabel.charAt(1) - '0' : lastHeading.level;
        }

        @Override
        public String toString() {
            //   <h3><a name="Identity_Elements" href="#Identity_Elements">5.3 Identity Elements</a></h3>
            String id = ids.isEmpty() ? "NOID" : ids.iterator().next();
            String result = "<" + getLabel()
                + "<a name=\"" + id + "\" href=\"#" + id + "\">"
                + (!isHeader ? "" : suppressSection ? "" : levels + " ")
                + TransliteratorUtilities.toHTML.transform(text)
                + "</a>";
            if (ids.size() > 1) {
                boolean first = true;
                for (String id2 : ids) {
                    if (first) {
                        first = false;
                    } else {
                        result += "<a name=\"" + id2 + "\"></a>";
                    }
                }
            }
            return result + "</" + getLabel();
        }

        public String getLabel() {
            return isHeader ? "h" + level + ">" : "caption>";
        }

        public String toHeader() {
            String id = ids.iterator().next();
            return ("<li>"
                + (!isHeader ? (text.contains("Table") || text.contains("Figure") ? "" : "Table: ") : suppressSection ? "" : levels + " ")
                + "<a href=\"#" + id + "\">"
                + TransliteratorUtilities.toHTML.transform(text)
                + "</a>");
        }

        public void addText(String toAppend) {
            String temp = TransliteratorUtilities.fromHTML.transform(toAppend);
            if (text.isEmpty()) {
                if (temp.startsWith(" ")) {
                    text = temp.substring(1);
                } else {
                    text = temp;
                }
            } else {
                text += temp;
            }
            text = SPACES.matcher(text).replaceAll(" "); // clean up all spaces; make more efficient later
            // used to trim, but we need to retain space between elements. So only trim the start, and later, the end
        }

        public boolean isContents() {
            return text.toString().startsWith("Contents");
        }

        void addId(String id) {
            this.ids.add(id);
        }

        public void setLevels(int line, Levels levels, Set<String> errors) {
            this.levels.set(levels);
            String error = "";
            if (badSectionMatcher.reset(text).find()) {
                text = text.substring(badSectionMatcher.end());
                error += "Extra 'Section...' at start; ";
            }
            if (isHeader) {
                if (!headerMatcher.reset(text).matches()) {
                    if (!SUPPRESS_SECTION_NUMBER.matcher(text).matches()) {
                        error += "Missing section numbers; ";
                    }
                } else {
                    text = text.substring(headerMatcher.end(1));
                    if (text.startsWith(".")) {
                        text = text.substring(1).trim();
                        error += "Extra . at start; ";
                    }
                    Levels parsedLevels = Levels.parse(headerMatcher.group(1));
                    if (levels.compareTo(parsedLevels) != 0) {
                        error += "Section numbers mismatch, was " + parsedLevels + "; ";
                    }
                }
            }
            if (ids.isEmpty()) {
                addId(text.toString().trim().replaceAll("[^A-Za-z0-9]+", "_"));
                error += "Missing double link";
            }
            if (!error.isEmpty()) {
                errors.add(this + "\t<!-- " + line + ": " + error + " -->");
            }
            suppressSection = SUPPRESS_SECTION_NUMBER.matcher(text).matches();
        }

        public void addIds(Counter<String> idCounter) {
            for (String id : ids) {
                idCounter.add(id, 1);
            }
        }

        public HeadingInfo fixText() {
            if (text.endsWith(" ")) {
                text = text.substring(0, text.length() - 1);
            }
            return this;
        }
    }

    static Matcher headerMatcher = WELLFORMED_HEADER.matcher("");
    static Matcher badSectionMatcher = BADSECTION.matcher("");

    static class HeadingInfoList {
        private static final long serialVersionUID = -6722150173224993960L;
        Levels lastBuildLevel;
        private Set<String> errors = new LinkedHashSet<String>();
        Output<Boolean> missingLevel = new Output<Boolean>(false);
        private String fileName;
        ArrayList<HeadingInfo> list = new ArrayList<>();

        public HeadingInfoList(String fileName, int h2_START) {
            this.fileName = fileName;
            lastBuildLevel = new Levels(h2_START);
        }

        public boolean add(int line, HeadingInfo h) {
            h.fixText();
            if (SUPPRESS_REVISION.matcher(h.text).matches()) {
                return false;
            }
            if (h.isHeader) {
                h.setLevels(line, lastBuildLevel.next(h.level, missingLevel), errors);
            } else {
                h.setLevels(line, lastBuildLevel, errors);
            }
            if (missingLevel.value) {
                errors.add("FATAL: Missing Level in: " + h);
            }
            return list.add(h);
        }

        static final String PAD = "\t";

        public void listContents() {

            System.out.print("\n\t\t<!-- START Generated TOC: CheckHtmlFiles -->");
            Counter<String> idCounter = new Counter<String>();

            int lastLevel = new Levels().getDepth();
            String pad = PAD;
            int ulCount = 0;
            int liCount = 0;
            for (HeadingInfo h : list) {
                h.addIds(idCounter);
                final int depth = h.levels.getDepth() + (h.isHeader ? 0 : 1);
                int levelDiff = depth - lastLevel;
                lastLevel = depth;
                if (levelDiff > 0) {
                    System.out.println();
                    for (int i = 0; i < levelDiff; ++i) {
                        pad += PAD;
                        System.out.println(pad + "<ul class=\"toc\">");
                        ++ulCount;
                    }
                    pad += PAD;
                } else if (levelDiff < 0) {
                    System.out.println("</li>");
                    --liCount;
                    for (int i = 0; i > levelDiff; --i) {
                        pad = pad.substring(PAD.length());
                        System.out.println(pad + "</ul>");
                        --ulCount;
                        pad = pad.substring(PAD.length());
                        System.out.println(pad + "</li>");
                        --liCount;
                    }
                } else {
                    System.out.println("</li>");
                    --liCount;
                }

                System.out.print(pad + h.toHeader());
                ++liCount;

                //              <li>1.1 <a href="#Conformance">Conformance</a></li>

                //                <ul class="toc">
                //                <li>1 <a href="#Introduction">Introduction</a>
                //                  <ul class="toc">
                //                    <li>1.1 <a href="#Conformance">Conformance</a>
                //                    </li>
                //                    ...
                //                  </ul>
                //                </li>
            }

            // finish up and make sure we are balances

            int levelDiff = -lastLevel;
            System.out.println("</li>");
            --liCount;
            for (int i = 0; i > levelDiff; --i) {
                pad = pad.substring(PAD.length());
                System.out.println(pad + "</ul>");
                --ulCount;
                pad = pad.substring(PAD.length());
                System.out.println(pad + "</li>");
                --liCount;
            }
            pad = pad.substring(PAD.length());
            System.out.println(pad + "</ul>");
            System.out.println(pad + "<!-- END Generated TOC: CheckHtmlFiles -->");
            --ulCount;
            if (liCount != 0 || ulCount != 0) {
                throw new IllegalArgumentException("Mismatched counts in generated contents, li:" + liCount + ", ul:" + ulCount);
            }
            for (String id : idCounter) {
                long count = idCounter.get(id);
                if (count != 1) {
                    errors.add("FATAL: Non-Unique ID: " + id);
                }
            }
        }

        /**
         * Prints out errs
         * @return fatal err count
         */
        public int showErrors() {
            int fatalCount = 0;
            if (!errors.isEmpty()) {
                System.out.println("\n*ERRORS*\n");
                for (String error : errors) {
                    if (error.startsWith("FATAL:")) {
                        System.out.println(fileName + "\t" + error);
                        fatalCount++;
                    }
                }
                if (fatalCount == 0) {
                    for (String error : errors) {
                        System.out.println(fileName + "\t" + error);
                    }
                }
            }
            if (this.list.size() == 0) {
                System.out.println("No header items (eg <h2>) captured.");
                fatalCount = 1;
            }
            return fatalCount;
        }

        /**
         * @return total number of errors
         */
        public int totalErrorCount() {
            return errors.size();
        }
    }

    static class ElementLine {
        final String element;
        final int line;

        public ElementLine(String element, int line) {
            super();
            this.element = element;
            this.line = line;
        }

        @Override
        public String toString() {
            return element + '[' + line + ']';
        }
    }

    static class Data implements Iterable<String> {
        private static final Pattern ELEMENT_ATTLIST = Pattern.compile("<!(ELEMENT|ATTLIST)\\s+(\\S+)[^>]*>");
        List<String> sentences = new ArrayList<String>();
        M4<String, String, String, Boolean> dtdItems = ChainedMap.of(
            new LinkedHashMap<String, Object>(),
            new TreeMap<String, Object>(),
            new TreeMap<String, Object>(), Boolean.class);
        Counter<String> hashedSentences = new Counter<String>();
        int count = 0;
        int totalErrorCount = 0;
        int totalFatalCount = 0;

        public Data getSentences(String fileRegex) throws IOException {
            String base;
            String regex;
            try {
                int firstParen = fileRegex.indexOf('(');
                if (firstParen < 0) {
                    firstParen = fileRegex.length();
                }
                int lastSlash = fileRegex.lastIndexOf(File.separatorChar, firstParen);
                base = fileRegex.substring(0, lastSlash);
                regex = fileRegex.substring(lastSlash + 1);
            } catch (Exception e) {
                throw new IllegalArgumentException("Target file must be in special format. " +
                    "Up to the first path part /.../ containing a paragraph is constant, and the rest is a regex.");
            }

            //File sourceFile = new File(fileRegex);
            File sourceDirectory = new File(base);
            if (!sourceDirectory.exists()) {
                throw new IllegalArgumentException("Can't find " + sourceDirectory);
            }
            String canonicalBase = sourceDirectory.getCanonicalPath();
            String FileRegex = canonicalBase + File.separator + regex;
            FileRegex = FileRegex.replace("\\", "\\\\");
            FileRegex = FileRegex.replace("\\\\.", "\\.");
            Matcher m = PatternCache.get(FileRegex).matcher("");
            System.out.println("Matcher: " + m);

            return getSentences(sourceDirectory, m);
        }

        public Data getSentences(File sourceDirectory, Matcher m) throws IOException {
            //System.out.println("Processing:\t" + sourceDirectory);
            for (File file : sourceDirectory.listFiles()) {
                if (file.isDirectory()) {
                    getSentences(file, m);
                    continue;
                }
                String fileString = file.getCanonicalFile().toString();
                File fileCanonical = new File(fileString);
                if (!m.reset(fileString).matches()) {
                    if (verbose == Verbosity.all) {
                        System.out.println("Skipping: " + RegexUtilities.showMismatch(m, fileString)
                            + "\t" + sourceDirectory);
                    }
                    continue;
                }

                System.out.println("\nProcessing:\t" + sourceDirectory + File.separator + fileString);

                int H2_START = fileString.contains("tr18") ? -1 : 0;
                try (Reader in = new FileReader(fileCanonical)) {
                    parseFile(fileCanonical, H2_START, in);
                }
            }
            return this;
        }

        SimpleHtmlParser parser = new SimpleHtmlParser();

        public void parseFile(File fileCanonical, int H2_START, Reader in) throws IOException {
            Matcher wsMatcher = WHITESPACE.matcher("");
            ++count;
            // SimpleHtmlParser parser = new SimpleHtmlParser().setReader(in);
            parser.setReader(in);
            StringBuilder buffer = new StringBuilder();
            StringBuilder content = new StringBuilder();
            HeadingInfo heading = new HeadingInfo();
            final String fileName = fileCanonical.getName();
            HeadingInfoList headingInfoList = new HeadingInfoList(fileName, H2_START);
            Stack<ElementLine> elementStack = new Stack<>();
            Stack<Pair<String, String>> attributeStack = new Stack<>();
            String contentString;
            boolean inHeading = false;
            boolean inPop = false;
            boolean inAnchor = false;
            boolean haveContents = false;
            HeadingInfo lastHeading = null;
            // for detecting missing captions
            boolean pushedTable = false;
            boolean checkCaption = false;
            List<Integer> captionWarnings = new ArrayList<Integer>();

            main: while (true) {
                int lineCount = parser.getLineCount();
                Type x = parser.next(content);
                if (verbose == Verbosity.all && !SUPPRESS.contains(x)) {
                    LOG.write(parser.getLineCount() + "\t" + x + ":\t«" + content + "»");
                    //SimpleHtmlParser.writeResult(x, content, LOG);
                    LOG.write("\n");
                    LOG.flush();
                }
                switch (x) {
                case QUOTE:
                    contentString = content.toString().toLowerCase(Locale.ENGLISH).trim();
                    if (contentString.equalsIgnoreCase("nocaption")) {
                        pushedTable = false;
                    }
                    break;
                case ATTRIBUTE:
                    contentString = content.toString().toLowerCase(Locale.ENGLISH);
                    if (inHeading && (contentString.equals("name") || contentString.equals("id"))) {
                        inAnchor = true;
                    } else {
                        inAnchor = false;
                    }
                    attributeStack.add(new Pair<String, String>(contentString, null));
                    break;
                case ATTRIBUTE_CONTENT:
                    contentString = content.toString().toLowerCase(Locale.ENGLISH);
                    if (inAnchor) {
                        heading.addId(content.toString());
                    }
                    Pair<String, String> lastAttribute = attributeStack.peek();
                    if (lastAttribute.getSecond() != null) {
                        System.out.println(lineCount + "\tDouble Attribute: " + contentString + ", peek=" + lastAttribute);
                    } else {
                        lastAttribute.setSecond(contentString);
                    }
                    break;
                case ELEMENT:
                    contentString = content.toString().toLowerCase(Locale.ENGLISH);
                    if (inPop) {
                        ElementLine peek;
                        while (true) {
                            peek = elementStack.peek();
                            if (!NOPOP.contains(peek.element)) {
                                break;
                            }
                            elementStack.pop();
                        }
                        if (!peek.element.equals(contentString)) {
                            System.out.println(lineCount
                                + "\tCouldn't pop: " + contentString
                                + ", " + showElementStack(elementStack));
                        } else {
                            elementStack.pop();
                        }
                    } else {
                        // check that the first element following a table is a caption
                        if (pushedTable && !"caption".equals(contentString)) {
                            captionWarnings.add(lineCount);
                        }
                        elementStack.push(new ElementLine(contentString, lineCount));
                        pushedTable = checkCaption && "table".equals(contentString);
                        if (!checkCaption && "h3".equals(contentString)) { // h3 around Summary in standard format
                            checkCaption = true;
                        }
                    }
                    if (verbose != Verbosity.none) {
                        LOG.write(parser.getLineCount() + "\telem:\t" + showElementStack(elementStack) + "\n");
                        LOG.flush();
                    }
                    if (FORCEBREAK.contains(contentString)) {
                        buffer.append("\n");
                    }
                    if (DO_CONTENTS.contains(contentString)) {
                        if (inPop) {
                            if (inHeading) {
                                inHeading = false;
                                if (heading.isContents()) {
                                    haveContents = true;
                                } else if (haveContents) {
                                    headingInfoList.add(parser.getLineCount(), heading);
                                    lastHeading = heading;
                                }
                                heading = new HeadingInfo();
                            }
                        } else {
                            heading.setLevel(contentString, lastHeading);
                            inHeading = true;
                        }
                    }
                    break;
                case ELEMENT_START:
                    inPop = false;
                    break;
                case ELEMENT_END:
                    if (verbose == Verbosity.all && !attributeStack.isEmpty()) {
                        LOG.write(parser.getLineCount() + "\tattr:\t" + showAttributeStack(attributeStack) + System.lineSeparator());
                        LOG.flush();
                    }
                    attributeStack.clear();
                    inPop = false;
                    break;
                case ELEMENT_POP:
                    inPop = true;
                    break;
                case ELEMENT_CONTENT:
                    contentString = wsMatcher.reset(content).replaceAll(" ").replace("&nbsp;", " ");
                    buffer.append(contentString.indexOf('&') >= 0
                        ? TransliteratorUtilities.fromHTML.transform(contentString)
                        : contentString);
                    if (inHeading) {
                        heading.addText(contentString);
                    }
                    break;
                case DONE:
                    break main;
                default:
                    break; // skip everything else.
                }
            }

            // get DTD elements
            Matcher m = ELEMENT_ATTLIST.matcher(buffer);
            while (m.find()) {
                dtdItems.put(fileName, m.group(2), m.group(), true);
                //System.out.println(fileName + "\t" + m.group());
            }
            BreakIterator sentenceBreak = BreakIterator.getSentenceInstance(ULocale.ENGLISH);
            String bufferString = normalizeWhitespace(buffer);
            sentenceBreak.setText(bufferString);
            int last = 0;
            while (true) {
                int pos = sentenceBreak.next();
                if (pos == BreakIterator.DONE) {
                    break;
                }
                String sentence = bufferString.substring(last, pos).trim();
                last = pos;
                if (sentence.isEmpty()) {
                    continue;
                }
                hashedSentences.add(sentence, 1);
                sentences.add(sentence);
            }
            if (!captionWarnings.isEmpty()) {
                System.out.println("WARNING: Missing <caption> on the following lines: "
                    + "\n    " + CollectionUtilities.join(captionWarnings, ", ")
                    + "\n\tTo fix, add <caption> after the <table>, such as:"
                    + "\n\t\t<table>"
                    + "\n\t\t\t<caption>Private Use Codes in CLDR</a></caption>"
                    + "\n\tOften the sentence just before the <table> can be made into the caption."
                    + "\n\tThe next time you run this program, you’ll be prompted with double-links."
                    + "\n\tIf it really shouldn't have a caption, add <!-- nocaption --> after the <table> instead.");
            }
            int fatalCount = headingInfoList.showErrors();
            totalFatalCount += fatalCount;
            totalErrorCount += headingInfoList.totalErrorCount();
            if (fatalCount == 0) {
                headingInfoList.listContents();
            } else {
                System.out.println("\nFix fatal errors in " + fileCanonical + " before contents can be generated");
            }
        }

        private String showAttributeStack(Stack<Pair<String, String>> attributeStack) {
            StringBuilder result = new StringBuilder();
            for (Pair<String, String> s : attributeStack) {
                result.append("[@");
                result.append(s.getFirst());
                final String second = s.getSecond();
                if (second != null) {
                    result.append("='");
                    result.append(second);
                    result.append("'");
                }
                result.append("]");
            }
            return result.toString();
        }

        private String showElementStack(Stack<ElementLine> elementStack) {
            StringBuilder result = new StringBuilder();
            for (ElementLine s : elementStack) {
                result.append('/').append(s);
            }
            return result.toString();
        }

        /**
         * Return string after collapsing multiple whitespace containing '\\n' to '\\n',
         * and otherwise 'space'.
         * @param input
         * @return
         */
        private String normalizeWhitespace(CharSequence input) {
            Matcher m = WHITESPACE.matcher(input);
            StringBuilder buffer = new StringBuilder();
            int last = 0;
            while (m.find()) {
                int start = m.start();
                buffer.append(input.subSequence(last, start));
                last = m.end();
                String whiteString = m.group();
                if (whiteString.indexOf('\n') >= 0) {
                    buffer.append('\n');
                } else {
                    buffer.append(' ');
                }
            }
            buffer.append(input.subSequence(last, input.length()));
            return buffer.toString().trim();
        }

        public long getCount(String sentence) {
            return hashedSentences.getCount(sentence);
        }

        @Override
        public Iterator<String> iterator() {
            return sentences.iterator();
        }
    }
}
