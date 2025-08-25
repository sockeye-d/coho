plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains:markdown:0.7.3")
}

kotlin {
    jvmToolchain(21)
}
