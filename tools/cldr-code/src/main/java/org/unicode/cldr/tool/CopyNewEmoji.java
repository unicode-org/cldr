package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class CopyNewEmoji {
    public static void main(String[] args) throws IOException {
        File sourceDir = new File("/Users/markdavis/eclipse-workspace/new_emoji");
        File targetDir =
                new File(
                        "/Users/markdavis/github/cldr/tools/cldr-apps/src/main/webapp/images/emoji");

        System.out.println(
                "sourceDir.isDirectory(): " + sourceDir.isDirectory() + ", " + sourceDir.exists());
        System.out.println(
                "targetDir.isDirectory(): " + targetDir.isDirectory() + ", " + targetDir.exists());

        final String[] sourceList = sourceDir.list();
        final String[] targetList = targetDir.list();

        Set<String> sourceFiles = ImmutableSet.copyOf(sourceList);
        Set<String> targetFiles = ImmutableSet.copyOf(targetList);

        System.out.println("sourceDir.isDirectory(): " + sourceFiles.size());
        System.out.println("targetDir.isDirectory(): " + targetFiles.size());

        for (String sourceFile : sourceList) {
            if (!targetFiles.contains(sourceFile)) {
                Files.move(new File(sourceDir, sourceFile), new File(targetDir, sourceFile));
                System.out.println(sourceFile + " => " + targetDir);
            }
        }
    }
}
