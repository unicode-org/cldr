package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StringIterables;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

/**
 * One-time class to fix delimiters
 *
 * @author markdavis
 *
 */
public class FixDelimiters {

    public static void main(String[] args) throws IOException {
        SupplementalDataInfo info = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = info.getDefaultContentLocales();

        for (Entry<String, R2<Quotes, Quotes>> entry : Data.locales2delimiters.entrySet()) {
            System.out.println(entry);
        }
        Factory factory = SimpleFactory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> remainder = new LinkedHashSet<String>(Data.locales2delimiters.keySet());

        String[] paths = {
            "//ldml/delimiters/quotationStart",
            "//ldml/delimiters/quotationEnd",
            "//ldml/delimiters/alternateQuotationStart",
            "//ldml/delimiters/alternateQuotationEnd"
        };
        String[] oldValue = new String[4];
        String[] newValue = new String[4];

        System.out.println("Writing data");
        for (String locale : factory.getAvailable()) {
            if (defaultContentLocales.contains(locale)) continue;

            R2<Quotes, Quotes> data = Data.locales2delimiters.get(locale);
            if (data == null) {
                continue;
            }
            if (!remainder.contains(locale)) {
                System.out.println("Superflous: " + locale);
            } else {
                remainder.remove(locale);
            }
            CLDRFile cldrFile = factory.make(locale, false).cloneAsThawed();

            newValue[0] = data.get0().start;
            newValue[1] = data.get0().end;
            newValue[2] = data.get1().start;
            newValue[3] = data.get1().end;

            for (int i = 0; i < paths.length; ++i) {
                String value = cldrFile.getStringValue(paths[i]);
                oldValue[i] = value;
                if (newValue[i].equals(oldValue[i])) {
                    continue;
                }
                cldrFile.add(paths[i], newValue[i]);
                String revalue = cldrFile.getStringValue(paths[i]);
                System.out.println(locale + "\t" + paths[i] + "\t" + oldValue[i] + "\t=>\t" + revalue);
            }
            PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "temp/", locale + ".xml");
            cldrFile.write(pw);
            pw.close();
        }
        System.out.println("Missing: " + remainder);
    }

    static class Quotes {
        static Matcher quotes = PatternCache.get("(.*)…(.*)").matcher("");
        final String start;
        final String end;

        Quotes(String input) {
            if (!quotes.reset(input).matches()) {
                throw new IllegalArgumentException(input);
            }
            start = quotes.group(1);
            end = quotes.group(2);
        }

        public String toString() {
            return start + "...." + end;
        }
    }

    static class Data {
        static Map<String, Row.R2<Quotes, Quotes>> locales2delimiters = new LinkedHashMap<String, Row.R2<Quotes, Quotes>>();
        static Matcher localeString = PatternCache.get(".*\\((.*)\\)").matcher("");
        static {
            final String instructionFile = "delimiterFixes.txt";
            System.out.println("Instruction file: " + instructionFile);
            for (String line : StringIterables.in(FixDelimiters.class, instructionFile)) {
                int first = line.indexOf(' ');
                int second = line.indexOf(' ', first + 1);
                Quotes qmain = new Quotes(line.substring(0, first));
                Quotes qalt = new Quotes(line.substring(first + 1, second));
                R2<Quotes, Quotes> both = Row.of(qmain, qalt);
                String last = line.substring(second);
                String[] locales = last.split("\\s*;\\s*");
                for (String locale : locales) {
                    if (!localeString.reset(locale).matches()) {
                        throw new IllegalArgumentException("<" + locale + "> in " + line);
                    }
                    String localeCode = localeString.group(1);
                    locales2delimiters.put(localeCode, both);
                }
            }
        }
    }
}
