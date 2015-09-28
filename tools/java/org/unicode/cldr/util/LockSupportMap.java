package org.unicode.cldr.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Helper class: Provide a way to associate a String with an Object that can be used for locking.
 * Different instances of the same String will return the same Object
 *
 * @author ribnitz
 *
 */
public class LockSupportMap<E> {
    /**
     * Map to keep the track of the locks used for single entries; these are used for synchronization
     */
    private final ConcurrentMap<E, Object> locks = new ConcurrentHashMap<>();

    public LockSupportMap() {
        // do nothing
    }

    /**
     * Retrieve the object that is used to lock the entry for this filename
     * @param fileName
     * @return
     */
    public Object getItemLock(E item) {
        /*
         * The putIfAbsent needs an object to insert, if it none is there,
         * the "cheapest" object that can be created is Object.
         */
        Object oldLock = new Object();
        Object newLock = locks.putIfAbsent(item, oldLock);
        /*
         * PutIfAbsent has returned the previous value, if one was there
         * So, the oldLock needs is the value to return if newLock is null.
         */
        Object sync = newLock == null ? oldLock : newLock;
        return sync;
    }

    /**
     * Remove the the object associated with the name from the map, and return it
     * @param nameToRemove
     * @return
     */
    public Object removeItemLock(E itemToRemove) {
        synchronized (getItemLock(itemToRemove)) {
            return locks.remove(itemToRemove);
        }
    }
}