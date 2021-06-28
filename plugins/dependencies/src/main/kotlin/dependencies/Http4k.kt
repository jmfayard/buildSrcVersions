import de.fayard.refreshVersions.core.internal.DependencyGroup

private val GROUP = "org.http4k"

object Http4k : DependencyGroup(
    group = GROUP,
    usePlatformConstraints = true,
    rawRule = """
        org.http4k:http4k-*
            ^^^^^^
    """.trimIndent()
) {
    val bom = "org.http4k:http4k-bom:_"

    val aws = module("http4k-aws")
    val cloudnative = module("http4k-cloudnative")
    val contract = module("http4k-contract")
    val core = module("http4k-core")
    val graphql = module("http4k-graphql")
    val incubator = module("http4k-incubator")
    val jsonrpc = module("http4k-jsonrpc")
    val metricsMicrometer = module("http4k-metrics-micrometer")
    val multipart = module("http4k-multipart")
    val opentelemetry = module("http4k-opentelemetry")
    val realtimeCore = module("http4k-realtime-core")
    val resilience4j = module("http4k-resilience4j")
    val securityOauth = module("http4k-security-oauth")

    val client = Client

    object Client : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val apache = module("http4k-client-apache")
        val apacheAsync = module("http4k-client-apache-async")
        val apache4 = module("http4k-client-apache4")
        val apache4Async = module("http4k-client-apache4-async")
        val jetty = module("http4k-client-jetty")
        val okhttp = module("http4k-client-okhttp")
        val websocket = module("http4k-client-websocket")
    }

    val format = Format

    object Format : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val argo = module("http4k-format-argo")
        val core = module("http4k-format-core")
        val gson = module("http4k-format-gson")
        val jackson = module("http4k-format-jackson")
        val jacksonXml = module("http4k-format-jackson-xml")
        val jacksonYaml = module("http4k-format-jackson-yaml")
        val klaxon = module("http4k-format-klaxon")
        val kotlinxSerialization = module("http4k-format-kotlinx-serialization")
        val moshi = module("http4k-format-moshi")
        val xml = module("http4k-format-xml")
    }


    val server = Server

    object Server : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val apache = module("http4k-server-apache")
        val apache4 = module("http4k-server-apache4")
        val jetty = module("http4k-server-jetty")
        val ktorcio = module("http4k-server-ktorcio")
        val ktornetty = module("http4k-server-ktornetty")
        val netty = module("http4k-server-netty")
        val ratpack = module("http4k-server-ratpack")
        val undertow = module("http4k-server-undertow")
    }

    val serverless = Serverless

    object Serverless : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val alibaba = module("http4k-serverless-alibaba")
        val azure = module("http4k-serverless-azure")
        val gcf = module("http4k-serverless-gcf")
        val lambda = module("http4k-serverless-lambda")
        val lambdaRuntime = module("http4k-serverless-lambda-runtime")
        val openwhisk = module("http4k-serverless-openwhisk")
        val tencent = module("http4k-serverless-tencent")
    }

    val template = Template

    object Template : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val core = module("http4k-template-core")
        val dust = module("http4k-template-dust")
        val freemarker = module("http4k-template-freemarker")
        val handlebars = module("http4k-template-handlebars")
        val jade4j = module("http4k-template-jade4j")
        val pebble = module("http4k-template-pebble")
        val thymeleaf = module("http4k-template-thymeleaf")
    }

    val testing = Testing

    object Testing : DependencyGroup(GROUP, usePlatformConstraints = true) {
        val approval = module("http4k-testing-approval")
        val chaos = module("http4k-testing-chaos")
        val hamkrest = module("http4k-testing-hamkrest")
        val kotest = module("http4k-testing-kotest")
        val servirtium = module("http4k-testing-servirtium")
        val strikt = module("http4k-testing-strikt")
        val webdriver = module("http4k-testing-webdriver")
    }
}

