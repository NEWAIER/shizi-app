package com.family.shizi.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope

/** 家长入口：长按验证 + 成人算式，从首页独立出来。 */
@Composable
fun ParentEntry(
    onParentAuthorized: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var panelVisible by remember { mutableStateOf(false) }
    var bubbleExpanded by remember { mutableStateOf(true) }
    var gateOpen by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf(TextFieldValue("")) }
    var gateMessage by remember { mutableStateOf("长按 3 秒，再输入算式答案") }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        TextButton(
            onClick = { panelVisible = true; bubbleExpanded = true },
            modifier = Modifier.testTag("parent_floating_entry"),
        ) { Text(if (bubbleExpanded) "家长" else "•••") }

        AnimatedVisibility(
            visible = panelVisible,
            enter = slideInHorizontally { it }, exit = slideOutHorizontally { it },
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
                                    if (releasedAt - start >= 3_000L) { gateOpen = true; gateMessage = "请完成成人算式" }
                                    else gateMessage = "长按不足 3 秒"
                                }
                            }
                        }
                    },
                ) { Text("长按验证") }
                if (gateOpen) {
                    OutlinedTextField(
                        value = answer, onValueChange = { answer = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("adult_gate_answer"),
                        label = { Text("8 + 7 = ?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Button(onClick = {
                        if (answer.text.trim() == "15") { onParentAuthorized() } else gateMessage = "算式不正确"
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("adult_gate_submit")) { Text("进入家长设置") }
                }
                TextButton(onClick = { panelVisible = false; bubbleExpanded = false }) { Text("隐藏入口") }
            }
        }
        }
    }
}

/** 我的星球入口卡（原 HomeScreen 内的 profile 菜单）。 */
@Composable
fun ProfileMenuCard(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp).testTag("home_profile_menu"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("我的星球", style = MaterialTheme.typography.titleMedium)
            Text("头像 · 成长星星 · 徽章", modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
