package com.family.shizi.data.content

import kotlinx.serialization.Serializable

/** Machine-oriented foundation data. It is never shown directly as child copy. */
@Serializable
data class BaseCharacterLibrary(
    val schemaVersion: Int,
    val sourceId: String,
    val sourceSnapshot: String,
    val characters: List<BaseCharacterRecord>,
)

@Serializable
data class BaseCharacterRecord(
    val id: String,
    val character: String,
    val unicode: String,
    val pinyin: String,
    val tone: Int,
    val strokeCount: Int,
    val radical: String? = null,
    val frequency: Int? = null,
)
