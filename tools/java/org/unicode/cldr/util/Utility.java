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
import java.io.PrintWriter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;

public class Utility {
	/**
		 * Used to replace variables in a string. For example<p>
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
	public static final String UTIL_DATA_DIR = 	"./org/unicode/cldr/util/";		// "C:/ICU4C/locale/tools/java/org/unicode/cldr/util/";
	public static final String COMMON_DIRECTORY = BASE_DIRECTORY + "common/";
	public static final String MAIN_DIRECTORY = COMMON_DIRECTORY + "main/";
	public static final String GEN_DIRECTORY = COMMON_DIRECTORY + "gen/";
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
            if (line2 == null) return LINES_DIFFERENT;
            
            // now check equality
			if (line1.equals(line2)) return LINES_SAME;
			
			// if not equal, see if we are skipping spaces
			if ((flags & SKIP_CVS_TAGS) != 0 && line1.indexOf('$') >= 0 && line2.indexOf('$') >= 0) {
				line1 = stripTags(line1);
				line2 = stripTags(line2);
				if (line1.equals(line2)) return LINES_SAME;
			}
			if ((flags & SKIP_SPACES) != 0 && si1.set(line1).matches(si2.set(line2))) return LINES_SAME;
			return LINES_DIFFERENT;
		}
		
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
			        skip = lineComparer.compare(line1, line2);
			        if (skip == LineComparer.LINES_DIFFERENT) break;
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
    
    static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile, LineComparer lineComparer) {
        try {
            String batDir = targetDir + "diff" + File.separator;
            String batName = targetFile + ".bat";
            String[] failureLines = new String[2];

            if (!areFileIdentical(sourceDir + sourceFile, targetDir + targetFile, failureLines, lineComparer)) {
                PrintWriter bat = BagFormatter.openUTF8Writer(batDir, batName);
                try {
                	bat.println(COMPARE_PROGRAM + " " +
                        new File(sourceDir + sourceFile).getCanonicalPath() + " " +
                        new File(targetDir + targetFile).getCanonicalPath());               
                } finally {
                	bat.close();
                }
            } else {
                File f = new File(batDir + batName);
                if (f.exists()) {
                    if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
                    f.delete();
                }
                f = new File(targetDir + targetFile);
                if (BagFormatter.SHOW_FILES) System.out.println("*Renaming old " + f.getCanonicalPath());
                f.renameTo(new File(targetDir + "_unchanged_" + targetFile));
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
			contents.addAll(collection);
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
	
}