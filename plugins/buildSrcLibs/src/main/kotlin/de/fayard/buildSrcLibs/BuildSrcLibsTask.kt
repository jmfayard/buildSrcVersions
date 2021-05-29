package de.fayard.buildSrcLibs

import com.squareup.kotlinpoet.FileSpec
import de.fayard.buildSrcLibs.internal.*
import de.fayard.buildSrcLibs.internal.PluginConfig
import de.fayard.buildSrcLibs.internal.checkModeAndNames
import de.fayard.buildSrcLibs.internal.kotlinpoet
import de.fayard.refreshVersions.core.internal.OutputFile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage")
open class BuildSrcLibsTask : DefaultTask() {

    @TaskAction
    fun taskActionInitializeBuildSrc() {
        OutputFile.checkWhichFilesExist(project.rootDir)
        project.file(OutputFile.OUTPUT_DIR.path).also {
            if (it.isDirectory.not()) it.mkdirs()
        }
        val initializationMap = mapOf(
            OutputFile.BUILD to PluginConfig.INITIAL_BUILD_GRADLE_KTS,
            OutputFile.GIT_IGNORE to PluginConfig.INITIAL_GITIGNORE
        )
        for ((outputFile, initialContent) in initializationMap) {
            if (outputFile.existed.not()) {
                project.file(outputFile.path).writeText(initialContent)
                OutputFile.logFileWasModified(outputFile.path, outputFile.existed)
            }
        }
        OutputFile.VERSIONS_KT.run {
            if (existed) {
                project.file(path).delete()
                logFileWasModified(delete = true)
            }
        }
    }


    @TaskAction
    fun taskUpdateLibsKt() {
        val outputDir = project.file(OutputFile.OUTPUT_DIR.path)

        val allDependencies = project.findDependencies()
        val resolvedUseFqdn = computeUseFqdnFor(
            libraries = allDependencies,
            configured = emptyList(),
            byDefault = MEANING_LESS_NAMES
        )
        val deps = allDependencies.checkModeAndNames(resolvedUseFqdn, Case.snake_case)

        val libsFile: FileSpec = kotlinpoet(deps)

        libsFile.writeTo(outputDir)
        OutputFile.logFileWasModified(OutputFile.LIBS.path, OutputFile.LIBS.existed)
    }

}

