package com.family.shizi.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.R
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.components.ChildPrimaryButton
import com.family.shizi.ui.components.MascotBubble
import com.family.shizi.ui.theme.ShiziShapes

/** 首页欢迎区：头像、问候语、今日任务卡与主按钮。 */
@Composable
fun HomeHero(
    todayCharacter: String,
    todayPinyin: String,
    message: String,
    primaryAction: String,
    canStart: Boolean,
    error: String?,
    dueReviewCount: Int,
    onPrimary: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.xiaohe_launcher),
                contentDescription = "小禾头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(58.dp).clip(CircleShape).clickable { onOpenProfile() }.testTag("home_avatar_menu"),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("欢迎回来", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("小朋友，今天认识$todayCharacter 宝宝吗？", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("今天认识一个新朋友", modifier = Modifier.padding(top = 18.dp).testTag("page_home"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("$todayCharacter 宝宝正在等你打招呼", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        error?.let { Text(it, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            shape = ShiziShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(todayCharacter, modifier = Modifier.testTag("home_today_character"), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Text(todayPinyin, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.titleMedium)
                MascotBubble("你好呀！我是$todayCharacter 宝宝", modifier = Modifier.padding(top = 12.dp))
                if (dueReviewCount > 0) {
                    Text("还有 $dueReviewCount 个老朋友想见你", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium)
                }
                ChildPrimaryButton(
                    text = if (canStart) "开始和它认识" else primaryAction,
                    enabled = canStart,
                    modifier = Modifier.padding(top = 16.dp).testTag("home_primary"),
                    onClick = onPrimary,
                )
            }
        }
        Text("$message", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** 首页底部进度摘要。 */
@Composable
fun HomeProgressSummary(
    learnedCount: Int,
    dailyCompletedCount: Int = 0,
    totalStars: Int,
    dailyTarget: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text("今日进度 $dailyCompletedCount/${dailyTarget}", style = MaterialTheme.typography.labelLarge)
        Text("成长星星 $totalStars", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
