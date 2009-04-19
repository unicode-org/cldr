package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.MergeLists;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.RuleBasedTransliterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;

public class TestTransforms {
	static String target = Utility.BASE_DIRECTORY + "dropbox/gen/icu-transforms/";
	static Matcher getId = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*").matcher("");
	static Matcher getSource = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*\\\"(.*)\\\".*").matcher("");
	
	public static void main(String[] args) throws IOException {
		File dir = new File(target);
		// get the list of files to take, and their directions
		BufferedReader input = BagFormatter.openUTF8Reader(target, "root.txt");
		String id = null;
		String filename = null;
		String lastId = null;
		String lastFilename = null;
		Map aliasMap = new LinkedHashMap();
		
		// Remove all of the current registrations
		// first load into array, so we don't get sync problems.
		List<String> rawAvailable = new ArrayList<String>();
		for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
		  rawAvailable.add((String)en.nextElement());
		}
		
		List<String> available = getDependentOrder(rawAvailable);
		available.retainAll(rawAvailable); // remove the items we won't touch anyway
    rawAvailable.removeAll(available); // now the ones whose order doesn't matter
    removeTransliterators(rawAvailable);
    removeTransliterators(available);

    for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
      String oldId = (String)en.nextElement();
		  System.out.println("Retaining: " + oldId);
		}
		
		// do first, since others depend on these
    /**
     * Special aliases. 
     * Tone-Digit {
            alias {"Pinyin-NumericPinyin"}
        }
        Digit-Tone {
            alias {"NumericPinyin-Pinyin"}
        }
     */
		Utility.registerTransliteratorFromFile("Latin-ConjoiningJamo", target, null);
		Utility.registerTransliteratorFromFile("Pinyin-NumericPinyin", target, null);
    Transliterator.registerAlias("Tone-Digit", "Pinyin-NumericPinyin");
    Transliterator.registerAlias("Digit-Tone", "NumericPinyin-Pinyin");
    Utility.registerTransliteratorFromFile("Fullwidth-Halfwidth", target, null);
    Utility.registerTransliteratorFromFile("Hiragana_Katakana", target, null);
    Utility.registerTransliteratorFromFile("Latin-Katakana", target, null);
    
    String fileMatcherString = Utility.getProperty("file", ".*");
    Matcher fileMatcher = Pattern.compile(fileMatcherString).matcher("");
		
		while (true) {
			String line = input.readLine();
			if (line == null) break;
			line = line.trim();
			if (line.startsWith("TransliteratorNamePattern")) break; // done
//			if (line.indexOf("Ethiopic") >= 0) {
//				System.out.println("Skipping Ethiopic");
//				continue;
//			}
			if (getId.reset(line).matches()) {
				String temp = getId.group(1);
				if (!temp.equals("file") && !temp.equals("internal")) id = temp;
				continue;
			}
			if (getSource.reset(line).matches()) {
				String operation = getSource.group(1);
				String source = getSource.group(2);
				if (operation.equals("alias")) {
					aliasMap.put(id, source);
					checkIdFix(id);
					lastId = id;
					id = null;
				} else if (operation.equals("resource:process(transliterator)")) {
					filename = source;
				} else if (operation.equals("direction")) {
					try {
						if (id == null || filename == null) {
							System.out.println("skipping: " + line);
							continue;
						}
						if (filename.indexOf("InterIndic") >= 0 && filename.indexOf("Latin") >= 0) {
							System.out.print("**" + id);
						}
						checkIdFix(id);
						if (source.equals("FORWARD")) {
							Utility.registerTransliteratorFromFile(id, target, filename, Transliterator.FORWARD, false);
						} else {
							Utility.registerTransliteratorFromFile(id, target, filename, Transliterator.REVERSE, false);
						}
						lastId = id;
						id = null;
						lastFilename = filename;
						filename = null;
					} catch (RuntimeException e) {
						throw (RuntimeException) new IllegalArgumentException("Failed with " + filename + ", " + source).initCause(e);
					}
				} else {
					System.out.println(dir + "root.txt unhandled line:" + line);
				}
				continue;
			}
			String trimmed = line.trim();
			if (trimmed.equals("")) continue;
			if (trimmed.equals("}")) continue;
			if (trimmed.startsWith("//")) continue;
			System.out.println("Unhandled:" + line);
		}
		for (java.util.Iterator it = aliasMap.keySet().iterator(); it.hasNext();) {
			id = (String)it.next();
			String source = (String) aliasMap.get(id);
			Transliterator.unregister(id);
			Transliterator t = Transliterator.createFromRules(id, "::" + source + ";", Transliterator.FORWARD);
			Transliterator.registerInstance(t);
			System.out.println("Registered new Transliterator Alias: " + id);

		}
		System.out.println("Fixed IDs");
		for (Iterator it = fixedIDs.keySet().iterator(); it.hasNext();) {
			String id2 = (String) it.next();
			System.out.println("\t" + id2 + "\t" + fixedIDs.get(id2));
		}
		System.out.println("Odd IDs");
		for (Iterator it = oddIDs.iterator(); it.hasNext();) {
			String id2 = (String) it.next();
			System.out.println("\t" + id2);
		}
		//if (true) return;
		checkScript("Latin", "Devanagari", "abcd", 20);
		checkScript("Latin", "Greek", "abcd", 20);
		checkScript("Latin", "Cyrillic", "abcd", 20);
		checkScript("Hiragana", "Katakana", "\u3041\u308F\u3099\u306E\u304B\u3092\u3099", 20);
		checkScript("Katakana", "Hiragana", "\u30A1\u30F7\u30CE\u30F5\u30F6", 20);
		//if (true) return;
		
    Transliterator.registerAny(); // do this last!
		
		//String[] files = dir.list();
//		List tryList = new ArrayList();
//		List tryListR = new ArrayList();
//		List failureList = new ArrayList();
//		List failureListR = new ArrayList();

		// before doing anything else, register 
		
//		Utility.registerTransliteratorFromFile(target, "Latin-ConjoiningJamo");
//		testString("Latin-ConjoiningJamo");
//
//		for (int i = 0; i < files.length; ++i) {
//			String id = files[i];
//			//System.out.println(id);
//			if (!id.endsWith(".txt")) {
//				continue;
//			}
//			id = files[i].substring(0,files[i].length()-4);
//			if (id.equals("root")) continue;
//			if (id.indexOf("Ethiopic") >= 0) continue;
//			id = id.replace('_', '-');
//			// tryList.add(id);
//			if (id.equals("CanadianAboriginal-Latin")) {
//				System.out.println("Debug");
//			}
//			if (id.equals("ConjoiningJamo-Latin")) continue;
//			if (id.equals("Thai-Latin")) continue;
//			Utility.registerTransliteratorFromFile(target, id);
//			testString(id);
//			//Transliterator t = Transliterator.getInstance("Latin-ConjoiningJamo");
//			//Transliterator t2 = Transliterator.getInstance("ConjoiningJamo-Latin");
//		}
        try{
    		Class ta = Class.forName("com.ibm.icu.dev.test.translit.TestAll");
    		Object testAll = ta.newInstance();
            //String[] params = new String[]{"-n "};
            Method m = ta.getDeclaredMethod("main", new Class[]{String[].class});
            System.out.println("Starting ICU Test");
            m.invoke(ta, new Object[]{args});
    		//TestAll.main(new String[]{"-n"});
        }catch(Exception ex){
            System.err.println("Could not load TestAll. Encountered exception: " + ex.toString());
        }
	}
	
	/*
	 *     
	 *     MergeLists<String> mergeLists = new MergeLists<String>(new TreeSet(new UTF16.StringComparator(true, false, 0)))
    .add(Arrays.asList("ldml"))
    .addAll(elementOrderings); // 
    List<String> result = mergeLists.merge();
    Collection badOrder = MergeLists.hasConsistentOrderWithEachOf(result, elementOrderings);
    if (badOrder != null) {
      throw new IllegalArgumentException("Failed to find good order: " + badOrder);
    }
	 */
  private static List<String> getDependentOrder(Collection<String> available) {
    MergeLists<String> mergeLists = new MergeLists<String>(new TreeSet(new UTF16.StringComparator(true, false, 0)));
    // We can't determine these from looking at the dependency lists, since they are used in the rules.
    mergeLists.add("Latin-NumericPinyin", "Tone-Digit", "Pinyin-NumericPinyin");
    mergeLists.add("NumericPinyin-Latin", "Digit-Tone", "NumericPinyin-Pinyin");
    mergeLists.add("Han-Latin", "Fullwidth-Halfwidth");
    mergeLists.add("Hiragana-Latin", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Katakana-Latin", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Latin-Hiragana", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Latin-Katakana", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    for (String oldId : available) {
      Transliterator t = Transliterator.getInstance(oldId);
      addDependingOn(mergeLists, oldId, t);
    }
    return mergeLists.merge();
  }

  private static Set<String> SKIP_DEPENDENCIES = new HashSet<String>();
  static {
    SKIP_DEPENDENCIES.add("%Pass1");
    SKIP_DEPENDENCIES.add("NFC(NFD)");
    SKIP_DEPENDENCIES.add("NFD(NFC)");
    SKIP_DEPENDENCIES.add("NFD");
    SKIP_DEPENDENCIES.add("NFC");
  }
  private static void addDependingOn(MergeLists<String> mergeLists, String oldId, Transliterator t) {
    Transliterator[] elements = t.getElements();
    for (Transliterator s : elements) {
      final String id = s.getID();
      if (id.equals(oldId) || SKIP_DEPENDENCIES.contains(id)) {
        continue;
      }
      mergeLists.add(oldId, id);
      addDependingOn(mergeLists, id, s);
    }
  }

  private static void removeTransliterators(Collection<String> available) {
    for (String oldId : available) {
		  Transliterator t;
      try {
        t = Transliterator.getInstance(oldId);
      } catch (Exception e) {
        System.out.println("Skipping: " + oldId);
        t = Transliterator.getInstance(oldId);
        continue;
      }
		  String className = t.getClass().getName();
		  if (className.endsWith(".CompoundTransliterator")
		          || className.endsWith(".RuleBasedTransliterator")
		          || className.endsWith(".AnyTransliterator")) {
        System.out.println("REMOVING: " + oldId);
        Transliterator.unregister(oldId);
		  } else {
	      System.out.println("Retaining: " + oldId + "\t\t" + className);
		  }
		}
  }
	static Matcher translitID = Pattern.compile("([^-]+)-([^/]+)+(?:[/](.+))?").matcher("");
	static Map fixedIDs = new TreeMap();
	static Set oddIDs = new TreeSet();
	
	private static void checkIdFix(String id) {
		if (fixedIDs.containsKey(id)) return;
		if (!translitID.reset(id).matches()) {
			System.out.println("Can't fix: " + id);
			fixedIDs.put(id, "?"+id);
			return;
		}
		String source1 = translitID.group(1);
		String target1 = translitID.group(2);
		String variant = translitID.group(3);
		String source = fixID(source1);
		String target = fixID(target1);
		fixedIDs.put(source1, source);
		fixedIDs.put(target1, target);
		if (variant != null) oddIDs.add("variant: " + variant);
	}
	private static String fixID(String source) {
		if (source.equals("Any")) return "und";
		if (source.equals("el")) return source;
		int[] scriptCodes = UScript.getCode(source);
		if (scriptCodes != null && scriptCodes.length == 1) {
			return "und_" + UScript.getShortName(scriptCodes[0]);
		}

		source = "x-" + source;
		oddIDs.add(source);
		return source;
	}
	private static void checkScript(String script1, String script2, String test, int limit) {
		Transliterator toLatin = Transliterator.getInstance(script1 + "-" + script2);
		Transliterator fromLatin = Transliterator.getInstance(script2 + "-" + script1);
		System.out.println(test);
		String nativeString = toLatin.transliterate(test);
		System.out.println("from " + script1 + " to " + script2 + ": " + nativeString);
		String back = fromLatin.transliterate(nativeString);
		System.out.println("from " + script2 + " to " +script1 + ": " + back);
		showTransliterator("", fromLatin, limit);
	}
	
	static void showTransliterator(String prefix, Transliterator t, int limit) {
		System.out.println(prefix + "ID:\t" + t.getID());
		System.out.println(prefix + "Class:\t" + t.getClass().getName());
		if (t.getFilter() != null) System.out.println(prefix + "Filter:\t" + t.getFilter());
		prefix += "\t";
		if (t instanceof RuleBasedTransliterator) {
			RuleBasedTransliterator rbt = (RuleBasedTransliterator)t;
			String[] rules = rbt.toRules(true).split("\n");
			int length = rules.length;
			if (limit >= 0 && limit < length) length = limit;
			for (int i = 0; i < length; ++i) {
				System.out.println(prefix + rules[i]);
			}
		} else {
			Transliterator[] elements = t.getElements();
			if (elements[0] == t) {
				System.out.println(prefix + "Other type.");
				return;
			}
			for (int i = 0; i < elements.length; ++i) {
				showTransliterator(prefix, elements[i], limit);
			}
		}
	}
	/*
	 * 		// The order of creation might matter, so try these multiple times.
		
		for (int j = 0; j < 4; ++j) {
			System.out.println("Pass " + j + "\t trying: " + tryList.size());
			for (int i = tryList.size()-1; i >= 0 ; --i) {
				String id = (String)tryList.get(i);
				try {
					Utility.registerTransliteratorFromFile(target, id, Transliterator.FORWARD);
				} catch (RuntimeException e) {
					System.out.println("**Skipping (F) in pass " + j + ": " + id);
					if (j >= 0) {
						e.printStackTrace(System.out);
					}
					failureList.add(id);
				}
			}
			int numberDone = tryList.size() - failureList.size();
			tryList.clear();
			tryList.addAll(failureList);
			failureList.clear();

			for (int i = tryListR.size()-1; i >= 0 ; --i) {
				String id = (String)tryListR.get(i);
				try {
					Utility.registerTransliteratorFromFile(target, id, Transliterator.REVERSE);
				} catch (RuntimeException e) {
					System.out.println("**Skipping (R) in pass " + j + ": " + id);
					if (j >= 0) {
						e.printStackTrace(System.out);
					}
					failureListR.add(id);
				}
			}
			int numberDone2 = tryListR.size() - failureList.size();
			tryListR.clear();
			tryListR.addAll(failureList);
			failureListR.clear();
			
			if (numberDone + numberDone2 == 0) {
				System.out.println("Failed to make progress! Aborting!");
				throw new RuntimeException("Failed to make progress!");
			}
			
			t = Transliterator.getInstance("Latin-ConjoiningJamo");
			t2 = Transliterator.getInstance("ConjoiningJamo-Latin");
		}

	 */
	private static void testString(String id) {
		int dir1 = Transliterator.FORWARD;
		int dir2 = Transliterator.REVERSE;
		if (id.startsWith("Latin")) {
			// continue;
		} else if (id.endsWith("Latin")) {
			dir2 = Transliterator.FORWARD;
			dir1 = Transliterator.REVERSE;
		} else {
			return;
		}
		Transliterator t = Transliterator.getInstance(id, dir1);
		String test = t.transliterate("abcde");
		//System.out.println(t.toRules(true));
		System.out.println("Test\t" + id + "\t" + dir1 + "\t" + test);
		Transliterator t2 = Transliterator.getInstance(id, dir2);
		//System.out.println(t2.toRules(true));
		String test2 = t2.transliterate(test);
		System.out.println("Test\t" + id + "\t" + dir2 + "\t" + test2);

	}
}