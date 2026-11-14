package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ValueStatus;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;

public class FindElements {

    private static final File CLDR_BASE_DIRECTORY = CLDRConfig.getInstance().getCldrBaseDirectory();
    private static final Splitter SPLIT_COMMA = Splitter.on(",").trimResults();
    private static final boolean SHOW = false;
    SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    public static void main(String[] args) {
        String value = "undefined, undefine, auto";
        for (DtdType type : DtdType.STANDARD_SET) {
            DtdData dtdData = DtdData.getInstance(type, CLDR_BASE_DIRECTORY);
            Set<Attribute> results = process(dtdData.ROOT, value);
            for (Attribute attribute : results) {
                System.out.println(type + "\t" + attribute.element.name + "\t" + attribute.name);
            }
        }
    }

    private static Set<Attribute> process(Element element, String attributeValues) {
        Set<Attribute> results = new LinkedHashSet<>();
        process(
                0,
                element,
                ImmutableSet.copyOf(SPLIT_COMMA.splitToList(attributeValues)),
                new HashSet<>(),
                results);
        return results;
    }

    private static void process(
            int level,
            Element element,
            Set<String> attributeValues,
            Set<Element> seenAlready,
            Set<Attribute> results) {
        if (seenAlready.contains(element)) {
            return;
        }
        final Set<Attribute> attributes = element.getAttributes().keySet();
        final Set<Element> children = element.getChildren().keySet();
        if (SHOW) {
            System.out.println(
                    "  ".repeat(level) + element + "\t" + attributes + "\t" + children.size());
        }
        for (Attribute attribute : attributes) {
            if (attribute.isDeprecated() || attribute.getStatus() == AttributeStatus.metadata) {
                continue;
            }

            for (String value : attributeValues) {
                if (!attribute.isDeprecatedValue(value)
                        && attribute.getValueStatus(value) == ValueStatus.valid) {
                    results.add(attribute);
                }
            }
        }
        seenAlready.add(element);
        for (Element child : children) {
            process(level + 1, child, attributeValues, seenAlready, results);
        }
    }
}
