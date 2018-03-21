package org.unicode.cldr.unittest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Row.R2;

public class TestSubdivisions extends TestFmwkPlus {
    private static final String SUB_DIR = CLDRPaths.COMMON_DIRECTORY + "subdivisions/";
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestSubdivisions().run(args);
    }

    public void TestContainment() {
        Set<String> containers = SDI.getContainersForSubdivisions();
        assertNotNull("subdivision containers", containers);
        Set<String> states = SDI.getContainedSubdivisions("US");

        assertRelation("US contains CA", true, states, TestFmwkPlus.CONTAINS, "usca");

        /*
         * <subgroup type="BE" contains="WAL BRU VLG"/>
         * <subgroup type="BE" subtype="WAL" contains="WLX WNA WHT WBR WLG"/>
         * <subgroup type="BE" subtype="VLG" contains="VBR VWV VAN VLI VOV"/>
         */
        assertEquals("BE",
            new HashSet<String>(Arrays.asList("bewal", "bebru", "bevlg")),
            SDI.getContainedSubdivisions("BE"));
        assertEquals("BE",
            new HashSet<String>(Arrays.asList("bewlx", "bewna", "bewht", "bewbr", "bewlg")),
            SDI.getContainedSubdivisions("bewal"));
    }

    public void TestNames() {
        final Map<String, R2<List<String>, String>> subdivisionAliases = SDI.getLocaleAliasInfo().get("subdivision");
        // <subdivisionAlias type="CN-71" replacement="TW" reason="overlong"/>
        //        R2<List<String>, String> region = subdivisionAliases.get(value);
        final Validity VALIDITY = Validity.getInstance();
        Map<String, Status> deprecated = VALIDITY.getCodeToStatus(LstrType.subdivision);

        for (String file : new File(SUB_DIR).list()) {
            if (!file.endsWith(".xml")) {
                continue;
            }
            checkSubdivisionFile(file, subdivisionAliases, deprecated);
        }
    }

    private void checkSubdivisionFile(String file,
        final Map<String, R2<List<String>, String>> subdivisionAliases,
        Map<String, Status> deprecated) {
        String lang = file.replace(".xml", "");

        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(SUB_DIR + file, data, true);
        logln(file + "\t" + data.size());
        ChainedMap.M4<String, String, String, Status> countryToNameToSubdivisions = ChainedMap.of(
            new TreeMap<String, Object>(), new TreeMap<String, Object>(), new TreeMap<String, Object>(), Status.class);

        for (Pair<String, String> entry : data) {
            // <subdivision type="AD-02">Canillo</subdivision>
            XPathParts parts = XPathParts.getFrozenInstance(entry.getFirst());
            if (!parts.getElement(-1).equals("subdivision")) {
                continue;
            }
            String name = entry.getSecond();
            final String subdivision = parts.getAttributeValue(-1, "type");
            String country = subdivision.substring(0, 2);

            // if there is an alias, we're ok, don't bother with it
            R2<List<String>, String> subdivisionAlias = subdivisionAliases.get(subdivision);
            if (subdivisionAlias != null) {
                // String countryName = CLDRConfig.getInstance().getEnglish().getName(CLDRFile.TERRITORY_NAME, country);
                // assertEquals("country " + country + " = subdivision " + subdivision, countryName, value);
                continue;
            }
            countryToNameToSubdivisions.put(country, name, subdivision, deprecated.get(subdivision));
        }
        // now look for uniqueness
        LinkedHashSet<String> problemSet = new LinkedHashSet<>();
        for (Entry<String, Map<String, Map<String, Status>>> entry1 : countryToNameToSubdivisions) {
            String country = entry1.getKey().toUpperCase(Locale.ROOT);
            for (Entry<String, Map<String, Status>> entry2 : entry1.getValue().entrySet()) {
                String name = entry2.getKey();
                Map<String, Status> subdivisionMap = entry2.getValue();
                if (subdivisionMap.size() == 1) {
                    continue;
                }
                logln(lang + "," + country + "Name «" + name + "» for " + subdivisionMap.keySet());
                // we have multiple names.
                // remove the deprecated ones, but generate aliases
                problemSet.clear();
                for (Iterator<Entry<String, Status>> it = subdivisionMap.entrySet().iterator(); it.hasNext();) {
                    Entry<String, Status> entry = it.next();
                    if (entry.getValue() != Status.regular) { // if not deprecated
                        problemSet.add(entry.getKey());
                        it.remove();
                    }
                }
                if (problemSet.size() < 2) {
                    continue;
                }
                // warn about collisions
                errln(lang + "," + country + "Name collision for «" + name + "» in " + problemSet);

                // show the possible aliases to add
                String first = problemSet.iterator().next();
                for (String deprecatedItem : subdivisionMap.keySet()) {
                    warnln(lang + "," + country + "Consider adding: "
                        + "<subdivisionAlias type=\"" + deprecatedItem
                        + "\" replacement=\"" + first
                        + "\" reason=\"deprecated\"/>");
                }
            }
        }
    }
}
