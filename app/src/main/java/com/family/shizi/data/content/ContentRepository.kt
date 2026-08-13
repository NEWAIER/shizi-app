package com.family.shizi.data.content

import android.content.Context
import com.family.shizi.data.db.LearningSessionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ContentCatalog(
    val schemaVersion: Int,
    val activePackId: String,
    val packs: List<ContentPackDescriptor>,
)

@Serializable
data class ContentPackDescriptor(
    val packId: String,
    val version: String,
    val contentPath: String,
    val manifestPath: String,
    val assetRoot: String,
    val legacyCompatibleVersions: List<String> = emptyList(),
)

data class LoadedContent(
    val descriptor: ContentPackDescriptor,
    val content: ContentPackage,
) {
    fun assetPath(relativePath: String): String = "${descriptor.assetRoot.trimEnd('/')}/$relativePath"
}

/** Pure resolver so catalog compatibility can be verified on the JVM without Android assets. */
object ContentCatalogResolver {
    fun resolve(catalog: ContentCatalog, packId: String, version: String? = null): ContentPackDescriptor? =
        catalog.packs.firstOrNull {
            it.packId == packId && (version == null || it.version == version || version in it.legacyCompatibleVersions)
        }

    fun resolveVersion(catalog: ContentCatalog, version: String): ContentPackDescriptor? =
        catalog.packs.firstOrNull { it.version == version || version in it.legacyCompatibleVersions }

    fun versionConflicts(catalog: ContentCatalog): List<String> {
        val seen = mutableMapOf<String, String>()
        val conflicts = mutableListOf<String>()
        catalog.packs.forEach { pack ->
            (listOf(pack.version) + pack.legacyCompatibleVersions).forEach { version ->
                val previous = seen.putIfAbsent(version, pack.packId)
                if (previous != null) conflicts += "$version resolves to both $previous and ${pack.packId}"
            }
        }
        return conflicts
    }
}

/**
 * Process-wide, offline content registry. It caches a loaded pack by (packId, version),
 * while legacy V1 records resolve through catalog legacyCompatibleVersions.
 */
class ContentRepository private constructor(private val appContext: Context) {
    private val cache = mutableMapOf<Pair<String, String>, LoadedContent>()
    private val json = Json { ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }
    private val catalog: ContentCatalog by lazy {
        appContext.assets.open(CATALOG_PATH).bufferedReader(Charsets.UTF_8).use { json.decodeFromString<ContentCatalog>(it.readText()) }
            .also { require(ContentCatalogResolver.versionConflicts(it).isEmpty()) { "Content catalog has ambiguous versions" } }
    }

    @Synchronized fun active(): LoadedContent = load(catalog.activePackId, null)

    @Synchronized fun load(packId: String, version: String? = null): LoadedContent {
        val descriptor = ContentCatalogResolver.resolve(catalog, packId, version)
            ?: error("Unknown content package: $packId@$version")
        val key = descriptor.packId to descriptor.version
        return cache.getOrPut(key) {
            val decoded = appContext.assets.open(descriptor.contentPath).bufferedReader(Charsets.UTF_8).use { ContentLoader.decode(it.readText()) }
            require(decoded.contentVersion == descriptor.version || decoded.contentVersion in descriptor.legacyCompatibleVersions) {
                "Catalog/content version mismatch for ${descriptor.packId}"
            }
            LoadedContent(descriptor, decoded)
        }
    }

    fun loadForSession(session: LearningSessionEntity): LoadedContent =
        ContentCatalogResolver.resolveVersion(catalog, session.contentVersion)
            ?.let { load(it.packId, session.contentVersion) }
            ?: active()

    fun assetPathFor(contentVersion: String, relativePath: String): String =
        ContentCatalogResolver.resolveVersion(catalog, contentVersion)
            ?.let { "${it.assetRoot.trimEnd('/')}/$relativePath" }
            ?: "$LEGACY_ASSET_ROOT/$relativePath"

    companion object {
        const val CATALOG_PATH = "content/catalog.json"
        const val LEGACY_PACK_ID = "legacy-five-v1"
        const val LEGACY_ASSET_ROOT = "content/v1"
        @Volatile private var instance: ContentRepository? = null
        fun get(context: Context): ContentRepository = instance ?: synchronized(this) {
            instance ?: ContentRepository(context.applicationContext).also { instance = it }
        }
        fun clearForTest() { synchronized(this) { instance = null } }
    }
}
