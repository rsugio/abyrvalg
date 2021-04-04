val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.0-M2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0-M2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jetbrains.dokka") version "1.4.30"
}

group = "io.rsug"
version = "0.0.1-build1"

application {
    mainClass.set("ApplicationKt")
    mainClassName = "ApplicationKt"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.1.0")

    implementation("net.devrieze:xmlutil-jvm:0.81.1")
    implementation("net.devrieze:xmlutil-serialization-jvm:0.81.1")
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:6+")


    // клиентское логирование
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
//    implementation("io.ktor:ktor-client-serialization:$ktor_version")
//    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("org.apache.groovy:groovy:4.0.0-alpha-2")
    implementation("org.apache.groovy:groovy-xml:4.0.0-alpha-2")

    //sourceArtifacts(files("C:/workspace/Karlutka/build/libs/karlutka-0.0.1-build3-sources.jar"))
    implementation(files("C:/workspace/rsug.io/karlutka/build/libs/karlutka-0.0.1-build4.jar"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks.shadowJar {
    //minimize()        -- не работает
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}
