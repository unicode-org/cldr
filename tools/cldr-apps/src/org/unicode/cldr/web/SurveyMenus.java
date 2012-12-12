package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.Relation;

public class SurveyMenus implements Iterable<SurveyMenus.Section> {
    PathHeader.Factory phf;
    STFactory fac;
    List<Section> sections = new ArrayList<Section>();
    SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

    public SurveyMenus(STFactory stFactory, PathHeader.Factory phf) {
        fac = stFactory;
        this.phf = phf;

        CLDRFile b = stFactory.sm.getBaselineFile();
        phf = PathHeader.getFactory(b);
        for (String xp : b) {
            phf.fromPath(xp);
        }
        for (String xp : b.getExtraPaths()) {
            phf.fromPath(xp);
        }

        Relation<SectionId, PageId> s2p = Factory.getSectionIdsToPageIds();

        fac = stFactory;
        this.phf = phf;

        for (Entry<SectionId, Set<PageId>> q : s2p.keyValuesSet()) {
            if (q.getKey() == SectionId.Special) {
                continue; // skip special
            }
            Section newSection = new Section(q);
            if (!newSection.isEmpty()) { // empty sections have no read/write
                                         // component
                sections.add(newSection);
            }
        }
    }

    @Override
    public Iterator<Section> iterator() {
        return sections.iterator();
    }

    public class Section implements Iterable<Section.Page> {
        private SectionId sectionKey;
        private List<Page> subitems = new ArrayList<Page>();
        SurveyToolStatus status = SurveyToolStatus.HIDE;

        public SurveyToolStatus getStatus() {
            return status;
        }

        public Section(Entry<SectionId, Set<PageId>> q) {
            sectionKey = q.getKey();
            for (PageId p : q.getValue()) {
                Page pg = new Page(p);
                if (pg.getPageStatus() == SurveyToolStatus.READ_WRITE) {
                    subitems.add(pg);
                    status = pg.getPageStatus();
                }
            }
        }

        public boolean isEmpty() {
            return subitems.isEmpty();
        }

        public String toString() {
            return sectionKey.toString();
        }

        public SectionId getSection() {
            return sectionKey;
        }

        public class Page {
            private PageId pageKey;
            private SurveyToolStatus pageStatus = SurveyToolStatus.READ_WRITE;

            public SurveyToolStatus getPageStatus() {
                return pageStatus;
            }

            private Page(final PageId p) {
                pageKey = p;

                PathHeader ph = null;
                // check visibility
                Iterable<String> iter = getPagePaths();
                if (iter != null)
                    for (String xp : iter) {
                        ph = phf.fromPath(xp);
                        SurveyToolStatus xStatus = ph.getSurveyToolStatus();
                        if (xStatus == SurveyToolStatus.HIDE || xStatus == SurveyToolStatus.DEPRECATED) {
                            pageStatus = SurveyToolStatus.HIDE;
                        }
                        break;
                    }
            }

            public PageId getKey() {
                return pageKey;
            }

            public String toString() {
                return pageKey.toString();
            }

            public synchronized int getCoverageLevel(CLDRLocale loc) {
                Integer ret = levs.get(loc);
                if (ret == null) {
                    // ElapsedTimer et = new ElapsedTimer("Cov for " + loc +
                    // " "+displayName + ":"+pageDisplayName);
                    int min = Level.OPTIONAL.getLevel();
                    Iterable<String> iter = getPagePaths();
                    if (iter != null) {
                        for (String xp : iter) {
                            int l = sdi.getCoverageValue(xp, loc.toULocale());
                            if (l < min) {
                                min = l;
                            }
                        }
                    }
                    ret = min;
                    levs.put(loc, ret);
                    // System.err.println("Calc " + et);
                }
                return ret;
            }

            public Iterable<String> getPagePaths() {
                return PathHeader.Factory.getCachedPaths(sectionKey, pageKey);
            }

            Map<CLDRLocale, Integer> levs = new HashMap<CLDRLocale, Integer>();
        }

        @Override
        public Iterator<Page> iterator() {
            return subitems.iterator();
        }
    }

}
