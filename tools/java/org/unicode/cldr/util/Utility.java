/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class Utility {
	/**
		 * Very simple class, used to replace variables in a string. For example<p>
	<pre>static VariableReplacer langTag = new VariableReplacer()
			.add("$alpha", "[a-zA-Z]")
			.add("$digit", "[0-9]")
			.add("$alphanum", "[a-zA-Z0-9]")
			.add("$x", "[xX]");
			...
			String langTagPattern = langTag.replace(...);
	</pre>
		 */
		public static class VariableReplacer {
			// simple implementation for now
			Comparator c;
			private Map m = new TreeMap(Collections.reverseOrder());
			public VariableReplacer add(String variable, String value) {
				m.put(variable, value);
				return this;
			}
			public String replace(String source) {
				String oldSource;
				do {
					oldSource = source;
					for (Iterator it = m.keySet().iterator(); it.hasNext();) {
						String variable = (String) it.next();
						String value = (String) m.get(variable);
						source = replaceAll(source, variable, value);
					}
				} while (!source.equals(oldSource));
				return source;
			}
			public String replaceAll(String source, String key, String value) {
				while (true) {
					int pos = source.indexOf(key);
					if (pos < 0) return source;
					source = source.substring(0,pos) + value + source.substring(pos+key.length());
				}
			}
		}

	static final boolean DEBUG_SHOW_BAT = false;
	/** default working directory for Eclipse is . = ${workspace_loc:cldr}, which is <CLDR>/tools/java/ */
	public static final String BASE_DIRECTORY = "../../";	// get up to <CLDR>
	public static final String UTIL_DATA_DIR = 	"./org/unicode/cldr/util/data/";		// "C:/ICU4C/locale/tools/java/org/unicode/cldr/util/";
    public static final String UTIL_CLASS_DIR = "org.unicode.cldr.util";
	public static final String COMMON_DIRECTORY = BASE_DIRECTORY + "common/";
    public static final String MAIN_DIRECTORY = COMMON_DIRECTORY + "main/";
    public static final String SUPPLEMENTAL_DIRECTORY = COMMON_DIRECTORY + "supplemental/";
	public static final String GEN_DIRECTORY = BASE_DIRECTORY + "dropbox/gen/";
    public static final String TEST_DIR = Utility.COMMON_DIRECTORY + "test/";

	
	/** If the generated BAT files are to work, this needs to be set right */
	public static final String COMPARE_PROGRAM = "\"C:\\Program Files\\Compare It!\\wincmp3.exe\"";
	
	public static final List MINIMUM_LANGUAGES = Arrays.asList(new String[] {"ar", "en", "de", "fr", "hi", "it", "es", "pt", "ru", "zh", "ja"}); // plus language itself
	public static final List MINIMUM_TERRITORIES = Arrays.asList(new String[] {"US", "GB", "DE", "FR", "IT", "JP", "CN", "IN", "RU", "BR"});
	
	public interface LineComparer {
		static final int LINES_DIFFERENT = -1, LINES_SAME = 0, SKIP_FIRST = 1, SKIP_SECOND = 2;
		/**
		 * Returns LINES_DIFFERENT, LINES_SAME, or if one of the lines is ignorable, SKIP_FIRST or SKIP_SECOND
		 * @param line1
		 * @param line2
		 * @return
		 */
		int compare(String line1, String line2);
	}
	
	public static class SimpleLineComparator implements LineComparer {
	    public static final int TRIM = 1, SKIP_SPACES = 2, SKIP_EMPTY = 4, SKIP_CVS_TAGS = 8;
	    StringIterator si1 = new StringIterator();
        StringIterator si2 = new StringIterator();
        int flags;
        public SimpleLineComparator(int flags) {
			this.flags = flags;
		}
		public int compare(String line1, String line2) {
			// first, see if we want to skip one or the other lines
			int skipper = 0;
			if (line1 == null) {
				skipper = SKIP_FIRST;
			} else {
				if ((flags & TRIM)!= 0) line1 = line1.trim();
				if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) skipper = SKIP_FIRST;
			}
			if (line2 == null) {
				skipper = SKIP_SECOND;
			} else {
	            if ((flags & TRIM)!= 0) line2 = line2.trim();
	            if ((flags & SKIP_EMPTY)!= 0 && line2.length() == 0) skipper += SKIP_SECOND;
			}
            if (skipper != 0) {
            	if (skipper == SKIP_FIRST + SKIP_SECOND) return LINES_SAME; // ok, don't skip both
            	return skipper;
            }
            
            // check for null
            if (line1 == null) {
            	if (line2 == null) return LINES_SAME;
            	return LINES_DIFFERENT;          	
            }
            if (line2 == null) {
            	return LINES_DIFFERENT;
            }
            
            // now check equality
			if (line1.equals(line2)) return LINES_SAME;
			
			// if not equal, see if we are skipping spaces
			if ((flags & SKIP_CVS_TAGS) != 0) {
				if (line1.indexOf('$') >= 0 && line2.indexOf('$') >= 0) {
					line1 = stripTags(line1);
					line2 = stripTags(line2);
					if (line1.equals(line2)) return LINES_SAME;
				} else if (line1.startsWith("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/")
						&& line2.startsWith("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/")) {
					return LINES_SAME;
				}
			}
			if ((flags & SKIP_SPACES) != 0 && si1.set(line1).matches(si2.set(line2))) return LINES_SAME;
			return LINES_DIFFERENT;
		}
		
//		private Matcher dtdMatcher = Pattern.compile(
//				"\\Q<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/\\E.*\\Q/ldml.dtd\">\\E").matcher("");
		
		private String[] CVS_TAGS = {"Revision", "Date"};
		
		private String stripTags(String line) {
			//$Revision$
			//$Date$
			int pos = line.indexOf('$');
			if (pos < 0) return line;
			pos++;
			int endpos = line.indexOf('$', pos);
			if (endpos < 0) return line;
			for (int i = 0; i < CVS_TAGS.length; ++i) {
				if (!line.startsWith(CVS_TAGS[i], pos)) continue;
				line = line.substring(0,pos + CVS_TAGS[i].length()) + line.substring(endpos);
			}
			return line;
		}
		
	}
	
	/**
	 * 
	 * @param file1
	 * @param file2
	 * @param failureLines on input, String[2], on output, failing lines
	 * @param lineComparer
	 * @return
	 * @throws IOException
	 */
    public static boolean areFileIdentical(String file1, String file2, String[] failureLines, 
    		LineComparer lineComparer) throws IOException {
        BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
        try {
			BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
			try {
				String line1 = "";
				String line2 = "";
				int skip = 0;
      
			    for (int lineCount = 0; ; ++lineCount) {
			        if ((skip & LineComparer.SKIP_FIRST) == 0) line1 = br1.readLine();
			        if ((skip & LineComparer.SKIP_SECOND) == 0) line2 = br2.readLine();
			        if (line1 == null && line2 == null) return true;
			        if (line1 == null || line2 == null) {
			        	// System.out.println("debug");
			        }
			        skip = lineComparer.compare(line1, line2);
			        if (skip == LineComparer.LINES_DIFFERENT) {
			        	break;
			        }
			    }
			    failureLines[0] = line1 != null ? line1 : "<end of file>";
			    failureLines[1] = line2 != null ? line2 : "<end of file>";
			    return false;
			} finally {
			    br2.close();
			}
		} finally {
		    br1.close();
		}
    }
    
    public static void registerExtraTransliterators() {
    	String tzadir = UTIL_DATA_DIR;
		// HACK around lack of Armenian, Ethiopic				
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Latin-Armenian");
		//TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Latin-Ethiopic");
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Cyrillic-Latin");
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Arabic-Latin");	
		// needed
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Thaana-Latin");		
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Syriac-Latin");		
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Canadian_Aboriginal-Latin");
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Georgian-Latin");
		
		// do nothing, too complicated to do quickly
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Tibetan-Latin");
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Khmer-Latin");
		TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Lao-Latin");
    }
    


    /*
    static String getLineWithoutFluff(BufferedReader br1, boolean first, int flags) throws IOException {
        while (true) {
            String line1 = br1.readLine();
            if (line1 == null) return line1;
            if ((flags & TRIM)!= 0) line1 = line1.trim();
            if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) continue;
            return line1;
        }
    }
    */
    
    public final static class StringIterator {
    	String string;
    	int position = 0;
    	char next() {
    		while (true) {
    			if (position >= string.length()) return '\uFFFF';
    			char ch = string.charAt(position++);
    			if (ch != ' ' && ch != '\t') return ch;
    		}
    	}
    	StringIterator reset() {
    		position = 0;
    		return this;
    	}
    	StringIterator set(String string) {
    		this.string = string;
    		position = 0;
    		return this;
    	}
    	boolean matches(StringIterator other) {
    		while (true) {
    			char c1 = next();
    			char c2 = other.next();
    			if (c1 != c2) return false;
    			if (c1 == '\uFFFF') return true;
    		}
    	}
		/**
		 * @return Returns the position.
		 */
		public int getPosition() {
			return position;
		}
    }
    
    static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile) {
        generateBat( sourceDir,  sourceFile,  targetDir,  targetFile, new Utility.SimpleLineComparator(0)); 
    }
    
    static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile, LineComparer lineComparer) {
        try {
            String batDir = targetDir + "diff" + File.separator;
            String batName = targetFile + ".bat";
            String[] failureLines = new String[2];
            
            String fullSource = sourceDir + File.separator + sourceFile;
            String fullTarget = targetDir + File.separator + targetFile;

            if (!new File(sourceDir + File.separator + sourceFile).exists()) {
                File f = new File(batDir + batName);
                if (f.exists()) {
                    if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
                    f.delete();
                }
            } else if (!areFileIdentical(fullSource, fullTarget, failureLines, lineComparer)) {
                PrintWriter bat = BagFormatter.openUTF8Writer(batDir, batName);
                try {
                	bat.println(COMPARE_PROGRAM + " " +
                        new File(fullSource).getCanonicalPath() + " " +
                        new File(fullTarget).getCanonicalPath());               
                } finally {
                	bat.close();
                }
            } else {
                File f = new File(batDir + batName);
                if (f.exists()) {
                    if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
                    f.delete();
                }
                f = new File(fullTarget);
                if (BagFormatter.SHOW_FILES) System.out.println("*Renaming old " + f.getCanonicalPath());
                f.renameTo(new File(targetDir + File.separator + "_unchanged_" + targetFile));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	public static String[] splitArray(String source, char separator) {
		return splitArray(source, separator, false);
	}
	
	public static String[] splitArray(String source, char separator, boolean trim) {
		List piecesList = splitList(source, separator, trim);
		String[] pieces = new String[piecesList.size()];
		piecesList.toArray(pieces);
		return pieces;
	}

	public static List splitList(String source, char separator) {
		return splitList(source, separator, false, null);
	}
	
	public static List splitList(String source, char separator, boolean trim) {
		return splitList(source, separator, trim, null);
	}
	
	public static List splitList(String source, char separator, boolean trim, List output) {
		if (output == null) output = new ArrayList();
		if (source.length() == 0) return output;
		int pos = 0;
		do {
			int npos = source.indexOf(separator, pos);
			if (npos < 0) npos = source.length();
			String piece = source.substring(pos, npos);
			if (trim) piece = piece.trim();
			output.add(piece);
			pos = npos+1;
		} while (pos < source.length());
		return output;
	}

	/**
	 * Utility to indent by a certain number of tabs.
	 * @param out
	 * @param count
	 */
	static void indent(PrintWriter out, int count) {
		out.print(repeat("\t", count));
	}
	
	/**
	 * Utility to indent by a certain number of tabs.
	 * @param s
	 * @param count
	 */
	public static String repeat(String s, int count) {
		if (count == 0) return "";
		if (count == 1) return s;
		StringBuffer result = new StringBuffer();
	    for (int i = 0; i < count; ++i) {
	        result.append(s);
	    }
	    return result.toString();
	}
	
	/**
	 * Protect a collection (as much as Java lets us!) from modification.
	 */
	public static Object protectCollection(Object source) {
		// TODO - exclude UnmodifiableMap, Set, ...
		if (source instanceof Map) {
			Map map = (Map)source;
			Set keySet = map.keySet();
			int size = keySet.size();
			ArrayList keys = new ArrayList(size); // do this so it doesn't change
			ArrayList values = new ArrayList(size); // do this so it doesn't change
			for (Iterator it = keySet.iterator(); it.hasNext();) {
				Object key = it.next();
				keys.add(protectCollection(key));
				values.add(protectCollection(map.get(key)));
			}
			map.clear();
			for (int i = 0; i < size; ++i) {
				map.put(keys.get(i), values.get(i));
			}
			return map instanceof SortedMap ? Collections.unmodifiableSortedMap((SortedMap)map)
					: Collections.unmodifiableMap(map);
		} else if (source instanceof Collection) {
			Collection collection = (Collection) source;
			ArrayList contents = new ArrayList();
			//contents.addAll(collection);
			for (Iterator it = collection.iterator(); it.hasNext();) {
				contents.add(protectCollection(it.next()));
			}
			collection.clear();
			collection.addAll(contents);
			return collection instanceof List ? Collections.unmodifiableList((List)collection)
				: collection instanceof SortedSet ? Collections.unmodifiableSortedSet((SortedSet)collection)
				: collection instanceof Set ? Collections.unmodifiableSet((Set)collection)
				: Collections.unmodifiableCollection(collection);
		} else {
			return source; // can't protect
		}
	}
	
    /** Appends two strings, inserting separator if either is empty
	 */
	public static String joinWithSeparation(String a, String separator, String b) {
		if (a.length() == 0) return b;
		if (b.length() == 0) return a;
		return a + separator + b;
	}
	
    /** Appends two strings, inserting separator if either is empty. Modifies first map
	 */
	public static Map joinWithSeparation(Map a, String separator, Map b) {
		for (Iterator it = b.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			String bvalue = (String) b.get(key);
			String avalue = (String) a.get(key);
			if (avalue != null) {
				if (avalue.trim().equals(bvalue.trim())) continue;
				bvalue = joinWithSeparation(avalue, separator, bvalue);
			}
			a.put(key, bvalue);
		}
		return a;
	}
  
  public static String join(Collection c, String separator) {
    StringBuffer output = new StringBuffer();
    boolean isFirst = true;
    for (Object item : c) {
      if (isFirst) {
        isFirst = false;
      } else {
        output.append(separator);
      }
      output.append(item == null ? item : item.toString());
    }
    return output.toString();
  }
  
  public static String join(Object[] c, String separator) {
    StringBuffer output = new StringBuffer();
    boolean isFirst = true;
    for (Object item : c) {
      if (isFirst) {
        isFirst = false;
      } else {
        output.append(separator);
      }
      output.append(item == null ? item : item.toString());
    }
    return output.toString();
  }

	
	/**
	 * Utility like Arrays.asList()
	 */
	public static Map asMap(Object[][] source, Map target, boolean reverse) {
		int from = 0, to = 1;
		if (reverse) {
			from = 1; to = 0;
		}
    	for (int i = 0; i < source.length; ++i) {
    		target.put(source[i][from], source[i][to]);
    	}
    	return target;
	}
	
	public static Map asMap(Object[][] source) {
    	return asMap(source, new HashMap(), false);
	}
	
	/**
	 * Utility that ought to be on Map
	 */
	public static Map removeAll(Map m, Collection itemsToRemove) {
	    for (Iterator it = itemsToRemove.iterator(); it.hasNext();) {
	    	Object item = it.next();
	    	m.remove(item);
	    }
	    return m;
	}
	
	/**
	 * Returns the canonical name for a file.
	 */
	public static String getCanonicalName(String file) {
		try {
			return new File(file).getCanonicalPath();
		} catch (IOException e) {
			return file;
		}
	}
	
	/**
	 * Scan the string value from index forward. Stop at any point where the character
	 * at index is not in the UnicodeSet.
	 */
	public static int scan(UnicodeSet uset, String value, int index) {
		int cp;
		for (; index < value.length(); index += UTF16.getCharCount(cp)) {
			cp = UTF16.charAt(value, index);
			if (!uset.contains(cp)) break;
		}
		return index;
	}
	
  /**
   * Scan the string value from index forward. Stop at any point where the character
   * at index is in the UnicodeSet.
   */
  public static int scanNot(UnicodeSet uset, String value, int index) {
    int cp;
    for (; index < value.length(); index += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(value, index);
      if (uset.contains(cp)) break;
    }
    return index;
  }
  
  /**
   * Simplify the ranges in a Unicode set by merging any ranges that are only separated by characters in the dontCare set. 
   * For example, the ranges: \\u2E80-\\u2E99\\u2E9B-\\u2EF3\\u2F00-\\u2FD5\\u2FF0-\\u2FFB\\u3000-\\u303E change to \\u2E80-\\u303E if the dontCare set includes unassigned characters.
   * @param input Set to be modified
   * @param dontCare Set with the don't-care characters for spanning
   * @return the input set, modified
   */
  public static UnicodeSet addDontCareSpans(UnicodeSet input, UnicodeSet dontCare) {
    UnicodeSet notInInput = new UnicodeSet(input).complement();
    for (UnicodeSetIterator it = new UnicodeSetIterator(notInInput); it.nextRange();) {
      if (dontCare.contains(it.codepoint,it.codepointEnd)) {
        input.add(it.codepoint,it.codepointEnd);
      }
    }
    return input;
  }
  

    /**
     * Replaces all occurances of piece with replacement, and returns new String
     * Dumb implementation for now.
     */
    public static String replace(String source, String piece, String replacement) {
        if (source == null || source.length() < piece.length()) return source;
    	int pos = 0;
        while (true) {
            pos = source.indexOf(piece, pos);
            if (pos < 0) return source;
            source = source.substring(0,pos) + replacement + source.substring(pos + piece.length());
            pos += replacement.length();
        }
    }

    public static void addTreeMapChain(Map coverageData, Object[] objects) {
        Map base = coverageData;
        for (int i = 0; i < objects.length-2; ++i) {
            Map nextOne = (Map) base.get(objects[i]);
            if (nextOne == null) base.put(objects[i], nextOne = new TreeMap());
            base = nextOne;
        }
        base.put(objects[objects.length-2], objects[objects.length-1]);
    }
    
	public static abstract class Transform {
		public abstract Object transform(Object source);
		public Collection transform(Collection input, Collection output) {
			for (Iterator it = input.iterator(); it.hasNext();) {
				Object result = transform(it.next());
				if (result != null) output.add(result);
			}
			return output;
		}
		public Collection transform(Collection input) {
			return transform(input, new ArrayList());
		}
	}
	
	public static abstract class Filter {
		public abstract boolean contains(Object o);
		public Collection retainAll(Collection c) {
			for (Iterator it = c.iterator(); it.hasNext();) {
				if (!contains(it.next())) it.remove();
			}
			return c;
		}
		public Collection removeAll(Collection c) {
			for (Iterator it = c.iterator(); it.hasNext();) {
				if (contains(it.next())) it.remove();
			}
			return c;
		}
	}
	
	public static class MatcherFilter extends Filter {
		private Matcher matcher;
		public MatcherFilter(String pattern) {
			this.matcher = Pattern.compile(pattern).matcher("");
		}
		public MatcherFilter(Matcher matcher) {
			this.matcher = matcher;
		}
		public boolean contains(Object o) {
			return matcher.reset(o.toString()).matches();
		}		
	}

    /**
     * Fetch data from jar
     * @param name name of thing to load (org.unicode.cldr.util.name)
     */
    static public BufferedReader getUTF8Data(String name) throws java.io.IOException {
      java.io.InputStream is = null;
      try {
        is = 
            com.ibm.icu.impl.ICUData.getRequiredStream(Class.forName(Utility.UTIL_CLASS_DIR+".Utility"), "data/" + name);
       } catch (ClassNotFoundException cnf) { 
          
        throw new FileNotFoundException("Couldn't load " + Utility.UTIL_CLASS_DIR + "." + name + " - ClassNotFoundException." + cnf.toString());
//            .initCause(cnf);
       } catch (java.util.MissingResourceException mre) {
        // try file
         return BagFormatter.openUTF8Reader(Utility.UTIL_DATA_DIR + File.separator, name);
       }
       return new java.io.BufferedReader(   new java.io.InputStreamReader(is,"UTF-8") );
    }

    /**
     * Takes a Map that goes from Object to Set, and fills in the transpose
     * @param source_key_valueSet
     * @param output_value_key
     */
    public static void putAllTransposed(Map source_key_valueSet, Map output_value_key) {
        for (Iterator it = source_key_valueSet.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Set values = (Set) source_key_valueSet.get(key);
            for (Iterator it2 = values.iterator(); it2.hasNext();) {
                Object value = it2.next();
                output_value_key.put(value, key);
            }
        }
    }

	public static int countInstances(String source, String substring) {
		int count = 0;
		int pos = 0;
		while (true) {
			pos = source.indexOf(substring, pos) + 1;
			if (pos <= 0) break;
			count++;
		}
		return count;
	}

	public static void registerTransliteratorFromFile(String id, String dir, String filename) {
		registerTransliteratorFromFile(id, dir, filename, Transliterator.FORWARD, true);
		registerTransliteratorFromFile(id, dir, filename, Transliterator.REVERSE, true);
	}
	
	public static void registerTransliteratorFromFile(String id, String dir, String filename, int direction, boolean reverseID) {
		try {
			if (filename == null) {
				filename = id.replace('-', '_');
				filename = filename.replace('/', '_');
				filename += ".txt";
			}
			BufferedReader br = BagFormatter.openUTF8Reader(dir, filename);
			StringBuffer buffer = new StringBuffer();
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
				if (line.startsWith("//")) continue;
				buffer.append(line).append("\r\n");
			}
			br.close();
			String rules = buffer.toString();
			Transliterator t;
			int pos = id.indexOf('-');
			String rid;
			if (pos < 0) {
				rid = id + "-Any";
				id = "Any-" + id;
			} else {
				rid = id.substring(pos+1) + "-" + id.substring(0, pos);
			}
            if (!reverseID) rid = id;
            
			if (direction == Transliterator.FORWARD) {
				Transliterator.unregister(id);
				t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
				Transliterator.registerInstance(t);
				System.out.println("Registered new Transliterator: " + id);
			}
	
			/*String test = "\u049A\u0430\u0437\u0430\u049B";
			System.out.println(t.transliterate(test));
			t = Transliterator.getInstance(id);
			System.out.println(t.transliterate(test));
			*/
	
			if (direction == Transliterator.REVERSE) {
				Transliterator.unregister(rid);
				t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
				Transliterator.registerInstance(t);
				System.out.println("Registered new Transliterator: " + rid);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Can't open " + dir + ", " + id);
		}
	}
	
  public static void callMethod(String methodNames, Class cls) {
    for (String methodName : methodNames.split(",")) {
      try {
        Method method;
        try {
          method = cls.getMethod(methodName, (Class[]) null);
          try {
            method.invoke(null, (Object[]) null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } catch (Exception e) {
          System.out.println("No such method: " + methodName);
          showMethods(cls);
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public static void showMethods(Class cls) throws ClassNotFoundException {
    System.out.println("Possible methods of " + cls.getCanonicalName() + " are: ");
    Method[] methods = cls.getMethods();
    Set<String> names = new TreeSet<String>();
    for (int i = 0; i < methods.length; ++i) {
      if (methods[i].getGenericParameterTypes().length != 0) continue;
      int mods = methods[i].getModifiers();
      //if (!Modifier.isStatic(mods)) continue;
      String name = methods[i].getName();
      names.add(name);
    }
    for (Iterator it = names.iterator(); it.hasNext();) {
      System.out.println("\t" + it.next());
    }
  }
  
  /**
   * Breaks lines if they are too long, or if matcher.group(1) != last. Only breaks just before matcher.
   * @param input
   * @param separator
   * @param matcher must match each possible item. The first group is significant; if different, will cause break
   * @return
   */
  static public String breakLines(CharSequence input, String separator, Matcher matcher, int width) {
    StringBuffer output = new StringBuffer();
    String lastPrefix = "";
    int lastEnd = 0;
    int lastBreakPos = 0;
    matcher.reset(input);
    while (true) {
      boolean match = matcher.find();
      if (!match) {
        output.append(input.subSequence(lastEnd, input.length()));
        break;
      }
      String prefix = matcher.group(1);
      if (!prefix.equals(lastPrefix) || matcher.end() - lastBreakPos > width) { // break before?
        output.append(separator);
        lastBreakPos = lastEnd;
      } else if (lastEnd != 0){
        output.append(' ');
      }
      output.append(input.subSequence(lastEnd, matcher.end()).toString().trim());
      lastEnd = matcher.end();
      lastPrefix = prefix;
    }
    return output.toString();
  }
}