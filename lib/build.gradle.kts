plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    id("maven-publish")
}

group = "io.rsug"
version = "0.0.1-build9"

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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$kotlinx_serialization_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    requireNotNull(property("gpr.user"))
    requireNotNull(property("gpr.key"))
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rsugio/abyrvalg")
            credentials {
                username = property("gpr.user") as String
                password = property("gpr.key") as String
            }
        }
    }
    publications {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group as String
                artifactId = "abyrvalg"
                version = project.version as String

                from(components["java"])
            }
        }
    }
}
