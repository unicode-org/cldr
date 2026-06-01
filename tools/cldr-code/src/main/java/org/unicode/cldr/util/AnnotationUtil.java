package org.unicode.cldr.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.icu.impl.Utility;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class AnnotationUtil {
    static final Logger logger = Logger.getLogger(AnnotationUtil.class.getSimpleName());

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
        String fileName = "emoji_" + Utility.hex(noVs, 4, "_").toLowerCase(Locale.ROOT) + ".png";
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
        String fileName = AnnotationUtil.calculateEmojiImageFilename(emoji);
        File file = new File(APPS_EMOJI_DIRECTORY, fileName);
        return file;
    }

    public static String getEmojiFromXPath(final String xpath) {
        XPathParts emoji = XPathParts.getFrozenInstance(xpath);
        String cp = emoji.getAttributeValue(-1, "cp");
        return cp;
    }

    // cache of file presence
    static final LoadingCache<String, Boolean> doWeHaveTheEmoji =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<String, Boolean>() {
                                @Override
                                public Boolean load(@Nonnull final String emoji) throws Exception {
                                    return getEmojiImageFile(emoji).exists();
                                }
                            });

    /**
     * @return true if getEmojiImageFile is expected to return an existing file
     */
    public static final boolean haveEmojiImageFile(String emoji) {
        try {
            return doWeHaveTheEmoji.get(emoji);
        } catch (ExecutionException e) {
            logger.log(
                    java.util.logging.Level.SEVERE,
                    getEnglishAnnotationName(emoji) + " - " + getEmojiImageFile(emoji).getName(),
                    e);
            return false;
        }
    }
}
