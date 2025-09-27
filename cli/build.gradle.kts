val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
    application
    id("com.gradleup.shadow") version "9.0.2"
}

dependencies {
    implementation(libs.kt.stdlib)
    implementation(libs.kt.reflect)
    implementation(libs.ktx.cli)
    implementation(libs.bundles.ktor)

    implementation(project(":core"))
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

application {
    mainClass = "dev.fishies.coho.cli.MainKt"
    applicationName = "coho"
}

tasks.shadowJar {
    // minimize {
    //     libs.bundles.kts.map { include(dependency(it)) }
    // }
    minimize {
        include(dependency(libs.kt.reflect))
    }
}

sourceSets.main {
    resources {
        exclude("template/build")
    }
}