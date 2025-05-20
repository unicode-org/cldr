package org.unicode.cldr.util;

import com.ibm.icu.impl.Utility;
import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class AnnotationUtil {
    private static final String APPS_EMOJI_DIRECTORY =
            CLDRPaths.BASE_DIRECTORY + "/tools/cldr-apps/src/main/webapp/images/emoji";

    private static final String PATH_PREFIX = "//ldml/annotations/annotation";

    private static final Pattern BAD_EMOJI = Pattern.compile("E\\d+(\\.\\d+)?-\\d+");

    /**
     * Does the given value match an annotation code like "E10-840"?
     *
     * @param value like "E10-836" (bad) or "grinning face" (good)
     * @return true if value matches a code (bad), else false (good)
     */
    public static boolean matchesCode(String value) {
        return BAD_EMOJI.matcher(value).matches();
    }

    public static boolean pathIsAnnotation(String path) {
        return path.startsWith(PATH_PREFIX);
    }

    public static String removeEmojiVariationSelector(String emoji) {
        String noVs = emoji.replace(Emoji.EMOJI_VARIANT, "");
        return noVs;
    }

    public static String calculateEmojiImageFilename(String emoji) {
        String noVs = AnnotationUtil.removeEmojiVariationSelector(emoji);
        // example: emoji_1f1e7_1f1ec.png
        String fileName = "emoji_" + Utility.hex(noVs, 4, "_").toLowerCase(Locale.ENGLISH) + ".png";
        return fileName;
    }

    public static boolean isRightFacingFilename(String fileName) {
        return fileName.endsWith("_200d_27a1.png");
    }

    public static String getEnglishAnnotationName(String emoji) {
        String noVs = AnnotationUtil.removeEmojiVariationSelector(emoji);
        final Factory factoryAnnotations =
                SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        final CLDRFile enAnnotations = factoryAnnotations.make("en", false);
        final String name =
                enAnnotations.getStringValue(
                        "//ldml/annotations/annotation[@cp=\"" + noVs + "\"][@type=\"tts\"]");
        return name;
    }

    public static File getEmojiImageFile(String emoji) {
        String noVs = AnnotationUtil.removeEmojiVariationSelector(emoji);
        String fileName = AnnotationUtil.calculateEmojiImageFilename(noVs);
        File file = new File(APPS_EMOJI_DIRECTORY, fileName);
        return file;
    }
}
