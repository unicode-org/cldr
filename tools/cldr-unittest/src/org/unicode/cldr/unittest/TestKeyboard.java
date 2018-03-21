package org.unicode.cldr.unittest;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.draft.Keyboard;
import org.unicode.cldr.draft.Keyboard.KeyboardWarningException;
import org.unicode.cldr.util.CLDRConfig;

public class TestKeyboard extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestKeyboard().run(args);
    }

    static final String dtdLocation;
    static {
        try {
            dtdLocation = CLDRConfig.getInstance().getCldrBaseDirectory()
                .getCanonicalPath()
                + "/keyboards/dtd/ldmlKeyboard.dtd";
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void TestGoodSample() throws IOException {
        // preload these to make debugging easier
        // DtdData.getInstance(DtdType.ldml);
        // DtdData.getInstance(DtdType.keyboard);

        String fixedDtdReference = sampleGood.replace(
            "../dtd/ldmlKeyboard.dtd", dtdLocation);
        Reader r = new StringReader(fixedDtdReference);
        Set<Exception> errors = new LinkedHashSet<>();
        Keyboard.getKeyboard("no-error", r, errors);
        assertEquals("sample-without-errors", Collections.EMPTY_SET, errors);
    }

    public void TestBadSample() throws IOException {
        // preload these to make debugging easier
        // DtdData.getInstance(DtdType.ldml);
        // DtdData.getInstance(DtdType.keyboard);

        String fixedDtdReference = sampleBad.replace("../dtd/ldmlKeyboard.dtd",
            dtdLocation);
        Reader r = new StringReader(fixedDtdReference);
        Set<Exception> errors = new LinkedHashSet<>();
        Keyboard.getKeyboard("sample-with-error", r, errors);
        assertNotEquals("should have errors", Collections.EMPTY_SET, errors);
        if (isVerbose()) {
            for (Exception e : errors) {
                showException(e, "");
            }
        }
    }

    public void showException(Throwable e, String indent) {
        logln(e + "\t" + e.getMessage());
        for (StackTraceElement ste : e.getStackTrace()) {
            String className = ste.getClassName();
            if ((className.startsWith("org.unicode") && !className.contains("XMLFileReader"))
                || (className.startsWith("com.ibm") && !className.contains("TestFmwk"))) {
                logln("\t" + indent + ste);
            }
        }
        Throwable cause = e.getCause();
        if (cause != null) {
            showException(cause, "\t" + indent);
        }
    }

    String sampleGood = "<?xml version='1.0' encoding='UTF-8' ?>\n"
        + "<!DOCTYPE keyboard SYSTEM '../dtd/ldmlKeyboard.dtd'>\n"
        + "<keyboard locale='en-t-k0-android'>\n"
        + "  <version platform='4.4' number='$Revision: 9576 $'/>\n"
        + "  <names>\n"
        + "      <name value='English United States'/>\n"
        + "  </names>\n"
        + "  <keyMap>\n"
        + "      <map iso='D01' to='q'/>\n"
        + "      <map iso='D02' to='w'/>\n"
        + "      <map iso='D03' to='e' longPress='è é ê ë ē'/>\n"
        + "      <map iso='D04' to='r'/>\n"
        + "      <map iso='D05' to='t'/>\n"
        + "      <map iso='D06' to='y'/>\n"
        + "      <map iso='D07' to='u' longPress='û ü ù ú ū'/>\n"
        + "      <map iso='D08' to='i' longPress='î ï í ī ì'/>\n"
        + "      <map iso='D09' to='o' longPress='ô ö ò ó œ ø ō õ'/>\n"
        + "      <map iso='D10' to='p'/>\n"
        + "      <map iso='C01' to='a' longPress='à á â ä æ ã å ā'/>\n"
        + "      <map iso='C02' to='s' longPress='ß'/>\n"
        + "      <map iso='C03' to='d'/>\n"
        + "      <map iso='C04' to='f'/>\n"
        + "      <map iso='C05' to='g'/>\n"
        + "      <map iso='C06' to='h'/>\n"
        + "      <map iso='C07' to='j'/>\n"
        + "      <map iso='C08' to='k'/>\n"
        + "      <map iso='C09' to='l'/>\n"
        + "      <map iso='B01' to='z'/>\n"
        + "      <map iso='B02' to='x'/>\n"
        + "      <map iso='B03' to='c' longPress='ç'/>\n"
        + "      <map iso='B04' to='v'/>\n"
        + "      <map iso='B05' to='b'/>\n"
        + "      <map iso='B06' to='n' longPress='ñ'/>\n"
        + "      <map iso='B07' to='m'/>\n"
        + "      <map iso='A02' to=','/> <!-- (key to left of space) -->\n"
        + "      <map iso='A03' to=' '/> <!-- space -->\n"
        + "      <map iso='A04' to='.' longPress='# ! , ? - : &apos; @'/> <!-- (key to right of space) -->\n"
        + "  </keyMap>\n" + "</keyboard>";

    String sampleBad = "<?xml version='1.0' encoding='UTF-8' ?>\n"
        + "<!DOCTYPE keyboard SYSTEM '../dtd/ldmlKeyboard.dtd'>\n"
        + "<keyboard locale='en'>\n"
        + "  <version platform='4.4' number='$Revision: 9576 $'/>\n"
        + "  <names>\n"
        + "      <name value='English United States'/>\n"
        + "  </names>\n"
        + "  <keyMap>\n"
        + "      <map iso='D01' to='q'/>\n"
        + "      <map iso='D02' to='w'/>\n"
        + "      <map iso='D03' to='e' longPress='è é ê ë ē'/>\n"
        + "      <map iso='D04' to='r'/>\n"
        + "      <map iso='D05' to='t'/>\n"
        + "      <map iso='D06' to='y'/>\n"
        + "      <map iso='D07' to='u' longPress='û ü ù ú ū'/>\n"
        + "      <map iso='D08' to='i' longPress='î ï í ī ì'/>\n"
        + "      <map iso='D09' to='o' longPress='ô ö ò ó œ ø ō õ'/>\n"
        + "      <map iso='D10' to='p'/>\n"
        + "      <map iso='C01' to='a' longPress='à á â ä æ ã å ā'/>\n"
        + "      <map iso='C02' to='s' longPress='ß'/>\n"
        + "      <map iso='C03' to='d'/>\n"
        + "      <map iso='C04' to='f'/>\n"
        + "      <map iso='C05' to='g'/>\n"
        + "      <map iso='C06' to='h'/>\n"
        + "      <map iso='C07' to='j'/>\n"
        + "      <map iso='C08' to='k'/>\n"
        + "      <map iso='C09' to='l'/>\n"
        + "      <map iso='B01' to='z'/>\n"
        + "      <map iso='B02' to='x'/>\n"
        + "      <map iso='B03' to='c' longPress='ç'/>\n"
        + "      <map iso='B04' to='v'/>\n"
        + "      <map iso='B05'/>\n"
        + "      <map iso='B06' to='n' longPress='ñ'/>\n"
        + "      <map iso='B07' to='m'/>\n"
        + "      <map iso='A02' to=','/> <!-- (key to left of space) -->\n"
        + "      <map iso='A03' to=' '/> <!-- space -->\n"
        + "      <map iso='A04' to='.' longPress='# ! , ? - : &apos; @'/> <!-- (key to right of space) -->\n"
        + "  </keyMap>\n" + "</keyboard>";

    public void testVerifyKeyboardLoad() {
        Set<Exception> errors = new LinkedHashSet<>();
        for (String keyboardPlatformId : Keyboard.getPlatformIDs()) {
            for (String keyboardId : Keyboard.getKeyboardIDs(keyboardPlatformId)) {
                errors.clear();
                try {
                    Keyboard keyboard = Keyboard.getKeyboard(keyboardPlatformId, keyboardId, errors);
                    String localeId = keyboard.getLocaleId();
                    if (!localeId.equals(keyboardId.replace(".xml", ""))) {
                        errors.add(new IllegalArgumentException("filename and id don't match:\t" + keyboardId + "\t" + localeId));
                    }
                } catch (Exception e) {
                    errors.add(e);
                }
                int errorCount = 0;
                StringBuilder errorString = new StringBuilder();
                for (Exception item : errors) {
                    if (!(item instanceof KeyboardWarningException)) {
                        errorCount++;
                    }
                }
                msg(keyboardPlatformId + "\t" + keyboardId,
                    errors.size() == 0 ? LOG : errorCount == 0 ? WARN : ERR,
                    true, true);
                for (Exception item : errors) {
                    showException(item, "");
                }
            }
        }
    }
}
