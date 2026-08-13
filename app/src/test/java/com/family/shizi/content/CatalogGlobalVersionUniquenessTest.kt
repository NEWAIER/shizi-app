package com.family.shizi.content

import com.family.shizi.data.content.ContentCatalog
import com.family.shizi.data.content.ContentCatalogResolver
import com.family.shizi.data.content.ContentPackDescriptor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogGlobalVersionUniquenessTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test fun legacyVersionResolvesUniquely() {
        val catalog = json.decodeFromString<ContentCatalog>(File("src/main/assets/content/catalog.json").readText())
        assertTrue(ContentCatalogResolver.versionConflicts(catalog).isEmpty())
        assertEquals("legacy-five-v1", ContentCatalogResolver.resolveVersion(catalog, "1.0.0")?.packId)
    }

    @Test fun duplicateVersionOrAliasIsRejected() {
        val catalog = ContentCatalog(
            schemaVersion = 2,
            activePackId = "one",
            packs = listOf(
                ContentPackDescriptor("one", "1.0.0", "a", "b", "c"),
                ContentPackDescriptor("two", "2.0.0", "d", "e", "f", listOf("1.0.0")),
            ),
        )
        assertTrue(ContentCatalogResolver.versionConflicts(catalog).isNotEmpty())
    }
}
