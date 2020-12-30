package org.unicode.cldr.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.json.LdmlConvertRules.SplittableAttributeSpec;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.MatchValue;
import org.unicode.cldr.util.Pair;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

class LdmlConvertRulesTest {
    final File cldrDir = CLDRConfig.getInstance().getCldrBaseDirectory();
    final DtdData dtds[] = {
        DtdData.getInstance(DtdType.supplementalData, cldrDir),
        DtdData.getInstance(DtdType.ldml, cldrDir)
    };

    @Test
    void testSplittableAttributes() {
        // collect JSON list
        Set<Pair<String,String>> jsonSplittableAttrs = new TreeSet<>();
        for(final SplittableAttributeSpec e : LdmlConvertRules.getSplittableAttrs()) {
            if(e.element.equals("measurementSystem-category-temperature")) continue; // skip this deprecated item
            jsonSplittableAttrs.add(Pair.of(e.element, e.attribute));
        }

        LdmlConvertRules.ATTR_AS_VALUE_SET.forEach(s -> {
                final String triple[] = s.split(":");
                jsonSplittableAttrs.add(Pair.of(triple[1],triple[2]));
            });

        // collect DTD list
        Set<Pair<String,String>> dtdSplittableAttrs = new TreeSet<>();
        for(final DtdData dtd: dtds) {
            for(final Element element : dtd.getElements()) {
                if(element.getAttributes() == null) continue; // ?
                for(final Entry<Attribute, Integer> q : element.getAttributes().entrySet()) {
                    Attribute attr = q.getKey();
                    if(attr.matchValue != null && attr.matchValue instanceof MatchValue.SetMatchValue) {
                        boolean isSpacesepArray = LdmlConvertRules.CHILD_VALUE_IS_SPACESEP_ARRAY.contains(element.name);
                        boolean childIsSpacesepArray = LdmlConvertRules.VALUE_IS_SPACESEP_ARRAY.matcher(element.name).matches();
                        boolean attrValueIsArraySet = LdmlConvertRules.ATTRVALUE_AS_ARRAY_SET.contains(attr.name);

                        if(isSpacesepArray ||
                           childIsSpacesepArray ||
                           attrValueIsArraySet) {
                            jsonSplittableAttrs.add(Pair.of(element.name, attr.name));
                        }
                        dtdSplittableAttrs.add(Pair.of(element.name, attr.name));
                    }
                }
            }
        }


        // ** Add some exceptions

        // Handled manually
        jsonSplittableAttrs.add(Pair.of("defaultContent", "locales"));

        // handled as calendarPreferenceData
        jsonSplittableAttrs.add(Pair.of("calendarPreference", "ordering"));

        // These aren't a set, in practice.
        dtdSplittableAttrs.remove(Pair.of("grammaticalFeatures", "targets"));
        dtdSplittableAttrs.remove(Pair.of("territoryAlias", "replacement"));
        dtdSplittableAttrs.remove(Pair.of("subdivisionAlias", "replacement"));
        dtdSplittableAttrs.remove(Pair.of("territoryAlias", "type"));
        dtdSplittableAttrs.remove(Pair.of("deriveCompound", "feature"));
        dtdSplittableAttrs.remove(Pair.of("deriveComponent", "feature"));
        dtdSplittableAttrs.remove(Pair.of("deriveCompound", "structure"));
        dtdSplittableAttrs.remove(Pair.of("deriveComponent", "structure"));
        dtdSplittableAttrs.remove(Pair.of("deriveCompound", "value"));

        //Keep these as not-a-set for compatibility
        jsonSplittableAttrs.add(Pair.of("paradigmLocales", "locales"));


        SetView<Pair<String, String>> onlyInDtd = Sets.difference(dtdSplittableAttrs, jsonSplittableAttrs);


//        SetView<Pair<String, String>> onlyInJson = Sets.difference(jsonSplittableAttrs, dtdSplittableAttrs);
//        if(!onlyInJson.isEmpty()) {
//            System.err.println("Only in JSON, missing from DTD (!): " + onlyInJson); // just noise
//        }

        assertEquals(Collections.emptySet(), onlyInDtd, "set items missing from JSON configuration");

    }

}
