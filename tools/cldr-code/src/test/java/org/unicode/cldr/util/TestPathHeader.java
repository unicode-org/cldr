import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.*;

public class TestPathHeader {
    static final String GREGORIAN = "gregorian";

    @Test
    public void TestNonGregorianMonths() {
        CLDRFile english = CLDRConfig.getInstance().getCldrFactory().make("en", true);
        PathHeader.Factory phf = PathHeader.getFactory(null);
        for (final String xpath : english.fullIterable()) {
            // skip unless a month name
            if (!xpath.startsWith("//ldml/dates/calendars/calendar")) continue;
            if (!xpath.contains("/month[")) continue;
            final PathHeader ph = phf.fromPath(xpath);
            final String value = english.getStringValue(xpath);
            // skip any null values.
            if (value == null) continue;
            // skip any hidden items
            final XPathParts xpp = XPathParts.getFrozenInstance(xpath);
            final String calType = xpp.getAttributes(3).get("type");
            // we skip Gregorian itself.
            if (calType.equals(GREGORIAN)) continue;
            // now, we need Gregorian for comparison.
            final String gregopath =
                    xpp.cloneAsThawed().setAttribute("calendar", "type", GREGORIAN).toString();
            final String gregovalue = english.getStringValue(gregopath);
            final PathHeader gph = phf.fromPath(gregopath);
            if (gph.shouldHide()) continue; // hide if the *gregorian* is hidden.

            if (gregovalue != null && value.equals(gregovalue)) {
                // The month name is the same as the Gregorian. So we assume the codes will be the
                // same.
                if (!ph.shouldHide())
                    assertEquals(
                            ph.getCode(),
                            gph.getCode(),
                            () ->
                                    "Expected the same PathHeader code"
                                            + ph
                                            + " as Gregorian  "
                                            + gph
                                            + " for xpath "
                                            + xpath
                                            + " because the month "
                                            + value
                                            + " was the same as gregorian "
                                            + gregopath);
                assertTrue(
                        ph.shouldHide(),
                        () ->
                                "Should be hidden. To fix: remove "
                                        + calType
                                        + " from the %G line of PathHeader.txt.");
            } else {
                assertNotEquals(
                        ph.getCode(),
                        gph.getCode(),
                        () ->
                                "Expected a different PathHeader code from Gregorian for xpath "
                                        + xpath
                                        + " because the month "
                                        + value
                                        + " was the differnet from gregorian "
                                        + gregovalue
                                        + ".");
            }
        }
    }
}
