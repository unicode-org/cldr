package org.unicode.cldr.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.Builder;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;

public class IcuTextWriter {
    /**
     * The default tab indent (actually spaces)
     */
    private static final String TAB = "    ";

    /**
     * ICU paths have a simple comparison, alphabetical within a level. We do
     * have to catch the / so that it is lower than everything.
     */
    public static final Comparator<String>       PATH_COMPARATOR       =
        new Comparator<String>() {
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

    /**
     * Write a file in ICU format. LDML2ICUConverter currently has some
     * funny formatting in a few cases; don't try to match everything.
     * 
     * @param icuData the icu data structure to be written
     * @param dirPath the directory to write the file to
     * @param hasSpecial true if a special file was used to create the ICU data
     */
    public static void writeToFile(IcuData icuData, String dirPath) throws IOException {
        String name = icuData.getName();
        PrintWriter out = BagFormatter.openUTF8Writer(dirPath, name + ".txt");
        out.write('\uFEFF');

        // Append the header.
        String[] replacements = { "%source%", icuData.getSourceFile() };
        FileUtilities.appendFile(LDMLConverter.class, "ldml2icu_header.txt", null, replacements, out);
        if (icuData.hasSpecial()) {
            out.println("/**");
            out.println(" *  ICU <specials> source: <path>/xml/main/" + name + ".xml");
            out.println(" */");
        }

        // Write the ICU data to file.
        out.append(name);
        if (!icuData.hasFallback()) out.append(":table(nofallback)");
        List<String> sortedPaths = new ArrayList<String>(icuData.keySet());
        Collections.sort(sortedPaths, PATH_COMPARATOR);
        String[] lastLabels = new String[] {};
        boolean wasSingular = false;
        for (String path : sortedPaths) {
            // Write values to file.
            String[] labels = path.split("/", -1); // Don't discard trailing slashes.
            int common = getCommon(lastLabels, labels);
            for (int i = lastLabels.length - 1; i > common; --i) {
                if (wasSingular) {
                    wasSingular = false;
                } else {
                    out.append(Utility.repeat(TAB, i));
                }
                out.append("}\n");
            }
            for (int i = common + 1; i < labels.length; ++i) {
                final String pad = Utility.repeat(TAB, i);
                out.append(pad);
                String label = labels[i];
                if (!label.startsWith("<") && !label.endsWith(">")) {
                    out.append(label);
                }
                out.append('{');
                if (i != labels.length - 1) {
                    out.append('\n');
                }
            }
            boolean quote = !IcuData.isIntRbPath(path);
            List<String[]> values = icuData.get(path);
            wasSingular = appendValues(values, labels.length, quote, out);
            out.flush();
            lastLabels = labels;
        }
        // Add last closing braces.
        for (int i = lastLabels.length - 1; i > 0; --i) {
            if (wasSingular) {
                wasSingular = false;
            } else {
                out.append(Utility.repeat(TAB, i));
            }
            out.append("}\n");
        }
        out.append("}\n");
        out.close();
    }

    /**
     * Inserts padding and values between braces.
     * @param values
     * @param numTabs
     * @param quote
     * @param out
     * @return
     */
    private static boolean appendValues(List<String[]> values, int numTabs, boolean quote, PrintWriter out) {
        String[] firstArray;
        boolean wasSingular = false;
        if (values.size() == 1) {
            if ((firstArray = values.get(0)).length == 1) {
                String value = quoteInside(firstArray[0]);
                int maxWidth = 84 - Math.min(4, numTabs) * TAB.length();
                if (value.length() <= maxWidth) {
                    // Single value for path: don't add newlines.
                    appendQuoted(value, quote, out);
                    wasSingular = true;
                } else {
                    // Value too long to fit in one line, so wrap.
                    final String pad = Utility.repeat(TAB, numTabs);
                    out.append('\n');
                    int end;
                    for (int i = 0; i < value.length(); i = end) {
                        end = goodBreak(value, i + maxWidth);
                        String part = value.substring(i, end);
                        out.append(pad);
                        appendQuoted(part, quote, out).append('\n');
                    }
                }
            } else {
                // Only one array for the rbPath, so don't add an extra set of braces.
                final String pad = Utility.repeat(TAB, numTabs);
                out.append('\n');
                appendArray(pad, firstArray, quote, out);
            }
        } else {
            final String pad = Utility.repeat(TAB, numTabs);
            out.append('\n');
            for (String[] valueArray : values) {
                if (valueArray.length == 1) {
                    // Single-value array: print normally.
                    appendArray(pad, valueArray, quote, out);
                } else {
                    // Enclose this array in braces to separate it from other
                    // values.
                    out.append(pad).append("{\n");
                    appendArray(pad + TAB, valueArray, quote, out);
                    out.append(pad).append("}\n");
                }
            }
        }
        return wasSingular;
    }
    
    private static PrintWriter appendArray(String padding, String[] valueArray, boolean quote, PrintWriter out) {
        for (String value : valueArray) {
            out.append(padding);
            appendQuoted(quoteInside(value), quote, out).append(",\n");
        }
        return out;
    }

    private static PrintWriter appendQuoted(String value, boolean quote, PrintWriter out) {
        if (quote) {
            return out.append('"').append(value).append('"');
        } else {
            return out.append(value);
        }
    }

    /**
     * Can a string be broken here? If not, backup until we can.
     * 
     * @param quoted
     * @param end
     * @return
     */
    private static int goodBreak(String quoted, int end) {
        if (end > quoted.length()) {
            return quoted.length();
        }
        // Don't break escaped Unicode characters.
        for (int i = end - 1; i > end - 6; i--) {
            if (quoted.charAt(i) == '\\') {
                if (quoted.charAt(i + 1) == 'u') {
                    return i;
                }
                break;
            }
        }
        while (end > 0) {
            char ch = quoted.charAt(end - 1);
            if (ch != '\\' && (ch < '\uD800' || ch > '\uDFFF')) {
                break;
            }
            --end;
        }
        return end;
    }

    /**
     * Fix characters inside strings.
     * 
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
     * 
     * @param item
     * @return
     */
    private static int getCommon(String[] lastLabels, String[] labels) {
        int min = Math.min(lastLabels.length, labels.length);
        int i;
        for (i = 0; i < min; ++i) {
            if (!lastLabels[i].equals(labels[i])) {
                return i - 1;
            }
        }
        return i - 1;
    }
}
