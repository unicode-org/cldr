---
title: ISO 636 Deprecation Requests - DRAFT
---

# ISO 636 Deprecation Requests - DRAFT

We have become aware over time of cases where ISO 639 inaccurately assigns different language codes to the same language. Its goal is to distinguish all and only those languages that are are not mutually comprehensible. Making too many distinctions can be as harmful as making too few, since it artificially separates two dialects, and disrupts the ability of software to identify them as variants. The remedy used in the past has been to deprecate codes: for example, [mol (mo)](http://www.sil.org/iso639-3/documentation.asp?id=mol) has been merged with [rol (ro)](http://www.sil.org/iso639-3/documentation.asp?id=rol). See also [Picking the Right Language Code](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code) and [Language Distance Data](https://cldr.unicode.org/development/development-process/design-proposals/language-distance-data).

The current cases in question are listed below. However we need to collate and organize a background document of information before we go further.

| Codes | Alternates | Comments |  Recomended disposition |
|---|---|---|---|
| [aka (ak)](http://www.sil.org/iso639-3/documentation.asp?id=aka) Akan | [fat](http://www.sil.org/iso639-3/documentation.asp?id=fat) Fanti; [twi](http://www.sil.org/iso639-3/documentation.asp?id=twi) Twi | Sources in Africa confirm what [wikipedia](http://en.wikipedia.org/wiki/Akan_language) says: that Fanti and Twi are mutually comprehensible, and both are considered Akan. | Deprecate 'fat' and 'twi'; add the names "Fanti" and "Twi" to 'aka' |
| [fas (fa)](http://www.sil.org/iso639-3/documentation.asp?id=fas) Persian | [pes](http://www.sil.org/iso639-3/documentation.asp?id=pes) Western Farsi; [prs](http://www.sil.org/iso639-3/documentation.asp?id=prs) Dari | Again, native speakers confirm that Dari and Farsi are mutually comprehensible, and Dari is simply the name given to Farsi in Afganistan and other places. That is, in RFC 4646 parlance, Dari and Western Farsi are as close as, es-ES and es-AR; fa-AF and prs are essentially synonyms. | Deprecate 'pes' and 'prs'; add the names to 'fas' |
| [tgl (tl)](https://479453595-atari-embeds.googleusercontent.com/embeds/16cb204cf3a9d4d223a0a3fd8b0eec5d/goog_1243893892557) Tagalog | [fil](http://www.sil.org/iso639-3/documentation.asp?id=fil) Filipino | These are widely recognized to be mutually comprehensible. There appear to be only political reasons for separating them. See http://en.wikipedia.org/wiki/Filipino_language , which is corrobborated by our native speaker contacts. | Deprecate 'fil'; adding the name "Filipino" to 'tgl' |
| [hbs](http://www.sil.org/iso639-3/documentation.asp?id=hbs) (sh) Serbo-Croatian | [bos](http://www.sil.org/iso639-3/documentation.asp?id=bos) (bs) Bosnian; [hrv](http://www.sil.org/iso639-3/documentation.asp?id=hrv) (hr) Croatian; [srp](http://www.sil.org/iso639-3/documentation.asp?id=srp) (sr) Serbian | These are all mutually comprehensible according to many native speakers. | Ideally, we would deprecate bos, hrv, srp; add the names to 'hbs'; however, there is probably too much installed base to do this. |


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)