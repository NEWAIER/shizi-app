package com.family.shizi.ui.home.components.assets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.R
import com.family.shizi.ui.home.components.CaterpillarState

@Composable
fun CaterpillarArtwork(
    state: CaterpillarState,
    facingLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val bob = rememberInfiniteTransition(label = "caterpillar_artwork_bob").animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "caterpillar_artwork_bob_offset",
    ).value
    Image(
        painter = painterResource(R.drawable.caterpillar_mascot_main),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .testTag("growth_map_caterpillar")
            .graphicsLayer {
                translationY = bob.dp.toPx()
                scaleX = if (facingLeft) -1f else 1f
                alpha = if (state == CaterpillarState.CHALLENGE) .98f else 1f
            },
    )
}
