package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.NameType;
import org.unicode.cldr.util.NestedMap;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.ImmutableMap3;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.Splitters;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

public class TestNestedMaps extends TestFmwk {
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    public static void main(String[] args) {
        new TestNestedMaps().run(args);
    }

    public void testBasic() {
        List<String> key1s = List.of("b", "a", "b");
        List<Double> key2s = List.of(993.2d, 5.3d, 99d);
        Boolean v1 = true;
        List<String> actualParts = List.of("[b, 993.2, true]", "[a, 5.3, true]", "[b, 99.0, true]");
        // tree order is 1,2,0

        Map<Supplier<Map<Object, Object>>, List<Integer>> supplierAndOrders =
                ImmutableMap.of(
                        TreeMap::new,
                        List.of(1, 2, 0),
                        LinkedHashMap::new,
                        List.of(0, 2, 1), // keys are ordered together
                        HashMap::new,
                        List.of(1, 0, 2),
                        ConcurrentHashMap::new,
                        List.of(1, 0, 2));
        for (Entry<Supplier<Map<Object, Object>>, List<Integer>> supplierAndOrder :
                supplierAndOrders.entrySet()) {
            Supplier<Map<Object, Object>> supplier = supplierAndOrder.getKey();
            List<Integer> order = supplierAndOrder.getValue();

            Map2<String, Double, Boolean> nm2 = Map2.create(supplier);

            for (int i = 0; i < key1s.size(); ++i) {
                String key1 = key1s.get(i);
                Double key2 = key2s.get(i);
                String stringified = join(i, actualParts, order);
                nm2.put(key1, key2, v1);
                Boolean result = nm2.get(key1, key2);
                assertEquals(String.valueOf(i), v1, result);

                String actual = Joiners.VBAR.join(nm2.stream().collect(Collectors.toList()));
                assertEquals(String.valueOf(i) + " " + order, stringified, actual);
            }

            ImmutableMap2<String, Double, Boolean> nm2_immutable = nm2.createImmutable();

            nm2.equals(nm2_immutable);
            if (!assertEquals("immutable equals?", nm2, nm2_immutable)) {}
        }
    }

    private String join(int i, List<String> actualParts, List<Integer> order) {
        List<String> result = new ArrayList<>();
        for (int j : order) {
            if (j > i) continue;
            result.add(actualParts.get(j));
        }
        return Joiners.VBAR.join(result);
    }

    public void testWithBcp47() {
        NestedMap.Map3<String, String, String, String> fullMulti =
                NestedMap.Map3.create(TreeMap::new);
        Map<R2<String, String>, Map<String, String>> bcp47Full = SDI.getBcp47Full();
        bcp47Full.entrySet().stream()
                .forEach(
                        x ->
                                x.getValue().entrySet().stream()
                                        .forEach(
                                                y ->
                                                        fullMulti.put(
                                                                x.getKey().get0(),
                                                                x.getKey().get1(),
                                                                y.getKey(),
                                                                y.getValue())));
        ImmutableMap3<String, String, String, String> im = fullMulti.createImmutable();
        im.stream().forEach(System.out::println);
    }

    public void testWithValidity() {
        NestedMap.Map3<LstrType, String, Reason, List<String>> validity2 =
                NestedMap.Map3.create(LinkedHashMap::new);
        Map<String, Map<String, R2<List<String>, String>>> localeAliasInfo =
                SDI.getLocaleAliasInfo();
        localeAliasInfo.entrySet().stream()
                .forEach(
                        x ->
                                x.getValue().entrySet().stream()
                                        .forEach(
                                                y ->
                                                        validity2.put(
                                                                get(x.getKey()),
                                                                y.getKey(),
                                                                Reason.valueOf(y.getValue().get1()),
                                                                y.getValue().get0())));
        ImmutableMap3<LstrType, String, Reason, List<String>> im = validity2.createImmutable();
        im.stream().forEach(System.out::println);
    }

    enum Reason {
        overlong,
        deprecated,
        macrolanguage,
        bibliographic,
        legacy
    }

    static LstrType get(String item) {
        switch (item) {
            case "territory":
                return LstrType.region;
            default:
                return LstrType.valueOf(item);
        }
    }

    enum UN_M49 {
        // there is always a global, and an ISO_alpha2_Code.
        // Antarctica is odd, so we include it in Oceania
        // That being done, there is always a Region_Code
        // If there is an Intermediate Region Code, then it is the subcontinent, and the Subregion
        // code is a subcontinent group
        // Otherwise the Subregion code is the subcontinent
        // 1    World   2   Africa  202 Sub-Saharan Africa  14  Eastern Africa  British Indian Ocean
        // Territory  86  IO  IOT

        Global_Code,
        Global_Name,
        Region_Code,
        Region_Name,
        Sub_region_Code,
        Sub_region_Name,
        Intermediate_Region_Code,
        Intermediate_Region_Name,
        Country_or_Area,
        M49_Code,
        ISO_alpha2_Code,
        ISO_alpha3_Code;
        static final Set<UN_M49> CODES =
                ImmutableSet.of(
                        Global_Code,
                        Region_Code,
                        Sub_region_Code,
                        Intermediate_Region_Code,
                        ISO_alpha2_Code);
    }

    public void testWithCountainment() throws IOException {
        final Multimap<String, String> containerToContainedsExternal = TreeMultimap.create();
        Path path = Paths.get(CLDRPaths.UTIL_SRC_DATA_DIR, "data/external/UN-M49.tsv");

        Files.lines(path)
                .forEach(
                        line -> {
                            List<String> parts = Splitters.TAB.splitToList(line);
                            main:
                            for (UN_M49 item : UN_M49.CODES) {
                                String container = parts.get(item.ordinal());
                                if (container.isEmpty()) {
                                    continue;
                                }
                                for (UN_M49 item2 : UN_M49.CODES) {
                                    if (item2.compareTo(item) <= 0) {
                                        continue;
                                    }
                                    String contained = parts.get(item2.ordinal());
                                    if (contained.isEmpty()) {
                                        continue;
                                    }
                                    containerToContainedsExternal.put(container, contained);
                                    continue main;
                                }
                            }
                        });

        // now get internal
        final Multimap<String, String> containerToContainedsCldr = TreeMultimap.create();
        SDI.getContainmentCore().entrySet().stream()
                .forEach(x -> containerToContainedsCldr.put(x.getKey(), x.getValue()));

        SetView<Entry<String, String>> externalButNotInternal =
                Sets.difference(
                        ImmutableSet.copyOf(containerToContainedsExternal.entries()),
                        ImmutableSet.copyOf(containerToContainedsCldr.entries()));
        SetView<Entry<String, String>> extra =
                Sets.difference(
                        ImmutableSet.copyOf(containerToContainedsCldr.entries()),
                        ImmutableSet.copyOf(containerToContainedsExternal.entries()));

        NameGetter ng = new NameGetter(CLDRConfig.getInstance().getEnglish());
        if (!assertEquals("Missing from Internal", "[]", externalButNotInternal.toString())) {
            externalButNotInternal.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            ng.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getKey())
                                                    + " > "
                                                    + ng.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getValue())));
        }
        if (!assertEquals("Extra in Internal", "[]", extra.toString())) {
            extra.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            ng.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getKey())
                                                    + " > "
                                                    + ng.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getValue())));
        }
    }
}
