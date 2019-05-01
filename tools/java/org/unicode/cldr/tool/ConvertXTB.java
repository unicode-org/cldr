package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InputStreamFactory;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A command-line tool for converting XTB files to the CLDR format and checking
 * them against current CLDR data.
 *
 * @author jchye@google.com (Jennifer Chye)
 */
public class ConvertXTB {
    private static final Pattern ID_PATTERN = PatternCache.get(
        "\\[@id=\"(\\d++)\"]");
    private static final Pattern PLURAL_MESSAGE_FORMAT = PatternCache.get(
        "\\{[A-Z_]++,plural, (.*)}");

    private static PatternPlaceholders patternPlaceholders;
    private static Map<String, Map<String, String>> loadedReverseTagMaps;
    private static PathDescription pathDescription;

    private Factory factory;
    private File xtbDir;
    private File inputDir;
    private String outputDir;
    private CheckCLDR checkCldr;
    private CLDRFile englishFile;
    private PrintStream out;

    /***
     * Constructor. The input directory must have the following format:
     *
     * <pre>
     * inputDir/
     *   ar/
     *     ar.wsb
     *   ca/
     *     ca.wsb
     *   ...
     *   xtb/
     *     ar.xtb
     *     ca.xtb
     *     ...
     * </pre>
     *
     * @param inputDir
     *            the directory to read the wsb and xtb files from
     * @param outputDir
     *            the directory to write the generated CLDR files to
     * @param checkFilter
     *            the CheckCLDR regex filter for checking
     */
    private ConvertXTB(String inputDir, String outputDir, String checkFilter) {
        xtbDir = new File(inputDir, "xtb");
        this.inputDir = new File(inputDir);
        this.outputDir = outputDir;
        factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        englishFile = factory.make("en", true);
        this.checkCldr = CheckCLDR.getCheckAll(factory, checkFilter);
        CheckCLDR.setDisplayInformation(englishFile);
        out = System.out;
    }

    /**
     * Sets the PrintStream that any errors will be sent to. System.out is used
     * by default.
     *
     * @param out
     */
    private void setErrorOutput(PrintStream out) {
        this.out = out;
    }

    /**
     * Wrapper class for the contents of an XTB file.
     */
    private class XtbInfo implements Iterable<XtbEntry> {
        public String locale;
        public List<XtbEntry> entries;

        public XtbInfo(String locale) {
            this.locale = locale;
            entries = new ArrayList<XtbEntry>();
        }

        @Override
        public Iterator<XtbEntry> iterator() {
            return entries.iterator();
        }

        public void add(XtbEntry entry) {
            entries.add(entry);
        }
    }

    /**
     * Wrapper class for information related to a &lt;translation&gt; node in
     * an XTB file.
     */
    private class XtbEntry {
        public String messageId;
        public String xpath;
        public String value;

        public XtbEntry(String messageId, String xpath, String value) {
            this.messageId = messageId;
            this.xpath = xpath;
            this.value = value;
        }
    }

    /**
     * An XML handler for XTB files.
     */
    private class XtbHandler implements ContentHandler {
        private DisplayAndInputProcessor daip;
        private StringBuffer currentText;
        private String lastId;
        private String lastXpath;

        private Set<String> orphanedMessages;
        private Set<String> oldMessages;
        private XtbInfo output;

        public XtbHandler(Set<String> oldMessages, XtbInfo output) {
            daip = new DisplayAndInputProcessor(CLDRLocale.getInstance(output.locale));
            currentText = new StringBuffer();
            orphanedMessages = new HashSet<String>();
            this.oldMessages = oldMessages;
            this.output = output;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentText.append(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attr) throws SAXException {
            if (qName.equals("translation")) {
                lastId = attr.getValue("id");
                lastXpath = IdToPath.getPath(lastId);
                currentText.setLength(0);
            } else if (qName.equals("ph")) {
                String name = attr.getValue("name");
                String placeholder = getPlaceholderForName(lastXpath, name);
                currentText.append(placeholder);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            if (qName.equals("translation")) {
                if (lastXpath == null) {
                    orphanedMessages.add(lastId);
                } else if (!oldMessages.contains(lastId)) {
                    // Only add new values to reduce computation time.
                    addValue(lastXpath, currentText.toString());
                }
                currentText.setLength(0);
            }
        }

        /**
         * Add the specified xpath and value to the output.
         *
         * @param xpath
         * @param value
         */
        private void addValue(String xpath, String value) {
            Matcher matcher = PLURAL_MESSAGE_FORMAT.matcher(value);
            if (matcher.matches()) {
                // Parse the plural value. Example plural value:
                // {NUMBER,plural, =0{0 {CURRENCY_NAME}}=1{1 {CURRENCY_NAME}}
                // one{# {CURRENCY_NAME}}other{# {CURRENCY_NAME}}}
                addPluralValue(xpath, matcher.group(1));
            } else {
                addValueToOutput(xpath, value);
            }
        }

        /**
         * Processes a plural value and xpath and adds them to the output.
         *
         * @param xpath
         * @param value
         */
        private void addPluralValue(String xpath, String value) {
            // Example plural value to be parsed:
            // =0{0 {CURRENCY_NAME}}=1{1 {CURRENCY_NAME}}
            // one{# {CURRENCY_NAME}}other{# {CURRENCY_NAME}}
            int numOpen = 0;
            StringBuffer buffer = new StringBuffer();
            String countType = null;
            int nameStart = -1;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                case '{':
                    if (numOpen == 0) {
                        int startIndex = buffer.charAt(0) == '=' ? 1 : 0;
                        countType = buffer.substring(startIndex);
                        buffer.setLength(0);
                    } else {
                        // Start of placeholder.
                        nameStart = i + 1;
                    }
                    numOpen++;
                    break;
                case '}':
                    numOpen--;
                    if (numOpen == 0) {
                        // Special handling for decimal format lengths.
                        if (lastXpath.contains("decimalFormatLength")) {
                            if (countType.length() == 1) {
                                countType = countType.charAt(0) == '1' ? "one" : "zero";
                            } else if (countType.equals("one")) {
                                // skip, contains rubbish
                                buffer.setLength(0);
                                countType = null;
                                break;
                            }
                        }
                        // Add the count attribute back to the xpath.
                        String pluralXPath = xpath + "[@count=\"" + countType + "\"]";
                        // Add any remaining missing placeholders.
                        String pluralValue = buffer.toString();
                        if (pluralValue.contains("{1}") && !pluralValue.contains("{0}")) {
                            // Fix placeholder numbering. Assumes there is only one
                            // placeholder in the pattern.
                            if (countType.matches("[01]")) {
                                pluralValue = pluralValue.replaceAll(countType + "(?!})", "{0}");
                            } else {
                                pluralValue = pluralValue.replace("{1}", "{0}");
                            }
                        }
                        addValueToOutput(pluralXPath, pluralValue);
                        buffer.setLength(0);
                        countType = null;
                    } else {
                        // End of placeholder.
                        String name = value.substring(nameStart, i);
                        buffer.append(getPlaceholderForName(xpath, name));
                    }
                    break;
                case '#':
                    buffer.append(lastXpath.contains("decimalFormatLength") ? '#' : "{0}");
                    break;
                default:
                    // Don't append placeholder names.
                    if (numOpen < 2) {
                        buffer.append(c);
                    }
                }
            }
        }

        private void addValueToOutput(String xpath, String value) {
            value = daip.processInput(xpath, value, null);
            output.add(new XtbEntry(lastId, xpath, value));
        }

        @Override
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
        }

        @Override
        public void processingInstruction(String arg0, String arg1) throws SAXException {
        }

        @Override
        public void skippedEntity(String arg0) throws SAXException {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
            if (orphanedMessages.size() > 0) {
                System.err.println(orphanedMessages.size() +
                    " message IDs with no matching xpaths: ");
                for (String messageID : orphanedMessages) {
                    System.err.println(messageID);
                }
            }
        }

        @Override
        public void setDocumentLocator(Locator arg0) {
        }

        @Override
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {
        }

        @Override
        public void endPrefixMapping(String arg0) throws SAXException {
        }
    }

    /**
     * An XML handler for WSB files.
     */
    private class WsbHandler extends XMLFileReader.SimpleHandler {
        private Set<String> messageIds;

        public WsbHandler() {
            messageIds = new HashSet<String>();
        }

        @Override
        public void handlePathValue(String path, String value) {
            Matcher matcher = ID_PATTERN.matcher(path);
            if (matcher.find()) {
                messageIds.add(matcher.group(1));
            }
        }

        public Set<String> getOldMessages() {
            return messageIds;
        }
    }

    /**
     *
     * @param xpath
     *            the xpath that the placeholder belongs to
     * @param name
     *            the name to get the placeholder for
     * @return the placeholder, e.g. "{0}" or "{1}"
     */
    private String getPlaceholderForName(String xpath, String name) {
        if (loadedReverseTagMaps == null) {
            loadedReverseTagMaps = new HashMap<String, Map<String, String>>();
        }
        Map<String, String> map = loadedReverseTagMaps.get(xpath);
        if (map == null) {
            map = new HashMap<String, String>();
            loadedReverseTagMaps.put(xpath, map);
            Map<String, PlaceholderInfo> tagMap = getTagMap(xpath);
            for (Map.Entry<String, PlaceholderInfo> entry : tagMap.entrySet()) {
                map.put(entry.getValue().name, entry.getKey());
            }
        }
        return map.get(name);
    }

    /**
     * @param xpath
     *            the xpath to get placeholder information for
     * @return a mapping of placeholders to placeholder information for the
     *         specified xpath
     */
    private Map<String, PlaceholderInfo> getTagMap(String xpath) {
        if (patternPlaceholders == null) {
            patternPlaceholders = PatternPlaceholders.getInstance();
        }
        return patternPlaceholders.get(xpath);
    }

    /**
     * Loads the contents of an XTB file into memory.
     *
     * @param locale
     *            the locale of the XTB file to be loaded
     * @return
     */
    private XtbInfo load(String locale) {
        // HACKETY HACK: The wsb files use old langauge codes with hyphens
        // instead of CLDR's underscores.
        // The xtb files use hyphens but differ yet again from the CLDR and xtb
        // language codes, e.g. xtb and wsb use "no" but CLDR uses "nb", and
        // wsb uses "iw" but xtb and CLDR use "he". This means that we can't
        // convert the locale to the CLDR standard until after reading in the
        // xtb/wsb files. Sigh.
        // Get the set of previously translated messages from the WSB file.
        Set<String> oldMessages = null;
        try {
            oldMessages = loadOldMessages(locale);
        } catch (IllegalArgumentException e) {
            System.err.println("No wsb found for " + locale + ", skipping");
            return null;
        }

        // Parse the XTB file.
        XtbInfo info = new XtbInfo(LanguageCodeConverter.fromGoogleLocaleId(locale));
        XtbHandler handler = new XtbHandler(oldMessages, info);
        XMLReader xmlReader = XMLFileReader.createXMLReader(false);
        xmlReader.setContentHandler(handler);
        File inputFile = new File(xtbDir, locale + ".xtb");
        try (InputStream fis = InputStreamFactory.createInputStream(inputFile)) {
            //  FileInputStream fis = new FileInputStream(inputFile);
            InputSource is = new InputSource(fis);
            xmlReader.parse(is);
            // fis.close();
        } catch (SAXException | IOException e) {
            System.err.println("Error loading " + inputFile.getAbsolutePath());
            e.printStackTrace();
        }
//            catch (SAXException e) {
//            System.err.println("Error loading " + inputFile.getAbsolutePath());
//            e.printStackTrace();
//        }
        return info;
    }

    /**
     * Loads the set of messages that were previously translated.
     *
     * @param locale
     *            the locale of the messages to be retrieved
     * @return
     * @throws IllegalArgumentException
     *             if there was an error parsing the wsb
     */
    private Set<String> loadOldMessages(String locale) throws IllegalArgumentException {
        locale = LanguageCodeConverter.toGoogleLocaleId(locale);
        WsbHandler handler = new WsbHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(handler);
        File wsbFile = new File(inputDir, locale + '/' + locale + ".wsb");
        xfr.read(wsbFile.getAbsolutePath(), -1, true);
        return handler.getOldMessages();
    }

    /**
     * Processes all XTB files in the input directory that match the specified
     * regex.
     *
     * @param regexFilter
     */
    private void processAll(String regexFilter) {
        out.println("Locale\tMessage ID\tDescription\tEnglish Value\t" +
            "Translated Value\tType\tError Message");
        for (String filename : xtbDir.list()) {
            if (filename.matches(regexFilter + "\\.xtb")) {
                String locale = filename.substring(0, filename.length() - 4);
                XtbInfo xtbInfo = load(locale);
                if (xtbInfo == null) continue;
                check(xtbInfo);
                writeXml(xtbInfo);
            }
        }
    }

    /**
     * Checks the contents of the XTB file against the existing CLDR data.
     *
     * @param xtbInfo
     *            the contents of the XTB to be checked
     */
    private void check(XtbInfo xtbInfo) {
        String locale = xtbInfo.locale;
        CLDRFile cldrFile = factory.make(locale, false).cloneAsThawed();
        for (XtbEntry info : xtbInfo) {
            cldrFile.add(info.xpath, info.value);
        }
        Map<String, String> options = new HashMap<String, String>();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        checkCldr.setCldrFileToCheck(cldrFile, options, possibleErrors);
        for (CheckStatus status : possibleErrors) {
            System.out.println(locale + "\tLOCALE ERROR\t" + status.getMessage());
        }
        int numErrors = 0;
        for (XtbEntry info : xtbInfo) {
            String xpath = CLDRFile.getDistinguishingXPath(info.xpath, null);
            String fullPath = cldrFile.getFullXPath(xpath);
            String value = info.value;
            checkCldr.check(xpath, fullPath, value, options, possibleErrors);
            numErrors += displayErrors(locale, info.messageId, xpath, value, possibleErrors);
        }
        if (numErrors == 0) System.out.println("No errors found for " + locale);
        out.flush();
    }

    /**
     * Displays any errors that occurred when checking the specified xpath
     *
     * @param locale
     *            the locale of the xpath being checked
     * @param id
     *            the message ID corresponding to the xpath
     * @param xpath
     *            the xpath that was checked
     * @param value
     *            the value of the xpath
     * @param possibleErrors
     *            a list of errors generated by checking the xpath
     */
    private int displayErrors(String locale, String id, String xpath,
        String value, List<CheckStatus> possibleErrors) {
        String description = getDescription(xpath, value);
        // Ignore these interval formats since they'll be removed.
        if (id.equals("8190100716823312848") || id.equals("8190100716823312848")) return 0;
        int numErrors = 0;
        for (CheckStatus status : possibleErrors) {
            out.println(locale + "\t" + id +
                "\t" + description +
                "\t" + englishFile.getStringValue(xpath) +
                "\t" + value + "\t" + status.getType() + "\t" + status.getMessage().replace('\t', ' '));
            if (status.getType().equals("Error")) {
                numErrors++;
            }
        }
        return numErrors;
    }

    /**
     * Writes the contents of the XTB file to an XML file in CLDR format.
     *
     * @param xtbInfo
     */
    private void writeXml(XtbInfo xtbInfo) {
        String locale = xtbInfo.locale;
        File xmlFile = new File(outputDir, locale + ".xml");
        // Add proposed alt tags to all xpaths.
        XMLSource altSource = new SimpleXMLSource(locale);
        for (XtbEntry info : xtbInfo) {
            altSource.putValueAtPath(info.xpath, info.value);
        }
        PrintWriter out;
        try {
            out = new PrintWriter(xmlFile);
            new CLDRFile(altSource).write(out);
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't write " + xmlFile.getAbsolutePath() + " to disk.");
        }
    }

    private String getDescription(String path, String value) {
        if (pathDescription == null) {
            SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
            pathDescription = new PathDescription(supplementalDataInfo, englishFile, null, null,
                PathDescription.ErrorHandling.CONTINUE);
        }
        final String description = pathDescription.getDescription(path, value, null, null);
        return description;
    }

    private static final Options options = new Options()
        .add("source_dir", ".*", "The source directory containing the xtb and wsb files to be read")
        .add("destination_dir", ".*", "The destination directory to write the XML files to")
        .add("locale_filter", ".*", ".*", "A regex filter for (Google) locales to be processed")
        .add("test_filter", ".*", ".*", "A regex filter for CheckCLDR tests")
        .add("error_file", ".*", "./errors.tsv", "The file that checking results should be written to");

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        options.parse(args, true);
        String inputDir = null;
        Option option = options.get("source_dir");
        if (option.doesOccur()) {
            inputDir = option.getValue();
        } else {
            throw new RuntimeException("Input dir must be specified");
        }
        String outputDir = null;
        option = options.get("destination_dir");
        if (option.doesOccur()) {
            outputDir = option.getValue();
        } else {
            throw new RuntimeException("Output dir must be specified");
        }
        String localeFilter = options.get("locale_filter").getValue();
        String testFilter = options.get("test_filter").getValue();
        String errorFile = options.get("error_file").getValue();

        // input, output
        ConvertXTB converter = new ConvertXTB(inputDir, outputDir, testFilter);
        PrintStream out = new PrintStream(new File(errorFile));
        converter.setErrorOutput(out);
        converter.processAll(localeFilter);
        out.close();
    }
}
