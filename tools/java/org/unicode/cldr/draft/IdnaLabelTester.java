package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.VariableReplacer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class IdnaLabelTester {

  private static final boolean DEBUG = false;

  enum Result {none, next, fail};

  static class Rule {
    final Matcher before;
    final Matcher at;
    final Result result;
    final String title;
    final int lineNumber;
    int length;
    String label;

    public Rule(String before, String at, String result, String title, int lineNumber) {
      if (before != null) {
        before = before.trim();
      }
      this.before = before == null || before == "" ? null 
              : Pattern.compile(".*" + before, Pattern.COMMENTS).matcher(""); // hack, because Java doesn't have lookingBefore
      this.at = Pattern.compile(at.trim(), Pattern.COMMENTS).matcher("");
      this.result = Result.valueOf(result.toLowerCase().trim());
      this.title = title;
      this.lineNumber = lineNumber;
    }

    public Result match(int position) {
      if (before != null) {
        before.region(0, position);
        //before.reset(label.substring(0,position));
        if (!before.matches()) {
          return Result.none;
        }
      }
      at.region(position, length);
      //at.reset(label.substring(position,length));
      if (!at.lookingAt()) {
        return Result.none;
      }

      return result;
    }

    public void setLabel(String label) {
      this.label = label;
      if (before != null) {
        before.reset(label);
      }
      at.reset(label);
      length = label.length();
    }
  }

  private List<Rule> rules = new ArrayList<Rule>();

  public IdnaLabelTester(String file) throws IOException {

    VariableReplacer variables = new VariableReplacer();
    BufferedReader in = openFile(file);

    String title = "???";
    for (int lineCount = 1; ; ++lineCount) {
      String line = in.readLine();
      if (line == null) break;
      int commentPos = line.indexOf("#");
      if (commentPos >= 0) {
        line = line.substring(0,commentPos);
      }
      line = line.trim();
      if (line.length() == 0) continue;

      // do title

      if (line.startsWith("Title:")) {
        title = line.substring(6).trim();
        continue;
      }

      // do variables

      if (line.startsWith("$")) {
        int equals = line.indexOf("=");
        if (equals >= 0) {
          final String variable = line.substring(0,equals).trim();
          UnicodeSet s = new UnicodeSet(variables.replace(line.substring(equals+1).trim())).complement().complement();
          if (DEBUG) {
            System.out.println(variable + "\t=\t" + s.toPattern(false));
          }
          variables.add(variable, s.toPattern(false));
          continue;
        }
      }

      // do rules. This could be much more compact, but is broken out for debugging

      String[] pieces = line.split("\\s*;\\s*");
      if (DEBUG) {
        System.out.println(Arrays.asList(pieces));
      }
      String before, at, result;
      switch (pieces.length) {
        case 2: before = null; at = variables.replace(pieces[0]); result= pieces[1]; break;
        case 3: before = variables.replace(pieces[0]); at = variables.replace(pieces[1]); result= pieces[2]; break;
        default: throw new IllegalArgumentException(line + " => " + Arrays.asList(pieces));
      }
      Rule rule = new Rule(before, at, result, title, lineCount);
      rules.add(rule);
    }
    in.close();
  }

  private static BufferedReader openFile(String file) throws IOException {
    try {
      return GenerateNormalizeForMatch.openUTF8Reader(file);
    } catch (Exception e) {
      File f = new File(file);
      throw new IllegalArgumentException("Bad file name: " + f.getCanonicalPath());
    }
  }

  static class TestStatus {
    String title;
    int position;
    int ruleLine;

    public TestStatus(int position, String title, int lineNumber) {
      this.position = position;
      this.title = title;
      this.ruleLine = lineNumber;
    }
  }

  /**
   * Test a label; null for success.
   * Later, return information.
   * @param label
   * @return
   */

  public TestStatus test(String label) {
    // initialize
    for (Rule rule : rules) {
      rule.setLabel(label);
    }
    charLoop:
      // note: it doesn't matter if we test in the middle of a supplemental character
      for (int i= 0; i < label.length(); ++i) {
        for (Rule rule : rules) {
          Result result = rule.match(i);
          switch (result) {
            case next: 
              if (DEBUG) {
                rule.match(i);
              }
              continue charLoop;
            case fail: 
              if (DEBUG) {
                rule.match(i);
              }
              return new TestStatus(i, rule.title, rule.lineNumber);
            default:
              if (DEBUG) {
                rule.match(i);
              }
            break;
          }
        }
      }
    return null; // success!
  }

  public static void main(String[] args) throws IOException {
    String dir = "java/org/unicode/cldr/draft/";
    IdnaLabelTester tester = new IdnaLabelTester(dir + "idnaContextRules.txt");
    Transliterator unescaper = Transliterator.getInstance("hex-any");
    Transliterator escaper = Transliterator.getInstance("[[:z:][:me:][:mn:][:di:]-[:cn:]] any-hex");
    BufferedReader in = openFile(dir + "idnaTestCases.txt");
    boolean expectedSuccess = true;
    int failures = 0;
    int successes = 0;

    for (int lineCount = 1; ; ++lineCount) {
      String line = in.readLine();
      if (line == null) break;
      int commentPos = line.indexOf("#");
      if (commentPos >= 0) {
        line = line.substring(0,commentPos);
      }
      line = unescaper.transform(line);

      line = line.trim();
      if (line.length() == 0) continue;

      if ("valid".equalsIgnoreCase(line)) {
        expectedSuccess = true;
        continue;
      } else if ("invalid".equalsIgnoreCase(line)) {
        expectedSuccess = false;
        continue;
      }

      TestStatus result = tester.test(line);

      boolean showLine = false;
      if (result == null) {
        if (expectedSuccess) {
          if (DEBUG) {
            System.out.print(lineCount + ". \tSuccess - expected and got Valid:\t");
            showLine = true;
          }
          successes++;
        } else {
          System.out.print(lineCount + ". \tFAILURE - expected Invalid, was valid:\t");
          failures++;
          showLine = true;
        }
        if (showLine) {
          System.out.println(escaper.transform(line));
        }
      } else {
        if (expectedSuccess) {
          System.out.print(lineCount + ". \tFAILURE - expected Valid, was invalid:\t");
          failures++;
          showLine = true;
        } else {
          if (DEBUG) {
            System.out.print(lineCount + ". \tSuccess - expected and got Invalid:\t");
            showLine = true;
          }
          successes++;
        }
        if (showLine) {
          System.out.println(escaper.transform(line.substring(0, result.position)) 
                  + "\u2639" + escaper.transform(line.substring(result.position)) 
                  + "\t\t" + result.title
                  + "; \tRuleLine: " + result.ruleLine);
        }
      }
    }
    System.out.println("Successes:\t" + successes);
    System.out.println("Failures:\t" + failures);
    in.close();
  }
}
