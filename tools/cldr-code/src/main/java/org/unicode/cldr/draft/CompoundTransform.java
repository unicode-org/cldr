package org.unicode.cldr.draft;

import com.ibm.icu.text.StringTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable class that does a compound transform
 *
 * @author markdavis
 */
public class CompoundTransform implements StringTransform {
    private final List<StringTransform> transforms;

    public CompoundTransform(List<StringTransform> transforms) {
        this.transforms = new ArrayList<>(transforms);
    }

    @Override
    public String transform(String source) {
        for (int i = 0; i < transforms.size(); ++i) {
            source = transforms.get(i).transform(source);
        }
        return source;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < transforms.size(); ++i) {
            if (i != 0) {
                result.append(":: NULL ;\n");
            }
            result.append(transforms.get(i).toString());
        }
        return result.toString();
    }
}
