package de.fayard.refreshVersions.core.extensions.text

internal fun String.substringBetween(
    prefix: String,
    suffix: String
): String {

    val startIndex = indexOf(prefix).also {
        if (it == -1) throw NoSuchElementException("Didn't find the passed prefix into the given String")
    } + prefix.length

    val endIndex = indexOf(suffix, startIndex = startIndex).also {
        if (it == -1) throw NoSuchElementException("Didn't find the passed suffix into the given String")
    }

    return substring(startIndex = startIndex, endIndex = endIndex)
}

internal fun String.substringAfterLastLineStartingWith(
    linePrefix: String,
    missingDelimiterValue: String = this
): String {
    val indexOfLastLineStartingWith = lastIndexOf(linePrefix).also {
        if (it == -1) return missingDelimiterValue
    }
    val indexOfLineAfter = indexOf('\n', startIndex = indexOfLastLineStartingWith).also {
        if (it == -1) return missingDelimiterValue
    }
    return substring(startIndex = indexOfLineAfter + 1)
}

/**
 * Returns a substring up to the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
internal fun String.substringUpTo(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index + 1)
}
