package org.unicode.cldr.tool

import com.google.common.collect.ImmutableSet
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.util.ULocale
import java.io.IOException
import java.util.TreeSet
import java.util.function.Predicate
import org.unicode.cldr.util.CLDRConfig
import org.unicode.cldr.util.CLDRPaths
import org.unicode.cldr.util.CalculatedCoverageLevels
import org.unicode.cldr.util.Factory
import org.unicode.cldr.util.Level
import org.unicode.cldr.util.TempPrintWriter
import kotlin.math.pow

object GenerateDecimalFormatTestDataKt {

    private const val OUTPUT_SUBDIR = "decimal"

    private val CLDR_CONFIG = CLDRConfig.getInstance()
    private val CLDR_FACTORY = CLDR_CONFIG.cldrFactory

    // Minimal lists
    private val MINIMAL_LOCALES = setOf("en_US", "fr", "de_CH", "ar", "hi", "bn", "zh", "ru", "ja")

    private val MINIMAL_NUMBERS = setOf(
        0.0, 1.0, 1.2, 12.0, 123.0, 1234.56, 1234567.0, -1234.56, 0.000123, -0.0
    )

    enum class Style(val label: String) {
        DECIMAL("decimal"),
        PERCENT("percent"),
        SCIENTIFIC("scientific"),
        COMPACT_SHORT("compact-short"),
        COMPACT_LONG("compact-long")
    }

    // Complete lists
    private fun getCompleteLocales(): Set<String> {
        val results = TreeSet<String>()
        for (locale in CLDR_FACTORY.availableLanguages) {
            val coverageLevel = CalculatedCoverageLevels.getInstance().getEffectiveCoverageLevel(locale)
            if (coverageLevel != null && coverageLevel.isAtLeast(Level.MODERN)) {
                results.add(locale)
            }
        }
        return results
    }

    private fun getCompleteNumbers(): Set<Double> {
        val results = TreeSet<Double>()
        // Add powers of 10
        for (i in -6..12) {
            results.add(10.0.pow(i))
            results.add(10.0.pow(i) * 1.5)
            results.add(10.0.pow(i) * 5)
        }
        // Add some standard test numbers
        results.addAll(MINIMAL_NUMBERS)
        results.addAll(listOf(0.5, 1.5, 2.5, 3.5, 0.125, 0.135, 999.9, 999999.9))

        // Add negatives
        val negatives = TreeSet<Double>()
        for (d in results) {
            if (d > 0) {
                negatives.add(-d)
            }
        }
        results.addAll(negatives)
        return results
    }

    class Combination(val locale: String, val style: Style, val number: Double)

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

    private fun generateAndWrite(
        locales: Iterable<String>,
        styles: Iterable<Style>,
        numbers: Iterable<Double>,
        filename: String,
        filter: Predicate<Combination>
    ) {
        TempPrintWriter.openUTF8Writer(CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, filename).use { pw ->
            pw.println("locale\tstyle\tinput\texpected")

            for (localeStr in locales) {
                val locale = ULocale(localeStr)
                for (style in styles) {
                    for (number in numbers) {
                        val combo = Combination(localeStr, style, number)
                        if (!filter.test(combo)) {
                            continue
                        }

                        val expected = format(locale, style, number)
                        pw.println("$localeStr\t${style.label}\t$number\t$expected")
                    }
                }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val completeLocales = getCompleteLocales()
        val completeNumbers = getCompleteNumbers()
        val allStyles = Style.values().toList()

        // Filter to skip redundant entries covered in decimal.tsv
        val skipRedundant = Predicate<Combination> { combo ->
            !(MINIMAL_LOCALES.contains(combo.locale) && MINIMAL_NUMBERS.contains(combo.number))
        }

        // Filter for 5% random subset of missing tests
        val filterRandom5Percent = Predicate<Combination> { combo ->
            var nonMinimalCount = 0
            if (!MINIMAL_LOCALES.contains(combo.locale)) nonMinimalCount++
            if (!MINIMAL_NUMBERS.contains(combo.number)) nonMinimalCount++

            if (nonMinimalCount <= 1) {
                false
            } else {
                val key = "${combo.locale}\t${combo.style.label}\t${combo.number}"
                val hash = key.hashCode()
                Math.abs(hash % 20) == 0
            }
        }

        // 0. Minimal product (baseline)
        generateAndWrite(
            MINIMAL_LOCALES,
            allStyles,
            MINIMAL_NUMBERS,
            "decimal_kt.tsv"
        ) { true }

        // 1. Complete Locales, Minimal Numbers
        generateAndWrite(
            completeLocales,
            allStyles,
            MINIMAL_NUMBERS,
            "decimal_all_locales_kt.tsv",
            skipRedundant
        )

        // 2. Complete Numbers, Minimal Locales
        generateAndWrite(
            MINIMAL_LOCALES,
            allStyles,
            completeNumbers,
            "decimal_all_numbers_kt.tsv",
            skipRedundant
        )

        // 3. 5% random subset of missing tests
        generateAndWrite(
            completeLocales,
            allStyles,
            completeNumbers,
            "decimal_random_5percent_kt.tsv",
            filterRandom5Percent
        )

        println("Decimal format test data generation (Kotlin) completed.")
    }
}
