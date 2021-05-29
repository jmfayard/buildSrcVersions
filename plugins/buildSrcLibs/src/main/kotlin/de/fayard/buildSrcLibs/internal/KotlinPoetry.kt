package de.fayard.buildSrcLibs.internal

import com.squareup.kotlinpoet.*
import de.fayard.buildSrcLibs.internal.Case.*




internal fun kotlinpoet(
    deps: Deps
): FileSpec {
    val libraries: List<Library> = deps.libraries
    val indent = "    "

    val libsProperties: List<PropertySpec> = libraries
        .distinctBy { it.groupModule() }
        .map { d ->
            val libValue = when {
                d.version == "none" -> CodeBlock.of("%S", d.groupModule())
                else -> CodeBlock.of("%S", d.groupModuleUnderscore())
            }
            constStringProperty(
                name = deps.names[d]!!,
                initializer = libValue,
                kdoc = null
            )
        }

    val libsTypeSpec = TypeSpec.objectBuilder("Libs")
        .addKdoc(PluginConfig.KDOC_LIBS)
        .addProperties(libsProperties)
        .build()


    return FileSpec.builder("", "Libs")
        .indent(indent)
        .addType(libsTypeSpec)
        .build()

}



internal fun constStringProperty(
    name: String,
    initializer: CodeBlock,
    kdoc: CodeBlock? = null
): PropertySpec = PropertySpec.builder(name, String::class)
    .addModifiers(KModifier.CONST)
    .initializer(initializer)
    .apply {
        if (kdoc != null) addKdoc(kdoc)
    }.build()
