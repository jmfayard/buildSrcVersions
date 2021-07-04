package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.DependencyVersionsFetcher
import de.fayard.refreshVersions.core.FeatureFlag
import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.Version
import de.fayard.refreshVersions.core.extensions.gradle.moduleId
import de.fayard.refreshVersions.core.internal.versions.VersionsPropertiesModel
import de.fayard.refreshVersions.core.internal.versions.writeWithNewEntry
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.invocation.Gradle
import kotlin.reflect.full.memberFunctions

internal const val versionPlaceholder = "_"

internal fun Gradle.setupVersionPlaceholdersResolving(versionsMap: Map<String, String>) {

    val versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
    var currentVersionsMap: Map<String, String> = versionsMap
    val refreshVersionsMap = { updatedMap: Map<String, String> ->
        currentVersionsMap = updatedMap
    }
    beforeProject {
        val project: Project = this@beforeProject

        fun replaceVersionPlaceholdersFromDependencies(
            configuration: Configuration,
            isFromBuildscript: Boolean
        ) {
            if (configuration.name in configurationNamesToIgnore) return

            configuration.replaceVersionPlaceholdersFromDependencies(
                project = project,
                isFromBuildscript = isFromBuildscript,
                versionKeyReader = versionKeyReader,
                initialVersionsMap = currentVersionsMap,
                refreshVersionsMap = refreshVersionsMap
            )
        }

        project.buildscript.configurations.configureEach {
            replaceVersionPlaceholdersFromDependencies(
                configuration = this,
                isFromBuildscript = true
            )
        }

        configurations.configureEach {
            replaceVersionPlaceholdersFromDependencies(
                configuration = this,
                isFromBuildscript = false
            )
        }
    }
}

private val configurationNamesToIgnore: List<String> = listOf(
    "embeddedKotlin",
    "kotlinCompilerPluginClasspath",
    "kotlinCompilerClasspath"
)

@InternalRefreshVersionsApi
fun getVersionPropertyName(
    moduleId: ModuleId,
    versionKeyReader: ArtifactVersionKeyReader
): String {

    val group = moduleId.group
    val name = moduleId.name

    //TODO: Pos pluginDependencyNotationToVersionKey ?
    return when(moduleId) {
        is ModuleId.Maven -> when {

            name == "gradle" && group == "com.android.tools.build" -> "plugin.android"
            moduleId.name.endsWith(".gradle.plugin") -> {
                name.substringBeforeLast(".gradle.plugin").let { pluginId ->
                    when {
                        pluginId.startsWith("org.jetbrains.kotlin.") -> "version.kotlin"
                        pluginId.startsWith("com.android") -> "plugin.android"
                        else -> "plugin.$pluginId"
                    }
                }
            }
            else -> {
                val versionKey = versionKeyReader.readVersionKey(
                    group = moduleId.group,
                    name = moduleId.name
                ) ?: "${moduleId.group}..${moduleId.name}"
                "version.$versionKey"
            }
        }
        is ModuleId.Npm -> {
            val group = moduleId.group
            val name = moduleId.name
            when (group) {
                null -> "version.npm.$name"
                else -> "version.npm.@$group/$name"
            }
        }
    }
}

internal tailrec fun resolveVersion(
    properties: Map<String, String>,
    key: String,
    redirects: Int = 0
): String? {
    if (redirects > 5) error("Up to five redirects are allowed, for readability. You should only need one.")
    val value = properties[key] ?: return null
    return if (value.isAVersionAlias()) resolveVersion(properties, value, redirects + 1) else value
}

/**
 * Expects the value of a version property (values of the map returned by [RefreshVersionsConfigHolder.readVersionsMap]).
 */
internal fun String.isAVersionAlias(): Boolean = startsWith("version.") || startsWith("plugin.")

private val lock = Any()

private fun Configuration.replaceVersionPlaceholdersFromDependencies(
    project: Project,
    isFromBuildscript: Boolean,
    versionKeyReader: ArtifactVersionKeyReader,
    initialVersionsMap: Map<String, String>,
    refreshVersionsMap: (updatedMap: Map<String, String>) -> Unit
) {

    val repositories = if (isFromBuildscript) project.buildscript.repositories else project.repositories
    var properties = initialVersionsMap

    val dependenciesToReplace = mutableListOf<Pair<Dependency, Dependency>>()

    @Suppress("UnstableApiUsage")
    withDependencies {
        for (dependency in this) {
            if (dependency.version != versionPlaceholder) continue
            val moduleId = dependency.moduleId() ?: continue
            val propertyName = getVersionPropertyName(moduleId, versionKeyReader)
            val versionFromProperties = resolveVersion(
                properties = properties,
                key = propertyName
            ) ?: synchronized(lock) {
                RefreshVersionsConfigHolder.readVersionsMap().let { updatedMap ->
                    properties = updatedMap
                    refreshVersionsMap(updatedMap)
                }
                resolveVersion(properties, propertyName)
                    ?: `Write versions candidates using latest most stable version and get it`(
                        repositories = repositories,
                        propertyName = propertyName,
                        dependency = dependency,
                        moduleId = moduleId
                    ).also {
                        RefreshVersionsConfigHolder.readVersionsMap().let { updatedMap ->
                            properties = updatedMap
                            refreshVersionsMap(updatedMap)
                        }
                    }
            }
            if (dependency is ExternalDependency) {
                dependency.version {
                    require(versionFromProperties)
                    reject(versionPlaceholder) // Remember that we're managing the version of this dependency.
                }
            } else if (moduleId is ModuleId.Npm) {
                val version = if (FeatureFlag.NPM_IMPLICIT_RANGE.isEnabled && Version(versionFromProperties).isRange.not()){
                    "^$versionFromProperties"
                } else {
                    versionFromProperties
                }
                dependenciesToReplace += dependency to npmDependencyWithVersion(dependency, version)
            }
        }
        dependenciesToReplace.forEach { (old, new) ->
            remove(old)
            add(new)
        }
    }
}

/**
 * Equivalent to: dependency.copy(version = versionFromProperties)
 */
private fun npmDependencyWithVersion(
    dependency: Dependency,
    versionFromProperties: String
): Dependency {
    val copyFun =
        dependency::class.memberFunctions.first { it.name == "copy" && it.parameters.any { p -> p.name == "version" } }
    val versionParameter = copyFun.parameters.first { it.name == "version" }
    val receiverParameter = copyFun.parameters.first { it.name == null }

    return copyFun.callBy(
        mapOf(
            receiverParameter to dependency,
            versionParameter to versionFromProperties
        )
    ) as Dependency
}

@Suppress("unused") // Used in the dependencies plugin
@InternalRefreshVersionsApi
fun Project.writeCurrentVersionInProperties(
    versionKey: String,
    currentVersion: String
) {
    VersionsPropertiesModel.writeWithNewEntry(
        propertyName = versionKey,
        versionsCandidates = listOf(Version(currentVersion))
    )
}

@Suppress("FunctionName")
private fun `Write versions candidates using latest most stable version and get it`(
    repositories: ArtifactRepositoryContainer,
    propertyName: String,
    dependency: Dependency,
    moduleId: ModuleId
): String = `Write versions candidates using latest most stable version and get it`(
    propertyName = propertyName,
    dependencyVersionsFetchers = when (moduleId) {
        is ModuleId.Maven -> repositories.filterIsInstance<MavenArtifactRepository>().mapNotNull { repo ->
            DependencyVersionsFetcher.forMaven(
                httpClient = RefreshVersionsConfigHolder.httpClient,
                dependency = dependency,
                repository = repo
            )
        }
        is ModuleId.Npm -> listOf(
            DependencyVersionsFetcher.forNpm(
                httpClient = RefreshVersionsConfigHolder.httpClient,
                npmDependency = dependency,
                npmRegistry = "https://registry.npmjs.org/"
            )
        )
    }
)

@Suppress("FunctionName")
internal fun `Write versions candidates using latest most stable version and get it`(
    propertyName: String,
    dependencyVersionsFetchers: List<DependencyVersionsFetcher>
): String = runBlocking {
    dependencyVersionsFetchers.getVersionCandidates(
        currentVersion = Version(""),
        resultMode = RefreshVersionsConfigHolder.resultMode
    ).let { versionCandidates ->
        val bestStability = versionCandidates.minBy { it.stabilityLevel }!!.stabilityLevel
        val versionToUse = versionCandidates.last { it.stabilityLevel == bestStability }
        VersionsPropertiesModel.writeWithNewEntry(
            propertyName = propertyName,
            versionsCandidates = versionCandidates.dropWhile { it != versionToUse }
        )
        versionToUse.value
    }
}
