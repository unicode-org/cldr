package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.test.util.BagFormatter;

public class GetDescriptions {
  
  static Matcher matcher = Pattern.compile("([^,(]+)(,([^(]+))?(.*)").matcher("");
  
  static Map<String,String> items = new TreeMap<String,String>();
  static int allCount = 1;
  static int commaCount = 1;

  private static Map<String, Map<String,Set<String>>> name_type_codes = new TreeMap<String, Map<String,Set<String>>>();

  private static Set<String> preCommas = new TreeSet<String>();

  private static Set<String> postCommas = new TreeSet<String>();

  private static Map<String,String> descriptionWithoutComments = new TreeMap<String,String>();

  private static Set<String> uninvertedNames = new HashSet<String>();

  public static void main(String[] args) throws IOException {
    StandardCodes sc = StandardCodes.make();
    PrintWriter commas = BagFormatter.openUTF8Writer("c:\\data\\gen\\ltru\\","ltru-commas.txt");
    commas.write('\uFEFF');
    PrintWriter all = BagFormatter.openUTF8Writer("c:\\data\\gen\\ltru\\","ltru-all.txt");
    all.write('\uFEFF');
    
    for (String type : (Set<String>) sc.getAvailableTypes()) {
      if (type.equals("tzid")) continue;
      if (type.equals("currency")) continue;
      for (String code : (Set<String>) sc.getAvailableCodes(type)) {
        Map x = sc.getLangData(type, code);
        if (x == null) {
          continue;
        }
        boolean isDeprecated = x.get("Deprecated") != null;
        
        all.println(allCount++ + "\t" + type + "\t" + code + "\t" + x);
        String descriptionField = (String) x.get("Description");
        String[] descriptions = descriptionField.split("\u25AA");
        items.clear();
        
        for (String description : descriptions) {
          if (!matcher.reset(description).matches()) {
            commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + description + "\t@NO_MATCH");
            continue;
          }
          String preComma = matcher.group(1).trim();
          String postComma = matcher.group(3);
          postComma = postComma == null ? "" : postComma.trim();
          String parens = matcher.group(4);
          parens = parens == null ? "" : parens.trim();
          
          if (preComma.length() != 0) preCommas.add(preComma);
          if (postComma.length() != 0) postCommas.add(postComma);
          
          String newDescription = preComma;
          
          String descriptionWithoutComment = preComma;          
          String newDescriptionWithoutComment = preComma;
          uninvertedNames.add(newDescriptionWithoutComment);
          
          if (postComma.length() != 0) {
            descriptionWithoutComment += ", " + postComma;
            newDescription = postComma + " " + newDescription;
            newDescriptionWithoutComment = newDescription;
          }
          if (parens.length() != 0) {
            newDescription += " " + parens;
          }
          
          if (!isDeprecated) {
            if (descriptionWithoutComment.length() != 0) descriptionWithoutComments.put(descriptionWithoutComment,newDescriptionWithoutComment);
            addTypeNameCode(name_type_codes, type, code, newDescriptionWithoutComment);
          }
          
          if (!descriptionField.contains(",") && !descriptionField.contains("(")) {
            continue;
          }
          
          checkDuplicates(commas, type, code, descriptionWithoutComment, description);
          if (!newDescriptionWithoutComment.equals(descriptionWithoutComment)) {
            checkDuplicates(commas, type, code, newDescriptionWithoutComment, description);
          }
          
          if (postComma.contains(",")) {
            commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + description + "\t@DOUBLE_COMMA");
            continue;
          }

          if (postComma.length() == 0) {
            commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + description);
            continue;
          }

          commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + description + "\t=>\t" + newDescription);
        }
        checkInversion(commas, type, code, descriptions);
      }
    }
    all.close();
    commas.close();
    showReverse();
    System.out.println("DONE");
  }

  private static void showReverse() throws IOException {
    PrintWriter reverse = BagFormatter.openUTF8Writer("c:\\data\\gen\\ltru\\", "ltru-reverse.txt");
    reverse.write('\uFEFF');
    int reverseCount = 1;
    for (String name : name_type_codes.keySet()) {
      boolean privateUse = name.equals("PRIVATE USE");
      Map<String, Set<String>> type_codes = name_type_codes.get(name);
      Set<String> types = type_codes.keySet();
      for (String type : type_codes.keySet()) {
        String baseCode = null;
        for (String code : type_codes.get(type)) {
          if (baseCode == null || privateUse) {
            baseCode = code;
            reverse.println(reverseCount++ + "\t" + name + "\t" + type + "\t" + code);
            continue;
          }
          reverse.println(reverseCount++ + "\t" + name + "\t" + type + "\t" + code + "\t@DUPLICATE_IN\t" + "\t" + baseCode);
        }
      }
      reverseIfPossible(name, types);
    }
    reverse.close();
    reverseCount = 1;
    PrintWriter inversions = BagFormatter.openUTF8Writer("c:\\data\\gen\\ltru\\", "ltru-inversions.txt");
    for (String invertedName : descriptionWithoutComments.keySet()) {
      String name = descriptionWithoutComments.get(invertedName);
      if (name.equals(invertedName)) continue;
      inversions.println(reverseCount++ + "\t" + invertedName + "\t" + name);
    }
    inversions.close();
  }
  
  static void reverseIfPossible(String name, Set<String> types) {
    for (String uninvert : uninvertedNames) {
      if (name.endsWith(uninvert)) {
        addEnd(name, uninvert, types);
      }
      if (name.startsWith(uninvert)) {
        addStart(name, uninvert, types);
      }
    }
    for (String preComma : preCommas) {
      if (name.endsWith(preComma)) {
        addEnd(name, preComma, types);
      }
    }
    for (String postComma : postCommas) {
      if (name.startsWith(postComma)) {
        addStart(name, postComma, types);
      }
    }
  }

  private static void addStart(String name, String postComma, Set<String> types) {
    if (name.equals(postComma)) return;
    if (!name.startsWith(postComma+" ")) return;
    String trial = name.substring(postComma.length()).trim() + ", " + postComma;
    if (descriptionWithoutComments.keySet().contains(trial)) {
      return;
    }
    descriptionWithoutComments.put(trial, name + "\t@MISSING\t" + types);
  }

  private static void addEnd(String name, String preComma, Set<String> types) {
    if (name.equals(preComma)) return;
    if (!name.endsWith(" "+preComma)) return;
    String trial = preComma + ", " + name.substring(0,name.length()-preComma.length()).trim();
    if (descriptionWithoutComments.keySet().contains(trial)) {
      return;
    }
    descriptionWithoutComments.put(trial, name + "\t@MISSING\t" + types);
  }

  private static void addTypeNameCode(Map<String,Map<String,Set<String>>> name_type_codes, String type, String code, String newDescriptionWithoutComment) {
    Map<String,Set<String>> type_codes = name_type_codes.get(newDescriptionWithoutComment);
    if (type_codes == null) name_type_codes.put(newDescriptionWithoutComment, type_codes = new TreeMap<String,Set<String>>());
    Set<String> codes = type_codes.get(type);
    if (codes == null) type_codes.put(type, codes = new TreeSet<String>());
    codes.add(code);
  }

  static Matcher directional = Pattern.compile("(West Central|Northern|Southern|Western|Eastern|North|South|East|West|Central|Ancient|Classical|Coastal" +
      "|Highland|Isthmus|Low|Lower|Lowland|Middle|Northeastern|Northwestern|Old|Plains|Southeastern|Southwestern|Straits|Upper|Valley" +
      "|Written)\\s+(.+)").matcher("");
  
  private static void checkInversion(PrintWriter commas, String type, String code, String[] parts) {
    Set<String> items = new TreeSet<String>(Arrays.asList(parts));
    for (String item : items) {
      if (!directional.reset(item).matches()) {
        continue;
      }
      String trial = directional.group(2) + (directional.group(2).contains(",") ? " " : ", ") + directional.group(1);
      if (!items.contains(trial)) {
        commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + "\t@MISSING\t" + trial);
      }
    }
  }

  private static void checkDuplicates(PrintWriter commas, String type, String code, String newPartNoComment, String part) {
    String old = items.get(newPartNoComment);
    if (old != null) {
      commas.println(commaCount++ + "\t" + type + "\t" + code + "\t" + part + "\t@DUPLICATES\t" + old);
    } else {
      items.put(newPartNoComment, part);
    }
  }
}