@file:Suppress("PackageDirectoryMismatch", "SpellCheckingInspection", "unused")

import de.fayard.refreshVersions.core.internal.DependencyGroup
import org.gradle.api.Incubating

@Incubating
/**
 * Build and deploy beautiful documentation sites that grow with you.
 *
 * Official website: [orchid.run](https://orchid.run/)
 *
 * [Documentation](https://orchid.run/wiki/user-manual/getting-started)
 *
 * [Quick-Start](https://orchid.run/wiki/user-manual/getting-started/quickstart)
 *
 * [GitHub releases](https://github.com/orchidhq/Orchid/releases)
 *
 * GitHub page: [orchidhq/Orchid](https://github.com/orchidhq/Orchid)
 */
object Orchid : DependencyGroup(
    group = "io.github.javaeden.orchid",
    rawRule = """
        io.github.javaeden.orchid:*
                           ^^^^^^
    """.trimIndent()
) {

    private const val groupId = "io.github.javaeden.orchid"

    val core = module("OrchidCore")
    val test = module("OrchidTest")

    /**
     * Official webpage: [orchid.run/plugins](https://orchid.run/plugins)
     */
    val plugins = Plugins

    object Plugins : DependencyGroup(group = "io.github.javaeden.orchid") {
        val changelog = module("OrchidChangelog")
        val forms = module("OrchidForms")
        val groovydoc = module("OrchidGroovydoc")
        val javadoc = module("OrchidJavadoc")
        val kotlindoc = module("OrchidKotlindoc")
        val kss = module("OrchidKSS")
        val netlifyCMS = module("OrchidNetlifyCMS")
        val pages = module("OrchidPages")
        val pluginDocs = module("OrchidPluginDocs")
        val posts = module("OrchidPosts")
        val presentations = module("OrchidPresentations")
        val search = module("OrchidSearch")
        val sourceDoc = module("OrchidSourceDoc")
        val swagger = module("OrchidSwagger")
        val swiftdoc = module("OrchidSwiftdoc")
        val taxonomies = module("OrchidTaxonomies")
        val wiki = module("OrchidWiki")
        val asciidoc = module("OrchidAsciidoc")
        val bible = module("OrchidBible")
        val diagrams = module("OrchidDiagrams")
        val syntaxHighlighter = module("OrchidSyntaxHighlighter")
        val writersBlocks = module("OrchidWritersBlocks")
        val azure = module("OrchidAzure")
        val bitbucket = module("OrchidBitbucket")
        val github = module("OrchidGithub")
        val gitlab = module("OrchidGitlab")
        val netlify = module("OrchidNetlify")
    }

    /**
     * Official webpage: [orchid.run/themes](https://orchid.run/themes)
     */
    val themes = Themes

    object Themes : DependencyGroup(group = "io.github.javaeden.orchid") {
        val bsDoc = module("OrchidBsDoc")
        val copper = module("OrchidCopper")
        val editorial = module("OrchidEditorial")
        val futureImperfect = module("OrchidFutureImperfect")
    }

    val bundles = Bundles

    object Bundles : DependencyGroup(group = "io.github.javaeden.orchid") {
        val all = module("OrchidAll")
        val blog = module("OrchidBlog")
        val docs = module("OrchidDocs")
        val languagePack = module("OrchidLanguagePack")
    }
}
