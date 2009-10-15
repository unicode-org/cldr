package org.unicode.cldr.ant;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ibm.icu.dev.tool.UOption;

public class CLDRBuild extends Task {
  private String toolName;
  private String srcFile;
  private String destFile;
  private boolean noArgs;
  private List<Run> runs = new ArrayList<Run>();

  private UOption srcDir = UOption.SOURCEDIR();
  private UOption destDir = UOption.DESTDIR();

  private static final char FILE_EXT_SEPARATOR = '.';

  private static class PatternFilter implements FileFilter {
    private final String filePattern;

    public PatternFilter(String filePattern) {
      this.filePattern = filePattern;
    }

    public boolean accept(File pathname) {
      return filePattern != null && pathname.getName().matches(filePattern);
    }
  }

  public static boolean matchesLocale(List<String> locales, String localePattern) {
    for (String locale : locales) {
      if (localePattern.equals(locale) || localePattern.matches(locale)) {
        return true;
      }
    }
    return false;
  }

  public Map<String, String> getLocalesList(Config config, String src, String dest) {
    File srcdir = new File(src);
    File[] srcFiles = srcdir.listFiles(new PatternFilter(srcFile));
    File destdir = new File(dest);
    File[] destFiles = destdir.listFiles(new PatternFilter(destFile));

    Map<String, String> ret = new TreeMap<String, String>();

    if (config != null) {
      List<InExclude> localesList = config.locales.localesList;
      for (InExclude inex : localesList) {
        for (File file : srcFiles) {
          String fileName = file.getName();
          if (inex.matchesFileName(fileName)) {
            if (inex.include) {
              ret.put(fileName, inex.draft);
            } else {
              ret.remove(fileName);
            }
          }
        }
      }
    } else {
      for (File file : srcFiles) {
        ret.put(file.getName(), ".*");
      }
    }

    // only build the files that need to be built
    for (File file : destFiles) {
      String destName = file.getName();
      long destMod = file.lastModified();
      destName = destName.substring(0, destName.indexOf(FILE_EXT_SEPARATOR) + 1);

      if (srcFile != null) {
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
      } else {
        if (file.exists()) {
          ret.remove(destFile);
        }
      }
    }

    if (ret.size() == 0 && destFiles.length == 1) {
      return null;
    }

    return ret;
  }

  private static String getFileExtension(String name) {
    // a regular expression
    int i = name.indexOf('*');
    if (i == -1) {
      i = name.indexOf('.');
      if (i != -1) {
        return name.substring(i+1);
      }
    }
    return name;
  }

  static void exitWithException(Throwable t) {
    errln(t.getMessage());
    t.printStackTrace(System.err);
    System.exit(-1);
  }

  static void exitWithError(String msg) {
    errln(msg);
    System.exit(-1);
  }

  static void errln(String msg) {
    System.err.println("ERROR: " + msg);
  }

  static void warnln(String msg) {
    System.out.println("WARNING: " + msg);
  }

  static void infoln(String msg){
    System.out.println("INFO: " + msg);
  }

  private String getDirString(Args runArgs, UOption key) {
    String value = runArgs.map.get("--" + key.longName);
    if (value == null) {
      value = runArgs.map.get("-" + key.shortName);
    }
    return value;
  }

  // The method executing the task
  @Override
  public void execute() throws BuildException {
    if (toolName == null) {
      throw new BuildException("Tool name not set");
    }

    try {
      for (Run run : runs) {
        Config config = run.config;

        Args runArgs = run.args;

        Map<String, String> localesMap = getLocalesList(
            run.config, getDirString(runArgs, srcDir), getDirString(runArgs, destDir));

        if (localesMap == null || (localesMap.size() == 0 && !noArgs)) {
          continue;
        }

        List<String> argList = new ArrayList<String>();
        StringBuilder printArgs = new StringBuilder();
        for (Map.Entry<String, String> e : runArgs.map.entrySet()) {
          String key = e.getKey();
          String value = e.getValue();
          printArgs.append(key).append(' ');
          argList.add(key);
          if (value != null && value.length() > 0) {
            printArgs.append(value).append(' ');
            argList.add(value);
          }
        }

        Object obj = createObject(toolName);
        if (!(obj instanceof CLDRConverterTool)) {
          exitWithError(toolName + " not a subclass of CLDRConverterTool!");
        }

        CLDRConverterTool tool = (CLDRConverterTool) obj;
        tool.setLocalesMap(localesMap);

        if (run.deprecates != null) {
          tool.setAliasLocaleList(run.deprecates.aliasLocaleList);
          tool.setAliasMap(run.deprecates.aliasMap);
          tool.setEmptyLocaleList(run.deprecates.emptyLocaleList);
        }

        if (run.config != null) {
          if (run.config.paths != null){
            tool.setPathList(run.config.paths.pathList);
          }

          if(run.config.ofb != null){
            tool.setOverrideFallbackList(run.config.ofb.pathsList);
          }
        }

        tool.processArgs(argList.toArray(new String[argList.size()]));
      }
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private static Object createObject(String className) {
    Object object = null;
    try {
      Class<?> classDefinition = Class.forName(className);
      object = classDefinition.newInstance();
    } catch (InstantiationException e) {
      exitWithException(e);
    } catch (IllegalAccessException e) {
      exitWithException(e);
    } catch (ClassNotFoundException e) {
      exitWithException(e);
    } catch (Throwable t) {
      exitWithException(t);
    }
    return object;
  }

  public void addConfiguredRun(Run run){
    runs.add(run);
  }

  public void setToolName(String name) {
    toolName = name;
  }

  public void setSrcFile(String sf) {
    srcFile = sf;
  }

  public void setDestFile(String df) {
    destFile = df;
  }

  public void setNoArgs(String bool) {
    noArgs = bool.equals("true");
  }

  public static class Run extends Task {
    String type;
    Args args;
    Config config;
    Deprecates deprecates;

    public void setType(String type) {
      this.type = type;
    }

    public void addConfiguredArgs(Args args) {
      this.args = args;
    }

    public void addConfiguredConfig(Config config) {
      this.config = config;
    }

    public void addConfiguredDeprecates(Deprecates deprecates) {
      this.deprecates = deprecates;
    }
  }

  public static class Args extends Task {
    Map<String, String> map = new HashMap<String, String>();

    public void addConfiguredArg(Arg arg) {
      if (arg.name == null) {
        throw new IllegalArgumentException("argument missing name");
      }
      map.put(arg.name, arg.value);
    }
  }

  public static class Arg extends Task {
    String name;
    String value;

    public void setName (String name) {
      this.name = name;
    }

    public void setValue (String value){
      this.value = value;
    }
  }

  public static class Config extends Task {
    Locales locales;
    Paths paths;
    OverrideFallback ofb;
    String type;

    public void addConfiguredLocales(Locales loc){
      if (locales != null) {
        exitWithError("Multiple <locales> elements not supported");
      }
      locales = loc;
    }

    public void addConfiguredPaths(Paths ps){
      if (paths != null) {
        exitWithError("Multiple <paths> elements not supported");
      }
      paths = ps;
    }

    public void addConfiguredOverrideFallback(OverrideFallback ofb){
      if (this.ofb != null) {
        exitWithError("Multiple <overrideFallback> elements not allowed!");
      }
      this.ofb = ofb;
    }

    public void setType(String type){
      this.type = type;
    }
  }

  public static class Locales extends Task {
    List<InExclude> localesList = new ArrayList<InExclude>();

    public void addConfiguredInclude(Include include) {
      addInEx(include);
    }

    public void addConfiguredExclude(Exclude exclude) {
      addInEx(exclude);
    }

    private void addInEx(InExclude inex) {
      inex.validate();
      localesList.add(inex);
    }
  }

  public static class InExclude extends Task {
    static final List<String> ANY = Collections.emptyList();

    final boolean include;
    List<String> locales;
    String draft;
    String xpath;
    String alt;

    protected InExclude(boolean include) {
      this.include = include;
    }

    public void setDraft(String draft) {
      this.draft = draft;
    }

    public void setLocales(String locales) {
      if (".*".equals(locales)) {
        this.locales = ANY;
      } else {
        this.locales = Arrays.asList(locales.split("\\s+"));
      }
    }

    public void setXpath(String xpath) {
      this.xpath = xpath;
    }

    public void setAlt(String alt) {
      this.alt = alt;
    }

    void validate() {
      if (locales == null) {
        exitWithError("locales attribute not set for include/exclude element!");
      }
    }

    boolean matchesFileName(String fileName) {
      if (locales == ANY) {
        return true;
      }
      String localePattern = fileName.substring(0, fileName.indexOf(".xml"));
      return matchesLocale(locales, localePattern);
    }

    @Override
    public boolean equals(Object o){
      if (!(o instanceof InExclude)) {
        return false;
      }

      if (o == this) {
        return true;
      }

      InExclude rhs = (InExclude) o;
      return
        include == rhs.include &&
        equalLists(locales, rhs.locales) &&
        equalStrings(draft, rhs.draft) &&
        equalStrings(xpath, rhs.xpath) &&
        equalStrings(alt, rhs.alt);
    }

    @Override
    public int hashCode() {
      return hash(locales, hash(draft, hash(xpath, hash(alt, 0))));
    }

    private boolean equalStrings(String lhs, String rhs) {
      return lhs == rhs || (lhs != null && lhs.equals(rhs));
    }

    private <T> boolean equalLists(List<? extends T> lhs, List<? extends T> rhs) {
      return lhs == rhs || (lhs != null && lhs.equals(rhs));
    }

    private int hash(Object rhs, int hash) {
      return rhs == null ? hash : (hash * 31) ^ rhs.hashCode();
    }
  }

  public static class Include extends InExclude {
    public Include() {
      super(true);
    }
  }

  public static class Exclude extends InExclude {
    public Exclude() {
      super(false);
    }
  }

  public static class Deprecates extends Task {
    List<String> aliasLocaleList;
    List<String> emptyLocaleList;
    Map<String, CLDRConverterTool.Alias> aliasMap;

    public void addConfiguredAlias(Alias alias){
      if (aliasMap == null){
        aliasMap= new TreeMap<String, CLDRConverterTool.Alias>();
      }
      aliasMap.put(alias.from, new CLDRConverterTool.Alias(alias.to, alias.xpath));
    }

    public void addConfiguredEmptyLocale(EmptyLocale alias){
      if (emptyLocaleList == null) {
        emptyLocaleList = new ArrayList<String>();
      }
      emptyLocaleList.add(alias.locale);
    }

    public void addConfiguredAliasLocale(AliasLocale alias){
      if (aliasLocaleList == null) {
        aliasLocaleList = new ArrayList<String>();
      }
      aliasLocaleList.add(alias.locale);
    }
  }

  public static class Alias extends Task {
    String from;
    String to;
    String xpath;

    public void setFrom(String from) {
      this.from = from;
    }

    public void setTo(String to) {
      this.to = to;
    }

    public void setXpath(String xpath) {
      this.xpath = xpath;
    }
  }

  public static class AliasLocale extends Task {
    String locale;

    public void setLocale(String locale) {
      this.locale = locale;
    }
  }

  public static class EmptyLocale extends Task {
    String locale;
    String list;

    public void setLocale(String locale) {
      this.locale = locale;
    }

    public void setList(String list){
      this.list = list;
    }
  }

  public static class Paths extends Task {
    public String fallback;
    public String locales;
    public String draft;

    private List<Task> pathList = new ArrayList<Task>();

    public void addConfiguredInclude(Include include) {
      pathList.add(include);
    }

    public void addConfiguredExclude(Exclude exclude) {
      pathList.add(exclude);
    }

    public void setFallback(String fallback) {
      this.fallback = fallback;
    }

    public void setLocales(String locales) {
      this.locales = locales;
    }

    public void setDraft(String draft) {
      this.draft = draft;
    }

    public void addConfiguredCoverageLevel(CoverageLevel level){
      level.validate();
      pathList.add(level);
    }
  }

  public static class CoverageLevel extends Task {
    public String group;
    public String level;
    public String locales;
    public String draft;
    public String org;

    public void setDraft(String draft) {
      this.draft = draft;
    }

    public void setLevel(String level) {
      this.level = level;
    }

    public void setLocales(String locales) {
      this.locales = locales;
    }

    public void setOrg(String org) {
      this.org = org;
    }

    public void setGroup(String group) {
      this.group = group;
    }

    void validate() {
      if ((group == null) != (org == null)){
        exitWithError("Invalid specification of coverageLevel element; org && group not set!");
      }

      if (level == null) {
        exitWithError("Invalid specification of coverageLevel element; level not set!");
      }
    }
  }

  public static class OverrideFallback extends Task {
    List<Paths> pathsList = new ArrayList<Paths>();

    public void addConfiguredPaths(Paths paths) {
      pathsList.add(paths);
    }
  }
}
