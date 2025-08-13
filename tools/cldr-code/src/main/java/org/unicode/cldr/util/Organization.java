package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This list needs updating as a new organizations are added; that's by design so that we know when
 * new ones show up.
 */
public enum Organization {
    // Please update Locales.txt for default coverage when adding an organization here.
    adlam("Winden Jangen Adlam"),
    adobe("Adobe"),
    afghan_csa("Afghan CSA"),
    afghan_mcit("Afghan MCIT"),
    afrigen("Afrigen"),
    airbnb("Airbnb"),
    anii("Anii Research Group"),
    apple("Apple"),
    bangladesh("Bangladesh", "Bangladesh Computer Council"),
    bangor_univ("Bangor Univ."),
    bhutan("Bhutan DDC"),
    breton("Office of Breton Lang"),
    cherokee("Cherokee Nation"),
    choctaw("Choctaw Nation"),
    chuvash_sih(
            "Chuvash SIH",
            "Chuvash State Institute of Humanities",
            "Чăваш патшалăх гуманитарии ăслăлăхсен институчĕ"),
    cihan_uni("Cihan University-Erbil"),
    cldr("Cldr"),
    gaeilge("Foras na Gaeilge"),
    georgia_isi("Georgia ISI"),
    gnome("Gnome Foundation"),
    google("Google"),
    ibm("IBM"),
    ihll_ufrc(
            "IHLL UFRC",
            "Institute of History, Language and Literature at Ufa Federal Research Center"),
    india("India MIT"),
    iran_hci("Iran HCI"),
    kazakh_tbi(
            "Kazakh TBI",
            "The Institute of Linguistics named after Akhmet Baitursunuly",
            "Ахмет Байтұрсынұлы атындағы Тіл білімі институты"),
    kendra("Kendra (Nepal)"),
    kotoistus("Kotoistus (Finnish IT Ctr)"),
    kunsill_malti(
            "Il-Kunsill Nazzjonali tal-Ilsien Malti",
            "National Council for the Maltese Language",
            "malta",
            "malti"),
    lakota_lc("Lakota LC"),
    lao_dpt("Lao Posts/Telecom"),
    longnow("The Long Now Foundation", "Long Now", "PanLex", "Utilika", "Utilka Foundation"),
    mayan_lpp("Mayan LPP", "Mayan Languages Preservation Project"),
    meta("Meta", "Facebook"),
    microsoft("Microsoft"),
    mikmaw_kinamatnewey("Mi'kmaw Kina'matnewey"),
    motorola("Motorola"),
    mozilla("Mozilla"),
    netflix("Netflix"),
    nyiakeng_puachue_hmong("Nyiakeng Puachue Hmong"),
    openinstitute("Open Inst (Cambodia)"),
    openoffice_org("Open Office"),
    oracle("Oracle", "sun", "Sun Micro"),
    pakistan("Pakistan"),
    rodakych("Rodakych", "Nigerian Pidgin"),
    rohingyazuban("Rohingya Language Council", "RLC", "Rohingya Zuban"),
    rumantscha("Lia Rumantscha"),
    sardware("Sardware", "Sardware"),
    shan_iit("Shan IIT", "Shan Institute of Information Technology"),
    sicilian_cademia("Cademia Siciliana", "Sicilian Academy"),
    sil("SIL", "SIL International"),
    silicon("Stanford SILICON"),
    sitelen_pona("Sitelen Pona", "Sitelen Pona Publishers and Typographers Association"),
    special("High Coverage and Generated"),
    srilanka("Sri Lanka ICTA", "Sri Lanka"),
    sunuwar_ws("Sunuwar Sewa Samaj", "Sunuwar Welfare Society"),
    surveytool("Survey Tool"),
    unaffiliated("Unaffiliated", "Guest"),
    unibz(
            "Free University of Bozen-Bolzano",
            "Libera Università di Bolzano",
            "Freie Universität Bozen"),
    venetian("VeC - Lengua Veneta"),
    welsh_lc("Welsh LC"),
    wikimedia("Wikimedia Foundation"),
    wod_nko("WOD N’ko", "World Organization for the Development of N’ko", "WODN"),
    wsci_wg("WSC+I WG", "Western Swampy Cree+Internet Working Group"),
    yahoo("Yahoo"),
    ;

    private static final Set<Organization> TC_ORGS =
            ImmutableSet.copyOf(EnumSet.of(apple, google, meta, microsoft));

    /**
     * Get a list of the TC Organizations
     *
     * @return the set
     */
    public static Set<Organization> getTCOrgs() {
        return TC_ORGS;
    }

    /**
     * Is this organization a TC Org?
     *
     * @return true if it is TC
     */
    public boolean isTCOrg() {
        return getTCOrgs().contains(this);
    }

    private final String displayName;
    private final String[] names;

    public static Organization fromString(String name) {
        if (name == null) {
            throw new NullPointerException("Organization.fromString(null) called");
        }
        if (name.contains("Government of Pakistan")) {
            /*
             * "Government of Pakistan - National Language Authority"
             * occurs in the cldr_users table; avoid problems with hyphen
             */
            return Organization.pakistan;
        } else if (name.contains("Utilika")) {
            /*
             * "Utilika" and "Utilika Foundation" occur in the cldr_users table.
             * Compare "Utilka Foundation", one of the variants for Organization.longnow
             */
            return Organization.longnow;
        }
        name = name.toLowerCase().replace('-', '_').replace('.', '_');
        return OrganizationNameMap.get(name);
    }

    public String getDisplayName() {
        return displayName;
    }

    static final Map<String, Organization> OrganizationNameMap;

    static {
        OrganizationNameMap = new HashMap<>();
        for (Organization x : values()) {
            OrganizationNameMap.put(
                    x.displayName.toLowerCase().replace('-', '_').replace('.', '_'), x);
            for (String name : x.names) {
                OrganizationNameMap.put(name.toLowerCase().replace('-', '_').replace('.', '_'), x);
            }
            OrganizationNameMap.put(x.name().toLowerCase().replace('-', '_').replace('.', '_'), x);
        }
    }

    /**
     * @param displayName Preferred display name for the organization
     * @param names Alternate aliases for this organization
     */
    Organization(String displayName, String... names) {
        this.displayName = displayName;
        this.names = names;
    }

    private LocaleSet localeSet = null;

    public LocaleSet getCoveredLocales() {
        if (localeSet == null) {
            final Set<String> localeNameSet = StandardCodes.make().getLocaleCoverageLocales(this);
            if (localeNameSet.contains(LocaleNormalizer.ALL_LOCALES)) {
                localeSet = LocaleNormalizer.ALL_LOCALES_SET;
            } else {
                localeSet = new LocaleSet(localeNameSet);
            }
        }
        return localeSet;
    }

    public boolean visibleOnFrontEnd() {
        return this != Organization.special;
    }
}
