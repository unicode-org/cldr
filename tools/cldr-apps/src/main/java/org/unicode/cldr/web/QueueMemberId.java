package org.unicode.cldr.web;

import java.util.Hashtable;

/**
 * Uniquely identify members that participate in queues to receive data; associate
 * an arbitrary object with each member
 *
 * A member may be (1) a user (maybe logged in), identified by their CookieSession, or
 * (2) an automatically scheduled background operation not initiated by a particular user
 *
 * Multiple HTTP requests and responses may be required for the same user to fetch the same
 * data set -- gathering the data is assumed to be a potentially long-running task
 *
 * When there are concurrent requests by multiple users, the CookieSession is used
 * for determining which requests are from the same user
 *
 * The associated object could be anything, depending on the caller; for VettingViewerQueue,
 * the object might belong to the class org.unicode.cldr.web.VettingViewerQueue.QueueEntry
 *
 * The "key" parameter for the put/set methods enables reusing this class for different kinds of queue
 *
 * For VettingViewerQueue, the key might be VettingViewerQueue.class.getName(); other kinds of
 * queues should have distinct keys
 *
 * For a given key, there is assumed to be no more than one "automatic" queue member at a time
 *
 * This class is meant to be re-usable and not to depend on VettingViewerQueue
 */
public class QueueMemberId {

    /**
     * The hash of objects associated with the automatic member
     * -- null if the member is not automatic
     */
    private Hashtable<String, Object> autoEntryHash = null;

    /**
     * The CookieSession that identifies a member who is a user
     * -- null if the member is automatic
     */
    private CookieSession cs = null;

    /**
     * This constructor is only intended for automatically scheduled operations,
     * not for operations initiated by a particular user
     */
    public QueueMemberId() {
        autoEntryHash = new Hashtable<String, Object>();
    }

    /**
     * This constructor is for usage initiated by a particular user
     *
     * @param cs the CookieSession identifying the user
     */
    public QueueMemberId(CookieSession cs) {
        this.cs = cs;
    }

    public void put(String key, Object entry) {
        if (autoEntryHash != null) {
            autoEntryHash.put(key, entry);
        } else if (cs != null) {
            cs.put(key, entry);
        }
    }

    public Object get(String key) {
        if (autoEntryHash != null) {
            return autoEntryHash.get(key);
        } else if (cs != null) {
            return cs.get(key);
        } else {
            return null;
        }
    }
}
