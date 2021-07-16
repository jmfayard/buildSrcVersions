@file:Suppress("PackageDirectoryMismatch", "SpellCheckingInspection", "unused")

import org.gradle.api.Incubating
import org.gradle.kotlin.dsl.IsNotADependency

@Incubating
object Koin {
    val core = Core
    val junit = JUnit
    val android = Android
    val ktor = Ktor

    private const val artifactPrefix = "io.insert-koin:koin"

    object Core : IsNotADependency {
        const val core = "$artifactPrefix-core:_"
        const val test = "$artifactPrefix-test:_"
    }

    object JUnit : IsNotADependency {
        const val junit4 = "$artifactPrefix-test-junit4:_"
        const val junit5 = "$artifactPrefix-test-junit5:_"
    }

    object Android : IsNotADependency {
        const val android = "$artifactPrefix-android:_"
        const val androidCompat = "$artifactPrefix-android-compat:_"
        const val workManager = "$artifactPrefix-androidx-workmanager:_"
        const val compose = "$artifactPrefix-androidx-compose:_"
    }

    object Ktor : IsNotADependency {
        const val ktor = "$artifactPrefix-ktor:_"
        const val logger = "$artifactPrefix-logger-slf4j:_"
    }
}
