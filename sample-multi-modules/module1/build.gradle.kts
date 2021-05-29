plugins {
    java
    kotlin("jvm") version "1.4-M2"
    id("org.gradle.hello-world")
}

version = "unspecified"

repositories {
    mavenCentral()
}

useDependency()

dependencies {
    implementation(kotlin("stdlib"))
    implementation(Square.okHttp3.okHttp)
    testImplementation("junit", "junit", "4.12")
    implementation("com.github.javadev:okio:1.6.0")
}
