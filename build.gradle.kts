plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}

tasks.register("testEngine") {
    description = "Runs the unit tests of the included engine build."
    dependsOn(gradle.includedBuild("engine").task(":test"))
}
