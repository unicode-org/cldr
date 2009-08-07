package org.unicode.cldr.draft;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class IdnaFrequency {
    private static final Charset UTF8 = Charset.forName("utf-8");
    private static final Charset LATIN1 = Charset.forName("8859-1");
    private static final boolean DEBUG = true;

    /**
cm000162    3    481    31032    6    22    \xc5\xa2\xc4\x98\xc4\x82M\xc4\x8c\xc5\x96@\xc5\xb9\xc5\xb8.yoll.net    278    41991    0    29    home.\xc5\xa2replug.net
0   c = child, p=parent; m = mapped, u=unmapped; 000162 hex code point
1   3 - count
2   481 - navboost
3   31032 - page rank
4   6 - language
5   22 - encoding
6   url in utf-8 (c-style byte escapes)
7,8,9,10
11  url
12,13,14,15
16 url
...
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        showCharsets();
        Counter<Integer> charTotal = getData(true);
        for (int cp : charTotal.getKeysetSortedByCount(false)) {
            String norm = normalize(cp);
            if (!norm.equals(UTF16.valueOf(cp))) {
                final int dt = UCharacter.getIntPropertyValue(cp, UProperty.DECOMPOSITION_TYPE);
                final String tdName = UCharacter.getPropertyValueName(UProperty.DECOMPOSITION_TYPE, dt, NameChoice.LONG);
                System.out.println(charTotal.getCount(cp) + "\t" + tdName + "\t" + getCodeAndName(cp) + "\t=>\t" + getCodeAndName(norm));
            }
        }
    }
    
    static UnicodeSet testchars = new UnicodeSet("[[:script=greek:]ÄäÖöÜüß]");

    static Counter<Integer> getData(boolean writeOut) throws IOException {
        BufferedReader in = BagFormatter.openUTF8Reader("", CldrUtility.getProperty("idnaFrequency"));
        PrintWriter out = !writeOut ? null : BagFormatter.openUTF8Writer("/Users/markdavis/Desktop/Google/", "idn41-data.txt");
        if (writeOut) {
            out.write((char)0xFEFF);
        }
        Mapper<Language> languageMapper = new Mapper<Language>(Language.values());
        Mapper<Encoding> encodingMapper = new Mapper<Encoding>(Encoding.values());
        Counter<Integer> charTotal = new Counter();

        for (int counter = 0; ; ++counter) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            try {
                String[] parts = line.split("\t");
                boolean child = parts[0].charAt(0) == 'c';
                boolean mapped = parts[0].charAt(0) == 'm';
                int cp = Integer.parseInt(parts[0].substring(2),16);
                long count = Long.parseLong(parts[1]);
                charTotal.add(cp, count);
                if (!testchars.contains(cp)) continue;
                Language lang = languageMapper.fromOrdinal(Integer.parseInt(parts[4]));
                Encoding encoding = encodingMapper.fromOrdinal(Integer.parseInt(parts[5]));
                String url1 = unescape(parts[6], Encoding.UTF8);
                String url2 = unescape(parts[11], Encoding.UTF8);
                String url3 = unescape(parts[16], Encoding.UTF8);
                if (writeOut) {
                    out.println(counter + "\t" + getCodeAndName(cp) + "\t" + count + "\t" + url2);
                }
            } catch (Exception e) {
                System.err.println("ERROR: " + line);
            }
        }
        in.close();
        if (writeOut) {
            out.close();
        }
        return charTotal;
    }

    private static void showCharsets() {
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        HashMap<Charset,Set<String>> charset2strings = new HashMap();
        for (String charsetname : charsets.keySet()) {
            Charset charset = charsets.get(charsetname);
            Set<String> set = charset2strings.get(charset);
            if (set == null) {
                charset2strings.put(charset, set = new TreeSet<String>());
            }
            set.add(charsetname);
        }
        for (Charset charset : charset2strings.keySet()) {
            System.out.println(charset.name() + "\t" + charset2strings.get(charset));
        }
    }

    static UnicodeSet diSet = new UnicodeSet("[\\u034F \\u180B-\\u180D\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\u206A-\\u206F \\uFE00-\\uFE0F\\uFEFF\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F \\U000E0100-\\U000E01EF \\u00AD \\u17B4 \\u17B5 \\u115F \\u1160\\u3164\\uFFA0 \\u2065-\\u2069 \\uFFF0-\\uFFF8]");
    static Matcher defaultIgnorables = Pattern.compile(diSet.toPattern(false), Pattern.COMMENTS).matcher("");
    private static String normalize(int cp) {
        String a = Normalizer.normalize(cp, Normalizer.NFKC);
        String b = UCharacter.foldCase(a, true);
        String c = defaultIgnorables.reset(b).replaceAll("");
        String d = Normalizer.normalize(c, Normalizer.NFKC);
        return d;
    }

    private static String getCodeAndName(int cp) {
        return "U+" + Utility.hex(cp, 4) + "\t( " + com.ibm.icu.text.UTF16.valueOf(cp) + " )\t" + UCharacter.getName(cp);
    }

    private static String getCodeAndName(String cp) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < cp.length(); ++i) {
            if (i != 0) {
                buffer.append("; ");
            }
            buffer.append(getCodeAndName(cp.charAt(i)));
        }
        return buffer.toString();
    }


    private static String unescape(String string, Encoding encoding) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        int chBuffer = 0;
        for (int i = 0; i < string.length(); ++i) {
            char b = string.charAt(i);
            switch (state) {
            case 0:
                if (b == '\\') {
                    state = 1;
                } else {
                    out.write(b);
                }
                break;
            case 1:
                if (b != 'x') {
                    out.write(b);
                    state = 0;
                } else {
                    state = 2;
                }
                break;
            case 2:
                chBuffer = getNybble(b) << 4;
                state = 3;
                break;
            case 3:
                chBuffer |= getNybble(b);
                out.write((byte)chBuffer);
                state = 0;
                break;
            }
        }
        out.close();
        try {
            return new String(out.toByteArray(), encoding.charset.toString());
        } catch (Exception e) {
            FAILURES.add(encoding, 1);
            return new String(out.toByteArray(), LATIN1.toString());
        }
    }

    static Counter<Encoding> FAILURES = new Counter<Encoding>();

    private static int getNybble(char b) {
        b -= '0';
        if (b < 0) {
            throw new IllegalArgumentException();
        }
        if (b < 10) return b;
        b -= 'a' - '0';
        if (b < 0 || b > 5) {
            throw new IllegalArgumentException();
        }
        return b + 10;
    }

    public enum Encoding {
        // 0: Teragram ASCII
        ISO_8859_1(0, "ASCII", "ISO-8859-1"),
        // 1: Teragram Latin2
        ISO_8859_2(1, "Latin2", "ISO-8859-2"),
        // 2: in BasisTech but not in Teragram
        ISO_8859_3(2, "Latin3", "ISO-8859-3"),
        // 3: Teragram Latin4
        ISO_8859_4(3, "Latin4", "ISO-8859-4"),
        // 4: Teragram ISO-8859-5
        ISO_8859_5(4, "ISO-8859-5", "ISO-8859-5"),
        // 5: Teragram Arabic
        ISO_8859_6(5, "Arabic", "ISO-8859-6"),
        // 6: Teragram Greek
        ISO_8859_7(6, "Greek", "ISO-8859-7"),
        // 7: Teragram Hebrew
        ISO_8859_8(7, "Hebrew", "ISO-8859-8"),
        // 8: in BasisTech but not in Teragram
        ISO_8859_9(8, "Latin5", "ISO-8859-9"),
        // 9: in BasisTech but not in Teragram
        ISO_8859_10(9, "Latin6", "ISO-8859-10"),
        // 10: Teragram EUC_JP
        JAPANESE_EUC_JP(10, "EUC-JP",  "EUC-JP"),
        // 11: Teragram SJS
        JAPANESE_SHIFT_JIS(11, "SJS", "Shift_JIS"),
        // 12: Teragram JIS
        JAPANESE_JIS(12, "JIS", "ISO-2022-JP"),
        // 13: Teragram BIG5
        CHINESE_BIG5(13, "BIG5", "Big5"),
        // 14: Teragram GB
        CHINESE_GB(14, "GB",  "GB2312"),
        // 15: Misnamed. Should be EUC_TW. Was Basis Tech
        //      CNS11643EUC, before that Teragram EUC-CN(!)
        //      See //i18n/basistech/basistech_encodings.h
        CHINESE_EUC_CN(15, "EUC-CN", "EUC-CN"),
        // 16: Teragram KSC
        KOREAN_EUC_KR(16, "KSC", "EUC-KR"),
        // 17: Teragram Unicode
        UNICODE(17, "Unicode", "UTF-16LE"),
        // 18: Misnamed. Should be EUC_TW. Was Basis Tech
        //      CNS11643EUC, before that Teragram EUC.
        CHINESE_EUC_DEC(18, "EUC", "EUC-TW"),
        // 19: Misnamed. Should be EUC_TW. Was Basis Tech
        //      CNS11643EUC, before that Teragram CNS.
        CHINESE_CNS(19, "CNS", "CNS"),
        // 20: Teragram BIG5_CP950
        CHINESE_BIG5_CP950(20, "BIG5-CP950", "BIG5-CP950"),
        // 21: Teragram CP932
        JAPANESE_CP932(21, "CP932", "CP932"),
        // 22
        UTF8(22, "UTF8", "UTF-8"),
        // 23
        UNKNOWN_ENCODING(23, "Unknown", "x-unknown"),
        // 24: ISO_8859_1 with all characters <= 127.
        //      Should be present only in the crawler
        //      and in the repository,
        //      *never* as a result of Document::encoding().
        ASCII_7BIT(24, "ASCII-7-bit", "US-ASCII"),
        // 25: Teragram KOI8R
        RUSSIAN_KOI8_R(25, "KOI8R", "KOI8-R"),
        // 26: Teragram CP1251
        RUSSIAN_CP1251(26, "CP1251", "windows-1251"),
        //----------------------------------------------------------
        // These are _not_ output from teragram. Instead, they are as
        // detected in the headers of usenet articles.
        // 27: CP1252 aka MSFT euro ascii
        MSFT_CP1252(27, "CP1252", "windows-1252"),
        // 28: CP21866 aka KOI8-U, used for Ukrainian.
        //      Misnamed, this is _not_ KOI8-RU but KOI8-U.
        //      KOI8-U is used much more often than KOI8-RU.
        RUSSIAN_KOI8_RU(28, "KOI8U", "KOI8-U"),
        // 29: CP1250 aka MSFT eastern european
        MSFT_CP1250(29, "CP1250", "windows-1250"),
        // 30: aka ISO_8859_0 aka ISO_8859_1 euroized
        ISO_8859_15(30, "ISO-8859-15", "ISO-8859-15"),
        //----------------------------------------------------------
        //----------------------------------------------------------
        // These are in BasisTech but not in Teragram. They are
        // needed for new interface languages. Now detected by
        // research langid
        // 31: used for Turkish
        MSFT_CP1254(31, "CP1254", "windows-1254"),
        // 32: used in Baltic countries
        MSFT_CP1257(32, "CP1257", "windows-1257"),
        //----------------------------------------------------------
        //----------------------------------------------------------
        //----------------------------------------------------------
        // New encodings detected by Teragram
        // 33: aka TIS-620, used for Thai
        ISO_8859_11(33, "ISO-8859-11", "ISO-8859-11"),
        // 34: used for Thai
        MSFT_CP874(34, "CP874", "windows-874"),
        // 35: used for Arabic
        MSFT_CP1256(35, "CP1256", "windows-1256"),
        //----------------------------------------------------------
        // Detected as ISO_8859_8 by Teragram, but can be found in META tags
        // 36: Logical Hebrew Microsoft
        MSFT_CP1255(36, "CP1255", "windows-1255"),
        // 37: Iso Hebrew Logical
        ISO_8859_8_I(37, "ISO-8859-8-I", "ISO-8859-8-I"),
        // 38: Iso Hebrew Visual
        HEBREW_VISUAL(38, "VISUAL", "ISO-8859-8"),
        //----------------------------------------------------------
        //----------------------------------------------------------
        // Detected by research langid
        // 39
        CZECH_CP852(39, "CP852", "cp852"),
        // 40: aka ISO_IR_139 aka KOI8_CS
        CZECH_CSN_369103(40, "CSN_369103", "csn_369103"),
        // 41: used for Greek
        MSFT_CP1253(41, "CP1253", "windows-1253"),
        // 42
        RUSSIAN_CP866(42, "CP866", "IBM866"),
        //----------------------------------------------------------
        //----------------------------------------------------------
        // Handled by iconv in glibc
        // 43
        ISO_8859_13(43, "ISO-8859-13", "ISO-8859-13"),
        // 44
        ISO_2022_KR(44, "ISO-2022-KR", "ISO-2022-KR"),
        // 45
        GBK(45, "GBK", "GBK"),
        // 46
        GB18030(46, "GB18030", "GB18030"),
        // 47
        BIG5_HKSCS(47, "BIG5_HKSCS", "BIG5-HKSCS"),
        // 48
        ISO_2022_CN(48, "ISO_2022_CN", "ISO-2022-CN"),
        //-----------------------------------------------------------
        // Detected by xin liu's detector
        // Handled by transcoder
        // (Indic encodings)
        // 49
        TSCII(49, "TSCII", "tscii"),
        // 50
        TAMIL_MONO(50, "TAM", "tam"),
        // 51
        TAMIL_BI(51, "TAB", "tab"),
        // 52
        JAGRAN(52, "JAGRAN", "jagran"),
        // 53
        MACINTOSH_ROMAN(53, "MACINTOSH", "MACINTOSH"),
        // 54
        UTF7(54, "UTF7", "UTF-7"),
        // 55 Indic encoding - Devanagari
        BHASKAR(55, "BHASKAR", "bhaskar"),
        // 56 Indic encoding - Devanagari
        HTCHANAKYA(56, "HTCHANAKYA", "htchanakya"),
        //-----------------------------------------------------------
        // These allow a single place (inputconverter and outputconverter)
        // to do UTF-16 <==> UTF-8 bulk conversions and UTF-32 <==> UTF-8
        // bulk conversions, with interchange-valid checking on input and
        // fallback if needed on ouput.
        // 57 big-endian UTF-16
        UTF16BE(57, "UTF-16BE", "UTF-16BE"),
        // 58 little-endian UTF-16
        UTF16LE(58, "UTF-16LE", "UTF-16LE"),
        // 59 big-endian UTF-32
        UTF32BE(59, "UTF-32BE", "UTF-32BE"),
        // 60 little-endian UTF-32
        UTF32LE(60, "UTF-32LE", "UTF-32LE"),
        //-----------------------------------------------------------
        //-----------------------------------------------------------
        // An encoding that means "This is not text, but it may have some
        // simple ASCII text embedded". Intended input conversion (not yet
        // implemented) is to keep strings of >=4 seven-bit ASCII characters
        // (follow each kept string with an ASCII space), delete the rest of
        // the bytes. This will pick up and allow indexing of e.g. captions
        // in JPEGs. No output conversion needed.
        // 61
        BINARYENC(61, "X-BINARYENC", "x-binaryenc"),
        //-----------------------------------------------------------
        //-----------------------------------------------------------
        // Some Web pages allow a mixture of HZ-GB and GB-2312 by using
        // ~{ ... ~} for 2-byte pairs, and the browsers support this.
        // 62
        HZ_GB_2312(62, "HZ-GB-2312", "HZ-GB-2312");

        private final int encodingNum;
        private final String encodingName;
        private final String mimeEncodingName;
        private final Charset charset;

        //-----------------------------------------------------------
        private Encoding(int encodingNum, String encodingName, String mimeEncodingName) {
            this.encodingNum = encodingNum;
            this.encodingName = encodingName;
            this.mimeEncodingName = mimeEncodingName;
            Charset charset0;
            try {
                charset0 = Charset.forName(mimeEncodingName);
            } catch (Exception e) {
                charset0 = null;
                System.out.println("Couldn't make: " + mimeEncodingName);
            }
            this.charset = charset0;
        }
        public Charset getCharset() {
            return charset;
        }
        public String toString() {
            return encodingNum + ":" + encodingName + ":" + mimeEncodingName;
        }
    }

    static class Mapper<T extends Enum> {
        T[] items;
        public Mapper(T[] languages) {
            items = languages;
        }
        T fromOrdinal(int ordinal) {
            return items[ordinal];
        }
    }

    enum Language {
        ENGLISH,
        DANISH,
        DUTCH,
        FINNISH,
        FRENCH,
        GERMAN,
        HEBREW,
        ITALIAN,
        JAPANESE,
        KOREAN,
        NORWEGIAN,
        POLISH,
        PORTUGUESE,
        RUSSIAN,
        SPANISH,
        SWEDISH,
        CHINESE,
        CZECH,
        GREEK,
        ICELANDIC,
        LATVIAN,
        LITHUANIAN,
        ROMANIAN,
        HUNGARIAN,
        ESTONIAN,
        TG_UNKNOWN_LANGUAGE,
        UNKNOWN_LANGUAGE,
        BULGARIAN,
        CROATIAN,
        SERBIAN,
        IRISH,
        GALICIAN,
        TAGALOG,
        TURKISH,
        UKRAINIAN,
        HINDI,
        MACEDONIAN,
        BENGALI,
        INDONESIAN,
        LATIN,
        MALAY,
        MALAYALAM,
        WELSH,
        NEPALI,
        TELUGU,
        ALBANIAN,
        TAMIL,
        BELARUSIAN,
        JAVANESE,
        OCCITAN,
        URDU,
        BIHARI,
        GUJARATI,
        THAI,
        ARABIC,
        CATALAN,
        ESPERANTO,
        BASQUE,
        INTERLINGUA,
        KANNADA,
        PUNJABI,
        SCOTS_GAELIC,
        SWAHILI,
        SLOVENIAN,
        MARATHI,
        MALTESE,
        VIETNAMESE,
        FRISIAN,
        SLOVAK,
        CHINESE_T,
        FAROESE,
        SUNDANESE,
        UZBEK,
        AMHARIC,
        AZERBAIJANI,
        GEORGIAN,
        TIGRINYA,
        PERSIAN,
        BOSNIAN,
        SINHALESE,
        NORWEGIAN_N,
        PORTUGUESE_P,
        PORTUGUESE_B,
        XHOSA,
        ZULU,
        GUARANI,
        SESOTHO,
        TURKMEN,
        KYRGYZ,
        BRETON,
        TWI,
        YIDDISH,
        SERBO_CROATIAN,
        SOMALI,
        UIGHUR,
        KURDISH,
        MONGOLIAN,
        ARMENIAN,
        LAOTHIAN,
        SINDHI,
        RHAETO_ROMANCE,
        AFRIKAANS,
        LUXEMBOURGISH,
        BURMESE,
        KHMER,
        TIBETAN,
        DHIVEHI,
        CHEROKEE,
        SYRIAC,
        LIMBU,
        ORIYA,
        ASSAMESE,
        CORSICAN,
        INTERLINGUE,
        KAZAKH,
        LINGALA,
        MOLDAVIAN,
        PASHTO,
        QUECHUA,
        SHONA,
        TAJIK,
        TATAR,
        TONGA,
        YORUBA,
        CREOLES_AND_PIDGINS_ENGLISH_BASED,
        CREOLES_AND_PIDGINS_FRENCH_BASED,
        CREOLES_AND_PIDGINS_PORTUGUESE_BASED,
        CREOLES_AND_PIDGINS_OTHER,
        MAORI,
        WOLOF,
        ABKHAZIAN,
        AFAR,
        AYMARA,
        BASHKIR,
        BISLAMA,
        DZONGKHA,
        FIJIAN,
        GREENLANDIC,
        HAUSA,
        HAITIAN_CREOLE,
        INUPIAK,
        INUKTITUT,
        KASHMIRI,
        KINYARWANDA,
        MALAGASY,
        NAURU,
        OROMO,
        RUNDI,
        SAMOAN,
        SANGO,
        SANSKRIT,
        SISWANT,
        TSONGA,
        TSWANA,
        VOLAPUK,
        ZHUANG,
        KHASI,
        SCOTS,
        GANDA,
        MANX,
        MONTENEGRIN,
        AKAN,
        IGBO,
        MAURITIAN_CREOLE
    }
}
