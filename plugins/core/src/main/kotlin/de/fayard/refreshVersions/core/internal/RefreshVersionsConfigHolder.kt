package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.DependencySelection
import de.fayard.refreshVersions.core.extensions.gradle.isBuildSrc
import de.fayard.refreshVersions.core.extensions.gradle.isRootProject
import de.fayard.refreshVersions.core.internal.versions.VersionsPropertiesModel
import de.fayard.refreshVersions.core.internal.versions.VersionsPropertiesModel.Section.VersionEntry
import de.fayard.refreshVersions.core.internal.versions.readFrom
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@InternalRefreshVersionsApi
object RefreshVersionsConfigHolder {

    internal val resettableDelegates = ResettableDelegates()

    var dependencyFilter: DependencySelection.() -> Boolean = { false }

    fun markSetupViaSettingsPlugin() {
        isSetupViaPlugin = true
    }

    internal var isSetupViaPlugin = false
        private set

    private val versionKeyReaderDelegate = resettableDelegates.LateInit<ArtifactVersionKeyReader>()

    var versionKeyReader: ArtifactVersionKeyReader by versionKeyReaderDelegate
        private set

    var versionsPropertiesFile: File by resettableDelegates.LateInit()
        private set

    val buildSrc: Project? get() = buildSrcSettings?.gradle?.rootProject

    internal var settings: Settings by resettableDelegates.LateInit()
        private set

    internal var lastlyReadVersionsMap: Map<String, String> by resettableDelegates.MutableLazy {
        readVersionsMap()
    }

    fun readVersionsMap(): Map<String, String> {
        val model = VersionsPropertiesModel.readFrom(versionsPropertiesFile)
        return model.sections.filterIsInstance<VersionEntry>().associate { it.key to it.currentVersion }.also {
            lastlyReadVersionsMap = it
        }
    }

    fun allProjects(project: Project): Sequence<Project> {
        require(project.isRootProject)
        val buildSrcProjects = buildSrc?.allprojects?.asSequence() ?: emptySequence()
        return buildSrcProjects + project.allprojects.asSequence()
    }

    internal val resultMode: VersionCandidatesResultMode = VersionCandidatesResultMode(
        filterMode = VersionCandidatesResultMode.FilterMode.AllIntermediateVersions,
        sortingMode = VersionCandidatesResultMode.SortingMode.ByVersion
    )

    internal val httpClient: OkHttpClient by resettableDelegates.Lazy {
        OkHttpClient.Builder()
            .addInterceptor( //TODO: Allow disabling/configuring logging.
                HttpLoggingInterceptor(logger = object : HttpLoggingInterceptor.Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }).setLevel(HttpLoggingInterceptor.Level.BASIC)
            )
            .build()
    }

    internal fun initialize(
        settings: Settings,
        artifactVersionKeyRules: List<String>,
        versionsPropertiesFile: File
    ) {
        require(settings.isBuildSrc.not())
        settings.gradle.buildFinished {
            clearStaticState()
        }
        this.settings = settings

        this.versionsPropertiesFile = versionsPropertiesFile.also {
            it.createNewFile() // Creates the file if it doesn't exist yet
        }
        this.artifactVersionKeyRules = artifactVersionKeyRules
        versionKeyReader = ArtifactVersionKeyReader.fromRules(filesContent = artifactVersionKeyRules)
    }

    internal fun initializeBuildSrc(settings: Settings) {
        require(settings.isBuildSrc)
        buildSrcSettings = settings

        // The buildSrc will be built a second time as a standalone project by IntelliJ or
        // Android Studio after running initially properly after host project settings evaluation.
        // To workaround this, we persist the configuration and attempt restoring it if needed,
        // to not fail the second build (since it'd display errors, and
        // prevent from seeing resolved dependencies in buildSrc sources in the IDE).
        //
        // Should the IDE really build the buildSrc as a standalone mode when not explicitly
        // requested?
        // Might be worth opening an issue on https://youtrack.jetbrains.com to find out.
        if (versionKeyReaderDelegate.isInitialized) {
            persistInitData(settings)
        } else {
            runCatching {
                restorePersistedInitData(settings)
            }.onFailure { e ->
                throw IllegalStateException(
                    "You also need to bootstrap refreshVersions in the " +
                            "settings.gradle[.kts] file of the root project",
                    e
                )
            }
            settings.gradle.buildFinished {
                clearStaticState()
            }
        }
    }

    private fun clearStaticState() {
        httpClient.dispatcher.executorService.shutdown()
        resettableDelegates.reset()
        // Clearing static state is needed because Gradle holds onto previous builds, yet,
        // duplicates static state.
        // We need to beware of never retaining Gradle objects.
        // This must be called in gradle.buildFinished { }.
    }

    private var artifactVersionKeyRules: List<String> by resettableDelegates.LateInit()

    internal var buildSrcSettings: Settings? by resettableDelegates.NullableDelegate()
        private set


    private fun persistInitData(settings: Settings) {
        versionsPropertiesFile // Check it is initialized by accessing it.
        artifactVersionKeyRules // Check it is initialized by accessing it.
        versionKeyReader // Check it is initialized by accessing it.

        settings.artifactVersionKeyRulesFile.let { file ->
            if (file.exists().not()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            ObjectOutputStream(file.outputStream()).use {
                it.writeObject(artifactVersionKeyRules.toTypedArray())
            }
        }
        settings.versionsPropertiesFileFile.let { file ->
            if (file.exists().not()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            ObjectOutputStream(file.outputStream()).use {
                it.writeObject(versionsPropertiesFile)
            }
        }
    }

    private fun restorePersistedInitData(settings: Settings) {
        versionsPropertiesFile = settings.versionsPropertiesFileFile.let { file ->
            ObjectInputStream(file.inputStream()).use { it.readObject() as File }
        }
        artifactVersionKeyRules = settings.artifactVersionKeyRulesFile.let { file ->
            ObjectInputStream(file.inputStream()).use {
                @Suppress("unchecked_cast")
                (it.readObject() as Array<String>).asList()
            }
        }
        versionKeyReader = ArtifactVersionKeyReader.fromRules(filesContent = artifactVersionKeyRules)
    }

    private val Settings.artifactVersionKeyRulesFile: File
        get() = rootDir.resolve("build").resolve("refreshVersions_artifactVersionKeyRules.bin")

    private val Settings.versionsPropertiesFileFile: File
        get() = rootDir.resolve("build").resolve("refreshVersions_versionsPropertiesFilePath.bin")
}
