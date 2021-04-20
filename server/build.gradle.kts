
plugins {
    id("application")
    id("distribution")
    kotlin("jvm") version "1.5.0-RC"
}
val ktor_version: String by project

group = "io.rsug.abyrvalg"
version = "0.0.1-build124"

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

application {
    mainClass.set("io.ktor.server.cio.EngineMain")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.31-kotlin-1.2.41")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

sourceSets {
    main {
        java.srcDir("src/main")
        resources.srcDir("resources")
    }
}
