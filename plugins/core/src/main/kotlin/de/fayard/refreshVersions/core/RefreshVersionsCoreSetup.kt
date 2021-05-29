@file:JvmName("RefreshVersionsCoreSetup")

package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.extensions.gradle.isBuildSrc
import de.fayard.refreshVersions.core.internal.*
import de.fayard.refreshVersions.core.internal.resolveVersion
import de.fayard.refreshVersions.core.internal.setupVersionPlaceholdersResolving
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule
import org.gradle.kotlin.dsl.apply
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.GradleVersion
import java.io.File

/**
 * Boostrap refreshVersions-core **only** (excludes dependencies constants and version key rules).
 *
 * Supports both Kotlin and Groovy Gradle DSL.
 *
 * // **`settings.gradle.kts`**
 * ```kotlin
 * import de.fayard.refreshVersions.core.bootstrap
 *
 * buildscript {
 *     dependencies.classpath("de.fayard.refreshVersions:refreshVersions-core:VERSION")
 * }
 *
 * settings.bootstrapRefreshVersionsCore()
 * ```
 *
 * // **`settings.gradle`**
 * ```groovy
 * import de.fayard.refreshVersions.core.RefreshVersionsCoreSetup
 * buildscript {
 *     dependencies.classpath("de.fayard.refreshVersions:refreshVersions-core:VERSION")
 * }
 *
 * RefreshVersionsCoreSetup.bootstrap(settings)
 * ```
 */
@JvmOverloads
@JvmName("bootstrap")
fun Settings.bootstrapRefreshVersionsCore(
    artifactVersionKeyRules: List<String> = emptyList(),
    versionsPropertiesFile: File = rootDir.resolve("versions.properties")
) {
    require(settings.isBuildSrc.not()) {
        "This bootstrap is only for the root project. For buildSrc, please call " +
                "bootstrapRefreshVersionsCoreForBuildSrc() instead (Kotlin DSL)," +
                "or RefreshVersionsCoreSetup.bootstrapForBuildSrc() if you're using Groovy DSL."
    }
    RefreshVersionsConfigHolder.initialize(
        settings = settings,
        artifactVersionKeyRules = artifactVersionKeyRules,
        versionsPropertiesFile = versionsPropertiesFile
    )
    setupRefreshVersions(settings = settings)
}

/**
 * **For buildSrc only!**
 *
 * Boostrap refreshVersions-core **only** (excludes dependencies constants and version key rules).
 *
 * Supports both Kotlin and Groovy Gradle DSL.
 *
 * // **`settings.gradle.kts`**
 * ```kotlin
 * import de.fayard.refreshVersions.core.bootstrapRefreshVersionsCoreForBuildSrc
 *
 * buildscript {
 *     dependencies.classpath("de.fayard.refreshVersions:refreshVersions-core:VERSION")
 * }
 *
 * settings.bootstrapRefreshVersionsCoreForBuildSrc()
 * ```
 *
 * // **`settings.gradle`**
 * ```groovy
 * import de.fayard.refreshVersions.core.RefreshVersionsCoreSetup
 * buildscript {
 *     dependencies.classpath("de.fayard.refreshVersions:refreshVersions-core:VERSION")
 * }
 *
 * RefreshVersionsCoreSetup.bootstrapForBuildSrc(settings)
 * ```
 */
@JvmName("bootstrapForBuildSrc")
fun Settings.bootstrapRefreshVersionsCoreForBuildSrc() {
    RefreshVersionsConfigHolder.initializeBuildSrc(this)
    setupRefreshVersions(settings = settings)
}

/**
 * Sets up a resolution strategy for the plugins that does the following:
 * For each plugin, tries to find the corresponding version declared in the versions properties file, and uses it.
 *
 * The expected property key is the id of the plugin, prefixed with `plugin.` (e.g. `plugin.io.fabric`), excepted for
 * the Kotlin and Android gradle plugins where `version.kotlin` and `plugin.android` are used.
 *
 * Note that the properties can alias to another one from `version.properties` (hard limit is 5 property redirects),
 * you just need to put the key of the property you want to use (e.g. `plugin.some-plugin=plugin.android`).
 *
 * This function also sets up the module for the Android and Fabric (Crashlytics) Gradle plugins, so you can avoid the
 * buildscript classpath configuration boilerplate.
 */
private fun setupRefreshVersions(settings: Settings) {
    val supportedGradleVersion = "6.3" // 6.2 fail with this error: https://gradle.com/s/shp7hbtd3i3ii
    if (GradleVersion.current() < GradleVersion.version(supportedGradleVersion)) {
        throw UnsupportedVersionException("""
            The plugin "de.fayard.refreshVersions" only works with Gradle $supportedGradleVersion and above.
            See https://jmfayard.github.io/refreshVersions/setup/#update-gradle-if-needed
            """.trimIndent())
    }


    val versionsMap = RefreshVersionsConfigHolder.readVersionsMap()
    @Suppress("unchecked_cast")
    setupPluginsVersionsResolution(
        settings = settings,
        properties = versionsMap
    )

    settings.gradle.setupVersionPlaceholdersResolving(versionsMap = versionsMap)

    settings.gradle.rootProject {
        apply<RefreshVersionsCorePlugin>()
    }
}

private fun setupPluginsVersionsResolution(
    settings: Settings,
    properties: Map<String, String>
) {
    settings.pluginManagement {
        resolutionStrategy.eachPlugin {
            val pluginId = requested.id.id
            if (pluginId == "de.fayard.refreshVersions") {
                return@eachPlugin // Already in the buildscript with a defined version that will be used.
            }
            val pluginNamespace = requested.id.namespace ?: ""
            val versionKey = when {
                pluginNamespace.startsWith("org.jetbrains.kotlin") -> "version.kotlin"
                pluginNamespace.startsWith("com.android") -> "plugin.android"
                else -> "plugin.$pluginId"
            }
            val version = resolveVersion(properties, versionKey)
            if (version == null) {
                val pluginVersion = requested.version ?: return@eachPlugin
                UsedPluginsHolder.noteUnusedPlugin(pluginIdToDependency(pluginId, pluginVersion))
                return@eachPlugin
            }
            when {
                pluginNamespace.startsWith("com.android") -> {
                    val dependencyNotation = "com.android.tools.build:gradle:$version"
                    UsedPluginsHolder.noteUsedPluginDependency(
                        dependencyNotation = dependencyNotation,
                        repositories = repositories
                    )
                    useModule(dependencyNotation)
                }
                else -> {
                    UsedPluginsHolder.noteUsedPluginDependency(
                        dependencyNotation = "$pluginId:$pluginId.gradle.plugin:$version",
                        repositories = repositories
                    )
                    useVersion(version)
                }
            }
        }
    }
}

fun pluginDependencyNotationToVersionKey(dependencyNotation: String): String? =
    when {
        dependencyNotation.startsWith("com.android") -> "plugin.android"
        dependencyNotation.startsWith("org.jetbrains.kotlin") -> "version.kotlin"
        dependencyNotation.endsWith(".gradle.plugin") -> "plugin." + dependencyNotation.removeSuffix(".gradle.plugin")
        else -> null
    }

fun pluginIdToDependency(pluginId: String, version: String): ExternalDependency =
    DefaultClientModule(pluginId, "$pluginId.gradle.plugin", version)
