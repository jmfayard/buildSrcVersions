package de.fayard.refreshVersions.core

data class DependencySelection(
    val moduleId: ModuleId,
    val current: Version,
    val versionKey: String,
    var candidate: Version
)
