package de.fayard

import de.fayard.refreshVersions.core.internal.ArtifactVersionKeyReader
import de.fayard.refreshVersions.internal.DependencyMapping
import de.fayard.refreshVersions.internal.getArtifactNameToConstantMapping
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import testutils.isInCi
import java.io.File

val testResources: File = File(".").absoluteFile.resolve("src/test/resources")

class NonRegression: FreeSpec({

    "We should never remove a property" {
        val existingProperties = testResources.resolve("dependencies-mapping-validated.txt")
        val receivedProperties = testResources.resolve("dependencies-mapping-received.txt")

        val existingMapping = existingProperties.readLines().mapNotNull { DependencyMapping.fromLine(it) }
        val receivedMapping = getArtifactNameToConstantMapping()
        receivedProperties.writeText(receivedMapping.joinToString(separator = "\n", postfix = "\n"))

        val breakingChanges = existingMapping - receivedMapping
        withClue("diff -u ${existingProperties.absolutePath}  ${receivedProperties.absolutePath}") {
            breakingChanges should haveSize(0)
        }
        receivedProperties.copyTo(existingProperties, overwrite = true)
        receivedProperties.deleteOnExit()
    }

    "We should not change version keys" {
        val mainResources: File = File(".").absoluteFile.resolve("src/main/resources")
        val rulesDir = mainResources.resolve("refreshVersions-rules")
        val versionKeyReader = ArtifactVersionKeyReader.fromRules(rulesDir.listFiles()!!.map { it.readText() })

        val existingKeys = testResources.resolve("dependencies-versions-key-validated.txt")
        val receivedKeys = testResources.resolve("dependencies-versions-key-received.txt")

        val existingMapping = existingKeys.readLines().mapNotNull { DependencyMapping.fromLine(it) }
        val receivedMapping = getArtifactNameToConstantMapping().map {
            val key = versionKeyReader.readVersionKey(it.group, it.artifact) ?: "NO-RULE"
            it.copy(constantName = "version.$key")
        }
        receivedKeys.writeText(receivedMapping.joinToString(separator = "\n", postfix = "\n"))

        val breakingChanges = existingMapping - receivedMapping
        withClue("diff -u ${existingKeys.absolutePath}  ${receivedKeys.absolutePath}") {
            breakingChanges should haveSize(0)
        }
        withClue("Changes to $existingKeys must be committed, but I got new entries") {
            if (isInCi()) (receivedMapping - existingMapping) should haveSize(0)
        }
        receivedKeys.copyTo(existingKeys, overwrite = true)
        receivedKeys.deleteOnExit()
    }

    "Dependencies should not be in the `dependencies` package" {
        getArtifactNameToConstantMapping().forEach {
            if (it.constantName.startsWith("dependencies.")) {
                fail("This dependency should not be in the dependencies package: ${it.constantName}")
            }
            it.constantName.startsWith("dependencies.").shouldBeFalse()
        }
    }

})

