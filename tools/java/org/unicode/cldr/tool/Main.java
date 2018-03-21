package org.unicode.cldr.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTool;

/**
 *
 * Implement a 'main' for the CLDR jar.
 */
@CLDRTool(alias = "main", description = "The 'main' class invoked when java -jar or doubleclicking the jar.", hidden = "Hidden so as not to list itself",
    url = Main.TOOLSURL)
class Main {
    private static final String CLASS_SUFFIX = ".class";
    private static final String MAIN = "main";
    public static final String TOOLSURL = "http://cldr.unicode.org/tools/";

    public static void main(String args[]) throws Throwable {
        if (args.length == 0) {
            System.out.println("Usage:  [ -l | [class|alias] args ...]");
            System.out.println("Example usage:");
            System.out.println(" (java -jar cldr.jar ) -l          -- prints a list of ALL tool/util/test classes with a 'main()' function.");
            System.out.println(" (java -jar cldr.jar ) org.unicode.cldr.util.XMLValidator  somefile.xml ...");
            System.out.println(" (java -jar cldr.jar ) validate  somefile.xml ...");
            System.out.println("For more info: " + TOOLSURL);
            System.out.println("CLDRFile.GEN_VERSION=" + CLDRFile.GEN_VERSION);
            System.out.println("(Use the -l option to list hidden/undocumented tools)");
            System.out.println();
            listClasses(false, null);
        } else if (args.length == 1 && args[0].equals("-l")) {
            listClasses(true, null);
        } else {
            final String mainClass = args[0];
            final String args2[] = new String[args.length - 1];
            System.arraycopy(args, 1, args2, 0, args2.length);
            Class<?> c = null;

            try {
                c = Class.forName(mainClass);
            } catch (ClassNotFoundException e) {
                // not found
            }
            if (c == null) {
                c = listClasses(false, mainClass);
            }
            if (c == null) {
                throw new IllegalArgumentException("Class not found and not an alias: " + mainClass);
            }

            if (c == Main.class) {
                throw new IllegalArgumentException("Cowardly refusing to invoke myself recursively. Stop.");
            }

            final java.lang.reflect.Method main = getStaticMain(c);

            System.err.println(">> " + c.getName());

            main.invoke(null, (Object) args2);
        }
    }

    /**
     * @param showAll if true, include even hidden classes
     * @param match if non-null, will return the class instead of listing it
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static Class<?> listClasses(boolean showAll, final String match) throws IOException, FileNotFoundException {
        final java.util.jar.JarInputStream jis = getJarInputStream();
        final ClassLoader classLoader = Main.class.getClassLoader();
        if (jis == null) {
            return null;
        }
        java.util.jar.JarEntry je;
        while ((je = jis.getNextJarEntry()) != null) {
            final String name = je.getName();
            if (inOuterClass(name)) {
                final String className = filenameToClassName(name);
                try {
                    final Class<?> c = java.lang.Class.forName(className, false, classLoader);

                    if ((match != null) && showAll == false && !c.isAnnotationPresent(CLDRTool.class))
                        continue;

                    Method method = getStaticMain(c);

                    if (method != null) { // skip classes w/o static method - even if they have an annotatoin.
                        CLDRTool annotation = c.getAnnotation(CLDRTool.class);

                        if (match == null) {
                            // list mode
                            if (showAll ||
                                (annotation != null && annotation.hidden().length() == 0)) {

                                if (annotation != null) {
                                    System.out.println("" + annotation.alias() + " - " + annotation.description());
                                    if (annotation.url().length() > 0) {
                                        System.out.println("   <" + annotation.url() + ">");
                                    } else {
                                        System.out.println("   <" + TOOLSURL + annotation.alias() + ">");
                                    }
                                    System.out.println(" = " + className);
                                    if (annotation.hidden().length() > 0) {
                                        System.out.println("   HIDDEN: " + annotation.hidden());
                                    }
                                    System.out.println();
                                } else {
                                    System.out.print("   " + className);
                                    System.out.println(" (no @CLDRTool annotation)");
                                }
                            }
                        } else {
                            if (match.equalsIgnoreCase(className)) {
                                return c; // match the classname
                            }
                            if (annotation != null &&
                                annotation.alias().length() > 0 &&
                                match.equalsIgnoreCase(annotation.alias())) {
                                return c; // match the alias
                            }
                        }
                    }
                } catch (Throwable t) {
                    // ignore uninstantiable.
                    //System.out.println(t);
                }
            }
        }
        return null; // not found or not needed.
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
        if (false) System.out.println("Classes in " + url.getPath());
        final java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(new java.io.FileInputStream(url.getPath()));
        java.util.jar.JarEntry je = null;
        return jis;
    }
}
