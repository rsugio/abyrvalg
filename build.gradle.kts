val ktor_version: String by project
val kotlin_version = "1.5.0-M2"
val logback_version: String by project

plugins {
    idea
    application
    kotlin("jvm") version "1.5+"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5+"
//    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jetbrains.dokka") version "1.4.30"
    id("maven-publish")
//    id ("com.palantir.git-version") version "0.12.3"
    id("fr.brouillard.oss.gradle.jgitver") version "0.6.1"
}

group = "io.rsug"
//version = "0.0.X" //gitVersion()

jgitver {
    // Your config goes here
}

application {
    mainClass.set("ApplicationKt")
}

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5+")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.1.0")

    implementation("net.devrieze:xmlutil-jvm:+")//0.81.1
    implementation("net.devrieze:xmlutil-serialization-jvm:+")//0.81.1
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:+") // 6.2.4

    // разбор форматов АПИ
    implementation("io.rsug:karlutka:+") //0.0.1-build7

    // поиск
    // не понравилось - много лишнего из implementation("com.github.jillesvangurp:es-kotlin-client:1.0.9")
//    implementation("net.paslavsky:ktor-elastic:+") //1.3.2-1

    // клиентское логирование
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
//    implementation("io.ktor:ktor-client-serialization:$ktor_version")
//    implementation("io.ktor:ktor-client-gson:$ktor_version")

    // нынешний груви модулен
    implementation("org.apache.groovy:groovy:4.0.0-alpha-2")
//    implementation("org.apache.groovy:groovy-xml:4.0.0-alpha-2")
//    implementation("org.apache.groovy:groovy-json:4.0.0-alpha-2")

    testImplementation(kotlin("test-junit"))
}

//kotlin.sourceSets["main"].kotlin.srcDirs("src")
//kotlin.sourceSets["test"].kotlin.srcDirs("test")
//
//sourceSets["main"].resources.srcDirs("resources")
//sourceSets["test"].resources.srcDirs("testresources")

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