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
            // As part of CLDR-14336 I had to change the minimalDraftStatus parameter below from null to unconfirmed.
            // Without that an NPE exception was thrown below the following. The Factory.make call ends up in
            // SimpleFactory.handleMake; the first call there for "mul" with resolved=true calls makeResolvingSource
            // which recursively calls SimpleFactory.handleMake, eventually ending up with a call for "root" with
            // resolved=false that calls new CLDRFile. That ends up in XMLNormalizingLoader.getFrozenInstance. With
            // the null DraftStatus, the NPE occurred inside the call there to cache.getUnchecked(key) which is
            // Google library code.
            CLDRFile f = factory.make(l.getBaseName(), true, DraftStatus.unconfirmed);
            List<CheckCLDR.CheckStatus> errs = new LinkedList<>();
            check.setCldrFileToCheck(f, options, errs);
            for(final CheckCLDR.CheckStatus err : errs) {
                System.err.println(l.getBaseName() + ": " + err.getMessage() + " - " + err.getType() + "/" +  err.getSubtype());
            }
            assertTrue(errs.isEmpty(), "had " + errs.size() +" error(s) in " + l);
        }
    }
}
