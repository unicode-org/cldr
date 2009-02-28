package org.unicode.cldr.draft;

import java.text.*;
import java.util.*;
import java.util.regex.MatchResult;
import java.io.*;

public class CharacterListCompressor {
  public static List<Integer> unicode2Base88(int code) {

    List<Integer> list = new ArrayList<Integer>();

    int rem = code % 88;
    list.add(rem);
    code = code / 88;

    if (code != 0) {
      rem = code % 88;
      list.add(rem);
      code = code / 88;
    }
    if (code != 0) {
      rem = code % 88;

      list.add(rem);
      code = code / 88;

      rem = code % 88;
      list.add(rem);
      code = code / 88;
    }
    return list;
  }

  public static int byteCount4Base88(int code) {

    int count = 0;
    code = code / 88;

    if (code != 0) {
      count = 1;
      code = code / 88;
    }
    if (code != 0) {
      count = 2;
    }
    return count;
  }

  public static List<Integer> compressPair2Base88(List<Integer> pair) {
    int value = pair.get(0);
    int type = pair.get(1);
    int code = value * 8 + type * 3;
    code += byteCount4Base88(code);

    return unicode2Base88(code);
  }

  public static List<List<Integer>> decodeBase88ToValueTypePairs(String str) {
    List<Integer> list = str2list(str);

    int metawindowsize = 8;

    List<List<Integer>> result = new ArrayList<List<Integer>>();
    int i = 0;

    while (i < list.size()) {
      int c = list.get(i);
      int meta = c % metawindowsize;

      int type = meta / 3;
      int leng = (meta % 3) + 1;

      if (leng == 3) {
        leng++;
      }

      int value = getCodeFromListAt(list, i, leng) / metawindowsize;
      List<Integer> pair = new ArrayList<Integer>();

      pair.add(value);
      pair.add(type);
      result.add(pair);
      i += leng;
    }
    return result;
  }

  public static int getCodeFromListAt(List<Integer> list, int start, int leng) {

    int result = 0;
    for (int i = 0; i < leng; i++) {
      int c = list.get(start + i);
      result += c * Math.pow(88, i);
    }
    return result;
  }

  public static List<List<Integer>> getValueTypePairsFromStr(String str) {

    List<List<Integer>> result = new ArrayList<List<Integer>>();
    int currCharCode = 0;

    final CharacterIterator it = new StringCharacterIterator(str);
    for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()) {

      int code = ch;
      int type = 0;
      int value = 0;
      if ((0xe000 <= code) && (code < 0xf8ff)) {
        type = 2;
        value = code - 0xe000;

        // range is big and spit it

        while (value >= 968) { // 968 = 88 * 88 / 8

          List<Integer> pair = new ArrayList<Integer>();
          pair.add(967); // 968 chars - 0..967
          pair.add(type);

          result.add(pair);

          value -= 968; // 968 chars are already added above
          currCharCode += 968;
        }
        currCharCode += value + 1; // 0 means 1 char
      }

      else if (currCharCode < code) {
        type = 0;
        value = code - currCharCode;
        currCharCode = code;
      } else if (code < currCharCode) {
        type = 1;
        value = currCharCode - code;

        currCharCode = code;
      }
      List<Integer> pair = new ArrayList<Integer>();
      pair.add(value);
      pair.add(type);
      result.add(pair);
    }
    return result;

  }

  public static String getStrFromValueTypePairs(List<List<Integer>> pairs) {
    StringBuilder strBuild = new StringBuilder();
    int currCode = 0;
    for (int i = 0; i < pairs.size(); i++) {

      List<Integer> pair = pairs.get(i);

      int value = pair.get(0);
      int type = pair.get(1);

      if (type == 0) {
        currCode += value;
        strBuild.append((char) currCode);

      }
      if (type == 1) {
        currCode -= value;
        strBuild.append((char) currCode);
      }
      if (type == 2) {
        int code = 0xe000 + value;
        strBuild.append((char) code);

        currCode += value + 1;
      }
    }
    return strBuild.toString();
  }

  public static String encodeValueTypePairs2Base88(List<List<Integer>> pairs) {
    List<Integer> result = new ArrayList<Integer>();

    for (int i = 0; i < pairs.size(); i++) {
      List<Integer> pair = pairs.get(i);
      result.addAll(compressPair2Base88(pair));
    }
    return list2str(result);
  }

  public final static String ascii = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%()*+,-.:;<=>?@[]^_`{|}~";

  public static String list2str(List<Integer> list) {
    StringBuilder str = new StringBuilder();
    for (int i = 0; i < list.size(); i++) {
      int code = list.get(i);

      str.append(ascii.charAt(code));

    }
    return str.toString();
  }

  public static List<Integer> str2list(String str) {
    List<Integer> list = new ArrayList<Integer>();
    for (int i = 0; i < str.length(); i++) {

      char ch = str.charAt(i);
      int code = ascii.indexOf(ch);

      list.add(code);
    }
    return list;
  }

  public static void main(String[] args) {

    StringBuilder strBuild = new StringBuilder();

    try {
      Scanner sc = new Scanner(new File("/home/cibu/CharData.java"), "UTF-8");
      while (sc.hasNext()) {
        if (sc.findInLine("\\/\\*.*,\"(.*)\"},\\s*") != null) {

          MatchResult match = sc.match();
          strBuild.append(match.group(1));
        } else {
          sc.next();
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();

    }

    String str = strBuild.toString();
    str = "\uDBFF\uDFFD";
    List<List<Integer>> pairs = getValueTypePairsFromStr(str);
    String compressed = encodeValueTypePairs2Base88(pairs);

    List<List<Integer>> decompairs = decodeBase88ToValueTypePairs(compressed);
    String resultStr = getStrFromValueTypePairs(decompairs);

    System.out.println(pairs.equals(decompairs) ? "Matched!" : "Did not match!!");

  }
}
