import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublish)
}

// Published to Maven Central as `io.github.codedtx:json_exporter:<version>`.
// The version comes from the pushed Git tag (CI sets VERSION); falls back to a SNAPSHOT locally.
group = "io.github.codedtx"
version = System.getenv("VERSION") ?: "0.1.0-SNAPSHOT"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Ktor — single shared HTTP transport for OTLP/JSON export.
            implementation(libs.ktor.client.core)
            // Serialization — used for the OTLP/JSON id hex-fix.
            implementation(libs.kotlinx.serialization.json)
            // Coroutines — runBlocking wrapper so a background worker can call the suspend POST.
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "jsonexporter"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    // Upload to the Central Portal and auto-release once validation passes.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    // Sign all publications (required by Maven Central). Key + password come from CI env
    // (ORG_GRADLE_PROJECT_signingInMemoryKey / ...KeyPassword).
    signAllPublications()

    coordinates("io.github.codedtx", "json_exporter", version.toString())

    pom {
        name.set("json_exporter")
        description.set(
            "Kotlin Multiplatform (Android + iOS) helper for sending OTLP/JSON telemetry: " +
                "a shared HTTP transport plus trace/span id hex normalisation."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/CodeDTX/json_exporter")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("codedtx")
                name.set("CodeDTX")
                url.set("https://github.com/CodeDTX")
            }
        }
        scm {
            url.set("https://github.com/CodeDTX/json_exporter")
            connection.set("scm:git:git://github.com/CodeDTX/json_exporter.git")
            developerConnection.set("scm:git:ssh://git@github.com/CodeDTX/json_exporter.git")
        }
    }
}
