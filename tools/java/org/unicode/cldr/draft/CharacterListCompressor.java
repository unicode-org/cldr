package org.unicode.cldr.draft;

import java.util.*;
import java.util.regex.MatchResult;
import java.io.*;
import com.ibm.icu.text.UCharacterIterator;

/**
 * Compresses a String of Unicode characters with ranges denoted by PUA chars
 * into a Base88 string. The range is denoted by adding its (size - 1) to U+E000.
 * 
 * Compression usage:
 * String encodedStr = base88Encode("\uDBFF\uDFF0\uE00D\u0001");
 * 
 * Decompression usage:
 * String decodedStr = base88Decode(encodedStr);
 */

public class CharacterListCompressor {
  
  static class Interval {
    public Interval(int first2, int last2) {
      first = first2;
      last = last2;
    }
    int first;
    int last;
  }
  
  public static List<Integer> unicode2Base88(int code) {

    List<Integer> list = new ArrayList<Integer>();
    int rem = code % 88;
    list.add(rem);
    code = code/88;
    
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
      
      int value = getCodeFromListAt(list, i, leng)/metawindowsize;
      addPair(result, value, type);
      i += leng;
    }
    return result;
  }
  
  public static int getCodeFromListAt(List<Integer> list, int start, int leng) {
    int result = 0;
    for (int i = 0; i < leng; i++) {
      int c = list.get(start+i);
      result += c * Math.pow(88, i);
    }
    return result;
  }
  
  public static String StrList2String(String str) {
  
    StringBuilder sbuild = new StringBuilder();
    final UCharacterIterator it = UCharacterIterator.getInstance(str);
    int code;
    int currcode = 0;
    while((code = it.nextCodePoint())!= UCharacterIterator.DONE) {
      
      if (0xe000 <= code && code <= 0xf8ff) {
        int leng = code - 0xe000 + 1;
        for (int i = 0; i < leng; i++) {
          currcode++;
          sbuild.appendCodePoint(currcode);
        }
      }
      else {
        sbuild.appendCodePoint(code);
        currcode = code;
      }
    }
    
    return sbuild.toString();
  }
  
  public static boolean isEqual(String s1, String s2) {
  
    final UCharacterIterator it1 = UCharacterIterator.getInstance(StrList2String(s1));
    final UCharacterIterator it2 = UCharacterIterator.getInstance(StrList2String(s2));
    int c1 = 0;
    int c2 = 0;
    int count = 0;
    while(c1 == c2 && c1 != UCharacterIterator.DONE) {
      count ++;
      c1 = it1.nextCodePoint();
      c2 = it2.nextCodePoint();
      
      System.out.print("Comparing c1 = c2 = ");
      System.out.print(c1);
      System.out.print((char)c1);
      System.out.print(" ; count = ");
      System.out.println(count);
    }
    System.out.print(count);
    System.out.println(" characters compared");
   
    if (c1 != c2) {
      System.out.print("Mismatch at c1 = ");
      System.out.print(c1);
      System.out.print(" c2 = ");
      System.out.println(c2);
      return false;
    }
    return true;
  }
  
  public static void addPair(List<List<Integer>> pairs, int value, int type) {
    List <Integer>pair = new ArrayList<Integer>();
    pair.add(value);
    pair.add(type);
    pairs.add(pair);
  }
  
  public static List<List<Integer>> getValueTypePairsFromStr(String str) {
    List<List<Integer>> result = new ArrayList<List<Integer>>();
    int lastCode = 0;
    
    final UCharacterIterator it = UCharacterIterator.getInstance(str);
    int code;
    while((code = it.nextCodePoint())!= UCharacterIterator.DONE) {
       int type = 0;
       int value = 0;
       
       if ((0xe000 <= code) && (code < 0xf8ff)) {
         value = code - 0xe000;
         
         // range is big and spit it 
         int rangesize = 0x3c8; // 968 = 88 * 88 / 8
         while (value >= rangesize) { 
           
           addPair(result, rangesize - 1, 2); // rangesize chars - 0..(rangesize - 1)        
           value -= rangesize; // rangesize chars are already added above
           lastCode += rangesize;

           // add one more char
           addPair(result, 1, 0);
           value--;
           lastCode ++;
         }
         
         if (value >= 0) {
           addPair(result, value, 2);
           lastCode += value+1; // 0 means 1 char        
         }
       }
       else if (lastCode <= code) {
         addPair(result, code - lastCode, 0);
         lastCode = code;         
       }
       else if (lastCode > code) {
         addPair(result, lastCode - code, 1);
         lastCode = code;
       }
    }
    return result;
  }
  
  public static String getStrFromValueTypePairs(List<List<Integer>> pairs) {
    StringBuilder strBuild = new StringBuilder();
    
    int currCode = 0;
    for(int i = 0; i < pairs.size(); i++) {
      List<Integer> pair = pairs.get(i);
      
      int value = pair.get(0);
      int type = pair.get(1);
      
      if (type == 0) {
        currCode += value;
        strBuild.appendCodePoint(currCode);
      }
      if (type == 1) {
        currCode -= value;
        strBuild.appendCodePoint(currCode);
      }
      if (type == 2) {
        int code = 0xe000 + value;
        strBuild.appendCodePoint(code);
        currCode += value + 1;
      }
    }
    return strBuild.toString();
  }
  
  public static String encodeValueTypePairs2Base88(List<List<Integer>> pairs) {
    List<Integer> result = new ArrayList<Integer>();
    for(int i = 0; i < pairs.size(); i++) {
      List<Integer> pair = pairs.get(i);
      result.addAll(compressPair2Base88(pair));
    }
    return list2str(result);
  }
  
  public final static String ascii = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%()*+,-.:;<=>?@[]^_`{|}~";
  
  public static String list2str(List<Integer> list) {
    StringBuilder str=new StringBuilder();
    for(int i=0; i< list.size(); i++) {
      int code = list.get(i);
      
      str.append(ascii.charAt(code));
    }
    return str.toString();
  }
  
  public static List<Integer> str2list(String str) {
    List<Integer> list = new ArrayList<Integer>();
    for(int i=0; i<str.length(); i++) {
      char ch = str.charAt(i);
      int code = ascii.indexOf(ch);
      
      list.add(code);
    }
    return list;
  }

  public static String base88Encode(String str) {
    List<List<Integer>> pairs = getValueTypePairsFromStr(str);
    String encoded = encodeValueTypePairs2Base88(pairs);
    
    return encoded;
  }
  
  public static String base88Decode(String str) {
    List<List<Integer>> pairs = decodeBase88ToValueTypePairs(str);
    String decoded = getStrFromValueTypePairs(pairs);
    
    return decoded;
  }
  
  public static String base88EncodeList(List<Interval> intervalList) {
    // dummy implementation
    StringBuffer result = new StringBuffer();
    for (Interval interval : intervalList) {
      result.appendCodePoint(interval.first);
      result.appendCodePoint(interval.last);
    }
    return result.toString();
  }
  
  public static List<Interval> base88DecodeList(String base88String) {
    // dummy implementation
    List<Interval> result = new ArrayList<Interval>();
    for (int i = 0; i < base88String.length();) {
      int first = base88String.codePointAt(i);
      i += Character.charCount(first);
      int last = base88String.codePointAt(i);
      i += Character.charCount(last);
      result.add(new Interval(first,last));
    }
    return result;
  }

  public static void main(String[] args) {
  
    StringBuilder strBuild=new StringBuilder();
    try {
      Scanner sc = new Scanner(new File("/home/cibu/CharData.java"), "UTF-8");
      while (sc.hasNext()) {
        if (sc.findInLine("\\/\\*.*,\"(.*)\"},\\s*") != null) {
          MatchResult match = sc.match();
          strBuild.append(match.group(1));
        }
        else {
          sc.next();
        }
      }
    }
    catch (IOException ex){
      ex.printStackTrace();
    }

    String str = strBuild.toString();
    //str = "\\\\&";
    String encodedStr = base88Encode(str);
    String decodedStr = base88Decode(encodedStr);
    
    isEqual(str, decodedStr);
    
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/compressed.txt"));
      out.write(encodedStr);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/original.txt"));
      out.write(str);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
