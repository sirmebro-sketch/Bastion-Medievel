import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.roborazzi)
}

val baseVersionName = providers.gradleProperty("bastion.versionName").get()
// CI passes a build number for the visible version name and a version code that grows
// across all workflows (minutes since 2026), so every new APK installs over the old one.
val buildNumber = providers.gradleProperty("bastion.buildNumber").orNull?.toInt() ?: 1
val versionCodeValue = providers.gradleProperty("bastion.versionCode").orNull?.toInt() ?: 1

// Beta builds (branches, pull requests, or main while the release key is not set up)
// get their own app id and signing key, so they install next to the real app.
val isBeta = providers.gradleProperty("bastion.beta").orNull?.toBoolean() ?: true

android {
    namespace = "de.bastion.medieval"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = if (isBeta) "de.bastion.medieval.beta" else "de.bastion.medieval"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = versionCodeValue
        versionName = "$baseVersionName ($buildNumber)" + if (isBeta) " beta" else ""
        manifestPlaceholders["appLabel"] = if (isBeta) "Bastion Beta" else "Bastion Medieval"
    }

    signingConfigs {
        create("release") {
            // Set by the release workflow from repository secrets; never stored in git.
            System.getenv("BASTION_KEYSTORE_PATH")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("BASTION_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BASTION_KEY_ALIAS")
                keyPassword = System.getenv("BASTION_KEY_PASSWORD")
            }
        }
        create("beta") {
            // A public test key: beta builds are for testing only and never published.
            storeFile = rootProject.file("signing/beta.keystore")
            storePassword = "bastion-beta"
            keyAlias = "beta"
            keyPassword = "bastion-beta"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (isBeta) "beta" else "release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("de.bastion.medieval:engine:1.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
