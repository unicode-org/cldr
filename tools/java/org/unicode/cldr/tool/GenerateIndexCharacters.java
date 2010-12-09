package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.WinningChoice;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.AlphabeticIndex;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateIndexCharacters {
    public static void main(String[] args) throws IOException {
        CLDRFile.Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        Set<String> available = cldrFactory.getAvailable();

        for (String locale : available) {
            String cleanedSet = getConstructedIndexSet(locale, cldrFactory.make(locale, true));
            CLDRFile temp = CLDRFile.make(locale);
            temp.add("//ldml/characters/exemplarCharacters[@type=\"index\"][@draft=\"unconfirmed\"]", cleanedSet);
            PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "indexchars/", locale + ".xml");
            temp.write(out);
            out.close();
        }
    }

    public static String getConstructedIndexSet(String locale, CLDRFile cFile) {
        ULocale uLocale = new ULocale(locale);
        Collator collator = Collator.getInstance(uLocale).setStrength2(Collator.PRIMARY);// ought to build from CLDR, but...
        AlphabeticIndex index = new AlphabeticIndex(uLocale, (RuleBasedCollator)collator, cFile.getExemplarSet("", WinningChoice.WINNING));
        UnicodeSet uset = new UnicodeSet();
        List<String> items = index.getBucketLabels();
        for (String item : items) {
            uset.add(item);
        }
        PrettyPrinter pp = new PrettyPrinter()
        .setCompressRanges(true)
        .setToQuote(DisplayAndInputProcessor.TO_QUOTE)
        .setOrdering(collator)
        .setSpaceComparator(collator);

        String cleanedSet = DisplayAndInputProcessor.getCleanedUnicodeSet(uset, pp, false);
        return cleanedSet;
    }
}
