package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import org.junit.jupiter.api.Test;

public class TestAnnotationUtil {

    @Test
    public void testEmojiImages() {
        assumeFalse(CLDRPaths.ANNOTATIONS_DIRECTORY.contains("cldr-staging/production/"));
        // don't bother checking production for this: the images are only in main, not
        // production

        for (String emoji : Emoji.getNonConstructed()) {
            File file = AnnotationUtil.getEmojiImageFile(emoji);
            if (AnnotationUtil.isRightFacingFilename(file.getName())) {
                continue;
            }
            assertTrue(
                    file.exists(),
                    () ->
                            file.getName()
                                    + " missing; "
                                    + AnnotationUtil.getEnglishAnnotationName(emoji));
        }
    }

    @Test
    public void testEmojiImagesVsPaths() {
        for (final String xpath :
                CLDRConfig.getInstance().getAnnotationsFactory().make("en", false).fullIterable()) {
            if (AnnotationUtil.pathIsAnnotation(xpath)) {
                final String cp = AnnotationUtil.getEmojiFromXPath(xpath);
                final File file = AnnotationUtil.getEmojiImageFile(cp);
                final boolean expectedExists = AnnotationUtil.haveEmojiImageFile(cp);
                final boolean actuallyExists = file.exists();
                assertEquals(
                        expectedExists,
                        actuallyExists,
                        () ->
                                "cache (expected) vs actual (disk) differ for "
                                        + file.getAbsolutePath()
                                        + " for "
                                        + xpath);
            }
        }
    }

    @Test
    public void testRemoveVS() {
        final String CP = Character.toString(0x1F557);
        final String CP2 =
                Character.toString(0x2764)
                        + Character.toString(0xFE0F)
                        + Character.toString(0x200D)
                        + Character.toString(0x1F525); // "❤️‍🔥"
        final String CP2_NOVS =
                Character.toString(0x2764)
                        + Character.toString(0x200D)
                        + Character.toString(0x1F525); // "❤️‍🔥"
        assertEquals(CP, AnnotationUtil.removeEmojiVariationSelector(CP + Emoji.EMOJI_VARIANT));
        assertEquals(CP2_NOVS, AnnotationUtil.removeEmojiVariationSelector(CP2));
    }
}
