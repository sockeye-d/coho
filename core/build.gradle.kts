val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(libs.stdlib)
    implementation(libs.jsr233.kt)
    implementation(libs.markdown)
    implementation(libs.ktx.json)
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
}
