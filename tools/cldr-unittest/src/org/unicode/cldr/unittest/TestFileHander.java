package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.ICUUncheckedIOException;

abstract public class TestFileHander {

    public static final List<String> DEBUG_LINE = Collections.singletonList("@debug");
    public static final Splitter SEMICOLON = Splitter.on(';').trimResults();
    public static final String SEPARATOR = " ; \t";

    protected TestFmwk framework = null;
    protected int minArgumentCount = 3;
    protected int maxArgumentCount = 4;
    private List<List<String>> lines = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    public TestFileHander setFramework(TestFmwk testFramework) {
        this.framework = testFramework;
        return this;
    }

    public <T extends Appendable> T appendLines(T out) {
        try {
            for (int i = 0; i < lines.size(); ++i) {
                List<String> components = lines.get(i);
                String comment = comments.get(i);
                if (components.isEmpty()) {
                    if(!comment.isEmpty()) {
                        out.append("# ").append(comment);
                    }
                } else {
                    out.append(CollectionUtilities.join(components, SEPARATOR));
                    if (!comment.isEmpty()) {
                        out.append("\t# ").append(comment);
                    }
                }
                out.append('\n');
            }
            return out;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    protected TestFileHander addLine(List<String> arguments, String commentBase) {
        lines.add(Collections.unmodifiableList(arguments));
        comments.add(commentBase);
        return this;
    }
    
    public TestFileHander run(Class<?> classFileIsRelativeTo, String file) {
        return load(classFileIsRelativeTo, file)
            .test();
    }

    public boolean isTestLine(List<String> arguments) {
        return !arguments.isEmpty() && !arguments.equals(DEBUG_LINE);
    }
    
    public TestFileHander test() {
        boolean breakpoint = false;
        for (int i = 0; i < lines.size(); ++i) {
            List<String> arguments = lines.get(i);
            String comment = comments.get(i);
            if (arguments.isEmpty()) {
                if (!comment.isEmpty()) {
                    framework.logln(comment);
                }
                continue;
            } else if (arguments.equals(DEBUG_LINE)) {
                breakpoint = true;
                continue;
            }
            try {
                handle(breakpoint, comment, arguments);
            } catch (Exception e) {
                framework.errln("Illegal data test file entry: " + arguments + " # " + comment);
            }
            breakpoint = false;
        }
        return this;
    }

    public TestFileHander load(Class<?> classFileIsRelativeTo, String file) {
        try (BufferedReader in = FileUtilities.openFile(classFileIsRelativeTo, file)) {
            boolean breakpoint = false;

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    addLine(Collections.emptyList(), "");
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
                    addLine(Collections.emptyList(), commentBase);
                    continue;
                }
                if (line.startsWith("@debug")) {
                    addLine(DEBUG_LINE, "");
                    continue;
                }
                List<String> arguments = SEMICOLON.splitToList(line);
                if (arguments.size() < minArgumentCount || arguments.size() > maxArgumentCount) {
                    framework.errln("Malformed data line:" + line + comment);
                    continue;
                }
                addLine(arguments, commentBase);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        lines = Collections.unmodifiableList(lines); // should do deep unmodifiable...
        comments = Collections.unmodifiableList(comments);
        return this;
    }

    protected boolean assertEquals(String message, Object expected, Object actual) {
        return framework.handleAssert(Objects.equal(expected, actual), message, stringFor(expected), stringFor(actual), null, false);
    }

    private final String stringFor(Object obj) {
        return obj == null ? "null" 
            : obj instanceof String ? "\"" + obj + '"' 
                : obj instanceof Number ? String.valueOf(obj)
                    : obj.getClass().getName() + "<" + obj + ">";
    }

    abstract public void handle(boolean breakpoint, String commentBase, List<String> arguments);

    public List<List<String>> getLines() {
        return lines;
    }
}