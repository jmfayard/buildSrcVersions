package de.fayard.refreshVersions.core.internal

import org.gradle.kotlin.dsl.IsNotADependency

@InternalRefreshVersionsApi
open class DependencyGroup(
    val group: String,
    rawRule: String? = null,
    var usePlatformConstraints: Boolean = false
) : IsNotADependency {

    val rule: ArtifactVersionKeyRule? = rawRule?.let {
        val lines = it.lines()
        assert(lines.size == 2) {
            "2 lines were expected, but ${lines.size} were found: $it"
        }
        ArtifactVersionKeyRule(
            artifactPattern = lines.first(),
            versionKeyPattern = lines.last()
        )
    }


    companion object {
        private val ALL = mutableListOf<DependencyGroup>()
        val ALL_RULES: List<ArtifactVersionKeyRule>
            get() = ALL.mapNotNull { it.rule }
    }

    init {
        assert(group.isNotBlank()) { "Group shall not be blank" }
        ALL.add(this)
    }

    fun module(module: String): String {
        assert(module.trimStart() == module) { "module=[$module] has superfluous leading whitespace" }
        assert(module.trimEnd() == module) { "module=[$module] has superfluous trailing whitespace" }
        assert(module.contains(":").not()) { "module=[$module] is invalid" }
        return "$group:$module" + if (usePlatformConstraints) "" else ":_"
    }
}
