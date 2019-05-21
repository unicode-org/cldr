package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;

public class TestCompatibility extends TestFmwkPlus {
    private static final File ARCHIVE = new File(CLDRPaths.ARCHIVE_DIRECTORY);

    public static void main(String[] args) {
        new TestCompatibility().run(args);
    }

    public void TestReadWrite() throws IOException {
        checkFiles(ARCHIVE);
    }

    private void checkFiles(File dir) throws IOException {
        for (File file : dir.listFiles()) {
            // for now, only look at common
            if (file.getName().equals("main")) {
                checkXmlFile(file);
            } else if (file.isDirectory()) {
                checkFiles(file);
            }
        }
    }

    // for now, only look at common main
    private void checkXmlFile(File file) throws IOException {
        if (!file.getCanonicalPath().contains("cldr-27.0")) {
            return;
        }
        Factory factory = Factory.make(file.getCanonicalPath(), ".*");
        for (String language : factory.getAvailableLanguages()) {
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(language, false);
            } catch (Exception e) {
                errln("Couldn't read " + language + ":\t" + e.getLocalizedMessage() + ", in " + file.getCanonicalPath());
                continue;
            }
            try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);) {
                cldrFile.write(pw);
            } catch (Exception e) {
                errln("Couldn't write " + language + ":\t" + e.getLocalizedMessage() + ", in " + file.getCanonicalPath());
            }
        }
    }
}
