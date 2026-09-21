package jsonexporter

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

/**
 * Shared OTLP/JSON export transport — the single "plugin" both platforms use to send telemetry.
 *
 * Why this module exists: the native OTel SDKs (opentelemetry-android / opentelemetry-swift) can
 * only export protobuf, but the backend gateway accepts `application/json` only. Each platform must
 * still own the serialize step (it consumes SDK-specific in-memory span/log/metric types — and on
 * iOS that half is Swift), but everything downstream of "we have OTLP/JSON bytes" — the POST, the
 * timeout, the `application/json` content type, and the never-throw contract — is identical and
 * lives here once instead of being duplicated in Kotlin (`OtlpJsonExporters`) and Swift
 * (`SwiftOtlpJsonExporters`).
 *
 * Contract: [send] is blocking and NEVER throws. The native SDKs invoke exporters on their batch
 * worker threads (never the main thread), so blocking here is safe.
 *
 * Note (Android): this uses Ktor-OkHttp, not the SDK's auto-instrumented client. The project does
 * not apply the byte-buddy weaving plugin, so opentelemetry-android does NOT auto-instrument OkHttp
 * — there is no risk of export requests being traced and feeding back into telemetry.
 */
object OtlpJsonTransport {

    const val CONTENT_TYPE_JSON: String = "application/json"
    const val TIMEOUT_MS: Long = 15_000

    private val client: HttpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MS
                connectTimeoutMillis = TIMEOUT_MS
                socketTimeoutMillis = TIMEOUT_MS
            }
        }
    }

    /** POST [body] as JSON to [endpoint]. Returns true on 2xx. Never throws. */
    fun send(endpoint: String, headers: Map<String, String>, body: String): Boolean {
        return try {
            runBlocking {
                val response: HttpResponse = client.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    headers.forEach { (k, v) -> header(k, v) }
                    setBody(body)
                }
                response.status.value in 200..299
            }
        } catch (_: Throwable) {
            false
        }
    }
}
