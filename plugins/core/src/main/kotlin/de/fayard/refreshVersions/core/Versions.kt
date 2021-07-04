@file:JvmName("Versions")

package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.internal.RefreshVersionsConfigHolder
import de.fayard.refreshVersions.core.internal.getVersionPropertyName
import de.fayard.refreshVersions.core.internal.resolveVersion

fun versionFor(versionKey: String): String {
    // This function is overloaded to allow named parameter usage in Kotlin.
    // However, no check is performed here because we cannot detect if
    // the function wasn't called with named argument.
    return retrieveVersionFor(dependencyNotationOrVersionKey = versionKey)
}

fun versionFor(dependencyNotation: CharSequence): String {
    // This function is overloaded to allow named parameter usage in Kotlin.
    // However, no check is performed here because we cannot detect if
    // the function wasn't called with named argument.
    return retrieveVersionFor(dependencyNotationOrVersionKey = dependencyNotation)
}

private fun retrieveVersionFor(dependencyNotationOrVersionKey: CharSequence): String {
    val isDependencyNotation = ':' in dependencyNotationOrVersionKey
    val versionKey = when {
        isDependencyNotation -> {
            require(dependencyNotationOrVersionKey.endsWith(":_")) {
                "Expects a refreshVersions compatible dependency notation with the version placeholder (_)." +
                    "\n" +
                    "If the dependency is from BoM, " +
                    "pass the BoM dependency notation itself with the version placeholder."
            }
            dependencyNotationOrVersionKey.toString().let {
                getVersionPropertyName(
                    moduleId = ModuleId.Maven(
                        group = it.substringBefore(':'),
                        name = it.substringBeforeLast(':').substringAfter(':')
                    ),
                    versionKeyReader = RefreshVersionsConfigHolder.versionKeyReader
                )
            }
        }
        else -> dependencyNotationOrVersionKey.toString().also {
            require(it.startsWith("version.") || it.startsWith("plugin.")) {
                "Version keys all start with 'version.' or 'plugin.'. You need to pass the full version key."
            }
        }
    }
    return resolveVersion(
        properties = RefreshVersionsConfigHolder.lastlyReadVersionsMap,
        key = versionKey
    ) ?: resolveVersion(
        properties = RefreshVersionsConfigHolder.readVersionsMap(),
        key = versionKey
    ) ?: RefreshVersionsConfigHolder.versionsPropertiesFile.name.let { versionsFileName ->
        val errorMessage = when {
            isDependencyNotation -> "The version of the artifact $dependencyNotationOrVersionKey requested in " +
                "versionFor call wasn't found in the $versionsFileName file.\n" +
                "Expected a value for the corresponding key: $versionKey"
            else -> "The version for the key $versionKey requested in " +
                "versionFor call wasn't found in the $versionsFileName file"
        }
        error(errorMessage)
    }
}
