package org.unicode.cldr.ant;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.XPathParts;

/**
 * All tools thtat would like to make use of CLDR Build process 
 * throought the ant plugin should implement this interface.
 * @author ram
 *
 */
public abstract class CLDRConverterTool {
    /**
     * List of locales that are aliases to other locales 
     * support of %%Alias
     */
    protected ArrayList aliasLocaleList    = null;
    
    /**
     * Empty localeas list for deprecated locales list
     */
    protected ArrayList emptyLocaleList    = null;
    
    /** 
     * Map of alias locales
     * Key: from locale
     * Value: to locale
     */
    protected TreeMap   aliasMap           = null;
    /**
     * Map of locales that need to processed.
     * Key : locale name
     * Value: draft attribute
     */
    protected TreeMap   localesMap          = null;
    /**
     * List of xpaths to include or exclude
     * 
     */     
    protected Vector pathList = null;
    
    /**
     * Object that holds information about aliases on the
     * <alias from="in" to="id" />  elements.
     * @author ram
     *
     */
    public static class Alias{
        public String to;
        public String xpath;
        public Alias(String to, String xpath){
            this.to = to;
            this.xpath = xpath;
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
     *      </deprecates> 
     * @param type The type of for aliases. Usually is the name of the tree
     * @param list The map of locales for which the alias locales need to be written.
     *              From locale name is the key and to locale is the value in the map
     */
    public void setAliasMap(TreeMap map){
        aliasMap = map;
    }
    
    /**
     * For support and interpretation of
     *      <deprecates>
     *          <aliasLocale locale="zh_SG" /> 
     *          <aliasLocale locale="zh_TW" />
     *      </deprecates> 
     * @param type The type of for aliases. Usually is the name of the tree
     * @param list The list of locales for which the alias locales need to be written.
     */ 
    public void setAliasLocaleList(ArrayList list){
        aliasLocaleList = list;
    }
    
    /**
     * For support and interpretation of
     *      <deprecates>
     *          <emptyLocale locale="hi_" />
     *          <emptyLocale locale="zh_" />  
     *      </deprecates> 
     * @param type The type of for aliases. Usually is the name of the tree
     * @param list The list of locales for which the empty locales need to be written.
     */     
    public void setEmptyLocaleList(ArrayList list){
        emptyLocaleList = list;
    }
    /**
     * 
     * @param map
     */
    public void setLocalesMap(TreeMap map){
        localesMap = map;
    }
    /**
     * Sets the list of objects that contain information in 
     * include and exclude elements
     * 
     *   <include xpath="//ldml/.* /dateTimeElements/.*" draft=".*"/>
     *   <exclude xpath="//ldml/.* /language.*" preferAlt="proposed" draft=".*"/>
     * @param list
     */
    public void setPathList(Vector list){
        pathList = list;
    }
    /**
     * Computes the convertible xpaths by walking through the xpathList given and applying the rules
     * in children of <path> elements. 
     * @param xpathList
     * @param localeName
     * @return the computed convertible xpaths
     */
    protected ArrayList computeConvertibleXPaths(ArrayList xpathList, String localeName){
        /*
         * Assumptions:
         * 1. Vetted nodes do not have draft attribute
         * 2. Nodes with draft attribute set and alt atrribute not set do not have a vetted counterpart
         * 3. Nodes with alt attribute may or may not have a draft attribute
         * 4. If no draft field is set in the preferences object assume vetted node is requested 
         *
         *  
         */

        XPathParts parts = new XPathParts(null, null);
        ArrayList myXPathList = new ArrayList(xpathList.size());
        
        // iterator of xpaths of the current CLDR file being processed
        // this map only contains xpaths of the leaf nodes
        for(int i=0; i<xpathList.size();i++){
            String xpath = (String) xpathList.get(i);
            parts = parts.set(xpath);
            
            boolean include = false;
            for(int j=0; j<pathList.size(); j++){
                Object obj = pathList.get(j);
                if(obj instanceof CLDRBuild.Exclude){
                    CLDRBuild.Exclude exc = (CLDRBuild.Exclude)obj;
                    //fast path if locale attribute is set
                    if(exc.locale!=null && exc.locale.indexOf(localeName)<0){
                        continue;
                    }
                    if(exc.xpath!=null && xpath.matches(exc.xpath)){
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
                        Map attr = parts.getAttributes(parts.size()-1);
                        String draftVal = (String)attr.get(LDMLConstants.DRAFT);
                        String altVal = (String)attr.get(LDMLConstants.ALT);
                        if(exc.preferAlt!=null){
                            // the current xpath does not have the alt attribute set
                            // since the list is sorted we can be sure that if the
                            // next xpath matches the the current one then additional
                            // alt attribute should be set
                            if(altVal==null){
                                // now check if next xpath contains alt attribute
                                String nxp = (String) xpathList.get(i+1);
                                XPathParts nparts = (new XPathParts(null, null)).set(nxp);
                                Map nattr = nparts.getAttributes(parts.size()-1);
                                // make sure the type attribute is the same
                                if(parts.isLike(nparts)){
                                    altVal = (String)nattr.get(LDMLConstants.ALT);
                                    if(altVal.matches(exc.preferAlt)){
                                        draftVal = (String)nattr.get(LDMLConstants.DRAFT);
                                        xpath = nxp;
                                        i++;
                                    }
                                }
                            }else{
                                if(altVal.matches(exc.preferAlt)){
                                    include = false;
                                }
                            }
                        }
                        if(exc.draft!=null){
                            if(   draftVal==null && 
                                 (exc.draft.equals("false")|| exc.draft.equals(".*")) 
                               ){
                                include = false;
                            }else if(draftVal!=null && draftVal.matches(exc.draft)){
                                include = false;
                            }else{
                                include = true;
                            }
                        }
                    } 
                }else if(obj  instanceof CLDRBuild.Include){
                    CLDRBuild.Include inc = (CLDRBuild.Include)obj;
                    //fast path if locale attribute is set
                    if(inc.locale!=null && inc.locale.indexOf(localeName)<0){
                        continue;
                    }
                    if(inc.xpath!=null && xpath.matches(inc.xpath)){
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
                        Map attr = parts.getAttributes(parts.size()-1);
                        String draftVal = (String)attr.get(LDMLConstants.DRAFT);
                        String altVal = (String)attr.get(LDMLConstants.ALT);
                        if(inc.preferAlt!=null){
                            // the current xpath does not have the alt attribute set
                            // since the list is sorted we can be sure that if the
                            // next xpath matches the the current one then additional
                            // alt attribute should be set
                            if(altVal==null){
                                // now check if next xpath contains alt attribute
                                String nxp = (String) xpathList.get(i+1);
                                XPathParts nparts = (new XPathParts(null, null)).set(nxp);
                                Map nattr = nparts.getAttributes(parts.size()-1);
                                // make sure the type attribute is the same
                                if(parts.isLike(nparts)){
                                    altVal = (String)nattr.get(LDMLConstants.ALT);
                                    if(altVal.matches(inc.preferAlt)){
                                        draftVal = (String)nattr.get(LDMLConstants.DRAFT);
                                        xpath = nxp;
                                        i++;
                                    }
                                }
                            }else{
                                if(altVal.matches(inc.preferAlt)){
                                    include = true;
                                }
                            }
                        }
                        if(inc.draft!=null){
                            if(   draftVal==null && 
                                 (inc.draft.equals("false")|| inc.draft.equals(".*")) 
                               ){
                                include = true;
                            }else if(draftVal!=null && draftVal.matches(inc.draft)){
                                include = true;
                            }else{
                                include = false;
                            }
                        }
                    }       
                }else{
                    System.err.println("ERROR: computeConvertibleXPath method cannot handle object of type: "+ obj.getClass().toString());
                }
            }
            if(include==true){
                myXPathList.add(xpath);
            }
        }
        return myXPathList;
    }
}
