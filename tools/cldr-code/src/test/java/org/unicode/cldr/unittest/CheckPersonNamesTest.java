package org.unicode.cldr.unittest;

import com.google.common.base.Splitter;
import com.ibm.icu.text.PersonName.FieldModifier;
import com.ibm.icu.text.PersonName.NameField;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.text.PersonNameFormatter.DisplayOrder;
import com.ibm.icu.text.PersonNameFormatter.Formality;
import com.ibm.icu.text.PersonNameFormatter.Length;
import com.ibm.icu.text.PersonNameFormatter.Usage;
import com.ibm.icu.text.SimplePersonName;
import com.ibm.icu.text.SimplePersonName.Builder;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.unicode.cldr.icu.dev.test.AbstractTestLog;
import org.unicode.cldr.util.CLDRPaths;

public class CheckPersonNamesTest {
    /**
     * Old notes for API (in ICU 72), not sure which are still relevant in ICU 73 1. .... 2. All
     * classes should have toString(). Otherwise hard to debug. 3. ... 4. It would be useful for
     * testing to have an @internal method to override the order with givenFirst or surnameFirst 5.
     * No enum constant com.ibm.icu.text.PersonName.FieldModifier.informal 6. It appears that ICU
     * isn't handling the SORTING option properly, so the test is skipping all but 1 of those per
     * locale.
     */
    private static final Splitter DASH_SPLITTER = Splitter.on('-');

    private static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();

    public static void main(String[] args) throws IOException {
        AbstractTestLog logger =
                new AbstractTestLog() {

                    @Override
                    public void msg(String message, int level, boolean incCount, boolean newln) {
                        System.out.println("Error " + message);
                    }
                };
        check(logger);
    }

    static void check(AbstractTestLog logger) throws IOException {
        // just check a single file for now
        Path path = Paths.get(CLDRPaths.TEST_DATA, "personNameTest");
        Files.list(path)
                .forEach(
                        file -> {
                            try {
                                Path fileName = file.getFileName();
                                String fileNameStr = fileName.toString();
                                if (fileNameStr.endsWith(".txt")) {
                                    LineHandler handler = new LineHandler(fileNameStr, logger);
                                    Files.lines(file).forEach(handler::handleLine);
                                }
                            } catch (IOException e) {
                                logger.errln("Failure with " + file + "\t" + e);
                            }
                        });
    }

    static class LineHandler {
        final Locale locale;
        SimplePersonName personName;
        String expectedResult;
        Builder personNameBuilder = SimplePersonName.builder();
        NameField nameField = null;
        AbstractTestLog testIcuPersonNames;
        boolean skipSameExpectedValue = false;
        boolean skipAllSortingErrors = false;

        public LineHandler(String fileNameStr, AbstractTestLog testIcuPersonNames) {
            String localeStr =
                    fileNameStr.substring(0, fileNameStr.length() - 4); // remove suffix .txt
            locale = new Locale(localeStr);
            this.testIcuPersonNames = testIcuPersonNames;
        }

        void handleLine(String line) {
            if (line.isBlank() || line.startsWith("#")) {
                return;
            }
            DisplayOrder orderOption = DisplayOrder.SORTING;

            Iterator<String> fields = SEMI_SPLITTER.split(line).iterator();

            switch (fields.next()) {
                case "enum":
                    // TODO
                    String type = fields.next();
                    List<String> values = COMMA_SPLITTER.splitToList(fields.next());
                    break;

                case "name":
                    // # name ; <field> ; <value>
                    Set<FieldModifier> modifiers = new LinkedHashSet<>();

                    String field = fields.next();
                    String fieldValue = fields.next();

                    if (field.equals("locale")) {
                        personNameBuilder.setLocale(new ULocale(fieldValue).toLocale());
                    } else {
                        nameField = null;
                        DASH_SPLITTER
                                .split(field)
                                .forEach(
                                        fieldPart -> {
                                            if (nameField == null) { // handle first one specially
                                                nameField =
                                                        NameField.valueOf(
                                                                fieldPart.toUpperCase(Locale.ROOT));
                                            } else {
                                                modifiers.add(
                                                        FieldModifier.valueOf(
                                                                fieldPart.toUpperCase(
                                                                        Locale.ROOT)));
                                            }
                                        });
                        personNameBuilder.addField(nameField, modifiers, fieldValue);
                    }
                    break;

                case "expectedResult":
                    // # expectedResult; <value>
                    expectedResult = fields.next();
                    personName = personNameBuilder.build();
                    skipSameExpectedValue = false; // suppress duplicate errors
                    break;

                case "parameters":
                    // # parameters; <order>; <length>; <usage>; <formality>
                    // Handle order specially
                    String order = fields.next();
                    switch (order) {
                        case "sorting":
                            orderOption = DisplayOrder.SORTING;
                            break;
                        case "n/a":
                            orderOption = DisplayOrder.DEFAULT;
                    }

                    Length length = Length.valueOf(fields.next().toUpperCase(Locale.ROOT));
                    Usage usage = Usage.valueOf(fields.next().toUpperCase(Locale.ROOT));
                    Formality formality = Formality.valueOf(fields.next().toUpperCase(Locale.ROOT));

                    PersonNameFormatter formatter =
                            PersonNameFormatter.builder()
                                    .setLocale(locale)
                                    .setDisplayOrder(
                                            orderOption) // HACKed above because PNF requires
                                    // mutability
                                    .setLength(length)
                                    .setUsage(usage)
                                    .setFormality(formality)
                                    .build();

                    String actual = formatter.formatToString(personName);

                    if (!skipSameExpectedValue
                            && !(skipAllSortingErrors && orderOption == DisplayOrder.SORTING)
                            && !Objects.equals(expectedResult, actual)) {
                        testIcuPersonNames.errln(
                                locale
                                        + ", "
                                        + personName.getNameLocale()
                                        + ", "
                                        + orderOption
                                        + ", "
                                        + length
                                        + ", "
                                        + usage
                                        + ", "
                                        + formality
                                        + ": expected: \""
                                        + expectedResult
                                        + "\" actual: \""
                                        + actual
                                        + "\"");
                        skipSameExpectedValue = true;
                        if (orderOption == DisplayOrder.SORTING) {
                            skipAllSortingErrors = true;
                        }
                    }
                    break;

                case "endName":
                    // get ready for the next name
                    personNameBuilder = SimplePersonName.builder();
                    break;
            }
            if (fields.hasNext()) {
                String nextValue = fields.next();
                testIcuPersonNames.errln(
                        "handled all fields: expected: \"\" actual: \"" + nextValue + "\"");
            }
        }
    }
}
