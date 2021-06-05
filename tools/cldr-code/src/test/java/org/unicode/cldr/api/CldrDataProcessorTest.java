package org.unicode.cldr.api;

import com.ibm.icu.dev.test.TestFmwk;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class CldrDataProcessorTest extends TestFmwk {
    private static final AttributeKey TERRITORY_TYPE = AttributeKey.keyOf("territory", "type");
    private static final AttributeKey CURRENCY_TYPE = AttributeKey.keyOf("currency", "type");

    // An overly simplistic value type for currency for testing purposes. In real code you would
    // probably want an immutable type and a separate builder, or a mutable type just to collect
    // values that doesn't need equals/hashcode (this class serves 2 purposes in the test).
    private static final class CurrencyData {
        final String key;
        String name = "";
        String symbol = "";

        CurrencyData(String key) {
            this.key = key;
        }

        CurrencyData(String key, String name, String symbol) {
            this.key = key;
            this.name = name;
            this.symbol = symbol;
        }

        @Override public boolean equals(Object o) {
            if (o instanceof CurrencyData) {
                CurrencyData that = (CurrencyData) o;
                return key.equals(that.key) && name.equals(that.name) && symbol.equals(that.symbol);
            }
            return false;
        }

        @Override public int hashCode() {
            return Objects.hash(key, name, symbol);
        }

        @Override public String toString() {
            return String.format("CurrencyData{name=%s, symbol='%s'}", name, symbol);
        }
    }

    // For collecting processed values.
    private static final class State {
        ImmutableMap<String, String> names = ImmutableMap.of();
        ImmutableMap<String, CurrencyData> currencies = ImmutableMap.of();

        void setNames(Map<String, String> map) {
            names = ImmutableMap.copyOf(map);
        }

        void setCurrencies(Map<String, CurrencyData> map) {
            currencies = ImmutableMap.copyOf(map);
        }
    }

    private static final CldrDataProcessor<State> VISITOR = createTestVisitor();

    private static CldrDataProcessor<State> createTestVisitor() {
        // Note that this is deliberately doing things the "messy" way by creating and then copying
        // a map. This is to show an extra level of processing in tests. You could just have a
        // value action which adds the territory to a map in the State object.
        CldrDataProcessor.Builder<State> builder = CldrDataProcessor.builder();
        builder
            .addAction(
                "//ldml/localeDisplayNames/territories",
                () -> new LinkedHashMap<String, String>(),
                State::setNames)
            .addValueAction(
                "territory[@type=*]",
                (map, value) -> map.put(value.getPath().get(TERRITORY_TYPE), value.getValue()));

        // Another convoluted example for testing. This has the same additional level for a map
        // just so we can show a 3-level processor. In real code this wouldn't look so messy.
        CldrDataProcessor.SubProcessor<CurrencyData> currencyProcessor = builder
            .addAction(
                "//ldml/numbers/currencies",
                () -> new LinkedHashMap<String, CurrencyData>(),
                State::setCurrencies)
            .addAction(
                "currency[@type=*]",
                (map, path) -> new CurrencyData(path.get(CURRENCY_TYPE)),
                (map, data) -> map.put(data.key, data));
        currencyProcessor.addValueAction(
            "displayName",
            (data, value) -> data.name = value.getValue());
        currencyProcessor.addValueAction(
            "symbol",
            (data, value) -> data.symbol = value.getValue());

        return builder.build();
    }

    public void TestTwoLevelProcessing() {
        CldrData data = CldrDataSupplier.forValues(Arrays.asList(
            ldml("localeDisplayNames/territories/territory[@type=\"BE\"]", "Belgium"),
            ldml("localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland"),
            ldml("localeDisplayNames/territories/territory[@type=\"IN\"]", "India")));

        State state = VISITOR.process(data, new State(), CldrData.PathOrder.DTD);

        assertTrue("Expected output names", state.names.equals(ImmutableMap.of(
            "BE", "Belgium",
            "CH", "Switzerland",
            "IN", "India")));
    }

    public void TestThreeLevelProcessing() {
        CldrData data = CldrDataSupplier.forValues(Arrays.asList(
            ldml("numbers/currencies/currency[@type=\"EUR\"]/displayName", "euro"),
            ldml("numbers/currencies/currency[@type=\"EUR\"]/symbol", "€"),
            ldml("numbers/currencies/currency[@type=\"CHF\"]/displayName", "Swiss franc"),
            ldml("numbers/currencies/currency[@type=\"CHF\"]/symbol", "Fr."),
            ldml("numbers/currencies/currency[@type=\"INR\"]/displayName", "Indian rupee"),
            ldml("numbers/currencies/currency[@type=\"INR\"]/symbol", "₹")));

        State state = VISITOR.process(data, new State(), CldrData.PathOrder.DTD);

        assertTrue("Expected output currencies", state.currencies.equals(ImmutableMap.of(
            "CHF", new CurrencyData("CHF", "Swiss franc", "Fr."),
            "EUR", new CurrencyData("EUR", "euro", "€"),
            "INR", new CurrencyData("INR", "Indian rupee", "₹"))));
    }

    private static CldrValue ldml(String path, String value) {
        return CldrValue.parseValue("//ldml/" + path, value);
    }
}
