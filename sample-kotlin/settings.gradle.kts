import de.fayard.refreshVersions.core.FeatureFlag.*

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    val versionFile = rootDir.parentFile.resolve("plugins/version.txt")
    val pluginsVersion = versionFile.readLines().first()

    @Suppress("UnstableApiUsage")
    plugins {
        id("de.fayard.refreshVersions").version(pluginsVersion)
    }
}

plugins {
    id("com.gradle.enterprise").version("3.1.1")
    id("de.fayard.refreshVersions")
}

refreshVersions {
    featureFlags {
        enable(LIBS)
        disable(GRADLE_UPDATES)
    }

    extraArtifactVersionKeyRules(file("refreshVersions-extra-rules.txt"))

    // Reject unstable versions unless the current version is also unstable
    rejectVersionIf {
        versionKey != "version.kotlinx.coroutines" && candidate.stabilityLevel.isLessStableThan(current.stabilityLevel)
    }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "sample-kotlin"
