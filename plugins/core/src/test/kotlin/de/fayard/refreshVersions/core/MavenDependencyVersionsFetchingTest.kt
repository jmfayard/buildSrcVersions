package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.internal.GradleUpdateChecker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import testutils.disabledBecauseIsAnExperiment
import testutils.getVersionCandidates

class MavenDependencyVersionsFetchingTest {

    @Test
    fun fetchGradleVersion() = runBlocking {
        val checker = GradleUpdateChecker(defaultHttpClient)
        GradleUpdateChecker.VersionType.values().filterNot {
            it == GradleUpdateChecker.VersionType.All // Filtered out because quite slow and unused for now.
        }.forEach { versionType ->
            launch {
                println(checker.fetchGradleVersion(versionType))
            }
        }
    }


    @Test
    @Disabled(disabledBecauseIsAnExperiment)
    fun testKotlin() {
        runBlocking {
            val versions = getVersionCandidates(
                moduleId = ModuleId.Maven(
                    group = "org.jetbrains.kotlin",
                    name = "kotlin-stdlib"
                ),
                currentVersion = Version("1.3.50"),
                repoUrls = listOf(
                    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/",
                    "https://repo1.maven.org/maven2/"
                )
            ).joinToString(separator = "\n") { it.value }
            println(versions)
        }
    }

    private suspend fun getVersionCandidates(
        moduleId: ModuleId.Maven,
        currentVersion: Version,
        repoUrls: List<String>
    ): List<Version> = getVersionCandidates(
        httpClient = defaultHttpClient,
        mavenModuleId = moduleId,
        currentVersion = currentVersion,
        repoUrls = repoUrls
    )

    private val defaultHttpClient by lazy { createTestHttpClient() }

    private fun createTestHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor())
            .build()
    }
}
