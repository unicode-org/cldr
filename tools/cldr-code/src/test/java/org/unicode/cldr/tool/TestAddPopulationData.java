package org.unicode.cldr.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ibm.icu.util.Output;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.Pair;

public class TestAddPopulationData {
    @Test
    public void TestParseUnStats() throws IOException {
        Output<Boolean> err = new Output<>(false);
        // this is already run once during static init. we run it again to capture err value
        List<Pair<String, Double>> unLiteracy = AddPopulationData.getUnLiteracy(err);
        assertFalse(
                err.value,
                "getUnLiteracy() returned errs - check err log for 'ERROR: CountryCodeConverter'");
        assertFalse(unLiteracy.isEmpty(), "un literacy shouldn't be empty");
        // optionally dump out values
        if (false)
            for (final Pair<String, Double> p : unLiteracy) {
                System.out.println(p.getFirst() + " - " + p.getSecond());
            }
    }
}
