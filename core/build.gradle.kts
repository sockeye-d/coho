val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
}

dependencies {
    implementation(libs.stdlib)
    implementation(libs.bundles.kts)
    implementation(libs.markdown)
    implementation(libs.ktx.yaml)
    implementation(libs.ktx.coroutines.core)
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
}
