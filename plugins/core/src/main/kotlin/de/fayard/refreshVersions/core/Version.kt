package de.fayard.refreshVersions.core

import org.gradle.api.Incubating
import java.math.BigInteger

@Incubating
data class Version(val value: String) : Comparable<Version> {

    /**
     * Check order is important. From least stable to most stable, then unknown
     */
    val stabilityLevel: StabilityLevel by lazy(LazyThreadSafetyMode.NONE) {
        when {
            "SNAPSHOT" in value -> StabilityLevel.Snapshot
            "preview" in value -> StabilityLevel.Preview
            "dev" in value -> StabilityLevel.Development
            "alpha" in value -> StabilityLevel.Alpha
            "beta" in value -> StabilityLevel.Beta
            "eap" in value -> StabilityLevel.EarlyAccessProgram
            isMilestone() -> StabilityLevel.Milestone
            value.contains("rc", ignoreCase = true) -> StabilityLevel.ReleaseCandidate
            isStable() -> StabilityLevel.Stable
            else -> StabilityLevel.Unknown
        }
    }

    val isRange: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        value.isRange()
    }

    override fun compareTo(other: Version): Int = versionComparator.compare(this, other)

    private val comparableList: List<Comparable<*>> by lazy(LazyThreadSafetyMode.NONE) { toComparableList() }

    companion object {

        //region Version comparison logic details (private symbols only)

        private val npmRangeCharsRegex = "[<=>^~*]|\\.x".toRegex()

        private val versionComparator: Comparator<Version> = object : Comparator<Version> {

            // Usage of this comparator is likely to get discontinued in favor of distinct per-repo ordered results.
            //
            // Still this was made as an attempt to sort the versions, but it didn't follow the rules that Gradle defined,
            // because I wasn't aware of them, but here's the documentation link for version ordering, for reference:
            // https://docs.gradle.org/current/userguide/single_versions.html#version_ordering

            override fun compare(o1: Version, o2: Version): Int {
                if (o1 == o2) return 0

                val v1 = o1.comparableList
                val v2 = o2.comparableList
                val lastCommonIndex = minOf(v1.lastIndex, v2.lastIndex)
                for (i in 0..lastCommonIndex) {
                    val e1 = v1[i]
                    val e2 = v2[i]
                    when (e1) {
                        is BigInteger -> {
                            if (e2 is BigInteger) {
                                val comparison = e1.compareTo(e2)
                                if (comparison != 0) return comparison
                            } else {
                                check(e2 is StabilityLevel)
                                return +1
                            }
                        }
                        else -> {
                            check(e1 is StabilityLevel)
                            if (e2 is StabilityLevel) {
                                val comparison = reverseStabilityComparator.compare(e1, e2)
                                if (comparison != 0) return comparison
                            } else {
                                check(e2 is BigInteger)
                                return -1
                            }
                        }
                    }
                }
                return when {
                    v1.lastIndex > lastCommonIndex -> {
                        val e1 = v1[lastCommonIndex + 1]
                        if (e1 is BigInteger) {
                            +1
                        } else {
                            check(e1 is StabilityLevel)
                            -1
                        }
                    }
                    v2.lastIndex > lastCommonIndex -> {
                        val e2 = v2[lastCommonIndex + 1]
                        if (e2 is BigInteger) {
                            -1
                        } else {
                            check(e2 is StabilityLevel)
                            +1
                        }
                    }
                    else -> o1.value.compareTo(o2.value)
                }
            }
        }

        private val reverseStabilityComparator: Comparator<StabilityLevel> = compareByDescending { it }

        private val knownVersionSuffixes = listOf("-android", "-jre")
        private val knownStableKeywords = listOf("RELEASE", "FINAL", "GA")
        private val digitsOnlyBasedVersionNumberRegex = "^[0-9,.v-]+$".toRegex()

        private fun Version.isStable(): Boolean {
            val version = value
            val uppercaseVersion = version.toUpperCase()
            val hasStableKeyword = knownStableKeywords.any { it in uppercaseVersion }
            return hasStableKeyword || digitsOnlyBasedVersionNumberRegex.matches(version.withoutKnownSuffixes())
        }

        private fun Version.isMilestone(): Boolean {
            val version = value
            return when (val indexOfM = version.indexOfLast { it == 'M' }) {
                -1 -> false
                version.lastIndex -> false
                else -> version.substring(startIndex = indexOfM + 1).all { it.isDigit() }
            }
        }

        private fun String.isRange(): Boolean = when {
            // npm operators
            startsWith('^') -> true
            startsWith('~') -> true
            startsWith('*') -> true
            // yarn operators
            startsWith('>') -> true
            startsWith('<') -> true
            startsWith('=') -> true
            // yarn hyphen range
            contains(" - ") -> true
            // yarn union
            contains(" || ") -> true
            // x ranges
            contains(".x") -> true
            else -> false
        }

        private fun String.rangeComponents(): List<Version> {
            return this
                .replace(npmRangeCharsRegex, "")
                .split(" - ", " || ",  " ")
                .filter { it.isNotBlank() }
                .map { Version(it) }
        }

        private fun Version.toComparableList(): List<Comparable<*>> {
            if(value.isRange()) {
                val lowerBound: Version = value.rangeComponents().min() ?: error("no lower version bound found in range: '$value'")
                return lowerBound.toComparableList()
            }
            return value.withoutKnownStableKeywordsOrSuffixes().split(".", "-").flatMap {
                it.toBigIntegerOrNull()?.let { number -> listOf(number) }
                    ?: Version(it).stabilityLevel.let { level ->
                        val indexOfLastNonDigit = it.indexOfLast { c -> c.isDigit().not() }
                        if (indexOfLastNonDigit == -1 || indexOfLastNonDigit == it.lastIndex) listOf(level)
                        else listOf(level, it.substring(startIndex = indexOfLastNonDigit + 1).toBigInteger())
                    }
            }
        }

        private fun String.withoutKnownStableKeywordsOrSuffixes(): String {
            return withoutKnownSuffixes().withoutKnownStableKeywords()
        }

        private fun String.withoutKnownSuffixes(): String {
            var result: String = this
            for (suffix in knownVersionSuffixes) {
                result = result.removeSuffix(suffix)
            }
            return result
        }

        private fun String.withoutKnownStableKeywords(): String {
            var result: String = this
            for (suffix in knownStableKeywords) {
                result = result.replace(suffix, "")
            }
            return result
        }

        //endregion
    }
}
