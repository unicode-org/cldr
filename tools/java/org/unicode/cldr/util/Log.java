/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 * 
 * in shell:  (such as .cldrrc)
 *   export CWDEBUG="-DCLDR_DTD_CACHE=/tmp/cldrdtd/"
 *   export CWDEFS="-DCLDR_DTD_CACHE_DEBUG=y ${CWDEBUG}"
 *
 * 
 * in code:
 *   docBuilder.setEntityResolver(new CachingEntityResolver());
 * 
 */

package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.icu.dev.test.util.BagFormatter;

public class Log {
	static private PrintWriter log;
	
	public static void logln(int test, String message) {
		if (log != null && test != 0) log.println(message);
	}
	
	public static void logln(boolean test, String message) {
		if (log != null && test) log.println(message);
	}
	
	public static void logln(String message) {
		if (log != null) log.println(message);
	}
	
	/**
	 * @return Returns the log.
	 */
	public static PrintWriter getLog() {
		return log;
	}
	/**
	 * @param newlog The log to set.
	 */
	public static void setLog(PrintWriter newlog) {
		log = newlog;
	}
	
	/**
	 */
	public static void close() {
		if (log != null) log.close();
	}
	
	public static void setLog(String file) throws IOException {
		log = BagFormatter.openUTF8Writer("", file);
		log.print('\uFEFF');
	}
}