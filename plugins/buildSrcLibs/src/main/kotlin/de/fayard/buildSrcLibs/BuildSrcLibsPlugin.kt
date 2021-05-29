package de.fayard.buildSrcLibs

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class BuildSrcLibsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<BuildSrcLibsTask>(
            name = "buildSrcLibs"
        ) {
            group = "refreshVersions"
            description = "Update buildSrc/src/main/kotlin/Libs.kt"
            outputs.upToDateWhen { false }
            dependsOn("refreshVersionsMissingEntries")
        }
        project.tasks.register<DefaultTask>(
            name = "buildSrcVersions"
        ) {
            group = "refreshVersions"
            description = "Update buildSrc/src/main/kotlin/Libs.kt"
            dependsOn("buildSrcLibs")
        }
    }
}
