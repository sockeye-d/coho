plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains:markdown:0.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(project(":core"))
}

kotlin {
    jvmToolchain(21)
}
