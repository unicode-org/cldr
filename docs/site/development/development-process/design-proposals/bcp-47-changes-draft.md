---
title: BCP 47 Changes (DRAFT)
---

# BCP 47 Changes (DRAFT)

With the new release of the new version of [BCP 47](http://www.inter-locale.com/ID/draft-ietf-ltru-4646bis-18.html), there are various changes we need to make in Unicode CLDR and LDML. Already in CLDR 1.7 we have made modifications anticipating the release: see [BCP 47 Tag Conversion](http://unicode.org/reports/tr35/#BCP_47_Tag_Conversion) in the spec (and the orginal [design proposal](https://cldr.unicode.org/development/development-process/design-proposals/bcp47-syntax-mapping)), but more changes need to be made.

## Formula

We need to take another look at which languages we show in the survey tool for translation, because the new version is [very large](http://tools.ietf.org/html/draft-ietf-ltru-4645bis), around 7,000 languages. Showing all of those languages in the Survey tool would neither be good for the usability of the tool for most translators, nor for tool performance, so we need some formula for picking which languages to show by default.

For feedback on this document, please file a Reply under [http://www.unicode.org/cldr/bugs/locale-bugs?findid=1977](http://www.unicode.org/cldr/bugs/locale-bugs?findid=1977). For discussion of issues, please send email to [cldr-users@unicode.org](mailto:cldr-users@unicode.org).

**Draft Formula**

A. We show a language code X for translation if any of the following conditions are true:

1. X is a qualified language\*\*, **and** has at least **100K** speakers, **and** at least one of the following is true:
	1. X is has official status\* in any country
	2. X exceeds a threshold population† of literate users worldwide: **10M**
	3. X exceeds a threshold population† in some country Z: **1M** ***and*** **1/3** of Z's population†.
2. X has ***non-draft*** minimal language coverage‡ in CLDR itself.
3. *Only for translation in locale Y:* X is a qualified language\*\* that already has a translation in CLDR data in Y.
4. X is an exception explicitly approved by the committee, either in root, or in some language Y.
	1. Current examples: Latin, Sanskrit

If a translator finds that X is needed for translation in language Y, then a bug can be filed. If we find the volume is high, we may need to add is some way for a translator to add a language in the survey tool.

B. We show a script code S for translation if and only if it is one of the scripts used by one of the languages shown.

**Notes**

\*\* qualified language: excluding collection (except for macrolanguages with predominant forms), ancient, historic, and extinct languages: see [Scope](http://www.sil.org/iso639-3/scope.asp) and [Types](http://www.sil.org/iso639-3/types.asp). Some could be added as exceptions as needed.

‡ minimal coverage - see [Coverage Levels](http://www.unicode.org/reports/tr35/#Coverage_Levels) - at a non-draft level.

\* *official status* means official, de facto official, official regional, or de facto official regional.

† *population* means literate 14-day active users (well, theoretically - we can only get an approximation of that), based on  [CLDR figures](http://www.unicode.org/cldr/data/charts/supplemental/language_territory_information.html). Our concern is with written language, not spoken, and so we don't focus on variants that don't have much written usage; moreover, the population figures we want to focus on are the literate population. For this reason and others, we don't rely on the Ethnologue figures. See also [Picking the Right Language Code](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code).


**Please review the generated lists in** [**Filtered Scripts and Languages**](https://cldr.unicode.org/development/development-process/design-proposals/bcp-47-changes-draft)**.** A spreadsheet with some details is on<http://spreadsheets.google.com/pub?key=rORMJfeNEUR37PlS8HIa_rQ>. The first column is the language, 2rd is the world population of the language (literate), and the remaining columns are the reasons (data for 1.1, 1.2, 1.3 from the above).

Known issues:

- need to add Norwegian [no], resolve tl vs fil, ...
- Tokelau has no speakers (bug filed)

### Survey Tool Changes

The above would only require a small tool change: the main change is that the approved list from #1 and #2 would be in CODE\_FALLBACK, and nothing else would. Languages would get #3 cases by virtue of there being a translated tag already in the language, even if Root doesn't have anything (because it is not in CODE\_FALLBACK). Thus if the locale doesn't already contain a translation for, say, Ancient Greek, it would not show up in the survey tool.

We would add the lists to the supplemental metadata for access by the tools. The Coverage tool and spec also need to be aligned with the above.

## Other Changes

We also need to make other changes to the spec in regards to the new version of BCP 47. In particular, those [macrolanguages](http://www.sil.org/iso639-3/macrolanguages.asp) with an encompassed language that is a "predominant form", CLDR treats the predominant form and the macrolanguage as aliases. See [Locale Field Definitions](http://unicode.org/reports/tr35/#Locale_Field_Definitions) in the spec. We need to flesh that table out to include all of the [macrolanguages](http://www.sil.org/iso639-3/macrolanguages.asp) that are in the [Included Languages](https://cldr.unicode.org/development/development-process/design-proposals/bcp-47-changes-draft), such as Azerbaijani. Here is a start at that (but still just draft). The first part of this list is from a draft of BCP 47bis. The last three are codes that are in the current (2006) version of BCP 47.

Macrolanguage Table

| Macrolanguage | Encompassed Language | Comments |
|---|---|---|
| Arabic ' ar ' | Standard Arabic ' arb ' |  |
| Konkani ' kok ' | Konkani ( individual language) ' knn ' |  |
| Malay ' ms ' | Standard Malay ' zsm ' |  |
| Swahili ' sw ' | Swahili ( individual language) ' swh ' |  |
| Uzbek ' uz ' | Northern Uzbek ' uzn ' |  |
| Chinese ' zh ' | Mandarin Chinese ' cmn ' |  |
| Norwegian ' no ' | Norwegian Bokmal ' nob ' = nb | To regularize, we may want to switch in CLDR from nb as the 'norm' to no. |
| Serbo-croatian ' sh ' |  | *This is a complex situation, and we'll probably leave as is.* |
| Kurdish 'ku' | Northern Kurdish 'kmr'? | We probably want to change the default content locale to ku-Latn |
| Akan ' ak ' | Twi ' tw ' and Fanti ' fat' | This appears to be a mistake in ISO 639. See: ISO 636 Deprecation Requests . |
| Persian  fas (fa) | Western Farsi pes and prs Dari | This appears to be a mistake in ISO 639. See: ISO 636 Deprecation Requests . |
 
These would also go into the \<alias> element of the supplemental metadata. We may add more such aliases over time, as we find new predominant forms. Note that we still need to offer both aliases for translation in many cases. For example, we want to show both 'no' and 'nb'.

## Lenient Parsing

There are many circumstances where we get less than perfect language identifiers coming in. I think we should have some guidelines as to how to do this. Here are the possibilities:

1. case / hyphen insensitivity
2. map valid non-canonical forms to their canonical equivalents (zh-cmn, cmn => zh)
3. map certain common invalid forms to their canonical equivalents:
	1. UK => GB
	2. eng => en // and other illegal 3-letter 639 codes that correspond to 2-letter codes
	3. 840 => US // other numeric region codes that correspond to 2-letter codes
4. map away extlangs. Formally, en-yue is valid (this slipped by us in doing BCP 47), and canonicalizes in BCP 47 to yue, the same as zh-yue does. In any event, the simplest thing for us to do is if there is a syntactic extlang:
	1. Verify that the base language and extlang are both valid language subtags
	2. Remove the base language
	3. This avoids having to store which languages are also extlangs, and what their prefixes are.

People have to do #1. We should recommend #2, and make it easy to support #3. 

See demo at [http://unicode.org/cldr/utility/languageid.jsp](http://unicode.org/cldr/utility/languageid.jsp)

Also, we should consider modifying the canonical form of language identifiers so as to have lowercase variants (with the exception of some set of grandfathered codes). The following are generated by GenerateMaximalLocales, plus 7 hand modifications for the last line.

## Filtered Scripts and Languages

The following script/language names would be included (/excluded) from default translation. For the method used to get this list, see [Formula](https://cldr.unicode.org/development/development-process/design-proposals/bcp-47-changes-draft).

The languages are listed in the format Abkhazian [ab]-OR, where [xx] is the code, and "OR" is the abbreviated "best" status in some territory: **U**nknown, **O**fficial **R**egional, **O**fficial **M**inority, **D**e facto official, **O**fficial.

### Included Script Names: 41+

- Arabic [Arab], Armenian [Armn]
- Bengali [Beng]
- Cyrillic [Cyrl]
- Devanagari [Deva]
- Ethiopic [Ethi]
- Georgian [Geor], Greek [Grek], Gujarati [Gujr], Gurmukhi [Guru]
- Hebrew [Hebr], Han [Hani]
	- Simplified Han [Hans], Traditional Han [Hant], Bopomofo [Bopo]
- Japanese [Jpan]
	- Hiragana [Hira], Katakana [Kana]
- Kannada [Knda], Khmer [Khmr], Korean [Kore]
	- Hangul [Hang]
- Lao [Laoo], Latin [Latn]
- Malayalam [Mlym], Mongolian [Mong], Myanmar [Mymr]
- Oriya [Orya]
- Sinhala [Sinh]
- Tamil [Taml], Telugu [Telu], Thaana [Thaa], Thai [Thai], Tibetan [Tibt]
- Special codes:
	- Common [Zyyy], Symbols [Zsym], Unwritten [Zxxx], Unknown or Invalid Script [Zzzz]
	- Braille [Brai]
- *Possibly also in the future:*
	- Tifinagh [Tfng], Yi [Yiii],
	- Unified Canadian Aboriginal Syllabics [Cans],

### Excluded Script Names:

- Avestan [Avst]
- Balinese [Bali], Batak [Batk], Blissymbols [Blis], Book Pahlavi [Phlv], Brahmi [Brah], Buginese [Bugi], Buhid [Buhd]
- Carian [Cari], Chakma [Cakm], Cham, Cherokee [Cher], Cirth [Cirt], Coptic [Copt], Cypriot [Cprt]
- Deseret [Dsrt]
- Eastern Syriac [Syrn], Egyptian demotic [Egyd], Egyptian hieratic [Egyh], Egyptian hieroglyphs [Egyp], Estrangelo Syriac [Syre]
- Fraktur Latin [Latf]
- Gaelic Latin [Latg], Georgian Khutsuri [Geok], Glagolitic [Glag], Gothic [Goth]
- Hanunoo [Hano]
- Imperial Aramaic [Armi], Indus [Inds], Inherited [Qaai], Inscriptional Pahlavi [Phli], Inscriptional Parthian [Prti]
- Javanese [Java]
- Kaithi [Kthi], Katakana or Hiragana [Hrkt], Kayah Li [Kali], Kharoshthi [Khar]
- Lanna [Lana], Lepcha [Lepc], Limbu [Limb], Linear A [Lina], Linear B [Linb], Lisu, Lycian [Lyci], Lydian [Lydi]
- Mandaean [Mand], Manichaean [Mani], Mathematical Notation [Zmth], Mayan hieroglyphs [Maya], Meitei Mayek [Mtei], Meroitic [Mero], Moon
- N’Ko [Nkoo], New Tai Lue [Talu], Nkgb
- Ogham [Ogam], Ol Chiki [Olck], Old Church Slavonic Cyrillic [Cyrs], Old Hungarian [Hung], Old Italic [Ital], Old Permic [Perm], Old Persian [Xpeo], Orkhon [Orkh], Osmanya [Osma]
- Pahawh Hmong [Hmng], Phags-pa [Phag], Phoenician [Phnx], Pollard Phonetic [Plrd], Psalter Pahlavi [Phlp]
- Rejang [Rjng], Rongorongo [Roro], Runic [Runr]
- Samaritan [Samr], Sarati [Sara], Saurashtra [Saur], Shavian [Shaw], SignWriting [Sgnw], Sumero-Akkadian Cuneiform [Xsux], Sundanese [Sund], Syloti Nagri [Sylo], Syriac [Syrc]
- Tagalog [Tglg], Tagbanwa [Tagb], Tai Le [Tale], Tai Viet [Tavt], Tengwar [Teng], Tifinagh [Tfng]
- Ugaritic [Ugar]
- Vai [Vaii], Visible Speech [Visp]
- Western Syriac [Syrj]
- Yi [Yiii]
- Inherited [Zinh]

### Included Languages: 202

- Abkhazian [ab]-OR, Adyghe [ady]-OR, Afrikaans [af]-O, Akan [ak]-U, Albanian [sq]-O, Amharic [am]-O, Arabic [ar]-O, Armenian [hy]-O, Assamese [as]-O, Asturian [ast]-OR, Avaric [av]-OR, Awadhi [awa]-U, Aymara [ay]-O, Azerbaijani [az]-O
- Bambara [bm]-U, Bashkir [ba]-OR, Basque [eu]-OR, Belarusian [be]-O, Bengali [bn]-O, Bhojpuri [bho]-U, Bislama [bi]-O, Bosnian [bs]-O, Bulgarian [bg]-O, Burmese [my]-O
- Catalan [ca]-O, Cebuano [ceb]-OR, Chamorro [ch]-O, Chechen [ce]-OR, Chinese [zh]-O, Chuukese [chk]-O, Croatian [hr]-O, Czech [cs]-O
- Danish [da]-O, Divehi [dv]-O, Dutch [nl]-O, Dzongkha [dz]-O
- Efik [efi]-O, English [en]-O, Erzya [myv]-OR, Estonian [et]-O, Ewe [ee]-OR
- Faroese [fo]-O, Fijian [fj]-O, Filipino [fil]-O, Finnish [fi]-O, French [fr]-O
- Ga [gaa]-OR, Gagauz [gag]-OR, Galician [gl]-OR, Georgian [ka]-O, German [de]-O, Gilbertese [gil]-O, Greek [el]-O, Guarani [gn]-O, Gujarati [gu]-O
- Haitian [ht]-O, Hausa [ha]-O, Hawaiian [haw]-OR, Hebrew [he]-O, Hiligaynon [hil]-OR, Hindi [hi]-O, Hiri Motu [ho]-O, Hungarian [hu]-O
- Icelandic [is]-O, Igbo [ig]-O, Iloko [ilo]-OR, Indonesian [id]-O, Ingush [inh]-OR, Inuktitut [iu]-OR, Irish [ga]-O, Italian [it]-O
- Japanese [ja]-O, Javanese [jv]-U
- Kabardian [kbd]-OR, Kalaallisut [kl]-O, Kannada [kn]-O, Karachay-Balkar [krc]-OR, Kashmiri [ks]-O, Kazakh [kk]-O, Khasi [kha]-OR, Khmer [km]-O, Kinyarwanda [rw]-O, Kirghiz [ky]-O, Komi-Permyak [koi]-OR, Komi-Zyrian [kpv]-OR, Konkani [kok]-OR, Korean [ko]-O, Kosraean [kos]-O, Krio [kri]-U, Kumyk [kum]-OR, Kurdish [ku]-OR
- Lahnda [lah]-U, Lak [lbe]-OR, Lao [lo]-O, Latin [la]-DO, Latvian [lv]-O, Lezghian [lez]-OR, Lingala [ln]-O, Lithuanian [lt]-O, Luxembourgish [lb]-O
- Macedonian [mk]-O, Madurese [mad]-U, Maguindanao [mdh]-OR, Maithili [mai]-OR, Malagasy [mg]-O, Malay [ms]-O, Malayalam [ml]-O, Maltese [mt]-O, Maore Comorian [swb]-O, Maori [mi]-O, Marathi [mr]-O, Marshallese [mh]-O, Moksha [mdf]-OR, Mongolian [mn]-O, Mossi [mos]-U
- Nauru [na]-O, Nepali [ne]-O, Niuean [niu]-O, Northeastern Thai [tts]-U, Northern Sami [se]-OR, Northern Sotho [nso]-O, Norwegian Bokmål [nb]-O, Norwegian Nynorsk [nn]-O, Nyanja [ny]-O
- Oriya [or]-O, Oromo [om]-U, Ossetic [os]-OR
- Palauan [pau]-O, Pangasinan [pag]-OR, Papiamento [pap]-DO, Pashto [ps]-O, Persian [fa]-O, Plains Cree [crk]-OR, Pohnpeian [pon]-O, Polish [pl]-O, Portuguese [pt]-O, Punjabi [pa]-O
- Quechua [qu]-O
- Rhaeto-Romance [rm]-O, Romanian [ro]-O, Rundi [rn]-O, Russian [ru]-O
- Samoan [sm]-O, Sango [sg]-O, Sanskrit [sa]-O, Santali [sat]-OR, Scottish Gaelic [gd]-OR, Serbian [sr]-O, Shona [sn]-U, Sindhi [sd]-O, Sinhala [si]-O, Slovak [sk]-O, Slovenian [sl]-O, Somali [so]-O, Southern Sotho [st]-O, Spanish [es]-O, Sundanese [su]-O, Swahili [sw]-O, Swati [ss]-O, Swedish [sv]-O, Swiss German [gsw]-U
- Tagalog [tl]-OR, Tahitian [ty]-O, Tajik [tg]-O, Tamil [ta]-O, Tatar [tt]-OR, Tausug [tsg]-OR, Telugu [te]-O, Tetum [tet]-O, Thai [th]-O, Tibetan [bo]-OR, Tigrinya [ti]-DO, Tok Pisin [tpi]-O, Tokelau [tkl]-O, Tonga [to]-O, Tsonga [ts]-O, Tswana [tn]-O, Turkish [tr]-O, Turkmen [tk]-O, Tuvalu [tvl]-O, Tuvinian [tyv]-OR, Twi [tw]-OR
- Udmurt [udm]-OR, Uighur [ug]-OR, Ukrainian [uk]-O, Ulithian [uli]-O, Unknown or Invalid Language [und]-S, Urdu [ur]-O, Uzbek [uz]-O
- Venda [ve]-O, Vietnamese [vi]-O
- Waray [war]-OR, Welsh [cy]-OR, Western Frisian [fy]-OR, Wolof [wo]-O, Woods Cree [cwd]-OR
- Xhosa [xh]-O
- Yakut [sah]-OR, Yapese [yap]-O, Yoruba [yo]-O
- Zhuang [za]-OR, Zulu [zu]-O

### Excluded Languages: 299

- Achinese [ace]-U, Acoli [ach]-U, Adangme [ada]-U, Afar [aa]-U, Afrihili [afh]-U, Afro-Asiatic Language [afa]-U, Ainu [ain]-U, Akkadian [akk]-U, Aleut [ale]-U, Algonquian Language [alg]-U, Altaic Language [tut]-U, Ancient Egyptian [egy]-U, Ancient Greek [grc]-U, Angika [anp]-U, Apache Language [apa]-U, Aragonese [an]-U, Aramaic [arc]-U, Arapaho [arp]-U, Araucanian [arn]-U, Arawak [arw]-U, Aromanian [rup]-U, Artificial Language [art]-U, Athapascan Language [ath]-U, Atsam [cch]-U, Australian Language [aus]-U, Austronesian Language [map]-U, Avestan [ae]-U
- Balinese [ban]-U, Baltic Language [bat]-U, Baluchi [bal]-U, Bamileke Language [bai]-U, Banda [bad]-U, Bantu [bnt]-U, Basa [bas]-U, Batak [btk]-U, Beja [bej]-U, Bemba [bem]-U, Berber [ber]-U, Bihari [bh]-U, Bikol [bik]-U, Bini [bin]-U, Blin [byn]-U, Blissymbols [zbl]-U, Braj [bra]-U, Breton [br]-U, Buginese [bug]-U, Buriat [bua]-U
- Caddo [cad]-U, Carib [car]-U, Caucasian Language [cau]-U, Celtic Language [cel]-U, Central American Indian Language [cai]-U, Chagatai [chg]-U, Chamic Language [cmc]-U, Cherokee [chr]-U, Cheyenne [chy]-U, Chibcha [chb]-U, Chinook Jargon [chn]-U, Chipewyan [chp]-U, Choctaw [cho]-U, Church Slavic [cu]-U, Chuvash [cv]-U, Classical Newari [nwc]-U, Classical Syriac [syc]-U, Coptic [cop]-U, Cornish [kw]-U, Corsican [co]-U, Cree [cr]-U, Creek [mus]-U, Creole or Pidgin [crp]-U, Crimean Turkish [crh]-U, Cushitic Language [cus]-U
- Dakota [dak]-U, Dargwa [dar]-U, Dayak [day]-U, Delaware [del]-U, Dinka [din]-U, Dogri [doi]-U, Dogrib [dgr]-U, Dravidian Language [dra]-U, Duala [dua]-U, Dyula [dyu]-U
- Eastern Frisian [frs]-U, Ekajuk [eka]-U, Elamite [elx]-U, English-based Creole or Pidgin [cpe]-U, Esperanto [eo]-U, Ewondo [ewo]-U
- Fang [fan]-U, Fanti [fat]-U, Finno-Ugrian Language [fiu]-U, Fon [fon]-U, French-based Creole or Pidgin [cpf]-U, Friulian [fur]-U, Fulah [ff]-U
- Ganda [lg]-U, Gayo [gay]-U, Gbaya [gba]-U, Geez [gez]-U, Germanic Language [gem]-U, Gondi [gon]-U, Gorontalo [gor]-U, Gothic [got]-U, Grebo [grb]-U, Gwichʼin [gwi]-U
- Haida [hai]-U, Herero [hz]-U, Himachali [him]-U, Hittite [hit]-U, Hmong [hmn]-U, Hupa [hup]-U
- Iban [iba]-U, Ido [io]-U, Ijo [ijo]-U, Inari Sami [smn]-U, Indic Language [inc]-U, Indo-European Language [ine]-U, Interlingua [ia]-U, Interlingue [ie]-U, Inupiaq [ik]-U, Iranian Language [ira]-U, Iroquoian Language [iro]-U
- Jju [kaj]-U, Judeo-Arabic [jrb]-U, Judeo-Persian [jpr]-U
- Kabyle [kab]-U, Kachin [kac]-U, Kalmyk [xal]-U, Kamba [kam]-U, Kanuri [kr]-U, Kara-Kalpak [kaa]-U, Karelian [krl]-U, Karen [kar]-U, Kashubian [csb]-U, Kawi [kaw]-U, Khoisan Language [khi]-U, Khotanese [kho]-U, Kikuyu [ki]-U, Kimbundu [kmb]-U, Klingon [tlh]-U, Komi [kv]-U, Kongo [kg]-U, Koro [kfo]-U, Kpelle [kpe]-U, Kru [kro]-U, Kuanyama [kj]-U, Kurukh [kru]-U, Kutenai [kut]-U
- Ladino [lad]-U, Lamba [lam]-U, Limburgish [li]-U, Lojban [jbo]-U, Low German [nds]-U, Lower Sorbian [dsb]-U, Lozi [loz]-U, Luba-Katanga [lu]-U, Luba-Lulua [lua]-U, Luiseno [lui]-U, Lule Sami [smj]-U, Lunda [lun]-U, Luo [luo]-U, Lushai [lus]-U
- Magahi [mag]-U, Makasar [mak]-U, Manchu [mnc]-U, Mandar [mdr]-U, Mandingo [man]-U, Manipuri [mni]-U, Manobo Language [mno]-U, Manx [gv]-U, Mari [chm]-U, Marwari [mwr]-U, Masai [mas]-U, Mayan Language [myn]-U, Mende [men]-U, Micmac [mic]-U, Middle Dutch [dum]-U, Middle English [enm]-U, Middle French [frm]-U, Middle High German [gmh]-U, Middle Irish [mga]-U, Minangkabau [min]-U, Mirandese [mwl]-U, Miscellaneous Language [mis]-S, Mohawk [moh]-U, Mon-Khmer Language [mkh]-U, Mongo [lol]-U, Multiple Languages [mul]-S, Munda Language [mun]-U
- N’Ko [nqo]-U, Nahuatl [nah]-U, Navajo [nv]-U, Ndonga [ng]-U, Neapolitan [nap]-U, Newari [new]-U, Nias [nia]-U, Niger-Kordofanian Language [nic]-U, Nilo-Saharan Language [ssa]-U, No linguistic content [zxx]-S, Nogai [nog]-U, North American Indian Language [nai]-U, North Ndebele [nd]-U, Northern Frisian [frr]-U, Norwegian [no]-U, Nubian Language [nub]-U, Nyamwezi [nym]-U, Nyankole [nyn]-U, Nyasa Tonga [tog]-U, Nyoro [nyo]-U, Nzima [nzi]-U
- Occitan [oc]-U, Ojibwa [oj]-U, Old English [ang]-U, Old French [fro]-U, Old High German [goh]-U, Old Irish [sga]-U, Old Norse [non]-U, Old Persian [peo]-U, Old Provençal [pro]-U, Osage [osa]-U, Otomian Language [oto]-U, Ottoman Turkish [ota]-U
- Pahlavi [pal]-U, Pali [pi]-U, Pampanga [pam]-U, Papuan Language [paa]-U, Philippine Language [phi]-U, Phoenician [phn]-U, Portuguese-based Creole or Pidgin [cpp]-U, Prakrit Language [pra]-U
- Rajasthani [raj]-U, Rapanui [rap]-U, Rarotongan [rar]-U, Romance Language [roa]-U, Romany [rom]-U, Root [root]-U
- Salishan Language [sal]-U, Samaritan Aramaic [sam]-U, Sami Language [smi]-U, Sandawe [sad]-U, Sardinian [sc]-U, Sasak [sas]-U, Scots [sco]-U, Selkup [sel]-U, Semitic Language [sem]-U, Serer [srr]-U, Shan [shn]-U, Sichuan Yi [ii]-U, Sicilian [scn]-U, Sidamo [sid]-U, Sign Language [sgn]-U, Siksika [bla]-U, Sino-Tibetan Language [sit]-U, Siouan Language [sio]-U, Skolt Sami [sms]-U, Slave [den]-U, Slavic Language [sla]-U, Sogdien [sog]-U, Songhai [son]-U, Soninke [snk]-U, Sorbian Language [wen]-U, South American Indian Language [sai]-U, South Ndebele [nr]-U, Southern Altai [alt]-U, Southern Sami [sma]-U, Sranan Tongo [srn]-U, Sukuma [suk]-U, Sumerian [sux]-U, Susu [sus]-U, Syriac [syr]-U
- Tai Language [tai]-U, Tamashek [tmh]-U, Tereno [ter]-U, Tigre [tig]-U, Timne [tem]-U, Tiv [tiv]-U, Tlingit [tli]-U, Tsimshian [tsi]-U, Tumbuka [tum]-U, Tupi Language [tup]-U, Tyap [kcg]-U
- Ugaritic [uga]-U, Umbundu [umb]-U, Upper Sorbian [hsb]-U
- Vai [vai]-U, Volapük [vo]-U, Votic [vot]-U
- Wakashan Language [wak]-U, Walamo [wal]-U, Walloon [wa]-U, Washo [was]-U
- Yao [yao]-U, Yiddish [yi]-U, Yupik Language [ypk]-U
- Zande [znd]-U, Zapotec [zap]-U, Zaza [zza]-U, Zenaga [zen]-U, Zuni [zun]-U

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)