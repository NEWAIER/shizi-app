package com.family.shizi.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.R

/** Minimal HUD on the map home: avatar, stars and gallery entry. */
@Composable
fun ChildHud(
    stars: Int,
    onOpenProfile: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Image(
            painter = painterResource(R.drawable.xiaohe_launcher),
            contentDescription = "打开我的星球",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(54.dp).clip(CircleShape).clickable(onClick = onOpenProfile).testTag("home_profile_avatar"),
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp).clickable(onClick = onOpenGallery).testTag("home_gallery_entry"),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(30.dp)) {
                    drawRoundRect(Color(0xFFF8E7B5), topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width * .42f, size.height * .72f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    drawRoundRect(Color(0xFFFFF5D7), topLeft = androidx.compose.ui.geometry.Offset(size.width * .5f, 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width * .42f, size.height * .72f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    drawLine(Color(0xFFD89B62), androidx.compose.ui.geometry.Offset(size.width / 2f, 5.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * .78f), strokeWidth = 2.dp.toPx())
                }
            }
            Box(modifier = Modifier.padding(start = 2.dp).size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(26.dp)) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val path = androidx.compose.ui.graphics.Path()
                    repeat(10) { index ->
                        val angle = (-90 + index * 36) * Math.PI / 180.0
                        val radius = if (index % 2 == 0) size.minDimension / 2f else size.minDimension / 5f
                        val point = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius)
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    drawPath(path, Color(0xFFFFC857))
                }
                androidx.compose.material3.Text("$stars", modifier = Modifier.padding(start = 26.dp), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            }
        }
    }
}