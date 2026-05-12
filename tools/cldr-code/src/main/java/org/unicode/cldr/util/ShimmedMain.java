package org.unicode.cldr.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.icu.dev.test.TestFmwk.TestGroup;

/**
 * Utilities for the TestShims should go away when we move to all junit tests. These are here so
 * they can be shared between tools/java and tools/cldr-apps
 */
public class ShimmedMain {
    public static final String TEST_ARGS = ".testArgs";
    public static final String DEFAULT_ARGS = "-n -q";
    final String[] args;
    private final Class<? extends TestGroup> clazz;
    private static final Map<String, String> allProps = new ConcurrentHashMap<>();

    /** get the "args" for this class */
    public static String[] getArgs(Class<?> clazz) {
        return getArgs(clazz, DEFAULT_ARGS);
    }

    /** get the "args" for this class, with a specific default argument */
    public static String[] getArgs(Class<?> clazz, final String defaultArgs) {
        final String packageName = clazz.getPackage().getName();
        return getArgs(packageName, defaultArgs);
    }

    public static String[] getArgs(final String packageName, final String defaultArgs) {
        final String propKey = packageName + TEST_ARGS;
        final String[] s = getAndSplit(defaultArgs, propKey);
        return s;
    }

    private static String[] getAndSplit(final String defaultArgs, final String propKey) {
        // only get and print once
        final String toSplit =
                allProps.computeIfAbsent(
                        propKey,
                        k -> {
                            final String v = System.getProperty(k, defaultArgs);
                            System.err.println(k + "=" + v); // only print once due to the hashmap
                            return v;
                        });
        final String s[] = toSplit.split(" "); // TODO: quoted strings, etc.
        return s;
    }

    public static String[] getArgs(final String packageName) {
        return getArgs(packageName, DEFAULT_ARGS);
    }

    protected ShimmedMain(Class<? extends TestGroup> clazz) {
        this.clazz = clazz;
        args = getArgs(clazz);
    }

    public String[] getArgs() {
        return args;
    }

    /* call from main to run tests */
    public int runTests() {
        try (final PrintWriter out = new PrintWriter(System.out)) {
            final Method m = clazz.getMethod("main", String[].class, PrintWriter.class);
            return (Integer) m.invoke(null, getArgs(), out);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException nsm) {
            throw new RuntimeException("Could not invoke main for " + clazz, nsm);
        }
    }

    /** Go fishing and find all TestFmwk subclasses in the package. */
    public static String[] findAllTests(final Package inPackage) {
        // keep these in lexical order
        final Set<String> allTestClasses = new TreeSet<String>();
        // use ClassGraph to look for all subclasses of TestFmwk
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(inPackage.getName())
                        .enableAnnotationInfo()
                        .enableClassInfo()
                        .scan()) {
            for (ClassInfo info : scanResult.getSubclasses(TestFmwk.class)) {
                if (info.getPackageName().equals(inPackage.getName())
                        && !info.getSuperclass()
                                .getName()
                                .equals(TestFmwk.TestGroup.class.getName())) {
                    if (info.hasAnnotation("org.junit.jupiter.api.Disabled")) {
                        System.err.println("Skipping, @Disabled: " + info.getName());
                    } else {
                        allTestClasses.add(info.getName());
                    }
                }
            }
            return allTestClasses.toArray(new String[allTestClasses.size()]);
        }
    }
}
