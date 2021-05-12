plugins {
    kotlin("js")
}

repositories {
//    jcenter()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

val jswrapper_version: String by project

dependencies {
    testImplementation(kotlin("test-js"))
    implementation(platform(kotlin("stdlib")))
    implementation(npm("react", ">15.0.0 <=19"))
    implementation(npm("react-dom", ">15.0.0 <=19"))
    implementation("org.jetbrains:kotlin-extensions:1.0.1-$jswrapper_version")
    implementation("org.jetbrains:kotlin-styled:5.3.0-$jswrapper_version")
    implementation("org.jetbrains:kotlin-react:17.0.2-$jswrapper_version")
    implementation("org.jetbrains:kotlin-react-dom:17.0.2-$jswrapper_version")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-$jswrapper_version")
}
kotlin {
    js(LEGACY) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}