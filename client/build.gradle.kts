plugins {
    kotlin("js")
}

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
}

dependencies {
    testImplementation(kotlin("test-js"))
    val ver = "pre.154-kotlin-1.5.0"
    implementation(platform(kotlin("stdlib")))
    implementation(npm("react", ">15.0.0 <=19"))
    implementation(npm("react-dom", ">15.0.0 <=19"))
    implementation("org.jetbrains:kotlin-extensions:1.0.1-$ver")
    implementation("org.jetbrains:kotlin-styled:5.2.3-$ver")
    implementation("org.jetbrains:kotlin-react:17.0.2-$ver")
    implementation("org.jetbrains:kotlin-react-dom:17.0.2-$ver")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-$ver")
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