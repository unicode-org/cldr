package org.unicode.cldr.web;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.CLDRFileCache.CacheableXMLSource;

/**
 * @author srl
 *
 */
public class MuxedSource extends XMLSource {
	
	public interface MuxFactory {

		XMLSource getRootDbSource();

		CacheableXMLSource getSourceFromCache(CLDRLocale locale,
				boolean finalData);

		XMLSource getMuxedInstance(CLDRLocale instance);
		
	}
	
	private CLDRLocale locale;

	private XMLSource dbSource = null;
	private CacheableXMLSource cachedSource = null;
	private boolean finalData;
	MuxFactory mfactory = null;
	/**
	 * 
	 */
	public MuxedSource(MuxFactory mfactory, CLDRLocale locale, boolean finalData) {
		this.mfactory = mfactory;
		this.setLocaleID(locale.toString());
		this.locale = locale;
		this.finalData = finalData;
		this.dbSource = mfactory.getRootDbSource().make(locale.toString());
		this.cachedSource = mfactory.getSourceFromCache(locale, finalData);
	}
	
	public boolean invalid() {
		return cachedSource.invalid();
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getAvailableLocales()
	 */
	@Override
	public Set getAvailableLocales() {
		return dbSource.getAvailableLocales();
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
	 */
	@Override
	public String getFullPathAtDPath(String path) {
		if(false) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " = " + cachedSource.getFullPathAtDPath(path));    }
		return cachedSource.getFullPathAtDPath(path);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
	 */
	@Override
	public String getWinningPath(String path) {
		//   if(path.indexOf("=\"TR")>0&&path.indexOf("symbol")>0) { /*srl*/
		//      if(true) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " WP= " + cachedSource.getWinningPath(path));    }
		//  } */
		if(false) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " = " + cachedSource.getWinningPath(path));    }
		return cachedSource.getWinningPath(path);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getSupplementalDirectory()
	 */
	@Override
	public File getSupplementalDirectory() {
		// TODO Auto-generated method stub
		return dbSource.getSupplementalDirectory();
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
	 */
	@Override
	public String getValueAtDPath(String path) {
		//if(path.contains("gregorian")) throw new InternalError("Who wants to know?");
		if(false) {  System.err.println("NN: ["+path+"] @"+getLocaleID() + " = " + cachedSource.getValueAtDPath(path));    }
		return cachedSource.getValueAtDPath(path);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#getXpathComments()
	 */
	@Override
	public Comments getXpathComments() {
		// TODO Auto-generated method stub
		return cachedSource.getXpathComments();
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#iterator()
	 */
	@Override
	public Iterator<String> iterator() {
		// TODO Auto-generated method stub
		if(false) {  System.err.println("NN: @"+getLocaleID());
		for(Iterator i = (Iterator)cachedSource.iterator();i.hasNext();) {
			System.err.println("// "+i.next().toString());
		} }
		return cachedSource.iterator();
	}

	public Iterator<String> iterator(String str) {
		if(false) {  System.err.println("NN: ["+str+"] @"+getLocaleID());
		for(Iterator i = (Iterator)cachedSource.iterator(str);i.hasNext();) {
			System.err.println("// "+i.next().toString());
		} }
		return cachedSource.iterator(str);
	}
	//
	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#make(java.lang.String)
	 */
	@Override
	public XMLSource make(String localeID) {
		if(localeID.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
			return dbSource.make(localeID); // will fal through to raw files
		} else {
			return mfactory.getMuxedInstance(CLDRLocale.getInstance(localeID));
		}
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String, java.lang.String)
	 */
	@Override
	public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
		cachedSource.putFullPathAtDPath(distinguishingXPath, fullxpath);
		dbSource.putFullPathAtDPath(distinguishingXPath, fullxpath);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String, java.lang.String)
	 */
	@Override
	public void putValueAtDPath(String distinguishingXPath, String value) {
		// TODO Auto-generated method stub
		cachedSource.putValueAtDPath(distinguishingXPath, value);
		dbSource.putValueAtDPath(distinguishingXPath, value);
	}

	public String putValueAtPath(String x, String v) {
		cachedSource.putValueAtPath(x, v);
		return dbSource.putValueAtPath(x, v);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
	 */
	@Override
	public void removeValueAtDPath(String distinguishingXPath) {
		cachedSource.removeValueAtDPath(distinguishingXPath);
		dbSource.removeValueAtDPath(distinguishingXPath);
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr.util.XPathParts.Comments)
	 */
	@Override
	public void setXpathComments(Comments comments) {
		// TODO Auto-generated method stub
		throw new InternalError("not imp");

	}

	/**
	 * @see com.ibm.icu.util.Freezable#freeze()
	 */
	public Object freeze() {
		// TODO Auto-generated method stub
		//            locked = true;
		//            return this;
		throw new InternalError("freeze not suported. [clone and lock?]");
	}

}
