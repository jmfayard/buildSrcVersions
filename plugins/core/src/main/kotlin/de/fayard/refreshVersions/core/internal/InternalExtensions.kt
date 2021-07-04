package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.extensions.gradle.moduleId
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency

@InternalRefreshVersionsApi
fun Dependency.hasHardcodedVersion(
    versionMap: Map<String, String>,
    versionKeyReader: ArtifactVersionKeyReader
): Boolean = isManageableVersion(versionMap, versionKeyReader).not()

@InternalRefreshVersionsApi
fun Dependency.isManageableVersion(
    versionMap: Map<String, String>,
    versionKeyReader: ArtifactVersionKeyReader
): Boolean {
    return when {
        version == versionPlaceholder -> true
        this is ExternalDependency && versionPlaceholder in this.versionConstraint.rejectedVersions -> true
        name.endsWith(".gradle.plugin") -> {
            when (val moduleId = moduleId()) {
                is ModuleId.Maven -> {
                    val versionFromProperty = versionMap[getVersionPropertyName(moduleId, versionKeyReader)]
                        ?: return false
                    versionFromProperty.isAVersionAlias().not()
                }
                else -> false
            }
        }
        else -> false
    }
}
