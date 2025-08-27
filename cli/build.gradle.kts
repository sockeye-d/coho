plugins {
    kotlin("jvm") apply true
    application
    //distribution
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("com.gradleup.shadow") version "9.0.2"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("scripting-jsr223"))
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("io.ktor:ktor-server-websockets:3.2.3")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    implementation(project(":core"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "dev.fishies.coho.cli.MainKt"
    applicationName = "coho"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName = "coho"
            mainClass = "dev.fishies.coho.cli.MainKt"
            //buildArgs.add("-O3")
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(21)
                vendor = JvmVendorSpec.GRAAL_VM
            }
        }
    }
}

//tasks.shadowJar {
//    minimize()
//}