package org.unicode.cldr.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ibm.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;

public class CldrFileDataSourceTest extends TestFmwk {
    // The config knows if it's being run as part of a test. This is NOT immutable (but this test
    // won't mutate it anyway).
    private static final CLDRConfig testInfo = CLDRConfig.getInstance();

    public void TestRootPathsAndValues() {
        CldrFileDataSource src = new CldrFileDataSource(testInfo.getRoot());
        Map<CldrPath, CldrValue> arbitraryOrderMap = new LinkedHashMap<>();
        src.accept(ARBITRARY, value -> arbitraryOrderMap.put(value.getPath(), value));

        Map<CldrPath, CldrValue> dtdOrderMap = new LinkedHashMap<>();
        src.accept(DTD, value -> dtdOrderMap.put(value.getPath(), value));

        // Map equality doesn't care about key order.
        assertEquals("maps are equal", arbitraryOrderMap, dtdOrderMap);

        // This could give a false negative if the DTD order ever miraculously matched hash order,
        // and if it ever did, it should be sufficient to just change any aspect of the test data.
        List<CldrPath> arbitraryKeyList = new ArrayList<>(arbitraryOrderMap.keySet());
        List<CldrPath> dtdKeyList = new ArrayList<>(dtdOrderMap.keySet());
        assertNotEquals("dtd order differs", arbitraryKeyList, dtdKeyList);

        arbitraryKeyList.sort(Comparator.naturalOrder());
        assertEquals("sorted order same", arbitraryKeyList, dtdKeyList);
    }

    public void TestUnresolvedVsResolved() {
        CldrFileDataSource unresolved = new CldrFileDataSource(testInfo.getCLDRFile("en_GB", false));
        Map<CldrPath, CldrValue> unresolvedMap = new LinkedHashMap<>();
        unresolved.accept(DTD, value -> unresolvedMap.put(value.getPath(), value));

        CldrFileDataSource resolved = new CldrFileDataSource(testInfo.getCLDRFile("en_GB", true));
        Map<CldrPath, CldrValue> resolvedMap = new LinkedHashMap<>();
        resolved.accept(DTD, value -> resolvedMap.put(value.getPath(), value));

        assertTrue("unresolved is subset", unresolvedMap.size() < resolvedMap.size());
        Set<CldrPath> onlyUnresolved = Sets.difference(unresolvedMap.keySet(), resolvedMap.keySet());
        assertEquals("unresolved is subset", ImmutableSet.of(), onlyUnresolved);
    }

    public void TestNoDtdVersionPath() {
        CldrFileDataSource unresolved = new CldrFileDataSource(testInfo.getCLDRFile("en_GB", false));
        unresolved.accept(DTD, v ->
            assertFalse("is DTD version string",
                v.getPath().toString().startsWith("//ldml/version")));
    }
}
