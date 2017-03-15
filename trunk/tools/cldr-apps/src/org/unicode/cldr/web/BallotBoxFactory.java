/**
 *
 */
package org.unicode.cldr.web;

import org.unicode.cldr.util.CLDRLocale;

/**
 * @author srl
 *
 */
public interface BallotBoxFactory<T> {
    /**
     * Get the ballot box for some locale. This allows something like
     * XMLSource.make() to change its locale.
     *
     * @param locale
     * @return the ballot box
     */
    public BallotBox<T> ballotBoxForLocale(CLDRLocale locale);
}
