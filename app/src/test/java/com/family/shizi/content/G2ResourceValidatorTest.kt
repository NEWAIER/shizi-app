package com.family.shizi.content

import com.family.shizi.data.content.AssetByteSource
import com.family.shizi.data.content.AssetManifest
import com.family.shizi.data.content.AssetManifestEntry
import com.family.shizi.data.content.AssetManifestLoader
import com.family.shizi.data.content.ContentPackage
import com.family.shizi.data.content.ContentValidator
import com.family.shizi.data.content.G2ResourceErrorCode
import com.family.shizi.data.content.G2ResourceValidator
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class G2ResourceValidatorTest {
    private val root by lazy { File("src/main/assets/content/v1") }
    private val content by lazy { TestContent.packageData() }
    private val reviewed by lazy { content.copy(characters = content.characters.map {
        it.copy(contentReview = it.contentReview.copy(assetReviewedByDeveloper = true, assetReviewedByParent = true))
    }) }
    private val manifest by lazy { AssetManifestLoader.decode(File(root, "manifest.json").readText(Charsets.UTF_8)) }
    private val realFiles by lazy { manifest.resources.associate { it.path to File(root, it.path).readBytes() } }

    @Test fun completeFiftyResourcesPassG2() {
        val result = validate(reviewed)
        assertTrue(result.errors.joinToString("\n"), result.isValid)
        assertTrue(result.childTrialEnabled)
        assertTrue(ContentValidator.validate(content).isValid)
    }

    @Test fun missingMountainAudioFailsG2ButNotG1() {
        val files = realFiles - "audio/characters/char_shan_v1.mp3"
        val result = validate(reviewed, files = files)
        assertTrue(result.has(G2ResourceErrorCode.RESOURCE_FILE_MISSING))
        assertFalse(result.childTrialEnabled)
        assertTrue(ContentValidator.validate(content).isValid)
    }

    @Test fun emptyFileFails() {
        val path = manifest.resources.first().path
        assertHas(files = realFiles + (path to byteArrayOf()), code = G2ResourceErrorCode.RESOURCE_FILE_EMPTY)
    }

    @Test fun tamperedFileFailsHashAndLeavesG1Independent() {
        val path = "images/options/image_person_v1.webp"
        val changed = realFiles.getValue(path).clone().also { it[it.lastIndex] = (it.last() + 1).toByte() }
        assertHas(files = realFiles + (path to changed), code = G2ResourceErrorCode.RESOURCE_SHA256_MISMATCH)
        assertTrue(ContentValidator.validate(content).isValid)
    }

    @Test fun forgedBytesFails() {
        val changed = manifest.copy(resources = manifest.resources.mapIndexed { index, entry -> if (index == 0) entry.copy(bytes = entry.bytes + 1) else entry })
        assertHas(manifest = changed, code = G2ResourceErrorCode.RESOURCE_BYTES_MISMATCH)
    }

    @Test fun forgedShaFails() {
        val changed = manifest.copy(resources = manifest.resources.mapIndexed { index, entry -> if (index == 0) entry.copy(sha256 = "0".repeat(64)) else entry })
        assertHas(manifest = changed, code = G2ResourceErrorCode.RESOURCE_SHA256_MISMATCH)
    }

    @Test fun missingManifestReferenceFails() {
        val changed = manifest.copy(resources = manifest.resources.drop(1))
        assertHas(manifest = changed, code = G2ResourceErrorCode.RESOURCE_MANIFEST_REFERENCE_MISSING)
    }

    @Test fun requiredOrphanFails() {
        val bytes = byteArrayOf(1, 2, 3)
        val orphan = AssetManifestEntry("audio/orphan.mp3", sha(bytes), bytes.size.toLong(), true)
        assertHas(manifest = manifest.copy(resources = manifest.resources + orphan), files = realFiles + (orphan.path to bytes), code = G2ResourceErrorCode.RESOURCE_MANIFEST_ORPHAN)
    }

    @Test fun duplicateManifestPathFails() {
        assertHas(manifest = manifest.copy(resources = manifest.resources + manifest.resources.first()), code = G2ResourceErrorCode.RESOURCE_MANIFEST_DUPLICATE)
    }

    @Test fun wrongImageFormatFails() {
        val path = "images/options/image_person_v1.webp"
        val bytes = "not a webp".encodeToByteArray()
        assertHas(manifest = replaceFacts(path, bytes), files = realFiles + (path to bytes), code = G2ResourceErrorCode.RESOURCE_IMAGE_FORMAT_INVALID)
    }

    @Test fun wrongImageDimensionFails() {
        val path = "images/options/image_person_v1.webp"
        val bytes = realFiles.getValue(path).clone()
        val marker = bytes.indices.first { index -> index + 4 <= bytes.size && bytes.copyOfRange(index, index + 4).decodeToString() == "VP8 " }
        val data = marker + 8
        bytes[data + 6] = 0x00; bytes[data + 7] = 0x02
        assertHas(manifest = replaceFacts(path, bytes), files = realFiles + (path to bytes), code = G2ResourceErrorCode.RESOURCE_IMAGE_DIMENSION_INVALID)
    }

    @Test fun wrongAudioFormatFails() {
        val path = "audio/characters/char_ren_v1.mp3"
        val bytes = "not mp3".encodeToByteArray()
        assertHas(manifest = replaceFacts(path, bytes), files = realFiles + (path to bytes), code = G2ResourceErrorCode.RESOURCE_AUDIO_FORMAT_INVALID)
    }

    @Test fun wrongAudioDurationFails() {
        val path = "audio/characters/char_ren_v1.mp3"
        val bytes = realFiles.getValue("audio/meanings/meaning_shan_v1.mp3")
        assertHas(manifest = replaceFacts(path, bytes), files = realFiles + (path to bytes), code = G2ResourceErrorCode.RESOURCE_AUDIO_DURATION_INVALID)
    }

    @Test fun incompleteDeveloperOrParentReviewFails() {
        val developerFalse = reviewed.copy(characters = reviewed.characters.mapIndexed { i, c -> if (i == 0) c.copy(contentReview = c.contentReview.copy(assetReviewedByDeveloper = false)) else c })
        val parentFalse = reviewed.copy(characters = reviewed.characters.mapIndexed { i, c -> if (i == 0) c.copy(contentReview = c.contentReview.copy(assetReviewedByParent = false)) else c })
        assertTrue(validate(developerFalse).has(G2ResourceErrorCode.CONTENT_ASSET_REVIEW_INCOMPLETE))
        assertTrue(validate(parentFalse).has(G2ResourceErrorCode.CONTENT_ASSET_REVIEW_INCOMPLETE))
        assertFalse(validate(parentFalse).childTrialEnabled)
    }

    @Test fun bigAndSmallUseSharedNonLeakingPrompt() {
        val target = content.characters.filter { it.id in setOf("char_da", "char_xiao") }
            .flatMap { it.questionSeeds }
            .filter { it.type.name == "CHARACTER_CHOOSE_IMAGE" }
        assertTrue(target.size == 2)
        assertTrue(target.all { it.promptAudio == "audio/prompts/prompt_choose_picture_v1.mp3" })
    }

    private fun validate(value: ContentPackage, valueManifest: AssetManifest = manifest, files: Map<String, ByteArray> = realFiles) =
        G2ResourceValidator.validate(value, valueManifest, AssetByteSource { files[it] })

    private fun assertHas(
        manifest: AssetManifest = this.manifest,
        files: Map<String, ByteArray> = realFiles,
        code: G2ResourceErrorCode,
    ) {
        val result = validate(reviewed, manifest, files)
        assertTrue("Expected $code, got ${result.errors}", result.has(code))
        assertFalse(result.childTrialEnabled)
    }

    private fun replaceFacts(path: String, bytes: ByteArray) = manifest.copy(resources = manifest.resources.map {
        if (it.path == path) it.copy(bytes = bytes.size.toLong(), sha256 = sha(bytes)) else it
    })

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
