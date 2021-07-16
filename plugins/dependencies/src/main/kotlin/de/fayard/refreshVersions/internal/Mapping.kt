package de.fayard.refreshVersions.internal

import AndroidX
import COIL
import CashApp
import Firebase
import Google
import JakeWharton
import Kotlin
import KotlinX
import Ktor
import Splitties
import Square
import Testing
import Touchlab
import dependencies.DependencyNotationAndGroup
import org.gradle.api.artifacts.ModuleIdentifier
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

internal data class DependencyMapping(
    val group: String,
    val artifact: String,
    val constantName: String
) {
    companion object {
        fun fromLine(line: String): DependencyMapping? {
            if (line.isEmpty()) return null
            val (key, constantName) = line.split("=").takeIf { it.size == 2 } ?: return null
            val (group, artifact) = key.split("..").takeIf { it.size == 2 } ?: return null
            return DependencyMapping(group, artifact, constantName)
        }
    }

    override fun toString(): String = string

    private val string by lazy(LazyThreadSafetyMode.NONE) { "$group..$artifact=$constantName" }
}

internal fun getArtifactNameToConstantMapping(excludeBomDependencies: Boolean = false): List<DependencyMapping> {
    return sequenceOf(
        AndroidX,
        CashApp,
        Google,
        JakeWharton,
        Firebase,
        Kotlin,
        KotlinX,
        Splitties,
        Square,
        Ktor,
        Testing,
        COIL,
        Touchlab
    ).flatMap { objectInstance ->
        getArtifactNameToConstantMappingFromObject(
            objectInstance,
            excludeBomDependencies = excludeBomDependencies,
            isTopLevelObject = true
        )
    }.sortedBy { it.toString() }.toList()
}

internal fun getArtifactsFromDependenciesObject(objectInstance: Any): List<ModuleIdentifier> {
    return getArtifactNameToConstantMappingFromObject(
        objectInstance,
        objectInstance::class.simpleName!!
    ).map {
        object : ModuleIdentifier {
            override fun getGroup(): String = it.group
            override fun getName(): String = it.artifact
        }
    }.toList()
}

private fun getArtifactNameToConstantMappingFromObject(
    objectInstance: Any,
    prefix: String = with(objectInstance::class) {
        if (isCompanion) qualifiedName!!.removeSuffix(".Companion") else qualifiedName!!
    },
    excludeBomDependencies: Boolean = false,
    isTopLevelObject: Boolean = false
): Sequence<DependencyMapping> {
    val objectClass = objectInstance::class

    val mappingOfNestedObjects = objectClass.memberProperties.asSequence().filter { kProperty ->
        @OptIn(ExperimentalStdlibApi::class)
        kProperty.visibility == KVisibility.PUBLIC && kProperty.returnType.let {
            // Filter out dependency constants and redirection properties.
            it != typeOf<String>() && it.javaType != java.lang.Void::class.java
        }
    }.flatMap { kProperty ->
        @Suppress("unchecked_cast")
        val nestedObjectInstance = (kProperty as KProperty1<Any?, Any>).get(objectInstance)
        getArtifactNameToConstantMappingFromObject(
            objectInstance = nestedObjectInstance,
            prefix = "$prefix.${kProperty.name}",
            excludeBomDependencies = excludeBomDependencies
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    val currentObjectDependencyNotations: Sequence<Pair<KProperty<*>?, String>> =
        objectClass.memberProperties.asSequence().filter { kProperty ->
            kProperty.visibility == KVisibility.PUBLIC && kProperty.returnType.let {
                (it == typeOf<String>() || it.isSubtypeOf(typeOf<DependencyNotationAndGroup>())) &&
                        it.javaType != java.lang.Void::class.java // Filter out redirection properties.
            }
        }.mapNotNull { kProperty ->
            val javaField: Field? = kProperty.javaField

            @OptIn(ExperimentalStdlibApi::class)
            @Suppress("unchecked_cast")
            val dependencyNotation = when {
                kProperty.returnType.isSubtypeOf(typeOf<DependencyNotationAndGroup>()) -> {
                    (kProperty as KProperty1<Any?, DependencyNotationAndGroup>).get(objectInstance).backingString
                }
                kProperty.isConst -> javaField!!.get(null).toString()
                else -> try {
                    (kProperty as KProperty1<Any?, String>).get(objectInstance)
                } catch (e: IllegalArgumentException) {
                    javaField!!.get(objectInstance).toString()
                }
            }
            kProperty to dependencyNotation
        }

    val dependencyNotations = if (isTopLevelObject && objectInstance is DependencyNotationAndGroup) {
        currentObjectDependencyNotations + (null to objectInstance.backingString)
    } else currentObjectDependencyNotations

    val currentObjectDependencyMapping = dependencyNotations.mapNotNull { (kProperty, dependencyNotation) ->
        val artifactName = dependencyNotation.let { value ->
            val columnCount = value.count { it == ':' }
            if (columnCount < if (excludeBomDependencies) 2 else 1) return@mapNotNull null
            val hasVersion = columnCount == 2
            if (hasVersion) {
                value.substringBeforeLast(':') // Before version delimiter.
            } else value
        }
        val constantName = if (kProperty == null) {
            objectClass.simpleName!!
        } else {
            "$prefix.${kProperty.name}"
        }
        val group = artifactName.substringBefore(':')
        val name = artifactName.substringAfter(':')
        DependencyMapping(
            group = group,
            artifact = name,
            constantName = constantName
        )
    }

    return mappingOfNestedObjects + currentObjectDependencyMapping
}
