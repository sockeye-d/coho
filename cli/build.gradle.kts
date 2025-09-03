val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
    application
    id("com.gradleup.shadow") version "9.0.2"
}

dependencies {
    implementation(libs.stdlib)
    implementation(libs.reflect)
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
    // minimize {
    //     libs.bundles.kts.map { include(dependency(it)) }
    // }
    minimize {
        include(dependency(libs.reflect))
    }
}
