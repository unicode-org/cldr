/*
**********************************************************************
* Copyright (c) 2002-2010, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/

package org.unicode.cldr.posix;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import com.ibm.icu.dev.test.util.SortedBag;

public class POSIX_LCCollate {

   public RuleBasedCollator col;
   UnicodeSet chars;
   SortedBag allItems;
   SortedBag contractions;
   CLDRFile collrules;
   int longest_char;

   public POSIX_LCCollate ( CLDRFile doc, UnicodeSet repertoire, CLDRFile collrules , UnicodeSet CollateSet , String codeset , POSIXVariant variant ) throws Exception
   {
     String rules = "";
     String settings = "";
     String collationType = "standard";
     boolean UsingDefaultCollateSet = false;

     if ( CollateSet.isEmpty() ) // Generate default collation set from exemplar characters;
     {
        CollateSet.addAll(repertoire); 
        UsingDefaultCollateSet = true;
     }

     chars = new UnicodeSet(repertoire).retainAll(CollateSet);

     this.collrules = collrules;
     if ( collrules != null )
     {
        if ( variant.collation_type == "default" ) {
            String path = "//ldml/collations/default";
            String fp = collrules.getFullXPath(path);
            XPathParts xp = new XPathParts();
            xp.set(fp);
            if (xp.containsAttribute("choice")) {
                collationType = xp.getAttributeValue(-1,"choice");
            }
        } else {
            collationType = variant.collation_type;
        }
        
        String path = "//ldml/collations/collation[@type=\""+collationType+"\"]/settings";
        Set<String> settingsPaths = new TreeSet<String>(CLDRFile.ldmlComparator);
        settingsPaths = collrules.getPaths(path, null, settingsPaths);
        for ( Iterator<String> it = settingsPaths.iterator(); it.hasNext();) {
            String settingPath = it.next();
            System.out.println(settingPath);
            settings += POSIXUtilities.CollationSettingString(collrules,settingPath);           
        }

        path = "//ldml/collations/collation[@type=\""+collationType+"\"]/rules";
        Set<String> rulesPaths = new TreeSet<String>(CLDRFile.ldmlComparator);
        rulesPaths = collrules.getPaths(path, null, rulesPaths);
        for ( Iterator<String> it = rulesPaths.iterator(); it.hasNext();) {
            String rulePath = it.next();
            rules += POSIXUtilities.CollationRuleString(collrules,rulePath);           
        }
 
     }

//     Useful for debugging collation settings
//     System.out.println("Setting string is :"+settings);
//     System.out.println("Rules   string is :"+rules);

     if ( settings.length() > 0 || rules.length() > 0 )
        col = new RuleBasedCollator(settings+rules);
     else
        col = (RuleBasedCollator) RuleBasedCollator.getInstance();

     // Add all the tailored characters if we are using the default collation set 
     if ( codeset.equals("UTF-8") && UsingDefaultCollateSet )
     {
        UnicodeSet tailored = col.getTailoredSet();
        UnicodeSetIterator it = new UnicodeSetIterator(tailored);
        while ( it.next() )
        {
           if ( it.codepoint != UnicodeSetIterator.IS_STRING && it.codepoint < 0xffff )
              chars.add(it.codepoint);
        }
     }

     allItems = new SortedBag(col);
     contractions = new SortedBag(col);


     // add all the chars
     longest_char = 0;
     for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();)
     {
        allItems.add(it.getString());
        int CharNameLength = POSIXUtilities.POSIXCharName(it.getString()).length();
        if ( CharNameLength > longest_char )
           longest_char = CharNameLength;
     }

     // get the tailored contractions
     // we need to filter only the ones in chars
     UnicodeSet tailored = col.getTailoredSet();
     getFilteredSet(chars, tailored);

     UnicodeSet uca_contractions = new UnicodeSet("[{\u0406\u0308}{\u0410\u0306}{\u0410\u0308}{\u0413\u0301}{\u0413\u0341}{\u0415\u0306}{\u0416\u0308}{\u0417\u0308}{\u0418\u0306}{\u0418\u0308}{\u041A\u0301}{\u041A\u0341}{\u041E\u0308}{\u0423\u0306}{\u0423\u0308}{\u0423\u030B}{\u0427\u0308}{\u042B\u0308}{\u042D\u0308}{\u0430\u0306}{\u0430\u0308}{\u0433\u0301}{\u0433\u0341}{\u0435\u0306}{\u0436\u0308}{\u0437\u0308}{\u0438\u0306}{\u0438\u0308}{\u043A\u0301}{\u043A\u0341}{\u043E\u0308}{\u0443\u0306}{\u0443\u0308}{\u0443\u030B}{\u0447\u0308}{\u044B\u0308}{\u044D\u0308}{\u0456\u0308}{\u0474\u030F}{\u0475\u030F}{\u04D8\u0308}{\u04D9\u0308}{\u04E8\u0308}{\u04E9\u0308}{\u0627\u0653}{\u0627\u0654}{\u0627\u0655}{\u0648\u0654}{\u064A\u0654}{\u09C7\u09BE}{\u09C7\u09D7}{\u0B47\u0B3E}{\u0B47\u0B56}{\u0B47\u0B57}{\u0B92\u0BD7}{\u0BC6\u0BBE}{\u0BC6\u0BD7}{\u0BC7\u0BBE}{\u0C46\u0C56}{\u0CBF\u0CD5}{\u0CC6\u0CC2}{\u0CC6\u0CC2\u0CD5}{\u0CC6\u0CD5}{\u0CC6\u0CD6}{\u0CCA\u0CD5}{\u0D46\u0D3E}{\u0D46\u0D57}{\u0D47\u0D3E}{\u0DD9\u0DCA}{\u0DD9\u0DCF}{\u0DD9\u0DCF\u0DCA}{\u0DD9\u0DDF}{\u0DDC\u0DCA}{\u0E4D\u0E32}{\u0ECD\u0EB2}{\u0F71\u0F72}{\u0F71\u0F74}{\u0F71\u0F80}{\u0FB2\u0F71}{\u0FB2\u0F71\u0F80}{\u0FB2\u0F80}{\u0FB2\u0F81}{\u0FB3\u0F71}{\u0FB3\u0F71\u0F80}{\u0FB3\u0F80}{\u0FB3\u0F81}{\u1025\u102E}]");

     getFilteredSet(uca_contractions, tailored);
   }

   private void getFilteredSet(UnicodeSet chars, UnicodeSet tailored) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(tailored); it.next();) {
         if (it.codepoint != UnicodeSetIterator.IS_STRING) continue;
         String s = it.getString();
         s = Normalizer.compose(s,false);    // normalize to make sure
         if (!UTF16.hasMoreCodePointsThan(s, 1)) continue;
         if (!chars.containsAll(s)) continue;
         contractions.add(s);
         allItems.add(s);
      }
   }

   public void write ( PrintWriter out ) {
 
      out.println("*************");
      out.println("LC_COLLATE");
      out.println("*************");
      out.println();

      writeDefinitions(out);
      out.println();
      writeList(out);
      out.println();
      out.println("* assignment of characters to weights");
      out.println();
      for (Iterator<String> it = allItems.iterator(); it.hasNext();) {
         String currentChar = it.next();
         out.println(showLine(col, currentChar));
      }
      out.print("UNDEFINED");
      for ( int i = longest_char - 9 ; i > 0 ; i-- )
           out.print(" ");
      out.print(" ");
      out.println("IGNORE;IGNORE;IGNORE;...");
      out.println();
      out.println("order_end");
      out.println();
      out.println("END LC_COLLATE");
   }

	private void writeDefinitions(PrintWriter out) {
        //collating-element <A-A> from "<U0041><U0041>"
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> it = contractions.iterator(); it.hasNext();) {
            buffer.setLength(0);
            String s = it.next();
            buffer.append("collating-element ")
                  .append(POSIXUtilities.POSIXContraction(s))
                  .append(" from \"")
                  .append(POSIXUtilities.POSIXCharName(s))
                  .append("\"");
            out.println(buffer.toString());
        }        
    }
    
    private class IntList {
    	private BitSet stuff = new BitSet();
        private int leastItem = Integer.MAX_VALUE;
        void add(int item) {
            stuff.set(item);
            if (item < leastItem) leastItem = item;
        }
        void remove(int item) {
            stuff.clear(item);
            if (item == leastItem) {
            	// search for new least
                for (int i = item+1; i < stuff.size(); ++i) {
                	if (stuff.get(i)) {
                		leastItem = i;
                        return;
                    }
                }
                leastItem = Integer.MAX_VALUE; // failed, now empty
            }
        }
        int getLeast() {
        	return leastItem;
        }
    }

    Set<Weights> nonUniqueWeights = new HashSet<Weights>();
    Set<Weights> allWeights = new HashSet<Weights>();
    Map<String, Weights> stringToWeights = new HashMap<String, Weights>();

    private void writeList(PrintWriter out ) {
       // BitSet alreadySeen = new BitSet();
        BitSet needToWrite = new BitSet();
        needToWrite.set(1); // special weight for uniqueness
        //int maxSeen = 0;
        for (Iterator<String> it1 = allItems.iterator(); it1.hasNext();) {
            String string = it1.next();
            Weights w = new Weights(col.getCollationElementIterator(string));
            w.primaries.setBits(needToWrite);
            w.secondaries.setBits(needToWrite);
            w.tertiaries.setBits(needToWrite);
            if (allWeights.contains(w)) nonUniqueWeights.add(w);
            allWeights.add(w);
            stringToWeights.put(string, w);
        }
        out.println("");
        out.println("* Define collation weights as symbols");
        out.println("");

        for (int i = 0; i < needToWrite.size(); ++i) {
        	if (needToWrite.get(i))
                {
                   out.print("collating-symbol ");
                   out.println(getID('X', i));
                }
        }

        out.println("");
        out.println("order_start forward;" + 
                (col.isFrenchCollation() ? "backward" : "forward")
                + ";forward;forward");
        out.println("");

        out.println("");
        out.println("* collation weights in order");
        out.println("");
        for (int i = 0; i < needToWrite.size(); ++i) {
        	if (needToWrite.get(i))
                   out.println(getID('X', i));
        }
    }

	/**
	 * @param col
	 * @param string
	 */
	private String showLine(RuleBasedCollator col, String string) {
        String prefix = "";
		StringBuffer result = new StringBuffer();
        String ThisChar = POSIXUtilities.POSIXContraction(string);
        result.append(ThisChar);
        // pad for nice formatting
        for ( int i = longest_char - ThisChar.length() ; i > 0 ; i-- )
           result.append(" ");
        result.append(" ");
        // gather data
        Weights w = stringToWeights.get(string);
        result.append(w.primaries)
        .append(";")
        .append(w.secondaries)
        .append(";")
        .append(w.tertiaries)
        .append(";");
        
        if ( nonUniqueWeights.contains(w))
        {
            String decomposed = Normalizer.decompose(string,false);
            if ( decomposed.length() > 1 )
               result.append("\"");
            result.append(POSIXUtilities.POSIXCharName(decomposed));
            if ( decomposed.length() > 1 )
               result.append("\"");
        }
        else
           result.append("IGNORE");

        if (prefix.length() != 0) result.insert(0,prefix);
		return result.toString();
	}
    
	/**
	 * @param leadChar
	 * @param i
	 * @return
	 */
	private static String getID(char leadChar, int i) {
	    int length;
	    if ( i < 256 ) {
	        length = 2;
	    } else {
	        length = 4;
	    }
		return "<" + leadChar + Utility.hex(i,length)+ ">";
	}


	private class Weights {
		WeightList primaries = new WeightList();
		WeightList secondaries = new WeightList();
		WeightList tertiaries = new WeightList();
        public Weights(CollationElementIterator it) {
            while (true) {
                int ce = it.next();
                if (ce == CollationElementIterator.NULLORDER) break;
                int p = CollationElementIterator.primaryOrder(ce);
                primaries.append(p);
                secondaries.append(CollationElementIterator.secondaryOrder(ce));
                tertiaries.append(CollationElementIterator.tertiaryOrder(ce));
            }
        }
        public boolean equals(Object other) {
            Weights that = (Weights)other;
            return primaries.equals(that.primaries)
                && secondaries.equals(that.secondaries)
                && tertiaries.equals(that.tertiaries);
        }
        public int hashCode() {
        	return (primaries.hashCode()*37
                + secondaries.hashCode())*37
                + tertiaries.hashCode();
        }
    }

	private class WeightList {
        char[] weights = new char[100];
        // TODO lengthen on demand
        int count = 0;
        public void append(int i) {
            // add each 16-bit quantity
            for (int j = 16; j >= 0; j -= 16) {
                char b = (char)(i >>> j);
                if (b == 0) continue;
                weights[count++] = b;
            }
        }
        public void setBits(BitSet s) {
            for (int j = 0; j < count; ++j) s.set(weights[j]);   
        }

        public String toString() {
            if (count == 0) return "IGNORE";
            if (count == 1) return getID('X', weights[0]);
            String result = "\"";
            for (int i = 0; i < count; ++i) {
                result += getID('X', weights[i]);
            }
            return result + "\"";
        }
        public boolean equals(Object other) {
            WeightList that = (WeightList)other;
            for (int j = 0; j < count; ++j) {
            	if (weights[j] != that.weights[j]) return false;
            }
            return true;
        }
        public int hashCode() {
            int result = count;
            for (int j = 0; j < count; ++j) result = result*37 + weights[j];  
            return result;
        }
    }
    
    private String getID(String s, boolean isSingleID) {
        //Object defined = definedID.get(s);
        //if (defined != null) return (String) defined;
        //if (defined != null) return (String) defined;
        
        StringBuffer result = new StringBuffer();
        if (!UTF16.hasMoreCodePointsThan(s, 1)) {
        	// single code point
            appendID(UTF16.charAt(s,0), result, false);
        } else if (isSingleID) {
            result.append('<');
            int cp;
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                if (i != 0) result.append('-');
                appendID(cp, result, true);
            }
            result.append('>');            
        } else {
            result.append('"');
            int cp;
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                appendID(cp, result, false);
            }
            result.append('"');
        }
        return result.toString();
    }

	private StringBuffer appendID(int cp, StringBuffer result, boolean nakedID) {
        if (!nakedID) result.append('<');
		result.append('U').append(Utility.hex(cp,4));
        if (!nakedID) result.append('>');
        return result;
	}
}
