package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.unicode.cldr.util.CLDRPaths;

public class CopyNewEmoji {
    public static void main(String[] args) throws IOException {
        File targetDir = new File(CLDRPaths.APPS_EMOJI_DIRECTORY);

        // TODO make this a command argument
        File sourceDir = new File(CLDRPaths.CLDR_PRIVATE_DIRECTORY + "new_emoji");

        System.out.println(
                "sourceDir: "
                        + sourceDir
                        + "; isDirectory(), exists(): "
                        + sourceDir.isDirectory()
                        + ", "
                        + sourceDir.exists());
        System.out.println(
                "targetDir: "
                        + targetDir
                        + "; isDirectory(), exists(): "
                        + targetDir.isDirectory()
                        + ", "
                        + targetDir.exists());

        final String[] sourceList = sourceDir.list();
        final String[] targetList = targetDir.list();

        Set<String> sourceFiles = ImmutableSet.copyOf(sourceList);
        Set<String> targetFiles = ImmutableSet.copyOf(targetList);

        System.out.println("sourceFiles: " + sourceFiles.size());
        System.out.println("targetFiles: " + targetFiles.size());

        for (String sourceFile : sourceList) {
            if (!targetFiles.contains(sourceFile)) {
                Files.move(new File(sourceDir, sourceFile), new File(targetDir, sourceFile));
                System.out.println(sourceFile + " => " + targetDir);
            }
        }
    }
}
