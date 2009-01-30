package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.UnicodeMap;
import org.unicode.cldr.util.UnicodeMap.Composer;

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
          "[ [:blk=Ancient_Greek_Musical_Notation:]" +
          "[:blk=Buginese:] " +
          "[:blk=Buhid:] [:blk=Carian:] " +
          "[:blk=Coptic:] [:blk=Cuneiform:] " +
          "[:blk=Cuneiform_Numbers_And_Punctuation:] " +
          "[:blk=Cypriot_Syllabary:] [:blk=Deseret:] [:blk=Glagolitic:] " +
          "[:blk=Gothic:] [:blk=Hanunoo:] [:blk=Kharoshthi:] [:blk=Linear_B_Ideograms:] " +
          "[:blk=Linear_B_Syllabary:] [:blk=Lycian:] [:blk=Lydian:] [:blk=Ogham:]" +
          " [:blk=Old_Italic:] [:blk=Old_Persian:] [:blk=Osmanya:] [:blk=Phags_Pa:] " +
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
    System.out.println("Archaic: " + ARCHAIC.size() + ", " + ARCHAIC);
    UnicodeSet knownOk = new UnicodeSet("[\u0392\u0398\u03A0\u03A6\u03B2\u03B8\u03C0\u03C6]");
    final UnicodeSet caseProblems = new UnicodeSet(ARCHAIC).closeOver(UnicodeSet.CASE).removeAll(ARCHAIC).removeAll(knownOk);
    if (caseProblems.size() != 0) {
      throw new IllegalArgumentException("Case: " + caseProblems);
    }
  }

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
  
  static final Map<String, Integer> RADICAL_NUM2CHAR = new LinkedHashMap();
  static final Map<Integer, String> RADICAL_CHAR2NUM = new LinkedHashMap();
  static final Map<Integer, Integer> RADICAL_CHAR2STROKES = new LinkedHashMap();

  static {
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
            {"210'","?","14"},
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
        RADICAL_NUM2CHAR.put(pair[0], radicalChar);
        RADICAL_CHAR2NUM.put(radicalChar, pair[0]);
        RADICAL_CHAR2STROKES.put(radicalChar, Integer.parseInt(pair[2]));
      }
      // TODO protect these maps
  }

  static UnicodeMap getScriptData(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    UnicodeMap data = new UnicodeMap();
    Set<String> currentData = new TreeSet<String>();
    Composer composer = new Composer() {
      public Object compose(Object a, Object b) {
        Set<String> aa = (Set<String>) a;
        Set<String> bb = (Set<String>) b;
        if (aa == null) {
          return new TreeSet<String>(bb);
        }
        TreeSet<String> cc = new TreeSet<String>(bb);
        cc.addAll(aa);
        return cc;
      }
    };
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) {
        continue;
      }
      try {
        if (line.startsWith("@")) { // reassignment
          currentData.clear();
          line = line.substring(1);
          if (!line.equalsIgnoreCase("no-change")) {
            String[] parts = line.split("\\s*[,\\s]\\s*");
            for (String part : parts) {
              // check
              new UnicodeSet("[:" + part + ":]");
              currentData.add(part);
            }
          }
        } else if (line.startsWith("U+")) { // character
          line = line.substring(2, line.indexOf(' '));
          if (line.length() < 4) {
            throw new IllegalArgumentException();
          }
          data.composeWith(new UnicodeSet().add(Integer.parseInt(line,16)), currentData, composer);
        }  else if (line.startsWith("[") && !line.startsWith("[Ed")) { // unicode set
          UnicodeSet set = new UnicodeSet(line);
          data.composeWith(set, currentData, composer);
        }
      } catch (RuntimeException e) {
        throw (RuntimeException) new IllegalArgumentException("line: " + line).initCause(e);
      }
    }
    in.close();
    return data;
  }
  public static void main(String[] args) throws IOException {
    UnicodeMap data = getScriptData(args[0] + "ScriptData.txt");
    System.out.println(data);
  }
}
