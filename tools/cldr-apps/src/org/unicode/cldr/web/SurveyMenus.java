package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;

public class SurveyMenus implements Iterable<SurveyMenus.Section> {
    PathHeader.Factory phf;
    STFactory fac;
    List<Section> sections;
    SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    public SurveyMenus(STFactory stFactory, PathHeader.Factory phf) {
        fac = stFactory;
        this.phf = phf;
        
        sections = new ArrayList<Section>();
        addSection("//ldml/localeDisplayNames/scripts/script[@type=\"Arab\"]", PathUtilities.LOCALEDISPLAYNAMES_ITEMS);
        addSection("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"12\"]",
                SurveyMain.CALENDARS_ITEMS);
        addSection("//ldml/dates/timeZoneNames/metazone[@type=\"Africa_Central\"]/long/standard", // "Time Zones",
                SurveyMain.METAZONES_ITEMS);
        addSection("//ldml/posix/messages/nostr",                SurveyMain.OTHERROOTS_ITEMS);
    }

    
    @Override
    public Iterator<Section> iterator() {
        return sections.iterator();
    }
    
    private void addSection(String xpath, String[] items) {
        sections.add(new Section(xpath, items));
    }

    public class Section implements Iterable<Section.Page> {
        private String xpath;
        private String displayName;
        private List<Page> subitems;
        
        private Section(String xpath, String[] items) {
            this.xpath = xpath;
            this.displayName = phf.fromPath(xpath).getSection();
            this.subitems = new ArrayList<Page>();
            for(String item : items) {
                subitems.add(new Page(item));
            }
        }

        public String getXpath() {
            return xpath;
        }
        public String getDisplayName() {
            return displayName;
        }
        
        public class Page {
            private String pageXpath;
            private String pageXpathBase = null;
            private String pageDisplayName;
            private String pageKey;
            
            private Page(String key) {
                pageKey = key;
                
                pageDisplayName = key;
                pageXpath = null;
                
                PathHeader ph = null;
                if(displayName.equals("Calendars")) {
                    
                    pageXpathBase =  "//ldml/dates/calendars/calendar[@type=\"" + key + "\"]";
                    pageXpath =  pageXpathBase+"/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"12\"]";
                }
                
                if(pageXpath!=null) {
                    if(ph==null) {
                        ph = phf.fromPath(pageXpath);
                    }
                    if(ph!=null) {
                        pageDisplayName = ph.getPage();
                    }
                }
                
            }
            
            public String getXpath() {
                return pageXpath;
            }
            public String getDisplayName() {
                return pageDisplayName; //  + getCoverageLevel(CLDRLocale.getInstance("en"));
            }
            public String getKey() {
                return pageKey;
            }
            
            public synchronized int getCoverageLevel(CLDRLocale loc) {
                Integer ret = levs.get(loc);
                if(ret==null) {
                    if(pageXpath==null) {
                        ret = Level.COMPREHENSIVE.getValue(); // unknown
                    } else {
                        if(pageXpathBase!=null) {
                            int min = 108;
                            Set<String> paths = new HashSet<String>();
                            fac.make(loc,true).getPaths(pageXpathBase, null, paths );
                            for(String xp : paths) {
                                int l = sdi.getCoverageValue(xp, loc.toULocale());
                                if(l<min) {
                                    min=l;
                                }
                            }
                            ret = min;
                        } else {
                            ret = sdi.getCoverageValue(getXpath(), loc.toULocale());
                        }
                    }
                    levs.put(loc,ret);
                }
                return ret;
            }
            
            Map<CLDRLocale,Integer> levs = new HashMap<CLDRLocale,Integer>();
        }

        @Override
        public Iterator<Page> iterator() {
            return subitems.iterator();
        }
    }

}
