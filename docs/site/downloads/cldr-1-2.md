---
title: CLDR 1.2 Release Note
---

# CLDR 1.2 Release Note

| No. | Date       | Rel. Note | Data        | Spec        | Delta Tickets | GitHub Tag |
|:---:|:----------:|:---------:|:-----------:|:-----------:|:------------:|:----------:|
| 1.2 | 2004-11-04 | [v1.2]    | [CLDR1.2]   | [LDML1.2] | [~~Δ1.2~~] | [release-1-2] |

CLDR Version 1.2 contains data for 232 locales, covering 72 languages and 108 territories. There are also 63 draft locales in the process of being developed, covering an additional 27 languages and 28 territories.

*This is a stable release and may be used as reference material or cited as a normative reference by other specifications. Although the data in this release will not be changed, bug reports and feature requests for future versions filed at [Bug Reports].*

In this release, the major additions to CLDR are:

- **Data:** Many names for languages, territories, and scripts have been added, as well as for time zones, calendars, and other named items such as collation; the data has been compacted by removal of inherited or aliased data; plus other fixes and additions.
- **Structure:** The LDML specification has been enhanced substantially. It now has self-contained descriptions of date / number / choice format patterns, inheritance and validity, time zone fallbacks, and provides lists of all valid attribute values. The XML format adds structure to assist in vetting, to allow for multiple sets of exemplar characters (indicating characters in use in particular locales), to represent relative dates and times, to provide better support for time zone names, and to strengthen the alias mechanism.
- **Implementation:** The new collation tests allow implementations to verify correct use of locale data. For comparison of data, by-type charts and vetting charts were added. The CLDR source now does not require ICU for generation.
For information on the organization of CLDR data and the meaning of the links in the table above, see CLDR Releases.

## Languages and Territories

**Languages:** Afrikaans, Albanian (sq, Shqipe), Amharic (am, አማርኛ), Arabic (ar, ‎العربية‎), Armenian (hy, Հայերէն; hy_AM_REVISED, Հայերէն), Basque (eu, Euskara), Belarusian (be, Беларускі), Bengali (bn, বাংলা), Bulgarian (bg, Български), Catalan (ca, Català), Chinese (zh, 中文; zh_Hant, 中文), Cornish (kw, Kernewek), Croatian (hr, Hrvatski), Czech (cs, Čeština), Danish (da, Dansk), Dutch (nl, Nederlands), English (en; en_US_POSIX), Esperanto, Estonian (et, Eesti), Faroese (fo, Føroyskt), Finnish (fi, Suomi), French (fr, Français), Gallegan (gl, Galego), German (de, Deutsch), Greek (el, Ελληνικά), Gujarati (gu, ગુજરાતી), Hebrew (he, ‎עברית‎), Hindi (hi, हिंदी), Hungarian (hu, Magyar), Icelandic (is, Íslenska), Indonesian (id, Bahasa Indonesia), Irish (ga, Gaeilge), Italian (it, Italiano), Japanese (ja, 日本語), Kalaallisut, Kannada (kn, ಕನ್ನಡ), Kazakh (kk, Қазақ), Konkani (kok, कोंकणी), Korean (ko, 한국어), Latvian (lv, Latviešu), Lithuanian (lt, Lietuvių), Macedonian (mk, Македонски), Malay (ms, Bahasa Melayu), Maltese (mt, Malti), Manx (gv, Gaelg), Marathi (mr, मराठी), Norwegian Bokmål (nb, Norsk Bokmål), Norwegian Nynorsk (nn, Norsk Nynorsk), Oromo (om, Oromoo), Pashto (Pushto) (ps, ‎پښتو‎), Persian (fa, ‎فارسی‎), Polish (pl, Polski), Portuguese (pt, Português), Punjabi (pa, ਪੰਜਾਬੀ), Romanian (ro, Română), Russian (ru, Русский), Serbian (sr, Српски; sr_Latn, Srpski), Slovak (sk, Slovenský), Slovenian (sl, Slovenščina), Somali (so, Soomaali), Spanish (es, Español), Swahili (sw, Kiswahili), Swedish (sv, Svenska), Tamil (ta, தமிழ்), Telugu (te, తెలుగు), Thai (th, ไทย), Tigrinya (ti, ትግርኛ), Turkish (tr, Türkçe), Ukrainian (uk, Українська), Vietnamese (vi, Tiếng Việt), Welsh (cy, Cymraeg)

**Countries:** Afghanistan (fa, ‎افغانستان‎; ps, ‎افغانستان‎), Albania (sq, Shqipëria), Algeria (ar, ‎الجزائر‎), Argentina, Armenia (hy, Հայաստանի Հանրապետութիւն), Australia, Austria (de, Österreich), Bahrain (ar, ‎البحرين‎), Belarus (be, Беларусь), Belgium (de, Belgien; en; fr, Belgique; nl, België), Bolivia, Botswana, Brazil (pt, Brasil), Brunei, Bulgaria (bg, България), Canada (en; fr), Chile, China (zh, 中国), Colombia, Costa Rica, Croatia (hr, Hrvatska), Czech Republic (cs, Česká Republika), Denmark (da, Danmark), Djibouti (so, Jabuuti), Dominican Republic (es, República Dominicana), Ecuador, Egypt (ar, ‎مصر‎), El Salvador, Eritrea (ti, ኤርትራ), Estonia (et, Eesti), Ethiopia (am, ኢትዮጵያ; om, Itoophiyaa; so, Itoobiya; ti, ኢትዮጵያ), Faroe Islands (fo, Føroyar), Finland (fi, Suomi; sv), France, Germany (de, Deutschland), Greece (el, Ελλάδα), Greenland (kl, Kalaallit Nunaat), Guatemala, Honduras, Hong Kong S.A.R. China (en; zh_Hant, 中華人民共和國香港特別行政區), Hungary (hu, Magyarország), Iceland (is, Ísland), India (ar, ‎الهند‎; bn, ভারত; en; gu, ભારત; hi, भारत; kn, ಭಾರತ; kok, भारत; mr, भारत; pa, ਭਾਰਤ; ta, இந்தியா; te, భారత దేళ౦), Indonesia, Iran (fa, ‎ایران‎), Iraq (ar, ‎العراق‎), Ireland (en; ga, Éire), Israel (he, ‎ישראל‎), Italy (it, Italia), Japan (ja, 日本), Jordan (ar, ‎الاردن‎), Kazakhstan (kk, Қазақстан), Kenya (om, Keeniyaa; so, Kiiniya; sw), Kuwait (ar, ‎الكويت‎), Latvia (lv, Latvija), Lebanon (ar, ‎لبنان‎), Libya (ar, ‎ليبيا‎), Lithuania (lt, Lietuva), Luxembourg (de, Luxemburg; fr), Macao S.A.R. China (zh_Hant, 澳門特別行政區), Macedonia (mk, Македонија), Malaysia, Malta (en; mt), Mexico (es, México), Morocco (ar, ‎المغرب‎), Netherlands (nl, Nederland), New Zealand, Nicaragua, Norway (nb, Norge; nn, Noreg), Oman (ar, ‎عمان‎), Panama (es, Panamá), Paraguay, Peru (es, Perú), Philippines, Poland (pl, Polska), Portugal, Puerto Rico, Qatar (ar, ‎قطر‎), Romania (ro, România), Russia (ru, Россия), Saudi Arabia (ar, ‎العربية السعودية‎), Serbia and Montenegro (sr_Latn, Srbija I Crna Gora; sr, Србија И Црна Гора), Singapore (en; zh, 新加坡), Slovakia (sk, Slovenská Republika), Slovenia (sl, Slovenija), Somalia (so, Soomaaliya), South Africa (af, Suid-Afrika; en), South Korea (ko, 대한민국), Spain (ca, Espanya; es, España; eu, Espainia; gl, España), Sudan (ar, ‎السودان‎), Sweden (sv, Sverige), Switzerland (de, Schweiz; fr, Suisse; it, Svizzera), Syria (ar, ‎سورية‎), Taiwan (zh_Hant, 臺灣), Tanzania, Thailand (th, ประเทศไทย), Tunisia (ar, ‎تونس‎), Turkey (tr, Türkiye), U.S. Virgin Islands, Ukraine (ru, Украина; uk, Україна), United Arab Emirates (ar, ‎الامارات العربية المتحدة‎), United Kingdom (cy, Prydain Fawr; en; gv, Rywvaneth Unys; kw, Rywvaneth Unys), United States (en; es, Estados Unidos), Uruguay, Venezuela, Vietnam (vi, Việt Nam), Yemen (ar, ‎اليمن‎), Zimbabwe (zw)

**Draft languages:** Afar (aa; aa_ER_SAAHO), Assamese, Azerbaijani, Blin, Divehi, Dzongkha, Geez, Georgian, Hawaiian, Inuktitut, Khmer, Kirghiz, Lao, Malayalam (ml, മലയാളം), Mongolian, Oriya (or, ଓଡ଼ିଆ), Sanskrit, Sidamo, Syriac, Tatar, Tigre, Urdu, Uzbek, Walamo

Notes:

- Territories with multiple locales may appear more than once.
- Tooltips will show the language/territory code, English name, and Latin transliteration. (To set your tooltip font, right click on the desktop, pick Properties>Appearance>Advanced>Item: ToolTip, then set the font to Arial Unicode MS or other large font.)
- If the above doesn't display in your browser, see [Display Problems?]

[Bug Reports]: /requesting_changes
[Display Problems?]: https://www.unicode.org/help/display_problems.html
<!-- 1.2 release: 2004-11-04 -->
[v1.2]: /downloads/cldr-1-2
[CLDR1.2]: https://unicode.org/Public/cldr/1.2.0/
[LDML1.2]: https://www.unicode.org/reports/tr35/tr35-4.html
[~~Δ1.2~~]: https://unicode.org/cldr/trac/query?status=closed&col=id&col=summary&milestone=1.2
[release-1-2]: https://github.com/unicode-org/cldr/tree/release-1-2
