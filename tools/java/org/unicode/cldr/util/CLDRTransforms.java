/**
 * 
 */
package org.unicode.cldr.util;


import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLDRTransforms {

    private static Map<String, CLDRTransforms> cache = new HashMap();
  
    /**
     * 
     * @param showProgress null if no progress needed
     * @param filter TODO
     * @return
     */

    public static CLDRTransforms getinstance(PrintWriter showProgress, String filter) {
      synchronized (cache) {
        CLDRTransforms instance = cache.get(filter);
        if (instance == null) {
          instance = new CLDRTransforms(showProgress, filter);
          if (instance != null) {
            cache.put(filter, instance);
          }
        }
        return instance;
      }
    }

    
    Map id_instance = new HashMap();

    static Transliterator fixup = Transliterator
            .getInstance("[:Mn:]any-hex/java");

    Set available = new HashSet();
    String[] doFirst = {"Latin-ConjoiningJamo"};
    Relation<Matcher,String> dependsOn = new Relation(new LinkedHashMap(), LinkedHashSet.class);
    {
      addDependency(".*(Jamo|Hangul).*", "Latin-ConjoiningJamo");
      addDependency(".*Bengali.*", "Bengali-InterIndic", "InterIndic-Bengali");
      addDependency(".*Devanagari.*", "Devanagari-InterIndic", "Devanagari-Tamil");
      addDependency(".*Gujarati.*", "Gujarati-InterIndic", "Gujarati-Tamil");
      addDependency(".*Gurmukhi.*", "Gurmukhi-InterIndic", "Gurmukhi-Tamil");
      addDependency(".*Kannada.*", "Kannada-InterIndic", "Kannada-Tamil");
      addDependency(".*Malayalam.*", "Malayalam-InterIndic", "Malayalam-Tamil");
      addDependency(".*Oriya.*", "Oriya-InterIndic", "Oriya-Tamil");
      addDependency(".*Tamil.*", "Tamil-InterIndic", "InterIndic-Tamil");
      addDependency(".*Telugu.*", "Telugu-InterIndic", "Telugu-Tamil");
      addDependency(".*Tamil.*", "Tamil-InterIndic", "InterIndic-Tamil");
    }
    
    Factory cldrFactory = CLDRFile.Factory.make(
        Utility.COMMON_DIRECTORY + File.separatorChar +  "transforms/", ".*");
    
    private CLDRTransforms(PrintWriter showProgress, String filterString)  {        
        // reorder to preload some 
        Set<String> ordered = new LinkedHashSet<String>();
        
        Matcher filter = filterString == null ? null : Pattern.compile(filterString).matcher("");
        
        //ordered.addAll(Arrays.asList(doFirst));
        for (String item : cldrFactory.getAvailable()) {
          if (filter != null && !filter.reset(item).matches()) {
            System.out.println("Skipping " + item);
            continue;
          }
          // add dependencies first
          for (Matcher m : dependsOn.keySet()) {
            if (m.reset(item).matches()) {
              ordered.addAll(dependsOn.getAll(m));
            }
          }
          ordered.add(item);
        }
        System.out.println("Adding: " + ordered);
        
        for (Iterator it = ordered.iterator(); it.hasNext();) {
            String cldrFileName = (String) it.next();
            if (cldrFileName.contains("Ethiopic") || cldrFileName.contains("Aboriginal")) {
              System.out.println("Skipping Ethiopic");
             //Transliterator.DEBUG = true;
              continue;
            } else {
              //Transliterator.DEBUG = false;
            }

            CLDRFile file = cldrFactory.make(cldrFileName, false);
            cache(file, showProgress);
        }
        available = Collections.unmodifiableSet(id_instance.keySet());
    }

    private void addDependency(String pattern, String... whatItDependsOn) {
      dependsOn.putAll(Pattern.compile(pattern).matcher(""), Arrays.asList(whatItDependsOn));
    }

    public  Set getAvailableTransforms() {
        return available;
    }
    
    public  Transliterator getInstance(String id) {
        Transliterator result = (Transliterator) id_instance.get(id);
        if (result == null) {
            throw new IllegalArgumentException("No transform for " + id);
        }
        return result;
    }
    
    public static Pattern TRANSFORM_ID_PATTERN = Pattern.compile("(.+)-([^/]+)(/(.*))?");

   public  Transliterator getReverseInstance(String id) {
      Matcher matcher = TRANSFORM_ID_PATTERN.matcher(id);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("**No transform for " + id);
      }
      return getInstance(matcher.group(2) + "-" + matcher.group(1) + (matcher.group(4) == null ? "" : "/" + matcher.group(4)));
    }

    private  void cache(CLDRFile cldrFile, PrintWriter showProgress) {
        boolean first = true;
        StringBuffer rules = new StringBuffer();
        XPathParts parts = new XPathParts();
        String source = null;
        String target = null;
        String variant = null;
        String direction = null;

        for (Iterator it = cldrFile.iterator("", CLDRFile.ldmlComparator); it
                .hasNext();) {
            String path = (String) it.next();
            String value = cldrFile.getStringValue(path);
            if (first) {
                parts.set(path);
                Map attributes = parts.findAttributes("transform");
                if (attributes == null)
                    return; // error, not a transform file
                source = (String) attributes.get("source");
                target = (String) attributes.get("target");
                variant = (String) attributes.get("variant");
                direction = (String) attributes.get("direction");
                first = false;
            }
            if (path.indexOf("/comment") >= 0) {
                // skip
            } else if (path.indexOf("/tRule") >= 0) {
                // value = replaceUnquoted(value,"\u00A7", "&");
                // no longer need to replace arrows, ICU now handles the 2190/2192/2194 arrows
                //value = value.replace('\u2192', '>');
                //value = value.replace('\u2190', '<');
                //value = value.replaceAll("\u2194", "<>");
                value = fixup.transliterate(value);
                rules.append(value).append(Utility.LINE_SEPARATOR);
            } else {
                throw new IllegalArgumentException("Unknown element: "
                        + path + "\t " + value);
            }
        }
        String ruleString = rules.toString();
        if (direction.equals("both") || direction.equals("forward")) {
            String id = source + "-" + target + (variant == null ? "" : "/" + variant);
            internalRegister(id, ruleString, Transliterator.FORWARD, showProgress);
        }
        if (direction.equals("both") || direction.equals("backward")) {
            String id = target + "-" + source + (variant == null ? "" : "/" + variant);
            internalRegister(id, ruleString, Transliterator.REVERSE, showProgress);
        }
    }

    private  void internalRegister(String id, String ruleString, int direction, PrintWriter showProgress) {
        try {
            Transliterator t = Transliterator.createFromRules(id, ruleString, direction);
            id_instance.put(id, t);
            Transliterator.unregister(id);
            Transliterator.registerInstance(t);
            if (showProgress != null) {
              showProgress.println("Registered new Transliterator: " + id);
              showProgress.flush();
            }
        } catch (RuntimeException e) {
          if (showProgress != null) {
            showProgress.println("Couldn't register new Transliterator: " + id + "\t" + e.getMessage());
            showProgress.flush();
            throw (IllegalArgumentException) new IllegalArgumentException("Couldn't register new Transliterator: " + id).initCause(e);
          }
        }
    }
    
    public static class ParsedTransformID {
      public String source;
      public String target;
      public String variant;
      public ParsedTransformID set(String id) {
        variant = null;
        int pos = id.indexOf('-');
        if (pos < 0) {
          source = "Any";
          target = id;
          return this;
        }
        source = id.substring(0,pos);
        int pos2 = id.indexOf('/', pos);
        if (pos2 < 0) {
          target = id.substring(pos+1);
          return this;
        }
        target = id.substring(pos+1, pos2);
        variant = id.substring(pos2+1);
        return this;
      }
      public ParsedTransformID reverse() {
        String temp = source;
        source = target;
        target = temp;
        return this;
      }
      public String getTargetVariant() {
        return target + (variant == null ? "" : "/" + variant);
      }
      public String getSourceVariant() {
        return source + (variant == null ? "" : "/" + variant);
      }
      public String toString() {
        return source + "-" + getTargetVariant();
      }
      public static String getId(String source, String target, String variant) {
        String id = source + '-' + target;
        if (variant != null) id += "/" + variant;
        return id;
      }
    }

}
