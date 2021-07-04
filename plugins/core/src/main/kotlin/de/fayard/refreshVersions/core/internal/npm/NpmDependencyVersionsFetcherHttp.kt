package de.fayard.refreshVersions.core.internal.npm

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.fayard.refreshVersions.core.DependencyVersionsFetcher
import de.fayard.refreshVersions.core.ModuleId
import de.fayard.refreshVersions.core.Version
import de.fayard.refreshVersions.core.extensions.okhttp.await
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant

internal class NpmDependencyVersionsFetcherHttp(
    private val httpClient: OkHttpClient,
    moduleId: ModuleId.Npm,
    val repoUrl: String = "https://registry.npmjs.org/",
    repoAuthorization: String? = null
) : DependencyVersionsFetcher(
    moduleId = moduleId,
    repoKey = repoUrl
) {
    init {
        require(repoUrl.endsWith('/'))
    }

    private val request = Request.Builder().apply {
        val metadataUrlForArtifact = moduleId.let { (group, name) ->
            if (group == null || group == "npm") {
                "$repoUrl$name"
            } else {
                "$repoUrl@$group/$name"
            }
        }
        url(metadataUrlForArtifact)
//        header(
//            name = "Authorization",
//            value = repoAuthorization ?: return@apply
//        )
    }.build()


    override suspend fun getAvailableVersionsOrNull(versionFilter: ((Version) -> Boolean)?): SuccessfulResult? {

        val jsonString = getJsoMmetadataOrNull() ?: return null

        val jsonAdapter = moshi.adapter(NpmMetadata::class.java)
        val metadata = jsonAdapter.fromJson(jsonString) ?: return null

        val allVersions = parseVersionsFromNpmMetaData(metadata)
        return SuccessfulResult(
            lastUpdateTimestampMillis = parseLastUpdatedFromNpmMetaData(metadata),
            availableVersions = if (versionFilter == null) {
                allVersions
            } else {
                allVersions.filter(versionFilter)
            }
        )
    }

    suspend fun getJsoMmetadataOrNull(): String? {
        return httpClient.newCall(request).await().use { response ->
            if (response.isSuccessful) {
                response.use { it.body!!.string() }
            } else when (response.code) {
                404 -> null // Normal not found result
                401 -> null // Returned by some repositories that have optional authentication
                else -> throw HttpException(Response.error<Any?>(response.code, response.body!!))
            }
        }
    }

    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        private fun parseVersionsFromNpmMetaData(metadata: NpmMetadata): List<Version> {
            return metadata.versions.keys.mapNotNull {
                if (it.isNotBlank()) Version(it.trim()) else null
            }
        }

        private fun parseLastUpdatedFromNpmMetaData(metadata: NpmMetadata): Long {
            val timestamp = metadata.time["modified"]
            return Instant.parse(timestamp).epochSecond
        }
    }
}

internal data class NpmMetadata(
    val versions: Map<String, NpmVersion>,
    val time: Map<String, String>
)
internal data class NpmVersion(
    val name: String,
    val version: String
)
