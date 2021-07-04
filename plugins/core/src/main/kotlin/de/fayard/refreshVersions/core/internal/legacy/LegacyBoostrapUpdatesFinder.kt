package de.fayard.refreshVersions.core.internal.legacy

import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.RefreshVersionsCorePlugin
import de.fayard.refreshVersions.core.Version
import de.fayard.refreshVersions.core.internal.DependencyWithVersionCandidates
import de.fayard.refreshVersions.core.internal.RefreshVersionsConfigHolder
import de.fayard.refreshVersions.core.internal.VersionCandidatesResultMode
import de.fayard.refreshVersions.core.internal.getDependencyVersionFetchers
import de.fayard.refreshVersions.core.internal.getVersionCandidates
import okhttp3.OkHttpClient

internal object LegacyBoostrapUpdatesFinder {

    suspend fun getSelfUpdates(
        httpClient: OkHttpClient,
        resultMode: VersionCandidatesResultMode
    ): DependencyWithVersionCandidates {
        val moduleId = ModuleId.Maven(group = "de.fayard.refreshVersions", name = "refreshVersions")

        val versionsFetchers = RefreshVersionsConfigHolder.settings.getDependencyVersionFetchers(
            httpClient = httpClient,
            dependencyFilter = { dependency ->
                dependency.group == moduleId.group && dependency.name == moduleId.name
            }
        ).toList()

        val currentVersion = RefreshVersionsCorePlugin.currentVersion

        return DependencyWithVersionCandidates(
            moduleId = moduleId,
            currentVersion = currentVersion,
            versionsCandidates = versionsFetchers.getVersionCandidates(
                currentVersion = Version(currentVersion),
                resultMode = resultMode
            )
        )
    }
}
