import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // No version: the Kotlin Gradle plugin is already on the build classpath (the root project
    // applies kotlin.multiplatform), so requesting a version here would conflict.
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.mavenPublish)
}

// Published as the Gradle plugin `io.github.codedtx.jsonexporter`. Shares the repo version so the
// plugin always pulls the matching json_exporter library version.
group = "io.github.codedtx"
version = System.getenv("VERSION") ?: "0.1.0-SNAPSHOT"

// Target Java 8 bytecode (broad Gradle compatibility) using whatever JDK runs the build — no
// separate toolchain required.
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    // Needed to reference KotlinMultiplatformExtension when wiring the library into commonMain.
    compileOnly(libs.kotlin.gradle.plugin)
}

// Generate a constant with this build's version so the plugin adds the matching library version.
val versionDir = layout.buildDirectory.dir("generated/version/kotlin")
val generateVersion by tasks.registering {
    val v = version.toString()
    inputs.property("version", v)
    outputs.dir(versionDir)
    doLast {
        val pkg = versionDir.get().asFile.resolve("io/github/codedtx/gradle")
        pkg.mkdirs()
        pkg.resolve("JsonExporterVersion.kt").writeText(
            """
            package io.github.codedtx.gradle

            // AUTO-GENERATED at build time. Do not edit.
            internal object JsonExporterVersion {
                const val LIBRARY: String = "$v"
            }
            """.trimIndent() + "\n"
        )
    }
}
kotlin.sourceSets.getByName("main").kotlin.srcDir(generateVersion)

gradlePlugin {
    plugins {
        create("jsonexporter") {
            id = "io.github.codedtx.jsonexporter"
            implementationClass = "io.github.codedtx.gradle.JsonExporterPlugin"
            displayName = "json_exporter telemetry"
            description = "Adds the io.github.codedtx:json_exporter OTLP/JSON library to a Kotlin Multiplatform project."
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    coordinates("io.github.codedtx", "json_exporter-gradle-plugin", version.toString())
    pom {
        name.set("json_exporter-gradle-plugin")
        description.set("Gradle convention plugin that wires the json_exporter OTLP/JSON library into a KMP project.")
        inceptionYear.set("2026")
        url.set("https://github.com/CodeDTX/otel-json-exporter")
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
            url.set("https://github.com/CodeDTX/otel-json-exporter")
            connection.set("scm:git:git://github.com/CodeDTX/otel-json-exporter.git")
            developerConnection.set("scm:git:ssh://git@github.com/CodeDTX/otel-json-exporter.git")
        }
    }
}
