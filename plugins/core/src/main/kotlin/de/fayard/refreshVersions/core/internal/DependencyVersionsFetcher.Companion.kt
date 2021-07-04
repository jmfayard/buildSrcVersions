package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.DependencyVersionsFetcher
import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.extensions.gradle.npmModuleId
import de.fayard.refreshVersions.core.extensions.gradle.passwordCredentials
import de.fayard.refreshVersions.core.internal.npm.NpmDependencyVersionsFetcherHttp
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

internal fun DependencyVersionsFetcher.Companion.forMaven(
    httpClient: OkHttpClient,
    dependency: Dependency,
    repository: MavenArtifactRepository // TODO: Support Ivy repositories
): DependencyVersionsFetcher? {
    val group = dependency.group ?: return null // TODO: Support NPM dependencies from Kotlin/JS
    val name = dependency.name
    return when (repository.url.scheme) {
        "https" -> MavenDependencyVersionsFetcherHttp(
            httpClient = httpClient,
            moduleId = ModuleId.Maven(group, name),
            repoUrl = repository.url.toString().let { if (it.endsWith('/')) it else "$it/" },
            repoAuthorization = repository.passwordCredentials?.let { credentials ->
                Credentials.basic(
                    username = credentials.username ?: return@let null,
                    password = credentials.password ?: return@let null
                )
            }
        )
        "file" -> MavenDependencyVersionsFetcherFile(
            moduleId = ModuleId.Maven(group, name),
            repoUrl = repository.url.toString().let { if (it.endsWith('/')) it else "$it/" }
        )
        "gcs" -> MavenDependencyVersionsFetcherGoogleCloudStorage(
            moduleId = ModuleId.Maven(group, name),
            repoUrl = repository.url.toString()
        )
        "http" -> null //TODO: Show non fatal error that http is not supported for security reasons.
        else -> null //TODO: Support more transport protocols. Here's what Gradle supports:
        // https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:supported_transport_protocols
        // We should trigger a warning that it's not supported yet, with link to relevant issue,
        // and we should report any transport protocol not known to be supported by Gradle (but not crash
        // in case a future Gradle version adds more of them and we're not updated yet to support it).
    }
}


internal fun DependencyVersionsFetcher.Companion.forNpm(
    httpClient: OkHttpClient,
    npmDependency: Dependency,
    npmRegistry: String = "https://registry.npmjs.org/"
): DependencyVersionsFetcher {
    return NpmDependencyVersionsFetcherHttp(
        httpClient = httpClient,
        moduleId = npmDependency.npmModuleId(),
        repoUrl = npmRegistry,
        repoAuthorization = null
    )
}
