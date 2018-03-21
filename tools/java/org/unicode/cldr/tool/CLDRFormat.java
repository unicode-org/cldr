package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
//import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.dev.util.CollectionUtilities;

public class CLDRFormat {
    public static void main(String[] args) throws Exception {
        // TODO - make these parameters
        String filter = CldrUtility.getProperty("filter", ".*");
        Matcher matcher = PatternCache.get(filter).matcher("");
        File src = new File(CLDRPaths.COMMON_DIRECTORY);
        File dest = new File(CLDRPaths.BASE_DIRECTORY + "/common-test/");
        File dtd = new File(dest + "/main/" + "../../common/dtd/ldmlSupplemental.dtd");
        if (!dtd.exists()) {
            throw new IllegalArgumentException("Can't access DTD\nas is: " + dtd + "\ncanonical: "
                + dtd.getCanonicalPath());
        }
        // Log.setLog(Utility.GEN_DIRECTORY + "logCldr.txt");
        for (String subDir : src.list()) {
            if (subDir.equals("CVS") || subDir.equals("posix") || subDir.equals("test")) continue;
            final String srcSubdir = src + "/" + subDir;
            final File srcDir = new File(srcSubdir);
            if (!srcDir.isDirectory()) continue;
            final String destSubdir = dest + "/" + subDir;
            Factory cldrFactory = Factory.make(srcSubdir, ".*");
            for (String key : cldrFactory.getAvailable()) {
                String subDirKey = subDir + "/" + key;
                if (!matcher.reset(subDirKey).find()) {
                    // System.out.println("Skipping " + srcSubdir);
                    continue;
                }
                CLDRFile cldrFile = cldrFactory.make(key, false);

                // write
                PrintWriter out = FileUtilities.openUTF8Writer(destSubdir, key + ".xml");
                cldrFile.write(out);
                out.close();

                // StringWriter stringWriter = new StringWriter();
                // PrintWriter pw = new PrintWriter(stringWriter);
                // cldrFile.write(pw);
                // pw.flush();
                // final String results = stringWriter.toString();
                // pw.close();

                // check
                try {
                    // byte[] utf8 = results.getBytes("utf-8");
                    // CLDRFile regenFile = CLDRFile.make(destSubdir + key, key, new ByteArrayInputStream(utf8),
                    // DraftStatus.unconfirmed);
                    CLDRFile regenFile = SimpleFactory.makeFile(key, destSubdir + key, DraftStatus.unconfirmed);
                    String diff = findFirstDifference(cldrFile, regenFile);
                    if (diff != null) {
                        System.out.println("\tERROR: difference introduced in reformatting " + srcSubdir + "/" + key
                            + ".xml" + "\n" + diff);
                    }
                } catch (Exception e) {
                    System.err.println("\tERROR: can't read reformatted file " + srcSubdir + "/" + key + ".xml");
                    // e.printStackTrace();
                }
            }
        }
    }

    static Set<String> keys1 = new TreeSet<String>();
    static Set<String> keys2 = new TreeSet<String>();

    private static String findFirstDifference(CLDRFile cldrFile, CLDRFile regenFile) {
        keys1.clear();
        keys2.clear();
        CollectionUtilities.addAll(cldrFile.iterator(), keys1);
        CollectionUtilities.addAll(regenFile.iterator(), keys2);
        if (!keys1.equals(keys2)) {
            Set<String> missing = new TreeSet<String>(keys1);
            missing.removeAll(keys2);
            Set<String> extras = new TreeSet<String>(keys2);
            extras.removeAll(keys1);
            return "\tMissing: " + missing.toString().replace(", ", ",\n") + ";\n\tExtras: "
                + extras.toString().replace(", ", ",\n");

        }
        for (String path : keys1) {
            if (path.startsWith("//ldml/identity/generation") || path.startsWith("//ldml/identity/version")) {
                continue;
            }
            String full1 = cldrFile.getFullXPath(path);
            String full2 = regenFile.getFullXPath(path);
            if (!full1.equals(full2)) {
                return "\tFull XPaths differ: " + full1 + "!=" + full2;
            }
        }
//        Comments comments1 = cldrFile.getXpath_comments();
//        Comments comments2 = regenFile.getXpath_comments();
        // TODO fix later
        return null;
    }
}
