/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and        *
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

/**
 * @author Steven R Loomis
 * 
 * Caching entity resolver
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.MalformedURLException;


// SAX2 imports
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;


/**
 * Use this class to cache DTDs, speeding up tools.
 */
public class CachingEntityResolver implements EntityResolver {
    static final String CLDR_DTD_CACHE = "CLDR_DTD_CACHE";
    static final String CLDR_DTD_OVERRIDE = "CLDR_DTD_OVERRIDE";
    private static String gCacheDir = null;
    private static String gOverrideDir = System.getProperty(CLDR_DTD_OVERRIDE);
    private static boolean gCheckedEnv = false;
    private static boolean gDebug = false;
    
    // TODO: synch?
    
    /**
     * Create the cache dir if it doesn't exist.
     * delete all regular files within the cache dir.
     */
    public static void createAndEmptyCacheDir() {
        if(getCacheDir() == null) {
            return;
        }
        File cacheDir = new File(getCacheDir());
        cacheDir.mkdir();
        File cachedFiles[] = cacheDir.listFiles();
        if(cachedFiles != null) {
            for(int i=0;i<cachedFiles.length;i++) {
                if(cachedFiles[i].isFile()) {
                    cachedFiles[i].delete();
                }
            }
        }
    }
    
    public static void setCacheDir(String s) {
        gCacheDir = s;
        if((gCacheDir==null)||(gCacheDir.length()<=0)) {
            gCacheDir = null;
        }
    }
    public static String getCacheDir() {
//        boolean aDebug = false;
//        if((System.getProperty("CLDR_DTD_CACHE_DEBUG")!=null) || "y".equals(System.getProperty("CLDR_DTD_CACHE_ADEBUG"))) {
//            aDebug = true;
//        }

        if((gCacheDir == null) && (!gCheckedEnv)) {
            gCacheDir = System.getProperty(CLDR_DTD_CACHE);
            
            if(gCacheDir == null) {
                if(gDebug) {
                 System.err.println("CLDR_DTD_CACHE = " + System.getProperty(CLDR_DTD_CACHE));
                 System.err.println("CLDR_DTD_CACHE_DEBUG = " + System.getProperty("CLDR_DTD_CACHE_DEBUG"));
                }
//                String tmpdir = System.getProperty("java.io.tmpdir"));
//                if(tmpdir != null && tmpdir.length()>0) {
//                    File f = new File(tmpdir);
//                    if(f.canWrite()) {
//                    }
//                }
//                for (Object s : System.getProperties().keySet() ) { 
//                    //System.err.println("tmp was " + System.getProperty("java.tmp"));
//                    System.err.println(s);
//                }
            }
        
            if(gDebug) {
                System.out.println("CRE:  " + CLDR_DTD_CACHE + " = " + gCacheDir);
            }
            
            if((gCacheDir==null)||(gCacheDir.length()<=0)) {
                gCacheDir = null;
            }
            if((gOverrideDir==null)||(gOverrideDir.length()<=0)) {
                gOverrideDir = null;
            }
            gCheckedEnv=true;
        }
        return gCacheDir;
    }
    public InputSource resolveEntity (String publicId, String systemId) {
        boolean aDebug = gDebug;
        if((System.getProperty("CLDR_DTD_CACHE_DEBUG")!=null) || "y".equals(System.getProperty("CLDR_DTD_CACHE_ADEBUG"))) {
            aDebug = true;
        }
        
        String theCache = getCacheDir();

        if(aDebug) {
            System.out.println("CRE:  " + publicId + " | " + systemId + ", cache=" + theCache+", override="+gOverrideDir);
        }
        
        if(theCache!=null) {
            int i;

            if(systemId.startsWith("/")) {
                File xFile = new File(systemId);
                if(xFile.canRead()) {
                	String newUrl;
					try {
						newUrl = xFile.toURI().toURL().toString();
	                	if(gDebug) System.err.println("CRE: redir to " + newUrl);
	                	
	                	InputSource is =  new InputSource(newUrl);
	                	is.setEncoding("UTF8");
	                	return is;
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            }
            if(systemId.startsWith("file:") || !systemId.startsWith("http://")) {
//                File xFile = new File(systemId);
//                if(xFile.canRead()) {
                	return new InputSource(systemId);
//                }
//                return null;
            }
            StringBuffer systemNew = new StringBuffer(systemId);
//          char c = systemNew.charAt(0);
//          if((c=='.')||(c=='/')) {
//              return null;
//          }

            for(i=0;i<systemNew.length();i++) {
                char c = systemNew.charAt(i);
                if(!Character.isLetterOrDigit(c) && (c!='.')) {
                    systemNew.setCharAt(i, '_');
                }
            }

            if(aDebug) {
                System.out.println(systemNew.toString());
            }

            File aDir = new File(theCache);
            if(!aDir.exists() || !aDir.isDirectory()) {
                // doesn't exist or isn't a directory:
                System.err.println("CachingEntityResolver: Warning:  Cache not used, Directory doesn't exist, Check the value of  property " + CLDR_DTD_CACHE + " :  " + theCache);
                return null;
            }
            
            String newName = new String(systemNew);
            
            File t = new File(theCache,newName);
            if(t.exists()) {
                if(aDebug) {
                    System.out.println("Using existing: " + t.getPath());
                }
            } else {
                
                if(gOverrideDir != null) {
                    int lastSlash = systemId.lastIndexOf('/');
                    if(lastSlash != -1) {
                        String shortName = systemId.substring(lastSlash+1,systemId.length());
                        File aFile = new File(gOverrideDir,shortName);
                        if(aFile.exists()) {
                            if(aDebug) {
                                System.err.println("overridden "+aFile.toString());
                            }
                            return new InputSource(aFile.getPath());
                        }
                    }
                }
                
                if(aDebug) {
                    System.out.println(t.getPath() + " doesn't exist. fetching.");
                }
                
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new java.net.URL(systemId).openStream()));
                    BufferedWriter w = new BufferedWriter(new FileWriter(t.getPath()));
                    String s;
                    while((s=r.readLine())!=null) {
                        w.write(s);
                        w.newLine();
                    }
                    r.close();
                    w.close();
                } catch ( Throwable th ) {
                    System.err.println(th.toString() + " trying to fetch " + t.getPath());
                    
                    return null;
                }
                if(aDebug) {
                    System.out.println(t.getPath() + " fetched.");
                }
            }
            
            return new InputSource(t.getPath());
        }
        return null; // unhelpful
    }
}