import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
    kotlin("kapt")
    id("org.jetbrains.dokka") version "2.0.0"
}

dependencies {
    implementation(libs.kt.stdlib)
    implementation(libs.kt.reflect)
    implementation(libs.ktx.yaml)
    implementation(libs.ktx.coroutines.core)
    implementation(libs.bundles.kts)
    implementation(libs.markdown)
    implementation(libs.prism4j)
    implementation(libs.commonstext)
    kapt(libs.prism4j.bundler)
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.dokkaGeneratePublicationHtml {
    outputDirectory = project.rootProject.layout.buildDirectory.dir("docs")
}

tasks.withType<DokkaTask>().configureEach {
    moduleName = "coho"
    dokkaSourceSets.configureEach {
        sourceLink {
            remoteUrl = uri("https://github.com/sockeye-d/coho").toURL()
            remoteLineSuffix ="#L"
        }
    }
}