// The engine is a standalone Kotlin/JVM build. The Android app includes it via
// includeBuild("engine"), but it can also be built and tested on its own without
// any Android SDK: ./gradlew -p engine test
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "engine"
