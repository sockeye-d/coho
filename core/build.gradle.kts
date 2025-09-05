val jvmTarget: String by project

plugins {
    kotlin("jvm") apply true
    kotlin("kapt")
}

dependencies {
    implementation(libs.stdlib)
    implementation(libs.reflect)
    implementation(libs.bundles.kts)
    implementation(libs.markdown)
    implementation(libs.ktx.yaml)
    implementation(libs.ktx.coroutines.core)
    implementation(libs.prism4j)
    implementation(libs.commonstext)
    kapt(libs.prism4j.bundler)
}

kotlin {
    jvmToolchain(jvmTarget.toInt())
}
