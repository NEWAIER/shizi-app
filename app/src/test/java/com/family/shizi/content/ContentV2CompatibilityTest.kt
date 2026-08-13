package com.family.shizi.content

import com.family.shizi.data.content.CharacterContent
import com.family.shizi.data.content.ContentCatalog
import com.family.shizi.data.content.ContentCatalogResolver
import com.family.shizi.data.content.ContentErrorCode
import com.family.shizi.data.content.ContentValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentV2CompatibilityTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test fun legacyFivePackIsRegisteredAndOriginalContentStillLoads() {
        val catalog = catalog()
        val pack = ContentCatalogResolver.resolve(catalog, "legacy-five-v1", "1.0.0")
        assertNotNull(pack)
        assertEquals("content/v1/content.json", pack!!.contentPath)

        val content = TestContent.packageData()
        assertEquals(listOf("char_ren", "char_kou", "char_da", "char_xiao", "char_shan"), content.learningOrder)
        assertEquals("1.0.0", content.contentVersion)
        assertTrue(content.characters.all { it.readings.isEmpty() })
    }

    @Test fun catalogAndPackMetadataPassTheirV2Schemas() {
        val mapper = ObjectMapper()
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        val catalogSchema = factory.getSchema(mapper.readTree(File("src/main/assets/content/catalog.schema.json")))
        val packSchema = factory.getSchema(mapper.readTree(File("src/main/assets/content/packs/pack.schema.json")))
        assertTrue(catalogSchema.validate(mapper.readTree(File("src/main/assets/content/catalog.json"))).isEmpty())
        assertTrue(
            packSchema.validate(
                mapper.readTree(File("src/main/assets/content/packs/legacy-five-v1/1.0.0/pack.json")),
            ).isEmpty(),
        )
    }

    @Test fun legacySessionVersionResolvesToTheSameHistoricalPack() {
        val pack = ContentCatalogResolver.resolveVersion(catalog(), "1.0.0")
        assertNotNull(pack)
        assertEquals("legacy-five-v1", pack!!.packId)
    }

    @Test fun sixthCharacterDoesNotRequireKotlinBusinessLogicChanges() {
        val original = TestContent.packageData()
        val source = original.characters.last()
        val sixth: CharacterContent = source.copy(
            id = "char_test_six",
            character = "测",
            pinyin = "cè",
            order = 6,
            questionSeeds = source.questionSeeds.mapIndexed { index, seed -> seed.copy(id = "test_six_${index}_${seed.id}") },
        )
        val sixCharacterContent = original.copy(
            learningOrder = original.learningOrder + sixth.id,
            characters = original.characters + sixth,
        )
        val validation = ContentValidator.validate(sixCharacterContent)
        assertTrue(validation.errors.joinToString("\n"), validation.isValid)
        assertFalse(validation.errors.any { it.code == ContentErrorCode.CONTENT_REACHABILITY_FAILED })
    }

    @Test fun courseControlsThresholdAndBadgesWithoutLibrarySizeCap() {
        val course = TestContent.packageData().course
        assertEquals(3, course.stageTestThreshold)
        assertEquals(listOf(1, 3, 5), course.badgeMilestones.map { it.learnedCount })
    }

    private fun catalog(): ContentCatalog = json.decodeFromString(
        File("src/main/assets/content/catalog.json").readText(Charsets.UTF_8),
    )
}
