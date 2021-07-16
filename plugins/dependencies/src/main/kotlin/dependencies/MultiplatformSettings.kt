@file:Suppress("PackageDirectoryMismatch", "SpellCheckingInspection", "unused")

import dependencies.DependencyNotationAndGroup
import org.gradle.api.Incubating
import org.gradle.kotlin.dsl.IsNotADependency

@Incubating
object MultiplatformSettings : DependencyNotationAndGroup(
    group = "com.russhwolf",
    name = "multiplatform-settings"
) {
    @JvmField val coroutines = "$artifactPrefix-coroutines:_"
    @JvmField val coroutinesNativeMultithread = "$artifactPrefix-coroutines-native-mt:_"
    @JvmField val dataStore = "$artifactPrefix-datastore:_"
    @JvmField val noArg = "$artifactPrefix-no-arg:_"
    @JvmField val serialization = "$artifactPrefix-serialization:_"
    @JvmField val settings = "$artifactPrefix:_"
    @JvmField val test = "$artifactPrefix-test:_"
}
