package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.internal.OutputFile
import de.fayard.refreshVersions.core.internal.RefreshVersionsConfigHolder
import de.fayard.refreshVersions.core.internal.SettingsPluginsUpdater
import de.fayard.refreshVersions.core.internal.legacy.LegacyBootstrapUpdater
import de.fayard.refreshVersions.core.internal.lookupVersionCandidates
import de.fayard.refreshVersions.core.internal.versions.VersionsPropertiesModel
import de.fayard.refreshVersions.core.internal.versions.writeWithNewVersions
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.gradle.util.GradleVersion

/**
 *
 * $ ./gradlew help --task refreshVersions
 * Description
 * Search for new dependencies versions and update versions.properties
 *
 * OPTIONS
 *     --enable <FEATURE_FLAG>
 *     --disable <FEATURE_FLAG>
 */
open class RefreshVersionsTask : DefaultTask() {


    @Input
    @Optional
    @Option(option = "enable", description = "Enable a feature flag")
    var enableFlag: FeatureFlag? = null
        set(value) {
            field = value
            if (value != null) FeatureFlag.userSettings.put(value, true)
        }

    @Input
    @Optional
    @Option(option = "disable", description = "Disable a feature flag")
    var disableFlag: FeatureFlag? = null
        set(value) {
            field = value
            if (value != null) FeatureFlag.userSettings.put(value, false)
        }

    @TaskAction
    fun taskActionRefreshVersions() {
        OutputFile.checkWhichFilesExist(project.rootDir)
        if (FeatureFlag.userSettings.isNotEmpty()) {
            logger.lifecycle("Feature flags: " + FeatureFlag.userSettings)
        }
        //TODO: Filter using known grouping strategies to only use the main artifact to resolve latest version, this
        // will reduce the number of repositories lookups, improving performance a little more.

        runBlocking {
            val result = lookupVersionCandidates(
                httpClient = RefreshVersionsConfigHolder.httpClient,
                project = project,
                versionMap = RefreshVersionsConfigHolder.readVersionsMap(),
                versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
            )
            VersionsPropertiesModel.writeWithNewVersions(result.dependenciesUpdates)
            SettingsPluginsUpdater.updateGradleSettingsWithAvailablePluginsUpdates(
                rootProject = project,
                settingsPluginsUpdates = result.settingsPluginsUpdates,
                buildSrcSettingsPluginsUpdates = result.buildSrcSettingsPluginsUpdates
            )
            result.selfUpdatesForLegacyBootstrap?.let {
                LegacyBootstrapUpdater.updateGradleSettingsWithUpdates(
                    rootProject = project,
                    selfUpdates = it
                )
            }

            warnAboutHardcodedVersionsIfAny(result.dependenciesWithHardcodedVersions)
            warnAboutDynamicVersionsIfAny(result.dependenciesWithDynamicVersions)
            warnAboutGradleUpdateAvailableIfAny(result.gradleUpdates)
        }
        OutputFile.VERSIONS_PROPERTIES.logFileWasModified()
    }

    private fun warnAboutGradleUpdateAvailableIfAny(gradleUpdates: List<Version>) {
        if (gradleUpdates.isEmpty()) return
        val currentGradleVersion = GradleVersion.current()
        val message = buildString {
            appendln("The Gradle version used in this project is not up to date.")
            append("To update from version ${currentGradleVersion.version}, run ")
            if (gradleUpdates.size == 1) {
                appendln("this command:")
            } else {
                appendln("one of these commands:")
            }
            gradleUpdates.forEach { version ->
                appendln("./gradlew wrapper --gradle-version ${version.value}")
            }
            appendln()
            appendln("Be sure to read the migration guides to have a smooth upgrade process.")
            appendln("Note that you can replace with a specific intermediate version if needed.")
        }
        logger.warn(message)
    }

    private fun warnAboutDynamicVersionsIfAny(dependenciesWithDynamicVersions: List<Dependency>) {
        if (dependenciesWithDynamicVersions.isNotEmpty()) {
            //TODO: Suggest running a diagnosis task to list the dynamic versions.
            logger.error(
                """Found ${dependenciesWithDynamicVersions.count()} dynamic dependencies versions!
                    |This makes builds unreproducible, and can make you use unstable versions unknowingly.
                    |
                    |The simple fix is to replace the dynamic version by the version placeholder, i.e. the underscore (_)
                    |
                    |Another solution if that fits your needs is to specify a non dynamic strict or preferred version constraint.
                    |
                    |If you're not convinced to stop using dynamic versions, here's an article for you:
                    |https://blog.danlew.net/2015/09/09/dont-use-dynamic-versions-for-your-dependencies/
                    """.trimMargin()
            )
        }
    }

    private fun warnAboutHardcodedVersionsIfAny(dependenciesWithHardcodedVersions: List<Dependency>) {
        if (dependenciesWithHardcodedVersions.isNotEmpty()) {
            //TODO: Suggest running a diagnosis task to list the hardcoded versions.
            val warnFor = (dependenciesWithHardcodedVersions).take(3).map {
                "${it.group}:${it.name}:${it.version}"
            }
            val versionsFileName = RefreshVersionsConfigHolder.versionsPropertiesFile.name
            logger.warn(
                """Found ${dependenciesWithHardcodedVersions.count()} hardcoded dependencies versions.
                    |
                    |$warnFor...
                    |
                    |To ensure single source of truth, refreshVersions only works with version placeholders,
                    |that is the explicit way of marking the version is not there (but in the $versionsFileName file).
                    |
                    |If you intentionally want to keep hardcoded versions so a module has a different version of a
                    |dependency than the rest of the project, you can safely ignore this warning for these artifacts,
                    |but keep in mind refreshVersions will not show available updates for these.
                    |
                    |Note that a migration task is planned in a future version of refreshVersions.
                    |
                    |See https://github.com/jmfayard/refreshVersions/issues/160""".trimMargin()
            )
            //TODO: Replace issue link above with stable link to explanation in documentation.
        }
    }
}
