package com.family.shizi.content

import com.family.shizi.data.content.AssetByteSource
import com.family.shizi.data.content.AssetManifestLoader
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.G2ResourceValidator
import com.family.shizi.domain.engine.GrowthMapModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 校验 active pack（child-pack-v1）的 G2 资源完整性，
 * 覆盖新增 UI 反馈音频（audio/ui 目录）进入资源校验体系。
 */
class ChildPackResourceValidationTest {
    private val packRoot = File("src/main/assets/content/packs/child-pack-v1/1.0.0")
    private val content by lazy {
        ContentLoader.decode(packRoot.resolve("content.json").readText(Charsets.UTF_8))
    }
    private val manifest by lazy {
        AssetManifestLoader.decode(packRoot.resolve("manifest.json").readText(Charsets.UTF_8))
    }
    private val realFiles by lazy {
        manifest.resources.associate { it.path to packRoot.resolve(it.path).readBytes() }
    }

    @Test fun activePackPassesG2IncludingUiAudio() {
        val result = G2ResourceValidator.validate(content, manifest, AssetByteSource { realFiles[it] })
        assertTrue(result.errors.joinToString("\n"), result.isValid)
    }

    @Test fun uiAudioRegisteredInContentAndManifest() {
        assertEquals(7, content.uiAudio.size)
        assertTrue(content.uiAudio.all { it.startsWith("audio/ui/") && it.endsWith(".mp3") })
        val manifestUiPaths = manifest.resources.filter { it.path.startsWith("audio/ui/") }.map { it.path }.toSet()
        assertEquals(content.uiAudio.toSet(), manifestUiPaths)
        // G2 的引用收集必须包含 UI 音频，否则 required 资源会被判为 orphan
        val references = com.family.shizi.data.content.G2ResourceValidator.collectReferences(content)
        assertTrue(content.uiAudio.all { it in references })
    }

    @Test fun manifestCoversEveryRequiredCharacterAsset() {
        val characterAssets = content.characters.flatMap { character ->
            listOf(
                character.imageAsset,
                character.audio.character,
                character.audio.meaning,
                character.sentence.audioAsset,
            ) + character.words.map { it.audioAsset }
        }
        val manifestPaths = manifest.resources.map { it.path }.toSet()
        assertTrue(characterAssets.all { it in manifestPaths })
        assertEquals(50, content.characters.size)
        assertEquals(5, GrowthMapModel.chapterCount(content.characters.size))
    }
}
