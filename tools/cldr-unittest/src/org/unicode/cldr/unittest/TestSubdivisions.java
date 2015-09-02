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
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
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
        
        // <subgroup type="NZ" contains="S N CIT"/>
        assertEquals("NZ", 
            new HashSet<String>(Arrays.asList("NZ-S", "NZ-N", "NZ-CIT")), 
            SDI.getContainedSubdivisions("NZ"));
        // <subgroup type="NZ" subtype="S" contains="TAS MBH STL OTA CAN NSN WTC"/>
        assertEquals("NZ", 
            new HashSet<String>(Arrays.asList("NZ-TAS", "NZ-MBH", "NZ-STL", "NZ-OTA", "NZ-CAN", "NZ-NSN", "NZ-WTC")), 
            SDI.getContainedSubdivisions("NZ-S"));
    }
    
    public void TestEnglishNames() {
        final Map<String, R2<List<String>, String>> subdivisionAliases = SDI.getLocaleAliasInfo().get("subdivision");

        // <subdivision type="AL-DI">Dibër</subdivision>   <!-- in AL-09 : Dibër -->

        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + "subdivisions/en.xml", data , true);
        M3<String, String, String> countryToNameToSubdivision = ChainedMap.of(new TreeMap<String,Object>(), new TreeMap<String,Object>(), String.class);
        for (Pair<String, String> entry : data) {
            XPathParts parts = XPathParts.getFrozenInstance(entry.getFirst());
            if (!parts.getElement(-1).equals("subdivision")) {
                continue;
            }
            String value = entry.getSecond();
            final String subdivision = parts.getAttributeValue(-1, "type");
            R2<List<String>, String> subdivisionAlias = subdivisionAliases.get(subdivision);
            if (subdivisionAlias != null) {
                String country = subdivisionAlias.get0().get(0);
                String countryName = CLDRConfig.getInstance().getEnglish().getName(CLDRFile.TERRITORY_NAME, country);
                assertEquals("country " + country
                    + " = subdivision " + subdivision, countryName, value);
            }
            String country = subdivision.substring(0,2);
            String old = countryToNameToSubdivision.get(country, value);
            if (old == null) {
                countryToNameToSubdivision.put(country, value, subdivision);
            } else {
                Set<String> oldContained = SDI.getContainedSubdivisions(old);
                Set<String> newContained = SDI.getContainedSubdivisions(subdivision);

                errln("Collision between " + old + " and " + subdivision + ": «" + value + "»"
                    + (oldContained == null ? "" : !oldContained.contains(subdivision) ? "" : "\t" + old + " ∋ " + subdivision)
                    + (newContained == null ? "" : !newContained.contains(old) ? "" : "\t" + subdivision + " ∋ " + old)
                    );
            }
        }
        // <subdivisionAlias type="CN-71" replacement="TW" reason="overlong"/>
//        R2<List<String>, String> region = subdivisionAliases.get(value);

    }
}
