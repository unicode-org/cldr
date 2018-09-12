package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class TestFmwkPlus extends TestFmwk {

    @SuppressWarnings("unchecked")
    public <T, V, R extends TestRelation<T, V>> boolean assertTrue(
        String message, T arg0, R relation, V... args) {
        return assertRelation(message, true, arg0, relation, args);
    }

    @SuppressWarnings("unchecked")
    public <T, V, R extends TestRelation<T, V>> boolean assertFalse(
        String message, T arg0, R relation, V... args) {
        return assertRelation(message, false, arg0, relation, args);
    }

    @SuppressWarnings("unchecked")
    public <T, V, R extends TestRelation<T, V>> boolean assertTrue(T arg0,
        R relation, V... args) {
        return assertRelation(null, true, arg0, relation, args);
    }

    @SuppressWarnings("unchecked")
    public <T, V, R extends TestRelation<T, V>> boolean assertFalse(T arg0,
        R relation, V... args) {
        return assertRelation(null, false, arg0, relation, args);
    }

    @SuppressWarnings("unchecked")
    public <T, V, R extends TestRelation<T, V>> boolean assertRelation(
        String message, boolean expected, T arg0, R relation, V... args) {
        boolean actual = args.length == 0 ? relation.isTrue(arg0) : relation
            .isTrue(arg0, args);
        boolean test = expected == actual;
        if (!test) {
            errln(showArgs("", message, actual, arg0, relation, args)
                + "; expected " + expected);
        } else if (isVerbose()) {
            logln(showArgs("OK ", message, actual, arg0, relation, args));
        }
        return test;
    }

    public <E, S, T extends Transform<S, E>> boolean assertTransformsTo(
        String message, E expected, T transform, S source) {
        E actual = transform.transform(source);
        boolean test = CldrUtility.equals(expected, actual);
        if (!test) {
            errln(showArgs("", message, expected, actual, transform, source)
                + "; expected " + "‹" + expected + "›");
        } else if (isVerbose()) {
            logln(showArgs("OK ", message, expected, actual, transform, source));
        }
        return test;
    }

    private <E, S, T extends Transform<S, E>> String showArgs(String prefix,
        String message, E expected, E actual, T transform, S source) {
        String simpleName = transform instanceof Transliterator ? ((Transliterator) transform)
            .getID() : transform.getClass().getSimpleName();
        return prefix + sourceLocationPlus() + " "
            + (message == null ? "" : message + " : ") + "got ‹" + actual
            + "› from " + simpleName + "(‹" + source + "›)";
    }

    @SuppressWarnings("unchecked")
    private <T, V, R extends TestRelation<T, V>> String showArgs(String prefix,
        String message, boolean expected, T arg0, R relation, V... args) {
        StringBuilder others = new StringBuilder();
        for (V arg : args) {
            if (others.length() != 0) {
                others.append(", ");
            }
            others.append(relation.showOther(arg));
        }
        return prefix + sourceLocationPlus() + " "
            + (message == null ? "" : message + " : ")
            + relation.showFirst(arg0) + (expected ? " " : " NOT ")
            + relation + " " + others;
    }

    public abstract static class TestRelation<T, U> {
        @SuppressWarnings("unchecked")
        public abstract boolean isTrue(T a, U... b);

        @Override
        public String toString() {
            String name = this.getClass().getName();
            int pos = name.lastIndexOf('$');
            if (pos >= 0) {
                return name.substring(pos + 1);
            }
            pos = name.lastIndexOf('.');
            return pos < 0 ? name : name.substring(pos + 1);
        }

        public String showFirst(T a) {
            return show(String.valueOf(a));
        }

        public String showOther(U b) {
            return show(String.valueOf(b));
        }

        public String show(String a) {
            return "‹" + a + "›";
        }
    }

    public static class Invert<T, U> extends TestRelation<T, U> {
        private final TestRelation<T, U> other;

        public Invert(TestRelation<T, U> other) {
            this.other = other;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isTrue(T a, U... b) {
            return !other.isTrue(a, b);
        }

        @Override
        public String toString() {
            return "not " + other;
        }
    }

    public static class And<T, U> extends TestRelation<T, U> {
        private final TestRelation<T, U>[] others;

        @SuppressWarnings("unchecked")
        public And(TestRelation<T, U>... others) {
            this.others = others;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isTrue(T a, U... b) {
            for (TestRelation<T, U> other : others) {
                if (!other.isTrue(a, b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return CollectionUtilities.join(others, " and ");
        }
    }

    public static class Or<T, U> extends TestRelation<T, U> {
        private final TestRelation<T, U>[] others;

        @SuppressWarnings("unchecked")
        public Or(TestRelation<T, U>... others) {
            this.others = others;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isTrue(T a, U... b) {
            for (TestRelation<T, U> other : others) {
                if (other.isTrue(a, b)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return CollectionUtilities.join(others, " or ");
        }
    }

    @SuppressWarnings("rawtypes")
    public static TestRelation CONTAINS = new TestRelation<Collection, Object>() {
        @Override
        public boolean isTrue(Collection a, Object... bs) {
            for (Object b : bs) {
                if (!a.contains(b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "contains";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation CONTAINS_ALL = new TestRelation<Collection, Object>() {
        @Override
        public boolean isTrue(Collection a, Object... bs) {
            for (Object b : bs) {
                if (!(b instanceof Collection)) {
                    return false;
                }
                if (!a.containsAll((Collection) b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "contains-all";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation CONTAINS_SOME = new TestRelation<Collection, Object>() {
        @Override
        public boolean isTrue(Collection a, Object... bs) {
            for (Object b : bs) {
                if (!(b instanceof Collection)) {
                    return false;
                }
                if (Collections.disjoint(a, (Collection) b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "contains-some";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation EMPTY = new TestRelation<Collection, Object>() {
        @Override
        public boolean isTrue(Collection a, Object... bs) {
            if (bs.length != 0) {
                throw new IllegalArgumentException(
                    "Should only have 1 argument");
            }
            return a.size() == 0;
        }

        @Override
        public String toString() {
            return "is empty";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation LEQ = new TestRelation<Comparable, Comparable>() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean isTrue(Comparable a, Comparable... bs) {
            if (bs.length != 1) {
                throw new IllegalArgumentException("Should have 2 arguments");
            }
            return a.compareTo(bs[0]) <= 0;
        }

        @Override
        public String toString() {
            return " ≤ ";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation GEQ = new TestRelation<Comparable, Comparable>() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean isTrue(Comparable a, Comparable... bs) {
            if (bs.length != 1) {
                throw new IllegalArgumentException("Should have 2 arguments");
            }
            return a.compareTo(bs[0]) >= 0;
        }

        @Override
        public String toString() {
            return "≥ ";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation IDENTICAL = new TestRelation<Object, Object>() {
        @Override
        public boolean isTrue(Object a, Object... bs) {
            for (Object b : bs) {
                if (a != b) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "is identical to";
        }
    };

    @SuppressWarnings("rawtypes")
    public static TestRelation CONTAINS_US = new TestRelation<UnicodeSet, Object>() {
        @Override
        public boolean isTrue(UnicodeSet a, Object... bs) {
            for (Object b : bs) {
                if (b instanceof UnicodeSet) {
                    if (!a.containsAll((UnicodeSet) b)) {
                        return false;
                    }
                } else if (b instanceof Integer) {
                    if (!a.contains((Integer) b)) {
                        return false;
                    }
                }
            }
            return true;
        }

        public String showFirst(UnicodeSet a) {
            return show(a.toPattern(false));
        }

        public String showOther(UnicodeSet b) {
            return show(b.toPattern(false));
        }

        @Override
        public String toString() {
            return "contains";
        }
    };

    private String sourceLocationPlus() {
        // Walk up the stack to the first call site outside this file
        StackTraceElement[] st = new Throwable().getStackTrace();
        for (int i = 0; i < st.length; ++i) {
            if (!"TestFmwkPlus.java".equals(st[i].getFileName())) {
                return "File " + st[i].getFileName() + ", Line "
                    + st[i].getLineNumber();
            }
        }
        throw new InternalError();
    }

    public static void main(String[] args) {
        new TestFmwkPlus().run(args);
    }

    public void TestTest() {
        Set<String> containerA = new HashSet<String>();
        String stringA = "a";
        String stringA2 = "ab".substring(0, 1);
        containerA.add(stringA);

        String stringB = "b";

        logln("These work");

        assertNotEquals("should be different", stringA, stringB);
        assertEquals("should be same", stringA, stringA2);

        logln("These work, but the messages are not clear because you can't see the arguments");

        assertTrue("should be contained", containerA.contains(stringA));
        assertFalse("should not be contained", containerA.contains(stringB));

        logln("These work, because you can see the arguments");

        assertFalse(stringA, IDENTICAL, stringA2);
        assertTrue(containerA, EMPTY);

        assertTrue(containerA, CONTAINS, stringA);
        assertFalse(containerA, CONTAINS, stringB);

        assertTrue(containerA, new Or(CONTAINS, IDENTICAL), stringA);
        assertFalse(containerA, new And(CONTAINS, IDENTICAL), stringA);

        assertTrue(new UnicodeSet("[:L:]"), CONTAINS_US, 'a', new UnicodeSet(
            "[ab]"));
        assertTrue(3, LEQ, 4);
    }
}
