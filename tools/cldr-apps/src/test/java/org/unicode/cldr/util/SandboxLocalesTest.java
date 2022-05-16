package org.unicode.cldr.util;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRFile.DraftStatus;

class SandboxLocalesTest {

    private static File tmpdir;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        tmpdir = Files.createTempDirectory(SandboxLocalesTest.class.getSimpleName()).toFile();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        tmpdir.deleteOnExit();
    }

    @Test
    void test() throws IOException {
        CLDRConfig instance = CLDRConfig.getInstance();
        SandboxLocales s = new SandboxLocales(tmpdir);
        assertTrue(tmpdir.isDirectory(), "tmpdir exists");
        assertTrue(new File(s.getMainDir(), "mul.xml").canRead(), "mul.xml exists");
        assertTrue(new File(s.getAnnotationsDir(), "mul.xml").canRead(), "annotations mul.xml exists");
        assertFalse(new File(s.getMainDir(), "und.xml").canRead(), "und.xml exists");

        Factory factory = s.getFactory(new File(CLDRPaths.MAIN_DIRECTORY));
        CheckCLDR check = CheckCLDR.getCheckAll(factory, ".*");
        check.setEnglishFile(instance.getEnglish());
        CheckCLDR.Options options = new CheckCLDR.Options();
        // our factory includes common/main, so limit here.
        for(final CLDRLocale l : SpecialLocales.getByType(SpecialLocales.Type.scratch)) {
            System.out.println("Testing " + l);
            // A noted in CLDR-14336, factory.make needs at least DraftStatus.unconfirmed. That is the default
            // for the 2-argument form without an explicit minimalDraftStatus param.
            CLDRFile f = factory.make(l.getBaseName(), true);
            List<CheckCLDR.CheckStatus> errs = new LinkedList<>();
            check.setCldrFileToCheck(f, options, errs);
            for(final CheckCLDR.CheckStatus err : errs) {
                System.err.println(l.getBaseName() + ": " + err.getMessage() + " - " + err.getType() + "/" +  err.getSubtype());
            }
            assertTrue(errs.isEmpty(), "had " + errs.size() +" error(s) in " + l);
        }
    }
}
