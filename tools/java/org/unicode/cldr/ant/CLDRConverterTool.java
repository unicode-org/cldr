package org.unicode.cldr.ant;

import java.util.ArrayList;
import java.util.TreeMap;

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
     * Map of all xpaths specified and the respective preferences
     * Key: XPath
     * Value: Prefrences object
     */
    protected TreeMap xpathIncludeMap = null;
    /**
     * Map of all xpaths specified and the respective preferences
     * Key: XPath
     * Value: Prefrences object
     */
    protected TreeMap xpathExcludeMap = null;  
    
    /**
     * Object that holds information about preferences on the
     * <include> elements.
     * @author ram
     *
     */
    public static class Preferences{
        String preferAlt;
        String draft;
        public Preferences(String preferAlt, String draft){
            this.preferAlt = preferAlt;
            this.draft = draft;
        }
    }
    public static class Alias{
        public String to;
        public String xpath;
        public Alias(String to, String xpath){
            this.to = to;
            this.xpath = xpath;
        }
    }
    /**
     * Process the argsuments
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
    
    public void setLocalesMap(TreeMap map){
        localesMap = map;
    }
    
    public void setXPathIncludeMap(TreeMap map){
        xpathIncludeMap = map;
    }
    public void setXPathExcludeMap(TreeMap map){
        xpathExcludeMap = map;
    }
}
