package org.unicode.cldr.draft;

import com.ibm.icu.text.UnicodeSet;

public class ScriptCategories {

  // TODO - change to Blocks
  public static final UnicodeSet ARCHAIC = (UnicodeSet) new UnicodeSet(
          "[[:script=Bugi:][:script=Buhd:][:script=Cari:][:script=Copt:]" +
          "[:script=Cprt:][:script=Dsrt:][:script=Glag:][:script=Goth:][:script=Hano:][:script=Ital:][:script=Khar:][:script=Linb:]" +
          "[:script=Lyci:][:script=Lydi:][:script=Ogam:][:script=Osma:][:script=Phag:][:script=Phnx:][:script=Rjng:][:script=Runr:]" +
          "[:script=Shaw:][:script=Sund:][:script=Sylo:][:script=Syrc:][:script=Tagb:][:script=Tglg:][:script=Ugar:][:script=Xpeo:][:script=Xsux:]" +
          "[:block=Aegean_Numbers:][:block=Ancient_Symbols:][:block=Phaistos_Disc:][:block=Cuneiform Numbers and Punctuation:]" +
          "[:block=Byzantine Musical Symbols:][:block=Ancient Greek Musical Notation:]" + 
          "[[:HST=L:][:HST=V:][:HST=T:]-[\u1100-\u1112 \u1161-\u1175 \u11A8-\u11C2]]" +
          "[\u317F \u3181 \u3186 \u3164\uFFA0 \u318D \u3183]" +
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
