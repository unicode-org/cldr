package org.unicode.cldr.ant;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
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
    public static boolean matchesFileName(String locales, String fileName){
        String locale = fileName.substring(0,fileName.indexOf(".xml"));
        return matchesLocale(locales, locale);
    }
    public static boolean matchesLocale(String locales, String locale){
        String[] arr = locales.split("\\s+");
        for(int i=0; i<arr.length; i++){
            if(locale.matches(arr[i]) || locale.equals(arr[i])){
                return true;
            }
        }
        return false;
    }
    public TreeMap getLocalesList(Config config, String src, String dest){
        File srcdir = new File(src);
        File[] srcFiles = srcdir.listFiles(new Filter(srcFile));
        File destdir = new File(dest);
        File[] destFiles = destdir.listFiles(new Filter(destFile));
        TreeMap ret = new TreeMap();
        
        if(config !=null){
            Locales locs = config.getLocales();
            Vector locsList = locs.getList();
            // walk down the include list and 
            // add the stuff to return list 
            for(int i=0; i<locsList.size(); i++){
                Object obj = locsList.get(i);
                if(obj instanceof Exclude){
                    Exclude exc = (Exclude) obj;
                    if(exc.locales == null){
                        errln("locales attribute not set for exclude element!", true);
                    }
                    // fast path for .*
                    if(exc.locales.equals(ALL)){
                        for(int j=0; j<srcFiles.length;j++){
                            ret.remove(srcFiles[j].getName());
                        }
                        continue;
                    }
                    for(int j=0; j<srcFiles.length; j++){
                        String fileName = srcFiles[j].getName(); 
                        if(matchesFileName(exc.locales, fileName)){
                            ret.remove(fileName);
                            break;
                        }
                    }
                }else if(obj instanceof Include){
                    Include inc = (Include) obj;
                    if(inc.locales == null){
                        errln("locales attribute not set for include element!", true);
                    }
                    //fast path for .*
                    if(inc.locales.equals(ALL)){
                        for(int j=0; j<srcFiles.length;j++){
                            ret.put(srcFiles[j].getName(), inc.draft);
                        }
                        continue;
                    }
                    
                    for(int j=0; j<srcFiles.length; j++){
                        String fileName = srcFiles[j].getName(); 
                        if(matchesFileName(inc.locales, fileName)){
                            ret.put(fileName, inc.draft);
                            break;
                        }
                    }
                }
            }
          
        }else{
            for(int i=0; i<srcFiles.length;i++){
                ret.put(srcFiles[i].getName(),".*");
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
                if(run.config!=null){
                    if(run.config.paths!=null){
                        tool.setPathList(run.config.paths.pathList);
                    }
                    if(run.config.ofb!=null){
                        tool.setOverrideFallbackList(run.config.ofb.list);
                    }
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
        private OverrideFallback ofb =null;
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
            if(this.ofb!=null){
                errln("Multiple <overrideFallback> elements not allowed!", true);
            }
            this.ofb = ofb;
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
        private Vector localesList = new Vector();
        public void addConfiguredInclude(Include inc){
            localesList.add(inc);
        }
        public void addConfiguredExclude(Exclude ex){
            localesList.add(ex);
        }
        public Vector getList(){
            return localesList;
        }
    }
    
    public static class Include extends Task{
        public String draft;
        public String locales;
        public String xpath;
        public String alt;
        
        public void setDraft(String ds){
            draft = ds;
        }
        public void setLocales(String locs){
            locales = locs;
        }
        public void setXpath(String xp){
            xpath = xp;
        }
        public void setAlt(String pa){
            alt = pa;
        }
        
        public boolean equals(Object o){
            if(!(o instanceof Include)){
                return false;
            }
            Include other = (Include) o;
            if(o==this){
                return true;
            }
            if(locales.equals(other.locales)&&
                (draft==other.draft || (draft!=null && other.draft!=null && draft.equals(other.draft))) &&
                (xpath==other.xpath || (xpath!=null && other.xpath!=null && xpath.equals(other.xpath))) &&
                (alt==other.alt || (alt!=null && other.alt!=null && alt.equals(other.alt)))
               ){
                return true;
            }
            return false;
        }
        public int hashCode(){
            return locales.hashCode() + 
                   draft!=null? draft.hashCode():0+
                   xpath!=null? xpath.hashCode():0+
                   alt!=null? alt.hashCode():0;
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
    
    public static class EmptyLocale extends Task{
        String locale;
        String list;
        public void setLocale(String loc){
            locale = loc;
        }
        public void setList(String list){
            this.list = list;
        }
    }
    
    public static class Paths extends Task{
        public String fallback;
        public String locales;
        public String draft;
        
        private List<Task> pathList = new ArrayList<Task>();
        
        public void addConfiguredInclude(Include inc){
            pathList.add(inc);
        }
        public void addConfiguredExclude(Exclude ex){
            pathList.add(ex);
        }
        public void setFallback(String fb){
            fallback = fb;
        }
        public void setLocales(String locs){
            locales = locs;
        }
        public void setDraft(String dft){
            draft =dft;
        } 
        
        public void addConfiguredCoverageLevel(CoverageLevel level){
            //make sure all fields are set
            if((level.group!=null && level.org==null)|| (level.org!=null && level.group==null)){
                errln("Invalid specification of coverageLevel element. org && group not set!", true);
            }
            if(level.level==null){
                errln("Invalid specification of coverageLevel element. level not set!", true);
            }
            pathList.add(level);
        }
    }
    public static class CoverageLevel extends Task{
        public String group;
        public String level;
        public String locales;
        public String draft;
        public String org;
        
        public void setDraft(String dft){
            draft = dft;
        } 
        public void setLevel(String lvl){
            level = lvl;
        } 
        public void setLocales(String locs){
            locales = locs;
        } 
        public void setOrg(String o){
            org = o;
        } 
        public void setGroup(String g){
            group = g;
        } 
    }
    public static class OverrideFallback extends Task{
        private ArrayList list = new ArrayList();
        public void addConfiguredPaths(Paths paths){
            list.add(paths);
        }
    }
}
