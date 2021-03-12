package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.CLDRLocale;

public final class WhatisResponse {
    @Schema(description = "query string")
    public String q;
    public String q2 = null;
    public String err = null;
    public List<XpathResult> xpath = null;
    public List<StandardCodeResult> exact = null;
    public List<StandardCodeResult> full = null;
    public int pathsSearched = 0;
    public int localesSearched = 0;

    public WhatisResponse(String queryString) {
        q = queryString;
    }

    class XpathResult {
        public String loc;
        public String name;
        public List<PathAndValue> matches = new ArrayList<>();

        public XpathResult(CLDRLocale locale) {
            loc = locale.toString();
            name = locale.getDisplayName();
        }
    }

    class PathAndValue {
        public String path;
        public String value;

        public PathAndValue(String p, String v) {
            path = p;
            value = v;
        }
    }

    class StandardCodeResult {
        public String type;
        public String code;
        public List<StandardCodeResultDataItem> data = new ArrayList<>();

        public StandardCodeResult(String t, String c) {
            type = t;
            code = c;
        }
    }

    class StandardCodeResultDataItem {
        public String s;
        public boolean isWinner;

        public StandardCodeResultDataItem(String str, boolean win) {
            s = str;
            isWinner = win;
        }
    }
}
