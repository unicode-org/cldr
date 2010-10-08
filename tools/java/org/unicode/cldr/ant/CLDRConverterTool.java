package org.unicode.cldr.ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.Task;

import org.unicode.cldr.ant.CLDRBuild.Paths;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.icu.ResourceSplitter.SplitInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CoverageLevel;
import org.w3c.dom.Node;

/**
 * All tools that would like to make use of CLDR Build process
 * through the ant plug-in should extend this class. For implementing
 * the processArgs method basically move the implementation of main into
 * this method and add code to deal with situation where localesMap field
 * is set, see {@link org.unicode.cldr.icu.LDML2ICUConverter#processArgs(String[])}.
 * The subclasses are also expected to invoke computeConvertibleXPaths method
 * for all the xpaths in the file that they are currently processing and at
 * every leaf node should verify if an XPath is convertible or not. Please see
 * {@link org.unicode.cldr.icu.LDML2ICUConverter#isNodeNotConvertible(Node, StringBuilder)}.
 *
 * @author ram
 *
 */
public abstract class CLDRConverterTool {
    /**
     * Information from the deprecates build rules.
     */
    protected AliasDeprecates aliasDeprecates;

    /**
     * Map of locales that need to processed.
     * Key : locale name
     * Value: draft attribute
     */
    private Map<String, String> localesMap;
    
    private Set<String> includedLocales;

    /**
     * List of xpaths to include or exclude
     */
    protected List<Task> pathList;

    /**
     * Override fallbacks list
     */
    protected List<CLDRBuild.Paths> overrideFallbackList;

    /**
     * Information used by ResourceSplitter, if not null.
     */
    protected List<SplitInfo> splitInfos;
    
    /**
     * Object that holds information about aliases on the
     * <alias from="in" to="id" />  elements.
     * @author ram
     *
     */
    public static class Alias {
        public final String from;
        public final String to;
        public final String xpath;

        public Alias(String from, String to, String xpath) {
            this.from = from;
            this.to = to;
            this.xpath = xpath;
        }
    }
    
    public static class AliasDeprecates {
      public final List<Alias> aliasList;
      public final List<String> aliasLocaleList;
      public final List<String> emptyLocaleList;
      
      public AliasDeprecates(List<Alias> aliasList, List<String> aliasLocaleList,
          List<String> emptyLocaleList) {
        this.aliasList = aliasList;
        this.aliasLocaleList = aliasLocaleList;
        this.emptyLocaleList = emptyLocaleList;
      }
    }

    /**
     * Process the arguments
     * @param args
     */
    public abstract void processArgs(String[] args);

    /**
     * For support and interpretation of
     *      <deprecates>
     *          <alias from="no_NO_NY" to="nn_NO" />
     *          <alias from="en_RH" to="en_ZW" />
     *          <aliasLocale locale="zh_SG" />
     *          <aliasLocale locale="zh_TW" />
     *          <emptyLocale locale="hi_" />
     *          <emptyLocale locale="zh_" />
     *      </deprecates>
     */
    public void setAliasDeprecates(AliasDeprecates aliasDeprecates) {
        this.aliasDeprecates = aliasDeprecates;
    }

    /**
     *
     * @param map
     */
    public void setLocalesMap(Map<String, String> map){
        localesMap = map;
    }

    public void setIncludedLocales(Set<String> set){
        includedLocales = set;
    }
    /**
     * Sets the list of objects that contain information in
     * include and exclude elements
     *
     *   <include xpath="//ldml/.* /dateTimeElements/.*" draft=".*"/>
     *   <exclude xpath="//ldml/.* /language.*" preferAlt="proposed" draft=".*"/>
     * @param list
     */
    public void setPathList(List<Task> list){
        pathList = list;
    }

    /**
     * Set the fallback override list
     */
    public void setOverrideFallbackList(List<Paths> list){
//        overrideFallbackList = list;
    }

    public void setSplitInfos(List<SplitInfo> infos) {
      this.splitInfos = Collections.unmodifiableList(infos);
    }

    protected Node mergeOverrideFallbackNodes(Node main, String locale){
//        for (int i = 0; i < overrideFallbackList.size(); i++) {
//            CLDRBuild.Paths path = overrideFallbackList.get(i);
//            if (CLDRBuild.matchesLocale(path.locales, locale)){
//                //TODO write the merging algorithm
//            }
//        }
        return main;
    }

    private CoverageLevel coverageLevel = null;

    private void initCoverageLevel(
        String localeName, boolean exemplarsContainA_Z, String supplementalDir) {
        if (coverageLevel == null) {
            CLDRFile sd = CLDRFile.make(CLDRFile.SUPPLEMENTAL_NAME, supplementalDir, true);
            CLDRFile smd = CLDRFile.make(CLDRFile.SUPPLEMENTAL_METADATA, supplementalDir, true);
            coverageLevel = new CoverageLevel();
            CoverageLevel.init(sd, smd);
            ArrayList<CheckStatus> errors = new ArrayList<CheckStatus>();
            coverageLevel.setFile(localeName, exemplarsContainA_Z, false, null, null, errors);
        }
    }
    /**
     * Computes the convertible xpaths by walking through the xpathList given and applying the rules
     * in children of <path> elements.
     * @param xpathList A sorted list of all xpaths for the current run
     * @param localeName The name of locale being processed
     * @return an ArrayList of the computed convertible xpaths
     */
    protected List<String> computeConvertibleXPaths(
        List<String> xpathList, boolean exemplarsContainA_Z, String localeName,
        String supplementalDir) {
        /*
         * Assumptions:
         * 1. Vetted nodes do not have draft attribute
         * 2. Nodes with draft attribute set and alt atrribute not set do not have a vetted
         *    counterpart
         * 3. Nodes with alt attribute may or may not have a draft attribute
         * 4. If no draft field is set in the preferences object assume vetted node is requested
         */

        //fast path
        String draft = getLocalesMap() == null ? null : getLocalesMap().get(localeName + ".xml");
        XPathParts parts = new XPathParts(null, null);
        if (draft != null){
            for (int i = 0; i < xpathList.size(); i++){
                parts = parts.set(xpathList.get(i));
                Map<String, String> attr = parts.getAttributes(parts.size()-1);
                String draftVal = attr.get(LDMLConstants.DRAFT);
                String altVal = attr.get(LDMLConstants.ALT);
                if (draftVal != null && !draftVal.matches(draft)) {
                    xpathList.remove(i);
                }
                // remove xpaths with alt attribute set
                if (altVal != null) {
                    xpathList.remove(i);
                }
            }
            return xpathList;
        }

        if (pathList == null) {
            // include everything!
            return xpathList;
        }

        ArrayList<String> myXPathList = new ArrayList<String>(xpathList.size());
        StandardCodes sc = StandardCodes.make();
        // iterator of xpaths of the current CLDR file being processed
        // this map only contains xpaths of the leaf nodes
        for(int i=0; i<xpathList.size();i++){
            String xpath = xpathList.get(i);
            parts = parts.set(xpath);
            Map<String, String> attr = parts.getAttributes(parts.size() - 1);

            boolean include = false;
            for (int j = 0; j < pathList.size(); j++){
                Object obj = pathList.get(j);
                if (obj instanceof CLDRBuild.CoverageLevel) {
                  initCoverageLevel(localeName, exemplarsContainA_Z, supplementalDir);
                  CLDRBuild.CoverageLevel level = (CLDRBuild.CoverageLevel) obj;
                  if (level.locales != null) {
                    List<String> localeList = Arrays.asList(level.locales.split("\\s+"));
                    if (CLDRBuild.matchesLocale(localeList, localeName) == false) {
                      continue;
                    }
                  }

                  //process further only if the current locale is part of the given group and org
                  if (level.group != null
                      && !sc.isLocaleInGroup(localeName, level.group, level.org)) {
                    continue;
                  }

                  CoverageLevel.Level cv = CoverageLevel.Level.get(level.level);
                  // only include the xpaths that have the coverage level at least the coverage
                  // level specified by the locale
                  if (coverageLevel.getCoverageLevel(xpath).compareTo(cv) <= 0) {
                    String draftVal = attr.get(LDMLConstants.DRAFT);
                    if (level.draft != null) {
                      if (draftVal == null
                          && (level.draft.equals("false") || level.draft.equals(".*"))) {
                        include = true;
                      } else if (draftVal != null && draftVal.matches(level.draft)) {
                        include = true;
                      } else {
                        include = false;
                      }
                    } else {
                      if (draftVal == null) {
                        include = true;
                      }
                    }
                  }
                } else if (obj instanceof CLDRBuild.Exclude) {
                    CLDRBuild.Exclude exc = (CLDRBuild.Exclude) obj;
                    //fast path if locale attribute is set
                    if (exc.locales != null
                        && CLDRBuild.matchesLocale(exc.locales, localeName) == false) {
                        continue;
                    }
                    if (exc.xpath != null && xpath.matches(exc.xpath)) {
                        /*
                         * Now starts struggle for figuring out which xpaths should be excluded
                         * The following cases need to be handled:
                         * 1. <exclude xpath="//ldml/localeDisplayNames/languages/.*" draft="false">
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *          include = true
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = false
                         * 2. <exclude xpath="//ldml/localeDisplayNames/languages/.*" draft="true">
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *          include = false
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test'] then
                         *          include = true
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = true
                         * 3. <exclude xpath="//ldml/localeDisplayNames/languages/.*" draft=".*">
                         *      if      xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true']
                         *          or  xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test']
                         *          or  xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = false
                         * 4. <exclude xpath="//ldml/localeDisplayNames/languages/.*" draft="false" preferAlt='true'>
                         *      if xp of //ldml/localeDisplayNames/languages/language[@type='en' alt='.*'] exists then
                         *          if      xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true']
                         *              or  xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test']
                         *              or  xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *              include = true
                         *          else
                         *              apply rules for processing draft and alt attribute together.
                         *      else
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *              include = false
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test'] then
                         *              include = true
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *              include = true
                         */
                        String draftVal = attr.get(LDMLConstants.DRAFT);
                        String altVal = attr.get(LDMLConstants.ALT);
                        boolean altExc = false, draftExc = false;
                        if (exc.alt == null && altVal == null) {
                            altExc = true;
                        } else if (exc.alt == null && altVal != null) {
                            altExc = true;
                        } else if (exc.alt != null && altVal == null) {
                            altExc = false;
                        } else {
                            if (altVal.matches(exc.alt)) {
                                altExc = true;
                            }
                        }
                        if (exc.draft == null && draftVal == null) {
                            draftExc = true;
                        }else if (exc.draft != null && draftVal == null) {
                            if ((exc.draft.equals("false") || exc.draft.equals(".*"))) {
                                draftExc = true;
                            }
                        } else if (exc.draft == null && draftVal != null) {
                            draftExc = false;
                        } else {
                            if (draftVal.matches(exc.draft)) {
                                draftExc = true;
                            }
                        }
                        if (altExc == true && draftExc == true) {
                            include = false;
                        }
                    }
                } else if (obj instanceof CLDRBuild.Include) {
                    CLDRBuild.Include inc = (CLDRBuild.Include) obj;
                    //fast path if locale attribute is set
                    if (inc.locales != null
                        && CLDRBuild.matchesLocale(inc.locales, localeName) == false) {
                        continue;
                    }
                    if (inc.xpath != null && xpath.matches(inc.xpath)) {
                        /*
                         * The following cases need to be handled:
                         * 1. <include xpath="//ldml/localeDisplayNames/languages/.*" draft="false">
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *          include = false
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = true
                         * 2. <include xpath="//ldml/localeDisplayNames/languages/.*" draft="true">
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *          include = true
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test'] then
                         *          include = false
                         *      if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = false
                         * 3. <include xpath="//ldml/localeDisplayNames/languages/.*" draft=".*">
                         *      if      xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true']
                         *          or  xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test']
                         *          or  xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *          include = true
                         * 4. <include xpath="//ldml/localeDisplayNames/languages/.*" draft="false" preferAlt='true'>
                         *      if xp of //ldml/localeDisplayNames/languages/language[@type='en' alt='.*'] exists then
                         *          if      xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true']
                         *              or  xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test']
                         *              or  xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *              include = false
                         *          else
                         *              apply rules for processing draft and alt attribute together.
                         *      else
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en'] then
                         *              include = true
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='test'] then
                         *              include = false
                         *          if xp is //ldml/localeDisplayNames/languages/language[@type='en' and draft='true'] then
                         *              include = false
                         */
                        String draftVal = attr.get(LDMLConstants.DRAFT);
                        String altVal = attr.get(LDMLConstants.ALT);
                        boolean altInc = false;
                        if (inc.alt == null && altVal == null) {
                            altInc = true;
                        } else if (inc.alt == null && altVal != null) {
                             altInc = false;
                        } else if (inc.alt != null && altVal == null) {
                            // the current xpath does not have the alt attribute set
                            // since the list is sorted we can be sure that if the
                            // next xpath matches the the current one then additional
                            // alt attribute should be set
                            // now check if next xpath contains alt attribute
                            String nxp = xpathList.get(i+1);
                            XPathParts nparts = (new XPathParts(null, null)).set(nxp);
                            Map<String, String> nattr = nparts.getAttributes(parts.size()-1);
                            // make sure the type attribute is the same
                            if (parts.isLike(nparts)) {
                                altVal = nattr.get(LDMLConstants.ALT);
                                if (altVal.matches(inc.alt)) {
                                    draftVal = nattr.get(LDMLConstants.DRAFT);
                                    xpath = nxp;
                                    i++;
                                    altInc = true;
                                }
                            }
                        } else {
                            if (altVal.matches(inc.alt)) {
                                altInc = true;
                            }
                        }
                        boolean draftInc = false;
                        if (inc.draft == null && draftVal == null) {
                            draftInc = true;
                        } else if (inc.draft != null && draftVal == null) {
                            if ((inc.draft.equals("false") || inc.draft.equals(".*"))) {
                                draftInc = true;
                            }
                        } else if (inc.draft == null && draftVal != null) {
                            draftInc = false;
                        } else {
                            if (draftVal.matches(inc.draft)) {
                                draftInc = true;
                            }
                        }
                        if (altInc == true && draftInc == true) {
                            include = true;
                        }
                    }
                } else {
                    System.err.println(
                        "ERROR: computeConvertibleXPath method cannot handle object of type: "
                        + obj.getClass().toString());
                }
            }
            if (include == true) {
                myXPathList.add(xpath);
            }
        }

        return myXPathList;
    }

    protected Map<String, String> getLocalesMap() {
        return localesMap;
      }
    
    protected Set<String> getIncludedLocales() {
        return includedLocales;
      }
}
