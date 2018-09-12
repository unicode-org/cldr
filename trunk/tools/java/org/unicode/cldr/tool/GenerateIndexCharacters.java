package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;

import com.ibm.icu.text.AlphabeticIndex;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateIndexCharacters {
    public static void main(String[] args) throws IOException {
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> available = cldrFactory.getAvailable();

        for (String locale : available) {
            String cleanedSet = getConstructedIndexSet(locale, cldrFactory.make(locale, true));
            CLDRFile temp = SimpleFactory.makeFile(locale);
            temp.add("//ldml/characters/exemplarCharacters[@type=\"index\"][@draft=\"unconfirmed\"]", cleanedSet);
            PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "indexchars/", locale + ".xml");
            temp.write(out);
            out.close();
        }
    }

    public static String getConstructedIndexSet(String locale, CLDRFile cFile) {
        ULocale uLocale = new ULocale(locale);
        Collator collator = Collator.getInstance(uLocale);
        collator.setStrength(Collator.PRIMARY); // TODO: ought to build the collator from CLDR instead of from ICU.
        AlphabeticIndex<String> index = new AlphabeticIndex<String>(uLocale);
        index.clearRecords();
        UnicodeSet indexLabels = cFile.getExemplarSet("index", WinningChoice.WINNING);
        if (indexLabels != null && indexLabels.size() > 0) {
            index.addLabels(indexLabels);
        }
        UnicodeSet uset = new UnicodeSet();
        List<String> items = index.getBucketLabels();
        for (String item : items) {
            uset.add(item);
        }
        UnicodeSetPrettyPrinter pp = new UnicodeSetPrettyPrinter()
            .setCompressRanges(true)
            .setToQuote(DisplayAndInputProcessor.TO_QUOTE)
            .setOrdering(collator)
            .setSpaceComparator(collator);

        String cleanedSet = DisplayAndInputProcessor.getCleanedUnicodeSet(uset, pp, ExemplarType.index);
        return cleanedSet;
    }
}
