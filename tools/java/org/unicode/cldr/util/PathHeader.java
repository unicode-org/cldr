package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup.Finder;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

public class PathHeader implements Comparable<PathHeader> {
    public final String section;
    public final String page;
    public final String header;
    public final String code;
    private final int sectionOrder;
    private final int pageOrder;
    private final int headerOrder;
    private final int codeOrder;

    static final Pattern SEMI = Pattern.compile("\\s*;\\s*");

    static final Collator alphabetic = Collator.getInstance(ULocale.ENGLISH);

    static final RegexLookup<RawData> lookup = RegexLookup.of(new PathHeaderTransform())
    .setPatternTransform(RegexLookup.RegexFinderTransformPath)
    .loadFromFile(PathHeader.class, "PathHeader.txt");

    static final Output<String[]> args = new Output<String[]>(); // synchronized with lookup

    /**
     * @param section
     * @param sectionOrder
     * @param page
     * @param pageOrder
     * @param header
     * @param headerOrder
     * @param code
     * @param codeOrder
     */
    private PathHeader(String section, int sectionOrder, String page, int pageOrder, String header,
            int headerOrder, String code, int codeOrder) {
        this.section = section;
        this.sectionOrder = sectionOrder;
        this.page = page;
        this.pageOrder = pageOrder;
        this.header = header;
        this.headerOrder = headerOrder;
        this.code = code;
        this.codeOrder = codeOrder;
    }

    static final Matcher ALT_MATCHER = Pattern.compile("\\[@alt=\"([^\"]*+)\"]").matcher("");
    /**
     * @param section
     * @param page
     * @param header
     * @param code
     */
    public static PathHeader fromPath(String distinguishingPath) {
        synchronized (lookup) {
            // special handling for alt
            String alt = null;
            int altPos = distinguishingPath.indexOf("[@alt=");
            if (altPos >= 0) {
                if (ALT_MATCHER.reset(distinguishingPath).find()) {
                    alt = ALT_MATCHER.group(1);
                    distinguishingPath = distinguishingPath.substring(0,ALT_MATCHER.start()) + distinguishingPath.substring(ALT_MATCHER.end());
                } else {
                    throw new IllegalArgumentException();
                }
            }
            RawData data = lookup.get(distinguishingPath,null,args);
            if (data == null) {
                return null;
            }
            String section;
            String page;
            String header;
            String code;
            try {
                section = RegexLookup.replace(data.section, args.value);
                page = RegexLookup.replace(data.page, args.value);
                header = RegexLookup.replace(data.header, args.value);
                code = RegexLookup.replace(data.code, args.value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Probably too few capturing groups in regex for " + distinguishingPath);
            }
            if (alt != null) {
                code += "-" + alt;
            }
            return new PathHeader(
                    section, data.sectionOrder, 
                    page, data.pageOrder,  
                    header, data.headerOrder,
                    code, data.codeOrder);
        }
    }
    @Override
    public String toString() { 
        return section + "\t(" + sectionOrder + ");\t"
        + page + "\t(" + pageOrder + ");\t"
        + header + "\t(" + headerOrder + ");\t"
        + code + "\t(" + codeOrder + ")";
    }
    @Override
    public int compareTo(PathHeader other) {
        // Within each section, order alphabetically if the integer orders are not different.
        int result;
        if (0 != (result = sectionOrder - other.sectionOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(section, other.section))) {
            return result;
        }
        if (0 != (result = pageOrder - other.pageOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(page, other.page))) {
            return result;
        }
        if (0 != (result = headerOrder - other.headerOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(header, other.header))) {
            return result;
        }
        if (0 != (result = codeOrder - other.codeOrder)) {
            return result;
        }
        // Question: make English value order??
        if (0 != (result = alphabetic.compare(code, other.code))) {
            return result;
        }
        return 0;
    }


    public static class ChronologicalOrder<T> {
        Map<T,Integer> map = new HashMap<T,Integer>();
        int getOrder(T item) {
            Integer old = map.get(item);
            if (old != null) {
                return old.intValue();
            }
            int result = map.size();
            map.put(item, result);
            return result;
        }
    }

    static class RawData {
        static ChronologicalOrder<String> sectionOrdering = new ChronologicalOrder<String>();
        static ChronologicalOrder<String> pageOrdering = new ChronologicalOrder<String>();
        static ChronologicalOrder<String> headerOrdering = new ChronologicalOrder<String>();
        static ChronologicalOrder<String> codeOrdering = new ChronologicalOrder<String>();

        public RawData(String source) {
            String[] split = SEMI.split(source);
            section = split[0];
            sectionOrder = sectionOrdering.getOrder(section);
            page = split[1];
            pageOrder = pageOrdering.getOrder(page);
            header = split[2];
            headerOrder = headerOrdering.getOrder(header);
            code = split[3];
            codeOrder = codeOrdering.getOrder(code);
        }
        public final String section;
        public final int sectionOrder;
        public final String page;
        public final int pageOrder;
        public final String header;
        public final int headerOrder;
        public final String code;
        public final int codeOrder;
    }

    static class PathHeaderTransform implements Transform<String,RawData> {
        static ChronologicalOrder<String> pathOrder = new ChronologicalOrder<String>();
        @Override
        public RawData transform(String source) {
            return new RawData(source);
        }
    }


    public static void main(String[] args) {
        if (false) {
            for (R2<Finder, RawData> foo : lookup) {
                System.out.println(foo);
            }
        }

        PathStarrer pathStarrer = new PathStarrer();
        pathStarrer.setSubstitutionPattern("%A");

        // move to test
        CLDRFile english = TestInfo.getInstance().getEnglish();
        Map<PathHeader, String> sorted = new TreeMap();
        Map<String,String> missing = new TreeMap();
        Map<String,String> skipped = new TreeMap();
        Map<String,String> collide = new TreeMap();

        System.out.println("Traversing Paths");
        for (String path : english) {
            PathHeader pathHeader = PathHeader.fromPath(path);
            String value = english.getStringValue(path);
            if (pathHeader == null) {
                final String starred = pathStarrer.set(path);
                missing.put(starred, value + "\t" + path);
                continue;
            }
            if (pathHeader.section.equalsIgnoreCase("skip")) {
                final String starred = pathStarrer.set(path);
                skipped.put(starred, value + "\t" + path);
                continue;
            }
            String old = sorted.get(pathHeader);
            if (old != null && !path.equals(old)) {
                collide.put(path, old + "\t" + pathHeader);
            }
            sorted.put(pathHeader, value + ";\t" + path);
        }
        System.out.println("\nConverted:\t" + sorted.size());
        String lastHeader = "";
        for (Entry<PathHeader, java.lang.String> entry : sorted.entrySet()) {
            final PathHeader pathHeader = entry.getKey();
            if (lastHeader.equals(pathHeader.header)) {
                System.out.println();
                lastHeader = pathHeader.header;
            }
            System.out.println(pathHeader + ";\t" + entry.getValue());
        }
        System.out.println("\nCollide:\t" + collide.size());
        for (Entry<String, String> item : collide.entrySet()) {
            System.out.println("\t" + item);
        }
        System.out.println("\nMissing:\t" + missing.size());
        for (Entry<String, String> item : missing.entrySet()) {
            System.out.println("\t" + item.getKey() + "\tvalue:\t" + item.getValue());
        }
        System.out.println("\nSkipped:\t" + skipped.size());
        for (Entry<String, String> item : skipped.entrySet()) {
            System.out.println("\t" + item);
        }
    }
}
