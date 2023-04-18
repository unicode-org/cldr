package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.CLDRLocale;

@Schema(description = "Return value for Whatis query")
public final class WhatisResponse {
    @Schema(description = "query string")
    public String q;

    public String q2 = null;
    public String err = null;
    public int pathsSearched = 0;
    public int localesSearched = 0;

    /*
     * "By default, JSONB ignores properties with a non public access. All public
     * properties - either public fields or non public fields with public getters
     * are serialized into JSON text."
     * http://json-b.net/docs/user-guide.html#ignoring-properties
     */
    private List<XpathResult> xpath = null;
    private List<StandardCodeResult> exact = null;
    private List<StandardCodeResult> full = null;

    public WhatisResponse(String queryString) {
        q = queryString;
    }

    public XpathResult[] getXpath() {
        System.out.println("Hello my name is getXpath");
        return xpath.toArray(new XpathResult[xpath.size()]);
    }

    public StandardCodeResult[] getExact() {
        System.out.println("Hello my name is getExact");
        return exact.toArray(new StandardCodeResult[exact.size()]);
    }

    public StandardCodeResult[] getFull() {
        System.out.println("Hello my name is getFull");
        return full.toArray(new StandardCodeResult[full.size()]);
    }

    class XpathResult {
        public String loc;
        public String name;
        private List<PathAndValue> matches = new ArrayList<>();

        public XpathResult(CLDRLocale locale) {
            loc = locale.toString();
            name = locale.getDisplayName();
        }

        public PathAndValue[] getMatches() {
            System.out.println("Hello my name is getMatches");
            return matches.toArray(new PathAndValue[matches.size()]);
        }

        public void addMatch(PathAndValue pathAndValue) {
            matches.add(pathAndValue);
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
        private List<StandardCodeResultDataItem> data = new ArrayList<>();

        public StandardCodeResult(String t, String c) {
            type = t;
            code = c;
        }

        public StandardCodeResultDataItem[] getData() {
            System.out.println("Hello my name is getData");
            return data.toArray(new StandardCodeResultDataItem[data.size()]);
        }

        public void addData(StandardCodeResultDataItem item) {
            data.add(item);
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

    public void addExact(StandardCodeResult r) {
        if (exact == null) {
            exact = new ArrayList<>();
        }
        exact.add(r);
    }

    public void addFull(StandardCodeResult r) {
        if (full == null) {
            full = new ArrayList<>();
        }
        full.add(r);
    }

    public void addXpath(XpathResult r) {
        if (xpath == null) {
            xpath = new ArrayList<>();
        }
        xpath.add(r);
    }
}
