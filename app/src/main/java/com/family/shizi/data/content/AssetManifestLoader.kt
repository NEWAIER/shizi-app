package com.family.shizi.data.content

import android.content.Context
import kotlinx.serialization.json.Json

object AssetManifestLoader {
    const val MANIFEST_ASSET_PATH = "content/v1/manifest.json"
    private val json = Json { ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }

    fun decode(value: String): AssetManifest = json.decodeFromString(value)

    fun load(context: Context): AssetManifest =
        context.assets.open(MANIFEST_ASSET_PATH).bufferedReader(Charsets.UTF_8).use { decode(it.readText()) }
}
