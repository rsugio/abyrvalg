plugins {
    idea
    application
    kotlin("jvm") version "1.5.0-RC"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0-RC"
    id("org.jetbrains.dokka") version "1.4.30"
    id("maven-publish")
//    id("fr.brouillard.oss.gradle.jgitver") version "0.6.1"
}

group = "io.rsug"
version = "0.0.1-build124" //gitVersion()

//jgitver {
//    // Your config goes here
//}

application {
    mainClass.set("ApplicationKt")
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://jitpack.io") }
    maven{
        requireNotNull(property("gpr.user"))
        requireNotNull(property("gpr.key"))
        url = uri("https://maven.pkg.github.com/pdvrieze/xmlutil")
        credentials {
            username = property("gpr.user") as String
            password = property("gpr.key") as String
        }
        content {
            includeGroup("io.github.pdvrieze.xmlutil")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.1.0")

    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.81.2")
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:6.2.5") // 6.2.4

    // разбор форматов АПИ
    implementation("io.rsug:karlutka:+") //0.0.1-build7

    // клиентское логирование
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-client-logging:1.5.3")
    implementation("io.ktor:ktor-client-core:1.5.3")
    implementation("io.ktor:ktor-client-core-jvm:1.5.3")
    implementation("io.ktor:ktor-client-cio:1.5.3")

    // нынешний груви модулен
    implementation("org.apache.groovy:groovy:4.0.0-alpha-3")
    testImplementation(kotlin("test-junit"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
sourceSets["main"].resources.srcDirs("resources")

kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["test"].resources.srcDirs("testresources")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.test {
    useJUnit()
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
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
