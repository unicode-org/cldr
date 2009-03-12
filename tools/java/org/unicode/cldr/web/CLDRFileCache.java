/**
 * Copyright (C) 2008 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.SimpleXMLSource;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.CLDRDBSourceFactory.CLDRDBSource;

/**
 * @author srl
 * 
 */
public class CLDRFileCache {
	static final private boolean DEBUG_INSANE = true;
	/**
	 * @author srl
	 * 
	 */

	/** Some ints to twiddle. Used to separate cached. **/
	private static int nn = 0;
	/** Some ints to twiddle. Used to separate cached. **/
	private static int sernos = 0;
	/** Some ints to twiddle. Used to separate cached. **/
	private int serno = 0;

	/** Helper: factory implementation */
	private class CachedFilesFactory extends CLDRFile.Factory {

		@Override
		protected DraftStatus getMinimalDraftStatus() {
			return CLDRFile.DraftStatus.unconfirmed;
		}

		@Override
		public String getSourceDirectory() {
			// assume: thread safe
			return realSource.getSupplementalDirectory().getAbsolutePath();
		}

		@Override
		public CLDRFile handleMake(String localeID, boolean bool,
				DraftStatus madeWithMinimalDraftStatus) {
			return getCLDRFile(CLDRLocale.getInstance(localeID));
		}

		@Override
		protected Set<String> handleGetAvailable() {
			// assume: thread safe
			return realSource.getAvailableLocales();
		}

	}

	private CachedFilesFactory factory = new CachedFilesFactory();

	/**
	 * Implementation of XMLSource representing an entry into the cache.
	 * 
	 * @deprecated
	 * @author srl
	 * 
	 */
	private class CachedSource extends XMLSource {
		XMLSource cachedFileSource = null;
		XMLSource subSource = null;

		/**
         * 
         */
		public CachedSource() {
			if (DEBUG_INSANE)
				System.err.println("## " + serno + " subspawn @ "
						+ cacheDir.getAbsolutePath());
			setLocaleID(realSource.getLocaleID());
			subSource = realSource.make(getLocaleID());
		}

		public CachedSource(String locale) {
			if (DEBUG_INSANE)
				System.err.println("## " + serno + " subspawn " + locale
						+ " @ " + cacheDir.getAbsolutePath());
			setLocaleID(locale);
			subSource = realSource.make(locale);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.unicode.cldr.util.XMLSource#make(java.lang.String)
		 */
		@Override
		public XMLSource make(String localeID) {
			if (DEBUG_INSANE)
				System.err.println("## " + serno + " make " + localeID + " @ "
						+ cacheDir.getAbsolutePath());
			// TODO Auto-generated method stub
			return new CachedSource(localeID);
		}

		private String getLocaleFileName() {
			return CLDRFileCache.getLocaleFileName(getLocaleID(), false);
		}

		public File getLocaleFile() {
			return new File(cacheDir, getLocaleFileName());
		}

		public final XMLSource getCachedSource() {
			if (cachedFileSource == null) {
				cachedFileSource = CLDRFileCache.this
						.getCachedSource(getLocaleID());
			}
			return cachedFileSource;
		}

		public File getSupplementalDirectory() {
			return realSource.getSupplementalDirectory();
		}

		/* ------ overrides below here ------ */

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.unicode.cldr.util.XMLSource#getAvailableLocales()
		 */
		@Override
		public Set getAvailableLocales() {
			// uses *subSource* which is
			return realSource.getAvailableLocales();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
		 */
		@Override
		public String getFullPathAtDPath(String path) {
			// TODO Auto-generated method stub
			return getCachedSource().getFullPathAtDPath(path);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
		 */
		@Override
		public String getValueAtDPath(String path) {
			// TODO Auto-generated method stub
			// if(DEBUG_INSANE) System.err.println(path+" = " +
			// getCachedSource().getValueAtDPath(path));
			return getCachedSource().getValueAtDPath(path);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.unicode.cldr.util.XMLSource#getXpathComments()
		 */
		@Override
		public Comments getXpathComments() {
			// TODO Auto-generated method stub
			return getCachedSource().getXpathComments();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.unicode.cldr.util.XMLSource#iterator()
		 */
		@Override
		public Iterator<String> iterator() {
			// TODO Auto-generated method stub
			return getCachedSource().iterator();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String,
		 * java.lang.String)
		 */
		@Override
		public void putFullPathAtDPath(String distinguishingXPath,
				String fullxpath) {
			zapCache();
			getCachedSource()
					.putFullPathAtDPath(distinguishingXPath, fullxpath);
		}

		private void zapCache() {
			getLocaleFile().delete();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String,
		 * java.lang.String)
		 */
		@Override
		public void putValueAtDPath(String distinguishingXPath, String value) {
			zapCache();
			getCachedSource().putValueAtDPath(distinguishingXPath, value);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
		 */
		@Override
		public void removeValueAtDPath(String distinguishingXPath) {
			zapCache();
			getCachedSource().removeValueAtDPath(distinguishingXPath);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr
		 * .util.XPathParts.Comments)
		 */
		@Override
		public void setXpathComments(Comments comments) {
			throw new UnsupportedOperationException(
					"Attempt to modify read-only cache object");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.ibm.icu.util.Freezable#freeze()
		 */
		public Object freeze() {
			throw new UnsupportedOperationException(
					"Attempt to modify read-only cache object");
		}

		public String getWinningPath(String path) {
			String ret = subSource.getWinningPath(path);
			return ret;
		}

	}

	/**
	 * A simple xml source that tracks Winning amounts and can be cached.
	 * 
	 * @author srl
	 * 
	 */
	public class CacheableXMLSource extends SimpleXMLSource {
		private static final int COOKIE = 9295467;
		private Registerable token = new Registerable(sm.lcr, CLDRLocale
				.getInstance(this.getLocaleID())) {
		};
		protected HashMap<String, String> winningXpaths = new HashMap<String, String>();

		// public CacheableXMLSource createFinalData() {
		// return new WinningXMLSource(this);
		// }

		public CacheableXMLSource(Factory factory, CLDRLocale locale) {
			super(factory, locale.toString());
		}

		/**
		 * Create a shallow clone.
		 * 
		 * @param copyAsLockedFrom
		 * @param finalData
		 */
		protected CacheableXMLSource(CacheableXMLSource copyAsLockedFrom) {
			super(factory, copyAsLockedFrom.getLocaleID());
			this.winningXpaths = copyAsLockedFrom.winningXpaths;
			this.token = copyAsLockedFrom.token;
			this.putAll(copyAsLockedFrom, 0);
		}

		public String getWinningPath(String path) {
			String ret = winningXpaths.get(path);
			return ret;
		}

		private void setWinningPath(String path, String winningPath) {
			winningXpaths.put(path, winningPath);
		}

		// Stream Format. (all BE)
		// INT32:cookie
		// INT32:xpath INT32:fullxpath INT32:winxpath SHORT16:len [utf-16 code
		// units] BYTE<len>:data [utf-16 code units]
		// INT32:-1 INT32:count INT32:-1 SHORT16: -1
		public void save(String s) throws IOException {
			save(new File(cacheDir, s + ".xpt"));
		}

		public void save(File f) throws IOException {
			File tmp = null;
			tmp = File.createTempFile("tmp", CACHE_SUFFIX, f.getParentFile());
			FileOutputStream fos = new FileOutputStream(tmp);
			DataOutputStream dos = new DataOutputStream(fos);

			Iterator<String> iter = this.iterator();

			Set<String> itemsToSave = new HashSet<String>();

			for (; iter.hasNext();) {
				String xpath = iter.next();
				itemsToSave.add(xpath);
				String bxpath = sm.xpt.xpathToBaseXpath(xpath);
				if (!bxpath.equals(xpath)) {
					itemsToSave.add(bxpath);
				}
			}

			dos.writeInt(COOKIE);
			int n = 0;
			for (String xpath : itemsToSave) {
				n++;
				String fxpath = this.getFullPathAtDPath(xpath);
				String wxpath = this.getWinningPath(xpath);
				String val = this.getValueAtDPath(xpath);

				dos.writeInt(sm.xpt.getByXpath(xpath));
				dos.writeInt(fxpath != null ? sm.xpt.getByXpath(fxpath) : -1);
				dos.writeInt(wxpath != null ? sm.xpt.getByXpath(wxpath) : -1);

				int len = (val != null) ? val.length() : -1;
				dos.writeShort(len);
				if (val != null) {
					dos.writeChars(val);
				}
			}
			dos.writeInt(-1); // xpath = -1
			dos.writeInt(n); // fullxpath = count
			dos.writeInt(-1); // winxpath = -1
			dos.writeShort(-1); // len = -1
			dos.close();
			fos.close();
			if (f.exists()) {
				throw new InternalError("Err: " + f.getAbsolutePath()
						+ " exists trying to rename from "
						+ tmp.getAbsolutePath());
			}

			System.err.println("##ren " + f.getAbsolutePath() + " <<  "
					+ tmp.getAbsolutePath() + " - " + n + " rows written");
			tmp.renameTo(f);
		}

		public void load(File f) throws IOException {
			FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis);

			int cookie = dis.readInt();
			if (cookie != COOKIE) {
				throw new InternalError("Error: cache " + f
						+ " had bad cookie " + cookie + " expected " + COOKIE);
			}
			int n = 0;
			int fxpath = 0;
			int xpath = 0;
			while (xpath != -1) {
				n++;
				xpath = dis.readInt();
				fxpath = dis.readInt();
				int wxpath = dis.readInt();
				int len = dis.readShort();
				StringBuffer buf = new StringBuffer();
				for (int i = 0; i < len; i++) {
					buf.append(dis.readChar());
				}

				if (xpath == -1)
					break;

				String xpath_str = sm.xpt.getById(xpath);
				String fxpath_str = sm.xpt.getById(fxpath);
				String wxpath_str = sm.xpt.getById(wxpath);

				if (xpath_str == null)
					throw new InternalError("Error: cache " + f
							+ " had unknown xpath " + xpath);

				if (len >= 0) {
					this.putValueAtDPath(xpath_str, buf.toString());
				} else {
					// System.err.println("Skipping null (-1) val for " +
					// xpath_str);
				}
				if (fxpath_str != null) {
					this.putFullPathAtDPath(xpath_str, fxpath_str);
				} else {
					// System.err.println("Skipping null (-1) fxpath for " +
					// xpath_str);
				}
				if (wxpath_str != null) {
					this.setWinningPath(xpath_str, wxpath_str);
				} else {
					// System.err.println("Skipping null (-1) wxpath for " +
					// xpath_str);
				}
			}
			if (fxpath != (n - 1)) {
				throw new InternalError("Error: cache " + f
						+ " had invalid length, read " + (n - 1) + " but got "
						+ fxpath);
			}
			dis.close();
			fis.close();
			System.err.println("##" + f + " - read " + (n - 1) + " records.");
		}

		/**
		 * Notify that we are done loading, and notify.
		 */
		public void poke() {
			token.register();
			// this.freeze();
		}

		public boolean invalid() {
			return !token.isValid();
		}

		/**
		 * Load (copy) from another source.
		 * 
		 * @param make
		 */
		public void load(XMLSource from) {
			Iterator<String> iter = from.iterator();

			// dos.writeInt(COOKIE);
			int n = 0;
			for (; iter.hasNext();) {
				n++;
				String xpath = iter.next();
				String fxpath = from.getFullPathAtDPath(xpath);
				String wxpath = from.getWinningPath(xpath);
				String val = from.getValueAtDPath(xpath);
				String bxpath = sm.xpt.xpathToBaseXpath(xpath);
				this.putValueAtDPath(xpath, val);
				this.putFullPathAtDPath(xpath, fxpath);
				this.setWinningPath(xpath, wxpath);

				if (!bxpath.equals(xpath)) {
					String wbxpath = from.getWinningPath(bxpath);
					this.setWinningPath(bxpath, wbxpath);
				}

				// if(xpath.contains("singleCountries")) {
				// System.err.println(getLocaleID()+"\n/// " + xpath + "\n->- "
				// + fxpath + "\n$>- "+wxpath+"\n<<< "+bxpath);
				// }
			}
			System.err.println("## WXP load " + this.getLocaleID() + " from "
					+ from.getClass().getName() + "  - loaded " + n);
		}

		public void reloadWinning(XMLSource from) {
			Iterator<String> iter = from.iterator();

			// dos.writeInt(COOKIE);
			int n = 0;
			for (; iter.hasNext();) {
				n++;
				String xpath = iter.next();
				// String fxpath = from.getFullPathAtDPath(xpath);
				String wxpath = from.getWinningPath(xpath);
				// String val = from.getValueAtDPath(xpath);
				String bxpath = sm.xpt.xpathToBaseXpath(xpath);
				// this.putValueAtDPath(xpath, val);
				// this.putFullPathAtDPath(xpath, fxpath);
				this.setWinningPath(xpath, wxpath);
				if (!bxpath.equals(xpath)) {
					String wbxpath = from.getWinningPath(bxpath);
					this.setWinningPath(bxpath, wbxpath);
				}

				// if(xpath.contains("singleCountries")) {
				// System.err.println(getLocaleID()+"\n/// " + xpath + "\n->- "
				// + fxpath + "\n$>- "+wxpath+"\n<<< "+bxpath);
				// }
			}
			System.err.println("## WXP reload win" + this.getLocaleID()
					+ " from " + from.getClass().getName() + "  - loaded " + n);
		}

		public void initialize() {
			// if(this.getLocaleID().startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
			// throw new InternalError("sholdn't load supp data.");
			// }
			try {
				File f = getCacheFile();
				if (f.exists()) {
					System.err.println("## load: " + f.getAbsolutePath());
					load(f);
				} else {
					System.err.println("## create: " + f.getAbsolutePath());
					load();
					save(f);
				}
				// freeze();
				poke(); // mark as valid.

			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new InternalError("Could not load XMLSource on "
						+ getLocaleID() + " - " + ioe.toString());
			}
		}

		/**
		 * Write out current state of the cache.
		 */
		public void save() {
			try {
				File f = getCacheFile();
				if (f.exists()) {
					deleteInvalid();
					// System.err.println("## load: " + f.getAbsolutePath());
					// load(f);
				}
				{
					System.err.println("## re-create: " + f.getAbsolutePath());
					// load();
					save(f);
				}
				// freeze();
				poke(); // mark as valid.

			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new InternalError("Could not save XMLSource on "
						+ getLocaleID() + " - " + ioe.toString());
			}
		}

		/**
		 * Load from underlying source.
		 */
		protected void load() {
			load(realSource.make(getLocaleID()));
		}

		private File getCacheFile() {
			return new File(cacheDir, getCacheFileName());
		}

		protected String getCacheFileName() {
			return getLocaleID() + CACHE_SUFFIX;
		}

		public void deleteInvalid() {
			File f = getCacheFile();
			System.err.println("## delete-invalid: " + f.getAbsolutePath());
			f.delete();
		}
	}

	public class WinningXMLSource extends CacheableXMLSource {

		// protected WinningXMLSource(CacheableXMLSource copyAsLockedFrom) {
		// super(copyAsLockedFrom);
		// if(copyAsLockedFrom instanceof WinningXMLSource) {
		// throw new
		// InternalError("WinningXMLSource: Cowardly refusing to create a winning source from a winning source.");
		// }
		// }

		public WinningXMLSource(Factory f, CLDRLocale localeID) {
			super(f, localeID);
		}

		// public String getValueAtDPath(String dpath) {
		// String win = super.getWinningPath(dpath);
		// return super.getValueAtDPath(win);
		// }
		//        
		// public String getFullPathAtDPath(String dpath) {
		// String win = super.getWinningPath(dpath);
		// if(win==null) {
		// // It may not be a distinguising path
		// win = super.getWinningPath(CLDRFile.getNondraftNonaltXPath(dpath));
		// }
		// String full = super.getFullPathAtDPath(win);
		// if(full == null) {
		// throw new InternalError("FP@D: "+dpath+" -> " + win + " -> " + full);
		// }
		// return full;
		// }
		//        
		// public Iterator<String> iterator() {
		// return super.winningXpaths.keySet().iterator();
		// }
		//        
		// /**
		// * See CLDRFile getWinningPath for documentation.
		// * Default implementation is that it removes draft and
		// [@alt="...proposed..." if possible
		// * @param path
		// * @return
		// */
		// public String getWinningPath(String path) {
		// String newPath = CLDRFile.getNondraftNonaltXPath(path);
		// if (!newPath.equals(path)) {
		// String value = getValueAtPath(newPath); // ensure that it still works
		// if (value != null) {
		// return newPath;
		// }
		// }
		// return path;
		// }
		protected String getCacheFileName() {
			return "w." + super.getCacheFileName();
		}

		protected void load() {
			load(realVettedSource.make(getLocaleID()));
		}
	}

	private File cacheDir;
	private XMLSource realSource, realVettedSource;
	private SurveyMain sm;

	static int nextSerialNumber() {
		return ++sernos;
	}

	public CLDRFileCache(XMLSource subSource, XMLSource subVettedSource,
			File cacheParent, SurveyMain sm) {
		serno = nextSerialNumber();
		this.sm = sm;
		this.cacheDir = cacheParent;
		this.realSource = subSource;
		this.realVettedSource = subVettedSource;
		validateCache();
		if (DEBUG_INSANE)
			System.err.println("## " + serno + " bootation @ "
					+ cacheDir.getAbsolutePath());
	}

	// public XMLSource getXMLSource() {
	// if(DEBUG_INSANE) System.err.println("## "+serno+" spawn @ " +
	// cacheDir.getAbsolutePath());
	// return getSource();
	// }
	//    
	// public XMLSource getXMLSource(String locale) {
	// //if(DEBUG_INSANE) System.err.println("## "+serno+" spawn " + locale +
	// " @ " + cacheDir.getAbsolutePath());
	// return getSource(locale);
	// }

	private void validateCache() {
		// if(cacheDir.exists()) {
		// File old = new File(cacheDir.getParentFile(),CACHE_DIR+"_old");
		// cacheDir.renameTo(old);
		// }
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
	}

	private static String getLocaleFileName(String locale, boolean isVetted) {
		return (isVetted ? "v--" : "") + locale + ".xml";
	}

	public static final String CACHE_SUFFIX = ".xpt";

	public CLDRFile getCLDRFile(CLDRLocale locale) {
		return getCLDRFile(locale, false);
	}

	public CLDRFile getCLDRFile(CLDRLocale locale, boolean resolving) {
		XMLSource x = getSource(locale, false); // !vetted
		CLDRFile f = new CLDRFile(x, resolving); // !fallback
		// f.freeze();
		return f;
	}

	// protected CLDRFile getCLDRFile(String locale, boolean isVetted) {
	// XMLSource x = getSource(locale, isVetted);
	// CLDRFile f = new CLDRFile(x, false);
	// f.freeze();
	// return f;
	// }

	public CLDRFile getVettedCLDRFile(CLDRLocale locale) {
		XMLSource x = getSource(locale, true); // vetted
		CLDRFile f = new CLDRFile(x, true); // fallback
		// f.freeze();
		return f;
	}

	// public CLDRFile getVettedCLDRFile(String locale) {
	// CacheableXMLSource s = getSource(locale, true);
	// CacheableXMLSource x = s.createFinalData();
	// CLDRFile f = new CLDRFile(x, false);
	// f.freeze();
	// Iterator<String> i;
	// System.err.println("----->>>:"+locale);
	// i = x.iterator();
	// for(;i.hasNext();) {
	// System.err.println("X: "+i.next());
	// }
	// System.err.println("--");
	// //f.write(new PrintWriter(System.err));
	// // System.err.println("--");
	// // i = f.iterator();
	// // for(;i.hasNext();) {
	// // System.err.println("F: "+i.next());
	// // }
	// // System.err.println("--");
	// // i = s.iterator();
	// // for(;i.hasNext();) {
	// // System.err.println("S: "+i.next());
	// // }
	// System.err.println("-----<<<");
	// return f;
	// }

	private HashMap<String, CacheableXMLSource> sources = new HashMap<String, CacheableXMLSource>();

	public synchronized CacheableXMLSource getSource(CLDRLocale locale,
			boolean isVetted) {
		String key = (isVetted ? "w." : "")
				+ (locale != null ? locale : "null");
		CacheableXMLSource src = sources.get(key);
		if (src != null && src.invalid()) {
			src.deleteInvalid();
			System.err.println("## " + serno + " / invalid: " + key);
			src = null;
		}
		if (src == null) {
			src = makeXMLSource(locale, isVetted);
			sources.put(key, src);
			System.err.println("## " + serno + " / + " + key);
		} else {
			// System.err.println("## " + serno + " / reuse " + key);
		}
		return src;
	}

	private synchronized CacheableXMLSource makeXMLSource(CLDRLocale locale,
			boolean isVetted) {
		CacheableXMLSource wxs;
		if (!isVetted) {
			wxs = new CacheableXMLSource(factory, locale);
		} else {
			wxs = new WinningXMLSource(factory, locale);
		}
		wxs.initialize();
		return wxs;
	}

	public XMLSource getCachedSource(String localeID) {
		if (localeID.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
			return realSource.make(localeID);
		}
		File cacheFile = getLocaleFile(localeID);
		XMLSource cachedFileSource = null;
		try {
			XMLSource src = realSource.make(localeID);
			CLDRFile aFile = new CLDRFile(src, false);
			FileOutputStream fos;
			fos = new FileOutputStream(cacheFile);
			PrintWriter pw = new PrintWriter(fos);
			aFile.write(pw);
			pw.close();
			fos.close();
			if (DEBUG_INSANE)
				System.err.println("## " + serno + " 1getsrc " + localeID
						+ "  @ " + cacheFile.getAbsolutePath());
			cachedFileSource = new CLDRFile.SimpleXMLSource(factory, localeID);

			/* Cause load */
			CLDRFile f = new CLDRFile(cachedFileSource, false);
			f.loadFromFile(cacheFile, localeID,
					CLDRFile.DraftStatus.unconfirmed);

			CacheableXMLSource wxs = new CacheableXMLSource(factory, CLDRLocale
					.getInstance(localeID));
			CLDRFile g = new CLDRFile(wxs, false);
			g.loadFromFile(cacheFile, localeID,
					CLDRFile.DraftStatus.unconfirmed);
			wxs.save(new File(cacheFile.getParentFile(), localeID + ".xpt"));
			// g.freeze();
			// f.freeze();
			if (DEBUG_INSANE)
				System.err.println("## " + serno + " loadation " + localeID
						+ "  @ " + cacheFile.getAbsolutePath());

			// cachedFileSource =
			// CLDRFile.makeFromFile(cacheFile.getAbsolutePath(),
			// getLocaleID(),CLDRFile.DraftStatus.unconfirmed).dataSource;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InternalError("While trying to cache " + cacheFile
					+ " - " + e.toString());
		}
		return cachedFileSource;
	}

	private File getLocaleFile(String localeID) {
		// TODO Auto-generated method stub
		return getLocaleFile(localeID, false);
	}

	private File getLocaleFile(String localeID, boolean isVetted) {
		// TODO Auto-generated method stub
		return new File(cacheDir, getLocaleFileName(localeID, isVetted));
	}

	/**
	 * Close out the connection
	 */
	public void closeConnection() {
		if (realSource instanceof CLDRDBSource) {
			// ((CLDRDBSource)realSource).closeConnection();
		}
	}
}
