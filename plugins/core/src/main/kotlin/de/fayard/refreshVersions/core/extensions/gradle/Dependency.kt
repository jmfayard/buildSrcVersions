package de.fayard.refreshVersions.core.extensions.gradle

import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.internal.InternalRefreshVersionsApi
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency

@InternalRefreshVersionsApi
fun Dependency.moduleId(): ModuleId? = when {
    this is ExternalDependency -> ModuleId.Maven(group, name)
    this::class.simpleName == "NpmDependency" -> npmModuleId()
    else -> null
}

internal fun Dependency.npmModuleId(): ModuleId.Npm {
    val scope: String? = name.substringBefore('/').substringAfter('@').takeUnless { it == name }
    val nameWithoutScope = name.substringAfter('/')
    return ModuleId.Npm(scope, nameWithoutScope)
}
