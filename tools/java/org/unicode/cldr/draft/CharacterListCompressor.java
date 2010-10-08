package org.unicode.cldr.draft;

import java.util.*;
import java.util.regex.MatchResult;
import java.io.*;
import com.ibm.icu.text.UCharacterIterator;

/**
 * Compresses list of Unicode character ranges given as starting and ending char
 * into a Base88 string.
 * 
 * Compression usage:
 * String encodedStr = base88EncodeList(List<Interval>);
 * 
 * Decompression usage:
 * List<Interval> decodedStrList = base88DecodeList(encodedStr);
 * 
 * Interval has two integers - first, last - to represent the range.
 */

public class CharacterListCompressor {
  
  static class Interval {
    int first;
    int last;
    
    public Interval(int first, int last) {
      this.first = first;
      this.last = last;
    }
    
  }
  
  //
  // Pairs to Base88 methods
  //
  
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
  
  public static void addPair(List<List<Integer>> pairs, int value, int type) {
    List <Integer>pair = new ArrayList<Integer>();
    pair.add(value);
    pair.add(type);
    pairs.add(pair);
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

  
  public static String base88EncodeList(List<Interval> intervalList) {
    List<List<Integer>> pairs = getValueTypePairsFromStrRangeList(intervalList);
    String encoded = encodeValueTypePairs2Base88(pairs);
    
    return encoded;
  }
  
  public static List<Interval> base88DecodeList(String base88String) {
    List<List<Integer>> pairs = decodeBase88ToValueTypePairs(base88String);
    List<Interval> decoded = getStrRangeListFromValueTypePairs(pairs);
    
    return decoded;
  }
  // end of compression methods
  
  // Value Type pairs -- Str Range List
  public static List<List<Integer>> getValueTypePairsFromStrRangeList(List<Interval> ilist) {
    List<List<Integer>> result = new ArrayList<List<Integer>>();
    int lastCode = 0;
    
    for(int i = 0; i < ilist.size(); i++) {
       int value = 0;
       int first = ilist.get(i).first;
       int last = ilist.get(i).last;
       
       if (lastCode < first) {
         addPair(result, first - lastCode - 1, 0);
       }
       else if (lastCode > first) {
         addPair(result, lastCode - first - 1, 1);
       }
       else if (lastCode == first) {
         System.out.println("I am not expecting two contiguous chars to be the same");
       }
       lastCode = first;

       if (first < last) {
         value = last - first - 1;
         
         // range is big and spit it 
         int rangesize = 0x3c8; // 968 = 88 * 88 / 8
         while (value >= rangesize) { 
           
           addPair(result, rangesize - 1, 2); // rangesize chars - 0..(rangesize - 1)        
           value -= rangesize; // rangesize chars are already added above
           lastCode += rangesize;
         }
         addPair(result, value, 2);
         lastCode = last;
       }
    }
    return result;
  }
  
  public static List<Interval> getStrRangeListFromValueTypePairs(List<List<Integer>> pairs) {
    ArrayList<Interval> result = new ArrayList<Interval>();
    
    int lastCode = 0;
    for(int i = 0; i < pairs.size(); i++) {
      List<Integer> pair = pairs.get(i);
      
      int value = pair.get(0);
      int type = pair.get(1);
      
      if (type == 0) {
        lastCode += value + 1;
        addInterval(result, lastCode, lastCode);
      }
      else if (type == 1) {
        lastCode -= value + 1;
        addInterval(result, lastCode, lastCode);
      }
      else if (type == 2) {
        int first = lastCode + 1;
        int last = first + value;
        addInterval(result, first, last);
        lastCode += value + 1;
      }
    }
    return result;
  }
  
  public static void addInterval(List<Interval> list, int first, int last) {
    Interval i = new Interval(first, last);
    list.add(i);
  }
  
  // Str Range List -- Range Str

  public static List<Interval> getStrRangeListFromRangeStr(String str) {
    ArrayList<Interval> result = new ArrayList<Interval>();
    final UCharacterIterator it = UCharacterIterator.getInstance(str);
    
    int first;
    while((first = it.nextCodePoint())!= UCharacterIterator.DONE) {
      int last = it.nextCodePoint();
      addInterval(result, first, last);
    }
    return result;
  }
  

  //
  // To String methods
  //
  public static String strRangeList2string(List<Interval> ilist) {
    
    StringBuilder sbuild = new StringBuilder();
    for(int i = 0; i< ilist.size(); i++) {
      int first = ilist.get(i).first;
      int last = ilist.get(i).last;
      
      for (int j = first; j <= last; j++) {
        sbuild.appendCodePoint(j);
      }
    }
    return sbuild.toString();
  }
  
  public static String rangeString2string(String rstr) {
    
    StringBuilder sbuild = new StringBuilder();
    final UCharacterIterator it = UCharacterIterator.getInstance(rstr);
    
    int first;
    while((first = it.nextCodePoint())!= UCharacterIterator.DONE) {
      int last = it.nextCodePoint();
      
      for (int j = first; j <= last; j++) {
        sbuild.appendCodePoint(j);
      }
    }
    return sbuild.toString();
  }
  

  //
  // String comparison methods
  //

  public static boolean isStringsEqual(String s1, String s2) {
    
    final UCharacterIterator it1 = UCharacterIterator.getInstance(s1);
    final UCharacterIterator it2 = UCharacterIterator.getInstance(s2);
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
  
  
  // Main
  
  public static void main(String[] args) {
  
    StringBuilder strBuild=new StringBuilder();
    try {
      Scanner sc = new Scanner(new File("/home/cibu/CharData.java"), "UTF-8");
      while (sc.hasNext()) {
        if (sc.findInLine("\\/\\*.*,\"(.*)\"},\\s*") != null) {
          MatchResult match = sc.match();
          String str = match.group(1);
          str = str.replaceAll("\\\\(.)", "$1");
          System.out.println(str);
          strBuild.append(str);
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
    //str = "\uDBFF\uDC00\uDBFF\uDFFD\u0001\u0001";
    List<Interval> ilist = getStrRangeListFromRangeStr(str);

    String encodedStr = base88EncodeList(ilist);
    List<Interval> decodedStrRangeList = base88DecodeList(encodedStr);
    
    String str1 = rangeString2string(str);
    String str2 = strRangeList2string(decodedStrRangeList);
    isStringsEqual(str1, str2);
    
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/compressed.txt"));
      out.write(encodedStr);
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
