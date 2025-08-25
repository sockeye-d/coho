plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":core"))
    implementation(project(":markdown"))
    implementation(project(":server"))
}

kotlin {
    jvmToolchain(21)
}
