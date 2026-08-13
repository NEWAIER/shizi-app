package com.family.shizi.data.content

import kotlinx.serialization.Serializable

@Serializable
data class AssetManifest(
    val manifestVersion: Int,
    val resources: List<AssetManifestEntry>,
)

@Serializable
data class AssetManifestEntry(
    val path: String,
    val sha256: String,
    val bytes: Long,
    val required: Boolean,
)
