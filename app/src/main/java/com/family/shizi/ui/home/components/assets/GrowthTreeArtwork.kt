package com.family.shizi.ui.home.components.assets

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.family.shizi.R

/** 完整连续成长树：只绘制一张完整树图，不再拼接或循环树干。 */
@Composable
fun GrowthTreeArtwork(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.growth_tree_main),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}
