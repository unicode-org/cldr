package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This list needs updating as a new organizations are added; that's by design
 * so that we know when new ones show up.
 */
public enum Organization {
    // Please update Locales.txt for default coverage when adding an organization here.

    adlam("Winden Jangen Adlam"), adobe("Adobe"), afghan_csa("Afghan CSA"), afghan_mcit("Afghan MCIT"), afrigen("Afrigen"), apple("Apple"), bangladesh(
        "Bangladesh Computer Council"), bangor_univ("Bangor Univ."), bhutan("Bhutan DDC"), breton("Office of Breton Lang"), cherokee("Cherokee Nation"), cldr(
            "Cldr"), facebook("Facebook"), gaeilge("Foras na Gaeilge"), georgia_isi("Georgia ISI"), gnome("Gnome Foundation"), google(
                "Google"), guest("Guest (Unicode)"), ibm("IBM"), india("India MIT"), iran_hci("Iran HCI"), kendra("Kendra (Nepal)"), kotoistus(
                    "Kotoistus (Finnish IT Ctr)"), lakota_lc("Lakota LC"), lao_dpt("Lao Posts/Telecom??"), longnow("The Long Now Foundation", "Long Now",
                        "PanLex"), microsoft("Microsoft"), mozilla("Mozilla"), netflix("Netflix"), openinstitute("Open Inst (Cambodia)"), openoffice_org(
                            "Open Office"), oracle("Oracle", "sun", "Sun Micro"), pakistan("Pakistan"), rumantscha("Lia Rumantscha"), sil("SIL"), srilanka(
                                "Sri Lanka ICTA",
                                "Sri Lanka"), surveytool("Survey Tool"), welsh_lc("Welsh LC"), wikimedia("Wikimedia Foundation"), yahoo("Yahoo"),
    // To be removed.
    ;

    public final String displayName;
    private final String[] names;

    public static Organization fromString(String name) {
        name = name.toLowerCase().replace('-', '_').replace('.', '_');
        Organization org = OrganizationNameMap.get(name);
        return org;
    }

    public String getDisplayName() {
        return displayName;
    }

    static Map<String, Organization> OrganizationNameMap;
    static {
        OrganizationNameMap = new HashMap<String, Organization>();
        for (Organization x : values()) {
            OrganizationNameMap.put(x.displayName.toLowerCase().replace('-', '_').replace('.', '_'), x);
            for (String name : x.names) {
                OrganizationNameMap.put(name.toLowerCase().replace('-', '_').replace('.', '_'), x);
            }
            OrganizationNameMap.put(x.name().toLowerCase().replace('-', '_').replace('.', '_'), x);
        }
    }

    private Organization(String displayName, String... names) {
        this.displayName = displayName;
        this.names = names;
    }
}
