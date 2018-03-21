package org.unicode.cldr.draft;

/**
 * Alternate way to iterate through code points
 * 
 * @author markdavis
 */
public final class CodePoints {
    private static final int SUPPLEMENTAL_OFFSET = (Character.MIN_HIGH_SURROGATE << 10) + Character.MIN_LOW_SURROGATE
        - Character.MIN_SUPPLEMENTARY_CODE_POINT;

    private CharSequence buffer;
    private int length;
    private int position = 0;
    private int codePoint;

    /**
     * Set up the iterator.
     * 
     * @param s
     */
    public CodePoints(CharSequence s) {
        buffer = s;
        length = s.length();
    }

    public void reset(CharSequence s) {
        buffer = s;
        length = s.length();
        position = 0;
    }

    /**
     * Reset to the start.
     */
    public void reset() {
        position = 0;
    }

    /**
     * Get the next code point. False if at end of string. After successful next(), the codePoint field has the value.
     * 
     * @return
     */
    public boolean next() {
        if (position < 0 || position >= length) {
            codePoint = '\uFFFF';
            return false;
        }
        int cp = buffer.charAt(position++);
        if (cp >= Character.MIN_HIGH_SURROGATE && cp <= Character.MAX_HIGH_SURROGATE && position < length) {
            int trail = buffer.charAt(position);
            if (trail >= Character.MIN_LOW_SURROGATE && trail <= Character.MAX_LOW_SURROGATE) {
                cp = (cp << 10) + trail - SUPPLEMENTAL_OFFSET;
                ++position;
            }
        }
        codePoint = cp;
        return true;
    }

    /**
     * After calling next(), if it comes back true, this contains the code point; if not, it has U+FFFF.
     * 
     * @return
     */
    public int getCodePoint() {
        return codePoint;
    }

    public String toString() {
        return buffer.subSequence(0, position) + "|||" + buffer.subSequence(position, buffer.length());
    }

    /**
     * When iterating over full strings, this method is the fastest (as long as the string is not huge).
     * 
     * @param s
     * @return
     */
    public static int[] full(CharSequence s) {
        int len = s.length();
        int[] result = new int[len];
        int pos = 0;
        for (int i = 0; i < len;) {
            int cp = s.charAt(i++);
            // The key to performance is that surrogate pairs are very rare.
            // Test for a trail (low) surrogate.
            if (cp >= Character.MIN_LOW_SURROGATE && cp < Character.MAX_LOW_SURROGATE && pos > 0) {
                // If we get a trail, and if the last code point was a lead (high) surrogate,
                // we need to backup and set the correct value
                int last = result[pos - 1];
                if (last >= Character.MIN_HIGH_SURROGATE && last <= Character.MAX_HIGH_SURROGATE) {
                    --pos;
                    cp += (last << 10) - SUPPLEMENTAL_OFFSET;
                }
            }
            result[pos++] = cp;
        }
        // In the unusual case that we hit a supplemental code point, resize
        if (pos < len) {
            int[] result2 = new int[pos];
            System.arraycopy(result, 0, result2, 0, pos);
            result = result2;
        }
        return result;
    }

}
