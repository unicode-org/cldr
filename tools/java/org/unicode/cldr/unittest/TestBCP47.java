package org.unicode.cldr.unittest;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class TestBCP47 extends TestFmwk {
    static TestAll.TestInfo testInfo = TestAll.TestInfo.getInstance();
    public static void main(String[] args) {
        new TestBCP47().run(args);
    }

    public void TestEnglishKeyTranslations() {
        checkEnglishTranslations(true);
    }
    
    public void TestEnglishTypeTranslations() {
        checkEnglishTranslations(false);
    }

    public void checkEnglishTranslations(boolean keysVsTypes) {
        //        for (Iterator it = testInfo.getEnglish().iterator("//ldml/localeDisplayNames/types"); it.hasNext();) {
        //            System.out.println(it.next());
        //        }
        Relation<String, String> bcp47key_types = testInfo.getSupplementalDataInfo().getBcp47Keys();
        Relation<R2<String, String>, String> bcp47keyType_aliases = testInfo.getSupplementalDataInfo().getBcp47Aliases();

        Set<String> keys = new LinkedHashSet<String>();
        Set<String> types = new LinkedHashSet<String>();

        for (String key : bcp47key_types.keySet()) {

            setToItems(keys, key, bcp47keyType_aliases.get(Row.of(key,"")));
            String keyTranslation = getKeyTranslation(keys);
            if (keyTranslation == null) {
                if (keysVsTypes) errln("Translation for " + key + ":\t" + keyTranslation);
                keyTranslation = key;
            } else {
                if (keysVsTypes) logln("OK Translation for " + key + ":\t" + keyTranslation);
            }
            if (!keysVsTypes) {
                for (String subtype : bcp47key_types.get(key)) {
                    setToItems(types, subtype, bcp47keyType_aliases.get(Row.of(key,subtype)));
                    String typeTranslation = getTypeTranslation(keys, types);
                    if (typeTranslation == null) {
                        if (key.equals("cu") || key.equals("vt")) {
                            typeTranslation = subtype.toUpperCase();
                        } else if (key.equals("tz")) {
                            Iterator<String> it = types.iterator();
                            it.next();
                            typeTranslation = it.next();
                        }
                    }
                    if (typeTranslation == null) {
                        errln("Translation for " + key + "=" + subtype + ":\t" + keyTranslation + "=" + subtype);
                    } else {
                        logln("OK Translation for " + key + "=" + subtype + ":\t" + typeTranslation);
                    }
                }
            }
        }
    }

    private void setToItems(Set<String> keys, String key, Set<String> set) {
        keys.clear();
        keys.add(key);
        if (set != null) {
            keys.addAll(set);
        }
    }

    private String getKeyTranslation(Set<String> keys) {
        for (String key : keys) {
            String value = getKeyTranslation(key);
            if (value != null) return value;
        }
        return null;
    }

    private String getTypeTranslation(Set<String> keys, Set<String> types) {
        for (String key : keys) {
            for (String type : types) {
                String value = getTypeTranslation(key, type);
                if (value != null) return value;
            }
        }
        return null;
    }

    private String getKeyTranslation(String key) {
        return testInfo.getEnglish().getStringValue("//ldml/localeDisplayNames/keys/key[@type=\"" + key + "\"]");
    }

    private String getTypeTranslation(String key, String type) {
        return testInfo.getEnglish().getStringValue("//ldml/localeDisplayNames/types/type[@type=\"" + type + "\"][@key=\"" + key + "\"]");
    }

}
