package org.unicode.cldr.tool;

import java.io.File;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.dev.util.CollectionUtilities;

public class CLDRCompare {
    public static void main(String[] args) throws Exception {
        String filter = CldrUtility.getProperty("filter", ".*");
        Matcher matcher = PatternCache.get(filter).matcher("");
        File oldVersion = new File(CldrUtility.getProperty("old", new File(CLDRPaths.COMMON_DIRECTORY
            + "../../../common-cldr1.6").getCanonicalPath()));
        if (!oldVersion.exists()) {
            throw new IllegalArgumentException("Directory not found");
        }
        File newVersion = new File(CldrUtility.getProperty("new", CLDRPaths.COMMON_DIRECTORY));
        if (!newVersion.exists()) {
            throw new IllegalArgumentException("Directory not found");
        }

        printLine("Dir", "File", "Same", "New", "Deleted", "≠Value", "≠Path≠Value", "≠Path");
        System.out.println("Directory" + "\t" + "File" + "\t" + "New" + "\t" + "Deleted" + "\t" + "SameValue" + "\t"
            + "DifferentValue");

        for (String subDir : newVersion.list()) {
            if (subDir.equals("CVS") || subDir.equals("posix") || subDir.equals("test")) continue;

            final String newSubDir = newVersion.getCanonicalPath() + "/" + subDir;
            final File srcDir = new File(newSubDir);
            if (!srcDir.isDirectory()) continue;

            final String oldSubDir = oldVersion.getCanonicalPath() + "/" + subDir;

            TreeSet<String> files = new TreeSet<String>();

            Factory cldrFactory = Factory.make(newSubDir, ".*");
            files.addAll(cldrFactory.getAvailable());
            Factory oldFactory = null;
            try {
                oldFactory = Factory.make(oldSubDir, ".*");
                files.addAll(oldFactory.getAvailable());
            } catch (Exception e2) {
            }

            for (String file : files) {
                String subDirKey = subDir + "/" + file;
                if (!matcher.reset(subDirKey).find()) {
                    // System.out.println("Skipping " + srcSubdir);
                    continue;
                }

                HashSet<String> paths = new HashSet<String>();
                CLDRFile newCldrFile = null;
                try {
                    newCldrFile = cldrFactory.make(file, false);
                    CollectionUtilities.addAll(newCldrFile.iterator(), paths);
                } catch (Exception e) {
                }
                CLDRFile oldCldrFile = null;
                try {
                    oldCldrFile = oldFactory.make(file, false);
                    CollectionUtilities.addAll(oldCldrFile.iterator(), paths);
                } catch (Exception e1) {
                }

                int sameCount = 0;
                int diffBothCount = 0;
                int deletedCount = 0;
                int newCount = 0;
                int diffValueCount = 0;
                int diffPathCount = 0;
                for (String path : paths) {
                    String newValue = newCldrFile == null ? null : newCldrFile.getStringValue(path);
                    String oldValue = oldCldrFile == null ? null : oldCldrFile.getStringValue(path);
                    if (newValue == null) {
                        if (oldValue != null) {
                            ++deletedCount;
                        } else {
                            throw new IllegalArgumentException("Should never happen");
                        }
                    } else if (oldValue == null) {
                        ++newCount;
                    } else {
                        String newFullPath = newCldrFile.getFullXPath(path);
                        String oldFullPath = oldCldrFile.getFullXPath(path);
                        boolean valuesSame = newValue.equals(oldValue);
                        boolean pathsSame = newFullPath.equals(oldFullPath);

                        if (valuesSame && pathsSame) {
                            sameCount++;
                        } else if (valuesSame) {
                            diffValueCount++;
                        } else if (pathsSame) {
                            diffPathCount++;
                        } else {
                            diffBothCount++;
                        }
                    }
                }
                printLine(subDir, file, sameCount, newCount, deletedCount, diffValueCount, diffBothCount,
                    diffPathCount);
            }
        }
    }

    private static void printLine(String subDir, String file, Object sameCount, Object newCount, Object deletedCount,
        Object diffValueCount, Object diffBothCount, Object diffPathCount) {
        System.out.println(subDir + "\t" + file + "\t" + sameCount + "\t" + newCount + "\t" + deletedCount + "\t"
            + diffValueCount + "\t" + diffPathCount + "\t" + diffBothCount);
    }
}
