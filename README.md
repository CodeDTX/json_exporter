# otel-json-exporter

A tiny **Kotlin Multiplatform** (Android + iOS) helper for sending **OTLP/JSON** telemetry.

> Published as `io.github.codedtx:json_exporter` (library) and `io.github.codedtx.jsonexporter` (Gradle plugin).

Most OpenTelemetry HTTP exporters (opentelemetry-java, opentelemetry-swift) speak **protobuf only**.
When your collector/gateway accepts `application/json` instead, you still need to *send* the bytes
and, on iOS, fix the trace/span id encoding. This library owns exactly that shared step — the POST
and the id normalisation — so it isn't duplicated in Kotlin and Swift.

It has **no OpenTelemetry SDK dependency** — only Ktor, kotlinx.serialization, and coroutines. Each
platform keeps its own serialize step (it consumes SDK-specific span/log/metric types); this library
takes the resulting OTLP/JSON string and delivers it.

## API

```kotlin
package jsonexporter

object OtlpJsonTransport {
    /** POST [body] as application/json to [endpoint]. Blocking, never throws. Returns true on 2xx. */
    fun send(endpoint: String, headers: Map<String, String>, body: String): Boolean
}

/** Rewrite base64 traceId/spanId/parentSpanId in [json] to the lowercase hex OTLP/JSON requires. */
fun fixOtlpJsonIds(json: String): String
```

- `send` blocks and never throws — call it from a background/export worker thread, not the main thread.
- `fixOtlpJsonIds` is needed when your serializer emits proto3-canonical JSON (base64 byte fields),
  e.g. swift-protobuf. It's a safe no-op on already-hex ids.

## Install

Both options resolve from **Maven Central** (`mavenCentral()` is usually already in your repos).

### Option A — Gradle plugin (recommended)

The plugin adds the library to `commonMain` for you.

`settings.gradle.kts` — the plugin resolves from `pluginManagement`, and the library it adds
resolves from `dependencyResolutionManagement`, so both need `mavenCentral()`:

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Module `build.gradle.kts` (KMP):

```kotlin
plugins {
    id("io.github.codedtx.jsonexporter") version "1.1.1"
}
```

### Option B — plain dependency

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Module `build.gradle.kts` (KMP):

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.codedtx:json_exporter:1.1.1")
        }
    }
}
```

## Usage

```kotlin
import jsonexporter.OtlpJsonTransport
import jsonexporter.fixOtlpJsonIds

// 1. Serialize your spans/logs/metrics to an OTLP/JSON string with your platform SDK.
val json = fixOtlpJsonIds(serializedOtlpJson)   // no-op on Android/hex; fixes base64 on iOS

// 2. Send it.
val ok = OtlpJsonTransport.send(
    endpoint = "https://your-gateway/api/v1/telemetry/traces",
    headers = emptyMap(),
    body = json,
)
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
