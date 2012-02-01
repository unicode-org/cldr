package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.LDMLConverter.MultiFileOutput;
import org.unicode.cldr.draft.LDMLConverter.RegexResult;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.With;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UForwardCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Analyse the current mapping to ICU resource bundles so that it is easy to make the regexes for LDMLConverter.
 * @author markdavis
 *
 */
public class RBChecker {
    private static final boolean DEBUG = false;
    static Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");

    public static void main(String[] args) throws IOException {
        for (String locale : args){
            checkExistingRB(locale);
        }
    }

    static void checkExistingRB(String locale) throws IOException {
        MultiFileOutput output = new MultiFileOutput();
        List<Row.R2<MyTokenizer.Type, String>> comments = new ArrayList<Row.R2<MyTokenizer.Type, String>>();
        List<Row.R2<MyTokenizer.Type, String>> comments2 = comments;
        for (String dir : With.array("locales", "lang", "region", "curr", "zone")) {
            String source = "/Users/markdavis/Documents/workspace/icu/source/data/" + dir + "/" + locale + ".txt";
            parseRB(source, output, comments);
            comments2 = null; // only record first comments
        }
        // for (Entry<String, List<String>> entry : output.entrySet()) {
        // System.out.println(entry);
        // }
        analyseMatches(locale, output);
        output.writeRB(CldrUtility.TMP_DIRECTORY + "dropbox/mark/converter/rb-formatted/", locale);
    }

    private static void writeComments(List<R2<MyTokenizer.Type, String>> comments, Appendable output) throws IOException {
        for (R2<MyTokenizer.Type, String> entity : comments) {
            switch (entity.get0()) {
            case LINE_COMMENT:
                output.append("// ").append(entity.get1()).append('\n');
                break;
            case BLOCK_COMMENT:
                output.append("/*").append(entity.get1()).append("*/\n");
                break;
            }
        }
    }

    static void analyseMatches(String locale, MultiFileOutput output) {
        // TBD change to HashMap for speed later
        Relation<String,String> rbValue2paths = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class);
        Relation<String,String> cldrValue2paths = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class);
        for (Entry<String, Map<String, List<String>>> entry : output.entrySet()) {
            String dir = entry.getKey();
            for (Entry<String, List<String>> pathAndValues : entry.getValue().entrySet()) {
                String path = pathAndValues.getKey();
                for (String value : pathAndValues.getValue()) {
                    rbValue2paths.put(value, path);
                }
            }
        }
        CLDRFile cldrResolved = factory.make(locale, true);
        
        MultiFileOutput cldrOutput = new MultiFileOutput();
        cldrOutput.fillFromCLDR(factory, locale, null);
        
        for (String path : cldrOutput.getCldrPaths()) {
            String value = cldrResolved.getStringValue(path);
            cldrValue2paths.put(value, path);
        }
        
        Set<String> NONE = new HashSet<String>();
        NONE.add("NONE");
        NONE = Collections.unmodifiableSet(NONE);
        
        // now show matches
        int counter = 0;
        Relation<String,R4<Integer, Set<String>, Set<String>, Set<String>>> sorted 
        = Relation.of(new TreeMap<String,Set<R4<Integer, Set<String>, Set<String>, Set<String>>>>(), TreeSet.class);
        Set<String> cldrPathsGenerated = new HashSet<String>();

        for (Entry<String, Set<String>> entry : rbValue2paths.keyValuesSet()) {
            String value = entry.getKey();
            Set<String> paths = entry.getValue();

            Set<String> cldrPaths = cldrValue2paths.get(value);
            if (cldrPaths == null) {
                cldrPaths = NONE;
            }

            // get generated
            Set<String> generated = new TreeSet<String>();
            Output<String[]> arguments = new Output();
            for (String path : cldrPaths) {
                RegexResult regexResult = LDMLConverter.pathConverter.get(path, null, arguments);
                if (regexResult == null) {
                    continue;
                }
                String rbPath = "/" + locale + regexResult.getRbPath(arguments.value);
                generated.add(rbPath);
                cldrPathsGenerated.add(path);
            }

            final R4<Integer, Set<String>, Set<String>, Set<String>> row = Row.of(counter++, paths, cldrPaths, generated);
            sorted.put(paths.iterator().next(), row);
        }
        int errors = 0;
        for (Entry<String, R4<Integer, Set<String>, Set<String>, Set<String>>> entry : sorted.entrySet()) {
            final R4<Integer, Set<String>, Set<String>, Set<String>> row = entry.getValue();
            Set<String> cldrPaths = row.get2();
            Set<String> paths = row.get1();
            Set<String> generated = row.get3();
            Set<String> missing = new TreeSet<String>(paths);
            missing.removeAll(generated);
            Set<String> same = new TreeSet<String>(paths);
            same.retainAll(generated);
            Set<String> extra = new TreeSet<String>(generated);
            extra.removeAll(paths);

            int newErrors = missing.size() + extra.size();
            if (newErrors == 0) {
                continue;
            }
            errors += newErrors;
            for (String cldrPath : cldrPaths) {
                System.out.println(cldrPath.equals("NONE") ? "NONE" : cldrResolved.getFullXPath(cldrPath));
            }
            //System.out.println(CollectionUtilities.join(cldrPaths, "\n"));
            if (missing.size() != 0) {
                System.out.println("\n\tmissing:\t" + CollectionUtilities.join(missing, "\n\tmissing:\t"));
            }
            if (same.size() != 0) {
                System.out.println("\n\tsame:\t" + CollectionUtilities.join(same, "\n\tsame:\t"));
            }
            if (extra.size() != 0) {
                System.out.println("\n\textra:\t" + CollectionUtilities.join(extra, "\n\textra:\t"));
            }
            System.out.println("\n");
        }
        CLDRFile cldr = factory.make(locale, false);

        TreeSet<String> missingCldr = Builder.with(new TreeSet<String>(CLDRFile.ldmlComparator)).addAll(cldr).removeAll(cldrPathsGenerated).get();
        for (String s : missingCldr) {
            System.out.println("Missing CLDR:\t" + s);
        }
        System.out.println("Errors:\t" + errors);
    }


    /**
     * Parse an ICU resource bundle into key,value items
     * 
     * @param filename
     * @param output
     * @param comments
     */
    static void parseRB(String filename, MultiFileOutput output, List<R2<MyTokenizer.Type, String>> comments) {
        BufferedReader in = null;
        try {
            File file = new File(filename);
            String coreFile = file.getName();
            if (!coreFile.endsWith(".txt")) {
                throw new IllegalArgumentException("missing .txt in: " + filename);
            }
            coreFile = coreFile.substring(0, coreFile.length()-4);
            File parent = file.getParentFile();
            String directory = parent.getName();
            // redo this later on to use fixed PatternTokenizer
            in = BagFormatter.openUTF8Reader("", filename);
            MyTokenizer tokenIterator = new MyTokenizer(in);
            StringBuffer tokenText = new StringBuffer();
            List<String> oldPaths = new ArrayList<String>();
            String lastLabel = null;
            String path = "";
            boolean afterCurly = true;
            /*
             * AuxExemplarCharacters{
             * "[á à ă â å ä ã ā æ ç é è ĕ ê ë ē í ì ĭ î ï ī ñ ó ò ŏ ô ö ø ō œ ú ù ŭ û ü ū ÿ"
             * "]" } ExemplarCharacters{
             * "[a b c d e f g h i j k l m n o p q r s t u v w x y z]"}
             * ExemplarCharactersCurrency
             * {"[a b c č d e f g h i j k l ł m n o º p q r s t u v w x y z]"}
             * ExemplarCharactersIndex
             * {"[A B C D E F G H I J K L M N O P Q R S T U V W X Y Z]"}
             * ExemplarCharactersPunctuation{"[\- ‐ – — , ; \: ! ? . … ' ‘ ’ \"
             * “ ” ( ) \[ \] @ * / \& # † ‡ ′ ″ §]"}
             */
            main: while (true) {
                MyTokenizer.Type nextToken = tokenIterator.next(tokenText);
                if (DEBUG)
                    System.out.println(nextToken + "\t" + (nextToken == MyTokenizer.Type.SPACE ? "" : tokenText));
                switch (nextToken) {
                case SPACE: // ignore
                    break;
                case BLOCK_COMMENT:
                case LINE_COMMENT:
                    if (comments != null) {
                        comments.add(Row.of(nextToken, tokenText.toString()));
                    }
                    break;
                case DONE:
                    if (oldPaths.size() != 0) {
                        throw new IllegalArgumentException("missing }");
                    }
                    break main;
                case ID:
                    lastLabel = lastLabel == null ? tokenText.toString() : lastLabel + tokenText;
                    afterCurly = false;
                    break;
                case QUOTE:
                    lastLabel = lastLabel == null ? tokenText.toString() : lastLabel + tokenText;
                    afterCurly = false;
                    break;
                case SYNTAX:
                    int ch = tokenText.codePointAt(0);
                    if (ch == '{') {
                        if (afterCurly) {
                            throw new IllegalArgumentException("{{");
                        }
                        if (DEBUG)
                            System.out.println("PUSH:\t" + path);
                        oldPaths.add(path);
                        path = path + "/" + lastLabel;
                        lastLabel = null;
                        afterCurly = true;
                    } else if (ch == '}') {
                        if (lastLabel != null) {
                            output.add(directory, null, path, lastLabel);
                            lastLabel = null;
                        }
                        path = oldPaths.remove(oldPaths.size() - 1);
                        if (DEBUG)
                            System.out.println("POP:\t" + path);
                    } else if (ch == ',') {
                        if (lastLabel != null) {
                            output.add(directory, null, path, lastLabel);
                            lastLabel = null;
                        }
                    } else {
                        throw new IllegalArgumentException("Illegal character: " + tokenText + "\t" + Utility.hex(tokenText));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Illegal type: " + nextToken + "\t" + tokenText + "\t" + Utility.hex(tokenText));
                }
            }
            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    /**
     * Replace by updated PatternTokenizer someday
     * @author markdavis
     *
     */
    static class MyTokenizer {
        enum Type {DONE, ID, QUOTE, SYNTAX, SPACE, LINE_COMMENT, BLOCK_COMMENT, BROKEN_QUOTE, BROKEN_BLOCK_COMMENT, UNKNOWN}

        private final UForwardCharacterIterator source;
        private final UnicodeSet syntaxCharacters = new UnicodeSet("[:pattern_syntax:]");
        private final UnicodeSet spaceCharacters = new UnicodeSet("[\\u0000\\uFEFF[:pattern_whitespace:]]");
        private final UnicodeSet idCharacters = new UnicodeSet(LDMLConverter.ID_CHARACTERS);
        private final UnicodeSet quoteCharacters = new UnicodeSet("[\"']");

        private int bufferedChar;

        /**
         * @param reader
         */
        public MyTokenizer(Reader reader) {
            this.source = new UReaderForwardCharacterIterator(reader);
        }

        public Type next(StringBuffer tokenText) {
            int cp = getCodePoint();
            if (cp == -1) {
                return Type.DONE;
            }
            tokenText.setLength(0);
            if (cp == '/') {
                int oldCP = cp;
                cp = getCodePoint();
                if (cp == '/') { // line comment
                    while (true) {
                        cp = getCodePoint();
                        if (cp == '\n' || cp < 0) {
                            return Type.LINE_COMMENT;
                        }
                        tokenText.appendCodePoint(cp);
                    }
                } else if (cp == '*') { // block comment
                    while (true) {
                        cp = getCodePoint();
                        if (cp < 0) {
                            return Type.BROKEN_BLOCK_COMMENT;
                        }
                        while (cp == '*') {
                            int cp2 = getCodePoint();
                            if (cp2 < 0) {
                                return Type.BROKEN_BLOCK_COMMENT;
                            } else if (cp2 == '/') {
                                return Type.BLOCK_COMMENT;
                            }
                            tokenText.appendCodePoint(cp);
                            cp = cp2;
                        }
                        tokenText.appendCodePoint(cp);
                    }
                }
                pushCodePoint(cp);
                cp = oldCP;
            }
            if (quoteCharacters.contains(cp)) {
                int oldQuote = cp;
                while (true) {
                    cp = getCodePoint();
                    if (cp < 0) {
                        return Type.BROKEN_QUOTE;
                    } else if (cp == oldQuote) {
                        break;
                    } else if (cp == '\\') {
                        cp = getCodePoint();
                        if (cp < 0) {
                            tokenText.appendCodePoint(cp);
                            return Type.BROKEN_QUOTE;
                        } else if (cp != '"' && cp != '\\') {
                            tokenText.appendCodePoint('\\');
                        }
                    }
                    tokenText.appendCodePoint(cp);
                };
                return Type.QUOTE;
            }
            if (spaceCharacters.contains(cp)) {
                while (true) {
                    tokenText.appendCodePoint(cp);
                    cp = getCodePoint();
                    if (cp < 0 || !spaceCharacters.contains(cp)) {
                        pushCodePoint(cp);
                        return Type.SPACE;
                    }
                }
            }
            if (syntaxCharacters.contains(cp)) {
                tokenText.appendCodePoint(cp);
                return Type.SYNTAX;
            }
            if (idCharacters.contains(cp)) {
                while (true) {
                    tokenText.appendCodePoint(cp);
                    cp = getCodePoint();
                    if (cp < 0 || !idCharacters.contains(cp)) {
                        pushCodePoint(cp);
                        return Type.ID;
                    }
                }
            }
            tokenText.appendCodePoint(cp);
            return Type.UNKNOWN;
        }

        int getCodePoint() {
            if (bufferedChar >= 0) {
                int result = bufferedChar;
                bufferedChar = -1;
                return result;
            }
            return source.nextCodePoint();
        }
        void pushCodePoint(int codepoint) {
            if (bufferedChar >= 0) {
                throw new IllegalArgumentException("Cannot push twice");
            }
            bufferedChar = codepoint;
        }
    }

    public static class UReaderForwardCharacterIterator implements UForwardCharacterIterator {
        private Reader reader;
        private int bufferedChar = -1;

        /**
         * @param reader
         */
        public UReaderForwardCharacterIterator(Reader reader) {
            this.reader = reader;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.UForwardCharacterIterator#next()
         */
        public int next() {
            if (bufferedChar >= 0) {
                int temp = bufferedChar;
                bufferedChar = -1;
                return temp;
            }
            try {
                return reader.read();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.UForwardCharacterIterator#nextCodePoint()
         */
        public int nextCodePoint(){
            int ch1 = next();
            if(UTF16.isLeadSurrogate((char)ch1)){
                int bufferedChar = next();
                if(UTF16.isTrailSurrogate((char)bufferedChar)){
                    return UCharacterProperty.getRawSupplementary((char)ch1,
                            (char)bufferedChar);
                }
            }
            return ch1;
        }
    }
}
