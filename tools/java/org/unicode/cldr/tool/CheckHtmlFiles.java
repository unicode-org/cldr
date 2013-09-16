package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;

import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class CheckHtmlFiles {

    final static Options myOptions = new Options();
    final static Writer LOG = new OutputStreamWriter(System.out);
    static Pattern WELLFORMED_HEADER = Pattern.compile("\\s*(\\d+(\\.\\d+)*\\s*).*");
    static Pattern SUPPRESS_SECTION_NUMBER = Pattern.compile("Migration|References|Acknowledgments|Modifications|Revision \\d+");
    static Pattern SUPPRESS_REVISION = Pattern.compile("Revision \\d+");
    static Pattern SPACES = Pattern.compile("\\s+");

    enum MyOptions {
        old(".*", "/Users/markdavis/Google Drive/Backup-2012-10-09/Documents/indigo/cldr-archive/cldr-22.1/specs/ldml/tr35\\.html", "source data (regex)"),
        target(".*", CldrUtility.BASE_DIRECTORY + "specs/ldml/tr35(-.*)?\\.html", "target data (regex)"),
        verbose(null, null, "verbose debugging messages"),
        contents(".*", CldrUtility.BASE_DIRECTORY + "specs/ldml/tr35(-.*)?\\.html", "generate contents"),
        // /cldr-archive
        ;

        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static boolean verbose;
    static boolean doContents;

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.old, args, true);
        verbose = MyOptions.verbose.option.doesOccur();

        if (!MyOptions.target.option.doesOccur()) { // contents
            Data target = new Data().getSentences(MyOptions.contents.option.getValue());
            return;
        }
        Data source = new Data().getSentences(MyOptions.old.option.getValue());
        Data target = new Data().getSentences(MyOptions.target.option.getValue());

        int missingCount = 0, extraCount = 0;
        int line = 0;
        for (String sentence : source) {
            ++line;
            long sourceCount = source.getCount(sentence);
            long targetCount = target.getCount(sentence);
            if (targetCount == 0) {
                System.out.println(line + "\tMISSING:\t" + sourceCount + "≠" + targetCount + "\t" + sentence);
                ++missingCount;
            }
        }
        line = 0;
        for (String sentence : target) {
            ++line;
            long sourceCount = source.getCount(sentence);
            long targetCount = target.getCount(sentence);
            if (sourceCount == 0) {
                System.out.println(line + "\tEXTRA:\t" + targetCount + "≠" + sourceCount + "\t" + sentence);
                ++extraCount;
            }
        }
        System.out.println("Missing:\t" + missingCount);
        System.out.println("Extra:\t" + extraCount);
    }

    static Pattern WHITESPACE = Pattern.compile("[\\s]+");
    static Pattern BADSECTION = Pattern.compile("^\\s*(\\d+\\s*)?Section\\s*\\d+\\s*[-:]\\s*");
    
    static final Set<String> FORCEBREAK = new HashSet<String>();
    static {
        FORCEBREAK.addAll(Arrays.asList("table", "div", "blockquote",
            "p", "br", "td", "th", "h1", "h2", "h3", "h4", "h5", "li"));
    }
    static final Set<String> DO_CONTENTS = new HashSet<String>();
    static {
        DO_CONTENTS.addAll(Arrays.asList("h1", "h2", "h3", "h4", "h5"));
    }

    static class Levels implements Comparable<Levels> {
        int[] levels = new int[10];

        /**
         * h2 = level 0, h3 is level 1, etc.
         * @param level
         * @return
         */
        Levels next(int level, Output<Boolean> missingLevel) {
            level -= 2; // h2 = level 0
            missingLevel.value = false;
            for (int i = 0; i < level; ++i) {
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

        // temporary
        private int level;

        public void setLevel(String headingLabel) {
            level = headingLabel.charAt(1) - '0';
        }

        @Override
        public String toString() {
            //   <h3><a name="Identity_Elements" href="#Identity_Elements">5.3 Identity Elements</a></h3>
            String id = ids.isEmpty() ? "NOID" : ids.iterator().next();
            String result = "<h" + level + ">"
                + "<a name=\"" + id + "\" href=\"#" + id + "\">"
                + (suppressSection ? "" : levels + " ")
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
            return result + "</h" + level + ">";
        }

        public String toHeader() {
            String id = ids.iterator().next();
            return ("<li>"
                + (suppressSection ? "" : levels + " ")
                + "<a href=\"#" + id + "\">"
                + TransliteratorUtilities.toHTML.transform(text)
                + "</a>");
        }

        public void addText(String toAppend) {
            text += toAppend;
            text = SPACES.matcher(text).replaceAll(" ").trim(); // clean up all spaces; make more efficient later
        }
        
        public boolean isContents() {
            return text.toString().startsWith("Contents");
        }

        void addId(String id) {
            this.ids.add(id);
        }

        public void setLevels(Levels levels, Set<String> errors) {
            this.levels.set(levels);
            String error = "";
            if (badSectionMatcher.reset(text).find()) {
                text = text.substring(badSectionMatcher.end());
                error += "Extra 'Section...' at start; ";
            }
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
            if (ids.isEmpty()) {
                addId(text.toString().trim().replaceAll("[^A-Za-z0-9]+", "_"));
                error += "Missing double link";
            }
            if (!error.isEmpty()) {
                errors.add(this + "\t<!-- " + error + "-->");
            }
            suppressSection = SUPPRESS_SECTION_NUMBER.matcher(text).matches();
        }

        public void addIds(Counter<String> idCounter) {
            for (String id : ids) {
                idCounter.add(id, 1);
            }
        }
    }

    static Matcher headerMatcher = WELLFORMED_HEADER.matcher("");
    static Matcher badSectionMatcher = BADSECTION.matcher("");

    static class HeadingInfoList extends ArrayList<HeadingInfo> {
        private static final long serialVersionUID = -6722150173224993960L;
        Levels lastBuildLevel = new Levels();
        private Set<String> errors = new LinkedHashSet<String>();
        Output<Boolean> missingLevel = new Output<Boolean>(false);

        public boolean add(HeadingInfo h) {
            if (SUPPRESS_REVISION.matcher(h.text).matches()) {
                return false;
            }
            h.setLevels(lastBuildLevel.next(h.level, missingLevel), errors);
            if (missingLevel.value) {
                errors.add("FATAL: Missing Level in: " + h);
            }
            return super.add(h);
        }

        static final String PAD = "\t";

        public void listContents() {

            System.out.println("\n*REVISED TOC*");
            Counter<String> idCounter = new Counter<String>();

            Levels lastLevel = new Levels();
            String pad = PAD;
            int ulCount = 0;
            int liCount = 0;
            for (HeadingInfo h : this) {
                h.addIds(idCounter);

                int levelDiff = h.levels.getDepth() - lastLevel.getDepth();
                lastLevel = h.levels;
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

            int levelDiff = -lastLevel.getDepth();
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

        public int showErrors() {
            int fatalCount = 0;
            if (!errors.isEmpty()) {
                System.out.println("\n*ERRORS*\n");
                for (String error : errors) {
                    if (error.startsWith("FATAL:")) {
                        System.out.println(error);
                        fatalCount++;
                    }
                }
                if (fatalCount == 0) {
                    for (String error : errors) {
                        System.out.println(error);
                    }
                }
            }
            if (this.size() == 0) {
                System.out.println("No header items (eg <h2>) captured.");
                fatalCount = 1;
            }
            return fatalCount;
        }
    }

    static class Data implements Iterable<String> {
        List<String> sentences = new ArrayList<String>();
        Counter<String> hashedSentences = new Counter<String>();

        public Data getSentences(String fileRegex) throws IOException {
            File sourceFile = new File(fileRegex);
            File sourceDirectory = sourceFile.getParentFile();
            if (!sourceDirectory.exists()) {
                throw new IllegalArgumentException("Can't find " + sourceDirectory);
            }
            int count = 0;
            Matcher m = Pattern.compile(sourceFile.getName()).matcher("");
            Matcher wsMatcher = WHITESPACE.matcher("");
            for (File file : sourceDirectory.listFiles()) {
                String fileString = file.getName().toString();
                if (!m.reset(fileString).matches()) {
                    if (verbose) {
                        System.out.println("Skipping: " + RegexUtilities.showMismatch(m, fileString)
                            + "\t" + sourceDirectory);
                    }
                    continue;
                }
                ++count;

                System.out.println("\nProcessing:\t" + sourceDirectory + "/" + fileString + "\n");

                Reader in = new FileReader(new File(sourceDirectory, fileString));
                SimpleHtmlParser parser = new SimpleHtmlParser().setReader(in);
                StringBuilder buffer = new StringBuilder();
                StringBuilder content = new StringBuilder();
                HeadingInfo heading = new HeadingInfo();
                HeadingInfoList headingInfoList = new HeadingInfoList();
                String contentString;
                boolean inHeading = false;
                boolean inPop = false;
                boolean inAnchor = false;
                boolean haveContents = false;
                main: while (true) {
                    Type x = parser.next(content);
                    if (verbose) {
                        LOG.write(x + ":\t");
                        parser.writeResult(x, content, LOG);
                        LOG.write("\n");
                        LOG.flush();
                    }
                    switch (x) {
                    case ATTRIBUTE:
                        contentString = content.toString().toLowerCase(Locale.ENGLISH);
                        if (inHeading && (contentString.equals("name") || contentString.equals("id"))) {
                            inAnchor = true;
                        } else {
                            inAnchor = false;
                        }
                        break;
                    case ATTRIBUTE_CONTENT:
                        contentString = content.toString().toLowerCase(Locale.ENGLISH);
                        if (inAnchor) {
                            heading.addId(content.toString());
                        }
                        break;
                    case ELEMENT:
                        contentString = content.toString().toLowerCase(Locale.ENGLISH);
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
                                        headingInfoList.add(heading);
                                    }
                                    heading = new HeadingInfo();
                                }
                            } else {
                                heading.setLevel(contentString);
                                inHeading = true;
                            }
                        }
                        break;
                    case ELEMENT_START:
                    case ELEMENT_END:
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
                int fatalCount = headingInfoList.showErrors();
                if (fatalCount == 0) {
                    headingInfoList.listContents();
                } else {
                    System.out.println("\nFix fatal errors in " + fileString + " before contents can be generated");
                }
            }
            if (count == 0) {
                throw new IllegalArgumentException("No files matched with " + m.pattern() + " in " + sourceDirectory);
            }
            return this;
        }

        /**
         * Return string after collapsing multiple whitespace containing '\\n' to '\\n', 
         * and otherwise 'space'.
         * @param input
         * @return
         */
        private String normalizeWhitespace(CharSequence input) {
            Matcher m = WHITESPACE.matcher(input);
            int pos = input.toString().indexOf('\n');
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
