package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.extensions.gradle.isBuildSrc
import de.fayard.refreshVersions.core.extensions.gradle.isRootProject
import de.fayard.refreshVersions.core.internal.InternalRefreshVersionsApi
import de.fayard.refreshVersions.core.internal.RefreshVersionsConfigHolder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.slf4j.Marker
import org.slf4j.helpers.BasicMarkerFactory

open class RefreshVersionsCorePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        check(project.isRootProject) { "ERROR: de.fayard.refreshVersions.core should not be applied manually" }
        if (project.isBuildSrc.not()) {
            project.tasks.register<RefreshVersionsTask>(name = "refreshVersions") {
                group = "refreshVersions"
                val versionsFileName = RefreshVersionsConfigHolder.versionsPropertiesFile.name
                description = "Search for new dependencies versions and update $versionsFileName"
            }
        }
        cleanFilesFromPreviousVersions(project)
    }

    private fun cleanFilesFromPreviousVersions(project: Project) {
        if (project.isBuildSrc) {
            project.buildDir.resolve("refreshVersions_used_dependencies.txt").delete()
            project.buildDir.resolve("refreshVersions_used_repositories_maven.txt").delete()
        } else {
            project.buildDir.resolve("refreshVersions_used_dependencies_plugins.txt").delete()
            project.buildDir.resolve("refreshVersions_used_repositories_plugins_maven.txt").delete()
        }
    }

    @InternalRefreshVersionsApi
    object LogMarkers {
        @JvmField
        val default: Marker = BasicMarkerFactory().getMarker("refreshVersions")
    }

    @InternalRefreshVersionsApi
    companion object {
        val currentVersion by lazy {
            RefreshVersionsCorePlugin::class.java.getResourceAsStream("/version.txt")!!
                .bufferedReader()
                .useLines { it.first() }
        }
    }
}
