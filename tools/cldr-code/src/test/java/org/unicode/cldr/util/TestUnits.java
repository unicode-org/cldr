package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TestUnits {
    @Test
    public void TestEnglishUnitConsistency() {
        testUnitConsistency("en");
    }

    private void testUnitConsistency(String loc) {
        final CLDRFile file = CLDRConfig.getInstance().getCLDRFile(loc, false); // non resolved
        final Map<String, Set<String>> unitLengths = new HashMap<>();
        // invert the length->unit structure
        for (final Iterator<String> i = file.iterator("//ldml/units/"); i.hasNext(); ) {
            final String xpath = i.next();
            // Example: //ldml/units/unitLength[@type="long"]/unit[@type="graphics-dot"]/displayName
            XPathParts x = XPathParts.getFrozenInstance(xpath);
            final String unit = x.findAttributeValue("unit", "type");
            final String length = x.findAttributeValue("unitLength", "type");
            if (unit != null && length != null) {
                unitLengths.computeIfAbsent(unit, k -> new TreeSet<String>()).add(length);
                System.out.println(unit + ", " + length);
            }
        }
        // Now. all lengths
        final Set<String> allLengths = new TreeSet<String>();
        for (final Set<String> s : unitLengths.values()) {
            for (final String l : s) {
                allLengths.add(l);
            }
        }
        // Finally, find all units that don't have all lengths
        final Map<String, String> missingLengths = new HashMap<>();
        for (final String unit : unitLengths.keySet()) {
            final Set<String> missingHere = new TreeSet<>(allLengths);
            missingHere.removeAll(unitLengths.get(unit));
            if (!missingHere.isEmpty()) {
                missingLengths.put(unit, String.join(", ", missingHere.toArray(new String[0])));
            }
        }
        assertTrue(
                missingLengths.isEmpty(),
                () ->
                        "Inconsistent units, missing from "
                                + loc
                                + ": "
                                + missingLengths.entrySet().stream()
                                        .map(e -> String.format("%s(%s)", e.getKey(), e.getValue()))
                                        .collect(Collectors.joining(" ")));
    }
}
