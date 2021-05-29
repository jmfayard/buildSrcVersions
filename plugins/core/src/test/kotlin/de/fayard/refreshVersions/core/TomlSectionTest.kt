package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.VersionsCatalogTask.Companion.parseTomlInSection
import de.fayard.refreshVersions.core.VersionsCatalogTask.Companion.tomlSectionsToString
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TomlSectionTest : StringSpec({

    val toml = """
        ## This is a great comment

        [versions]
        groovy = "2.5.14"
        guava = "30.0-jre"
        jupiter = "5.7.1"

        [libraries]
        guava = { module="com.google.guava:guava", version.ref="guava" }
        junit-jupiter = { module="org.junit.jupiter:junit-jupiter-api", version.ref="jupiter" }
        junit-engine = { module="org.junit.jupiter:junit-jupiter-engine" }

        groovy-core = { module="org.codehaus.groovy:groovy", version.ref="groovy" }
        groovy-json = { module="org.codehaus.groovy:groovy-json", version.ref="groovy" }

        [bundles]
        testDependencies = ["junit-jupiter", "junit-engine"]
        """.trimIndent()

    val (a, b, c, d) = """
        ## This is a great comment

        ---
        groovy = "2.5.14"
        guava = "30.0-jre"
        jupiter = "5.7.1"

        ---
        guava = { module="com.google.guava:guava", version.ref="guava" }
        junit-jupiter = { module="org.junit.jupiter:junit-jupiter-api", version.ref="jupiter" }
        junit-engine = { module="org.junit.jupiter:junit-jupiter-engine" }

        groovy-core = { module="org.codehaus.groovy:groovy", version.ref="groovy" }
        groovy-json = { module="org.codehaus.groovy:groovy-json", version.ref="groovy" }

        ---
        testDependencies = ["junit-jupiter", "junit-engine"]
        """.trimIndent().split("---\n")
    val expected = mapOf("root" to a, "versions" to b, "libraries" to c, "bundles" to d)

    "Parse Toml in Sections" {
        parseTomlInSection(toml) shouldBe expected
    }

    "Sections to Toml" {
        tomlSectionsToString(expected) shouldBe toml
    }

})



