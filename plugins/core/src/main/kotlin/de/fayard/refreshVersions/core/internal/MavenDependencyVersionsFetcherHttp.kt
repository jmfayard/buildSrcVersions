package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.extensions.okhttp.await
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response

internal class MavenDependencyVersionsFetcherHttp(
    private val httpClient: OkHttpClient,
    moduleId: ModuleId.Maven,
    val repoUrl: String,
    repoAuthorization: String?
) : MavenDependencyVersionsFetcher(
    moduleId = moduleId,
    repoUrl = repoUrl
) {

    init {
        require(repoUrl.endsWith('/'))
    }

    private val request = Request.Builder().apply {
        val metadataUrlForArtifact = moduleId.let { (group, name) ->
            "$repoUrl${group!!.replace('.', '/')}/$name/maven-metadata.xml"
        }
        url(metadataUrlForArtifact)
        header(
            name = "Authorization",
            value = repoAuthorization ?: return@apply
        )
    }.build()

    override suspend fun getXmlMetadataOrNull(): String? {
        return httpClient.newCall(request).await().use { response ->
            if (response.isSuccessful) {
                response.use { it.body!!.string() }
            } else when (response.code) {
                403 -> when {
                    repoUrl.let {
                        it.startsWith("https://dl.bintray.com") || it.startsWith("https://jcenter.bintray.com")
                    } -> null // Artifact not available on jcenter nor bintray, post "sunset" announcement.
                    else -> throw HttpException(Response.error<Any?>(response.code, response.body!!))
                }
                404 -> null // Normal not found result
                401 -> null // Returned by some repositories that have optional authentication (like jitpack.io)
                else -> throw HttpException(Response.error<Any?>(response.code, response.body!!))
            }
        }
    }
}
