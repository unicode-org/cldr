package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import java.lang.Class;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;


/**
 *
 * Implement a 'main' for the CLDR jar.
 */
class Main {
    public static void main(String args[]) throws Throwable {
        if(args.length==0) {
            System.out.println("Usage:  [ -l | class args ...]");
            System.out.println("Example usage:");
            System.out.println(" (java -jar cldr.jar ) -l          -- prints a list of tool/util/test classes with a 'main()' function.");
            System.out.println(" (java -jar cldr.jar ) org.unicode.cldr.util.XMLValidator  somefile.xml ...");
            System.out.println("For more info: http://unicode.org/cldr");
            System.out.println("CLDRFile.GEN_VERSION="+CLDRFile.GEN_VERSION);
        } else if(args.length==1 && args[0].equals("-l")) {
            final java.net.URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
            System.out.println("Classes in " + url.getPath());
            final java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(new java.io.FileInputStream(url.getPath()));
            java.util.jar.JarEntry je = null;
            while((je=jis.getNextJarEntry())!=null) {
                final String name = je.getName();
                if(name.endsWith(".class") &&
                   !name.contains("$") &&
                   (name.startsWith("org/unicode/cldr/tool") ||
                    name.startsWith("org/unicode/cldr/util") ||
                    name.startsWith("org/unicode/cldr/test")))  {
                    final String className = name.substring(0, name.length()-(".class".length()))
                        .replaceAll("/",".");
                    try {
                        final java.lang.Class c = java.lang.Class.forName(className);
                        if(c.getMethod("main", String[].class)!=null) {
                            System.out.println(" " + className);
                        }
                    } catch(Throwable t) {
                        // ignore uninstantiable.
                        //System.out.println(t);
                    }
                }
            }
        } else {
            final String mainClass = args[0];
            final String args2[] = new String[args.length - 1];
            System.arraycopy(args,1,args2,0,args2.length);
            Class c = Class.forName(mainClass);

            if(c == Main.class) {
                throw new IllegalArgumentException("Cowardly refusing to invoke myself recursively. Stop.");
            }

            final java.lang.reflect.Method main = c.getMethod("main", String[].class);

            main.invoke(null, (Object)args2);
        }
    }
}
