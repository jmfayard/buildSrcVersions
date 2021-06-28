package de.fayard.refreshVersions.rules

import de.fayard.refreshVersions.core.internal.ArtifactVersionKeyReader
import de.fayard.refreshVersions.core.internal.DependencyGroup
import dependencies.ALL_DEPENDENCIES_NOTATIONS
import org.junit.jupiter.api.*
import testutils.junit.dynamicTest
import java.io.File

@Suppress("UnstableApiUsage")
class BundledRulesTest {

    private val mainResources: File = File(".").absoluteFile.resolve("src/main/resources")
    private val rulesDir = mainResources.resolve("refreshVersions-rules")
    private val versionKeyReader = ArtifactVersionKeyReader.fromRules(rulesDir.listFiles()!!.map { it.readText() })

    @TestFactory
    fun `test bundled version key rules against dependencies constants`(): List<DynamicTest> {
        return bundledRules.map { (expectedKey, moduleIdentifiers) ->
            dynamicTest(displayName = expectedKey) {
                moduleIdentifiers.forEach { moduleIdentifier ->
                    Assertions.assertEquals(
                        expectedKey,
                        versionKeyReader.readVersionKey(moduleIdentifier.group, moduleIdentifier.name)
                    )
                }
            }
        }
    }

    @Test
    fun `generate rules file for dependency groups`() {
        ALL_DEPENDENCIES_NOTATIONS
        val file = rulesDir.resolve("dependency-groups-alias-rules.txt")
        val content = DependencyGroup.ALL_RULES
            .sorted()
            .distinct()
            .joinToString(separator = "\n\n")
        file.writeText(content)
    }

    @Test
    @Disabled("Not implemented yet")
    fun `check all dependencies constants have version key rules`() {
        TODO()
    }
}
