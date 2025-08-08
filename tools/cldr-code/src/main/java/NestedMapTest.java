import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.NestedMap.ImmutableMap3;
import org.unicode.cldr.util.NestedMap.Map3;

public class NestedMapTest extends TestFmwk {

    enum MapType {
        Tree(TreeMap::new),
        Hash(HashMap::new),
        LinkedHash(LinkedHashMap::new),
        ConcurrentHash(ConcurrentHashMap::new);

        private final Supplier supplier;

        private MapType(Supplier x) {
            this.supplier = x;
        }
    }

    enum Mutability {
        mutable,
        immutable
    }

    enum Order {
        az,
        za,
        na
    }

    public static void main(String[] args) {
        new NestedMapTest().run(args);
    }

    public void testCore() {
        // Create a 3-level map with type-safety:
        // Level 1: String (Region)
        // Level 2: String (Product)
        // Level 3: Integer (Year)
        // Value: Double (Sales Amount)
        for (MapType tt : MapType.values()) {
            Map3<String, String, Integer, Double> salesData = Map3.create(tt.supplier);

            // --- Type-Safe PUT operations ---
            salesData.put("South America", "Widget", 2024, 150000.75);
            salesData.put("South America", "Widget", 2020, 0d);
            salesData.put("Europe", "Gadget", 2025, 95000.50);
            salesData.put("North Africa", "Widget", 2025, 175000.00);
            salesData.put("South Africa", "Gadget", 2025, 120000.00);
            salesData.put("Asia", "Gadget3", 2023, 666.00);

            String streamTest = tryOperations(tt, Mutability.mutable, salesData, null);

            // Immutable
            ImmutableMap3<String, String, Integer, Double> salesDataIM =
                    salesData.createImmutable();
            tryOperations(tt, Mutability.immutable, salesDataIM, streamTest);
        }
    }

    private String tryOperations(
            MapType tt,
            Mutability mutability,
            Map3<String, String, Integer, Double> salesData,
            String streamTest) {

        logln(tt + ", " + mutability);

        // The following line would cause a COMPILE ERROR due to incorrect type,
        // demonstrating the benefit of the shim.
        // salesData.put("Asia", "Widget", "2024", 99.99); //<- Compile Error! "2024" is not an
        // Integer.

        logln(salesData.toString());

        // --- Type-Safe GET operations ---
        // The result is automatically cast to Double, no manual cast needed.
        Double sales = salesData.get("Asia", "Gadget3", 2023);
        assertEquals("Sales for Asia, Gadget3, 2003", 666d, sales);

        // --- Type-Safe REMOVE operations ---
        String result = null;
        try {
            Double removedValue = salesData.remove("Europe", "Gadget", 2025);
            result = removedValue.toString();
        } catch (Exception e) {
            result = e.getClass().getName();
        }
        assertEquals(
                "put",
                mutability == Mutability.mutable
                        ? "95000.5"
                        : "java.lang.UnsupportedOperationException",
                result);

        assertNull("Data after removal", salesData.get("Europe", "Gadget", 2025));

        // check order
        List<String> list =
                salesData.stream().map(x -> x.getKey1()).collect(Collectors.toUnmodifiableList());
        Order order = getOrder(list);
        switch (tt) {
            case ConcurrentHash:
            case Hash:
                // should be unordered (normally)
                assertEquals(tt.name(), Order.na, order);
                break;
            case LinkedHash:
                // must be za (from the way we set up the data
                assertEquals(tt.name(), Order.za, order);
                break;
            case Tree:
                // must be az (because sorted)
                assertEquals(tt.name(), Order.az, order);
                break;
        }

        // Streaming
        String data = salesData.stream().map(x -> x.toString()).collect(Collectors.joining(", "));
        if (streamTest != null) {
            assertEquals("StreamTest", streamTest, data);
        } else {
            logln("StreamTest: " + data);
        }
        return data;
    }

    public void testOrder() {
        List<String> test =
                List.of("South America", "Asia", "Europe", "South Africa", "North Africa");
        assertEquals("Order ok", Order.na, getOrder(test));
    }

    private <T extends Comparable<T>> Order getOrder(Collection<T> list) {
        Set<Order> options = EnumSet.allOf(Order.class);
        T lastItem = null;
        for (T item : list) {
            if (lastItem != null) {
                int intOrder = lastItem.compareTo(item);
                if (intOrder < 0) {
                    options.remove(Order.za);
                    options.remove(Order.na);
                } else if (intOrder > 0) {
                    options.remove(Order.az);
                    options.remove(Order.na);
                }
            }
            lastItem = item;
        }
        return options.isEmpty() ? Order.na : options.iterator().next();
    }
}
