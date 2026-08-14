package com.family.shizi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.components.ChildPrimaryButton
import com.family.shizi.ui.components.MascotBubble
import com.family.shizi.ui.theme.ShiziShapes
import kotlinx.coroutines.coroutineScope

@Composable
fun HomeScreen(onNavigate: (ShiziRoute) -> Unit, onParentAuthorized: () -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    var parentPanelVisible by remember { mutableStateOf(false) }
    var parentBubbleExpanded by remember { mutableStateOf(true) }
    var adultGateOpen by remember { mutableStateOf(false) }
    var adultAnswer by remember { mutableStateOf(TextFieldValue("")) }
    var gateMessage by remember { mutableStateOf("长按 3 秒，再输入算式答案") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("字", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("欢迎回来", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("小朋友，今天也一起玩字宝宝", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("今天认识一个新朋友", modifier = Modifier.padding(top = 24.dp).testTag("page_home"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("它正在等你打招呼", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            state.error?.let { Text(it, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = ShiziShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.todayCharacter, modifier = Modifier.testTag("home_today_character"), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.todayPinyin, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.titleMedium)
                    MascotBubble("你好呀！我是${state.todayCharacter}宝宝", modifier = Modifier.padding(top = 12.dp))
                    if (state.dueReviewCount > 0) {
                        Text("还有 ${state.dueReviewCount} 个老朋友想见你", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    ChildPrimaryButton(
                        text = if (state.canStart) "开始和它认识" else state.primaryAction,
                        enabled = state.canStart,
                        modifier = Modifier.padding(top = 16.dp).testTag("home_primary"),
                        onClick = { viewModel.startOrContinue { onNavigate(it) } },
                    )
                }
            }
            Text("已认识 ${state.learnedCount} 个字宝宝", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (state.learnedCount / 50f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp).clip(RoundedCornerShape(99.dp)).testTag("home_progress"),
            )
        }

        // A discreet bubble keeps parent controls out of the child learning area.
        TextButton(
            onClick = { parentPanelVisible = true; parentBubbleExpanded = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("parent_floating_entry"),
        ) { Text(if (parentBubbleExpanded) "家长" else "•••") }

        AnimatedVisibility(
            visible = parentPanelVisible,
            enter = slideInHorizontally { it }, exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 12.dp),
        ) {
            Card(
                modifier = Modifier.widthIn(max = 310.dp).testTag("parent_slide_panel"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("家长入口", style = MaterialTheme.typography.titleMedium)
                    Text(gateMessage, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("adult_gate_hold").pointerInput(Unit) {
                            coroutineScope {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                                        val start = down.uptimeMillis
                                        var releasedAt = start
                                        do {
                                            val event = awaitPointerEvent()
                                            releasedAt = event.changes.firstOrNull()?.uptimeMillis ?: releasedAt
                                            if (!event.changes.any { it.pressed }) break
                                        } while (true)
                                        if (releasedAt - start >= 3_000L) { adultGateOpen = true; gateMessage = "请完成成人算式" }
                                        else gateMessage = "长按不足 3 秒"
                                    }
                                }
                            }
                        },
                    ) { Text("长按验证") }
                    if (adultGateOpen) {
                        OutlinedTextField(
                            value = adultAnswer, onValueChange = { adultAnswer = it }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("adult_gate_answer"),
                            label = { Text("8 + 7 = ?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Button(onClick = {
                            if (adultAnswer.text.trim() == "15") { onParentAuthorized() } else gateMessage = "算式不正确"
                        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("adult_gate_submit")) { Text("进入家长设置") }
                    }
                    TextButton(onClick = { parentPanelVisible = false; parentBubbleExpanded = false }) { Text("隐藏入口") }
                }
            }
        }
    }
}
