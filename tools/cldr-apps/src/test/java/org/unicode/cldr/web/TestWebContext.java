package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.web.WebContext.HTMLDirection;
import org.unicode.cldr.web.util.JsonUtil;

public class TestWebContext {

    @Test
    void testHtmlDir() {
        HTMLDirection dir = HTMLDirection.RIGHT_TO_LEFT;
        final Gson gson = JsonUtil.gson();
        Map<String, Object> locale = new HashMap<>();
        locale.put("dir", dir);
        final String s = gson.toJson(locale);
        // verify that the enum annotation is working properly
        assertFalse(s.contains("RIGHT_TO_LEFT"), s);
        assertTrue(s.contains("rtl"), s);
    }
}
