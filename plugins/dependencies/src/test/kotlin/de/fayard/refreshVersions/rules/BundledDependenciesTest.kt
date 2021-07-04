package de.fayard.refreshVersions.rules

import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.Version
import de.fayard.refreshVersions.internal.getArtifactNameToConstantMapping
import de.fayard.testResources
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Test
import testutils.getVersionCandidates
import testutils.isInCi

class BundledDependenciesTest {

    @Test
    fun `test bundled dependencies exist in standard repositories`() {

        val validatedDependencyMappingFile = testResources.resolve("bundled-dependencies-validated.txt")
        val validatedDependencyMapping = validatedDependencyMappingFile.readLines()
            .filter { it.isNotBlank() }
            .toSet()

        // "standard repositories" are mavenCentral and google
        val reposUrls = listOf(
            "https://repo.maven.apache.org/maven2/",
            "https://dl.google.com/dl/android/maven2/",
            "https://plugins.gradle.org/m2/"
        )

        val newValidatedMappings = runBlocking {

            getArtifactNameToConstantMapping()
                .filter { dependencyMapping ->
                    "${dependencyMapping.group}:${dependencyMapping.artifact}" !in validatedDependencyMapping
                }.map { dependencyMapping ->
                    ModuleId.Maven(dependencyMapping.group, dependencyMapping.artifact)
                }
                .distinct()
                .onEach { mavenModuleId ->
                    launch {
                        getVersionCandidates(
                            httpClient = defaultHttpClient,
                            mavenModuleId = mavenModuleId,
                            repoUrls = reposUrls,
                            currentVersion = Version("")
                        )
                    }
                }
        }

        when {
            newValidatedMappings.isEmpty() -> return
            isInCi() -> withClue(
                "Unit tests must be run and changes to bundled-dependencies-validated.txt must be committed, " +
                    "but that wasn't the case for those dependency notations."
            ) {
                newValidatedMappings shouldBe emptyList()
            }
            else -> {
                val mappings = getArtifactNameToConstantMapping().map {
                    "${it.group}:${it.artifact}"
                }.distinct().sorted().joinToString(separator = "\n")
                validatedDependencyMappingFile.writeText(mappings)
            }
        }
    }

    private val defaultHttpClient by lazy { createTestHttpClient() }

    private fun createTestHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor(logger = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    println(message)
                }
            }).setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
    }
}
