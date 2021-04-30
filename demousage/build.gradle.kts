plugins {
    groovy
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

val groovy_version: String by project
dependencies {
    implementation(project(":lib"))
    implementation("org.apache.groovy:groovy:$groovy_version")
    testImplementation("junit:junit:4+")
}

application {
    mainClass.set("DemoGroovy")
}