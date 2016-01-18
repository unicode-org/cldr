package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Row.R2;

public class TestSubdivisions extends TestFmwkPlus {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestSubdivisions().run(args);
    }

    public void TestContainment() {
        Set<String> containers = SDI.getContainersForSubdivisions();
        assertNotNull("subdivision containers", containers);
        Set<String> states = SDI.getContainedSubdivisions("US");
        
        assertRelation("US contains CA", true, states, TestFmwkPlus.CONTAINS, "US-CA");

        /*
         * <subgroup type="BE" contains="WAL BRU VLG"/>
         * <subgroup type="BE" subtype="WAL" contains="WLX WNA WHT WBR WLG"/>
         * <subgroup type="BE" subtype="VLG" contains="VBR VWV VAN VLI VOV"/>
         */
        assertEquals("BE",
            new HashSet<String>(Arrays.asList("BE-WAL", "BE-BRU", "BE-VLG")),
            SDI.getContainedSubdivisions("BE"));
        assertEquals("BE",
            new HashSet<String>(Arrays.asList("BE-WLX", "BE-WNA", "BE-WHT", "BE-WBR", "BE-WLG")),
            SDI.getContainedSubdivisions("BE-WAL"));
    }


    public void TestEnglishNames() {
        final Map<String, R2<List<String>, String>> subdivisionAliases = SDI.getLocaleAliasInfo().get("subdivision");
        final Validity VALIDITY = Validity.getInstance();
        Set<String> deprecated = VALIDITY.getData().get(LstrType.subdivision).get(Status.deprecated);

        // <subdivision type="AL-DI">Dibër</subdivision>   <!-- in AL-09 : Dibër -->

        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + "subdivisions/en.xml", data, true);
        M3<String, String, String> countryToNameToSubdivision = ChainedMap.of(new TreeMap<String, Object>(), new TreeMap<String, Object>(), String.class);
        for (Pair<String, String> entry : data) {
            XPathParts parts = XPathParts.getFrozenInstance(entry.getFirst());
            if (!parts.getElement(-1).equals("subdivision")) {
                continue;
            }
            String value = entry.getSecond();
            final String subdivision = parts.getAttributeValue(-1, "type");
            if (deprecated.contains(subdivision)) {
                continue; // skip deprecated names
            }
            R2<List<String>, String> subdivisionAlias = subdivisionAliases.get(subdivision);
            if (subdivisionAlias != null) {
                String country = subdivisionAlias.get0().get(0);
                String countryName = CLDRConfig.getInstance().getEnglish().getName(CLDRFile.TERRITORY_NAME, country);
                assertEquals("country " + country
                    + " = subdivision " + subdivision, countryName, value);
            }
            String country = subdivision.substring(0, 2);
            String old = countryToNameToSubdivision.get(country, value);
            if (old == null) {
                countryToNameToSubdivision.put(country, value, subdivision);
            } else {
                Set<String> oldContained = SDI.getContainedSubdivisions(old);
                Set<String> newContained = SDI.getContainedSubdivisions(subdivision);

                errln("Collision between " + old + " and " + subdivision + ": «" + value + "»"
                    + (oldContained == null ? "" : !oldContained.contains(subdivision) ? "" : "\t" + old + " ∋ " + subdivision)
                    + (newContained == null ? "" : !newContained.contains(old) ? "" : "\t" + subdivision + " ∋ " + old));
            }
        }
        // <subdivisionAlias type="CN-71" replacement="TW" reason="overlong"/>
//        R2<List<String>, String> region = subdivisionAliases.get(value);

    }
}
