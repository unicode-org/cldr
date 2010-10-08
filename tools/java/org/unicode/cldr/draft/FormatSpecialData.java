package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptCategories.RemapType;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.CollectionComparator;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;

public class FormatSpecialData {
  private static final UnicodeSet UNASSIGNED = new UnicodeSet("[:cn:]").freeze();
  public static final UnicodeSet simpOnly = new UnicodeSet("[㑩㓥㔉㖊㖞㛟㛠㛿㟆㧑㧟㨫㱩㱮㲿㶉㶶㶽㺍㻏㻘䁖䅉䇲䌶-䌺䌼-䌾䍀䍁䓕䗖䘛䙊䙓䜣䜥䜧䝙䞌䞍䞐䢂䥿-䦁䩄䯃-䯅䲝䲞䴓-䴙万与丑专业-丝丢两严丧个丰临为丽举么义乌乐乔习乡书买乱争于亏云亚产亩亲亵亸亿仅仆从仑仓仪们价众优会伛伞-传伣-伧伪伫体佣佥侠侣侥-侪侬俣俦俨-俫俭债倾偬偻偾偿傥傧-傩儿克兑兖党兰关兴兹养兽冁内冈册写军农冯冲决况冻净准凉减凑凛几凤凫凭凯击凿刍划刘-创删别-刮制刹刽刿剀剂剐剑剥剧劝办务劢动励-劳势勋勚匀匦匮区医华协单卖卜卢卤卫却厂厅历厉压-厍厐厕厘厢厣厦厨厩厮县叁参双发变叙叠只叶号叹叽同向吓吕吗吣吨听启吴呐呒呓呕-呙呛呜咏咙咛咝咤咸响哑-哕哗哙哜哝哟唛唝唠-唢唤啧啬-啮啴啸喷喽喾嗫嗳嘘嘤嘱噜嚣团园困囱围囵国图圆圣圹场坂坏块坚-坠垄-垆垒垦垩垫垭垱垲垴埘-埚埯堑堕墙壮声壳壶壸处备复够头夸-夺奁奂奋奖奥奸妆-妈妩-妫姗姹娄-娈娱娲娴婳-婶媪嫒嫔嫱嬷孙学孪宁宝实宠审宪宫宽宾寝对寻导寿将尔尘尝尧尴尸尽层屃屉届属屡屦屿岁岂岖-岛岭岽岿峄峡峣-峦崂-崄崭嵘嵚嵝巅巩巯币帅师帏帐帘帜带帧帮帱帻帼幂干并广庄庆庐庑库应庙庞废廪开异弃弑张弥弪弯弹强归当录彝彦彻征径徕御忆忏忧忾怀-怆怜总怼怿恋恒恳恶恸-恽悦悫-悯惊惧-惩惫-惯愠愤愦愿慑懑懒懔戆戋戏戗战戬戯户扑执扩-扬扰抚抛抟-抢护报担拟拢拣拥-择挂挚-挦挽捝-损捡-捣据掳掴掷掸掺掼揽-搂搅携摄-摈摊撄撑撵撷撸撺擞攒敌敛数斋斓斗斩断无旧时-旸昙昼-显晋晒-晖暂暧术朴机杀杂权杆条来杨杩杰松板构枞枢枣枥枧枨枪枫枭柜柠柽栀栅标-栌栎栏树栖栗样栾桠-桩梦梼梾-棂椁椟椠椤椭楼榄榅榇-榉槚槛槟槠横樯樱橥橱橹橼檩欢欤欧歼殁殇残殒殓殚殡殴毁毂毕毙毡毵氇气氢氩氲汇汉汤汹沈沟没沣-沧沩沪泞注泪泶-泸泺-泾洁洒洼浃浅-浈浊测浍-浔涂涛涝-涡涣涤润-涩淀渊渌-渎渐渑渔渖渗温湾湿溃溅溆滗滚滞-滢滤-滦滨-滪漓漤潆潇潋潍潜潴澜濑濒灏灭灯灵灶灾-炀炉炖炜炝点炼炽烁-烃烛烟烦-烩烫-热焕焖焘煴爱爷牍牦牵牺犊状-犹狈狝狞独-狲猃猎猕猡猪-猬献獭玑玚玛玮-玱玺珐珑珰珲琏琐琼瑶瑷璎瓒瓯电画畅畴疖疗疟-疡疬-疯疱疴症-痉痒痖痨痪痫瘅瘆瘗瘘瘪瘫瘾瘿癞癣癫皑皱皲盏-监盖-盘眍眦眬着睁睐睑瞆瞒瞩矫矶矾-码砖砗砚砜砺砻砾础硁硕-硗硙确硷碍碛碜碱礼祃祎祢祯祷祸禀禄禅离秃秆种积称秽秾稆税稣稳穑穷窃窍窎窑窜窝窥窦窭竖竞笃笋笔笕笺笼笾筑筚-筝筹筼签简箓箦-箫篑篓篮篱簖籁籴类籼粜粝粤粪粮糁糇系紧累絷纟-缏缑-缵罂网罗罚罢罴羁羟翘耢耧耸耻聂聋-聍联聩聪肃肠肤肮肴肾-胁胆胜胡胧胨胪胫胶脉脍脏-脑脓脔脚脱脶脸腊腭腻-腾膑臜致舆舍舣舰舱舻艰艳艺节芈芗芜芦芸苁苇苈苋-苏苹范茎茏茑茔茕茧荆荐荙-荜荞-荡荣-药莅莱-莴莶-莺莼萝萤-萨葱蒇蒉蒋蒌蓝蓟蓠蓣蓥蓦蔂蔷蔹蔺蔼蕰蕲蕴薮藓蘖虏虑虚虫虬虮虽-蚂蚕蚬蛊蛎蛏蛮蛰-蛴蜕蜗蝇-蝉蝼蝾螀螨蟏衅衔补表衬衮袄-袆袜袭袯装裆裈裢-裥褛褴见-觑觞触觯訚誉誊讠-谈谊-谷豮贝-赣赪赵赶趋趱趸跃跄跞践-跹跻踊踌踪踬踯蹑蹒蹰蹿躏躜躯车-辚辞辩辫边辽达迁过迈运还这进-迟迩迳迹适选逊递逦逻遗遥邓邝邬邮邹-邻郁郏-郑郓郦郧郸酂酝酦酱酽-酿采释里鉴銮錾钅-镶长门-阛队阳-阶际-陉陕陧-险随隐隶隽难雏雠雳雾霁霡霭靓静面靥鞑鞒鞯韦-韬韵页-颢颤-颧风-飚飞飨餍饣-馕马-骧髅髋髌鬓魇魉鱼-鳣鸟-鹭鹯-鹴鹾麦麸黄黉黡黩黪黾鼋鼍鼗鼹齐齑齿-龌龙-龛龟𡒄𨱏]").freeze();
  public static final UnicodeSet tradOnly = new UnicodeSet("[㠏㩜䊷䋙䋻䝼䯀䰾䱽䲁丟並乾亂亞佇併來侖侶俁係俔俠倀倆倈倉個們倫偉側偵偽傑傖傘備傭傯傳-債傷傾僂僅僉僑僕僞僥僨價儀儂億儈儉儐儔儕儘償優儲儷儸儺-儼兌兒兗內兩冊冪凈凍凜凱別刪剄則剋剎剗剛剝剮剴創劃劇劉劊劌劍劏劑劚勁動務勛勝勞勢勩勱勵勸勻匭匯匱區協卻厙厠厭厲厴參叄叢吒吳吶呂呆咼員唄唚問啓啞啟啢喎喚喪喬單喲嗆嗇嗊嗎嗚嗩嗶嘆嘍嘔嘖嘗嘜嘩嘮-嘰嘵嘸嘽噓噚噝噠噥噦噯噲噴噸噹嚀嚇嚌嚕嚙嚦嚨嚲-嚴嚶囀-囂囅囈囑囪圇國圍園圓圖團垵埡埰執堅堊堖堝堯報場塊塋塏塒塗塢塤塵塹墊墜墮墳墻墾壇壈壋壓壘-壚壞-壠壢壩壯壺壼壽夠夢夾奐奧奩奪奬奮奼妝姍姦娛婁婦婭媧媯媼媽嫗嫵嫻嫿嬀嬈嬋嬌嬙嬡嬤嬪嬰嬸孌孫學孿宮寢實寧審寫寬寵寶將專尋對導尷屆屍屓屜屢層屨屬岡峴島峽崍崗崢崬嵐嶁嶄嶇嶔嶗嶠嶢嶧嶮嶴嶸嶺嶼巋巒巔巰帥師帳帶幀幃幗幘幟幣幫幬幹幺幾庫廁廂廄廈廚廝廟-廣廩廬廳弒弳張強彈彌彎彙彞彥後徑從徠復徵徹恆恥悅悞悵悶惡惱惲惻愛愜愨愴愷愾慄態慍慘慚慟慣慤慪慫慮慳慶憂憊憐-憒憚憤憫憮憲憶懇應懌懍懟懣懨懲懶-懸懺懼懾戀戇戔戧戩戰-戲戶拋挩挾捨捫掃掄掗掙掛採揀揚換揮損搖搗搵搶摑摜摟摯摳摶摻撈撏撐撓撝撟撣撥撫撲撳撻撾撿擁擄擇擊擋擓擔據擠擬擯-擲擴擷擺-擼擾攄攆攏攔攖攙攛-攝攢-攤攪攬敗敘敵數斂斃斕斬斷於時晉晝暈暉暘暢暫曄曆曇曉曏曖曠曨曬書會朧東杴柵桿梔梘條梟梲棄棖棗棟棧棲棶椏楊楓楨業極榪榮榲榿構槍槤槧槨槳樁樂樅樓標樞樣樸-樺橈橋機橢橫檁檉檔檜檟檢檣檮檯檳檸檻櫃櫓櫚櫛櫝-櫟櫥櫧櫨櫪-櫬櫱櫳櫸櫻欄權欏欒欖欞欽歐歟歡歲歷歸歿殘殞殤殨殫殮-殰殲殺-殼毀毆毿氂氈氌氣氫氬氳決沒沖況洶浹涇涼淚淥淪淵淶淺渙減渦測渾湊湞湯溈準溝溫滄滅滌滎滬滯滲滷滸滻滾滿漁漚漢漣漬漲漵漸漿潁潑潔潙潛潤潯潰潷潿澀澆澇澗澠澤澦澩澮澱濁濃濕濘濟濤濫濰濱濺濼濾瀅-瀇瀉瀋瀏瀕瀘瀝瀟瀠瀦-瀨瀲瀾灃灄灑灕灘灝灠灣灤灧災為烏烴無煉煒煙煢煥煩煬煱熅熒熗熱熲熾燁燈燉燒燙燜營燦燭燴燶燼燾爍爐爛爭爲爺爾牆牘牽犖犢犧狀狹狽猙猶猻獁獃-獅獎獨獪獫獮獰-獲獵獷獸獺-獼玀現琺琿瑋瑒瑣瑤瑩瑪瑲璉璣璦璫環璽瓊瓏瓔瓚甌產産畝畢畫異當疇疊痙痾瘂瘋瘍瘓瘞瘡瘧瘮瘲瘺瘻療癆癇癉癘癟癢癤癥癧癩癬-癮癰-癲發皚皰皸皺盜盞盡監盤盧盪眥眾睏睜睞瞘瞜瞞瞶瞼矓矚矯硜硤硨硯碩碭碸確碼磑磚磣磧磯磽礆礎礙礦礪-礬礱祿禍禎禕禡禦禪禮禰禱禿秈稅稈稏稟種稱穀穌-穎穠-穢穩穫穭窩窪窮窯窵窶窺竄竅竇竈竊竪競筆筍筧筴箋箏節範築篋篔篤篩篳簀簍簞簡簣簫簹簽簾籃籌籙籜籟籠籩籪籬籮粵糝糞糧糲糴糶糹糾紀紂約-紉紋納紐紓-紝紡紬細-紳紵紹紺紼紿絀終組-絆絎結絕絛絝絞絡絢給絨絰-絳絶絹綁綃綆綈綉綌綏綐經綜綞綠綢綣綫-維綯-綵綸-綻綽-綿緄緇緊緋緑-緔緗-線緝緞締緡緣緦編緩緬緯緱緲練緶緹緻縈-縋縐縑縕縗縛縝-縟縣縧縫縭縮縱-縳縵-縷縹總績繃繅繆繒織繕繚繞繡繢繩-繫繭-繰繳繸繹繼-繿纈纊續纍纏纓纖纘纜缽罈罌罰罵罷羅羆羈羋羥義習翹耬耮聖聞聯聰聲聳聵-職聹聽聾肅脅脈脛脫脹腎腖腡腦腫腳腸膃膚膠膩膽-膿臉臍臏臘臚臟臠臢臨臺與-舊艙艤艦艫艱艷芻茲荊莊莖莢莧華萇萊萬萵葉葒著葤葦葯葷蒓蒔蒞蒼蓀蓋蓮蓯蓴蓽蔔蔞蔣蔥蔦蔭蕁蕆蕎蕒蕓蕕蕘蕢蕩蕪蕭蕷薀薈薊薌薔薘薟薦薩薳薴薺藍藎藝藥藪藴藶藹藺蘄蘆蘇蘊蘋蘚蘞蘢蘭蘺蘿虆處虛虜號虧虯蛺蛻蜆蝕蝟蝦蝸螄螞螢螮螻螿蟄蟈蟎蟣蟬蟯蟲蟶蟻蠅蠆蠐蠑蠟蠣蠨蠱蠶蠻衆術衕衚衛衝衹袞裊裏補裝裡製複褌褘褲褳褸褻襇襏襖襝襠襤襪襬襯襲覆見覎規覓視覘覡覥覦親覬覯覲覷覺覽覿觀觴觶觸訁-訃計訊訌討訐訒訓訕-記訛訝訟訢訣訥訩訪設許訴訶診註詁詆詎詐詒詔-詘詛詞詠-詣試詩詫-詮詰-詳詵詼詿誄-誇誌認誑誒誕誘誚語誠誡誣-誦誨說説誰課誶誹誼誾調諂諄談諉請諍諏諑諒論諗諛-諞諢諤諦諧諫諭諮諱諳諶-諸諺諼諾謀-謂謄謅謊謎謐謔謖謗謙-講謝謠謡謨謫-謭謳謹謾譅證譎譏譖識-譚譜譫譯議譴護譸譽譾讀變讎讒讓讕讖讜讞豈豎豐豬豶貓貙貝-貢貧-責貯貰貲-貴貶-貸貺-貽貿-賅資賈賊賑-賓賕賙賚賜賞賠-賤賦賧質-賭賰賴賵賺-賾贄贅贇贈贊贋贍贏贐贓贔贖贗贛贜赬趕趙趨趲跡踐踴蹌蹕蹣蹤蹺躂躉-躋躍躑-躓躕躚躡躥躦躪軀車-軍軑軒軔軛軟軤軫軲軸-軼軾較輅輇-輊輒-輕輛-輟輥輦輩輪輬輯輳輸輻輾-轀轂轄-轆轉轍轎轔轟轡轢轤辦辭-辯農逕這連進運過達違遙遜遞遠適遲遷選遺遼邁還邇邊邏邐郟郵鄆鄉鄒鄔鄖鄧鄭鄰鄲鄴鄶鄺酇酈醖醜醞醫醬醱釀釁釃釅釋釐釒-釕釗-釙針釣釤釧釩釵釷釹釺鈀鈁鈃鈄鈈鈉鈍鈎鈐-鈒鈔鈕鈞鈣鈥-鈧鈮鈰鈳鈴鈷-鈺鈽-鉀鉅鉈鉉鉋鉍鉑鉕鉗鉚鉛鉞鉢鉤鉦鉬鉭鉶鉸鉺鉻鉿銀銃銅銍銑銓銖銘銚-銜銠銣銥銦銨-銬銱銳銷銹銻銼鋁鋃鋅鋇鋌鋏鋒鋙鋝鋟鋣-鋦鋨-鋪鋭-鋱鋶鋸鋼錁錄錆-錈錏錐錒錕錘-錛錟-錢錦錨錩錫錮錯録錳錶錸鍀鍁鍃鍆-鍈鍋鍍鍔鍘鍚鍛鍠鍤鍥鍩鍬鍰鍵鍶鍺鍾鎂鎄鎇鎊鎔鎖鎘鎚鎛鎝鎡-鎣鎦鎧鎩鎪鎬鎮鎰鎲鎳鎵鎸鎿鏃鏇鏈鏌鏍鏐鏑鏗鏘鏜-鏟鏡鏢鏤鏨鏰鏵鏷鏹鏽鐃鐋鐐鐒-鐔鐘鐙鐝鐠鐦-鐨鐫鐮鐲鐳鐵鐶鐸鐺鐿鑄鑊鑌鑒鑔鑕鑞鑠鑣鑥鑭鑰-鑲鑷鑹鑼-鑿钁長門閂閃閆閈閉開閌閎閏閑間閔閘閡閣閥閨閩閫-閭閱閲閶閹閻-閿闃闆闈闊-闍闐闒-闖關闞闠闡闤闥阪陘陝陣陰陳陸陽隉隊階隕際隨險隱隴隸隻雋雖雙雛雜雞離難雲電霢霧霽靂靄靈靚靜靦靨鞀鞏鞝鞽韁韃韉韋-韍韓韙韜韞韻響頁-頃項-須頊頌頎-頓頗領頜頡頤頦頭頮頰頲頴頷-頹頻頽顆題-顏顒-顔願顙顛類顢顥顧顫顬顯-顱顳顴風颭-颯颱颳颶颸颺-颼飀飄飆飈飛飠飢飣飥飩-飫飭飯飲飴飼-飿餃-餅餉養餌餎餏餑-餓餕餖餘餚-餜餞餡館餱餳餶餷餺餼餾餿饁饃饅饈-饌饑饒饗饜饞饢馬-馮馱馳馴馹駁駐-駒駔駕駘駙駛駝駟駡駢駭駰駱駸駿騁騂騅騌-騏騖騙騤騧騫騭騮騰騶-騸騾驀-驅驊驌驍驏驕驗驚驛驟驢驤-驦驪驫骯髏髒體-髖髮鬆鬍鬚鬢鬥鬧鬩鬮鬱魎魘魚魛魢魨魯魴魷魺鮁鮃鮊鮋鮍鮎鮐-鮓鮚鮜-鮞鮦鮪鮫鮭鮮鮳鮶鮺鯀鯁鯇鯉鯊鯒鯔-鯗鯛鯝鯡鯢鯤鯧鯨鯪鯫鯰鯴鯷鯽鯿鰁-鰃鰈鰉鰍鰏鰐鰒鰓鰜鰟鰠鰣鰥鰨鰩鰭鰮鰱-鰳鰵鰷鰹-鰼鰾鱂鱅鱈鱉鱒鱔鱖-鱘鱝鱟鱠鱣鱤鱧鱨鱭鱯鱷鱸鱺鳥鳧鳩鳬鳲-鳴鳶鳾鴆鴇鴉鴒鴕鴛鴝-鴟鴣鴦鴨鴯鴰鴴鴷鴻鴿鵁-鵃鵐-鵓鵜鵝鵠鵡鵪鵬鵮鵯鵲鵷鵾鶄鶇鶉鶊鶓鶖鶘鶚鶡鶥鶩鶪鶬鶯鶲鶴鶹-鶼鶿-鷂鷄鷈鷊鷓鷖鷗鷙鷚鷥鷦鷫鷯鷲鷳鷸-鷺鷽鷿鸂鸇鸌鸏鸕鸘鸚鸛鸝鸞鹵鹹鹺鹼鹽麗麥麩麵麼麽黃黌點黨黲黶黷黽黿鼉鼴齊齋齎齏齒齔齕齗齙齜齟-齡齦齪齬齲齶齷龍龎龐龔龕龜𡞵𡠹𡢃𤪺𤫩𧜵𧝞𧩙𧵳𨋢𨦫𨧜𨯅𩣑𩶘]").freeze();
  //public static final UnicodeSet bothSimpTrad = new UnicodeSet("[:sc=han:]").removeAll(simpOnly).removeAll(tradOnly).freeze();
  public static final UnicodeSet specialIPA = new UnicodeSet("[βΒ θΘ χΧ]");

  public static void main(String[] args) throws IOException {
    String resource = getRelativeFileName(FormatSpecialData.class, "ScriptData.txt");
    final UnicodeMap<Set<String>> pivot = new UnicodeMap<Set<String>>();;

    Map<RemapType, Map<String,UnicodeSet>> data = ScriptCategories.getRemapData(resource);
    for (RemapType type : RemapType.values()) {
      System.out.println(type);
      Map<String, UnicodeSet> string2uset = data.get(type);
      if (string2uset == null) {
        System.out.println("null value");
        continue;
      }
      String specialName = type.toString().toLowerCase() + "Specials";
      PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "special/", specialName + ".txt");
      pivot.clear();
      addDataToPivot(string2uset, pivot);
      if (type == RemapType.SCRIPT) {
        addDataToPivot( ScriptCategories.SCRIPT_NEW, pivot);
        add("Hans", simpOnly, pivot);
        add("Hant", tradOnly, pivot);
        //add("Hans", bothSimpTrad, pivot);
        //add("Hant", bothSimpTrad, pivot);
      }
      pivot.putAll(UNASSIGNED, null);
      BagFormatter bf = new BagFormatter();
      bf.setValueSource(new MyUnicodeLabel(type, pivot));
      bf.setLabelSource(new OldUnicodeLabel(type));
      Set<Set<String>> sorted = new TreeSet<Set<String>>(new CollectionComparator<String>());
      sorted.addAll(pivot.values());
      for (Set<String> s : sorted) {
        UnicodeSet value = pivot.getSet(s);
        if (type == RemapType.SCRIPT && value.size() < 100) {
          bf.setMergeRanges(false);
        } else {
          bf.setMergeRanges(true);
        }
        out.println();
        out.println("# " + specialName + ": " + s);
        bf.showSetNames(out, value);
      }
      out.close();
    }
  }
  
  private static void addDataToPivot(Map<String, UnicodeSet> string2uset, final UnicodeMap<Set<String>> pivot) {
    for (String value : string2uset.keySet()) {
      UnicodeSet set = string2uset.get(value);
//      if (value.equals("Han")) {
//        add("Hant", set, pivot);
//        value = "Hans";
//      } 
      add(value, set, pivot);
    }
  }

  private static void add(String value, UnicodeSet set, final UnicodeMap<Set<String>> pivot) {
    for (String ch : set) {
      Set<String> oldValue = pivot.get(ch);
      TreeSet<String> newValue = new TreeSet<String>();
      newValue.add(value);

      if (oldValue != null) {
        newValue.addAll(oldValue);
      }
      pivot.put(ch, newValue);
    }
  }

  static class OldUnicodeLabel extends UnicodeLabel {
    int ICUPropEnum;

    public int getMaxWidth(boolean isShort) {
      return 24;
    }

    public OldUnicodeLabel(RemapType type) {
      ICUPropEnum = getIcuPropEnum(type);
    }


    @Override
    public String getValue(int codepoint, boolean isShort) {
      int code = UCharacter.getIntPropertyValue(codepoint, ICUPropEnum);
      return UCharacter.getPropertyValueName(ICUPropEnum, code, UProperty.NameChoice.SHORT)
      + " (" + UCharacter.getPropertyValueName(ICUPropEnum, code, UProperty.NameChoice.LONG) + ")";
    }
  }

  private static int getIcuPropEnum(RemapType type) {
    int icuProp;
    switch (type) {
    case SCRIPT: icuProp = UProperty.SCRIPT; break;
    default: icuProp = UProperty.GENERAL_CATEGORY; break;
    }
    return icuProp;
  }

  static class MyUnicodeLabel extends UnicodeLabel {
    private UnicodeMap<Set<String>> pivot;
    private int maxWidth;
    int ICUPropEnum;

    public MyUnicodeLabel(RemapType type, UnicodeMap<Set<String>> pivot) {
      this.pivot = pivot;
      this.maxWidth = type == RemapType.CATEGORY ? 4 : 24;
      ICUPropEnum = getIcuPropEnum(type);
    }
    public int getMaxWidth(boolean isShort) {
      return maxWidth;
    }
    public String getValue(int codepoint, boolean isShort) {
      Set<String> set = pivot.get(codepoint);
      Set<String> fixed = new TreeSet<String>();
      for (String s : set) {
        int code = UCharacter.getPropertyValueEnum(ICUPropEnum, s);
        s = UCharacter.getPropertyValueName(ICUPropEnum, code, UProperty.NameChoice.SHORT);
        fixed.add(s);
      }
      return CollectionUtilities.join(fixed, " ");
    }
  }

  public static String getRelativeFileName(Class<FormatSpecialData> class1, String filename) {
    URL resource = FormatSpecialData.class.getResource(filename);
    String resourceString = resource.toString();
    if (!resourceString.startsWith("file:")) {
      throw new IllegalArgumentException("File not found: " + "");
    }
    return resourceString.substring(5);
  }

  // should be in Utilities
  public static final Charset UTF8 = Charset.forName("utf-8");

  public static BufferedReader openFile(Class<?> class1, String file) throws IOException {
    //URL path = null;
    //String externalForm = null;
    try {
      //      //System.out.println("Reading:\t" + file1.getCanonicalPath());
      //      path = class1.getResource(file);
      //      externalForm = path.toExternalForm();
      //      if (externalForm.startsWith("file:")) {
      //        externalForm = externalForm.substring(5);
      //      }
      //      File file1 = new File(externalForm);
      //      boolean x = file1.canRead();
      //      final InputStream resourceAsStream = new FileInputStream(file1);
      final InputStream resourceAsStream = class1.getResourceAsStream(file);
      InputStreamReader reader = new InputStreamReader(resourceAsStream, UTF8);
      BufferedReader bufferedReader = new BufferedReader(reader,1024*64);
      return bufferedReader;
    } catch (Exception e) {
      File file1 = new File(file);
      throw (RuntimeException) new IllegalArgumentException("Bad file name: "
              //              + path + "\t" + externalForm + "\t" + 
              + file1.getCanonicalPath()
              + "\r\n" + new File(".").getCanonicalFile() + " => " + Arrays.asList(new File(".").getCanonicalFile().list())).initCause(e);
    }
  }
}
