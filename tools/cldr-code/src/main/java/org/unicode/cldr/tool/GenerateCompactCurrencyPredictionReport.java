package org.unicode.cldr.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;

public class GenerateCompactCurrencyPredictionReport {

  private static final Pattern NS_PATTERN = Pattern.compile("\\[@numberSystem=\"([^\"]+)\"\\]");
  private static final Pattern NUMERIC_PART_PATTERN = Pattern.compile("([#0,]+(?:\\.[#0]+)?)");

  private static final String[] POWERS = {
    "1000",
    "10000",
    "100000",
    "1000000",
    "10000000",
    "100000000",
    "1000000000",
    "10000000000",
    "100000000000",
    "1000000000000",
    "10000000000000",
    "100000000000000"
  };

  private static final String[] COUNTS = {"zero", "one", "two", "few", "many", "other"};

  public static class Template {
    final String prefix;
    final String suffix;

    Template(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    String apply(String compactDecimal) {
      if (compactDecimal == null) return null;
      if (isOnlyPlaceholder(compactDecimal)) {
        return compactDecimal;
      }
      return prefix + compactDecimal + suffix;
    }

    @Override
    public String toString() {
      return prefix + "{num}" + suffix;
    }
  }

  private static boolean isOnlyPlaceholder(String pattern) {
    if (pattern == null) return false;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c != '0' && c != '@' && c != '#') {
        return false;
      }
    }
    return true;
  }

  public static Template parseStandardPattern(String pattern) {
    if (pattern == null) return null;
    String posPattern = pattern.split(";")[0];

    Matcher matcher = NUMERIC_PART_PATTERN.matcher(posPattern);
    if (matcher.find()) {
      String numPart = matcher.group(1);
      int start = posPattern.indexOf(numPart);
      int end = start + numPart.length();

      String prefix = posPattern.substring(0, start);
      String suffix = posPattern.substring(end);
      return new Template(prefix, suffix);
    }
    return null;
  }

  private static Set<String> getNumberingSystems(CLDRFile resolvedFile, CLDRFile unresolvedFile) {
    Set<String> systems = new HashSet<>();

    String defaultNS = resolvedFile.getStringValue("//ldml/numbers/defaultNumberingSystem");
    if (defaultNS != null) {
      systems.add(defaultNS);
    }

    for (String type : new String[] {"native", "traditional", "financial"}) {
      String ns = resolvedFile.getStringValue("//ldml/numbers/otherNumberingSystems/" + type);
      if (ns != null) {
        systems.add(ns);
      }
    }

    for (String path : unresolvedFile) {
      if (path.contains("decimalFormats") || path.contains("currencyFormats")) {
        Matcher m = NS_PATTERN.matcher(path);
        if (m.find()) {
          systems.add(m.group(1));
        }
      }
    }

    if (systems.isEmpty()) {
      systems.add("latn");
    }
    return systems;
  }

  public static void main(String[] args) throws IOException {
    CLDRConfig config = CLDRConfig.getInstance();
    Factory factory = config.getCldrFactory();
    Set<String> locales = new TreeSet<>(factory.getAvailableLanguages());

    String outputPath = "../compact_currency_prediction_report.tsv";
    if (args.length > 0) {
      outputPath = args[0];
    }
    System.out.println("Writing report to " + outputPath);

    try (PrintWriter out = new PrintWriter(new FileWriter(outputPath))) {
      out.println(
          "Locale\tNS\tPower\tCount\tDecCompPattern\tStdCurrPattern\tStdCurrAltAlpha"
              + "\tActualCurrComp\tActualCurrCompAlpha\tPredictedCurrComp\tPredictedCurrCompAlpha"
              + "\tStdMatch\tAlphaMatch\tStdCategory\tAlphaCategory");

      StandardCodes sc = StandardCodes.make();
      for (String locale : locales) {
        Level cov = sc.getLocaleCoverageLevel("cldr", locale);
        if (!cov.isAtLeast(Level.MODERN)) {
          continue;
        }
        CLDRFile resolvedCldrFile = factory.make(locale, true);
        CLDRFile unresolvedCldrFile = factory.make(locale, false);
        Set<String> numberingSystems = getNumberingSystems(resolvedCldrFile, unresolvedCldrFile);

        for (String ns : numberingSystems) {
          if (ns.equals("finance") || ns.equals("traditional")) {
            continue;
          }

          // Get standard currency patterns for template
          String stdCurrPath =
              "//ldml/numbers/currencyFormats[@numberSystem=\""
                  + ns
                  + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
          String stdCurrAlphaPath =
              "//ldml/numbers/currencyFormats[@numberSystem=\""
                  + ns
                  + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"alphaNextToNumber\"]";

          String stdCurrPattern = resolvedCldrFile.getStringValue(stdCurrPath);
          String stdCurrAlphaPattern = resolvedCldrFile.getStringValue(stdCurrAlphaPath);

          Template stdTemplate = parseStandardPattern(stdCurrPattern);
          // Fallback for alpha template if not explicitly defined
          Template alphaTemplate =
              parseStandardPattern(
                  stdCurrAlphaPattern != null ? stdCurrAlphaPattern : stdCurrPattern);

          if (stdTemplate == null) {
            continue; // Cannot proceed without standard template
          }

          for (String power : POWERS) {
            for (String count : COUNTS) {
              String decCompPath =
                  "//ldml/numbers/decimalFormats[@numberSystem=\""
                      + ns
                      + "\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\""
                      + power
                      + "\"][@count=\""
                      + count
                      + "\"]";
              String accsPath =
                  "//ldml/numbers/currencyFormats[@numberSystem=\""
                      + ns
                      + "\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\""
                      + power
                      + "\"][@count=\""
                      + count
                      + "\"]";
              String accsAlphaPath =
                  "//ldml/numbers/currencyFormats[@numberSystem=\""
                      + ns
                      + "\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\""
                      + power
                      + "\"][@count=\""
                      + count
                      + "\"][@alt=\"alphaNextToNumber\"]";

              String decCompPattern = resolvedCldrFile.getStringValue(decCompPath);
              String actualCurrComp = resolvedCldrFile.getStringValue(accsPath);
              String actualCurrCompAlpha = resolvedCldrFile.getStringValue(accsAlphaPath);

              if (decCompPattern == null && actualCurrComp == null && actualCurrCompAlpha == null) {
                continue; // Skip if no data for this combination
              }

              // If decCompPattern is null, we can't predict.
              String predictedStd = stdTemplate.apply(decCompPattern);
              String predictedAlpha = alphaTemplate.apply(decCompPattern);

              boolean stdMatch = false;
              if (actualCurrComp == null && predictedStd == null) {
                stdMatch = true;
              } else if (actualCurrComp != null && actualCurrComp.equals(predictedStd)) {
                stdMatch = true;
              }

              boolean alphaMatch = false;
              String targetActualAlpha =
                  actualCurrCompAlpha != null ? actualCurrCompAlpha : actualCurrComp;

              if (targetActualAlpha == null && predictedAlpha == null) {
                alphaMatch = true;
              } else if (targetActualAlpha != null && targetActualAlpha.equals(predictedAlpha)) {
                alphaMatch = true;
              }

              String stdCategory = "EXISTS";
              if (actualCurrComp == null || actualCurrComp.isEmpty()) {
                stdCategory = "MISSING";
              }

              String alphaCategory = "EXISTS";
              if (actualCurrCompAlpha == null || actualCurrCompAlpha.isEmpty()) {
                alphaCategory = "MISSING";
              } else if (actualCurrCompAlpha.equals(actualCurrComp)) {
                alphaCategory = "FALLBACK";
              }

              out.printf(
                  "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%b\t%b\t%s\t%s\n",
                  locale,
                  ns,
                  power,
                  count,
                  decCompPattern != null ? decCompPattern : "",
                  stdCurrPattern != null ? stdCurrPattern : "",
                  stdCurrAlphaPattern != null ? stdCurrAlphaPattern : "",
                  actualCurrComp != null ? actualCurrComp : "",
                  actualCurrCompAlpha != null ? actualCurrCompAlpha : "",
                  predictedStd != null ? predictedStd : "",
                  predictedAlpha != null ? predictedAlpha : "",
                  stdMatch,
                  alphaMatch,
                  stdCategory,
                  alphaCategory);
            }
          }
        }
      }
    }
  }
}
