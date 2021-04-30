plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.5.0"
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

val logback_classic_version: String by project
val ktor_version: String by project
val kotlin_version: String by project
val kotlinx_serialization_version: String by project
val xmlutil_version: String by project
val woodstox_version: String by project
val groovy_version: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.rsug:karlutka:+")

    implementation("org.apache.groovy:groovy:$groovy_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$kotlinx_serialization_version")

}

