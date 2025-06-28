package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.NestedMap.ImmutableNestedMap2;
import org.unicode.cldr.util.NestedMap.NestedMap2;

public class TestNestedMaps extends TestFmwk {
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

            NestedMap2<String, Double, Boolean> nm2 = NestedMap2.create(supplier);

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

            ImmutableNestedMap2<String, Double, Boolean> nm2_immutable = nm2.createImmutable();
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
}
