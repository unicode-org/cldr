package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ICUUncheckedIOException;

abstract public class TestFileHander {
    
    public static final Splitter SEMICOLON = Splitter.on(';').trimResults();
    
    protected TestFmwk framework = null;
    protected Appendable out = null;
    
    public TestFileHander setPrinter(Appendable reformat) {
        this.out = reformat;
        return this;
    }

    public TestFileHander setFramework(TestFmwk testFramework) {
        this.framework = testFramework;
        return this;
    }
    
    public boolean isPrinting() {
        return out != null;
    }

    public void print(String s) {
        try {
            out.append(s);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
    
    public void println(String s) {
        try {
            out.append(s).append('\n');
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
    
    public void println() {
        println("");
    }
    
    public void run(Class<XLocaleMatcherTest> classFileIsRelativeTo, String file) {
        try (BufferedReader in = FileUtilities.openFile(classFileIsRelativeTo, file)) {
            if (isPrinting()) {
                println();
            }
            boolean breakpoint = false;

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    if (isPrinting()) {
                        println();
                    }
                    continue;
                }
                int hash = line.indexOf('#');
                String comment = "";
                String commentBase = "";
                if (hash >= 0) {
                    commentBase = line.substring(hash+1).trim();
                    line = line.substring(0,hash).trim();
                    comment = "# " + commentBase;
                    if (!line.isEmpty()) {
                        comment = "\t" + comment;
                    }
                }
                if (line.isEmpty()) {
                    if (isPrinting()) {
                        if (commentBase.startsWith("test")) {
                            println("##################################################\n");                        
                        }
                        println(comment + "\n");
                    } else {
                        framework.logln(comment);
                    }
                    continue;
                }
                if (line.startsWith("@debug")) {
                    if (isPrinting()) {
                        println("@debug" + comment + "\n");
                    }
                    breakpoint = true;
                    continue;
                }
                List<String> arguments = SEMICOLON.splitToList(line);
                if (arguments.size() < 3 || arguments.size() > 4) {
                    framework.errln("Malformed data line:" + line + comment);
                    continue;
                }
                breakpoint = handle(breakpoint, commentBase, arguments);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
    protected boolean assertEquals(String message, Object expected, Object actual) {
        return framework.handleAssert(Objects.equal(expected, actual), message, stringFor(expected), stringFor(actual), null, false);
    }
    
    private final String stringFor(Object obj) {
        return obj == null ? "null" 
            : obj instanceof String ? "\"" + obj + '"' 
                : obj.getClass().getName() + "<" + obj + ">";
    }
    
    abstract public boolean handle(boolean breakpoint, String commentBase, List<String> arguments);
}