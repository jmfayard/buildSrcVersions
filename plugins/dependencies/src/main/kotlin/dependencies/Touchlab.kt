@file:Suppress("PackageDirectoryMismatch", "SpellCheckingInspection", "unused")

import org.gradle.api.Incubating
import org.gradle.kotlin.dsl.IsNotADependency

@Incubating
object Touchlab {
    val stately = Stately

    object Stately : IsNotADependency {
        private const val artifactPrefix = "co.touchlab:stately"
        const val common = "$artifactPrefix-common:_"
        const val concurrency = "$artifactPrefix-concurrency:_"
        const val isolate = "$artifactPrefix-isolate:_"
        const val isoCollections = "$artifactPrefix-iso-collections:_"
    }
}
