/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.icu.dev.test.util.BagFormatter;

public class Utility {
	static final boolean DEBUG_SHOW_BAT = false;
	
	public interface LineComparer {
		static final int LINES_DIFFERENT = -1, LINES_SAME = 0, SKIP_FIRST = 1, SKIP_SECOND = 2;
		/**
		 * Returns LINES_DIFFERENT, LINES_SAME, or if one of the lines is ignorable, SKIP_FIRST or SKIP_SECOND
		 * @param line1
		 * @param line2
		 * @return
		 */
		int compare(String line1, String line2);
	}
	
	public static class SimpleLineComparator implements LineComparer {
	    public static final int TRIM = 1, SKIP_SPACES = 2, SKIP_EMPTY = 4;
	    StringIterator si1 = new StringIterator();
        StringIterator si2 = new StringIterator();
        int flags;
        public SimpleLineComparator(int flags) {
			this.flags = flags;
		}
		public int compare(String line1, String line2) {
			// first, see if we want to skip one or the other lines
			int skipper = 0;
			if (line1 == null) {
				skipper = SKIP_FIRST;
			} else {
				if ((flags & TRIM)!= 0) line1 = line1.trim();
				if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) skipper = SKIP_FIRST;
			}
			if (line2 == null) {
				skipper = SKIP_SECOND;
			} else {
	            if ((flags & TRIM)!= 0) line2 = line2.trim();
	            if ((flags & SKIP_EMPTY)!= 0 && line2.length() == 0) skipper += SKIP_SECOND;
			}
            if (skipper != 0) {
            	if (skipper == SKIP_FIRST + SKIP_SECOND) return LINES_SAME; // ok, don't skip both
            	return skipper;
            }
            
            // check for null
            if (line1 == null) {
            	if (line2 == null) return LINES_SAME;
            	return LINES_DIFFERENT;          	
            }
            if (line2 == null) return LINES_DIFFERENT;
            
            // now check equality
			if (line1.equals(line2)) return LINES_SAME;
			
			// if not equal, see if we are skipping spaces
			if ((flags & SKIP_SPACES) != 0 && si1.set(line1).matches(si2.set(line2))) return LINES_SAME;
			return LINES_DIFFERENT;
		}
		
	}
	
	/**
	 * 
	 * @param file1
	 * @param file2
	 * @param failureLines on input, String[2], on output, failing lines
	 * @param flags TRIM, SKIP_EMPTY
	 * @return
	 * @throws IOException
	 */
    public static boolean areFileIdentical(String file1, String file2, String[] failureLines, 
    		LineComparer lineComparer) throws IOException {
        BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
        try {
			BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
			try {
				String line1 = "";
				String line2 = "";
				int skip = 0;
      
			    for (int lineCount = 0; ; ++lineCount) {
			        if ((skip & LineComparer.SKIP_FIRST) == 0) line1 = br1.readLine();
			        if ((skip & LineComparer.SKIP_SECOND) == 0) line2 = br2.readLine();
			        if (line1 == null && line2 == null) return true;
			        skip = lineComparer.compare(line1, line2);
			        if (skip == LineComparer.LINES_DIFFERENT) break;
			    }
			    failureLines[0] = line1 != null ? line1 : "<end of file>";
			    failureLines[1] = line2 != null ? line2 : "<end of file>";
			    return false;
			} finally {
			    br2.close();
			}
		} finally {
		    br1.close();
		}
    }
    
    /*
    static String getLineWithoutFluff(BufferedReader br1, boolean first, int flags) throws IOException {
        while (true) {
            String line1 = br1.readLine();
            if (line1 == null) return line1;
            if ((flags & TRIM)!= 0) line1 = line1.trim();
            if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) continue;
            return line1;
        }
    }
    */
    
    public final static class StringIterator {
    	String string;
    	int position = 0;
    	char next() {
    		while (true) {
    			if (position >= string.length()) return '\uFFFF';
    			char ch = string.charAt(position++);
    			if (ch != ' ' && ch != '\t') return ch;
    		}
    	}
    	StringIterator reset() {
    		position = 0;
    		return this;
    	}
    	StringIterator set(String string) {
    		this.string = string;
    		position = 0;
    		return this;
    	}
    	boolean matches(StringIterator other) {
    		while (true) {
    			char c1 = next();
    			char c2 = other.next();
    			if (c1 != c2) return false;
    			if (c1 == '\uFFFF') return true;
    		}
    	}
		/**
		 * @return Returns the position.
		 */
		public int getPosition() {
			return position;
		}
    }
    
    static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile, LineComparer lineComparer) {
        try {
            String batDir = targetDir + File.separator + "diff" + File.separator;
            String batName = targetFile + ".bat";
            String[] failureLines = new String[2];

            if (!areFileIdentical(sourceDir + sourceFile, targetDir + targetFile, failureLines, lineComparer)) {
                PrintWriter bat = BagFormatter.openUTF8Writer(batDir, batName);
                try {
                	bat.println("\"C:\\Program Files\\Compare It!\\wincmp3.exe\" " +
                        new File(sourceDir + sourceFile).getCanonicalPath() + " " +
                        new File(targetDir + targetFile).getCanonicalPath());               
                } finally {
                	bat.close();
                }
            } else {
                File f = new File(batDir + batName);
                if (f.exists()) {
                    if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
                    f.delete();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


	public static List split(String source, char separator, boolean trim) {
		return split(source, separator, trim, null);
	}
	
	public static List split(String source, char separator, boolean trim, List output) {
		if (output == null) output = new ArrayList();
		if (source.length() == 0) return output;
		int pos = 0;
		do {
			int npos = source.indexOf(separator, pos);
			if (npos < 0) npos = source.length();
			String piece = source.substring(pos, npos);
			if (trim) piece = piece.trim();
			output.add(piece);
			pos = npos+1;
		} while (pos < source.length());
		return output;
	}

	/**
	 * Utility to indent by a certain number of tabs.
	 * @param out
	 * @param count
	 */
	static void indent(PrintWriter out, int count) {
		out.print(repeat("\t", count));
	}
	
	/**
	 * Utility to indent by a certain number of tabs.
	 * @param out
	 * @param count
	 */
	public static String repeat(String s, int count) {
		if (count == 0) return "";
		if (count == 1) return s;
		StringBuffer result = new StringBuffer();
	    for (int i = 0; i < count; ++i) {
	        result.append(s);
	    }
	    return result.toString();
	}
}