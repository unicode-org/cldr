package org.unicode.cldr.unittest;

import java.io.IOException;
import org.unicode.cldr.icu.dev.test.TestFmwk;

public class TestIcuPersonNames extends TestFmwk {

    /**
     * Notes for API 1. Options should be Option (singular for enums) 2. All classes should have
     * toString(). Otherwise hard to debug. 3. The code has the following in public
     * PersonNameFormatterImpl(Locale locale, … Set<PersonNameFormatter.Options> options) // asjust
     * for combinations of parameters that don't make sense in practice
     * options.remove(PersonNameFormatter.Options.SORTING); That breaks if the input options are
     * immutable. It should instead not try to modify input parameters, instead use: options = new
     * HashSet<>(options); // or enum set options.remove(PersonNameFormatter.Options.SORTING); 4. It
     * would be useful for testing to have an @internal method to override the order with givenFirst
     * or surnameFirst 5. No enum constant com.ibm.icu.text.PersonName.FieldModifier.informal
     */
    public static void main(String[] args) throws IOException {
        new TestIcuPersonNames().run(args);
    }

    public void testCldrTestData() throws IOException {
        CheckPersonNamesTest.check(this);
    }
}
