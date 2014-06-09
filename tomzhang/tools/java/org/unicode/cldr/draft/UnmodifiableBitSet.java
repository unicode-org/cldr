package org.unicode.cldr.draft;

import java.util.BitSet;

public class UnmodifiableBitSet extends BitSet {

    private static final long serialVersionUID = 2506181560723087578L;

    public UnmodifiableBitSet(BitSet s) {
        // clone for safety
        super.clear();
        super.or(s);
    }

    public BitSet getModifiableBitset() {
        // clone for safety
        BitSet result = new BitSet();
        result.or(this);
        return result;
    }

    @Override
    public void and(BitSet set) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void andNot(BitSet set) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void clear(int bitIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void clear(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void flip(int bitIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    };

    @Override
    public void flip(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void set(int bitIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void set(int bitIndex, boolean value) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void or(BitSet set) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    @Override
    public void xor(BitSet set) {
        throw new UnsupportedOperationException("Cannot modify.");
    }

    // public void TestBitset() {
    // BitSet s = new BitSet();
    // s.set(1);
    // BitSet z = new BitSet();
    // z.set(2);
    //
    // UnmodifiableBitSet t = new UnmodifiableBitSet(s);
    // try {
    // t.clear();
    // errln("Failed to fail");
    // } catch (UnsupportedOperationException e){}
    // try {
    // t.or(z);
    // errln("Failed to fail");
    // } catch (UnsupportedOperationException e){}
    // z.or(t);
    // }

}
