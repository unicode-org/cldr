package org.unicode.cldr.tool;

import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.UnicodeSet;

@SuppressWarnings("deprecation")
public final class EFixedDecimal extends FixedDecimal {
    private static final long serialVersionUID = 1L;
    private int e;

    private EFixedDecimal(String n, int e) {
        this(Double.parseDouble(n), getVisibleFractionCount(n), e);
    }

    private EFixedDecimal(double fd, int v, int e) {
        super(fd, v);
        this.e = e;
        if (super.getIntegerValue() < 0) {
            throw new IllegalArgumentException("unexpected negative value" + super.getIntegerValue());
        }
    }

    static final UnicodeSet exp = new UnicodeSet("[eE]").freeze();

    public static EFixedDecimal fromString(String expFormat) {
        int posE = exp.findIn(expFormat, 0, false); // returns length if not found
        final int exponent = posE == expFormat.length() ? 0 :
            Integer.parseInt(expFormat.substring(posE+1));
        return new EFixedDecimal(expFormat.substring(0,posE), exponent);
    }

    public static EFixedDecimal fromDoubleVisibleAndExponent(double fd, int v, int exp) {
        return new EFixedDecimal(fd, v, exp);
    }

    public static EFixedDecimal fromDoubleAndVisibleDigits(double fd, int visibleDigits) {
        return new EFixedDecimal(fd, visibleDigits, 0);
    }

    public int getExponent() {
        return e;
    }

    private static int getVisibleFractionCount(String value) { // is in FixedDecimal, but we don't have access
        value = value.trim();
        int decimalPos = value.indexOf('.') + 1;
        if (decimalPos == 0) {
            return 0;
        } else {
            return value.length() - decimalPos;
        }
    }

    @Override
    public double getPluralOperand(Operand operand) {
        switch(operand) {
        case e: return e;
        default: return super.getPluralOperand(operand);
        }
    }

    @Override
    public String toString() {
        return e == 0 ? super.toString() : super.toString() + "e" + e;
    }
    @Override
    public boolean equals(Object arg0) {
        return super.equals(arg0) && ((IFixedDecimal)arg0).getPluralOperand(Operand.e) == e;
    }

    public int compareTo(EFixedDecimal arg0) {
        int result = super.compareTo(arg0);
        return result != 0 ? result : Integer.compare(e, arg0.e);
    }

    @Override
    public double doubleValue() {
        return super.doubleValue()*Math.pow(10, e);
    }
}