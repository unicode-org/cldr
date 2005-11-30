package org.unicode.cldr.ant;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ibm.icu.dev.tool.UOption;

public class CLDRBuild extends Task{
    private String toolName;
    private String srcFile;
    private String destFile;
    private boolean noArgs = false;
    private Vector runs= new Vector();

    private UOption srcDir = UOption.SOURCEDIR();
    private UOption destDir = UOption.DESTDIR();
    
    private static final String ALL  = ".*";
    private static final char FILE_EXT_SEPARATOR = '.';
    public class Filter implements FileFilter{
        private String filePattern;
        public Filter(String fp){
            filePattern = fp;
        }
        public Filter(){         
        }
        public boolean accept(File pathname){
            if(filePattern != null && pathname.getName().matches(filePattern)){
                return true;
            }
            return false;
        }
    }
    public TreeMap getLocalesList(Config config, String src, String dest){
        File srcdir = new File(src);
        File[] srcFiles = srcdir.listFiles(new Filter(srcFile));
        File destdir = new File(dest);
        File[] destFiles = destdir.listFiles(new Filter(destFile));
        TreeMap ret = new TreeMap();
        
        if(config !=null){
            Locales locs = config.getLocales();
            Vector include = locs.getIncludeList();
            Vector exclude = locs.getExcludeList();
            // walk down the include list and 
            // add the stuff to return list 
            for(int i=0; i<include.size(); i++){
                Include inc = (Include)include.get(i);
                //fast path for .*
                if(inc.locale.equals(ALL)){
                    for(int j=0; j<srcFiles.length;j++){
                        ret.put(srcFiles[j].getName(), inc.draft);
                    }
                    continue;
                }
                // now make sure the files that
                // the files really exist
                boolean filesMissing = true;
                for(int j=0; j<srcFiles.length; j++){
                    String fileName = srcFiles[j].getName(); 
                    if(fileName.matches(inc.locale)|| (fileName.indexOf(inc.locale)==0)){
                        ret.put(fileName, inc.draft);
                        filesMissing = false;
                        break;
                    }
                }
                if(filesMissing){
                    //errln("Files in include list do not exist. Please check the config file!", true);
                }
            }
            // now walk down the exlude list and start removing
            // files from the working list
            for(int i=0; i<exclude.size(); i++){
                Exclude exc = (Exclude)exclude.get(i);
                //fast path for .*
                if(exc.locale.equals(ALL)){
                    if(exclude.size()>1){
                        errln("Exclude definition excludes all files and more. Please check the config.", true);
                    }
                    ret.clear();
                    continue;
                }
                ret.remove(exc.locale);
            }
        }else{
            if(srcFiles.length==1 && destFiles.length==1){
                ret.put(srcFiles[0].getName(),".*");
            }
        }
        // only build the files that need to be built
        for(int i=0; i<destFiles.length; i++){
            String destName = destFiles[i].getName();
            long destMod = destFiles[i].lastModified();
            destName = destName.substring(0, destName.indexOf(FILE_EXT_SEPARATOR)+1);
            if(srcFile!=null){
                destName = destName + getFileExtension(srcFile);
                // the  arrays are sorted so we save the 
                // index from last run to find the next match
                // we assume that we are running on an ASCII machine
                int save=0;
                for(int j=save; j<srcFiles.length; j++){
                    String srcName = srcFiles[j].getName();
                    if(srcName.matches(destName)){
                        save=j; 
                        if(destMod > srcFiles[j].lastModified()){
                            ret.remove(srcName);
                        }
                    }
                } 
            }else{
                if(destFiles[i].exists()){
                    ret.remove(destFile);
                }
            }
           
        }
        if(ret.size()==0 && destFiles.length == 1){
            return null;
        }
        return ret;

    }
    private String getFileExtension(String name){
        // a regular expression
        int i = name.indexOf('*');
        if(i>-1){
            return name.substring(i+1,name.length());
        }
        i = name.indexOf('.');
        if(i>-1){
            return name.substring(i+1,name.length());
        }
        return name;
    }
    public static void errln( String msg, boolean exit){
        System.err.println("ERROR: " + msg);
        if(exit){
            System.exit(-1);
        }
    }
    public static void warnln(String msg){
        System.out.println("WARNING: " + msg);
    }
    public static void infoln(String msg){
        System.out.println("INFO: " + msg);
    }
    
    private String getDirString(Hashtable runArgs, UOption key){
        String value = (String)runArgs.get("--" + key.longName);
        if(value==null){
            value = (String)runArgs.get("-" + String.valueOf(key.shortName));
        }
        return value;
    }
    //The method executing the task
    public void execute() throws BuildException {
        if (toolName==null) {
            throw new BuildException("Tool name not set");
        }
        //System.out.println("Ok loaded CLDRBuild! ToolName: "+ toolName);
        
        for(Iterator iter = runs.iterator(); iter.hasNext(); ){
            Run run = (Run)iter.next();
            Config config = run.getConfig();
            Hashtable runArgs = run.getArgs(null);
            
            TreeMap localesMap = getLocalesList(config, getDirString(runArgs, srcDir), getDirString(runArgs, destDir));
            
            if(localesMap==null){
                continue;
            }
            if(localesMap.size()==0 && noArgs==false){
                continue;
            }
            String[] args = new String[runArgs.size()*2];
            
            // create the args list
            int i=0;
            StringBuffer printArgs = new StringBuffer();
            for(Iterator iter1 = runArgs.keySet().iterator(); iter1.hasNext();){
                String key = (String) iter1.next();
                String value = (String) runArgs.get(key);
                args[i++] = key;
                if(value.length()>0){
                    args[i++] = value;
                }
                printArgs.append(key);
                printArgs.append(" ");
                printArgs.append(value);
                printArgs.append(" ");
            }
            for(;i<args.length;i++){
                args[i]="";
            }
            Object obj = createObject(toolName);
            if(obj instanceof CLDRConverterTool){
                CLDRConverterTool tool = (CLDRConverterTool) obj;
                tool.setLocalesMap(localesMap);
                if(run.deprecates!=null){
                    tool.setAliasLocaleList(run.deprecates.aliasLocaleList);
                    tool.setAliasMap(run.deprecates.aliasMap);
                    tool.setEmptyLocaleList(run.deprecates.emptyLocaleList);
                }
                if(run.config!=null && run.config.paths!=null){
                    tool.setXPathExcludeMap(run.config.paths.getXPathExcludeMap());
                    tool.setXPathIncludeMap(run.config.paths.getXPathIncludeMap());
                }
                tool.processArgs(args);
            }else{
                errln(toolName+" not a subclass of CLDRConverterTool! Cannot execute. Exiting", true);
            }
            // Method method = getMethod(tool, methodName, new Class[]{String[].class});
            // invoke(tool, method, new Object[]{args});
            System.out.println("");
        }
        
    }

    private static Object createObject(String className) {
        Object object = null;
        try {
            Class classDefinition = Class.forName(className);
            object = classDefinition.newInstance();
        } catch (InstantiationException e) {
            errln(e.getMessage(), true);
        } catch (IllegalAccessException e) {
            errln(e.getMessage(), true);
        } catch (ClassNotFoundException e) {
            errln(e.getMessage(), true);
        }
        return object;
     }
    //for run nested element
    public void addConfiguredRun(Run run){
        runs.add(run);
    }    
    
    //setter for toolName attribute
    public void setToolName(String name){
        toolName = name;
    }
    public void setSrcFile(String sf){
        srcFile = sf;
    }
    public void setDestFile(String df){
        destFile = df;
    }
    public void setNoArgs(String bool){
        noArgs = bool.equals("true")? true : false;
    }
    public static class Run extends Task{
        private String type;
        private Args args;
        private Config config; 
        private Deprecates deprecates;
        public void setType(String type){
            this.type = type;
        }
        public void addConfiguredArgs(Args args){
            this.args = args;
        }
        public void addConfiguredConfig(Config conf){
            config = conf;
        }
        public void addConfiguredDeprecates(Deprecates dep){
            this.deprecates = dep;
        }
        public Hashtable getArgs(Hashtable hash){
            if(hash==null){
                hash = new Hashtable();
            }
            args.getArgs(hash);
            
            return hash;
        }
        public Config getConfig(){
            return config;
        }
        public String getType(){
            return type;
        }
    }
    
    public static class Args extends Task{
        Vector args = new Vector();
        public void addConfiguredArg(Arg arg){
            args.add(arg);
        }
        public Hashtable getArgs(Hashtable hash){
            if(args.size()==0){
                return hash;
            }
            for(Iterator iter = args.iterator(); iter.hasNext();){
                Arg arg = (Arg)iter.next();
                hash.put(arg.name, arg.value);
            }
           return hash;
        }
    }
    
    public static class Arg extends Task{
        String name, value="";
        public void setName (String name){
            this.name = name;
        }
        public void setValue (String value){
            this.value = value;
        }
    }
    
    public static class Config extends Task{
        protected Locales locales;
        protected Paths paths;
        private Vector ofbs = new Vector();
        private String type;
        public void addConfiguredLocales(Locales loc){
            if(locales!=null){
                throw new BuildException("Multiple <locales> elements not supported");
            }
            locales = loc;
        }
        public void addConfiguredPaths(Paths ps){
            if(paths!=null){
                throw new BuildException("Multiple <paths> elements not supported");
            }
            paths = ps;
        }
        public void addConfiguredOverrideFallback(OverrideFallback ofb){
            ofbs.add(ofb);
        }
        public void setType(String type){
            this.type = type;
        }
        public Locales getLocales(){
           return locales;
        }
        public String getType(){
            return type;
        }
    }
    public static class Locales extends Task{
        private Vector include = new Vector();
        private Vector exclude = new Vector();
        public void addConfiguredInclude(Include inc){
            if(include.contains(inc)){
                errln("This build does not support multiple <include> or <exclude> elements with same locale attribute: "+inc.locale, true);
            }
            include.add(inc);
        }
        public void addConfiguredExclude(Exclude ex){
            if(exclude.contains(ex)){
                errln("This build does not support multiple <include> or <exclude> elements with same locale attribute: "+ex.locale, true);
            }
            exclude.add(ex);
        }
        public Vector getIncludeList(){
            return include;
        }
        public Vector getExcludeList(){
            return exclude;
        }
    }
    
    public static class Include extends Task{
        String draft;
        String locale;
        String xpath;
        String preferAlt;
        public void setDraft(String ds){
            draft = ds;
        }
        public void setLocale(String loc){
            if(xpath!=null){
                errln("Both xpath and locale attributes cannot be set on the same element! xpath=\""+xpath+"\" locale=\""+loc+"\"", true);
            }
            locale = loc;
        }
        public void setXpath(String xp){
            if(locale!=null){
                errln("Both xpath and locale attributes cannot be set on the same element! xpath=\""+xp+"\" locale=\""+locale+"\"", true);
            }
            xpath = xp;
        }
        public void setPreferAlt(String pa){
            preferAlt = pa;
        }
        public boolean equals(Object o){
            if(!(o instanceof Include)){
                return false;
            }
            Include other = (Include) o;
            if(o==this){
                return true;
            }
            if(locale.equals(other.locale)&&
                (draft==other.draft || (draft!=null && other.draft!=null && draft.equals(other.draft))) &&
                (xpath==other.xpath || (xpath!=null && other.xpath!=null && xpath.equals(other.xpath))) &&
                (preferAlt==other.preferAlt || (preferAlt!=null && other.preferAlt!=null && preferAlt.equals(other.preferAlt)))
               ){
                return true;
            }
            return false;
        }
        public int hashCode(){
            return locale.hashCode() + 
                   draft!=null? draft.hashCode():0+
                   xpath!=null? xpath.hashCode():0+
                   preferAlt!=null? preferAlt.hashCode():0;
        }
    }
    public static class Exclude extends Include{
        // all data & methods are identical to Include class 
    }
    
    public static class Deprecates extends Task{
        protected ArrayList aliasLocaleList    = null;
        protected ArrayList emptyLocaleList    = null;
        protected TreeMap   aliasMap           = null;
        public void addConfiguredAlias(Alias alias){
            if(aliasMap==null){
                aliasMap= new TreeMap();
            }
            aliasMap.put(alias.from, new CLDRConverterTool.Alias(alias.to, alias.xpath));
        }
        public void addConfiguredEmptyLocale(EmptyLocale alias){
            if(emptyLocaleList==null){
                emptyLocaleList = new ArrayList();
            }
            emptyLocaleList.add(alias.locale);
        }
        public void addConfiguredAliasLocale(AliasLocale alias){
            if(aliasLocaleList==null){
                aliasLocaleList = new ArrayList();
            }
            aliasLocaleList.add(alias.locale);            
        }
    }
    public static class Alias extends Task{
        String from;
        String to;
        String xpath;
        public void setFrom(String from){
            this.from = from;
        }
        public void setTo(String to){
            this.to = to;
        }
        public void setXpath(String xp){
            xpath = xp;
        }
        
    }
    public static class AliasLocale extends Task{
        String locale;
        public void setLocale(String loc){
            locale = loc;
        }
    }
    
    public static class EmptyLocale extends AliasLocale{
        // all data & methods are identical to AliasLocale class 
    }
    
    public static class Paths extends Task{
        private Vector include = new Vector();
        private Vector exclude = new Vector();
        public void addConfiguredInclude(Include inc){
            include.add(inc);
        }
        public void addConfiguredExclude(Exclude ex){
            exclude.add(ex);
        }
        public TreeMap getXPathIncludeMap(){
            return getXPathMap(include);
        }
        public TreeMap getXPathExcludeMap(){
            return getXPathMap(exclude);
        }
        private TreeMap getXPathMap(Vector vec){
            TreeMap map = new TreeMap();
            for(int i=0; i<vec.size(); i++){
                Include inc = (Include) vec.get(i);
                map.put(inc.xpath, new CLDRConverterTool.Preferences(inc.preferAlt, inc.draft));
            }
            return map;
        }
    }
    
    public static class OverrideFallback extends Config{
        private String fallbacks;
        public void setFallback(String fb){
            fallbacks = fb;
        }
        public String getFallbacks(){
            return fallbacks;
        }
    }
}
