@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.gradle.kotlin.dsl

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Marker interface that prevents the user to add a wrong dependency
 * so that the IDE can show a warning
 *
 * ```
 * dependencies {
 *     implementation(Testing.kotest)
 *                   ^^^^^^^^^^^^^^
 *        This is not a valid dependency notation but a group of dependency notations
 * }
 * ```
 */
interface IsNotADependency

private const val ErrorIsNotADependency =
    """This is not a valid dependency notation but a group of dependency notations"""

@Deprecated(ErrorIsNotADependency)
fun DependencyHandler.`implementation`(dependencyNotation: IsNotADependency): Dependency? =
    null

@Deprecated(ErrorIsNotADependency)
fun DependencyHandler.`api`(dependencyNotation: IsNotADependency): Dependency? =
    null

@Deprecated(ErrorIsNotADependency)
fun DependencyHandler.`testImplementation`(dependencyNotation: IsNotADependency): Dependency? =
    null
