package de.fayard.refreshVersions.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

class PluginsTest : StringSpec({
    val plugins: List<Pair<String, String?>> = listOf(
        "org.jetbrains.kotlin.multiplatform.gradle.plugin" to "version.kotlin",
        "org.jetbrains.kotlin.plugin.serialization.gradle.plugin" to "version.kotlin",
        "org.jetbrains.kotlin.jvm.gradle.plugin" to "version.kotlin",
        "com.android.application" to "plugin.android",
        "com.android.library" to "plugin.android",
        "com.adarshr.test-logger.gradle.plugin" to "plugin.com.adarshr.test-logger",
        "org.jetbrains.dokka.gradle.plugin" to "plugin.org.jetbrains.dokka",
        "com.github.ben-manes.versions.gradle.plugin" to "plugin.com.github.ben-manes.versions",
        "xxx.whatever.invalid" to null,
        "whatever this string is, it's not a plugin" to null
    )

    "Convert plugin dependency to version key" {
        plugins.forAll { (dependencyNotation, versionKey) ->
            pluginDependencyNotationToVersionKey(dependencyNotation) shouldBe versionKey
        }
    }
})

