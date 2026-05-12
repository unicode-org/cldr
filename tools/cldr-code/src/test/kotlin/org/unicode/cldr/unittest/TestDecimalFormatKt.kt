package org.unicode.cldr.unittest

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.util.ULocale
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.unicode.cldr.util.CLDRPaths

class TestDecimalFormatKt : TestFmwkPlus() {

    fun TestDecimaltsv() {
        try {
            runTsvTest("decimal_kt.tsv")
        } catch (e: IOException) {
            errln("IOException: ${e.message}")
        }
    }

    fun TestDecimalAllLocales() {
        try {
            runTsvTest("decimal_all_locales_kt.tsv")
        } catch (e: IOException) {
            errln("IOException: ${e.message}")
        }
    }

    fun TestDecimalAllNumbers() {
        try {
            runTsvTest("decimal_all_numbers_kt.tsv")
        } catch (e: IOException) {
            errln("IOException: ${e.message}")
        }
    }

    fun TestDecimalRandom5Percent() {
        try {
            runTsvTest("decimal_random_5percent_kt.tsv")
        } catch (e: IOException) {
            errln("IOException: ${e.message}")
        }
    }

    private fun runTsvTest(filename: String) {
        val filePath = Path.of(CLDRPaths.TEST_DATA + "decimal", filename)
        if (!Files.exists(filePath)) {
            errln("Test data file not found: $filePath")
            return
        }

        Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { reader ->
            val header = reader.readLine()
            if (header == null) {
                errln("Empty test data file: $filePath")
                return
            }

            var lineNum = 1
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                lineNum++
                val currentLine = line ?: continue
                if (currentLine.isBlank() || currentLine.startsWith("#")) {
                    continue
                }

                val parts = currentLine.split("\t")
                if (parts.size < 4) {
                    errln("$filename:$lineNum - Invalid line: $currentLine")
                    continue
                }

                val localeStr = parts[0]
                val styleStr = parts[1]
                val input = parts[2].toDouble()
                val expected = parts[3]

                val locale = ULocale(localeStr)
                val style = Style.fromLabel(styleStr)
                if (style == null) {
                    errln("$filename:$lineNum - Unknown style: $styleStr")
                    continue
                }

                val actual = format(locale, style, input)
                assertEquals("$filename:$lineNum - Failure for $localeStr ($styleStr) with input $input", expected, actual)
            }
        }
    }

    enum class Style(val label: String) {
        DECIMAL("decimal"),
        PERCENT("percent"),
        SCIENTIFIC("scientific"),
        COMPACT_SHORT("compact-short"),
        COMPACT_LONG("compact-long");

        companion object {
            fun fromLabel(label: String): Style? {
                return values().find { it.label == label }
            }
        }
    }

    private fun format(locale: ULocale, style: Style, number: Double): String {
        val lnf = when (style) {
            Style.DECIMAL -> NumberFormatter.withLocale(locale)
            Style.PERCENT -> NumberFormatter.withLocale(locale)
                .unit(com.ibm.icu.util.MeasureUnit.PERCENT)
                .scale(com.ibm.icu.number.Scale.powerOfTen(2))
            Style.SCIENTIFIC -> NumberFormatter.withLocale(locale)
                .notation(com.ibm.icu.number.Notation.scientific())
            Style.COMPACT_SHORT -> NumberFormatter.withLocale(locale)
                .notation(com.ibm.icu.number.Notation.compactShort())
            Style.COMPACT_LONG -> NumberFormatter.withLocale(locale)
                .notation(com.ibm.icu.number.Notation.compactLong())
        }
        return lnf.format(number).toString()
    }
}
