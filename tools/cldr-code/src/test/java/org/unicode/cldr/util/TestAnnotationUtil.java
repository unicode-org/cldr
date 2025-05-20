import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Emoji;

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
}
