// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.util.abstracts;

import java.time.Instant;
import org.unicode.cldr.util.Pair;

/**
 * An AbstractResult represents 
 * @author srl295
 */
public class AbstractResult {

    /**
     * @return the contentDate
     */
    public Instant getContentDate() {
        return contentDate;
    }

    /**
     * @return the resourceUri
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    private final Instant contentDate;
    private final String resourceUri;
    private final String content;
    
    public AbstractResult(Instant d, String u, String c) {
        this.contentDate = d;
        this.resourceUri = u;
        this.content = c;
    }
    
    public AbstractResult(Pair<String,String> resAbs) {
        this.contentDate = Instant.now();
        this.resourceUri = resAbs.getFirst();
        this.content = resAbs.getSecond();
    }
    
    @Override
    public String toString() {
        return "<"+resourceUri + ">\n\t" + contentDate.toString() + "\n\t" + content + "\n";
    }
}
