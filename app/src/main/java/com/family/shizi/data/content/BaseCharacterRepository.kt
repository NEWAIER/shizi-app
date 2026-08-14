package com.family.shizi.data.content

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Reads reusable foundation data independently from the child learning pack.
 * The repository intentionally exposes no child-facing copy or media fields.
 */
class BaseCharacterRepository private constructor(private val library: BaseCharacterLibrary) {
    fun all(): List<BaseCharacterRecord> = library.characters

    fun find(characterId: String): BaseCharacterRecord? = library.characters.firstOrNull { it.id == characterId }

    companion object {
        private val json = Json { ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }

        fun fromJson(raw: String): BaseCharacterRepository {
            val library = json.decodeFromString<BaseCharacterLibrary>(raw)
            validate(library).takeIf { it.isNotEmpty() }?.let { errors ->
                throw IllegalArgumentException("Invalid foundation library: ${errors.joinToString("; ")}")
            }
            return BaseCharacterRepository(library)
        }

        fun load(context: Context, assetPath: String = DEFAULT_ASSET_PATH): BaseCharacterRepository =
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { fromJson(it.readText()) }

        internal fun validate(library: BaseCharacterLibrary): List<String> = buildList {
            if (library.schemaVersion < 1) add("schemaVersion must be positive")
            if (library.sourceId.isBlank()) add("sourceId must not be blank")
            if (library.sourceSnapshot.isBlank()) add("sourceSnapshot must not be blank")
            val ids = mutableSetOf<String>()
            library.characters.forEachIndexed { index, record ->
                if (!ids.add(record.id)) add("duplicate id at index $index: ${record.id}")
                if (!record.id.matches(Regex("char_u[0-9a-f]+"))) add("invalid id: ${record.id}")
                if (record.character.codePointCount(0, record.character.length) != 1) add("character must be one code point: ${record.id}")
                val expectedUnicode = "U+%04X".format(record.character.codePointAt(0))
                if (record.unicode != expectedUnicode) add("unicode mismatch: ${record.id}")
                if (record.pinyin.isBlank()) add("pinyin is blank: ${record.id}")
                if (record.tone !in 1..4) add("tone out of range: ${record.id}")
                if (record.strokeCount < 1) add("strokeCount out of range: ${record.id}")
                if (record.frequency != null && record.frequency < 1) add("frequency out of range: ${record.id}")
            }
        }

        const val DEFAULT_ASSET_PATH = "content/foundation/v1/characters.json"
    }
}
