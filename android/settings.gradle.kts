pluginManagement {
    val flutterSdkPath =
        run {
            val properties = java.util.Properties()
            file("local.properties").inputStream().use { properties.load(it) }
            val flutterSdkPath = properties.getProperty("flutter.sdk")
            require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
            flutterSdkPath
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    // 🟢 ACTUALIZADO: AGP a 8.7.2 (Estable y compatible con SDK 36)
    id("com.android.application") version "8.7.2" apply false
    // 🟢 ACTUALIZADO: Kotlin a 2.0.20 (Recomendado para Flutter actual)
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}

include(":app")