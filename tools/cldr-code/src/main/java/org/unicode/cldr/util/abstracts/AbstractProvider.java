// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.util.abstracts;

/**
 * Interface for class which is able to provide an abstract
 * (summary) for a particular xpath.
 * 
 * @author srl295
 */
public interface AbstractProvider {
    /**
     * Does this provider match? Does not mean that data is available.
     * This is distinct from abstractFor so that we can tell which xpaths
     * have a provider at all, versus those for whom an actual abstract is not
     * available.
     * 
     * @param xpath
     * @return 
     */
    public boolean matches(String xpath);
    
    /**
     * Return the Abstract Result, or null
     * @param xpath
     * @return 
     */
    public AbstractResult abstractFor(String xpath);
}
