package org.unicode.cldr.draft;

import com.ibm.icu.text.UnicodeSet;

public class ScriptCategories {

  // From: http://www.phon.ucl.ac.uk/home/wells/ipa-unicode.htm
  public static final UnicodeSet IPA = (UnicodeSet) new UnicodeSet(
          "[a-zæçðøħŋœǀ-ǃɐ-ɨɪ-ɶ ɸ-ɻɽɾʀ-ʄʈ-ʒʔʕʘʙʛ-ʝʟʡʢ ʤʧʰ-ʲʴʷʼˈˌːˑ˞ˠˤ̀́̃̄̆̈ ̘̊̋̏-̜̚-̴̠̤̥̩̪̬̯̰̹-̽͜ ͡βθχ↑-↓↗↘]"
  ).freeze();
  
  public static final UnicodeSet IPA_EXTENSIONS = (UnicodeSet) new UnicodeSet(
          "[ɩɷɼɿʅ-ʇʓʖʗʚʞʠʣʥʦʨ-ʯ]"
  ).freeze();
  

  public static final UnicodeSet DEPRECATED_NEW = (UnicodeSet) new UnicodeSet("[[:deprecated:][\\u0149\\u0F77\\u0F79\\u17A4\\u2329\\u232A]-[\\u0340\\u0341\\u17D3]]").freeze();
  //removing 0340, 0341, 17D3, and adding 0149, 0F77, 0F79, 17A4, 2329, 232A

  // TODO - change to Blocks
  public static final UnicodeSet ARCHAIC_31  = (UnicodeSet) new UnicodeSet(
          //          "[[:script=Bugi:][:script=Buhd:][:script=Cari:][:script=Copt:]" +
          //          "[:script=Cprt:][:script=Dsrt:][:script=Glag:][:script=Goth:][:script=Hano:][:script=Ital:][:script=Khar:][:script=Linb:]" +
          //          "[:script=Lyci:][:script=Lydi:][:script=Ogam:][:script=Osma:][:script=Phag:][:script=Phnx:][:script=Rjng:][:script=Runr:]" +
          //          "[:script=Shaw:][:script=Sund:][:script=Sylo:][:script=Syrc:][:script=Tagb:][:script=Tglg:][:script=Ugar:][:script=Xpeo:][:script=Xsux:]" +
          //          "[:block=Ancient_Greek_Musical_Notation:][:block=Phaistos_Disc:]]"
          "[ [:blk=Ancient_Greek_Musical_Notation:] [:blk=Buginese:] [:blk=Buhid:] [:blk=Carian:] [:blk=Coptic:] [:blk=Cuneiform:] [:blk=Cuneiform_Numbers_And_Punctuation:] [:blk=Cypriot_Syllabary:] [:blk=Deseret:] [:blk=Glagolitic:] [:blk=Gothic:] [:blk=Hanunoo:] [:blk=Kharoshthi:] [:blk=Linear_B_Ideograms:] [:blk=Linear_B_Syllabary:] [:blk=Lycian:] [:blk=Lydian:] [:blk=Ogham:] [:blk=Old_Italic:] [:blk=Old_Persian:] [:blk=Osmanya:] [:blk=Phags_Pa:] [:blk=Phaistos_Disc:] [:blk=Phoenician:] [:blk=Rejang:] [:blk=Runic:] [:blk=Shavian:] [:blk=Sundanese:] [:blk=Syloti_Nagri:] [:blk=Syriac:] [:blk=Tagalog:] [:blk=Tagbanwa:] [:blk=Ugaritic:] [:sc=Copt:]]"
  ).freeze();
  // from the old version of UTS39
  public static final UnicodeSet ARCHAIC_39 = (UnicodeSet) new UnicodeSet(
          //  		"[\\u018D\\u01AA-\\u01AB\\u01B9-\\u01BB\\u01BE\\u01BF\\u021C-\\u021D\\u025F\\u0277\\u027C\\u029E\\u0343" +
          //      "\\u03D0-\\u03D1\\u03D5-\\u03E1\\u03F7-\\u03F8\\u03F9-\\u03FB\\u0483-\\u0486\\u05A2\\u05C5-\\u05C7\\u066E-\\u066F\\u068E\\u0CDE\\u10F1-\\u10F6\\u1100-\\u1159" +
          //      "\\u115A-\\u115E\\u1161-\\u11A2\\u11A3-\\u11A7\\u11A8-\\u11F9\\u11FA-\\u11FF\\u1680-\\u169A\\u16A0-\\u16EA\\u16EE-\\u16F0\\u1700-\\u170C\\u170E-\\u1714" +
          //      "\\u1720-\\u1734\\u1740-\\u1753\\u1760-\\u176C\\u176E-\\u1770\\u1772-\\u1773\\u17A8\\u17D1\\u17DD\\u1B00-\\u1B4B\\u1B50-\\u1B7C\\u1DC0-\\u1DC3" +
          //      "\\u2C00-\\u2C2E\\u2C30-\\u2C5E\\u3165-\\u318E\\uA700-\\uA707\\uA840-\\uA877"+
          //  		"\\U00010000-\\U0001000B\\U0001000D-\\U00010026\\U00010028-\\U0001003A\\U0001003C-\\U0001003D\\U0001003F-\\U0001004D" +
          //  		"\\U00010050-\\U0001005D\\U00010080-\\U000100FA\\U00010140-\\U00010174\\U00010300-\\U0001031E\\U00010330-\\U0001034A" +
          //  		"\\U00010380-\\U0001039D\\U0001039F-\\U000103C3\\U000103C8-\\U000103D5\\U00010400-\\U0001049D\\U000104A0-\\U000104A9" +
          //  		"\\U00010800-\\U00010805\\U00010808\\U0001080A-\\U00010835\\U00010837-\\U00010838\\U0001083C\\U0001083F\\U00010900-\\U00010919" +
          //  		"\\U0001091F\\U00010A00-\\U00010A03\\U00010A05-\\U00010A06\\U00010A0C-\\U00010A13\\U00010A15-\\U00010A17\\U00010A19-\\U00010A33" +
          //  		"\\U00010A38-\\U00010A3A\\U00010A3F-\\U00010A47\\U00010A50-\\U00010A58\\U00012000-\\U0001236E\\U00012400-\\U00012462\\U00012470-\\U00012473]"
          "[ " +
          //"[:blk=Balinese:] " +
          "[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]]"
  ).freeze();

  public static final UnicodeSet ARCHAIC_HEURISTIC = (UnicodeSet) new UnicodeSet(
          "[ [:blk=Ancient_Symbols:] [:blk=Ancient_Greek_Numbers:] [\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]]"
  ).freeze();
  
  public static final UnicodeSet ARCHAIC_ADDITIONS = (UnicodeSet) new UnicodeSet(
          "[ [:blk=Aegean_Numbers:] [:blk=Byzantine_Musical_Symbols:] [\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]]"
  ).freeze();

  public static final UnicodeSet ARCHAIC = (UnicodeSet) new UnicodeSet(ARCHAIC_31).addAll(ARCHAIC_39).addAll(ARCHAIC_HEURISTIC).addAll(ARCHAIC_ADDITIONS).freeze();

  public static final UnicodeSet EUROPEAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Latin:][:script=Greek:][:script=Coptic:][:script=Cyrillic:]" +
          "[:script=Glag:][:script=Armenian:][:script=Georgian:][:script=Shavian:][:script=braille:]" +
          "[:script=ogham:][:script=runic:][:script=Gothic:][:script=Cypriot:][:script=Linear b:]" +
          "[:script=old italic:]]"
  ).freeze();
  public static final UnicodeSet MIDDLE_EASTERN = (UnicodeSet) new UnicodeSet(
          "[[:script=Hebrew:][:script=Arabic:][:script=Syriac:][:script=Thaana:]" +
          "[:script=Carian:][:script=Lycian:][:script=Lydian:][:script=Phoenician:]" +
          "[:script=Cuneiform:][:script=old persian:][:ugaritic:]]"
  ).freeze();
  public static final UnicodeSet SOUTH_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Devanagari:][:script=Bengali:][:script=Gurmukhi:][:script=Gujarati:]" +
          "[:script=Oriya:][:script=Tamil:][:script=Telugu:][:script=Kannada:][:script=Malayalam:]" +
          "[:script=Sinhala:][:script=Tibetan:][:script=Phags-Pa:][:script=Limbu:][:script=Sylo:][:script=Kharoshthi:][:script=lepcha:][:saurashtra:][:script=ol chiki:]]"
  ).freeze();
  public static final UnicodeSet SOUTHEAST_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Thai:][:script=Lao:][:script=Myanmar:][:script=Khmer:]" +
          "[:script=Tai_Le:][:script=New Tai Lue:][:script=Tagalog:][:script=Hanunoo:][:script=Buhid:]" +
          "[:script=Tagbanwa:][:script=Buginese:][:script=Balinese:][:script=Cham:][:script=kayah li:][:script=rejang:][:script=sundanese:]]"
  ).freeze();
  public static final UnicodeSet EAST_ASIAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Bopomofo:][:script=Hiragana:][:script=Katakana:][:script=Mongolian:]" +
          "[:script=Yi:]]"
  ).freeze();
  public static final UnicodeSet AFRICAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Ethiopic:][:script=Osmanya:][:script=Tifinagh:]" +
          "[:script=Nko:][:script=vai:]]"
  ).freeze();
  public static final UnicodeSet AMERICAN = (UnicodeSet) new UnicodeSet(
          "[[:script=Cherokee:][:script=CANS:][:script=Deseret:]]"
  ).freeze();
  public static final UnicodeSet OTHER_SCRIPTS = (UnicodeSet) new UnicodeSet("[^[:script=common:][:script=inherited:]]")
  .removeAll(EUROPEAN)
  .removeAll(MIDDLE_EASTERN)
  .removeAll(SOUTH_ASIAN)
  .removeAll(SOUTHEAST_ASIAN)
  .removeAll(EAST_ASIAN)
  .removeAll(AFRICAN)
  .removeAll(AMERICAN)
  .removeAll(new UnicodeSet("[[:script=han:][:script=hangul:]]"))
  .freeze();

}
