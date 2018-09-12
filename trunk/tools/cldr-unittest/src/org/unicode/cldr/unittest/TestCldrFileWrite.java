package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XMLFileReader;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.util.ICUUncheckedIOException;

public class TestCldrFileWrite extends TestFmwkPlus {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestCldrFileWrite().run(args);
    }

    public void testAllXml() {
        Predicate<String> retain = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return !exclude.contains(input);
            }

            Set<String> exclude = ImmutableSet.of("tools", "specs");
        };
        checkDir(new File(CLDRPaths.BASE_DIRECTORY), new File(CLDRPaths.GEN_DIRECTORY + "formatted"), retain);
    }

    static Function<String, String> MyFilter = new Function<String, String>() {
        Pattern spaceHandler = Pattern.compile("\\s+");

        @Override
        public String apply(String input) {
            return spaceHandler.matcher(input).replaceAll(" ");
        }
    };

    static final Predicate<Pair<String, String>> MyPredicate = new Predicate<Pair<String, String>>() {
        @Override
        public boolean apply(Pair<String, String> input) {
            return input.getFirst().contains("//ldml/identity/version[@number");
        }
    };

    private void checkDir(File file, File outFile, Predicate<String> retain) {
        String dirName = file.getName();
        if (!retain.apply(dirName)) {
            return;
        }
        for (File subfile : file.listFiles()) {
            String subName = subfile.getName();
            if (subfile.isDirectory()) {
                checkDir(subfile, new File(outFile, subName), retain);
            } else if (subName.endsWith(".xml")) {
                checkFile(file, outFile, subName);
            }
        }
    }

    private void checkFile(File dirIn, File dirOut, String fileName) {
        // TODO cache factories
        String dirName = dirIn.toString();
        Factory afactory = SimpleFactory.make(dirName, ".*");
        String fileNameMinusXml = fileName.substring(0, fileName.length() - 4);
        CLDRFile cldrFile = afactory.make(fileNameMinusXml, false);
        String outDir = dirOut.toString();
        try (PrintWriter out = new PrintWriter(FileUtilities.openUTF8Writer(outDir, fileName))) {
            cldrFile.write(out);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(dirIn.toString(), e);
        }
        checkCompareXmlFiles(dirIn, dirOut, fileName);
    }

    private void checkCompareXmlFiles(File dirIn, File dirOut, String fileName) {
        File fileIn = new File(dirIn, fileName);
        File fileOut = new File(dirOut, fileName);
        new ArrayList<Pair<String, String>>();
        Multiset<Pair<String, String>> cldrPaths = HashMultiset.create(XMLFileReader.loadPathValues(
            fileIn.toString(), new ArrayList<Pair<String, String>>(), true, false, MyFilter));
        Multiset<Pair<String, String>> rewritePaths = HashMultiset.create(XMLFileReader.loadPathValues(
            fileOut.toString(), new ArrayList<Pair<String, String>>(), true, false, MyFilter));

        checkAMinusB(fileIn + "\tnot copied\t", cldrPaths, rewritePaths, MyPredicate);
        checkAMinusB(fileOut + "\tsuperfluous\t", rewritePaths, cldrPaths, MyPredicate);
    }

    private <T extends Comparable> void checkAMinusB(String title, Multiset<T> a, Multiset<T> b, Predicate<T> skip) {
        TreeMultiset<T> result = TreeMultiset.create(a);
        result.removeAll(b);
        if (result.size() != 0) {
            int i = 0;
            for (T item : result) {
                if (++i > 5) break;
                if (skip.apply(item)) {
                    continue;
                }
                String msg = item.toString();
                if (msg.length() > 200) {
                    msg = msg.substring(0, 200) + "…";
                }
                errln(title + msg);
            }
        }
    }
}
