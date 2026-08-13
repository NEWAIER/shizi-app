package com.family.shizi.data.content

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object ContentLoader {
    const val CONTENT_ASSET_PATH = "content/v1/content.json"
    const val SCHEMA_ASSET_PATH = "content/v1/content.schema.json"

    private val strictJson = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
        explicitNulls = true
    }

    fun decode(json: String): ContentPackage = try {
        strictJson.decodeFromString<ContentPackage>(json)
    } catch (error: SerializationException) {
        throw ContentValidationException(
            ContentValidationError(
                ContentErrorCode.CONTENT_STRUCTURE_INVALID,
                "$",
                error.message ?: "Strict JSON decoding failed",
            ),
            error,
        )
    }

    fun load(context: Context): ContentPackage =
        context.assets.open(CONTENT_ASSET_PATH).bufferedReader(Charsets.UTF_8).use {
            decode(it.readText())
        }
}
