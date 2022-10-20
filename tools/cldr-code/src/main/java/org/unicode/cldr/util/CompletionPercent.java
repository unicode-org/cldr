package org.unicode.cldr.util;

public class CompletionPercent {
    /**
     * Calculate a "user-friendly" percentage value between 0 and 100
     *
     * @param done the number of tasks that have been completed
     * @param total the total number of tasks
     * @return a number between 0 and 100
     */
    public static int calculate(int done, int total) {
        if (total <= 0) {
            // The task is finished since nothing needed to be done
            // Do not divide by zero
            return 100;
        }
        if (done <= 0) {
            return 0;
        }
        // Do not round 99.9 up to 100
        final int floor = (int) Math.floor((100 * (float) done) / (float) total);
        if (floor == 0) {
            // Do not round 0.001 down to zero
            // Instead, provide indication of slight progress
            return 1;
        }
        return Math.min(floor, 100);
    }
}
