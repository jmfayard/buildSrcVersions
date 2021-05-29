package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.internal.*
import de.fayard.refreshVersions.core.internal.versions.writeMissingEntriesInVersionProperties
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage")
open class MissingEntriesTask : DefaultTask() {


    @TaskAction
    fun initVersionsProperties() {
        require(project == project.rootProject) { "Expected a rootProject but got $project" }
        OutputFile.checkWhichFilesExist(project.rootDir)
        val configurationsWithHardcodedDependencies = project.findHardcodedDependencies()

        val versionsMap = RefreshVersionsConfigHolder.readVersionsMap()
        val versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
        val newEntries: Map<String, ExternalDependency> = findMissingEntries(
            configurations = configurationsWithHardcodedDependencies,
            versionsMap = versionsMap,
            versionKeyReader = versionKeyReader
        )
        val plugins = UsedPluginsHolder.unusedPlugins
            .distinctBy { d -> pluginDependencyNotationToVersionKey(d.name).also { println(it) } }
            .associateBy { d -> pluginDependencyNotationToVersionKey(d.name) }
            .filterKeys { key -> key != null && key !in versionsMap }
            as Map<String, ExternalDependency>


        writeMissingEntriesInVersionProperties(plugins + newEntries, isUsed = false)
        OutputFile.VERSIONS_PROPERTIES.logFileWasModified()
        Thread.sleep(1000)
    }

}


@InternalRefreshVersionsApi
fun Configuration.countDependenciesWithHardcodedVersions(
    versionsMap: Map<String, String>,
    versionKeyReader: ArtifactVersionKeyReader
): Int = dependencies.count { dependency ->
    dependency is ExternalDependency && dependency.hasHardcodedVersion(versionsMap, versionKeyReader)
}

@InternalRefreshVersionsApi
fun Project.countDependenciesWithHardcodedVersions(versionsMap: Map<String, String>): Int {
    val versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
    return configurations.sumBy { configuration ->
        if (configuration.shouldBeIgnored()) 0 else {
            configuration.countDependenciesWithHardcodedVersions(versionsMap, versionKeyReader)
        }
    }
}

internal fun Project.findHardcodedDependencies(): List<Configuration> {
    val versionsMap = RefreshVersionsConfigHolder.readVersionsMap()
    val projectsWithHardcodedDependenciesVersions: List<Project> = rootProject.allprojects.filter {
        it.countDependenciesWithHardcodedVersions(versionsMap) > 0
    }

    return projectsWithHardcodedDependenciesVersions.flatMap { project ->
        project.configurations.filterNot { configuration ->
            configuration.shouldBeIgnored() || 0 == configuration.countDependenciesWithHardcodedVersions(
                versionsMap = versionsMap,
                versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
            )
        }
    }
}

private val ignoredConfigurationNames = listOf(
    "kotlinCompilerPluginClasspath",
    "kotlinKaptWorkerDependencies",
    "lintClassPath"
)

@InternalRefreshVersionsApi
fun Configuration.shouldBeIgnored(): Boolean {
    return name.startsWith(prefix = "_internal") // Real-life example: _internal_aapt2_binary (introduced by AGP)
        || name in ignoredConfigurationNames || name.startsWith('-')
    //TODO: If unwanted configurations still get through, we can filter to known ones here, like
    // implementation, api, compileOnly, runtimeOnly, kapt, plus test, MPP and MPP test variants.
}

internal fun findMissingEntries(
    configurations: List<Configuration>,
    versionsMap: Map<String, String>,
    versionKeyReader: ArtifactVersionKeyReader
): Map<String, ExternalDependency> {

    val dependencyMap = configurations.flatMap { configuration ->
        configuration.dependencies
            .filterIsInstance<ExternalDependency>()
            .filter { it.hasHardcodedVersion(versionsMap, versionKeyReader) && it.version != null }
            .map { dependency: ExternalDependency ->
                val versionKey = getVersionPropertyName(dependency.module, versionKeyReader)
                versionKey to dependency
            }
    }
    val newEntries = dependencyMap
        .groupBy({ it.first }, { it.second })
        .filter { entry -> entry.key !in versionsMap }
        .mapValues { entry -> entry.value.maxBy { it.version!! }!! }

    return newEntries
}

