package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.extensions.gradle.isBuildSrc
import de.fayard.refreshVersions.core.extensions.gradle.isRootProject
import de.fayard.refreshVersions.core.extensions.text.indexOfPrevious
import de.fayard.refreshVersions.core.internal.codeparsing.gradle.extractGradleScriptSections
import de.fayard.refreshVersions.core.internal.codeparsing.gradle.findPluginBlocksRanges
import de.fayard.refreshVersions.core.internal.versions.VersionsPropertiesModel.Companion.availableComment
import org.gradle.api.Project
import java.io.File

internal object SettingsPluginsUpdater {

    fun updateGradleSettingsWithAvailablePluginsUpdates(
        rootProject: Project,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>,
        buildSrcSettingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {
        require(rootProject.isRootProject)
        require(rootProject.isBuildSrc.not())

        val rootProjectSettingsFile = rootProject.file("settings.gradle.kts").let { kotlinDslSettings ->
            if (kotlinDslSettings.exists()) kotlinDslSettings else {
                rootProject.file("settings.gradle").also {
                    check(it.exists())
                }
            }
        }
        val buildSrcSettingsFile = rootProject.file("buildSrc/settings.gradle.kts").let { kotlinDslSettings ->
            if (kotlinDslSettings.exists()) kotlinDslSettings else {
                rootProject.file("buildSrc/settings.gradle").takeIf {
                    it.exists()
                }
            }
        }
        updateGradleSettingsWithAvailablePluginsUpdates(
            settingsFile = rootProjectSettingsFile,
            settingsPluginsUpdates = settingsPluginsUpdates
        )
        buildSrcSettingsFile?.let {
            updateGradleSettingsWithAvailablePluginsUpdates(
                settingsFile = it,
                settingsPluginsUpdates = buildSrcSettingsPluginsUpdates
            )
        }
    }

    private fun updateGradleSettingsWithAvailablePluginsUpdates(
        settingsFile: File,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {
        val oldContent = settingsFile.readText()
        val isKotlinDsl = settingsFile.name.endsWith(".kts")
        val newContent = updatedGradleSettingsFileContentWithAvailablePluginsUpdates(
            fileContent = oldContent,
            isKotlinDsl = isKotlinDsl,
            settingsPluginsUpdates = settingsPluginsUpdates
        )
        if (oldContent != newContent) {
            settingsFile.writeText(newContent)
            (if (isKotlinDsl) OutputFile.SETTINGS_GRADLE_KTS else OutputFile.SETTINGS_GRADLE).logFileWasModified()
        }
    }

    internal fun updatedGradleSettingsFileContentWithAvailablePluginsUpdates(
        fileContent: String,
        isKotlinDsl: Boolean,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ): String {
        val fileContentWithoutOurComments = StringBuilder(fileContent.length).apply {
            append(fileContent)
            removeCommentsAddedByUs()
        }

        val ranges = fileContentWithoutOurComments.extractGradleScriptSections(isKotlinDsl = isKotlinDsl)

        fileContentWithoutOurComments.findPluginBlocksRanges(
            ranges = ranges
        ).asReversed().forEach { pluginBlockRange ->
            fileContentWithoutOurComments.insertAvailableVersionComments(
                pluginBlockRange = pluginBlockRange,
                isKotlinDsl = isKotlinDsl,
                settingsPluginsUpdates = settingsPluginsUpdates
            )
        }
        return fileContentWithoutOurComments.toString()
    }

    private fun StringBuilder.insertAvailableVersionComments(
        pluginBlockRange: TaggedRange<*>,
        isKotlinDsl: Boolean,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {

        val pluginIdRegex = if (isKotlinDsl) PluginIdRegexes.kotlinDsl else PluginIdRegexes.groovyDsl

        val pluginsUpdatesMap = settingsPluginsUpdates.associateBy { it.pluginId }
        val text = this

        pluginIdRegex.findAll(
            input = this.substring(0, pluginBlockRange.endIndex),
            startIndex = pluginBlockRange.startIndex
        ).toList().asReversed().forEach { result ->
            val idGroup = result.groups[1]!!
            val currentVersionGroup = result.groups[2]!!
            val versionCallClosingParenthesisGroup = result.groups[3]!!
            val pluginId = idGroup.value
            val pluginWithVersionCandidates = pluginsUpdatesMap[pluginId] ?: return@forEach

            if (pluginWithVersionCandidates.versionsCandidates.isEmpty()) return@forEach

            val currentVersionStartIndex = currentVersionGroup.range.first
            val currentVersionEndIndex = currentVersionGroup.range.last

            val indexOfPreviousNewLine = indexOfPrevious(
                char = '\n',
                startIndex = currentVersionStartIndex
            )
            val indexOfNextNewLine = indexOf(
                char = '\n',
                startIndex = currentVersionEndIndex
            )

            val shouldAddClosingParenthesis = versionCallClosingParenthesisGroup.let {
                it.value.isNotEmpty() && it.range.first < indexOfNextNewLine
            }

            val offsetOfCurrentVersionFromLineStart = currentVersionStartIndex - indexOfPreviousNewLine

            // The number 8 below is the length of "//// ", plus ':',
            // plus the 2 string literal delimiters.

            val availableVersionsComments = buildString {
                val padding = offsetOfCurrentVersionFromLineStart - 8
                pluginWithVersionCandidates.versionsCandidates.forEach { versionCandidate ->
                    appendln()
                    append("//// ")
                    append(availableComment.padStart((padding).coerceAtLeast(0)))
                    append(':')
                    val stringLiteralDelimiter = text[currentVersionEndIndex + 1]
                    append(stringLiteralDelimiter)
                    append(versionCandidate.value)
                    append(stringLiteralDelimiter)
                    if (shouldAddClosingParenthesis) {
                        append(versionCallClosingParenthesisGroup.value)
                    }
                }
            }
            insert(indexOfNextNewLine, availableVersionsComments)

            val currentVersionPadding = (availableComment.length + 8 - offsetOfCurrentVersionFromLineStart)
            if (currentVersionPadding > 0) {
                insert(currentVersionStartIndex - 1, "".padStart(currentVersionPadding))
            }
        }
    }

    /** Removes comments previously added by refreshVersions. */
    internal fun StringBuilder.removeCommentsAddedByUs() {
        val startOfRefreshVersionsCommentLines = "\n////"
        var startIndex = 0
        while (true) {
            val indexOfComment = indexOf(startOfRefreshVersionsCommentLines, startIndex = startIndex)
            if (indexOfComment == -1) return
            startIndex = indexOfComment
            val indexOfEndOfLine = indexOf(
                "\n",
                startIndex = indexOfComment + startOfRefreshVersionsCommentLines.length
            ).takeIf { it >= 0 }
            val endIndex = indexOfEndOfLine ?: length
            val commentContentStartIndex = indexOfComment + startOfRefreshVersionsCommentLines.length
            val commentContent = substring(commentContentStartIndex, endIndex)
            if (availableComment !in commentContent) {
                startIndex = endIndex
                continue
            }
            replace(
                /* start = */ indexOfComment,
                /* end = */ endIndex,
                /* str = */""
            )
        }
    }

    private object PluginIdRegexes {
        private const val kotlinStringLiteralPattern = """\"([0-9a-zA-Z\-\_.]*)\""""
        private const val groovyStringLiteralPattern = """[\"']([0-9a-zA-Z\-\_.]*)[\"']"""
        val kotlinDsl =
            """id\s*\(\s*$kotlinStringLiteralPattern\s*\)\s*\.?\s*version\s*\(?\s*$kotlinStringLiteralPattern\s*(\)?)""".toRegex()
        val groovyDsl =
            """id\s*\(?\s*$groovyStringLiteralPattern\s*\)?\s*\.?\s*version\s*\(?\s*$groovyStringLiteralPattern\s*(\)?)""".toRegex()
    }

}
