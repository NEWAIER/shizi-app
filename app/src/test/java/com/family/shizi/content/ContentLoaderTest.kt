package com.family.shizi.content

import com.family.shizi.data.content.ContentErrorCode
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentLoaderTest {
    @Test fun parsesFrozenPackageCounts() {
        val content = TestContent.packageData()
        assertEquals(1, content.schemaVersion)
        assertEquals("1.0.0", content.contentVersion)
        assertEquals(5, content.characters.size)
        assertEquals(17, content.optionCatalog.size)
        assertEquals(22, content.characters.sumOf { it.questionSeeds.size })
        assertEquals(listOf("char_ren", "char_kou", "char_da", "char_xiao", "char_shan"), content.learningOrder)
    }

    @Test fun missingRequiredFieldFailsStrictDecode() = assertStructureFailure(
        TestContent.json().replaceFirst("\"schemaVersion\": 1,", ""),
    )

    @Test fun wrongTypeFailsStrictDecode() = assertStructureFailure(
        TestContent.json().replaceFirst("\"schemaVersion\": 1", "\"schemaVersion\": true"),
    )

    @Test fun unknownEnumFailsStrictDecode() = assertStructureFailure(
        TestContent.json().replaceFirst("\"kind\": \"TEXT\"", "\"kind\": \"VIDEO\""),
    )

    @Test fun unknownPropertyFailsStrictDecode() = assertStructureFailure(
        TestContent.json().replaceFirst("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"unknownRequired\": true,"),
    )

    private fun assertStructureFailure(json: String) {
        val error = assertThrows(ContentValidationException::class.java) { ContentLoader.decode(json) }
        assertEquals(ContentErrorCode.CONTENT_STRUCTURE_INVALID, error.validationError.code)
    }
}
