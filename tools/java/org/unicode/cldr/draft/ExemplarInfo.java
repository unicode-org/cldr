package org.unicode.cldr.draft;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.WinningChoice;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.FilteredNormalizer2;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.ULocale;

/**
 * 
 */

public class ExemplarInfo {
  public static UnicodeSet IGNORE = new UnicodeSet("[[:sc=unknown:][:script=common:]-[:M:]]").freeze();
  public static UnicodeSet TEST_ENCODING = new UnicodeSet("[[:any:]-[:c:] [:cc:]]").freeze();

  public static final Normalizer2 nfkd = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);

  public static final Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Mode.DECOMPOSE);

  public static final Normalizer2 nfc = Normalizer2.getInstance(null, "nfc", Mode.COMPOSE);

  private static final Normalizer2 nfkdMinus = new FilteredNormalizer2(nfkd, new UnicodeSet("[" +
          "[:dt=Initial:][:dt=Medial:][:dt=Final:][:dt=Isolated:]" +
          "[:dt=Narrow:][:dt=Wide:][:dt=Vertical:]" +
  "[:dt=Canonical:]]").freeze());


  private static PrettyPrinter pp = new PrettyPrinter();

  private static Map<String,ExemplarInfo> languageToExemplars = new TreeMap<String, ExemplarInfo>();
  private static UnicodeSet az = new UnicodeSet("[a-z]").freeze();
  static {

    // qu - Quechua
    languageToExemplars.put("qu", new ExemplarInfo("[pt{ch}kq{pʼ}{tʼ}{chʼ}{kʼ}{qʼ}{ph}{th}{chh}{kh}{qh}s{sh}hmnjl{ll}rwyñaiu]", "[a-z]"));
    // co - Corsican
    languageToExemplars.put("co", new ExemplarInfo("[abc{chj}defg{ghj}hijlmnopqrstuvz]", "[a-z]"));
    // fy - West Frisian
    languageToExemplars.put("fy", new ExemplarInfo("[a  b c d e f g h i y j k l m n o p q r s t u v w x zâ  ê é ô û ú]", "[a-z]"));
    // bho - Bhojpuri
    languageToExemplars.put("bho", new ExemplarInfo("[:sc=deva:]", "[a-z]"));
    // gd - Scottish Gaelic
    languageToExemplars.put("gd", new ExemplarInfo("[abcdefghilmnoprstuàèìòù]", "[a-z]"));
    // ht - Haitian Creole
    languageToExemplars.put("ht", new ExemplarInfo("[a{an}b{ch}de{en}èfgijklmnoò{on}{ou}prst{tch}vwyz]", "[a-z]"));
    // jv - Javanese
    languageToExemplars.put("jv", new ExemplarInfo("[a  b c d e é è f g h i j k l m n o p q r s t u v w x y z]", "[a-z]"));
    // la - Latin
    languageToExemplars.put("la", new ExemplarInfo("[abcdefghiklmnopqrstuxyz]", "[a-z]"));
    // lb - Luxembourgish
    languageToExemplars.put("lb", new ExemplarInfo("[a-z é ä ë]", "[a-z]"));
    // sd - Sindhi
    languageToExemplars.put("sd", new ExemplarInfo("[جھ ڄ ج پ ث ٺ ٽ ٿ ت ڀ ٻ ب اڙ  ر ذ ڍ ڊ ڏ ڌ د خ ح ڇ چ ڃق  ڦ ف غ ع ظ ط ض ص ش س ز ڙھي ه و ڻ ن م ل ڱ گھ  ڳ گ ک ڪ]", "[:sc=deva:]"));
    // su - Sundanese
    languageToExemplars.put("su", new ExemplarInfo("[aeiouépbtdkgcjh{ng}{ny}mnswlry]", "[a-z]"));
    // gn - Guaraní = gug
    languageToExemplars.put("gn", new ExemplarInfo("[a-vx-zá é í ó ú ý ñ ã ẽ ĩ õ ũ ỹ {g\u0303}]", "[a-z]"));
  }
  // http://ja.wikipedia.org/wiki/学年別漢字配当表, http://kanji-a.seesaa.net/category/2203790-1.html
  private static UnicodeMap<String> JapaneseEducationLevels = new UnicodeMap<String>()
  .putAll(new UnicodeSet("[一七三-下中九二五人休先入八六円出力十千口右名四土夕大天女子字学小山川左年手文日早月木本村林校森正気水火犬玉王生田男町白百目石空立竹糸耳花草虫見貝赤足車金雨青音]"), "1")
  .putAll(new UnicodeSet("[万丸交京今会体何作元兄光公内冬刀分切前北午半南原友古台合同回図国園地場声売夏外多夜太妹姉室家寺少岩工市帰広店弓引弟弱強当形後心思戸才教数新方明星春昼時晴曜書朝来東楽歌止歩母毎毛池汽活海点父牛理用画番直矢知社秋科答算米紙細組絵線羽考聞肉自船色茶行西親角言計記話語読谷買走近通週道遠里野長門間雪雲電頭顔風食首馬高魚鳥鳴麦黄黒]"), "2")
  .putAll(new UnicodeSet("[丁世両主乗予事仕他代住使係倍全具写列助勉動勝化区医去反取受号向君味命和品員商問坂央始委守安定実客宮宿寒対局屋岸島州帳平幸度庫庭式役待急息悪悲想意感所打投拾持指放整旅族昔昭暑暗曲有服期板柱根植業様横橋次歯死氷決油波注泳洋流消深温港湖湯漢炭物球由申界畑病発登皮皿相県真着短研礼神祭福秒究章童笛第筆等箱級終緑練羊美習者育苦荷落葉薬血表詩調談豆負起路身転軽農返追送速進遊運部都配酒重鉄銀開院陽階集面題飲館駅鼻]"), "3")
  .putAll(new UnicodeSet("[不争付令以仲伝位低例便信倉候借停健側働億兆児共兵典冷初別利刷副功加努労勇包卒協単博印参史司各告周唱喜器囲固型堂塩士変夫失好季孫完官害察巣差希席帯底府康建径徒得必念愛成戦折挙改救敗散料旗昨景最望未末札材束松果栄案梅械極標機欠歴残殺毒氏民求治法泣浅浴清満漁灯無然焼照熱牧特産的省祝票種積競笑管節粉紀約結給続置老胃脈腸臣航良芸芽英菜街衣要覚観訓試説課議象貨貯費賞軍輪辞辺連達選郡量録鏡関陸隊静順願類飛飯養験]"), "4")
  .putAll(new UnicodeSet("[久仏仮件任似余価保修俵個備像再刊判制券則効務勢厚句可営因団圧在均基報境墓増夢妻婦容寄富導居属布師常幹序弁張往復徳志応快性恩情態慣承技招授採接提損支政故敵断旧易暴条枝査格桜検構武比永河液混減測準演潔災燃版犯状独率現留略益眼破確示祖禁移程税築精素経統絶綿総編績織罪群義耕職肥能興舌舎術衛製複規解設許証評講謝識護豊財貧責貸貿賀資賛質輸述迷退逆造過適酸鉱銅銭防限険際雑非預領額飼]"), "5")
  .putAll(new UnicodeSet("[並乱乳亡仁供俳値傷優党冊処刻割創劇勤危卵厳収后否吸呼善困垂城域奏奮姿存孝宅宇宗宙宝宣密寸専射将尊就尺届展層己巻幕干幼庁座延律従忘忠憲我批担拝拡捨探推揮操敬映晩暖暮朗机枚染株棒模権樹欲段沿泉洗派済源潮激灰熟片班異疑痛皇盛盟看砂磁私秘穀穴窓筋策簡糖系紅納純絹縦縮署翌聖肺背胸脳腹臓臨至若著蒸蔵蚕衆裁装裏補視覧討訪訳詞誌認誕誠誤論諸警貴賃遺郵郷針鋼閉閣降陛除障難革頂骨]"), "6")
  .putAll(new UnicodeSet("[丈与且丘丙丹乏乙乾了互井亜享亭介仙仰企伏伐伯伴伸伺但佐佳併侍依侮侯侵促俊俗俸倒倣倫倹偉偏偵偶偽傍傑傘催債傾僕僚僧儀儒償充克免兼冒冗冠准凍凝凡凶凸凹刃刈刑到刺削剖剛剣剤剰劣励劾勅勘募勧勲勺匁匠匹匿升卑卓占即却卸厄厘又及双叔叙叫召吉吏吐吟含吹呈呉咲哀哲唆唇唐唯啓喚喝喪喫嗣嘆嘱噴嚇囚圏坊坑坪垣埋執培堀堅堕堤堪塀塁塊塑塔塗塚塾墜墨墳墾壁壇壊壌壮壱奇奉契奔奥奨奪奴如-妄妊妙妥妨姓姫姻威娘娠娯婆婚婿媒嫁嫌嫡嬢孔孤宜宰宴宵寂寛寝寡寧審寮寿封尉尋尚尼-尿屈履屯岐岬岳峠峡峰崇崎崩巡巧巨帆帝帥帽幅幣幻幽幾床庶庸廃廉廊廷弊弐弔弦弧弾彩彫彰影彼征徐御循微徴徹忌忍忙怒怖怠怪恋恐恒恥恨恭恵悔悟悠患悦悩悼惑惜惨惰愁愉愚慈慌慎慕慢慨慮慰慶憂憎憤憩憶憾懇懐懲懸戒戯戻房扇扉払扱扶抄把抑抗抜択披抱抵抹押抽拍拐拒拓拘拙拠括拷挑挟振挿捕捜据掃掌排掘掛控措掲描揚換握援揺搬搭携搾摂摘摩撃撤撮撲擁擦擬攻敏敢敷斉斎斗斜斤斥施旋既旨旬昆昇是普晶暁暇暦暫曇更曹替朕朱朴朽杉杯析枠枢枯架柄某柔柳栓核栽桃桑桟棄棋棚棟棺楼概槽欄欧欺款歓歳殉殊殖殴殻殿汁汗汚江沈沖没沢沸沼況泊泌泡泥泰洞津洪浄浜浦浪浮浸涙涯涼淑淡添渇渉渋渓渡渦湾湿溝溶滅滋滑滝滞滴漂漆漏漠漫漬漸潜潟潤澄濁濃濫濯瀬炉炊炎為烈焦煙煩煮燥爆爵牲犠狂狩狭猛猟猫献猶猿獄獣獲玄珍珠琴環璽瓶甘甚甲畔畜畝畳疎疫疲疾症痘痢痴療癒癖皆盆盗監盤盲盾眠眺睡督瞬矛矯砕砲硝硫硬碁碑磨礁礎祈祉祥禅禍秀租秩称稚稲稼稿穂穏穫突窃窒窮窯竜端符筒箇範篤簿籍粋粒粗粘粛粧糧糾紋紛紡索紫累紳紹紺絞絡継維綱網緊緒締緩緯縁縄縛縫繁繊繕繭繰缶罰罷羅翁翻翼耐耗聴肌肖肝肢肩肪肯胆胎胞胴脂脅脚脱脹腐腕腰膚膜膨臭致舗舞舟般舶艇艦芋芝芳苗茂茎荒荘菊菌菓華葬蓄薄薦薪薫藩藻虐虚虜虞蚊蛇蛍蛮融衝衡衰衷袋被裂裕裸褐褒襟襲覆覇触訂託訟訴診詐詔詠詰該詳誇誉誓誘請諭諮諾謀謁謄謙謡謹譜譲豚豪貞貢販貫賄賊賓賜賠賢賦購贈赦赴超越趣距跡跳践踊踏躍軌軒軟軸較載輝輩轄辛辱込迅迎迫迭逃透逐逓途逝逮逸遂遅遇遍違遣遭遮遵遷避還邦邪邸郊郎郭酌酔酢酪酬酵酷醜醸釈釣鈍鈴鉛鉢銃銑銘鋭鋳錘錠錬錯鍛鎖鎮鐘鑑閑閥閲闘阻附陣陥陪陰陳陵陶隅隆随隔隠隣隷隻雄雅雇雌離雰零雷需震霊霜霧露靴韻響項頑頒頻頼顕顧飢飽飾餓香駄駆駐騎騒騰驚髄髪鬼魂魅魔鮮鯨鶏麗麻黙鼓齢]"), "9")
  .freeze();
  //  static {
  //    for (Integer value : Builder.with(new TreeSet<Integer>()).addAll(JapaneseEducationLevels.values()).get()) {
  //      System.out.println(".putAll(new UnicodeSet(\"" + JapaneseEducationLevels.getSet(value).toPattern(false) + "\"), " + value + ")");
  //    }
  //  }

  private UnicodeSet exemplars;
  UnicodeSet exemplarsX;
  UnicodeSet auxiliariesX;
  UnicodeSet exemplarScripts;
  UnicodeSet auxiliaryScripts;
  UnicodeMap<String> educationLevels = new UnicodeMap<String>();

  static CLDRFile.Factory cldrFactory  = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");

  private ExemplarInfo(String main, String aux) {
    this(new UnicodeSet(main), new UnicodeSet(aux), null);
  }

  public enum Status {O, M, A, S, T, X, N}

  public Status getStatus(String sequence) {
    if (IGNORE.containsAll(sequence)) {
      return Status.O;
    }
    if (exemplarsX.containsAll(sequence)) {
      return Status.M;
    }
    if (auxiliariesX.containsAll(sequence)) {
      return Status.A;
    }
    if (exemplarScripts.containsAll(sequence)) {
      return Status.S;
    }
    if (auxiliaryScripts.containsAll(sequence)) {
      return Status.T;
    }
    return Status.X;
  }

  public UnicodeSet getExemplars() {
    return exemplars;
  }

  private ExemplarInfo(UnicodeSet exemplars1, UnicodeSet auxiliary1, ULocale locale) {
    // check that the aux exemplars include all or none of a-z

    if (auxiliary1 == null) {
      auxiliary1 = new UnicodeSet();
    }
    exemplars = ExemplarInfo.flatten(exemplars1, locale).freeze();
    auxiliary1.addAll(exemplars1);
    if (auxiliary1.containsSome(az) && !auxiliary1.containsAll(az)) {
      System.err.println("WARNING " + locale + " Aux+Main inconsistent on a-z:\t" + pp.format(auxiliary1));
    }
    auxiliary1.addAll(az);
    auxiliariesX = ExemplarInfo.flatten(auxiliary1, locale).addAll(IGNORE).freeze();
    exemplarsX = new UnicodeSet(exemplars).addAll(IGNORE).freeze();
    exemplarScripts = expandScripts(exemplars1, locale).addAll(IGNORE).freeze();
    auxiliaryScripts = expandScripts(auxiliary1, locale).addAll(IGNORE).freeze();
    if (locale != null) {
      if (locale.equals(ULocale.JAPANESE)) {
        educationLevels.putAll(getCharset("Shift_JIS"), "SJIS");
        educationLevels.putAll(JapaneseEducationLevels);
      } else if (locale.equals(ULocale.KOREAN)) {
        educationLevels.putAll(getCharset("x-windows-949"), "949");
      } else if (locale.equals(ULocale.CHINESE)) {
        educationLevels.putAll(getCharset("GB2312"), "2312");
      } else if (locale.equals(ULocale.TRADITIONAL_CHINESE)) {
        educationLevels.putAll(getCharset("Big5"), "Big5");
      }
    }
  }

  static SortedMap<String, Charset> charsets = Charset.availableCharsets();


  UnicodeSet getCharset(String name) {
    UnicodeSet result = new UnicodeSet();
    Charset charset = charsets.get(name);
    CharEncoder encoder = new CharEncoder(charset, false, false);
    byte[] temp = new byte[100];
    for (UnicodeSetIterator usi = new UnicodeSetIterator(TEST_ENCODING); usi.next();) {
      int len = encoder.getValue(usi.codepoint, temp, 0);
      if (len > 0) {
        result.add(usi.codepoint);
      }
    }
    return result.freeze();
  }

  private UnicodeSet expandScripts(UnicodeSet source, ULocale locale) {
    UnicodeSet temp = new UnicodeSet();
    UnicodeSet scripts = new UnicodeSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(source); it.next();) {
      String s = it.getString();
      if (scripts.containsAll(s)) {
        continue;
      }
      int cp;
      for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
        cp = s.codePointAt(i);
        int script = UScript.getScript(cp);
        if (script != UScript.COMMON && script != UScript.UNKNOWN && script != UScript.INHERITED) {
          temp.applyIntPropertyValue(UProperty.SCRIPT, script);
          scripts.addAll(temp);
        }
      }
    }
    return ExemplarInfo.flatten(scripts, locale);
  }

  public static String getCldrLanguage(String language) {
    String cldrLanguage = language.replace("-", "_");
    if (cldrLanguage.equals("tl")) {
      cldrLanguage = "fil";
    } else if (cldrLanguage.equals("no")) {
      cldrLanguage = "nb";
    }
    return cldrLanguage;
  }

  public static String specialNormalize(String marks, ULocale locale) {
    marks = ExemplarInfo.nfd.normalize(marks);
    marks = locale == null ? UCharacter.toLowerCase(marks) : UCharacter.toLowerCase(locale, marks);
    marks = ExemplarInfo.nfkdMinus.normalize(marks);
    marks = ExemplarInfo.nfc.normalize(marks); // just in case
    return marks;
  }

  public static UnicodeSet flatten(UnicodeSet exemplar1, ULocale locale) {
    if (exemplar1 == null) {
      return null;
    }
    UnicodeSet result = new UnicodeSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplar1); it.next();) {
      String s = it.getString();
      s = ExemplarInfo.specialNormalize(s, locale);
      if (s.contains("ſ") && locale != null) {
        System.out.print("");
      }
      // add all the combining sequences in s to result;
      if (s.codePointCount(0, s.length()) == 1) {
        result.add(s);
        continue;
      }
      // multiple code points, so break it up.
      int lastPos = 0;
      int cp;
      for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
        cp = s.codePointAt(i);
        int type = UCharacter.getType(cp);
        if (type == UCharacter.ENCLOSING_MARK || type == UCharacter.NON_SPACING_MARK || type == UCharacter.COMBINING_SPACING_MARK) {
          // continue;
        } else {
          // add up to now, and reset pointer
          if (i > lastPos) {
            result.add(s.substring(lastPos, i));
          }
          lastPos = i;
        }
      }
      result.add(s.substring(lastPos, s.length()));

    }
    return result;
  }

  public static ExemplarInfo make(String language, Set<String> missingExemplars) {
    String cldrLanguage = ExemplarInfo.getCldrLanguage(language);
    ExemplarInfo exemplarInfo = languageToExemplars.get(cldrLanguage);
    if (exemplarInfo == null) {
      ULocale locale = new ULocale(cldrLanguage);
      UnicodeSet exemplars1 = null;
      UnicodeSet auxiliary1 = null;
      try {
        if (cldrLanguage.startsWith("zh")) {
          System.out.print("");
        }
        CLDRFile file = ExemplarInfo.cldrFactory.make(cldrLanguage, true);
        exemplars1 = file.getExemplarSet("", WinningChoice.WINNING, 0);
        auxiliary1 = file.getExemplarSet("auxiliary", WinningChoice.WINNING, 0);
      } catch (Exception e) {
        System.out.println("Can't read " + (exemplars1 == null ? "main" : "aux")
                + " exemplars for " + cldrLanguage);
        if (missingExemplars != null) {
          missingExemplars.add(cldrLanguage);
        }
      }
      if (exemplars1 == null) {
        exemplars1 = new UnicodeSet();
      }
      exemplarInfo = new ExemplarInfo(exemplars1, auxiliary1, locale);
      languageToExemplars.put(cldrLanguage, exemplarInfo);
    }
    return exemplarInfo;
  }

  public String getEducationLevel(CharSequence input) {
    String result = null;
    for (CodePoints cps = new CodePoints(input); cps.next();) {
      String level = educationLevels.get(cps.getCodePoint());
      if (level == null) return null;
      if (result == null || result.compareTo(level) < 0) {
        result = level;
      }
    }
    return result;
  }
  
  public static void main(String[] args) {
    Set<String> missingExemplars = new TreeSet<String>();
    System.out.println(Charset.availableCharsets());

    System.out.println(ExemplarInfo.make("ja", missingExemplars).getEducationLevel("\u4e00"));
    System.out.println(ExemplarInfo.make("zh", missingExemplars).getEducationLevel("\u4e00"));
    System.out.println(ExemplarInfo.make("zh-Hant", missingExemplars).getEducationLevel("\u4e00"));
    System.out.println(ExemplarInfo.make("ko", missingExemplars).getEducationLevel("\u4e00"));

  }
}