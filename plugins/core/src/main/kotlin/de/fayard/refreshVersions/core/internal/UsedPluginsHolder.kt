package de.fayard.refreshVersions.core.internal

import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency

internal object UsedPluginsHolder {

    fun noteUsedPluginDependency(
        dependencyNotation: String,
        repositories: ArtifactRepositoryContainer
    ) {
        synchronized(lock) {
            usedPluginDependencies += UsedPluginDependency(
                dependencyNotation = dependencyNotation,
                repositories = repositories
            )
        }
    }

    fun read(): Sequence<Pair<Dependency, ArtifactRepositoryContainer>> {
        return usedPluginDependencies.asSequence().map {
            ConfigurationLessDependency(it.dependencyNotation) to it.repositories
        }
    }

    fun noteUnusedPlugin(dependency: ExternalDependency) {
        synchronized(lock) {
            unusedPlugins.add(dependency)
        }
    }

    val unusedPlugins by RefreshVersionsConfigHolder.resettableDelegates.Lazy {
        mutableListOf<ExternalDependency>()
    }

    private val lock = Any()

    private data class UsedPluginDependency(
        val dependencyNotation: String,
        val repositories: ArtifactRepositoryContainer
    )

    private val usedPluginDependencies by RefreshVersionsConfigHolder.resettableDelegates.Lazy {
        mutableListOf<UsedPluginDependency>()
    }

    private class ConfigurationLessDependency(val dependencyNotation: String) : AbstractDependency() {

        override fun getGroup() = group
        override fun getName() = name
        override fun getVersion(): String? = version

        override fun contentEquals(dependency: Dependency): Boolean = throw UnsupportedOperationException()
        override fun copy(): Dependency = ConfigurationLessDependency(dependencyNotation)

        private val group = dependencyNotation.substringBefore(':').unwrappedNullableValue()
        private val name = dependencyNotation.substringAfter(':').substringBefore(':')
        private val version = dependencyNotation.substringAfterLast(':').unwrappedNullableValue()

        private fun String.unwrappedNullableValue(): String? = if (this == "null") null else this

        override fun toString() = "$group:$name:$version"
    }
}
