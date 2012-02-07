package org.unicode.cldr.web;

import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * @author srl
 *
 */
public interface CLDRProgressIndicator {
    public interface CLDRProgressTask {
        /**
         * Done with progress, reset to nothing.
         */
        public abstract void close();
        
        /**
         * Update on the progress
         * @param count current count - up to Max
         */
        public abstract void update(int count);

        /**
         * Update the progress
         * @param count - up to max
         * @param what change the sub-item (i.e. this current item)
         */
        public abstract void update(int count, String what);

        /**
         * Update the sub-progress without moving the count
         * @param what
         */
        public abstract void update(String what);

        /**
         * The start time of the entire task
         * @return
         */
        public abstract long startTime();
    }
    /**
     * Initialize a progress that will not show the actual count, but treat the count as a number from 0-100
     * @param what the user-visible string
     */
    public abstract CLDRProgressTask openProgress(String what);

    /**
     * Initialize a Progress. 
     * @param what what is progressing
     * @param max the max count, or <0 if it is an un-numbered percentage (0-100 without specific numbers)
     */
    public abstract CLDRProgressTask openProgress(String what, int max);
}