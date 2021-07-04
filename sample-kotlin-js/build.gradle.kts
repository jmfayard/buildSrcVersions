plugins {
    kotlin("multiplatform")
}

group = "de.fayard"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js() {
        browser {
            testTask {
                useMocha()
            }
        }
        useCommonJs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Kotlin.stdlib)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(Kotlin.test.common)
                implementation(Kotlin.test.annotationsCommon)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(npm("decamelize", "_", generateExternals = true))

                implementation(npm("leaflet", "_"))
                implementation(npm("leaflet-imageoverlay-rotated", "_"))

                implementation(npm("bootstrap-icons", "_"))

                implementation(npm("css-loader", "_"))
                implementation(npm("css-loader", "_"))
                implementation(npm("style-loader", "_"))
                implementation(npm("file-loader", "_"))

                implementation(npm("@googlemaps/js-api-loader", "_"))

                implementation(npm("debug", "_"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(Kotlin.test.js)
                implementation(Testing.Kotest.assertions.core)
            }
        }
    }

}

tasks.register<DefaultTask>("hello") {
    group = "Custom"
    description = "Minimal task that do nothing. Useful to debug a failing build"
}
