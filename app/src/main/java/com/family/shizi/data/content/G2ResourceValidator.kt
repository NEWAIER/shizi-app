package com.family.shizi.data.content

import android.content.Context
import java.security.MessageDigest

fun interface AssetByteSource {
    fun read(path: String): ByteArray?
}

class AndroidAssetByteSource(private val context: Context, private val assetRoot: String = ContentRepository.get(context).active().descriptor.assetRoot) : AssetByteSource {
    override fun read(path: String): ByteArray? = try {
        context.assets.open("${assetRoot.trimEnd('/')}/$path").use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}

object G2ResourceValidator {
    fun validate(content: ContentPackage, manifest: AssetManifest, source: AssetByteSource): G2ResourceValidationResult {
        val errors = mutableListOf<G2ResourceValidationError>()
        fun error(code: G2ResourceErrorCode, path: String, message: String) {
            errors += G2ResourceValidationError(code, path, message)
        }

        val references = collectReferences(content)
        val duplicatePaths = manifest.resources.groupingBy { it.path }.eachCount().filterValues { it > 1 }.keys
        duplicatePaths.forEach { error(G2ResourceErrorCode.RESOURCE_MANIFEST_DUPLICATE, it, "Duplicate manifest path") }
        val entries = manifest.resources.associateBy { it.path }

        (references - entries.keys).forEach {
            error(G2ResourceErrorCode.RESOURCE_MANIFEST_REFERENCE_MISSING, it, "Content reference absent from manifest")
        }
        manifest.resources.filter { it.required && it.path !in references }.forEach {
            error(G2ResourceErrorCode.RESOURCE_MANIFEST_ORPHAN, it.path, "Required resource is not referenced")
        }

        manifest.resources.filter { it.required }.distinctBy { it.path }.forEach { entry ->
            val bytes = source.read(entry.path)
            if (bytes == null) {
                error(G2ResourceErrorCode.RESOURCE_FILE_MISSING, entry.path, "Required file missing")
                return@forEach
            }
            if (bytes.isEmpty()) {
                error(G2ResourceErrorCode.RESOURCE_FILE_EMPTY, entry.path, "Required file empty")
                return@forEach
            }
            if (bytes.size.toLong() != entry.bytes) {
                error(G2ResourceErrorCode.RESOURCE_BYTES_MISMATCH, entry.path, "Expected ${entry.bytes}, actual ${bytes.size}")
            }
            val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            if (actualHash != entry.sha256) {
                error(G2ResourceErrorCode.RESOURCE_SHA256_MISMATCH, entry.path, "SHA-256 mismatch")
            }
            when {
                entry.path.startsWith("images/") -> validateWebP(entry.path, bytes, ::error)
                entry.path.startsWith("audio/") -> validateMp3(entry.path, bytes, ::error)
            }
        }

        content.characters.forEachIndexed { index, character ->
            if (!character.contentReview.assetReviewedByDeveloper || !character.contentReview.assetReviewedByParent) {
                error(G2ResourceErrorCode.CONTENT_ASSET_REVIEW_INCOMPLETE, "characters[$index].contentReview", "Developer and parent review are required")
            }
        }
        return G2ResourceValidationResult(errors)
    }

    internal fun collectReferences(content: ContentPackage): Set<String> = buildSet {
        content.optionCatalog.mapNotNullTo(this) { it.asset }
        content.characters.forEach { character ->
            add(character.imageAsset)
            character.words.mapTo(this) { it.audioAsset }
            add(character.sentence.audioAsset)
            add(character.audio.character)
            add(character.audio.meaning)
            character.questionSeeds.mapTo(this) { it.promptAudio }
        }
    }

    private fun validateWebP(path: String, bytes: ByteArray, error: (G2ResourceErrorCode, String, String) -> Unit) {
        val isWebP = bytes.size >= 16 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" &&
            bytes.copyOfRange(8, 12).decodeToString() == "WEBP"
        if (!isWebP) {
            error(G2ResourceErrorCode.RESOURCE_IMAGE_FORMAT_INVALID, path, "Not a RIFF WebP")
            return
        }
        val dimensions = WebPInspector.dimensions(bytes)
        if (dimensions != Pair(1024, 1024)) {
            error(G2ResourceErrorCode.RESOURCE_IMAGE_DIMENSION_INVALID, path, "Expected 1024x1024, actual $dimensions")
        }
    }

    private fun validateMp3(path: String, bytes: ByteArray, error: (G2ResourceErrorCode, String, String) -> Unit) {
        val facts = Mp3Inspector.inspect(bytes)
        if (facts == null || facts.sampleRate != 44_100 || facts.channels != 1 || facts.bitrateKbps != 96) {
            error(G2ResourceErrorCode.RESOURCE_AUDIO_FORMAT_INVALID, path, "Expected MP3 44.1kHz mono CBR 96kbps, actual $facts")
            return
        }
        val maxDuration = when {
            path.startsWith("audio/characters/") -> 1.5
            path.startsWith("audio/words/") -> 2.0
            path.startsWith("audio/sentences/") || path.startsWith("audio/prompts/") -> 4.0
            else -> Double.MAX_VALUE
        }
        val minDuration = if (path.startsWith("audio/characters/")) 0.5 else 0.0
        if (facts.durationSeconds < minDuration || facts.durationSeconds > maxDuration) {
            error(G2ResourceErrorCode.RESOURCE_AUDIO_DURATION_INVALID, path, "Duration ${facts.durationSeconds}s outside $minDuration..$maxDuration")
        }
    }
}

private object WebPInspector {
    fun dimensions(bytes: ByteArray): Pair<Int, Int>? {
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val type = bytes.copyOfRange(offset, offset + 4).decodeToString()
            val size = le32(bytes, offset + 4)
            val data = offset + 8
            if (type == "VP8 " && data + 10 <= bytes.size && bytes[data + 3] == 0x9d.toByte() && bytes[data + 4] == 0x01.toByte() && bytes[data + 5] == 0x2a.toByte()) {
                return Pair(le16(bytes, data + 6) and 0x3fff, le16(bytes, data + 8) and 0x3fff)
            }
            if (type == "VP8X" && data + 10 <= bytes.size) {
                val width = 1 + (bytes[data + 4].u() or (bytes[data + 5].u() shl 8) or (bytes[data + 6].u() shl 16))
                val height = 1 + (bytes[data + 7].u() or (bytes[data + 8].u() shl 8) or (bytes[data + 9].u() shl 16))
                return Pair(width, height)
            }
            offset = data + size + (size and 1)
        }
        return null
    }
    private fun le16(b: ByteArray, o: Int) = b[o].u() or (b[o + 1].u() shl 8)
    private fun le32(b: ByteArray, o: Int) = le16(b, o) or (le16(b, o + 2) shl 16)
}

internal data class Mp3Facts(val sampleRate: Int, val channels: Int, val bitrateKbps: Int, val durationSeconds: Double)

private object Mp3Inspector {
    private val bitrates = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
    private val rates = intArrayOf(44_100, 48_000, 32_000, 0)
    fun inspect(bytes: ByteArray): Mp3Facts? {
        var offset = if (bytes.size >= 10 && bytes.copyOfRange(0, 3).decodeToString() == "ID3") {
            10 + ((bytes[6].u() and 0x7f) shl 21) + ((bytes[7].u() and 0x7f) shl 14) + ((bytes[8].u() and 0x7f) shl 7) + (bytes[9].u() and 0x7f)
        } else 0
        while (offset + 4 <= bytes.size && !(bytes[offset].u() == 0xff && bytes[offset + 1].u() and 0xe0 == 0xe0)) offset++
        if (offset + 4 > bytes.size) return null
        val h1 = bytes[offset + 1].u(); val h2 = bytes[offset + 2].u(); val h3 = bytes[offset + 3].u()
        if ((h1 shr 3 and 3) != 3 || (h1 shr 1 and 3) != 1) return null
        val bitrate = bitrates[h2 shr 4 and 0xf]
        val rate = rates[h2 shr 2 and 3]
        if (bitrate == 0 || rate == 0) return null
        val channels = if (h3 shr 6 == 3) 1 else 2
        val duration = (bytes.size - offset) * 8.0 / (bitrate * 1000.0)
        return Mp3Facts(rate, channels, bitrate, duration)
    }
}

private fun Byte.u(): Int = toInt() and 0xff
