package de.fayard.refreshVersions.core

import org.gradle.api.Incubating

@Incubating
sealed class ModuleId {
    abstract val group: String?
    abstract val name: String

    data class Maven(
        override val group: String,
        override val name: String
    ) : ModuleId()

    data class Npm(
        override val group: String?,
        override val name: String
    ) : ModuleId()
}
