val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
    application
    id("com.gradleup.shadow") version "9.0.2"
}

dependencies {
    implementation(libs.stdlib)
    implementation(libs.jsr233.kt)
    implementation(libs.bundles.ktor)
    implementation(libs.ktx.cli)

    implementation(project(":core"))
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
}

application {
    mainClass = "dev.fishies.coho.cli.MainKt"
    applicationName = "coho"
}

tasks.shadowJar {
    minimize {
        include(dependency(libs.jsr233.kt))
    }
}
