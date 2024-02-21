package org.unicode.cldr.tool;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestAddPopulationData {
    @Test
    public void TestParseUnStats() throws IOException {
        AddPopulationData.loadUnLiteracy();
    }
}
