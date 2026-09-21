package jsonexporter

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * OTLP/JSON id normalisation.
 *
 * The canonical proto3 → JSON mapping (what swift-protobuf's `jsonUTF8Data()` emits) encodes
 * `bytes` fields — `traceId`, `spanId`, `parentSpanId` — as **base64**. The OTLP/JSON spec deviates
 * from that and requires them as **lowercase hex**; the collector rejects/mis-parses base64 ids.
 *
 * iOS needs this because swift-protobuf produces base64. Android does NOT (opentelemetry-java's
 * marshaler already writes hex) — but calling this on already-hex ids is a safe no-op, so the
 * function is platform-agnostic.
 */
private val otlpIdKeys: Set<String> = setOf("traceId", "spanId", "parentSpanId")

/** Rewrite base64 trace/span ids in [json] to lowercase hex. Returns [json] unchanged on failure. */
fun fixOtlpJsonIds(json: String): String {
    return try {
        val root = Json.parseToJsonElement(json)
        Json.encodeToString(JsonElement.serializer(), fixElement(root))
    } catch (_: Throwable) {
        json
    }
}

private fun fixElement(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.mapValues { (key, value) ->
            if (key in otlpIdKeys && value is JsonPrimitive && value.isString) {
                base64ToHex(value.content)?.let { JsonPrimitive(it) } ?: fixElement(value)
            } else {
                fixElement(value)
            }
        }
    )
    is JsonArray -> JsonArray(element.map { fixElement(it) })
    else -> element
}

@OptIn(ExperimentalEncodingApi::class)
private fun base64ToHex(value: String): String? {
    return try {
        Base64.decode(value).joinToString("") { byte ->
            val v = byte.toInt() and 0xFF
            val hi = HEX[v ushr 4]
            val lo = HEX[v and 0x0F]
            "$hi$lo"
        }
    } catch (_: Throwable) {
        null
    }
}

private const val HEX = "0123456789abcdef"
