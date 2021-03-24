package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRURLS;

/**
 * Implement a 'main' for the cldr-code jar.
 */
@CLDRTool(alias = "main",
    description = "The 'main' class invoked when java -jar or double-clicking the jar.",
    hidden = "Hidden so as not to list itself",
    url = CLDRURLS.TOOLSURL)
class Main {
    private static final String CLASS_SUFFIX = ".class";
    private static final String MAIN = "main";
    private static final boolean DEBUG = false;

    public static void main(String args[]) throws Throwable {
        if (args.length == 0) {
            printUsage();
            // To be friendly to the user, go ahead and list out classes while we're at it.
            listClasses(false);
        } else if (args.length == 1 && args[0].equals("-l")) {
            listClasses(true);
        } else {
            final String mainClass = args[0];
            final String args2[] = new String[args.length - 1];
            System.arraycopy(args, 1, args2, 0, args2.length);

            Class<?> c = findMainClass(mainClass);

            if (c == Main.class) {
                throw new IllegalArgumentException("Main doesn’t need to invoke Main.");
            }

            System.err.println(">> " + c.getName());

            tryCurrentDirAsCldrDir();
            invoke(c, args2);
        }
    }

    public static void invoke(Class<?> c, final String[] args2)
            throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Method main = getStaticMain(c);
        if(main == null) {
            throw new NullPointerException("No static main() found in " + c.getSimpleName());
        }
        main.invoke(null, (Object) args2);
    }

    public static Class<?> findMainClass(final String mainClass) throws IllegalArgumentException, IOException {
        Class<?> c = null;
        try {
            // This shortcut allows operation even out of a jar context.
            c = Class.forName(mainClass);
        } catch (ClassNotFoundException e) {
            // not found
        }
        if (c == null) {
            c = findMainClass(mainClass, getMainClassList());
        }
        if (c == null) {
            throw new IllegalArgumentException("Class not found and not an alias: " + mainClass);
        }
        return c;
    }

    /**
     * If CLDR_DIR is not set as a system property, try to set it.
     * This does not invoke the CLDRConfig mechanism, and so would be ignored
     * if run from (say) the SurveyTool environment.
     */
    public static void tryCurrentDirAsCldrDir() {
        try {
            if(System.getProperty("CLDR_DIR") == null) {
                if(new File("./common/main/root.xml").exists()) {
                    System.err.println("Note: CLDR_DIR was unset but you seem to be in a CLDR directory. Setting -DCLDR_DIR=.");
                    System.setProperty("CLDR_DIR", ".");
                }
            }
        } catch(SecurityException t) {
            // ignore
        }
    }

    public static void printUsage() {
        System.out.println("Usage:  [ -l | [class|alias] args ...]");
        System.out.println("Example usage:");
        System.out.println(" (java -jar cldr-code.jar ) -l          -- prints a list of ALL tool/util/test classes with a 'main()' function.");
        System.out.println(" (java -jar cldr-code.jar ) org.unicode.cldr.util.XMLValidator  somefile.xml ...");
        System.out.println(" (java -jar cldr-code.jar ) validate  somefile.xml ...");
        System.out.println("For more info: " + CLDRURLS.TOOLSURL);
        System.out.println("CLDRFile.GEN_VERSION=" + CLDRFile.GEN_VERSION);
        System.out.println("(Use the -l option to list hidden/undocumented tools)");
        System.out.println();
    }

    /**
     * Print out classes
     * @param includeHidden
     */
    private static void listClasses(boolean includeHidden) throws IOException {
        for(final ClassEntry e : getMainClassList()) {
            if(!includeHidden && e.isHidden()) {
                if(DEBUG) System.err.println("Skipping: " + e.fullName());
                continue; // skip these
            }
            final CLDRTool annotation = e.getAnnotation();
            if (annotation != null) {
                System.out.println(e.alias() + " - " + annotation.description());
                if (annotation.url().length() > 0) {
                    System.out.println("   <" + annotation.url() + ">");
                } else {
                    System.out.println("   <" + CLDRURLS.TOOLSURL + annotation.alias() + ">");
                }
                if (e.isHidden()) {
                    System.out.println("   HIDDEN: " + annotation.hidden());
                }
            } else {
                System.out.println(e.name() + " - " + "(no @CLDRTool annotation)");
            }
            System.out.println(" = " + e.fullName());
        }
    }

    private static Class<?> findMainClass(String mainClass, Set<ClassEntry> mainClassList) {
        for(final ClassEntry e : mainClassList) {
            if (mainClass.equalsIgnoreCase(e.name()) ||
                mainClass.equalsIgnoreCase(e.alias())) {
                return e.theClass; // match the classname
            }
        }
        return null;
    }

    /**
     * A candidate class with a main in it
     */
    public static class ClassEntry implements Comparable<ClassEntry> {
        public Class<?> theClass;
        public CLDRTool annotation;

        @Override

        public int compareTo(ClassEntry o) {
            return theClass.getSimpleName().compareTo(o.theClass.getSimpleName());
        }

        public ClassEntry(Class<?> c) {
            this.theClass = c;
            this.annotation = c.getAnnotation(CLDRTool.class);
        }

        public CLDRTool getAnnotation() {
            return annotation;
        }

        /**
         * is this class hidden from the usual list?
         * @return
         */
        public boolean isHidden() {
            return (annotation == null ||            // no annotation
                    !annotation.hidden().isEmpty()); // hidden≠""
        }

        public String name() {
            return this.theClass.getSimpleName();
        }

        String alias() {
            if( annotation != null) {
                return annotation.alias();
            } else {
                return "";
            }
        }

        private String fullName() {
            return theClass.getName();
        }
    }

    /**
     * Get a list of classes which have a static main
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static Set<ClassEntry> getMainClassList() throws IOException, FileNotFoundException {
        final Set<ClassEntry> theList = new TreeSet<>();
        final java.util.jar.JarInputStream jis = getJarInputStream();
        final ClassLoader classLoader = Main.class.getClassLoader();
        if (jis == null) {
            throw new NullPointerException("could not get Jar InputStream");
        }
        java.util.jar.JarEntry je;
        while ((je = jis.getNextJarEntry()) != null) {
            final String name = je.getName();
            if (inOuterClass(name)) {
                final String className = filenameToClassName(name);
                if(!isCldrClassName(className)) {
                    if(DEBUG) {
                        System.err.println("Skipping non-CLDR " + className);
                    }
                    continue; // skip non-CLDR things
                }
                try {
                    final Class<?> c = java.lang.Class.forName(className, false, classLoader);

                    if (getStaticMain(c) != null) {
                        // skip classes w/o static method - even if they have an annotation.
                        theList.add(new ClassEntry(c));
                    }
                } catch (ClassNotFoundException | NoSuchMethodException | NoClassDefFoundError t) {
                    // ignore uninstantiable.
                }
            }
        }
        return theList;
    }

    public static boolean isCldrClassName(final String className) {
        return className.startsWith("org.unicode.cldr");
    }

    /**
     * @param c
     * @return
     * @throws NoSuchMethodException
     */
    public static Method getStaticMain(final Class<?> c) throws NoSuchMethodException {
        return c.getMethod(MAIN, String[].class);
    }

    /**
     * @param name
     * @return
     */
    public static String filenameToClassName(final String name) {
        return name.substring(0, name.length() - (CLASS_SUFFIX.length()))
            .replaceAll("/", ".");
    }

    /**
     * @param name
     * @return
     */
    public static boolean inOuterClass(final String name) {
        return name.endsWith(CLASS_SUFFIX) &&
            !name.contains("$");
    }

    /**
     * Fetch the whole jar as an input stream
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static java.util.jar.JarInputStream getJarInputStream() throws IOException, FileNotFoundException {
        final java.net.URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getPath().endsWith(".jar")) {
            System.out.println("(Not inside a .jar file - no listing available.)");
            return null;
        }
        if (DEBUG) System.out.println("Classes in " + url.getPath());
        final java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(new java.io.FileInputStream(url.getPath()));
        java.util.jar.JarEntry je = null;
        return jis;
    }
}
