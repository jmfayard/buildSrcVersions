package de.fayard.buildSrcLibs.internal

import de.fayard.refreshVersions.core.internal.InternalRefreshVersionsApi
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency

@InternalRefreshVersionsApi
enum class Case {
    camelCase, snake_case; //, PascalCase, `kebab-case`

    companion object {
        internal fun toCamelCase(input: String): String = buildString {
            var wasWordBreak = false
            val wordBreaks = setOf(' ', '-', '_')
            for (c in input) {
                when {
                    c in wordBreaks -> {
                    }
                    wasWordBreak -> append(c.toUpperCase())
                    else -> append(c)
                }
                wasWordBreak = c in wordBreaks
            }
        }
    }
}

/**
 * We don't want to use meaningless generic libs like Libs.core
 *
 * Found many inspiration for bad libs here https://developer.android.com/jetpack/androidx/migrate
 * **/
@InternalRefreshVersionsApi
val MEANING_LESS_NAMES: MutableList<String> = mutableListOf(
    "common", "core", "testing", "runtime", "extensions",
    "compiler", "migration", "db", "rules", "runner", "monitor", "loader",
    "media", "print", "io", "collection", "gradle", "android"
)

@InternalRefreshVersionsApi
fun computeUseFqdnFor(
    libraries: List<Library>,
    configured: List<String>,
    byDefault: List<String> = MEANING_LESS_NAMES
): List<String> {
    val groups = (configured + byDefault).filter { it.contains(".") }.distinct()
    val depsFromGroups = libraries.filter { it.group in groups }.map { it.module }
    val ambiguities = libraries.groupBy { it.module }.filter { it.value.size > 1 }.map { it.key }
    return (configured + byDefault + ambiguities + depsFromGroups - groups).distinct().sorted()
}

@InternalRefreshVersionsApi
fun escapeLibsKt(name: String): String {
    val escapedChars = listOf('-', '.', ':')
    return buildString {
        for (c in name) {
            append(if (c in escapedChars) '_' else c.toLowerCase())
        }
    }
}

@InternalRefreshVersionsApi
fun Project.findDependencies(): List<Library> {
    val allDependencies = mutableListOf<Library>()
    allprojects {
        (configurations + buildscript.configurations)
            .flatMapTo(allDependencies) { configuration ->
                configuration.allDependencies
                    .filterIsInstance<ExternalDependency>()
                    .filter {
                        @Suppress("SENSELESS_COMPARISON")
                        it.group != null
                    }
                    .map { dependency ->
                        Library(dependency.group, dependency.name, dependency.version ?: "none")
                    }
            }
    }
    return allDependencies.distinctBy { d -> d.groupModule() }
}


@InternalRefreshVersionsApi
data class Library(
    val group: String = "",
    val module: String = "",
    val version: String = ""
) {
    val name: String get() = module
    fun groupModuleVersion() = "$group:$module:$version"
    fun groupModuleUnderscore() = "$group:$module:_"
    fun groupModule() = "$group:$module"
    fun versionNameCamelCase(mode: VersionMode): String =
        Case.toCamelCase(versionNameSnakeCase(mode))

    fun versionNameSnakeCase(mode: VersionMode): String = escapeLibsKt(
        when (mode) {
            VersionMode.MODULE -> module
            VersionMode.GROUP -> group
            VersionMode.GROUP_MODULE -> "${group}_$module"
        }
    )

    override fun toString() = groupModuleVersion()
}

@InternalRefreshVersionsApi
class Deps(
    val libraries: List<Library>,
    val names: Map<Library, String>
)


@InternalRefreshVersionsApi
enum class VersionMode {
    GROUP, GROUP_MODULE, MODULE
}

@InternalRefreshVersionsApi
fun List<Library>.checkModeAndNames(useFdqnByDefault: List<String>, case: Case): Deps {
    val dependencies = this

    val modes: Map<Library, VersionMode> =
        dependencies.associateWith { d ->
            when {
                d.module in useFdqnByDefault -> VersionMode.GROUP_MODULE
                escapeLibsKt(d.module) in useFdqnByDefault -> VersionMode.GROUP_MODULE
                else -> VersionMode.MODULE
            }
        }.toMutableMap()

    val versionNames = dependencies.associateWith { d ->
        val mode = modes.getValue(d)
        when (case) {
            Case.camelCase -> d.versionNameCamelCase(mode)
            Case.snake_case -> d.versionNameSnakeCase(mode)
        }
    }
    val sortedDependencies = dependencies.sortedBy { d: Library -> d.groupModule() }
    return Deps(sortedDependencies, versionNames)
}
