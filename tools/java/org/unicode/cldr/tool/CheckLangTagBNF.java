/**
 * 
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Quoter;

/**
 * Tests language tags.
 * <p>Internally, it generates a Regex Pattern for BCP 47 language tags, plus an ICU BNF pattern.
 * The first is a regular Java/Perl style pattern.
 * The ICU BNF will general random strings that will match that regex.
 * <p>Use -Dbnf=xxx for the source regex definition file, and -Dtest=yyy for the test file
 * @author markdavis
 *
 */
class CheckLangTagBNF {
  private static final String LANGUAGE_TAG_TEST_FILE = Utility.getProperty("test");
  private static final String BNF_DEFINITION_FILE = Utility.getProperty("bnf");

  private String rules;
  private String generationRules;
  private Pattern pattern;
  private BNF bnf;
  
  private static final String[] groupNames = {"whole", "lang", "script", "region", "variants", "extensions", 
  			"privateuse", "grandfathered", "privateuse"
  	};
  
  /**
   * Set the regex to use for testing, based on the contents of a file.
   * @param filename
   * @return
   * @throws IOException
   */
  public CheckLangTagBNF setFromFile(String filename) throws IOException {
    BufferedReader in = BagFormatter.openUTF8Reader("", filename);
    Utility.VariableReplacer result = new Utility.VariableReplacer();
    String variable = null;
    StringBuffer definition = new StringBuffer();
    StringBuffer ruleBuffer = new StringBuffer();
    StringBuffer generationRuleBuffer = new StringBuffer();
    for (int count = 1; ; ++count) {
      String line = in.readLine();
      if (line == null) break;
      ruleBuffer.append(line).append("\r\n");
      // remove initial bom, comments
      if (line.length() == 0) continue;
      if (line.charAt(0) == '\uFEFF') line = line.substring(1);
      int hashPos = line.indexOf('#');
      if (hashPos >= 0) line = line.substring(0, hashPos);
      line = line.trim(); // this may seem redundant, but we need it for the test for final ;
      if (line.length() == 0) continue;
      generationRuleBuffer.append(line).append("\r\n");
      
      String[] lineParts = line.split(";");
      for (int i = 0; i < lineParts.length; ++i) {
        String linePart = lineParts[i].trim();
        if (linePart.length() == 0) continue;
        int equalsPos = linePart.indexOf('=');
        if (equalsPos >= 0) {
          if (variable != null) {
            throw new IllegalArgumentException("Missing ';' before " + count + ") " + line);
          }
          variable = linePart.substring(0,equalsPos).trim();
          definition.append(linePart.substring(equalsPos+1).trim());
        } else { // no equals, so
          if (variable == null) {
            throw new IllegalArgumentException("Missing ':=' at " + count + ") " + line);
          }
          definition.append(linePart);
        }
        // we are terminated if i is not at the end, or the line ends with a ;
        if (i < lineParts.length - 1 || line.endsWith(";")) {
          result.add(variable, result.replace(definition.toString()));
          variable = null; // signal we have no variable
          definition.setLength(0);
        }
      }
    }
    if (variable != null) {
      throw new IllegalArgumentException("Missing ';' at end");
    }
    String resolved = result.replace("$root").replaceAll("[0-9]+%", "");
    System.out.println("Regex: " + resolved);
    rules = ruleBuffer.toString();
    generationRules = generationRuleBuffer.toString().replaceAll("\\?:", "").replaceAll("\\(\\?i\\)","");
    pattern = Pattern.compile(resolved, Pattern.COMMENTS);
    return this;
  }
  
  public BNF getBnf() {
    if (bnf != null) return bnf;
    bnf = new BNF(new Random(2), new Quoter.RuleQuoter())
    .setMaxRepeat(5)
    .addRules(generationRules)
    .complete();
    return bnf;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public String getRules() {
    return rules;
  }

  public String getGenerationRules() {
    return generationRules;
  }

  /**
   * Tests a file for correctness. 
   * There are two special lines in the file: WELL-FORMED and ILL-FORMED,
   * that signal the start of each section.
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
      CheckLangTagBNF bnfData = new CheckLangTagBNF();
      bnfData.setFromFile(BNF_DEFINITION_FILE);
      String contents = bnfData.getRules();
      Pattern pat = bnfData.getPattern();
      Matcher regexLanguageTag = pat.matcher("");
  
      BNF bnf = bnfData.getBnf();
      for (int i = 0; i < 100; ++i) {
        String trial = bnf.next();
        System.out.println(trial);
        if (!regexLanguageTag.reset(trial).matches()) {
          throw new IllegalArgumentException("Regex generation fails with: " + trial);
        }
      }
      
      // generate a bunch of ill-formed items. Try to favor ones that might actually cause problems.
      // TODO make all numeric and all alpha more common
      System.out.println("*** ILL-FORMED ***");
      BNF invalidBNF = new BNF(new Random(0), new Quoter.RuleQuoter())
      .setMaxRepeat(5)
      .addRules("$tag = ([A-Z a-z 0-9]{1,8} 50% 20% 10% 5% 5% 5% 5%);")
      .addRules("$s = [-_] ;")
      .addRules("$root = $tag ($s $tag){0,7} 10% 10% 10% 10% 10% 10% 10% 10% ; ")
      .complete();
      
      for (int i = 0; i < 100; ++i) {
        String trial = invalidBNF.next();
        if (regexLanguageTag.reset(trial).matches()) {
          continue;
        }
        System.out.println(trial);
      }
      
      System.out.println(contents);
      
  //		System.out.println(langTagPattern);
  //		System.out.println(cleanedLangTagPattern);
  		StandardCodes sc = StandardCodes.make();
  		Set grandfathered = sc.getAvailableCodes("grandfathered");
  //		for (Iterator it = grandfathered.iterator(); it.hasNext();) {
  //			System.out.print(it.next() + " | ");
  //		}
  //    System.out.println();
      
  		LanguageTagParser ltp = new LanguageTagParser();
      boolean expected = true;
      int errorCount = 0;
      BufferedReader in = BagFormatter.openUTF8Reader("", LANGUAGE_TAG_TEST_FILE);
      
  		for (int i = 0; ; ++i) {
        String test = in.readLine();
        if (test == null) break;
  
        // remove initial bom, comments
        if (test.length() == 0) continue;
        if (test.charAt(0) == '\uFEFF') test = test.substring(1);
        int hashPos = test.indexOf('#');
        if (hashPos >= 0) test = test.substring(0, hashPos);
        test = test.trim(); // this may seem redundant, but we need it for the test for final ;
        if (test.length() == 0) continue;
  
        if (test.equalsIgnoreCase("WELL-FORMED")) {
          expected = true;
          continue;
        } else if (test.equalsIgnoreCase("ILL-FORMED")) {
          expected = false;
          continue;
        }
  			try {
  				System.out.println("Parsing " + test);
  				ltp.set(test);
  				if (ltp.getLanguage().length() != 0) System.out.println("\tlang:    \t" + ltp.getLanguage() + (ltp.isGrandfathered() ? " (grandfathered)" : ""));
  				if (ltp.getScript().length() != 0) System.out.println("\tscript:\t" + ltp.getScript());
  				if (ltp.getRegion().length() != 0) System.out.println("\tregion:\t" + ltp.getRegion());
  				if (ltp.getVariants().size() != 0) System.out.println("\tvariants:\t" + ltp.getVariants());
  				if (ltp.getExtensions().size() != 0) System.out.println("\textensions:\t" + ltp.getExtensions());
  				System.out.println("\tisValid?\t" + ltp.isValid());
  			} catch (Exception e) {
  				System.out.println("\t" + e.getMessage());
  				System.out.println("\tisValid?\tfalse");
  			}
  			boolean matches = regexLanguageTag.reset(test).matches();
        if (matches != expected) {
          System.out.println("*** TEST FAILURE ***");
          ++errorCount;
        }
  			System.out.println("\tregex?\t" + matches + (matches == expected ? "" : "\t EXPECTED: " + expected + " for\t" + test));
  			if (matches) {
  				for (int j = 0; j <= regexLanguageTag.groupCount(); ++j) {
  					String g = regexLanguageTag.group(j);
  					if (g == null || g.length() == 0) continue;
  					System.out.println("\t" + j + "\t" + CheckLangTagBNF.groupNames[j] + ":\t" + g);
  				}
  			}
  		}
      System.out.println("Error count: " + errorCount);
  	}
}