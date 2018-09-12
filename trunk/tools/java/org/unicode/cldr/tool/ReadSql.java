package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M5;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

public class ReadSql {
    static final boolean DEBUG = false;
    static UserMap umap = new UserMap(CLDRPaths.DATA_DIRECTORY + "cldr/users.xml");

    enum MyOptions {
        organization(".*", "google", "organization"), verbose("", "", "verbose"),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = new Option(this, argumentPattern, defaultArgument, helpText);
        }

        static Options options = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                options.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return options.parse(MyOptions.values()[0], args, true);
        }
    }

    static Organization organization;
    static boolean verbose;

    public static void main(String[] args) throws IOException {
        MyOptions.parse(args, true);
        organization = Organization.valueOf(MyOptions.organization.option.getValue());
        verbose = MyOptions.verbose.option.doesOccur();

        long max = Long.MAX_VALUE;
        long maxItems = 10;
        boolean inCreate = false;
        try (BufferedReader r = FileUtilities.openFile(CLDRPaths.DATA_DIRECTORY, "cldr/cldr-DUMP-20160817.sql")) {
            while (--max > 0) {
                String line = r.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("INSERT")) {
                    //System.out.println(trunc(line, 100));
                    Data.parseLine(line, maxItems);
                } else if (line.startsWith("CREATE")) {
                    inCreate = true;
                    if (verbose) System.out.println(line);
                } else if (inCreate) {
                    if (verbose) System.out.println(line);
                    if (line.startsWith(") ENGINE")) {
                        inCreate = false;
                    }
                } else if (DEBUG) {
                    if (verbose) System.out.println(line);
                }
            }
        }
        Counter<String> keys = Data.getKeys();
        for (R2<Long, String> e : keys.getEntrySetSortedByCount(false, null)) {
            if (e.get0() > 0) {
                System.out.println(CldrUtility.toString(e));
            }
        }
        Data.show("_30");
    }

    private static String trunc(String line, int len) {
        return line.length() <= len ? line : line.substring(0, len) + "…";
    }

    static final Pattern INSERT = Pattern.compile("INSERT\\s+INTO\\s+`([^`]+)`\\s+VALUES\\s*");
    static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2014-05-01 17:19:57

    static class Items {
        final Date date;
        final User owner;
        final List<String> raw;

        public static Items of(String key, List<String> raw) {
            try {
                return new Items(key, raw);
            } catch (Exception e) {
                System.out.println("No user for: " + key + ": " + raw);
                return null;
            }
        }

        private Items(String key, List<String> raw) {
            Date temp;
            try {
                temp = df.parse(raw.get(raw.size() - 1));
            } catch (ParseException e) {
                temp = null;
            }
            this.date = temp;
            this.raw = raw;
            if (temp == null) {
                owner = null;
            } else {
                String ownerField;
                switch (key) {
                case "FEEDBACK":
                    ownerField = raw.get(1);
                    break;
                default:
                    ownerField = raw.get(2);
                    break;
                }
                owner = umap.get(ownerField);
            }
        }

        @Override
        public String toString() {
            return (date == null ? "???" : df.format(date)) + ";\t" + owner + ";\t" + CldrUtility.toString(raw);
        }
    }

    static class DateMap {
        M5<Integer, Integer, Integer, Integer, Boolean> yearMonthDays = ChainedMap.of(new TreeMap<>(), new TreeMap(), new TreeMap(), new TreeMap(),
            Boolean.class);
        int current = 0;

        void add(Date d) {
            yearMonthDays.put(d.getYear() + 1900, d.getMonth() + 1, d.getDate(), current++, Boolean.TRUE);
        }

        static DateFormat monthFormat = new SimpleDateFormat("MMM");

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            int years = 0;
            for (Entry<Integer, Map<Integer, Map<Integer, Map<Integer, Boolean>>>> yearMonthDay : yearMonthDays) {
                if (years++ > 0) {
                    result.append("; ");
                }
                final int year = yearMonthDay.getKey();
                result.append(year);
                result.append(": ");
                int months = 0;
                for (Entry<Integer, Map<Integer, Map<Integer, Boolean>>> monthDay : yearMonthDay.getValue().entrySet()) {
                    if (months++ > 0) {
                        result.append("; ");
                    }
                    final int month = monthDay.getKey();
                    result.append(monthFormat.format(new Date(year - 1900, month - 1, 1)));
                    result.append(": ");
                    int days = 0;
                    for (Entry<Integer, Map<Integer, Boolean>> dayCount : monthDay.getValue().entrySet()) {
                        if (days++ > 0) {
                            result.append(", ");
                        }
                        final int day = dayCount.getKey();
                        result.append(day);
                        final int count = dayCount.getValue().size();
                        if (count > 1) {
                            result.append('(');
                            result.append(count);
                            result.append(")");
                        }
                    }
                }
            }
            return result.toString();
        }
    }

    static class Data {
        final String key;
        final List<Items> dataItems = new ArrayList<Items>();
        static Map<String, Data> map = new TreeMap<>();

        public Data(String key) {
            this.key = key;
        }

        public static Counter<String> getKeys() {
            Counter<String> items = new Counter();
            for (Entry<String, Data> e : map.entrySet()) {
                items.add(e.getKey(), e.getValue().dataItems.size());
            }
            return items;
        }

        public static void show(String regex) {
            Matcher m = Pattern.compile(regex).matcher("");

            for (Entry<String, Data> e : map.entrySet()) {
                Data data = e.getValue();
                if (!m.reset(data.key).find()) {
                    continue;
                }
                Counter<User> counter = new Counter<>();
                Map<User, DateMap> dateMaps = new HashMap<>();
                for (Items item : data.dataItems) {
                    if (item.owner.org == organization) {
                        counter.add(item.owner, 1);
                        DateMap dateMap = dateMaps.get(item.owner);
                        if (dateMap == null) {
                            dateMaps.put(item.owner, dateMap = new DateMap());
                        }
                        dateMap.add(item.date);
                    }
                }
                for (R2<Long, User> item : counter.getEntrySetSortedByCount(false, null)) {
                    final Long count = item.get0();
                    final User user = item.get1();
                    System.out.println("key: " + data.key + "; count: " + count + "; " + user + "\t" + dateMaps.get(user));
                }
            }
        }

        @Override
        public String toString() {
            return key + "=" + CldrUtility.toString(dataItems);
        }

        public Items add(ArrayList<String> items) {
            final Items items2 = Items.of(key, items);
            if (items2 != null && items2.owner != null) {
                dataItems.add(items2);
                return items2;
            }
            return null;
        }

        static void parseLine(String line, long maxItems) {
            Matcher m = INSERT.matcher(line);
            String key;
            int i;
            if (m.lookingAt()) {
                key = m.group(1);
                i = m.end();
            } else {
                throw new IllegalArgumentException();
            }
            if (key.equals("FEEDBACK") || key.equals("sf_fora")) { // cf. private FeedBack.TABLE_FEEDBACK and public SurveyForum.DB_FORA
                return; // old format
            }
            boolean inQuote = false;
            boolean skipComma = true;
            StringBuilder buffer = new StringBuilder();
            ArrayList<String> items = new ArrayList<>();
            Data current = map.get(key);
            if (current == null) {
                map.put(key, current = new Data(key));
            }
            ArrayList<Data> rows = new ArrayList<>();

            while (i < line.length()) {
                int cp = line.codePointAt(i);
                i += Character.charCount(cp);
                if (inQuote) {
                    switch (cp) {
                    case '\'':
                        inQuote = false;
                        break;
                    case '\\':
                        cp = line.codePointAt(i);
                        i += Character.charCount(cp);
                        // fall through
                    default:
                        buffer.appendCodePoint(cp);
                        break;
                    }
                } else {
                    switch (cp) {
                    case '\'':
                        inQuote = true;
                        break;
                    case ',':
                        if (!skipComma) {
                            items.add(buffer.toString());
                            buffer.setLength(0);
                        }
                        break;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '(':
                        skipComma = false;
                        break;
                    case ')':
                        skipComma = true;
                        items.add(buffer.toString());
                        buffer.setLength(0);
                        Items lastItem = current.add(items);
                        if (--maxItems > 0 && lastItem != null) {
                            if (verbose) System.out.println(key + "\t" + lastItem);
                        }
                        items = new ArrayList<>();
                        break;
                    case '\\':
                        cp = line.codePointAt(i);
                        i += Character.charCount(cp);
                        // fall through
                    default:
                        buffer.appendCodePoint(cp);
                        break;
                    }
                }
            }
        }
    }

    static class User {
        final int id;
        final String email;
        final Level level;
        final String name;
        final Organization org;
        final Set<String> locales;

        public User(XPathParts parts) {
            this.id = Integer.parseInt(parts.getAttributeValue(-1, "id"));
            this.email = parts.getAttributeValue(-1, "email");
            this.level = Level.valueOf(parts.getAttributeValue(-1, "level"));
            this.name = parts.getAttributeValue(-1, "name");
            this.org = Organization.fromString(parts.getAttributeValue(-1, "org"));
            this.locales = ImmutableSet.copyOf(Arrays.asList(parts.getAttributeValue(-1, "locales").split("[, ]+")));
        }

        @Override
        public String toString() {
            return "id: " + id
                + "; email: " + email
                + "; name: " + name
                + "; level: " + level
                + "; org: " + org
                + "; locales: " + locales;
        }
    }

    static class UserMap {
        Map<Integer, User> map = new HashMap<>();

        UserMap(String filename) {
            List<Pair<String, String>> data = new ArrayList<>();
            XMLFileReader.loadPathValues(filename, data, false);
            //  <user id="1271" email="..." level="tc" name="..." org="adobe" locales="pt"/>
            for (Pair<String, String> e : data) {
                String path = e.getFirst();
                XPathParts parts = XPathParts.getInstance(path);
                User user = new User(parts);
                map.put(user.id, user);
            }
        }

        public User get(String string) {
            return map.get(Integer.valueOf(string));
        }
    }
}
