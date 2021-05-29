package de.fayard.refreshVersions.core

import de.fayard.buildSrcLibs.internal.Case
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

class CaseTest : StringSpec({
    val transformations = listOf(
        "hoplite_yaml" to "hopliteYaml",
        "picnic" to "picnic",
        "h2_ok" to "h2Ok",
        "mockito_kotlin" to "mockitoKotlin",
        "retrofit2_kotlinx_serialization_converter" to "retrofit2KotlinxSerializationConverter",
        "dagger_compiler" to "daggerCompiler"
    )

    "From snake_case to camelCase" {
        transformations.forAll { (snake, caml) ->
            Case.toCamelCase(snake) shouldBe caml
        }
    }

})
