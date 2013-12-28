package org.unicode.cldr.web;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StatisticsUtils {
    private static class di {
        public String s;
        public int v;
        public int d;

        di(String s) {
            this.s = s;
            this.v = 0;
            this.d = 0;
        }
    };

    public static final int NO_LIMIT = -1;

    public static String[][] calcSubmits(String[][] v, String[][] d) {
        return calcSubmits(v, d, NO_LIMIT);
    }

    public static String[][] calcSubmits(String[][] v, String[][] d, int limit) {
        Map<String, di> all = new TreeMap<String, di>();

        for (int i = 0; i < v.length; i++) {
            di ent = all.get(v[i][0]);
            if (ent == null) {
                all.put(v[i][0], (ent = new di(v[i][0])));
            }
            ent.v = Integer.parseInt(v[i][1]);
        }
        for (int i = 0; i < d.length; i++) {
            di ent = all.get(d[i][0]);
            if (ent == null) {
                all.put(d[i][0], (ent = new di(d[i][0])));
            }
            ent.d = Integer.parseInt(d[i][1]);
        }
        Set<di> asSet = new TreeSet<di>(new Comparator<di>() {

            @Override
            public int compare(di arg0, di arg1) {
                int rc = arg1.d - arg0.d;
                if (rc == 0) {
                    rc = arg0.s.compareTo(arg1.s);
                }
                return rc;
            }
        });
        asSet.addAll(all.values());

        int newSize = asSet.size();
        if (limit != NO_LIMIT && limit < newSize) {
            newSize = limit;
        }

        String[][] ret = new String[newSize][];
        int j = 0;
        for (di dd : asSet) {
            if (j == newSize) {
                return ret;
            }
            ret[j] = new String[3];
            ret[j][0] = dd.s;
            ret[j][1] = Integer.toString(dd.d);
            ret[j][2] = Integer.toString(dd.v);
            j++;
        }
        return ret;
    }

    // just put some common statistics here
    public static int getTotalItems() {
        if (!SurveyMain.isSetup || SurveyMain.isBusted()) {
            return -2;
        }
        try {
            return DBUtils.getFirstInt(DBUtils.queryToCachedJSON("total_items", 1 * 60 * 1000,
                "select count(*) from "+DBUtils.Table.VOTE_VALUE+" where submitter is not null"));
        } catch (Throwable t) {
            return -1;
        }
    }

}
