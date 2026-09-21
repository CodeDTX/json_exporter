package io.github.codedtx.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle convention plugin `io.github.codedtx.jsonexporter`.
 *
 * Applying it to a Kotlin Multiplatform project adds the matching `io.github.codedtx:json_exporter`
 * runtime library to `commonMain` — so consumers write one `plugins { }` line instead of the
 * `implementation(...)` dependency. The plugin is build-time glue; the actual OTLP/JSON transport
 * code lives in the library it pulls in.
 *
 * Requires `mavenCentral()` in the consuming project's dependency repositories.
 */
class JsonExporterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kotlin = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.sourceSets.getByName("commonMain").dependencies {
                implementation("io.github.codedtx:json_exporter:${JsonExporterVersion.LIBRARY}")
            }
        }
    }
}
