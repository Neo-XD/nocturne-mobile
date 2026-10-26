/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()
    
    // Regex for rich sync format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
    private val RICH_SYNC_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.+)".toRegex()
    private val RICH_SYNC_WORD_REGEX = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>\\s*([^<]+)".toRegex()
    
    // Regex for agent and background markers
    private val AGENT_REGEX = "\\{agent:([^}]+)\\}".toRegex()
    private val BACKGROUND_REGEX = "^\\{bg\\}".toRegex()

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Digraphs (Yoon - combinations like kya, sho) - Katakana & Hiragana
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        // Basic Katakana Characters
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", "ン" to "n",
        // Dakuten (voiced consonants)
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do",
        // Handakuten (p-sounds for 'h' group)
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo",
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po",
        // Basic Hiragana Characters
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "を" to "o", "ん" to "n",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        // Choonpu (long vowel mark)
        "ー" to ""
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "\u1100" to "g", "\u1101" to "kk", "\u1102" to "n", "\u1103" to "d",
            "\u1104" to "tt", "\u1105" to "r", "\u1106" to "m", "\u1107" to "b",
            "\u1108" to "pp", "\u1109" to "s", "\u110A" to "ss", "\u110B" to "",
            "\u110C" to "j", "\u110D" to "jj", "\u110E" to "ch", "\u110F" to "k",
            "\u1110" to "t", "\u1111" to "p", "\u1112" to "h"
        ),
        "jung" to mapOf(
            "\u1161" to "a", "\u1162" to "ae", "\u1163" to "ya", "\u1164" to "yae",
            "\u1165" to "eo", "\u1166" to "e", "\u1167" to "yeo", "\u1168" to "ye",
            "\u1169" to "o", "\u116A" to "wa", "\u116B" to "wae", "\u116C" to "oe",
            "\u116D" to "yo", "\u116E" to "u", "\u116F" to "wo", "\u1170" to "we",
            "\u1171" to "wi", "\u1172" to "yu", "\u1173" to "eu", "\u1174" to "eui",
            "\u1175" to "i"
        ),
        "jong" to mapOf(
            "\u11A8" to "k", "\u11A8\u110B" to "g", "\u11A8\u1102" to "ngn", "\u11A8\u1105" to "ngn", "\u11A8\u1106" to "ngm", "\u11A8\u1112" to "kh",
            "\u11A9" to "kk", "\u11A9\u110B" to "kg", "\u11A9\u1102" to "ngn", "\u11A9\u1105" to "ngn", "\u11A9\u1106" to "ngm", "\u11A9\u1112" to "kh",
            "\u11AA" to "k", "\u11AA\u110B" to "ks", "\u11AA\u1102" to "ngn", "\u11AA\u1105" to "ngn", "\u11AA\u1106" to "ngm", "\u11AA\u1112" to "kch",
            "\u11AB" to "n", "\u11AB\u1105" to "ll", "\u11AC" to "n", "\u11AC\u110B" to "nj", "\u11AC\u1102" to "nn", "\u11AC\u1105" to "nn",
            "\u11AC\u1106" to "nm", "\u11AC\u1112" to "nch", "\u11AD" to "n", "\u11AD\u110B" to "nh", "\u11AD\u1105" to "nn", "\u11AE" to "t",
            "\u11AE\u110B" to "d", "\u11AE\u1102" to "nn", "\u11AE\u1105" to "nn", "\u11AE\u1106" to "nm", "\u11AE\u1112" to "th", "\u11AF" to "l",
            "\u11AF\u110B" to "r", "\u11AF\u1102" to "ll", "\u11AF\u1105" to "ll", "\u11B0" to "k", "\u11B0\u110B" to "lg", "\u11B0\u1102" to "ngn",
            "\u11B0\u1105" to "ngn", "\u11B0\u1106" to "ngm", "\u11B0\u1112" to "lkh", "\u11B1" to "m", "\u11B1\u110B" to "lm", "\u11B1\u1102" to "mn",
            "\u11B1\u1105" to "mn", "\u11B1\u1106" to "mm", "\u11B1\u1112" to "lmh", "\u11B2" to "p", "\u11B2\u110B" to "lb", "\u11B2\u1102" to "mn",
            "\u11B2\u1105" to "mn", "\u11B2\u1106" to "mm", "\u11B2\u1112" to "lph", "\u11B3" to "t", "\u11B3\u110B" to "ls", "\u11B3\u1102" to "nn",
            "\u11B3\u1105" to "nn", "\u11B3\u1106" to "nm", "\u11B3\u1112" to "lsh", "\u11B4" to "t", "\u11B4\u110B" to "lt", "\u11B4\u1102" to "nn",
            "\u11B4\u1105" to "nn", "\u11B4\u1106" to "nm", "\u11B4\u1112" to "lth", "\u11B5" to "p", "\u11B5\u110B" to "lp", "\u11B5\u1102" to "mn",
            "\u11B5\u1105" to "mn", "\u11B5\u1106" to "mm", "\u11B5\u1112" to "lph", "\u11B6" to "l", "\u11B6\u110B" to "lh", "\u11B6\u1102" to "ll",
            "\u11B6\u1105" to "ll", "\u11B6\u1106" to "lm", "\u11B6\u1112" to "lh", "\u11B7" to "m", "\u11B7\u1105" to "mn", "\u11B8" to "p",
            "\u11B8\u110B" to "b", "\u11B8\u1102" to "mn", "\u11B8\u1105" to "mn", "\u11B8\u1106" to "mm", "\u11B8\u1112" to "ph", "\u11B9" to "p",
            "\u11B9\u110B" to "ps", "\u11B9\u1102" to "mn", "\u11B9\u1105" to "mn", "\u11B9\u1106" to "mm", "\u11B9\u1112" to "psh", "\u11BA" to "t",
            "\u11BA\u110B" to "s", "\u11BA\u1102" to "nn", "\u11BA\u1105" to "nn", "\u11BA\u1106" to "nm", "\u11BA\u1112" to "sh", "\u11BB" to "t",
            "\u11BB\u110B" to "ss", "\u11BB\u1102" to "tn", "\u11BB\u1105" to "tn", "\u11BB\u1106" to "nm", "\u11BB\u1112" to "th", "\u11BC" to "ng",
            "\u11BD" to "t", "\u11BD\u110B" to "j", "\u11BD\u1102" to "nn", "\u11BD\u1105" to "nn", "\u11BD\u1106" to "nm", "\u11BD\u1112" to "ch",
            "\u11BE" to "t", "\u11BE\u110B" to "ch", "\u11BE\u1102" to "nn", "\u11BE\u1105" to "nn", "\u11BE\u1106" to "nm", "\u11BE\u1112" to "ch",
            "\u11BF" to "k", "\u11BF\u110B" to "k", "\u11BF\u1102" to "ngn", "\u11BF\u1105" to "ngn", "\u11BF\u1106" to "ngm", "\u11BF\u1112" to "kh",
            "\u11C0" to "t", "\u11C0\u110B" to "t", "\u11C0\u1102" to "nn", "\u11C0\u1105" to "nn", "\u11C0\u1106" to "nm", "\u11C0\u1112" to "th",
            "\u11C1" to "p", "\u11C1\u110B" to "p", "\u11C1\u1102" to "mn", "\u11C1\u1105" to "mn", "\u11C1\u1106" to "mm", "\u11C1\u1112" to "ph",
            "\u11C2" to "t", "\u11C2\u110B" to "h", "\u11C2\u1102" to "nn", "\u11C2\u1105" to "nn", "\u11C2\u1106" to "mm", "\u11C2\u1103" to "t",
            "\u11C2\u1100" to "k"
        )
    )

    private val DEVANAGARI_ROMAJI_MAP: Map<String, String> = mapOf(
        "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ee", "उ" to "u", "ऊ" to "oo",
        "ऋ" to "ri", "ए" to "e", "ऐ" to "ai", "ओ" to "o", "औ" to "au",
        "क" to "k", "ख" to "kh", "ग" to "g", "घ" to "gh", "ङ" to "ng",
        "च" to "ch", "छ" to "chh", "ज" to "j", "झ" to "jh", "ञ" to "ny",
        "ट" to "t", "ठ" to "th", "ड" to "d", "ढ" to "dh", "ण" to "n",
        "त" to "t", "थ" to "th", "द" to "d", "ध" to "dh", "न" to "n",
        "प" to "p", "फ" to "ph", "ब" to "b", "भ" to "bh", "म" to "m",
        "य" to "y", "र" to "r", "ल" to "l", "व" to "v",
        "श" to "sh", "ष" to "sh", "स" to "s", "ह" to "h",
        "क्ष" to "ksh", "त्र" to "tr", "ज्ञ" to "gy", "श्र" to "shr",
        "ा" to "aa", "ि" to "i", "ी" to "ee", "ु" to "u", "ू" to "oo",
        "ृ" to "ri", "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au",
        "ं" to "n", "ः" to "h", "ँ" to "n", "्" to "", "़" to "",
        "०" to "0", "१" to "1", "२" to "2", "३" to "3", "४" to "4",
        "५" to "5", "६" to "6", "७" to "7", "८" to "8", "९" to "9",
        "ॐ" to "Om", "ऽ" to "",
        "क़" to "q", "ख़" to "kh", "ग़" to "g", "ज़" to "z", "ड़" to "r", "ढ़" to "rh", "फ़" to "f", "य़" to "y"
    )

    private val GURMUKHI_ROMAJI_MAP: Map<String, String> = mapOf(
        "ੳ" to "o", "ਅ" to "a", "ੲ" to "e", "ਸ" to "s", "ਹ" to "h",
        "ਕ" to "k", "ਖ" to "kh", "ਗ" to "g", "ਘ" to "gh", "ਙ" to "ng",
        "ਚ" to "ch", "ਛ" to "chh", "ਜ" to "j", "ਝ" to "jh", "ਞ" to "ny",
        "ਟ" to "t", "ਠ" to "th", "ਡ" to "d", "ਢ" to "dh", "ण" to "n",
        "ਤ" to "t", "ਥ" to "th", "ਦ" to "d", "ਧ" to "dh", "ਨ" to "n",
        "ਪ" to "p", "ਫ" to "ph", "ਬ" to "b", "ਭ" to "bh", "ਮ" to "m",
        "ਯ" to "y", "ਰ" to "r", "ਲ" to "l", "ਵ" to "v", "ੜ" to "r",
        "ਸ਼" to "sh", "ਖ਼" to "kh", "ਗ਼" to "g", "ਜ਼" to "z", "ਫ਼" to "f", "ਲ਼" to "l",
        "ਾ" to "aa", "ਿ" to "i", "ी" to "ee", "ੁ" to "u", "ੂ" to "oo",
        "ੇ" to "e", "ੈ" to "ai", "ੋ" to "o", "ौ" to "au",
        "ੰ" to "n", "ਂ" to "n", "ੱ" to "", "੍" to "", "਼" to "",
        "ੴ" to "Ek Onkar",
        "੦" to "0", "੧" to "1", "੨" to "2", "੩" to "3", "੪" to "4",
        "੫" to "5", "੬" to "6", "੭" to "7", "੮" to "8", "੯" to "9"
    )

    private val GENERAL_CYRILLIC_ROMAJI_MAP: Map<String, String> = mapOf(
        "А" to "A", "Б" to "B", "В" to "V", "Г" to "G", "Ґ" to "G", "Д" to "D",
        "Ѓ" to "G", "Ђ" to "Dj", "Е" to "E", "Ё" to "Yo", "Є" to "Ye", "Ж" to "Zh",
        "З" to "Z", "Ѕ" to "Dz", "И" to "I", "І" to "I", "Ї" to "Yi", "Й" to "Y",
        "Ј" to "J", "К" to "K", "Л" to "L", "Љ" to "Lj", "М" to "M", "Н" to "N",
        "Њ" to "Nj", "О" to "O", "П" to "P", "Р" to "R", "С" to "S", "Т" to "T",
        "Ћ" to "C", "У" to "U", "Ў" to "U", "Ф" to "F", "Х" to "Kh", "Ц" to "Ts",
        "Ч" to "Ch", "Џ" to "Dz", "Ш" to "Sh", "Щ" to "Shch", "Ъ" to "\"", "Ы" to "Y",
        "Ь" to "'", "Э" to "E", "Ю" to "Yu", "Я" to "Ya",
        "Ө" to "O", "Ү" to "U", "Ң" to "Ng",

        "а" to "a", "б" to "b", "в" to "v", "г" to "g", "ґ" to "g", "д" to "d",
        "ѓ" to "g", "ђ" to "dj", "е" to "e", "ё" to "yo", "є" to "ye", "ж" to "zh",
        "з" to "z", "ѕ" to "dz", "и" to "i", "і" to "i", "ї" to "yi", "й" to "y",
        "ј" to "j", "к" to "k", "л" to "l", "љ" to "lj", "м" to "m", "н" to "n",
        "њ" to "nj", "о" to "o", "п" to "p", "р" to "r", "с" to "s", "т" to "t",
        "ћ" to "c", "у" to "u", "ў" to "u", "ф" to "f", "х" to "kh", "ц" to "ts",
        "ч" to "ch", "џ" to "dz", "ш" to "sh", "щ" to "shch", "ъ" to "\"", "ы" to "y",
        "ь" to "'", "э" to "e", "ю" to "yu", "я" to "ya",
        "ө" to "o", "ү" to "u", "ң" to "ng"
    )

    private val RUSSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "ого" to "ovo", "Ого" to "Ovo", "его" to "evo", "Его" to "Evo"
    )

    private val UKRAINIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h",
        "Ґ" to "G", "ґ" to "g",
        "Є" to "Ye", "є" to "ye",
        "І" to "I", "і" to "i",
        "Ї" to "Yi", "ї" to "yi"
    )

    private val SERBIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ђ" to "Dj", "Љ" to "Lj", "Њ" to "Nj", "Ћ" to "C", "Ц" to "C",
        "Џ" to "Dz", "Ј" to "J", "Х" to "H",

        "ђ" to "dj", "љ" to "lj", "њ" to "nj", "ћ" to "c", "ц" to "c",
        "џ" to "dz", "ј" to "j", "х" to "h"
    )

    private val BULGARIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ж" to "Zh", "Ц" to "Ts", "Ч" to "Ch", "Ш" to "Sh", "Щ" to "Sht",
        "Ъ" to "A", "Ь" to "Y", "Ю" to "Yu", "Я" to "Ya",

        "ж" to "zh", "ц" to "ts", "ч" to "ch", "ш" to "sh", "щ" to "sht",
        "ъ" to "a", "ь" to "y", "ю" to "yu", "я" to "ya"
    )

    private val BELARUSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h", "Ў" to "W", "ў" to "w"
    )

    private val KYRGYZ_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ө" to "O", "ө" to "o", "Ү" to "Y", "ү" to "y", "Ң" to "Ng", "ң" to "ng"
    )

    private val MACEDONIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ѓ" to "Gj", "Ѕ" to "Dz", "И" to "I", "Ј" to "J", "Љ" to "Lj",
        "Њ" to "Nj", "Ќ" to "Kj", "Џ" to "Dz", "Ч" to "Ch", "Ш" to "Sh",
        "Ж" to "Zh", "Ц" to "C", "Х" to "H",

        "ѓ" to "gj", "ѕ" to "dz", "и" to "i", "ј" to "j", "љ" to "lj",
        "њ" to "nj", "ќ" to "kj", "џ" to "dz", "ч" to "ch", "ш" to "sh",
        "ж" to "zh", "ц" to "c", "х" to "h"
    )

    private val RUSSIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н",
        "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь",
        "Э", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н",
        "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь",
        "э", "ю", "я"
    )

    private val UKRAINIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Ґ", "Д", "Е", "Є", "Ж", "З", "И", "І", "Ї", "Й",
        "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч",
        "Ш", "Щ", "Ь", "Ю", "Я",

        "а", "б", "в", "г", "ґ", "д", "е", "є", "ж", "з", "и", "і", "ї", "й",
        "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч",
        "ш", "щ", "ь", "ю", "я"
    )

    private val SERBIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Ђ", "Е", "Ж", "З", "И", "Ј", "К", "Л", "Љ", "М",
        "Н", "Њ", "О", "П", "Р", "С", "Т", "Ћ", "У", "Ф", "Х", "Ц", "Ч", "Џ", "Ш",

        "а", "б", "в", "г", "д", "ђ", "е", "ж", "з", "и", "ј", "к", "л", "љ", "м",
        "н", "њ", "о", "п", "р", "с", "т", "ћ", "у", "ф", "х", "ц", "ч", "џ", "ш"
    )

    private val BULGARIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "Й", "К", "Л", "М",
        "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ",
        "Ъ", "Ь", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ж", "з", "и", "й", "к", "л", "м",
        "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ",
        "ъ", "ь", "ю", "я"
    )

    private val BELARUSIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "І", "Й", "К", "Л", "М", "Н",
        "О", "П", "Р", "С", "Т", "У", "Ў", "Ф", "Х", "Ц", "Ч", "Ш", "Ы", "Ь", "Э",
        "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "і", "й", "к", "л", "м", "н",
        "о", "п", "р", "с", "т", "у", "ў", "ф", "х", "ц", "ч", "ш", "ы", "ь", "э",
        "ю", "я"
    )

    private val KYRGYZ_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н",
        "Ң", "О", "Ө", "П", "Р", "С", "Т", "У", "Ү", "Ф", "Х", "Ц", "Ч", "Ш", "Щ",
        "Ъ", "Ы", "Ь", "Э", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н",
        "ң", "о", "ө", "п", "р", "с", "т", "у", "ү", "ф", "х", "ц", "ч", "ш", "щ",
        "ъ", "ы", "ь", "э", "ю", "я"
    )

    private val MACEDONIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Ѓ", "Е", "Ж", "З", "Ѕ", "И", "Ј", "К", "Л",
        "Љ", "М", "Н", "Њ", "О", "П", "Р", "С", "Т", "Ќ", "У", "Ф", "Х", "Ц",
        "Ч", "Џ", "Ш",

        "а", "б", "в", "г", "д", "ѓ", "е", "ж", "з", "ѕ", "и", "ј", "к", "л",
        "љ", "м", "н", "њ", "о", "п", "р", "с", "т", "ќ", "у", "ф", "х", "ц",
        "ч", "џ", "ш"
    )

    private val UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ґ", "Є", "І", "Ї", "ґ", "є", "і", "ї"
    )

    private val SERBIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ђ", "Ј", "Љ", "Њ", "Ћ", "Џ", "ђ", "ј", "љ", "њ", "ћ", "џ"
    )

    private val BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "І", "Ў", "і", "ў"
    )

    private val KYRGYZ_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ң", "Ө", "Ү", "ң", "ө", "ү"
    )

    private val MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ѓ", "Ѕ", "Ќ", "ѓ", "ѕ", "ќ"
    )

    // Lazy initialized Tokenizer
    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    private val HEX_ENTITY_REGEX = "&#x([0-9a-fA-F]+);".toRegex()
    private val DEC_ENTITY_REGEX = "&#(\\d+);".toRegex()

    private fun decodeHtmlEntities(text: String): String =
        text
            .replace(HEX_ENTITY_REGEX) { match ->
                match.groupValues[1].toIntOrNull(16)
                    ?.takeIf { it in 0..0x10FFFF }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
            .replace(DEC_ENTITY_REGEX) { match ->
                match.groupValues[1].toIntOrNull()
                    ?.takeIf { it in 0..0x10FFFF }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        // Unescape JSON string if needed
        val unescapedLyrics = lyrics
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")

        // Decode HTML entities (e.g. &#x27; -> ', &amp; -> &)
        val decodedLyrics = decodeHtmlEntities(unescapedLyrics)
        
        val lines = decodedLyrics.lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("[offset:") }
        
        // Check if this is rich sync format (contains <MM:SS.mm> patterns)
        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) && 
            RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }
        
        return if (isRichSync) {
            parseRichSyncLyrics(lines)
        } else {
            parseStandardLyrics(lines)
        }
    }
    
    /**
     * Parse rich sync lyrics format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
     * This format provides word-by-word timing for karaoke-style highlighting
     */
    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        
        lines.forEachIndexed { index, line ->
            val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(line.trim())
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L
                
                // Convert to milliseconds
                val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millisPart
                
                var content = matchResult.groupValues[4].trimStart()
                
                // Parse agent marker {agent:v1}
                val agentMatch = AGENT_REGEX.find(content)
                val agent = agentMatch?.groupValues?.get(1)
                if (agentMatch != null) {
                    content = content.replaceFirst(AGENT_REGEX, "")
                }
                
                // Parse background marker {bg}
                val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
                if (isBackground) {
                    content = content.replaceFirst(BACKGROUND_REGEX, "")
                }
                
                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                
                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()
                
                if (plainText.isNotBlank()) {
                    result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = agent, isBackground = isBackground))
                }
            }
        }
        
        return result.sorted()
    }
    
    /**
     * Parse word timestamps from rich sync content
     * Format: <MM:SS.mm> word <MM:SS.mm> word ...
     */
    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<WordTimestamp>? {
        val wordMatches = RICH_SYNC_WORD_REGEX.findAll(content).toList()
        
        if (wordMatches.isEmpty()) return null
        
        val wordTimings = mutableListOf<WordTimestamp>()
        
        wordMatches.forEachIndexed { index, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].toLongOrNull() ?: 0L
            
            // Convert to seconds (Double)
            val fractionPart = if (match.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            val startTimeSeconds = minutes * 60.0 + seconds + fractionPart
            
            val wordText = match.groupValues[4].trim()
            
            // Calculate end time: use next word's start time, or estimate from next line
            val endTimeSeconds = if (index < wordMatches.size - 1) {
                val nextMatch = wordMatches[index + 1]
                val nextMinutes = nextMatch.groupValues[1].toLongOrNull() ?: 0L
                val nextSeconds = nextMatch.groupValues[2].toLongOrNull() ?: 0L
                val nextFraction = nextMatch.groupValues[3].toLongOrNull() ?: 0L
                val nextFractionPart = if (nextMatch.groupValues[3].length == 3) nextFraction / 1000.0 else nextFraction / 100.0
                nextMinutes * 60.0 + nextSeconds + nextFractionPart
            } else {
                // For last word, try to get next line's start time or add a default duration
                val nextLineTime = getNextLineStartTime(currentIndex, allLines)
                nextLineTime ?: (startTimeSeconds + 0.5) // Default 500ms duration for last word
            }
            
            if (wordText.isNotBlank()) {
                wordTimings.add(WordTimestamp(wordText, startTimeSeconds, endTimeSeconds))
            }
        }
        
        return if (wordTimings.isNotEmpty()) wordTimings else null
    }
    
    /**
     * Get the start time of the next line for calculating the last word's end time
     */
    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Double? {
        if (currentIndex + 1 >= allLines.size) return null
        
        val nextLine = allLines[currentIndex + 1].trim()
        val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(nextLine) ?: return null
        
        val minutes = matchResult.groupValues[1].toLongOrNull() ?: return null
        val seconds = matchResult.groupValues[2].toLongOrNull() ?: return null
        val fraction = matchResult.groupValues[3].toLongOrNull() ?: 0L
        
        val fractionPart = if (matchResult.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
        return minutes * 60.0 + seconds + fractionPart
    }
    
    /**
     * Parse standard synced lyrics format: [MM:SS.mm] text
     */
    private fun parseStandardLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.trim().startsWith("<") || !line.trim().endsWith(">")) {
                val entries = parseLine(line, null)
                if (entries != null) {
                    val wordTimestamps = if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (nextLine.trim().startsWith("<") && nextLine.trim().endsWith(">")) {
                            parseWordTimestamps(nextLine.trim().removeSurrounding("<", ">"))
                        } else null
                    } else null
                    
                    if (wordTimestamps != null) {
                        result.addAll(entries.map { entry ->
                            LyricsEntry(entry.time, entry.text, wordTimestamps, agent = entry.agent, isBackground = entry.isBackground)
                        })
                    } else {
                        result.addAll(entries)
                    }
                }
            }
            i++
        }
        return result.sorted()
    }
    
    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size == 3) {
                    WordTimestamp(
                        text = parts[0],
                        startTime = parts[1].toDouble(),
                        endTime = parts[2].toDouble()
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLine(line: String, words: List<WordTimestamp>? = null): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        var text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)
        
        // Parse agent marker {agent:v1}
        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) {
            text = text.replaceFirst(AGENT_REGEX, "")
        }
        
        // Parse background marker {bg}
        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) {
            text = text.replaceFirst(BACKGROUND_REGEX, "")
        }

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text, words, agent = agent, isBackground = isBackground)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + 300L) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    fun findActiveLineIndices(
        lines: List<LyricsEntry>,
        position: Long,
    ): Set<Int> {
        val active = mutableSetOf<Int>()
        val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

        for (index in lines.indices) {
            val line = lines[index]
            if (line.time > position) break // Past current position, stop early

            // Determine this line's end time
            val lineEndMs: Long = if (!line.words.isNullOrEmpty()) {
                // Use last word's endTime converted to ms
                (line.words.last().endTime * 1000).toLong()
            } else {
                // Fallback: next line's start time
                if (index + 1 < lines.size) lines[index + 1].time else Long.MAX_VALUE
            }

            if (position <= lineEndMs) {
                active.add(index)
            }
        }

        if (!hasWordTimings && active.size > 1) {
            val mainActive = active.filter { !lines[it].isBackground }
            if (mainActive.size > 1) {
                val maxTime = mainActive.maxOf { lines[it].time }
                active.removeAll { it in mainActive && lines[it].time < maxTime }
            }
        }

        return active
    }

    fun katakanaToRomaji(katakana: String?): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length)
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        val tokens = kuromojiTokenizer.tokenize(text)
        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length)
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed && (katakana[i] == 'ッ' || katakana[i] == 'っ')) {
                val nextCharToDouble = katakana.getOrNull(i + 1) ?: nextKatakana?.getOrNull(0)
                if (nextCharToDouble != null) {
                    val nextCharRomaji = KANA_ROMAJI_MAP[nextCharToDouble.toString()]?.getOrNull(0)?.toString()
                        ?: nextCharToDouble.toString()
                    romajiBuilder.append(nextCharRomaji.lowercase().trim())
                }
                i += 1
                consumed = true
            }

            if (!consumed) {
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]
            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)
                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    suspend fun romanizeChinese(text: String): String = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext ""
        val builder = StringBuilder(text.length * 2)
        for (ch in text) {
            if (ch in '\u4E00'..'\u9FFF') {
                val py = Pinyin.toPinyin(ch).lowercase(Locale.getDefault())
                builder.append(py).append(' ')
            } else {
                builder.append(ch)
            }
        }
        // Remove whitespaces before ASCII and CJK punctuations
        builder.toString()
            .replace(Regex("\\s+([,.!?;:])"), "$1")
            .replace(Regex("\\s+([，。！？；：、（）《》「」『』【】])"), "$1")
            .trim()
    }

    suspend fun romanizeCyrillic(text: String): String? = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext null

        val cyrillicChars = text.filter { it in '\u0400'..'\u04FF' }

        if (cyrillicChars.isEmpty() ||
            (cyrillicChars.length == 1 && (cyrillicChars[0] == 'Я' || cyrillicChars[0] == 'я'))) {
            return@withContext null
        }

        when {
            isRussian(text) -> romanizeRussianInternal(text)
            isUkrainian(text) -> romanizeUkrainianInternal(text)
            isSerbian(text) -> romanizeSerbianInternal(text)
            isBulgarian(text) -> romanizeBulgarianInternal(text)
            isBelarusian(text) -> romanizeBelarusianInternal(text)
            isKyrgyz(text) -> romanizeKyrgyzInternal(text)
            isMacedonian(text) -> romanizeMacedonianInternal(text)
            else -> null
        }
    }

    private fun romanizeRussianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    var consumed = false
                    // Check for 3-character sequences
                    if (charIndex + 2 < word.length) {
                        val threeCharCandidate = word.substring(charIndex, charIndex + 3)
                        if (RUSSIAN_ROMAJI_MAP.containsKey(threeCharCandidate)) {
                            romajiBuilder.append(RUSSIAN_ROMAJI_MAP[threeCharCandidate])
                            charIndex += 3
                            consumed = true
                        }
                    }

                    if (!consumed) {
                        val charStr = word[charIndex].toString()
                        // Special case for 'е' or 'Е' at the start of a word
                        if ((charStr == "е" || charStr == "Е") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                            romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                        } else {
                            // Apply general Cyrillic mapping
                            val romanizedChar = GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                            romajiBuilder.append(romanizedChar)
                        }
                        charIndex += 1
                    }
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeUkrainianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    var processed = false

                    if (charIndex > 0 && word[charIndex - 1].isLetter() && !isCyrillicVowel(word[charIndex - 1])) {
                        // Check if the current character is Ю or Я and is preceded by a consonant
                        when (charStr) {
                            "Ю" -> {
                                romajiBuilder.append("Iu")
                                processed = true
                            }
                            "ю" -> {
                                romajiBuilder.append("iu")
                                processed = true
                            }
                            "Я" -> {
                                romajiBuilder.append("Ia")
                                processed = true
                            }
                            "я" -> {
                                romajiBuilder.append("ia")
                                processed = true
                            }
                        }
                    }

                    if (!processed) {
                        romajiBuilder.append(UKRAINIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    }
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeSerbianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = SERBIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBulgarianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = BULGARIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBelarusianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEach { word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    // Special case for 'е' or 'Е' at the start of a word
                    if ((charStr == "е" || charStr == "Е") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                        romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                    } else {
                        // General mapping
                        val romanizedChar = BELARUSIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                        romajiBuilder.append(romanizedChar)
                    }
                    charIndex += 1
                }
            }
        }

        return romajiBuilder.toString()
    }

    private fun romanizeKyrgyzInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = KYRGYZ_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeMacedonianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = MACEDONIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    fun isRussian(text: String): Boolean {
        return text.any { char ->
            RUSSIAN_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            val charStr = char.toString()
            RUSSIAN_CYRILLIC_LETTERS.contains(charStr) || !charStr.matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isUkrainian(text: String): Boolean {
        return text.any { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isSerbian(text: String): Boolean {
        return text.any { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBulgarian(text: String): Boolean {
        return text.any { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBelarusian(text: String): Boolean {
        return text.any { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isKyrgyz(text: String): Boolean {
        return text.any { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isMacedonian(text: String): Boolean {
        return text.any { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') || // Hiragana
                    (char in '\u30A0'..'\u30FF') || // Katakana
                    (char in '\u4E00'..'\u9FFF') // CJK Unified Ideographs
        }
    }

    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3') // Hangul Syllables
        }
    }

    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false
        val cjkCharCount = text.count { char -> char in '\u4E00'..'\u9FFF' }
        val hiraganaKatakanaCount = text.count { char -> (char in '\u3040'..'\u309F') || (char in '\u30A0'..'\u30FF') }
        return cjkCharCount > 0 && (hiraganaKatakanaCount.toDouble() / text.length.toDouble()) < 0.1
    }

    fun isHindi(text: String): Boolean {
        return text.any { char ->
            char in '\u0900'..'\u097F'
        }
    }

    suspend fun romanizeHindi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            var consumed = false
            // Check for 2-character sequences (e.g. char + nukta)
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = DEVANAGARI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val charStr = text[i].toString()
                sb.append(DEVANAGARI_ROMAJI_MAP[charStr] ?: charStr)
                i += 1
            }
        }
        sb.toString()
    }

    fun isPunjabi(text: String): Boolean {
        return text.any { char ->
            char in '\u0A00'..'\u0A7F'
        }
    }

    suspend fun romanizePunjabi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            var consumed = false

            // Check for Adhak (Gemination)
            if (char == '\u0A71') {
                 // Double next consonant if possible
                 if (i + 1 < text.length) {
                     val nextCharStr = text[i + 1].toString()
                     val nextMapped = GURMUKHI_ROMAJI_MAP[nextCharStr]
                     if (!nextMapped.isNullOrEmpty()) {
                         sb.append(nextMapped[0])
                     }
                 }
                 i++
                 continue
            }

            // Check for 2-character sequences (e.g. char + nukta)
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = GURMUKHI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val str = char.toString()
                sb.append(GURMUKHI_ROMAJI_MAP[str] ?: str)
                i++
            }
        }
        sb.toString()
    }

    private fun isCyrillicVowel(char: Char): Boolean {
        return "АЕЄИІЇОУЮЯаеєиіїоуюяЁЫЭёыэ".contains(char)
    }
}
