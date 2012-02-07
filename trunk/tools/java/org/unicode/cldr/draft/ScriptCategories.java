package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.VersionInfo;

public class ScriptCategories {

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_MAIN = false;

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
          "[ [:blk=Ancient_Greek_Musical_Notation:]" +
          "[:blk=Buginese:] " +
          "[:blk=Buhid:] [:blk=Carian:] " +
          "[:blk=Coptic:] [:blk=Cuneiform:] " +
          "[:blk=Cuneiform_Numbers_And_Punctuation:] " +
          "[:blk=Cypriot_Syllabary:] [:blk=Deseret:] [:blk=Glagolitic:] " +
          "[:blk=Gothic:] [:blk=Hanunoo:] [:blk=Kharoshthi:] [:blk=Linear_B_Ideograms:] " +
          "[:blk=Linear_B_Syllabary:] [:blk=Lycian:] [:blk=Lydian:] [:blk=Ogham:]" +
          "[:blk=Old_Italic:] [:blk=Old_Persian:] [:blk=Osmanya:] [:blk=Phags_Pa:] " +
          "[:blk=Phaistos_Disc:] [:blk=Phoenician:] [:blk=Rejang:] [:blk=Runic:] " +
          "[:blk=Shavian:] [:blk=Sundanese:] [:blk=Syloti_Nagri:] [:blk=Syriac:] " +
          "[:blk=Tagalog:] [:blk=Tagbanwa:] [:blk=Ugaritic:] [:sc=Copt:]]"
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
          "[:blk=Ancient_Greek_Numbers:]" +
          "[:Block=Hangul_Jamo:]" +
          "[:Block=Hangul_Compatibility_Jamo:]" +
          "[֢ ׅ ̓ ᷀-᷃ ҃-҆ ׇ ៑ ៝ ׆ ꜀-꜇ ɟ ʞ ɷ ɼ ƪ ƾ ƫ ƍ ƹ ƺ ȝȜ ƿ ƻ ϐ ϝϜ ϛϚ ϑ ϗ ϖ ϻϺ ϟϞ ϙϘ Ϲ ϕ ϡϠ ϸ Ϸ ჱ-ჶ ٮ ڎ ٯ ೞ ឨ]" +
          "]"
          //"[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]]"
  ).freeze();

  public static final UnicodeSet ARCHAIC_HEURISTIC = (UnicodeSet) new UnicodeSet(
          "[ " +
          "[:blk=Ancient_Symbols:]" +
          "[:blk=Ancient_Greek_Musical_Notation:] " +
          "[:blk=Cyrillic_Extended_A:] " +
          "[:blk=Cyrillic_Extended_B:]" +
          "[˯-˿ͣ-ͳͶͷߨ-ߪ᷎-᷿ᷦ᷾ẜẝẟ Ỻ-ỿ⁖⁘-⁞ↀ-Ↄↅ-ↈⱷ-ⱽ⸀-⸗⸪-⸰ ꜠꜡ꜰ-ꝸꟻ-ꟿ[ݾ ݿ ػ-ؿ]]" +
          "]"
          //"[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]]"
  ).freeze();

  public static final UnicodeSet ARCHAIC_ADDITIONS = (UnicodeSet) new UnicodeSet(
          "[ " +
          "[:blk=Aegean_Numbers:] " +
          "[:blk=Byzantine_Musical_Symbols:] " +
          "[:block=Georgian Supplement:]" +
          "[ͻ-ͽϏϽ-Ͽ[ƨ ƽ ƅ][ؕ-ؚ ۖ-ۤ ۧ ۨ ۪-ۭ ۩ ۥ ۦ][֑-֯][ׄ ׅ][ﬠ-ﬨ][ﭏ][Ⴀ-Ⴆ Ⴡ Ⴇ-Ⴌ Ⴢ Ⴍ-Ⴒ Ⴣ Ⴓ-Ⴞ Ⴤ Ⴟ Ⴠ Ⴥ][Ⴀ-Ⴥ][ƄƧƸƼǷϲϴↄ]჻]" +
          "]"
          // "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]]"
  ).freeze();

  public static final UnicodeSet ARCHAIC = (UnicodeSet) new UnicodeSet(ARCHAIC_31)
  .addAll(ARCHAIC_39)
  .addAll(ARCHAIC_HEURISTIC)
  .addAll(ARCHAIC_ADDITIONS)
  .removeAll(IPA)
  .removeAll(IPA_EXTENSIONS)
  .freeze();
  static {
    UnicodeSet knownOk = new UnicodeSet("[\u0392\u0398\u03A0\u03A6\u03B2\u03B8\u03C0\u03C6]");
    final UnicodeSet caseProblems = new UnicodeSet(ARCHAIC).closeOver(UnicodeSet.CASE).removeAll(ARCHAIC).removeAll(knownOk);
    if (caseProblems.size() != 0) {
      throw new IllegalArgumentException("Case: " + caseProblems);
    }
  }


  static final Map<String, Integer> RADICAL_NUM2CHAR;
  static final Map<Integer, String> RADICAL_CHAR2NUM;
  static final Map<Integer, Integer> RADICAL_CHAR2STROKES;

  static {
    Map<String, Integer> num2char = new LinkedHashMap();
    Map<Integer, String> char2num = new LinkedHashMap();
    Map<Integer, Integer> char2strokes = new LinkedHashMap();

    String[][] radicalData = {
            {"1","一","1"},
            {"2","丨","1"},
            {"3","丶","1"},
            {"4","丿","1"},
            {"5","乙","1"},
            {"6","亅","1"},
            {"7","二","2"},
            {"8","亠","2"},
            {"9","人","2"},
            {"10","儿","2"},
            {"11","入","2"},
            {"12","八","2"},
            {"13","冂","2"},
            {"14","冖","2"},
            {"15","冫","2"},
            {"16","几","2"},
            {"17","凵","2"},
            {"18","刀","2"},
            {"19","力","2"},
            {"20","勹","2"},
            {"21","匕","2"},
            {"22","匚","2"},
            {"23","匸","2"},
            {"24","十","2"},
            {"25","卜","2"},
            {"197'","卤","7"},
            {"26","卩","2"},
            {"27","厂","2"},
            {"28","厶","2"},
            {"29","又","2"},
            {"30","口","3"},
            {"31","囗","3"},
            {"32","土","3"},
            {"33","士","3"},
            {"34","夂","3"},
            {"35","夊","3"},
            {"36","夕","3"},
            {"37","大","3"},
            {"38","女","3"},
            {"39","子","3"},
            {"40","宀","3"},
            {"41","寸","3"},
            {"42","小","3"},
            {"43","尢","3"},
            {"44","尸","3"},
            {"45","屮","3"},
            {"46","山","3"},
            {"47","巛","3"},
            {"48","工","3"},
            {"49","己","3"},
            {"50","巾","3"},
            {"51","干","3"},
            {"52","幺","3"},
            {"53","广","3"},
            {"54","廴","3"},
            {"55","廾","3"},
            {"56","弋","3"},
            {"57","弓","3"},
            {"58","彐","3"},
            {"59","彡","3"},
            {"60","彳","3"},
            {"61","心","4"},
            {"62","戈","4"},
            {"63","戶","4"},
            {"64","手","4"},
            {"65","支","4"},
            {"66","攴","4"},
            {"67","文","4"},
            {"68","斗","4"},
            {"69","斤","4"},
            {"70","方","4"},
            {"71","无","4"},
            {"72","日","4"},
            {"73","曰","4"},
            {"74","月","4"},
            {"75","木","4"},
            {"76","欠","4"},
            {"77","止","4"},
            {"78","歹","4"},
            {"79","殳","4"},
            {"80","毋","4"},
            {"81","比","4"},
            {"82","毛","4"},
            {"83","氏","4"},
            {"84","气","4"},
            {"85","水","4"},
            {"86","火","4"},
            {"87","爪","4"},
            {"88","父","4"},
            {"89","爻","4"},
            {"90","爿","4"},
            {"91","片","4"},
            {"92","牙","4"},
            {"93","牛","4"},
            {"94","犬","4"},
            {"95","玄","5"},
            {"96","玉","5"},
            {"97","瓜","5"},
            {"98","瓦","5"},
            {"99","甘","5"},
            {"100","生","5"},
            {"101","用","5"},
            {"102","田","5"},
            {"103","疋","5"},
            {"104","疒","5"},
            {"105","癶","5"},
            {"106","白","5"},
            {"107","皮","5"},
            {"108","皿","5"},
            {"109","目","5"},
            {"110","矛","5"},
            {"111","矢","5"},
            {"112","石","5"},
            {"113","示","5"},
            {"114","禸","5"},
            {"115","禾","5"},
            {"116","穴","5"},
            {"117","立","5"},
            {"118","竹","6"},
            {"119","米","6"},
            {"120","糸","6"},
            {"120'","纟","3"},
            {"121","缶","6"},
            {"122","网","6"},
            {"123","羊","6"},
            {"124","羽","6"},
            {"125","老","6"},
            {"126","而","6"},
            {"127","耒","6"},
            {"128","耳","6"},
            {"129","聿","6"},
            {"130","肉","6"},
            {"131","臣","6"},
            {"132","自","6"},
            {"133","至","6"},
            {"134","臼","6"},
            {"135","舌","6"},
            {"136","舛","6"},
            {"137","舟","6"},
            {"138","艮","6"},
            {"139","色","6"},
            {"140","艸","6"},
            {"141","虍","6"},
            {"142","虫","6"},
            {"143","血","6"},
            {"144","行","6"},
            {"145","衣","6"},
            {"146","襾","6"},
            {"147","見","7"},
            {"147'","见","4"},
            {"148","角","7"},
            {"149","言","7"},
            {"149'","讠","2"},
            {"150","谷","7"},
            {"151","豆","7"},
            {"152","豕","7"},
            {"153","豸","7"},
            {"154","貝","7"},
            {"154'","贝","4"},
            {"155","赤","7"},
            {"156","走","7"},
            {"157","足","7"},
            {"158","身","7"},
            {"159","車","7"},
            {"159'","车","4"},
            {"160","辛","7"},
            {"161","辰","7"},
            {"162","辵","7"},
            {"163","邑","7"},
            {"164","酉","7"},
            {"165","釆","7"},
            {"166","里","7"},
            {"167","金","8"},
            {"167'","钅","5"},
            {"168","長","8"},
            {"168'","长","5"},
            {"169","門","8"},
            {"169'","门","3"},
            {"170","阜","8"},
            {"171","隶","8"},
            {"172","隹","8"},
            {"173","雨","8"},
            {"174","靑","8"},
            {"175","非","8"},
            {"176","面","9"},
            {"177","革","9"},
            {"178","韋","9"},
            {"178'","韦","4"},
            {"179","韭","9"},
            {"180","音","9"},
            {"181","頁","9"},
            {"181'","页","6"},
            {"182","風","9"},
            {"182'","风","4"},
            {"183","飛","9"},
            {"183'","飞","3"},
            {"184","食","9"},
            {"184'","饣","3"},
            {"185","首","9"},
            {"186","香","9"},
            {"187","馬","10"},
            {"187'","马","3"},
            {"188","骨","10"},
            {"189","高","10"},
            {"190","髟","10"},
            {"191","鬥","10"},
            {"192","鬯","10"},
            {"193","鬲","10"},
            {"194","鬼","10"},
            {"195","魚","11"},
            {"195'","鱼","8"},
            {"196","鳥","11"},
            {"196'","鸟","5"},
            {"197","鹵","11"},
            {"198","鹿","11"},
            {"199","麥","11"},
            {"199'","麦","11"},
            {"200","麻","11"},
            {"201","黃","12"},
            {"202","黍","12"},
            {"203","黑","12"},
            {"204","黹","12"},
            {"205","黽","13"},
            {"205'","黾","13"},
            {"206","鼎","13"},
            {"207","鼓","13"},
            {"208","鼠","13"},
            {"209","鼻","14"},
            {"210","齊","14"},
            {"210'","齐","6"},
            {"211","齒","15"},
            {"211'","齿","8"},
            {"212","龍","16"},
            {"212'","龙","5"},
            {"213","龜","16"},
            {"213'","龟","7"},
            {"214","龠","17"},
    };

    for (String[] pair : radicalData) {
      final int radicalChar = pair[1].codePointAt(0);
      num2char.put(pair[0], radicalChar);
      char2num.put(radicalChar, pair[0]);
      char2strokes.put(radicalChar, Integer.parseInt(pair[2]));
    }
    // Protect and assign
    RADICAL_NUM2CHAR = Collections.unmodifiableMap(num2char);
    RADICAL_CHAR2NUM = Collections.unmodifiableMap(char2num);
    RADICAL_CHAR2STROKES  = Collections.unmodifiableMap(char2strokes);
  }

  // START OF GENERATED CODE

  public static final UnicodeSet SCRIPT_CHANGED = (UnicodeSet) new UnicodeSet("[\\^`\\u00A8\\u00AF\\u00B4\\u00B5\\u00B8\\u02B9-\\u02DF\\u02E5-\\u02FF\\u0374\\u0375\\u037E\\u0385\\u0387\\u03F6\\u0589\\u0600-\\u0603\\u060C\\u061B\\u061F\\u0640\\u064B-\\u0655\\u0660-\\u0669\\u0670\\u06DD\\u0951\\u0952\\u0964\\u0965\\u0970\\u0CF1\\u0CF2\\u10FB\\u16EB-\\u16ED\\u1735\\u1736\\u1802\\u1803\\u1805\\u1D26-\\u1D2B\\u1D5D-\\u1D61\\u1D66-\\u1D6A\\u1D78\\u1DBF\\u2100-\\u2125\\u2127\\u2128\\u212C-\\u2131\\u2133\\u2134\\u2139-\\u213B\\u2145-\\u214A\\u214C\\u214D\\u249C-\\u24E9\\u2FF0-\\u2FFF\\u3001-\\u3004\\u3006\\u3008-\\u3020\\u302A-\\u3037\\u303C-\\u303F\\u3099-\\u309C\\u30A0\\u30FB\\u30FC\\u3190-\\u319F\\u31C0-\\u31E3\\u3220-\\u3243\\u3250\\u327F-\\u32B0\\u32C0-\\u32CF\\u3358-\\u33FF\\uA700-\\uA721\\uA788-\\uA78A\\uFDFD\\uFE45\\uFE46\\uFF61-\\uFF65\\uFF70\\uFF9E\\uFF9F]").freeze();
  public static final Map<String,UnicodeSet> SCRIPT_NEW;
  static {
  String[][] data = {
    {"Arabic", "[\\u0600-\\u0603\\u060C\\u061B\\u061F\\u0640\\u064B-\\u0655\\u0660-\\u0669\\u0670\\uFDFD]"},
    {"Armenian", "[\\u0589]"},
    {"Bengali", "[\\u0964\\u0965\\u0CF1\\u0CF2]"},
    {"Bopomofo", "[\\u02EA\\u02EB\\u3001-\\u3004\\u3006\\u3008-\\u3011\\u3013-\\u3020\\u302A-\\u302D\\u3030\\u3037\\u303C-\\u303F\\uFE45\\uFE46\\uFF61-\\uFF64]"},
    {"Buhid", "[\\u1735\\u1736]"},
    {"Common", "[\\u03F6\\u06DD]"},
    {"Cyrillic", "[\\u02BC]"},
    {"Devanagari", "[\\u0951\\u0952\\u0964\\u0965\\u0970\\u0CF1\\u0CF2]"},
    {"Georgian", "[\\u0589\\u10FB]"},
    {"Greek", "[\\u00B5\\u0374\\u0375\\u037E\\u0385\\u0387]"},
    {"Gujarati", "[\\u0CF1\\u0CF2]"},
    {"Gurmukhi", "[\\u0964\\u0965\\u0CF1\\u0CF2]"},
    {"Han", "[\\u2FF0-\\u2FFF\\u3001-\\u3004\\u3006\\u3008-\\u3011\\u3013-\\u3020\\u302A-\\u302D\\u3030\\u3037\\u303C-\\u303F\\u31C0-\\u31E3\\u3220-\\u3243\\u3280-\\u32B0\\u32C0-\\u32CB\\u3358-\\u3370\\u337B-\\u337F\\u33E0-\\u33FE\\uFE45\\uFE46\\uFF61-\\uFF64]"},
    {"Hangul", "[\\u3001-\\u3004\\u3006\\u3008-\\u3011\\u3013-\\u3020\\u302E-\\u3030\\u3037\\u303C-\\u303F\\u327F\\uFE45\\uFE46\\uFF61-\\uFF64]"},
    {"Hanunoo", "[\\u1735\\u1736]"},
    {"Hiragana", "[\\u3001-\\u3004\\u3006\\u3008-\\u3020\\u3030-\\u3037\\u303C-\\u303F\\u3099-\\u309C\\u30A0\\u30FB\\u30FC\\u3190-\\u319F\\uFE45\\uFE46\\uFF61-\\uFF65\\uFF70\\uFF9E\\uFF9F]"},
    {"Kannada", "[\\u0CF1\\u0CF2]"},
    {"Katakana", "[\\u3001-\\u3004\\u3006\\u3008-\\u3020\\u3030-\\u3037\\u303C-\\u303F\\u3099-\\u309C\\u30A0\\u30FB\\u30FC\\u3190-\\u319F\\uFE45\\uFE46\\uFF61-\\uFF65\\uFF70\\uFF9E\\uFF9F]"},
    {"Latin", "[\\^`\\u00A8\\u00AF\\u00B4\\u00B8\\u02B9-\\u02DF\\u02E5-\\u02E9\\u02EC-\\u02FF\\u1D26-\\u1D2B\\u1D5D-\\u1D61\\u1D66-\\u1D6A\\u1D78\\u1DBF\\u2100-\\u2125\\u2127\\u2128\\u212C-\\u2131\\u2133\\u2134\\u2139-\\u213B\\u2145-\\u214A\\u214C\\u214D\\u249C-\\u24E9\\u3250\\u32CC-\\u32CF\\u3371-\\u337A\\u3380-\\u33DF\\u33FF\\uA700-\\uA721\\uA788-\\uA78A]"},
    {"Malayalam", "[\\u0CF1\\u0CF2]"},
    {"Mongolian", "[\\u1802\\u1803\\u1805]"},
    {"Oriya", "[\\u0964\\u0965\\u0CF1\\u0CF2]"},
    {"Phags_Pa", "[\\u1802\\u1803\\u1805\\u3001\\u3002\\u3008-\\u3011\\u3014-\\u301B\\uFF61-\\uFF64]"},
    {"Runic", "[\\u16EB-\\u16ED]"},
    {"Syriac", "[\\u060C\\u061B\\u061F\\u0640\\u064B-\\u0655\\u0670]"},
    {"Tagalog", "[\\u1735\\u1736]"},
    {"Tagbanwa", "[\\u1735\\u1736]"},
    {"Tamil", "[\\u0CF1\\u0CF2]"},
    {"Telugu", "[\\u0CF1\\u0CF2]"},
    {"Thaana", "[\\u060C\\u061B\\u061F\\u0660-\\u0669]"},
    {"Tibetan", "[\\u3001\\u3002\\u3008-\\u3011\\u3014-\\u301B\\uFF61-\\uFF64]"},
    {"Yi", "[\\u3001\\u3002\\u3008-\\u3011\\u3014-\\u301B\\uFF61-\\uFF64]"},
  };
  SCRIPT_NEW = loadData(data);
  }
  public static final UnicodeSet CATEGORY_CHANGED = (UnicodeSet) new UnicodeSet("[\\u2102\\u210A-\\u2113\\u2115\\u2119-\\u211D\\u2124\\u2128\\u2129\\u212C\\u212D\\u212F-\\u2131\\u2133-\\u2138\\u213C-\\u213F\\u2145-\\u2149\\U0001D165\\U0001D166\\U0001D16D-\\U0001D172\\U0001D400-\\U0001D7FF]").freeze();
  public static final Map<String,UnicodeSet> CATEGORY_NEW;
  static {
  String[][] data = {
    {"Math_Symbol", "[\\u2102\\u210A-\\u2113\\u2115\\u2119-\\u211D\\u2124\\u2128\\u2129\\u212C\\u212D\\u212F-\\u2131\\u2133-\\u2138\\u213C-\\u213F\\u2145-\\u2149\\U0001D400-\\U0001D7FF]"},
    {"Modifier_Symbol", "[\\U0001D165\\U0001D166\\U0001D16D-\\U0001D172]"},
    {"Symbol", "[\\$+<->\\^`|~\\u00A2-\\u00A9\\u00AC\\u00AE-\\u00B1\\u00B4\\u00B6\\u00B8\\u00D7\\u00F7\\u02C2-\\u02C5\\u02D2-\\u02DF\\u02E5-\\u02EB\\u02ED\\u02EF-\\u02FF\\u0375\\u0384\\u0385\\u03F6\\u0482\\u0606-\\u0608\\u060B\\u060E\\u060F\\u06E9\\u06FD\\u06FE\\u07F6\\u09F2\\u09F3\\u09FA\\u0AF1\\u0B70\\u0BF3-\\u0BFA\\u0C7F\\u0CF1\\u0CF2\\u0D79\\u0E3F\\u0F01-\\u0F03\\u0F13-\\u0F17\\u0F1A-\\u0F1F\\u0F34\\u0F36\\u0F38\\u0FBE-\\u0FC5\\u0FC7-\\u0FCC\\u0FCE\\u0FCF\\u109E\\u109F\\u1360\\u1390-\\u1399\\u17DB\\u1940\\u19E0-\\u19FF\\u1B61-\\u1B6A\\u1B74-\\u1B7C\\u1FBD\\u1FBF-\\u1FC1\\u1FCD-\\u1FCF\\u1FDD-\\u1FDF\\u1FED-\\u1FEF\\u1FFD\\u1FFE\\u2044\\u2052\\u207A-\\u207C\\u208A-\\u208C\\u20A0-\\u20B5\\u2100-\\u2106\\u2108-\\u2125\\u2127-\\u2129\\u212C-\\u2131\\u2133-\\u2138\\u213A-\\u214D\\u214F\\u2190-\\u2328\\u232B-\\u23E7\\u2400-\\u2426\\u2440-\\u244A\\u249C-\\u24E9\\u2500-\\u269D\\u26A0-\\u26BC\\u26C0-\\u26C3\\u2701-\\u2704\\u2706-\\u2709\\u270C-\\u2727\\u2729-\\u274B\\u274D\\u274F-\\u2752\\u2756\\u2758-\\u275E\\u2761-\\u2767\\u2794\\u2798-\\u27AF\\u27B1-\\u27BE\\u27C0-\\u27C4\\u27C7-\\u27CA\\u27CC\\u27D0-\\u27E5\\u27F0-\\u2982\\u2999-\\u29D7\\u29DC-\\u29FB\\u29FE-\\u2B4C\\u2B50-\\u2B54\\u2CE5-\\u2CEA\\u2E80-\\u2E99\\u2E9B-\\u2EF3\\u2F00-\\u2FD5\\u2FF0-\\u2FFB\\u3004\\u3012\\u3013\\u3020\\u3036\\u3037\\u303E\\u303F\\u309B\\u309C\\u3190\\u3191\\u3196-\\u319F\\u31C0-\\u31E3\\u3200-\\u321E\\u322A-\\u3243\\u3250\\u3260-\\u327F\\u328A-\\u32B0\\u32C0-\\u32FE\\u3300-\\u33FF\\u4DC0-\\u4DFF\\uA490-\\uA4C6\\uA700-\\uA716\\uA720\\uA721\\uA789\\uA78A\\uA828-\\uA82B\\uFB29\\uFDFC\\uFDFD\\uFE62\\uFE64-\\uFE66\\uFE69\\uFF04\\uFF0B\\uFF1C-\\uFF1E\\uFF3E\\uFF40\\uFF5C\\uFF5E\\uFFE0-\\uFFE6\\uFFE8-\\uFFEE\\uFFFC\\uFFFD\\U00010102\\U00010137-\\U0001013F\\U00010179-\\U00010189\\U00010190-\\U0001019B\\U000101D0-\\U000101FC\\U0001D000-\\U0001D0F5\\U0001D100-\\U0001D126\\U0001D129-\\U0001D166\\U0001D16A-\\U0001D172\\U0001D183\\U0001D184\\U0001D18C-\\U0001D1A9\\U0001D1AE-\\U0001D1DD\\U0001D200-\\U0001D241\\U0001D245\\U0001D300-\\U0001D356\\U0001D400-\\U0001D7FF\\U0001F000-\\U0001F02B\\U0001F030-\\U0001F093]"},
  };
  CATEGORY_NEW = loadData(data);
  }


  // END OF GENERATED CODE

  // UnicodeSet override
  static UnicodeSet.XSymbolTable myXSymbolTable = new UnicodeSet.XSymbolTable() { 
    public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
      int propEnum = -1;
      int valueEnum = -1;
      if (propertyValue.trim().length() != 0) {
        propEnum = UCharacter.getPropertyEnum(propertyName);
      } else {
        try {
          propEnum = UProperty.GENERAL_CATEGORY_MASK;
          valueEnum = UCharacter.getPropertyValueEnum(propEnum, propertyName);
          propertyValue = UCharacter.getPropertyValueName(propEnum, valueEnum, UProperty.NameChoice.LONG);
        } catch (IllegalArgumentException e) {
          try {
            propEnum = UProperty.SCRIPT;
            valueEnum = UCharacter.getPropertyValueEnum(propEnum, propertyName);
            propertyValue = UCharacter.getPropertyValueName(propEnum, valueEnum, UProperty.NameChoice.LONG);
          } catch (Exception e1) {
            return false;
          }
        }
      }

      String pvalue;
      UnicodeSet result2;
      UnicodeSet additions;
      boolean general;
      switch(propEnum) {
        case UProperty.SCRIPT:
          pvalue = getFixedPropertyValue(propEnum, propertyValue);
          result2 = new UnicodeSet().applyIntPropertyValue(propEnum, UCharacter.getPropertyValueEnum(propEnum, pvalue)).removeAll(SCRIPT_CHANGED);
          additions = SCRIPT_NEW.get(pvalue);
          if (additions != null) {
            result2.addAll(additions);
          }
          result.set(result2);
          return true;
        case UProperty.GENERAL_CATEGORY_MASK: 
          general=true;
        case UProperty.GENERAL_CATEGORY: 
          // TODO: fix Mask
          pvalue = getFixedPropertyValue(propEnum, propertyValue);
          result2 = new UnicodeSet().applyIntPropertyValue(propEnum, UCharacter.getPropertyValueEnum(propEnum, pvalue)).removeAll(CATEGORY_CHANGED);
          additions = CATEGORY_NEW.get(pvalue);
          if (additions != null) {
            result2.addAll(additions);
          }
          result.set(result2);
          return true;
      }
      return false;
    }
  };

  public static UnicodeSet parseUnicodeSet(String input) {
    String parseInput = input.trim();
    ParsePosition parsePosition = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(parseInput, parsePosition, myXSymbolTable);
    int parseEnd = parsePosition.getIndex();
    if (parseEnd != parseInput.length()) {
      parseEnd--; // get input offset
      throw new IllegalArgumentException("Additional characters past the end of the set, at " 
              + parseEnd + ", ..." 
              + input.substring(Math.max(0, parseEnd - 10), parseEnd)
              + "|"
              + input.substring(parseEnd, Math.min(input.length(), parseEnd + 10))
      );
    }
    if (DEBUG) {
      checkDifferences(input, result, new UnicodeSet(input));
    }
    return result;
  }

  private static void checkDifferences(String input, UnicodeSet result, UnicodeSet original) {
    if (!original.equals(result)) {
      final UnicodeSet removed = new UnicodeSet(original).removeAll(result);
      final UnicodeSet added = new UnicodeSet(result).removeAll(original);
      System.out.println(" *Altered UnicodeSet - removed: " + removed.size() + ", added: " + added.size() + ", input: " + input);
      if (!removed.isEmpty()) System.out.println("\tRemoved: " + removed.toPattern(false));
      if (!added.isEmpty()) System.out.println("\tAdded: " + added.toPattern(false));
    }
  }

  public static UnicodeSet applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
    UnicodeSet original;
    if (DEBUG) {
      original = new UnicodeSet(result).applyPropertyAlias(propertyName, propertyValue);
    }
    if (!myXSymbolTable.applyPropertyAlias(propertyName, propertyValue, result)) {
      result.applyPropertyAlias(propertyName, propertyValue);
    }
    if (DEBUG) {
      checkDifferences(propertyName + "=" + propertyValue, result, original);
    }
    return result;
  }

  // Standard items
  public static final Set<String> EUROPEAN = loadUnmodifiable(new TreeSet<String>(),
          "Latin", "Greek", "Coptic", "Cyrillic", 
          "Glag", "Armenian", "Georgian", "Shavian", "braille", 
          "ogham", "runic", "Gothic", "Cypriot", "Linear b", 
  "old italic");

  public static final Set<String> MIDDLE_EASTERN = loadUnmodifiable(new TreeSet<String>(),
          "Hebrew", "Arabic", "Syriac", "Thaana", "Carian", "Lycian", "Lydian", "Phoenician", 
          "Cuneiform", "old persian", "ugaritic"
  );
  public static final Set<String> SOUTH_ASIAN = loadUnmodifiable(new TreeSet<String>(),
          "Devanagari", "Bengali", "Gurmukhi", "Gujarati", 
          "Oriya", "Tamil", "Telugu", "Kannada", "Malayalam", 
          "Sinhala", "Tibetan", "Phags-Pa", "Limbu", "Sylo", "Kharoshthi", "lepcha", "saurashtra", "ol chiki"
  );
  public static final Set<String> SOUTHEAST_ASIAN = loadUnmodifiable(new TreeSet<String>(),
          "Thai", "Lao", "Myanmar", "Khmer", 
          "Tai_Le", "New Tai Lue", "Tagalog", "Hanunoo", "Buhid",
          "Tagbanwa", "Buginese", "Balinese", "Cham", "kayah li", "rejang", "sundanese"
  );
  public static final Set<String> EAST_ASIAN = loadUnmodifiable(new TreeSet<String>(),
          "Bopomofo", "Hiragana", "Katakana", "Mongolian", "Yi", "Han", "Hangul"
  );
  public static final Set<String> AFRICAN = loadUnmodifiable(new TreeSet<String>(),
          "Ethiopic", "Osmanya", "Tifinagh", "Nko", "vai"
  );
  public static final Set<String> AMERICAN = loadUnmodifiable(new TreeSet<String>(),
          "Cherokee", "CANS", "Deseret"
  );
  
  public static final Set<String> HISTORIC_SCRIPTS = loadUnmodifiable(new TreeSet<String>(),
          "Buginese", "Buhid", "Carian", "Coptic", "Cypriot", "Deseret", "Glagolitic",
          "Gothic", "Hanunoo", "Old_Italic", "Kharoshthi", "Linear_B", "Lycian", "Lydian",
          "Ogham", "Osmanya", "Phags_Pa", "Phoenician", "Rejang", "Runic", "Shavian", "Sundanese",
          "Syloti_Nagri", "Syriac", "Tagbanwa", "Tagalog", "Ugaritic", "Old_Persian", "Cuneiform"
  );

  //  public static final UnicodeSet OTHER_SCRIPTS = (UnicodeSet) parseUnicodeSet("[^[:script=common:][:script=inherited:]]")
  //  .removeAll(EUROPEAN)
  //  .removeAll(MIDDLE_EASTERN)
  //  .removeAll(SOUTH_ASIAN)
  //  .removeAll(SOUTHEAST_ASIAN)
  //  .removeAll(EAST_ASIAN)
  //  .removeAll(AFRICAN)
  //  .removeAll(AMERICAN)
  //  .removeAll(parseUnicodeSet("[[:script=han:][:script=hangul:]]"))
  //  .freeze();

  // Code to generate lists

  private static Map<String, UnicodeSet> loadData(String[][] data) {
    Map<String,UnicodeSet> script_new = new TreeMap<String, UnicodeSet>();
    for (String[] pair : data) {
      script_new.put(pair[0], (UnicodeSet) new UnicodeSet(pair[1]).freeze());
    }
    Map<String, UnicodeSet> foo = Collections.unmodifiableMap(script_new);
    return foo;
  }

  private static <U> Set<U> loadUnmodifiable(Set<U> set, U... items) {
    for (U item : items) {
      set.add(item);
    }
    return Collections.unmodifiableSet(set);
  }

  enum RemapType {NONE, SCRIPT, CATEGORY};

  static Map<RemapType, Map<String,UnicodeSet>>  getRemapData(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Map<RemapType, Map<String,UnicodeSet>> data = new TreeMap();
    data.put(RemapType.SCRIPT, new TreeMap<String,UnicodeSet>());
    data.put(RemapType.CATEGORY, new TreeMap<String,UnicodeSet>());

    Set<String> propertiesToAddTo = new TreeSet<String>();
    //    Composer composer = new Composer() {
    //      public Object compose(Object a, Object b) {
    //        Set<String> aa = (Set<String>) a;
    //        Set<String> bb = (Set<String>) b;
    //        if (aa == null) {
    //          return new TreeSet<String>(bb);
    //        }
    //        TreeSet<String> cc = new TreeSet<String>(bb);
    //        cc.addAll(aa);
    //        return cc;
    //      }
    //    };
    RemapType remapType = RemapType.NONE;

    while (true) {
      String line = in.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) {
        continue;
      }
      try {
        if (line.startsWith("@")) { // reassignment
          propertiesToAddTo.clear();
          remapType = RemapType.NONE;
          line = line.substring(1);
          if (!line.equalsIgnoreCase("no-change")) {
            String[] parts = line.split("\\s*[,\\s]\\s*");
            for (String part : parts) {
              RemapType newRemapType = RemapType.NONE;
              String fixed = null;
              UnicodeSet scriptSet = null;
              // check
              try {
                //scriptSet = new UnicodeSet("[:script=" + part + ":]");
                fixed = getFixedPropertyValue(UProperty.SCRIPT, part);
                newRemapType = RemapType.SCRIPT;
                propertiesToAddTo.add(fixed);

              } catch (Exception e) {
                //scriptSet = new UnicodeSet("[:gc=" + part + ":]");
                fixed = getFixedPropertyValue(UProperty.GENERAL_CATEGORY, part);
                newRemapType = RemapType.CATEGORY;
                propertiesToAddTo.add(fixed);
              }
              if (remapType == RemapType.NONE) {
                remapType = newRemapType;
              } else if (remapType != newRemapType) {
                throw new IllegalArgumentException("Mixing Script and Category on one line: " + line);
              }
              //addToMapToUnicodeSet(data.get(remapType), fixed, scriptSet);
            }
            if (DEBUG_MAIN) System.out.println("Adding to " + propertiesToAddTo);
          }
        } else if (line.startsWith("U+")) { // character
          line = line.substring(2, line.indexOf(' '));
          if (line.length() < 4) {
            throw new IllegalArgumentException();
          }
          for (String fixed : propertiesToAddTo) {
            addToMapToUnicodeSet(data.get(remapType), fixed, new UnicodeSet().add(Integer.parseInt(line,16)));
          }
          //data.composeWith(new UnicodeSet().add(Integer.parseInt(line,16)), currentData, composer);
        }  else if (line.startsWith("[") && !line.startsWith("[Ed")) { // unicode set
          UnicodeSet set = new UnicodeSet(line);
          for (String fixed : propertiesToAddTo) {
            addToMapToUnicodeSet(data.get(remapType), fixed, set);
          }
          //data.composeWith(set, currentData, composer);
        }
      } catch (RuntimeException e) {
        throw (RuntimeException) new IllegalArgumentException("line: " + line).initCause(e);
      }
    }
    in.close();
    return data;
  }

  public static String getFixedPropertyValue(int propertyEnum, String valueName) {
    int valueEnum = UCharacter.getPropertyValueEnum(propertyEnum, valueName);
    String fixed = UCharacter.getPropertyValueName(propertyEnum, valueEnum, UProperty.NameChoice.LONG);
    return fixed;
  }

  public static String getFixedPropertyValue(String propertyName, String valueName) {
    return getFixedPropertyValue(UCharacter.getPropertyEnum(propertyName), valueName);
  }

  private static <T> void addToMapToUnicodeSet(Map<T, UnicodeSet> mapToUnicodeSet, T key, UnicodeSet additions) {
    UnicodeSet oldSet = mapToUnicodeSet.get(key);
    if (oldSet == null) {
      mapToUnicodeSet.put(key, oldSet = new UnicodeSet());
    }
    if (DEBUG_MAIN) System.out.println("Adding " + key + ", " + additions);
    oldSet.addAll(additions);
  }


  private static UnicodeSet getChanged(Map<RemapType, Map<String, UnicodeSet>> data, RemapType remapType) {
    UnicodeSet changed = new UnicodeSet();
    for (UnicodeSet value : data.get(remapType).values()) {
      changed.addAll(value);
    }
    return changed;
  }

  public static void main(String[] args) throws IOException {
      BitSet scripts = new BitSet();

      for (String item : Arrays.asList("EUROPEAN", "MIDDLE_EASTERN", "AFRICAN", "SOUTH_ASIAN", "EAST_ASIAN", "AMERICAN", "SOUTHEAST_ASIAN")) {
          Collection c =
              item.equals("EUROPEAN") ? EUROPEAN :
                  item.equals("MIDDLE_EASTERN") ? MIDDLE_EASTERN :
                      item.equals("AFRICAN") ? AFRICAN :
                          item.equals("SOUTH_ASIAN") ? SOUTH_ASIAN :
                              item.equals("EAST_ASIAN") ? EAST_ASIAN :
                                  item.equals("AMERICAN") ? AMERICAN :
                                      item.equals("SOUTHEAST_ASIAN") ? SOUTHEAST_ASIAN :
                  null;
          for (String scriptName : (Collection<String>) c) {
              int sc = UScript.getCodeFromName(scriptName);
              String shortName = "[:script=" + UScript.getShortName(sc) + ":]";
              scripts.set(sc);
              String age = getAge(new UnicodeSet(shortName));
              System.out.println(shortName + "\t" + item + "\t" + age);
          }
      }
      for (int sc = 0; sc < UScript.CODE_LIMIT; ++sc) {
          if (scripts.get(sc)) continue;
          String shortName = "[:script=" + UScript.getShortName(sc) + ":]";
          String age = getAge(new UnicodeSet(shortName));
          if (age != null) {
              System.out.println(shortName + "\t" + "???" + "\t" + age);
          }
      }

      if (true) return;
    System.out.println("Archaic: " + ARCHAIC.size() + ", " + ARCHAIC);

    parseUnicodeSet("[[:script=han:]-[:block=CJK Unified Ideographs:]]");
    parseUnicodeSet("[:Lm:]");
    parseUnicodeSet("[:s:]");
    parseUnicodeSet("[:Letter:]");
    parseUnicodeSet("[:script=Common:]");
    parseUnicodeSet("[[:Letter:]&[:script=common:]]");
    parseUnicodeSet("[[:So:]&[[:script=common:][:script=inherited:]][[:Letter:]&[:script=common:]]]");
    checkHistoricClosure();
    generateRemappingCode(args);
  }

  private static String getAge(UnicodeSet unicodeSet) {
      VersionInfo oldest = null;
    for (String s : unicodeSet) {
        VersionInfo age = UCharacter.getAge(s.codePointAt(0));
        if (oldest == null || age.compareTo(oldest) < 0) {
            oldest = age;
        }
    }
    if (oldest != null) {
        return oldest.getMajor() + "." + oldest.getMinor();
    }
    return null;
}

private static void checkHistoricClosure() {
    testNormalizationConsistency("Historic", ARCHAIC, Normalizer.NFKD);
    for (int pValue = UCharacter.getIntPropertyMinValue(UProperty.SCRIPT); 
      pValue <= UCharacter.getIntPropertyMaxValue(UProperty.SCRIPT); pValue++) {
      String pValueName = UCharacter.getPropertyValueName(UProperty.SCRIPT, pValue, UProperty.NameChoice.LONG);
      //System.out.println("Checking " + pValueName);
      UnicodeSet temp  = new UnicodeSet();
      boolean t = myXSymbolTable.applyPropertyAlias("script", pValueName, temp);
      testNormalizationConsistency("Script=" + pValueName, temp, Normalizer.NFKD);
    }
  }

  private static void testNormalizationConsistency(String title, UnicodeSet testSet, Normalizer.Mode mode) {
    UnicodeSet inSet = new UnicodeSet();
    UnicodeSet inDecompSet = new UnicodeSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[[:nfkdqc=n:]" +
    		//"-[:hangulsyllabletype=LV:]" +
    		//"-[:hangulsyllabletype=LVT:]" +
    		"]")); it.next();) {
      String nfkd = Normalizer.normalize(it.codepoint, mode);
      boolean isHistoric = testSet.contains(it.codepoint);
      boolean nfkdContainsHistoric = testSet.containsSome(nfkd);
      if (isHistoric != nfkdContainsHistoric) {
        if (isHistoric) inSet.add(it.codepoint);
        if (nfkdContainsHistoric) inDecompSet.add(it.codepoint);
      }
    }
    if (inSet.size() != 0 || inDecompSet.size() != 0) {
      System.out.println("// Possible Problems in " + title + ":");
      System.out.println("// \t In ordinary, but not in decomps: " + inSet.toPattern(false));
      System.out.println("// \t In decomps, but not in ordinary: " + inDecompSet.toPattern(false));
    }
  }

  private static void generateRemappingCode(String[] args) throws IOException {
    Map<RemapType, Map<String, UnicodeSet>> data = getRemapData(args[0] + "ScriptData.txt");
    for (RemapType r : data.keySet()) {
      UnicodeSet changed = getChanged(data, r);
      System.out.println("public static final UnicodeSet " + r + "_CHANGED = (UnicodeSet) new UnicodeSet(\""
              + changed.toString().replace("\\", "\\\\") + "\").freeze();");
      System.out.println("public static final Map<String,UnicodeSet> "+r+"_NEW;\n" +
              "static {\n" +
      "String[][] data = {");
      Map<String, UnicodeSet> map = data.get(r);
      for (String key : map.keySet()) {
        System.out.println("  {\"" + key + "\", \"" + map.get(key).toString().replace("\\", "\\\\") + "\"},");
      }
      if (r == RemapType.CATEGORY) {
        System.out.println("  {\"" + "Symbol" + "\", \"" + new UnicodeSet("[:Symbol:]").addAll(changed)
                .toString().replace("\\", "\\\\") + "\"},");
      }
      System.out.println("};\n" +
              ""+r+"_NEW = loadData(data);\n" +
      "}");
    }
    //System.out.println(data.toString().replace(" ", "\n ").replace("{", "{\n ").replace("}", "\n}"));
  }

}
