package com.family.shizi.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSchemaTest {
    private val mapper = ObjectMapper()
    private val schema by lazy {
        val node = mapper.readTree(TestContent.schemaFile)
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(node)
    }

    @Test fun frozenContentPassesExecutableSchema() {
        val errors = schema.validate(mapper.readTree(TestContent.contentFile))
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test fun unknownStructureAndBlankStringFailSchema() {
        val unknown = mapper.readTree(TestContent.json().replaceFirst("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"unknown\": true,"))
        val blank = mapper.readTree(TestContent.json().replaceFirst("\"contentVersion\": \"1.0.0\"", "\"contentVersion\": \"\""))
        assertTrue(schema.validate(unknown).isNotEmpty())
        assertTrue(schema.validate(blank).isNotEmpty())
    }


    @Test fun optionKindStrictlyBindsAssetType() {
        val mutations = listOf(
            "images/options/image_person_v1.webp" to "audio/options/wrong.mp3",
            "audio/characters/char_ren_v1.mp3" to "images/options/wrong.webp",
            "images/options/context_dashan_v1.webp" to "audio/options/wrong.mp3",
        )
        mutations.forEach { (valid, invalid) ->
            val mutated = mapper.readTree(TestContent.json().replaceFirst(valid, invalid))
            assertTrue("Schema accepted invalid asset mutation $valid -> $invalid", schema.validate(mutated).isNotEmpty())
        }
    }

    @Test fun promptAudioAndImageAssetRejectWrongMediaType() {
        val prompt = mapper.readTree(
            TestContent.json().replaceFirst("audio/prompts/prompt_find_ren_v1.mp3", "images/prompts/wrong.webp"),
        )
        val image = mapper.readTree(
            TestContent.json().replaceFirst("images/characters/char_ren_main_v1.webp", "audio/characters/wrong.mp3"),
        )
        assertTrue(schema.validate(prompt).isNotEmpty())
        assertTrue(schema.validate(image).isNotEmpty())
    }
}
