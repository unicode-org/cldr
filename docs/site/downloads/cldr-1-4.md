---
title: CLDR 1.4 Release Note
---

# CLDR 1.4 Release Note

| No. | Date       | Rel. Note | Data        | Spec        | Delta Tickets | GitHub Tag |
|:---:|:----------:|:---------:|:-----------:|:-----------:|:------------:|:----------:|
| 1.4 | 2006-07-17 | [Version1.4] | [CLDR1.4]   | [LDML1.4] | [~~Δ1.4~~] | [release-1-4] |


The following are the files for this release. For a description of their purpose and format, see [CLDR Releases (Downloads)].

This release of CLDR contains data for 121 languages and 142 territories -- 360 locales in all.
Version 1.4 of the repository contains over 25% more locale data than the previous release, with over 17,000 new or modified data items entered by over 100 different contributors.
Major contributors to CLDR 1.4 include Apple, Google, IBM, and Sun, plus official representatives from a number of countries.
Many other organizations and individuals around the globe have also made important contributions.

Some of the major features of LDML 1.4 used in the repository include new XML structures supporting customizable detection of words, lines, and sentences (segmentation),
transliteration between different alphabets, and full compatibility with the recently approved internet standards for language tags.
It also supports enhanced formats for dates and times, and adds new guidelines for date, time, and number parsing, as well as a number of clarifications.
Other features of the release include:

- Full separation of linguistic from non-linguistic data
- Additional distinctions among draft data (between provisional and unconfirmed)
- Translations for quarters in date formats (such as "2006 Q1")
- Additional time zone formatting
- Metadata for validating CLDR data
- Data for replacing obsoleted codes for languages, scripts, timezones, and so on.

## List of Languages and Territories in this Release

**Languages (121):** Afar [Qafar]; Afrikaans; Akan [ak]; Albanian [shqipe]; Amharic [አማርኛ]; Arabic [‎العربية‎]; Armenian [Հայերէն]; Assamese [অসমীয়া]; Azerbaijani - Cyrillic [Азәрбајҹан - Cyrl]; Azerbaijani - Latin [azərbaycanca - Latn]; Basque [euskara]; Belarusian [Беларускі]; Bengali [বাংলা]; Blin [ብሊን]; Bosnian [Bosanski]; Bulgarian [Български]; Catalan [català]; Chinese - Simplified Han [中文 - 简体中文]; Chinese - Traditional Han [中文 - 繁體漢語]; Cornish [kernewek]; Croatian [hrvatski]; Czech [Čeština]; Danish [Dansk]; Divehi [‎ދިވެހިބަސް‎]; Dutch [Nederlands]; Dzongkha [རྫོང་ཁ]; English; Esperanto; Estonian [Eesti]; Ewe [ee]; Faroese [føroyskt]; Finnish [suomi]; French [français]; Friulian [furlan]; Ga [gaa]; Galician [galego]; Geez [ግዕዝኛ]; Georgian [ქართული]; German [Deutsch]; Greek [Ελληνικά]; Gujarati [ગુજરાતી]; Hausa - Arabic [ha - Arab]; Hausa - Latin [ha - Latn]; Hawaiian [ʻōlelo Hawaiʻi]; Hebrew [‎עברית‎]; Hindi [हिंदी]; Hungarian [magyar]; Icelandic [Íslenska]; Igbo [ig]; Indonesian [Bahasa Indonesia]; Interlingua [ia]; Inuktitut [ᐃᓄᒃᑎᑐᑦ ᑎᑎᕋᐅᓯᖅ]; Irish [Gaeilge]; Italian [italiano]; Japanese [日本語]; Kalaallisut; Kamba [kam]; Kannada [ಕನ್ನಡ]; Kazakh [Қазақ]; Khmer [ភាសាខ្មែរ]; Kinyarwanda [rw]; Kirghiz [Кыргыз]; Konkani [कोंकणी]; Korean [한국어]; Kurdish - Arabic [kurdî - erebî]; Kurdish - Latin [kurdî - Latn]; Lao [ລາວ]; Latvian [latviešu]; Lingala [lingála]; Lithuanian [Lietuvių]; Macedonian [македонски]; Malay [Bahasa Melayu]; Malayalam [മലയാളം]; Maltese [Malti]; Manx [Gaelg]; Marathi [मराठी]; Mongolian [Монгол хэл]; Nepali [ne]; Northern Sami [se]; Northern Sotho [nso]; Norwegian Bokmål [bokmål]; Norwegian Nynorsk [nynorsk]; Nyanja; Chichewa; Chewa [ny]; Oriya [ଓଡ଼ିଆ]; Oromo [Oromoo]; Pashto [‎پښتو‎]; Persian [‎فارسی‎]; Polish [polski]; Portuguese [português]; Punjabi - Arabic [ਪੰਜਾਬੀ - Arab]; Punjabi - Gurmukhi [ਪੰਜਾਬੀ - ਗੁਰਮੁਖੀ]; Romanian [Română]; Russian [русский]; Sanskrit [संस्कृत]; Serbian - Cyrillic [Српски - Ћирилица]; Serbian - Latin [Srpski - Latinica]; Sidamo [Sidaamu Afo]; Slovak [slovenský]; Slovenian [Slovenščina]; Somali [Soomaali]; South Ndebele [nr]; Southern Sotho [st]; Spanish [español]; Swahili [Kiswahili]; Swati [ss]; Swedish [svenska]; Syriac [‎ܣܘܪܝܝܐ‎]; Tajik [tg]; Tamil [தமிழ்]; Tatar [Татар]; Telugu [తెలుగు]; Thai [ไทย]; Tigre [ትግረ]; Tigrinya [ትግርኛ]; Tsonga [ts]; Tswana [tn]; Turkish [Türkçe]; Ukrainian [Українська]; Urdu [‎اردو‎]; Uzbek - Arabic [‎اۉزبېک - Араб‎]; Uzbek - Cyrillic [Ўзбек - Кирил]; Uzbek - Latin [oʿzbek - Lotin]; Venda [ve]; Vietnamese [Tiếng Việt]; Walamo [ወላይታቱ]; Welsh [Cymraeg]; Xhosa [xh]; Yoruba [yo]; Zulu [zu]

**Territories (142):** Afghanistan [‎افغانستان‎]; Albania [Shqipëria]; Algeria [‎الجزائر‎]; American Samoa; Argentina; Armenia [Հայաստանի Հանրապետութիւն]; Australia; Austria [Österreich]; Azerbaijan [Azərbaycan, Азәрбајҹан]; Bahrain [‎البحرين‎]; Bangladesh [বাংলাদেশ]; Belarus [Беларусь]; Belgium [België, Belgien, Belgique]; Belize; Bhutan [འབྲུག]; Bolivia; Bosnia and Herzegovina [Bosna i Hercegovina, Босна и Херцеговина]; Botswana; Brazil [Brasil]; Brunei; Bulgaria [България]; Cambodia [កម្ពុជា]; Canada; Chile; China [中国]; Colombia; Congo (Brazzaville) [Kɔngɔ́ (Brazzaville)]; Congo (Kinshasa) [Kɔngɔ́ (Kinshasa)]; Costa Rica; Croatia [Hrvatska]; Cyprus [Κύπρος]; Czech Republic [Česká republika]; Denmark [Danmark]; Djibouti [Jabuuti, Yabuuti]; Dominican Republic [República Dominicana]; Ecuador; Egypt [‎مصر‎]; El Salvador; Eritrea [Eretria, ኤርትራ]; Estonia [Eesti]; Ethiopia [Itiyoophiya, Itoobiya, Itoophiyaa, Otobbia, ኢትዮጵያ]; Faroe Islands [Føroyar]; Finland [Suomi]; France; Georgia [საქართველო]; Germany [Deutschland]; Ghana [GH]; Greece [Ελλάδα]; Greenland [Kalaallit Nunaat]; Guam; Guatemala; Honduras; Hong Kong SAR China [中華人民共和國香港特別行政區]; Hungary [Magyarország]; Iceland [Ísland]; India [‎بھارت‎, भारत, भारतम्, ভারত, ভাৰত, ਭਾਰਤ, ભારત, ଭାରତ, இந்தியா, భారత దేళం, ಭಾರತ, ഇന്ത്യ]; Indonesia; Iran [IR, ‎ایران‎]; Iraq [IQ, ‎العراق‎]; Ireland [Éire]; Israel [‎ישראל‎]; Italy [Italia, Italie]; Jamaica; Japan [日本]; Jordan [‎الأردن‎]; Kazakhstan [Қазақстан]; Kenya [KE, Keeniyaa, Kiiniya]; Kuwait [‎الكويت‎]; Kyrgyzstan [Кыргызстан]; Laos [ລາວ]; Latvia [Latvija]; Lebanon [‎لبنان‎]; Libya [‎ليبيا‎]; Liechtenstein; Lithuania [Lietuva]; Luxembourg [Luxemburg]; Macao SAR China [澳門特別行政區]; Macedonia [Македонија]; Malawi [MW]; Malaysia; Maldives [‎ދިވެހި ރާއްޖެ‎]; Malta; Marshall Islands; Mexico [México]; Monaco; Mongolia [Монгол улс]; Morocco [‎المغرب‎]; Namibia [Namibië]; Nepal [NP]; Netherlands [Nederland]; New Zealand; Nicaragua; Niger [NE]; Nigeria [NG]; Northern Mariana Islands; Norway [Noreg, Norge, Norgga]; Oman [‎عمان‎]; Pakistan [PK, ‎پاکستان‎]; Panama [Panamá]; Paraguay; Peru [Perú]; Philippines; Poland [Polska]; Portugal; Puerto Rico; Qatar [‎قطر‎]; Romania [România]; Russia [Россия]; Rwanda [RW]; Saudi Arabia [‎المملكة العربية السعودية‎]; Serbia And Montenegro [Srbija i Crna Gora, Србија и Црна Гора]; Singapore [新加坡]; Slovakia [Slovenská republika]; Slovenia [Slovenija]; Somalia [Soomaaliya]; South Africa [Suid-Afrika, ZA]; South Korea [대한민국]; Spain [Espainia, España, Espanya]; Sudan [‎السودان‎]; Sweden [Sverige]; Switzerland [Schweiz, Suisse, Svizzera]; Syria [SY, ‎سوريا‎, ‎ܣܘܪܝܝܐ‎]; Taiwan [臺灣]; Tajikistan [TJ]; Tanzania; Thailand [ประเทศไทย]; Togo [TG]; Trinidad and Tobago; Tunisia [‎تونس‎]; Turkey [Tirkiye, Türkiye]; U.S. Virgin Islands; Ukraine [Украина, Україна]; United Arab Emirates [‎الامارات العربية المتحدة‎]; United Kingdom [Prydain Fawr, Rywvaneth Unys]; United States [Estados Unidos, ʻAmelika Hui Pū ʻIa]; United States Minor Outlying Islands; Uruguay; Uzbekistan [Oʿzbekiston, Ўзбекистон]; Venezuela; Vietnam [Việt Nam]; Yemen [‎اليمن‎]; Zimbabwe

Notes:

- The amount of data per locale and the status (unconfirmed, provisional, or approved) varies.
- Tooltips will show the ISO code and Latin transliteration if available.
  (To set your tooltip font in IE, right click on the desktop, pick Properties>Appearance>Advanced>Item: ToolTip, then set the font to Arial Unicode MS or other large font.)
- If the above doesn't display in your browser, see [Display Problems?]

[CLDR Releases (Downloads)]: /index/downloads
[Display Problems?]: https://www.unicode.org/help/display_problems.html
<!-- 1.2 release: 2006-07-17 -->
[Version1.4]: /downloads/cldr-1-4
[CLDR1.4]: https://unicode.org/Public/cldr/1.4.0/
[LDML1.4]: https://www.unicode.org/reports/tr35/tr35-6.html
[~~Δ1.4~~]: https://unicode.org/cldr/trac/query?status=closed&col=id&col=summary&milestone=1.4
[release-1-4]: https://github.com/unicode-org/cldr/tree/release-1-4
