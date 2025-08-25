plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("scripting-jsr223"))
}

kotlin {
    jvmToolchain(21)
}
