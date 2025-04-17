package org.unicode.cldr.web;

import com.ibm.icu.impl.Relation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.web.SurveyMenus.Section.Page;
import org.unicode.cldr.web.util.JSONArray;
import org.unicode.cldr.web.util.JSONException;
import org.unicode.cldr.web.util.JSONObject;

public class SurveyMenus implements Iterable<SurveyMenus.Section> {
    PathHeader.Factory phf;
    STFactory fac;
    List<Section> sections = new ArrayList<>();

    public SurveyMenus(STFactory stFactory, PathHeader.Factory phf) {
        fac = stFactory;
        this.phf = phf;

        CLDRFile b = stFactory.sm.getEnglishFile();
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
        private final SectionId sectionKey;
        private final List<Page> subitems = new ArrayList<>();
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

        @Override
        public String toString() {
            return sectionKey.toString();
        }

        public SectionId getSection() {
            return sectionKey;
        }

        public class Page {
            private final PageId pageKey;
            private SurveyToolStatus pageStatus = SurveyToolStatus.READ_WRITE;

            public SurveyToolStatus getPageStatus() {
                return pageStatus;
            }

            private Page(final PageId p) {
                pageKey = p;

                PathHeader ph;
                // check visibility
                Iterable<String> iter = getPagePaths();
                if (iter != null)
                    for (String xp : iter) {
                        ph = phf.fromPath(xp);
                        SurveyToolStatus xStatus = ph.getSurveyToolStatus();
                        if (xStatus == SurveyToolStatus.HIDE
                                || xStatus == SurveyToolStatus.DEPRECATED) {
                            pageStatus = SurveyToolStatus.HIDE;
                        }
                        break;
                    }
            }

            public PageId getKey() {
                return pageKey;
            }

            @Override
            public String toString() {
                return pageKey.toString();
            }

            public synchronized int getCoverageLevel(CLDRLocale loc) {
                Integer ret = levs.get(loc);
                if (ret == null) {
                    int min = Level.OPTIONAL.getLevel();
                    Iterable<String> iter = getPagePaths();
                    if (iter != null) {
                        CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
                        for (String xp : iter) {
                            int l = covInfo.getCoverageValue(xp, loc.getBaseName());
                            if (l < min) {
                                min = l;
                            }
                        }
                    }
                    ret = min;
                    levs.put(loc, ret);
                }
                return ret;
            }

            public Iterable<String> getPagePaths() {
                return PathHeader.Factory.getCachedPaths(sectionKey, pageKey);
            }

            Map<CLDRLocale, Integer> levs = new HashMap<>();
        }

        @Override
        public Iterator<Page> iterator() {
            return subitems.iterator();
        }
    }

    public JSONObject toJSON(CLDRLocale forLoc) throws JSONException {
        JSONArray sectionsJ = new JSONArray();

        for (Section s : sections) {
            JSONObject sectionJ =
                    new JSONObject()
                            .put("id", s.getSection().name())
                            .put("name", s.getSection().toString())
                            .put("status", s.getStatus());

            JSONArray pagesJ = new JSONArray();

            for (Page p : s) {
                JSONObject pageJ =
                        new JSONObject()
                                .put("id", p.getKey().name())
                                .put("name", p.getKey().toString());
                if (forLoc != null) {
                    pageJ.put(
                            "levs",
                            new JSONObject()
                                    .put(
                                            forLoc.getBaseName(),
                                            Integer.toString(p.getCoverageLevel(forLoc))));
                }
                pagesJ.put(pageJ);
            }

            sectionJ.put("pages", pagesJ);

            sectionsJ.put(sectionJ);
        }

        JSONObject ret = new JSONObject().put("sections", sectionsJ).put("levels", levelsJSON());
        if (forLoc != null) {
            ret.put("loc", forLoc.getBaseName());
        }
        return ret;
    }

    /**
     * TODO: move this into Level, once JSON libs are integrated into core
     *
     * @return
     * @throws JSONException
     */
    private static JSONObject levelsJSON() throws JSONException {
        JSONObject levels = new JSONObject();
        for (Level l : Level.values()) {
            levels.put(
                    l.name(),
                    new JSONObject()
                            .put("name", l.toString())
                            .put("level", Integer.toString(l.getLevel())));
        }
        return levels;
    }
}
