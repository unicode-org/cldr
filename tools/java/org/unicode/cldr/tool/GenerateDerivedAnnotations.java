package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.text.UnicodeSet;

public class GenerateDerivedAnnotations {
    // Use EmojiData.getDerivableNames() to update this for each version of Unicode.

    static final UnicodeSet SKIP = new UnicodeSet()
        .add(Annotations.ENGLISH_MARKER)
        .add(Annotations.BAD_MARKER)
        .add(Annotations.MISSING_MARKER)
        .freeze();

    public static void main(String[] args) throws IOException {
        Joiner BAR = Joiner.on(" | ");
        AnnotationSet enAnnotations = Annotations.getDataSet("en");
        CLDRFile english = CLDRConfig.getInstance().getEnglish();

        UnicodeSet derivables = new UnicodeSet(Emoji.getAllRgiNoES()).removeAll(enAnnotations.keySet()).freeze();
        Map<String, UnicodeSet> localeToFailures = new LinkedHashMap<>();
        Set<String> locales = ImmutableSortedSet.copyOf(Annotations.getAvailable());
        
        for (String locale : locales) {
            if ("root".equals(locale)) {
                continue;
            }
            UnicodeSet failures = new UnicodeSet();
            localeToFailures.put(locale, failures);
            
            AnnotationSet annotations;
            try {
                annotations = Annotations.getDataSet(locale);
            } catch (Exception e) {
                System.out.println("Can't create annotations for: " + locale);
                continue;
            }
            CLDRFile target = new CLDRFile(new SimpleXMLSource(locale));
            target.addComment("//ldml", "Derived short names and annotations, using GenerateDerivedAnnotations.java. See warnings in /annotations/ file.",
                CommentType.PREBLOCK);
            for (String derivable : derivables) {
                String shortName = null;
                try {
                    shortName = annotations.getShortName(derivable);
                } catch (Exception e) {
                }
                if (shortName == null || SKIP.containsSome(shortName)) {
                    failures.add(derivable);
                    continue; // missing
                }
                Set<String> keywords = annotations.getKeywordsMinus(derivable);
                String path = "//ldml/annotations/annotation[@cp=\"" + derivable + "\"]";
                if (!keywords.isEmpty()) {
                    Set<String> keywordsFixed = new LinkedHashSet<>();
                    for (String keyword : keywords) {
                        if (!SKIP.containsSome(keyword)) {
                            keywordsFixed.add(keyword);
                        }
                    }
                    if (!keywordsFixed.isEmpty()) {
                        target.add(path, BAR.join(keywordsFixed));
                    }
                }
                target.add(path + "[@type=\"tts\"]", shortName);
            }
            failures.freeze();
            if (!target.iterator().hasNext()) {
                System.out.println(locale + " is empty!");
            } else if (!failures.isEmpty()) {
                System.out.println("Failures\t" + locale 
                    + "\t" + english.getName(locale)
                    + "\t" + failures.size() 
                    + "\t" + failures.toPattern(false));
            }
            try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "annotationsDerived", locale + ".xml")) {
                target.write(pw);
            }
        }
    }
}
