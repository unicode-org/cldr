package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.text.RuleBasedTransliterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;
import com.ibm.icu.text.UnicodeSet;

public class TestTransforms {

  public static void main(String[] args) throws IOException {
    //checkRegistry();
    String source = Utility.getProperty("files", null, Utility.BASE_DIRECTORY + "dropbox/gen/icu-transforms/");
    boolean verbose = Utility.getProperty("verbose", false);
    PrintWriter out = verbose ? new PrintWriter(System.out) : null;

    CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");

    if (source == null) {
      CLDRTransforms transforms = CLDRTransforms.getinstance(out, ".*");
    } else {
      CLDRTransforms.registerFromIcuFormatFiles(source);
    }
    if (out != null) {
      out.flush();
    }
    
    CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");

    //if (true) return;
    //    checkScript("Latin", "Devanagari", "abcd", 20);
    //    checkScript("Latin", "Greek", "abcd", 20);
    //    checkScript("Latin", "Cyrillic", "abcd", 20);
    //    checkScript("Hiragana", "Katakana", "\u3041\u308F\u3099\u306E\u304B\u3092\u3099", 20);
    //    checkScript("Katakana", "Hiragana", "\u30A1\u30F7\u30CE\u30F5\u30F6", 20);
    //if (true) return;

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

  private static void checkRegistry() {
    CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");
    Transliterator t = Transliterator.createFromRules("foo", "a > b; ::[:greek:] halfwidth-fullwidth;", Transliterator.FORWARD);
    CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");
    Transliterator.registerInstance(t);
    CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");
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