package de.fayard.refreshVersions.core

import testutils.MavenRepoUrl
import testutils.disabledBecauseIsAnExperiment
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class PomUrlTest {

    @Test
    @Disabled("Because secret GitHub token propagation is not set up yet")
    fun testAuthenticatedRepo() {
        val xml = getMetadata(
            moduleId = ModuleId.Maven(
                group = "se.jsimo.hello.maven",
                name = "hello-world-maven"
            ),
            mavenRepoUrl = "https://maven.pkg.github.com/jesigloot/maven-hello-world",
            authorization = Credentials.basic(
                username = TODO(),
                password = TODO()
            )
        )
        println(xml)
    }

    @Test
    @Disabled(disabledBecauseIsAnExperiment)
    fun testGradlePlugin() {
        val xml = getMetadata(
            moduleId = ModuleId.Maven(
                group = "org.gradle.hello-world",
                name = "org.gradle.hello-world.gradle.plugin"
            ),
            mavenRepoUrl = "https://plugins.gradle.org/m2"
        )
        println(xml)
    }

    @Test
    @Disabled(disabledBecauseIsAnExperiment)
    fun testKotlinEap() {
        val xml = getMetadata(
            moduleId = ModuleId.Maven(
                group = "org.jetbrains.kotlin",
                name = "kotlin-stdlib"
            ),
            mavenRepoUrl = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/"
        )
        println(xml)
    }

    private fun getMetadata(
        httpClient: OkHttpClient = defaultHttpClient,
        moduleId: ModuleId.Maven,
        mavenRepoUrl: String,
        authorization: String? = null
    ): String {
        val repoUrl = MavenRepoUrl(url = mavenRepoUrl)
        val artifactUrl = repoUrl.metadataUrlForArtifact(
            group = moduleId.group,
            name = moduleId.name
        )
        val request = Request.Builder()
            .url(artifactUrl)
            .apply {
                authorization?.let { header("Authorization", value = authorization) }
            }
            .build()
        return httpClient.newCall(request).execute().body!!.charStream().readText()
    }

    private val defaultHttpClient by lazy { createTestHttpClient() }

    private fun createTestHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor())
            .build()
    }
}
