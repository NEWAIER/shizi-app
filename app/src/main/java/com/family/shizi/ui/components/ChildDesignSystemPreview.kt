package com.family.shizi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.family.shizi.ui.theme.ShiziTheme

/**
 * Central visual QA surface for PR-FE-01. This is intentionally not a product
 * route: it gives designers and engineers one place to inspect every public
 * child component state without touching learning data.
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
fun ChildDesignSystemPreview() {
    ShiziTheme {
        var selected by remember { mutableStateOf(false) }
        LazyColumn(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("字宝宝星球组件预览", style = MaterialTheme.typography.headlineMedium) }
            item { MascotBubble("欢迎来到字宝宝星球！") }
            item { CharacterHeroCard("人", "rén", onReplay = {}) }
            item { ProgressDots(total = 3, current = 1) }
            item {
                ChildPrimaryButton("默认按钮", onClick = {})
            }
            item {
                ChildPrimaryButton("禁用按钮", enabled = false, onClick = {})
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CharacterOptionCard("人", selected = selected, onClick = { selected = !selected }, modifier = Modifier.weight(1f))
                    PictureOptionCard("图片", selected = !selected, onClick = { selected = !selected }, modifier = Modifier.weight(1f))
                }
            }
            item { FeedbackBanner("找到了！", positive = true) }
            item { FeedbackBanner("不着急，再看一眼", positive = false) }
            item { StarReward(5) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BadgeCard("第一颗星", "完成一个字解锁", unlocked = true, modifier = Modifier.weight(1f))
                    BadgeCard("识字小芽", "再认识 5 个字", unlocked = false, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarCard("小熊", selected = true, onClick = {})
                    AvatarCard("小兔", selected = false, onClick = {})
                }
            }
            item { EmptyState("还没有字卡", "先去认识一个新字吧。") }
        }
    }
}
