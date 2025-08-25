plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
}

kotlin {
    jvmToolchain(21)
}
