/**
 * 
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StackTracker;

/**
 * @author srl
 *
 */
public class SurveyLog {
	final static boolean DEBUG = CldrUtility.getProperty("DEBUG", false);
	
	// Logging
	public static Logger logger;

	static {
		logger = Logger.getLogger("org.unicode.cldr.SurveyMain");
//		if(DEBUG) {
//			logger.setLevel(Level.ALL);
////			for(Handler h : logger.getHandlers()) {
////				h.setLevel(logger.getLevel());
////			}
//			ConsoleHandler ch = new ConsoleHandler();
//			ch.setLevel(logger.getLevel());
//			logger.addHandler(ch);
//		}
	}

	public static java.util.logging.Handler loggingHandler = null;

	static final void errln(String s) {
		logger.severe(s);
	}

	public static void logException(Throwable t) {
		logException(t,"");
	}

	public static void logException(Throwable t, String what) {
		logException(t,"(exception " + what + ")",StackTracker.stackToString(t.getStackTrace(), 0), null);
	}

	public static void logException(Throwable t, String what, String stack) {
		logException(t,what,stack,null);
	}

	public static synchronized void logException(Throwable t, String what, String stack, WebContext ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("** ST Exception:"+new Date()+", uptime " + SurveyMain.uptime+"\n");
		sb.append("EXCEPTION: " + t+"\n");
		sb.append("STACK:\n" + stack);
		sb.append("LOG SITE:\n" + StackTracker.currentStack()+"\n");
		sb.append("CTX:" + ctx +"\n");
		Throwable c = t.getCause();
		while(c!=null) {
	        sb.append(" CAUSE: " + c+"\n");
	        sb.append(" STACK:\n" + StackTracker.stackToString(c.getStackTrace(), 0) );
	        c = c.getCause();
		}
		// First, log to file
		if(SurveyMain.homeFile!=null) try {
			File logFile = new File(SurveyMain.homeFile, "exception.log");
			OutputStream file = new FileOutputStream(logFile, true); // Append
			PrintWriter pw = new PrintWriter(file);
			pw.append(sb);
			pw.close();
			file.close();
		} catch(IOException ioe) {
			logger.severe("Err: " + ioe + " trying to log exceptions!");
			ioe.printStackTrace();
		}

		// then, to screen
		logger.severe(sb.toString());
	}

	static void logException(Throwable t, WebContext ctx) {
		logException(t,"(exception)",StackTracker.stackToString(t.getStackTrace(), 0), ctx);
	}

	public static final void debug(Object string) {
		if(DEBUG) {
			_doDebug(string!=null?string.toString():null,Level.INFO);
		}
	}

	public static final void debug(String string) {
		if(DEBUG) {
			_doDebug(string,Level.INFO);
		}
	}

	private static void _doDebug(String string, Level l) {
		LogRecord lr = new LogRecord(l,string);
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		lr.setSourceClassName(st[4].getClassName());
		lr.setSourceMethodName(st[4].getMethodName());
		logger.log(lr);
	}

}
