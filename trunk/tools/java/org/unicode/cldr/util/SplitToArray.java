package org.unicode.cldr.util;

import java.util.List;

import com.google.common.base.Splitter;

public class SplitToArray {
    private Splitter splitter;

    public SplitToArray(Splitter splitter) {
        this.splitter = splitter;
    }

    String[] split(String source) {
        List<String> parts = splitter.splitToList(source);
        return parts.toArray(new String[parts.size()]);
    }
}
