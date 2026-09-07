package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.RbnfData;

public class ListRbnf {
    public static void main(String[] args) {
        listRbnf();
    }

    public static void listRbnf() {
        Joiner joinTab = Joiner.on('\t');
        Joiner joinCommaSpace = Joiner.on(", ");
        Splitter splitDash = Splitter.on('-');
        System.out.println("\nLocale to type to subtype");
        Set<String> keys = RbnfData.INSTANCE.getRbnfTypeToLocales().keySet();
        System.out.println("locale\t" + joinTab.join(keys));

        System.out.println("\nType to subtype");
        Set<String> allTypes = new TreeSet<>();
        Set<String> allPieces = new TreeSet<>();
        Set<String> allSubtypes = new TreeSet<>();

        for (String locale : RbnfData.INSTANCE.getLocaleToTypesToSubtypes().keySet()) {
            Map<String, Map<String, Boolean>> typeToSubtype =
                    RbnfData.INSTANCE.getLocaleToTypesToSubtypes().getMapMap(locale);

            for (Entry<String, Map<String, Boolean>> entry : typeToSubtype.entrySet()) {
                String type = entry.getKey();
                allTypes.add(type);
                Set<String> subTypes = entry.getValue().keySet();
                allSubtypes.addAll(subTypes);
                System.out.println(joinTab.join(locale, type, joinCommaSpace.join(subTypes)));
                subTypes.stream().forEach(x -> allPieces.addAll(splitDash.splitToList(x)));
            }
            System.out.println();
        }
        System.out.println("\nAll subtypes");
        System.out.println(joinCommaSpace.join(allSubtypes));

        System.out.println("\nAll subtype pieces");
        System.out.println(joinCommaSpace.join(allPieces));

        System.out.println("\nSubtype to locale");
        for (Entry<String, Collection<String>> entry :
                RbnfData.INSTANCE.getRbnfTypeToLocales().asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + joinCommaSpace.join(entry.getValue()));
        }
    }
}
