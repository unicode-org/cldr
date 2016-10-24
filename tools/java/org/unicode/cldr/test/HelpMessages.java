package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;
import org.unicode.cldr.util.TransliteratorUtilities;

import com.ibm.icu.text.MessageFormat;

/**
 * Private class to get the messages from a help file.
 */
public class HelpMessages {
    private static final Matcher CLEANUP_BOOKMARK = PatternCache.get("[^a-zA-Z0-9]").matcher("");

    private static final MessageFormat DEFAULT_HEADER_PATTERN = new MessageFormat("<p>{0}</p>"
        + CldrUtility.LINE_SEPARATOR);

    private static final Matcher HEADER_HTML = PatternCache.get("<h[0-9]>(.*)</h[0-9]>").matcher("");

    List<Matcher> keys = new ArrayList<Matcher>();

    List<String> values = new ArrayList<String>();

    enum Status {
        BASE, BEFORE_CELL, IN_CELL, IN_INSIDE_TABLE
    };

    StringBuilder[] currentColumn = new StringBuilder[2];

    int column = 0;

    private static HelpMessages helpMessages;

    /**
     * Create a HelpMessages object from a filename.
     * The file has to be in the format of a table of <keyRegex,htmlText> pairs,
     * where the key is a keyRegex expression and htmlText is arbitrary HTML text. For example:
     * <p>
     * {@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/chart_messages.html} is used for
     * chart messages, where the key is the name of the chart.
     * <p>
     * {@link http://unicode.org/cldr/data/tools/java/org/unicode/cldr/util/data/test_help_messages.html} is used
     * for help messages in the survey tool, where the key is an xpath.
     *
     * @param filename
     */
    public HelpMessages(String filename) {
        currentColumn[0] = new StringBuilder();
        currentColumn[1] = new StringBuilder();
        BufferedReader in;
        try {
            in = CldrUtility.getUTF8Data(filename);
            int tableCount = 0;

            boolean inContent = false;
            // if the table level is 1 (we are in the main table), then we look for <td>...</td><td>...</td>. That
            // means that we have column 1 and column 2.

            SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
            StringBuilder result = new StringBuilder();
            boolean hadPop = false;
            main: while (true) {
                Type x = simple.next(result);
                switch (x) {
                case ELEMENT: // with /table we pop the count
                    if (SimpleHtmlParser.equals("table", result)) {
                        if (hadPop) {
                            --tableCount;
                        } else {
                            ++tableCount;
                        }
                    } else if (tableCount == 1) {
                        if (SimpleHtmlParser.equals("tr", result)) {
                            if (hadPop) {
                                addHelpMessages();
                            }
                            column = 0;
                        } else if (SimpleHtmlParser.equals("td", result)) {
                            if (hadPop) {
                                inContent = false;
                                ++column;
                            } else {
                                inContent = true;
                                continue main; // skip adding
                            }
                        }
                    }
                    break;
                case ELEMENT_POP:
                    hadPop = true;
                    break;
                case ELEMENT_END:
                    hadPop = false;
                    break;
                case DONE:
                    break main;
                }
                if (inContent) {
                    SimpleHtmlParser.writeResult(x, result, currentColumn[column]);
                }
            }

            in.close();
        } catch (IOException e) {
            System.err.println("Can't initialize help text");
        }
    }

    /**
     * Get message corresponding to a key out of the file set on this object.
     * For many files, the key will be an xpath, but it doesn't have to be.
     * Note that <i>all</i> of pairs of <keyRegex,htmlText> where the key matches keyRegex
     * will be concatenated together in order to get the result.
     *
     * @param key
     * @return
     */
    public String find(String key) {
        return find(key, DEFAULT_HEADER_PATTERN);
    }

    /**
     * Get message corresponding to a key out of the file set on this object.
     * For many files, the key will be an xpath, but it doesn't have to be.
     * Note that <i>all</i> of pairs of <keyRegex,htmlText> where the key matches keyRegex
     * will be concatenated together in order to get the result.
     *
     * @param key
     * @param addHeader
     *            true if you want a header formed by looking at all the hN elements.
     * @return
     */
    public String find(String key, MessageFormat headerPattern) {
        StringBuilder header = new StringBuilder();
        StringBuilder result = new StringBuilder();
        int keyCount = 0;
        for (int i = 0; i < keys.size(); ++i) {
            if (keys.get(i).reset(key).matches()) {
                if (result.length() != 0) {
                    result.append(CldrUtility.LINE_SEPARATOR);
                }
                String value = values.get(i);
                if (headerPattern != null) {
                    HEADER_HTML.reset(value);
                    int lastEnd = 0;
                    StringBuilder newValue = new StringBuilder();
                    while (HEADER_HTML.find()) {
                        String contents = HEADER_HTML.group(1);
                        if (contents.contains("<")) {
                            continue; // disallow other formatting
                        }
                        String bookmark = "HM_" + CLEANUP_BOOKMARK.reset(contents).replaceAll("_");
                        keyCount++;
                        if (header.length() > 0) {
                            header.append(" | ");
                        }
                        header.append("<a href='#").append(bookmark).append("'>").append(contents).append("</a>");
                        newValue.append(value.substring(lastEnd, HEADER_HTML.start(1)));
                        newValue.append("<a name='").append(bookmark).append("'>").append(contents).append("</a>");
                        lastEnd = HEADER_HTML.end(1);
                    }
                    newValue.append(value.substring(lastEnd));
                    value = newValue.toString();
                }
                result.append(value);
            }
        }
        if (result.length() != 0) {
            if (keyCount > 1) {
                result.insert(0, headerPattern.format(new Object[] { header.toString() }));
            }
            return result.toString();
        }
        return null;
    }

    private void addHelpMessages() {
        if (column == 2) { // must have two columns
            try {
                // remove the first character and the last two characters, since the are >....</
                String key = currentColumn[0].substring(1, currentColumn[0].length() - 2).trim();
                String value = currentColumn[1].substring(1, currentColumn[1].length() - 2).trim();
                if (ExampleGenerator.DEBUG_SHOW_HELP) {
                    System.out.println("{" + key + "} => {" + value + "}");
                }
                Matcher m = Pattern.compile(TransliteratorUtilities.fromHTML.transliterate(key), Pattern.COMMENTS)
                    .matcher("");
                keys.add(m);
                values.add(value);
            } catch (RuntimeException e) {
                System.err.println("Help file has illegal regex: " + currentColumn[0]);
            }
        }
        currentColumn[0].setLength(0);
        currentColumn[1].setLength(0);
        column = 0;
    }

    public static String getChartMessages(String xpath) {
        synchronized (HelpMessages.class) {
            if (HelpMessages.helpMessages == null) {
                HelpMessages.helpMessages = new HelpMessages("chart_messages.html");
            }
        }
        return HelpMessages.helpMessages.find(xpath);
        // if (xpath.contains("/exemplarCharacters")) {
        // result = "The standard exemplar characters are those used in customary writing ([a-z] for English; "
        // + "the auxiliary characters are used in foreign words found in typical magazines, newspapers, &c.; "
        // + "currency auxilliary characters are those used in currency symbols, like 'US$ 1,234'. ";
        // }
        // return result == null ? null : TransliteratorUtilities.toHTML.transliterate(result);
    }
}