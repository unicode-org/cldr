/**
 * 
 */
package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16.StringComparator;

class Subheader {
    Matcher isArchaic = GeneratePickerData.IS_ARCHAIC.matcher("");
    Matcher subheadMatcher = Pattern.compile("(@+)\\s+(.*)").matcher("");
    Matcher hexMatcher = Pattern.compile("([A-Z0-9]+).*").matcher("");
    Map<Integer, String> codePoint2Subblock = new HashMap();
    Map<String, UnicodeSet> subblock2UnicodeSet = new TreeMap();
    Map<String,Set<String>> block2subblock = new TreeMap();
    Map<String,Set<String>> subblock2block = new TreeMap();

    Subheader(String unicodeDataDirectory, String outputDirectory) throws IOException {
        UnicodeSet archaicSubblock = new UnicodeSet();

        getDataFromFile(unicodeDataDirectory, "NamesList.*\\.txt");

        if (false) {
            if (GeneratePickerData.DEBUG)
                System.out.println("*** Fixing plurals");
            for (java.util.Iterator<String> it = subblock2UnicodeSet.keySet().iterator(); it.hasNext();) {
                String subblock = it.next();
                final String pluralSubblock = subblock + "s";
                UnicodeSet plural = subblock2UnicodeSet.get(pluralSubblock);
                if (plural != null) {
                    if (GeneratePickerData.DEBUG)
                        System.out.println(subblock + " => " + pluralSubblock);
                    UnicodeSet singular = subblock2UnicodeSet.get(subblock);
                    plural.addAll(singular);
                    it.remove();
                }
            }
            if (GeneratePickerData.DEBUG) System.out.println("*** Done Fixing plurals");
        }

        for (String subblock : subblock2UnicodeSet.keySet()) {
            final UnicodeSet uset = subblock2UnicodeSet.get(subblock);
            for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
                codePoint2Subblock.put(it.codepoint, subblock);

                String block = UCharacter.getStringPropertyValue(UProperty.BLOCK, it.codepoint, UProperty.NameChoice.LONG).toString().replace('_', ' ').intern();

                Set<String> set = block2subblock.get(block);
                if (set == null) {
                    block2subblock.put(block, set = new TreeSet());
                }
                set.add(subblock);

                set = subblock2block.get(subblock);
                if (set == null) {
                    subblock2block.put(subblock, set = new TreeSet());
                }
                set.add(block);

                String name = UCharacter.getExtendedName(it.codepoint);
                if (isArchaic.reset(block).find() || isArchaic.reset(subblock).find() || isArchaic.reset(name).find()) {
                    archaicSubblock.add(it.codepoint);
                }
            }
        }
        System.out.println("Characters with Archaic Subblocks: " + archaicSubblock);
        writeBlockInfo(outputDirectory);
    }

    private void writeBlockInfo(String outputDirectory) throws IOException, FileNotFoundException {
        System.out.println("***Block/Subblock start");
        PrintWriter out = GeneratePickerData.getFileWriter(outputDirectory, "blocks_subblocks.html");

        htmlHeader(out);
        out.println("<tr><th>" + "Block" + "</th><th>" + "Notes" + "</th><th>" + "Subblock" + "</th></tr>");
        for (String block : block2subblock.keySet()) {
            final Set<String> set = block2subblock.get(block);
            for (String subblock2 : set) {
                out.println("<tr><td>" + block + "</td><td>" +
                        (subblock2.equalsIgnoreCase(block) || subblock2.equalsIgnoreCase(block + "s") ? "duplicate" : "") +
                        (set.size() < 2 ? " singleton" : "")
                        + "\u00a0" 
                        + "</td><td>" + subblock2 + 
                "</td></tr>");
            }
        }
        out.println("</table></body></html>");
        out.close();
        System.out.println("***By subblocks");

        out = GeneratePickerData.getFileWriter(outputDirectory, "subblocks_blocks.html");


        htmlHeader(out);
        out.println("<tr><th>" + "Subblock" + "</th><th>" + "Notes" + "</th><th>" + "Blocks" + "</th></tr>");
        StringComparator caseless = new UTF16.StringComparator(true, true, 0);
        TreeSet<String> tests = new TreeSet(caseless);
        tests.addAll(subblock2block.keySet());
        for (String subblock2 : subblock2block.keySet()) {
            final Set<String> set = subblock2block.get(subblock2);
            final String first = set.iterator().next();
            String otherString = String.valueOf(set);
            otherString = otherString.substring(1,otherString.length()-1) + '\u00a0';
            out.println("<tr><td>" + subblock2 
                    + "</td><td>" + getComments(subblock2, tests) 
                    + "</td><td>" + otherString 
                    + "</td></tr>");
        }
        System.out.println("***Block/Subblock end");
        out.close();
    }

    private String getComments(String subblock2, Set<String> keySet) {

        if (keySet.contains(subblock2 + "s")
                || keySet.contains("Additional " + subblock2)
                || keySet.contains("Additional " + subblock2 + "s")
                || keySet.contains("Other " + subblock2)
                || keySet.contains("Other " + subblock2 + "s")
                || keySet.contains("Miscellaneous " + subblock2)
                || keySet.contains("Miscellaneous " + subblock2 + "s")
        ) return "has-longer";
        return "\u00a0";
    }

    private void htmlHeader(PrintWriter out) {
        out.println("<html><head>" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>" +
                "<style>" +
                "table    { border-spacing: 0; border-collapse: collapse; border-style: solid; border-color: blue; border-width: 1px; }" + 
                "th, td    { border-spacing: 0; border-collapse: collapse; border-style: solid; border-color: blue; border-width: 1px;" + 
                "color: black; vertical-align: top; text-align: left;        }" + 
                "</style>" +
                "</head>" +
                "<body><table>"
        );
    }

    private String getDataFromFile(String dir, String filenameRegex) throws FileNotFoundException, IOException {
        String subblock = "?";
        File actualName = getFileNameFromPattern(dir, filenameRegex);
        BufferedReader in = new BufferedReader(new FileReader(actualName));
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            if (subheadMatcher.reset(line).matches()) {
                subblock = subheadMatcher.group(1).equals("@") ? subheadMatcher.group(2) : "?";
                continue;
            }
            if (subblock.length() != 0 && hexMatcher.reset(line).matches()) {
                int cp = Integer.parseInt(hexMatcher.group(1), 16);
                UnicodeSet uset = subblock2UnicodeSet.get(subblock);
                if (uset == null) {
                    subblock2UnicodeSet.put(subblock, uset = new UnicodeSet());
                }
                uset.add(cp);
            }
        }
        in.close();
        return subblock;
    }

    public static File getFileNameFromPattern(String directory, String filenameRegex) {
        try {
            File dir = new File(directory);
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Not a directory: " + dir.getCanonicalPath());
            }
            String[] files = dir.list(new RegexFileFilter(filenameRegex));
            if (files.length != 1) {
                throw new IllegalArgumentException("Not a unique match for : " + dir.getCanonicalPath() + " / " + filenameRegex + " : " + Arrays.asList(files));
            }
            return new File(directory, files[0]);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class RegexFileFilter implements FilenameFilter {
        private Matcher m;
        public RegexFileFilter(String regex) {
            set(regex);
        }
        public boolean accept(File dir, String name) {
            return m.reset(name).matches();
        }
        public void set(String regex) {
            m = Pattern.compile(regex).matcher("");
        }
    }

    String getSubheader(int codepoint) {
        return codePoint2Subblock.get(codepoint);
    }
}