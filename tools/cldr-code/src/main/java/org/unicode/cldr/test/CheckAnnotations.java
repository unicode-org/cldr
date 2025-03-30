package org.unicode.cldr.test;

// import java.util.Arrays;
import java.util.HashMap;
// import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XPathParts;

public class CheckAnnotations extends CheckCLDR {
    private static final Pattern ANNOTATION_PATH = Pattern.compile("//ldml/annotations/.*");

    // Skip tts test for these locale/cp combinations unti
    // https://unicode-org.atlassian.net/browse/CLDR-18329 is fixed
    private static final Map<String, Set<String>> entriesLackingTts = new HashMap<>();

    // Temporarily comment out this block so we do get errors in ST/ConsoleCheck for these cases;
    // either they will be fixed in ST or we will need to remove keyword entries with no tts entry;
    // see https://unicode-org.atlassian.net/browse/CLDR-18329.
    // static {
    //    //                    locID                                            cp= values to skip
    //    entriesLackingTts.put("ak", new HashSet<>(Arrays.asList(new String[] {"🪀"})));
    //    entriesLackingTts.put("br", new HashSet<>(Arrays.asList(new String[] {"'"})));
    //    entriesLackingTts.put("ccp", new HashSet<>(Arrays.asList(new String[] {"🥪"})));
    //    entriesLackingTts.put(
    //            "ha",
    //            new HashSet<>(Arrays.asList(new String[] {"👨‍🦯", "👨‍🦼", "👩‍🦯", "👩‍🦼"})));
    //    entriesLackingTts.put(
    //            "kab",
    //            new HashSet<>(
    //                    Arrays.asList(
    //                            new String[] {"⚙", "🀄", "🌤", "🎀", "💊", "💲", "🖲", "🚀"})));
    //    entriesLackingTts.put("om", null); // all entries lack tts version
    //    entriesLackingTts.put("qu", new HashSet<>(Arrays.asList(new String[] {"✒"})));
    //    entriesLackingTts.put(
    //            "sat",
    //            new HashSet<>(
    //                    Arrays.asList(
    //                            new String[] {
    //                                "🍐", "🍑", "🍒", "🍓", "🐠", "🐡", "🐨", "🐳", "🐹", "👞",
    //                                "💥", "🤽‍♂", "🥐", "🥸", "🦈"
    //                            })));
    // };

    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (!ANNOTATION_PATH.matcher(path).matches()) return this;
        if (!accept(result)) return this;

        // check whether annotation is empty
        if (value == null || value.isEmpty()) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.nullOrEmptyValue)
                            .setMessage("The annotation may not be empty"));
            return this;
        }
        CLDRFile file = getCldrFileToCheck();
        final String ecode = hasAnnotationECode(value);

        // check whether annotation value is E-code
        if (ecode != null && file.isNotRoot(path)) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalAnnotationCode)
                            .setMessage(
                                    "The annotation must be a translation and not contain the E… code from root, or anything like it. ({0})",
                                    ecode));
        }

        // check whether name (tts) entry corresponding to keyword entry is missing in top-level
        // locales
        String localeID = file.getLocaleID();
        String parent = LocaleIDParser.getParent(localeID);
        boolean isTopLevel = (parent == null) || parent.equals("root");
        if (!path.contains("[@type=\"tts\"]") && isTopLevel) {
            // this is a keyword path in top-level locale, check that corresponding tts path is
            // present
            XPathParts parts = XPathParts.getFrozenInstance(path).cloneAsThawed();
            String ttsPath = parts.addAttribute("type", "tts").toString();
            String ttsValue = file.getWinningValue(ttsPath);
            boolean ttsMissing =
                    ttsValue == null
                            || ttsValue.isEmpty()
                            || (file.isNotRoot(path)
                                    && ecode == null
                                    && hasAnnotationECode(ttsValue) != null);
            if (ttsMissing && entriesLackingTts.keySet().contains(localeID)) {
                String cpValue = parts.findAttributeValue("annotation", "cp");
                Set<String> cpEntriesLackingTts = entriesLackingTts.get(localeID);
                if (cpEntriesLackingTts == null || cpEntriesLackingTts.contains(cpValue)) {
                    ttsMissing = !ttsMissing; // skip the error report, already have CLDR-18329
                }
            }
            if (ttsMissing) {
                final CLDRConfig.Environment env = CLDRConfig.getInstance().getEnvironment();
                final boolean onSurveyTool =
                        (env == CLDRConfig.Environment.PRODUCTION
                                || env == CLDRConfig.Environment.SMOKETEST);
                final Type mainType =
                        onSurveyTool
                                ? CheckStatus.errorType // this is an error in ST
                                : CheckStatus
                                        .warningType; // but a warning elsewhere (e.g. UNITTEST) so
                // we can commit
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(mainType)
                                .setSubtype(Subtype.ttsAnnotationMissing)
                                .setMessage(
                                        "Have keywords but missing the corresponding name (tts) entry; error vs warning depends on environment {0}",
                                        env));
            }
        }

        return this;
    }

    static String hasAnnotationECode(String value) {
        Matcher m = HAS_ANNOTATION_ECODE.matcher(value);
        if (m.find()) {
            return m.group();
        } else {
            return null;
        }
    }

    static final Pattern HAS_ANNOTATION_ECODE =
            PatternCache.get("E[0-9]{1,3}(?:[-\u2013:—.][0-9]{1,3}){0,2}");
}
