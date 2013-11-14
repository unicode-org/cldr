package org.unicode.cldr.tool;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class GenerateUnicodeCounts {
    public static void main(String[] args) {
        UnicodeMap<VersionInfo> ages = new UnicodeMap<VersionInfo>();
        UnicodeMap<String> scripts = new UnicodeMap<String>();
        UnicodeSet us = new UnicodeSet("[[:cn:][:cs:][:co:]]").complement();
        for (String s : us) {
            int i = s.codePointAt(0);
            VersionInfo age = UCharacter.getAge(i);
            ages.put(i, age);
            int script = UScript.getScript(i);
            scripts.put(i, UScript.getName(script).replace('_', ' '));
        }
        for (VersionInfo f : ages.getAvailableValues()) {
            UnicodeSet s = ages.getSet(f);
            System.out.println(f.getMajor() + "." + f.getMinor() + "\t" + s.size());
        }
        for (String f : scripts.getAvailableValues()) {
            UnicodeSet s = scripts.getSet(f);
            System.out.println(f + "\t" + s.size());
        }
    }
}
