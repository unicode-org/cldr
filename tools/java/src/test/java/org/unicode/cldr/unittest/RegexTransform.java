package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;

public class RegexTransform implements Transform<String, String> {

    public enum Processing {
        FIRST_MATCH, ONE_PASS, RECURSIVE
    }

    private final Processing processing;
    private final List<Row.R2<Matcher, String>> entries = new ArrayList<Row.R2<Matcher, String>>();

    public String transform(String source) {
        main: while (true) {
            for (R2<Matcher, String> entry : entries) {
                Matcher matcher = entry.get0();
                if (matcher.reset(source).find()) {
                    String replacement = entry.get1();
                    source = matcher.replaceAll(replacement);
                    switch (processing) {
                    case RECURSIVE:
                        continue main;
                    case FIRST_MATCH:
                        break main;
                    case ONE_PASS: // fall through and continue;
                    }
                }
            }
            break;
        }
        return source;
    }

    public RegexTransform(Processing processing) {
        this.processing = processing;
    }

    public RegexTransform add(String pattern, String replacement) {
        entries.add(Row.of(
            Pattern.compile(pattern, Pattern.COMMENTS).matcher(""),
            replacement));
        return this;
    }
}