package org.unicode.cldr.tool;

import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class UnLiteracyParser extends XMLFileReader.SimpleHandler {

    private static final String VALUE = "Value";
    private static final String RELIABILITY = "Reliability";
    private static final String LITERACY = "Literacy";
    private static final String YEAR = "Year";
    private static final String COUNTRY_OR_AREA = "Country or Area";
    private static final String AGE = "Age";
    static final String LITERATE = "Literate";
    static final String ILLITERATE = "Illiterate";
    private static final String UNKNOWN = "Unknown";
    private static final String TOTAL = "Total";
    // Debug stuff
    public static void main(String args[]) {
        final UnLiteracyParser ulp = new UnLiteracyParser().read();
        for (final Entry<String, PerCountry> e : ulp.perCountry.entrySet()) {
            final String country = e.getKey();
            final String latest = e.getValue().latest();
            final PerYear py = e.getValue().perYear.get(latest);

            Long literate = py.total(LITERATE);
            Long illiterate = py.total(ILLITERATE);
            Long unknown = py.total(UNKNOWN);
            Long total = py.total(TOTAL);

            System.out.println(
                    country
                            + "\t"
                            + latest
                            + "\t"
                            + literate
                            + "/"
                            + illiterate
                            + ", "
                            + unknown
                            + " = "
                            + total);
            if ((literate + illiterate + unknown) != total) {
                System.out.println(
                        "- doesn't add up for "
                                + country
                                + " - total is "
                                + (literate + illiterate + unknown));
            }
        }
    }

    int recCount = 0;

    // Reading stuff
    public static final String UN_LITERACY = "external/un_literacy.xml";

    UnLiteracyParser read() {
        System.out.println("* Reading " + UN_LITERACY);
        new XMLFileReader()
                .setHandler(this)
                .readCLDRResource(UN_LITERACY, XMLFileReader.CONTENT_HANDLER, false);
        // get the final record
        handleNewRecord();
        LocalizedNumberFormatter nf = NumberFormatter.with().locale(Locale.ENGLISH);
        System.out.println(
                "* Read "
                        + nf.format(recCount)
                        + " record(s) with "
                        + nf.format(perCountry.size())
                        + " region(s) from "
                        + UN_LITERACY);
        return this;
    }

    // Parsing stuff
    @Override
    public void handlePathValue(String path, String value) {
        if (!path.startsWith("//ROOT/data/record")) {
            return;
        }
        final String field = XPathParts.getFrozenInstance(path).getAttributeValue(-1, "name");
        handleField(field, value);
    }

    @Override
    public void handleElement(CharSequence path) {
        if ("//ROOT/data/record".equals(path.toString())) {
            handleNewRecord();
        }
    }

    // Data ingestion
    final Map<String, String> thisRecord = new HashMap<String, String>();

    private void handleField(String field, String value) {
        final String old = thisRecord.put(field, value);
        if (old != null) {
            throw new IllegalArgumentException(
                    "Duplicate field " + field + ", context: " + thisRecord);
        }
    }

    private void handleNewRecord() {
        if (!thisRecord.isEmpty() && validate()) {
            recCount++;
            handleRecord();
        }

        thisRecord.clear();
    }

    boolean validate() {
        try {
            assertEqual("Area", "Total");
            assertEqual("Sex", "Both Sexes");

            assertPresent(AGE);
            assertPresent(COUNTRY_OR_AREA);
            assertPresent(LITERACY);
            assertPresent(VALUE);
            assertPresent(YEAR);
            assertPresent(RELIABILITY);

            return true;
        } catch (Throwable t) {
            final String context = thisRecord.toString();
            throw new IllegalArgumentException("While parsing " + context, t);
        }
    }

    void assertPresent(String field) {
        String value = get(field);
        if (value == null) {
            throw new NullPointerException("Missing field: " + field);
        } else if (value.isEmpty()) {
            throw new NullPointerException("Empty field: " + field);
        }
    }

    void assertEqual(String field, String expected) {
        assertPresent(field);
        String value = get(field);
        if (!value.equals(expected)) {
            throw new NullPointerException(
                    "Expected " + field + "=" + expected + " but got " + value);
        }
    }

    private final String get(String field) {
        final String value = thisRecord.get(field);
        if (value == null) return value;
        return value.trim();
    }

    private void handleRecord() {
        final String country = get(COUNTRY_OR_AREA);
        final String year = get(YEAR);
        final String age = get(AGE);
        final String literacy = get(LITERACY);
        final String reliability = get(RELIABILITY);
        final PerAge pa =
                perCountry
                        .computeIfAbsent(country, (String c) -> new PerCountry())
                        .perYear
                        .computeIfAbsent(year, (String y) -> new PerYear())
                        .perAge
                        .computeIfAbsent(age, (String a) -> new PerAge());

        if (pa.reliability == null) {
            pa.reliability = reliability;
        } else if (!pa.reliability.equals(reliability)) {
            throw new IllegalArgumentException(
                    "Inconsistent reliability " + reliability + " for " + thisRecord);
        }
        final Long old = pa.perLiteracy.put(literacy, getLongValue());
        if (old != null) {
            System.err.println("Duplicate record " + country + " " + year + " " + age);
        }
    }

    private long getLongValue() {
        final String value = get(VALUE);
        if (value.contains(
                ".")) { // yes. some of the data has decimal points. Ignoring the fractional part.
            return Long.parseLong(value.split("\\.")[0]);
        } else {
            return Long.parseLong(value);
        }
    }

    final Map<String, PerCountry> perCountry = new TreeMap<String, PerCountry>();

    final class PerCountry {
        final Map<String, PerYear> perYear = new TreeMap<String, PerYear>();

        public String latest() {
            final String y[] = perYear.keySet().toArray(new String[0]);
            return y[y.length - 1];
        }
    }

    final class PerYear {
        final Map<String, PerAge> perAge = new TreeMap<String, PerAge>();

        Long total(String literacy) {
            return perAge.values().stream()
                    .map((pa) -> pa.perLiteracy.getOrDefault(literacy, 0L))
                    .reduce(0L, (Long a, Long b) -> a + b);
        }
    }

    final class PerAge {
        final Map<String, Long> perLiteracy = new TreeMap<String, Long>();
        String reliability = null;
    }
}
