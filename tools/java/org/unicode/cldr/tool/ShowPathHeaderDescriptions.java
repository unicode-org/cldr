package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.UnicodeSet;

public class ShowPathHeaderDescriptions {
    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCommonAndSeedAndMainAndAnnotationsFactory();
        CLDRFile english = factory.make("en", true);
        PathHeader.Factory phf = PathHeader.getFactory(english);

        String localeToTest = "cs";
        
        CLDRFile localeFile = factory.make(localeToTest, true);
        Multiset<String> sectionPageHeader = LinkedHashMultiset.create();
        Multiset<String> sectionPage = LinkedHashMultiset.create();
        Set<PathHeader> pathHeaders = new TreeSet<>();
        UnicodeSet emoji = new UnicodeSet("[:emoji:]");
        
        for (String path : localeFile.fullIterable()) {
            if (emoji.containsSome(path)) {
                
            }
            PathHeader pathHeader = phf.fromPath(path);
            if (pathHeader.getSectionId() == SectionId.Characters) {
                System.out.println(pathHeader);
            }
            Level level = config.getSupplementalDataInfo().getCoverageLevel(path, localeToTest);
            if (level.compareTo(Level.MODERN) > 0) {
                continue;
            }
            pathHeaders.add(pathHeader);
        }
        for (PathHeader pathHeader : pathHeaders) {
            String base = pathHeader.getSectionId() + "\t" + pathHeader.getPageId();
            sectionPage.add(base);
            String item = base + "\t" + pathHeader.getHeader();
            sectionPageHeader.add(item);
        }
        int i = 0;
        for (Multiset.Entry<String> entry : sectionPage.entrySet()) {
            System.out.println(++i + "\t" + entry.getElement() + "\t" + entry.getCount());
        }
        i = 0;
        for (Multiset.Entry<String> entry : sectionPageHeader.entrySet()) {
            System.out.println(++i + "\t" + entry.getElement() + "\t" + entry.getCount());
        }
    }
    public static void showDescriptions(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        CLDRFile english = config.getEnglish();
        PathHeader.Factory phf = PathHeader.getFactory(english);
        PathDescription pathDescriptionFactory = new PathDescription(config.getSupplementalDataInfo(), english, null, null,
            PathDescription.ErrorHandling.CONTINUE);

        // where X, *, * => single value, write it as such

        Multimap<SectionId, String> sv = TreeMultimap.create();
        Multimap<PageId, String> pv = TreeMultimap.create();
        Multimap<String, String> hv = TreeMultimap.create();

        Multimap<R2<SectionId, PageId>, String> spv = TreeMultimap.create();
        Multimap<R2<SectionId, String>, String> shv = TreeMultimap.create();
        Multimap<R2<PageId, String>, String> phv = TreeMultimap.create();

        Multimap<R3<SectionId, PageId, String>, String> sphv = TreeMultimap.create();
        Multimap<String, R3<SectionId, PageId, String>> valueToKey = TreeMultimap.create();

        Set<String> urls = new TreeSet<>();

        for (String path : english) {
            PathHeader pathHeader = phf.fromPath(path);
            String pdx = pathDescriptionFactory.getRawDescription(path, "VALUE", null);
            String url;
            if (pdx == null) {
                url = "NONE";
            } else {
                url = extractUrl(pdx);
            }
            urls.add(url);

            SectionId sectionId = pathHeader.getSectionId();
            PageId pageId = pathHeader.getPageId();
            String header = pathHeader.getHeader();
            int usingPos = header.indexOf(" using ");
            if (usingPos > 0) {
                header = header.substring(0, usingPos) + " using * Digits";
            }
            if (header.startsWith("Append-Fallback-")) {
                header = "Append-Fallback-*(calendar)";
            }
            sv.put(sectionId, url);
            pv.put(pageId, url);
            hv.put(header, url);

            spv.put(Row.of(sectionId, pageId), url);
            shv.put(Row.of(sectionId, header), url);
            phv.put(Row.of(pageId, header), url);

            R3<SectionId, PageId, String> full = Row.of(sectionId, pageId, header);
            sphv.put(full, url);
            valueToKey.put(url, full);
        }

        Set<String> done = new HashSet<>();

        process(SphType.s, sv, done);
        process(SphType.p, pv, done);
        process(SphType.h, hv, done);

        showProgress(done, valueToKey);

        process(SphType.sp, spv, done);
        process(SphType.sh, shv, done);
        process(SphType.ph, phv, done);

        process(SphType.sph, sphv, done);

        System.out.println(CollectionUtilities.join(urls, "\n"));
    }

    private static void showProgress(Set<String> done, Multimap<String, R3<SectionId, PageId, String>> valueToKey) {
        TreeSet<String> temp = new TreeSet<>(valueToKey.keys());
        temp.removeAll(done);
        System.out.println(temp);
    }

    enum SphType {
        s, p, h, sp, sh, ph, sph
    }

    private static <K> void process(SphType type, Multimap<K, String> sv, Set<String> done) {
        Set<String> newDone = new HashSet<>();
        for (Entry<K, Collection<String>> item : sv.asMap().entrySet()) {
            Set<String> remaining = new TreeSet<>();
            remaining.addAll(item.getValue());
            remaining.removeAll(done);
            if (remaining.size() == 1) {
                String value = remaining.iterator().next();
                if (type == type.s || type == type.p || type == type.sp) {
                    done.add(value);
                } else {
                    newDone.add(value);
                }
                System.out.println(getThree(type, item.getKey()) + "\t" + value);
            }
        }
        done.addAll(newDone);
    }

    private static <K> String getThree(SphType type, K k) {
        switch (type) {
        case s:
            return k + "\t" + "*" + "\t" + "*";
        case p:
            return "*\t" + k + "\t*";
        case h:
            return "*\t*\t" + k;
        case sp:
            return ((R2) k).get0() + "\t" + ((R2) k).get1() + "\t" + "*";
        case sh:
            return ((R2) k).get0() + "\t" + "*" + "\t" + ((R2) k).get2();
        case ph:
            return "*" + "\t" + ((R2) k).get1() + "\t" + ((R2) k).get2();
        case sph:
            return ((R3) k).get0() + "\t" + ((R3) k).get1() + "\t" + ((R3) k).get2();
        default:
            throw new IllegalArgumentException();
        }
    }

    private static String extractUrl(String pd) {
        String value = pd.replaceAll("^.*(http://.*)$", "$1");
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith(" for details")) {
            value = value.substring(0, value.length() - " for details".length());
        }
        return value;
    }

}
