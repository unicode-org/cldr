package org.unicode.cldr.draft;

import com.ibm.icu.text.UnicodeSet;

public class ScriptCategories {

  public static final UnicodeSet IPA = (UnicodeSet) new UnicodeSet(
          "[a-zæçðøħŋœǀ-ǃɐ-ɨɪ-ɶ ɸ-ɻɽɾʀ-ʄʈ-ʒʔʕʘʙʛ-ʝʟʡʢ ʤʧʰ-ʲʴʷʼˈˌːˑ˞ˠˤ̀́̃̄̆̈ ̘̊̋̏-̜̚-̴̠̤̥̩̪̬̯̰̹-̽͜ ͡βθχ↑-↓↗↘]"
  ).freeze();
  // From: http://www.phon.ucl.ac.uk/home/wells/ipa-unicode.htm
  
  // TODO - change to Blocks
  public static final UnicodeSet ARCHAIC = (UnicodeSet) new UnicodeSet(
          "[[:script=Bugi:][:script=Buhd:][:script=Cari:][:script=Copt:]" +
          "[:script=Cprt:][:script=Dsrt:][:script=Glag:][:script=Goth:][:script=Hano:][:script=Ital:][:script=Khar:][:script=Linb:]" +
          "[:script=Lyci:][:script=Lydi:][:script=Ogam:][:script=Osma:][:script=Phag:][:script=Phnx:][:script=Rjng:][:script=Runr:]" +
          "[:script=Shaw:][:script=Sund:][:script=Sylo:][:script=Syrc:][:script=Tagb:][:script=Tglg:][:script=Ugar:][:script=Xpeo:][:script=Xsux:]" +
          "[:block=Aegean_Numbers:][:block=Ancient_Symbols:][:block=Phaistos_Disc:][:block=Cuneiform Numbers and Punctuation:]" +
          "[:block=Byzantine Musical Symbols:][:block=Ancient Greek Musical Notation:][:Block=Ancient_Greek_Numbers:]" + 
          "[[:HST=L:][:HST=V:][:HST=T:]-[\u1100-\u1112 \u1161-\u1175 \u11A8-\u11C2]]" +
          "[\u317F \u3181 \u3186 \u3164\uFFA0 \u318D \u3183]" +
          "[ɩɷɼɿʅ-ʇʓʖʗʚʞʠʣʥʦʨ-ʯ]" +
          "[֢ ׅ ̓ ᷀-᷃ ҃-҆ ׇ ៑ ៝ ׆ ꜀-꜇ ɟ ƪ ƾ ƫ ƍ ƹ ƺ ȝ Ȝ ƿ ƻ ϐ ϝϜ ϛϚ ϑ ϗ ϖ ϻ Ϻ ϟϞ ϙϘ Ϲ ϕ ϡϠ ϸϷ ჱ-ჶ ٮ ڎ ٯ ೞ ឨ \u115A-\u115E \u11A3-\u11A7 \u11FA-\u11FF]" +
          "[:block=Hangul_Compatibility_Jamo:][:block=Halfwidth_And_Fullwidth_Forms:]" +
          "[ [:blk=Syriac:] [:blk=Ogham:] [:blk=Runic:] [:blk=Hangul_Compatibility_Jamo:] " +
          "[:blk=Halfwidth_And_Fullwidth_Forms:] [:blk=Old_Italic:] [:blk=Gothic:] [:blk=Deseret:] " +
          "[:blk=Byzantine_Musical_Symbols:] [:blk=Tagalog:] [:blk=Hanunoo:] [:blk=Buhid:] [:blk=Tagbanwa:]" +
          " [:blk=Linear_B_Syllabary:] [:blk=Linear_B_Ideograms:] [:blk=Aegean_Numbers:] [:blk=Ugaritic:]" +
          " [:blk=Shavian:] [:blk=Osmanya:] [:blk=Cypriot_Syllabary:] [:blk=Ancient_Greek_Musical_Notation:]" +
          " [:blk=Ancient_Greek_Numbers:] [:blk=Buginese:] [:blk=Coptic:] [:blk=Glagolitic:] [:blk=Kharoshthi:]" +
          " [:blk=Old_Persian:] [:blk=Syloti_Nagri:] [:blk=Phags_Pa:] [:blk=Phoenician:] [:blk=Cuneiform:] " +
          "[:blk=Cuneiform_Numbers_And_Punctuation:] [:blk=Sundanese:] [:blk=Rejang:] [:blk=Ancient_Symbols:]" +
          " [:blk=Phaistos_Disc:] [:blk=Lycian:] [:blk=Carian:] [:blk=Lydian:] [:sc=Copt:]" +
          "[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u0269\u0277\u027C\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u029E\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u07E8-\u07EA\u0CDE\u10F1-\u10F6\u1113-\u1159\u115F\u1160\u1176-\u11A2\u11C3-\u11F9\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA700-\uA707\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]]" +
          "]"
  ).freeze();
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
