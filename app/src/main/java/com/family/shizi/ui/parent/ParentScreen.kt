package com.family.shizi.ui.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.family.shizi.ShiziApplication
import com.family.shizi.data.db.OralCheckEntity
import com.family.shizi.data.db.OralStatus
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.repository.ShiziRepository
import com.family.shizi.navigation.ShiziRoute
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class ParentTab(val title: String) {
    Report("学习报告"),
    ErrorProne("易错字"),
    Oral("口头抽检"),
    Settings("设置与诊断"),
}

@Composable
fun ParentScreen(
    onNavigate: (ShiziRoute) -> Unit,
    initiallyAdultVerified: Boolean = false,
    onAuthorizationConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ShiziApplication
    val content = remember { ContentLoader.load(context) }
    val repo = app.repository
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(ParentTab.Report) }
    var reportLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var oralHistories by remember { mutableStateOf<Map<String, List<com.family.shizi.data.db.OralCheckEntity>>>(emptyMap()) }
    var diagnosticsPreview by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var clearArmed by remember { mutableStateOf(false) }
    var adultVerified by remember { mutableStateOf(initiallyAdultVerified) }
    var adultGateOpen by remember { mutableStateOf(false) }
    var adultAnswer by remember { mutableStateOf(TextFieldValue("")) }
    var adultGateMessage by remember { mutableStateOf("家长页需要再次成人验证") }

    suspend fun reload() {
        if (repo == null) {
            reportLines = listOf("数据库不可用，请联系技术支持")
            oralHistories = emptyMap()
            return
        }
        reportLines = repo.buildParentReports().map { report ->
            report.asLine().replace(report.characterId, characterLabel(content, report.characterId))
        }
            .ifEmpty { listOf("还没有学习记录") }
        val ids = repo.buildParentReports().map { it.characterId }
        oralHistories = ids.associateWith { repo.getOralHistory(it) }
    }

    LaunchedEffect(Unit) {
        onAuthorizationConsumed()
        reload()
    }

    BackHandler { onNavigate(ShiziRoute.Home) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text("家长页", modifier = Modifier.testTag("page_parent"), style = MaterialTheme.typography.headlineMedium)
            status.takeIf { it.isNotBlank() }?.let {
                Text(it, modifier = Modifier.padding(top = 8.dp).testTag("parent_status"))
            }
            if (!adultVerified) {
                ParentAdultGate(
                    message = adultGateMessage,
                    open = adultGateOpen,
                    answer = adultAnswer,
                    onAnswerChange = { adultAnswer = it },
                    onHoldPassed = {
                        adultGateOpen = true
                        adultGateMessage = "请完成成人算式"
                    },
                    onHoldShort = { adultGateMessage = "长按不足3秒" },
                    onSubmit = {
                        if (adultAnswer.text.trim() == "15") {
                            adultVerified = true
                            adultGateMessage = "成人验证通过"
                        } else {
                            adultGateMessage = "算式不正确"
                        }
                    },
                )
            } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ParentTab.entries.forEach { tab ->
                    Button(
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f).testTag("parent_tab_${tab.name}"),
                    ) {
                        Text(tab.title, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            when (selectedTab) {
                ParentTab.Report -> ParentReport(reportLines)
                ParentTab.ErrorProne -> ParentErrorProne(reportLines)
                ParentTab.Oral -> ParentOralChecks(
                    characterIds = oralHistories.keys.sorted(),
                    oralHistories = oralHistories,
                    characterLabels = content.characters.associate { it.id to "${it.character}（${it.pinyin}）" },
                    onRecord = { characterId, result, revisionOf ->
                        scope.launch {
                            repo?.appendOralCheck(
                                check = OralCheckEntity(
                                    id = UUID.randomUUID().toString(),
                                    characterId = characterId,
                                    result = result,
                                    checkedAt = Instant.now(),
                                    localDate = LocalDate.now(),
                                    eligibleForStable = result == OralStatus.INDEPENDENT_PASS,
                                    revisionOf = revisionOf,
                                ),
                                today = LocalDate.now(),
                                now = Instant.now(),
                            )
                            reload()
                            status = if (revisionOf != null) "口头抽检已修订" else "口头抽检已保存"
                        }
                    },
                )
                ParentTab.Settings -> ParentSettings(
                    diagnosticsPreview = diagnosticsPreview,
                    clearArmed = clearArmed,
                    onSetDailyNew = { count ->
                        scope.launch {
                            app.settingsStore.updateSettings { it.copy(dailyNewCharacterCount = count) }
                            status = "每日新字数已保存"
                        }
                    },
                    onSetLimit = { minutes ->
                        scope.launch {
                            app.settingsStore.updateSettings { it.copy(sessionLimitMinutes = minutes) }
                            status = "课程时长已保存，新课程生效"
                        }
                    },
                    onExport = {
                        scope.launch {
                            diagnosticsPreview = repo?.exportDiagnostics(
                                context = context,
                                appVersion = com.family.shizi.BuildConfig.VERSION_NAME,
                                exportedAt = Instant.now(),
                            ) ?: "数据库不可用，无法导出诊断"
                        }
                    },
                    onClearClick = {
                        if (!clearArmed) {
                            clearArmed = true
                            status = "再次点击确认清空，不可恢复"
                        } else {
                            scope.launch {
                                // SAFE clear flow via Repository: Room first, then Settings, with snapshot rollback
                                val result = repo?.clearLearningDataAndResetSettingsSafely()
                                clearArmed = false
                                diagnosticsPreview = ""
                                reload()
                                status = when (result) {
                                    ShiziRepository.ClearResult.Success -> "学习数据已清空，已恢复默认昵称"
                                    is ShiziRepository.ClearResult.Failed -> "清空失败：${result.stage} - ${result.cause?.message ?: "未知"}"
                                    null -> "数据库不可用，无法清空"
                                }
                                if (result is ShiziRepository.ClearResult.Success) {
                                    onNavigate(ShiziRoute.Home)
                                }
                            }
                        }
                    },
                )
            }
            }
        }
    }
}

@Composable
private fun ParentAdultGate(
    message: String,
    open: Boolean,
    answer: TextFieldValue,
    onAnswerChange: (TextFieldValue) -> Unit,
    onHoldPassed: () -> Unit,
    onHoldShort: () -> Unit,
    onSubmit: () -> Unit,
) {
    Text(
        text = message,
        modifier = Modifier.padding(top = 16.dp).testTag("parent_adult_gate_message"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("parent_adult_gate_hold")
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                            val start = down.uptimeMillis
                            var releasedAt = start
                            do {
                                val event = awaitPointerEvent()
                                releasedAt = event.changes.firstOrNull()?.uptimeMillis ?: releasedAt
                            } while (event.changes.any { it.pressed })
                            if (releasedAt - start >= 3000L) onHoldPassed() else onHoldShort()
                        }
                    }
                }
            },
    ) {
        Text("长按3秒验证")
    }
    if (open) {
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("parent_adult_gate_answer"),
            label = { Text("8 + 7 = ?") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("parent_adult_gate_submit"),
        ) {
            Text("验证")
        }
    }
}

@Composable
private fun ParentReport(lines: List<String>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("parent_report"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lines) { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ParentErrorProne(lines: List<String>) {
    // Use real error count from report: extract errorCount field from asLine()
    val filtered = lines.filter { line ->
        val errorMatch = Regex("错误次数=(\\d+)").find(line)
        errorMatch != null && errorMatch.groupValues[1].toInt() > 0
    }.ifEmpty { listOf("暂无易错字") }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("parent_error_prone"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filtered) { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ParentOralChecks(
    characterIds: List<String>,
    oralHistories: Map<String, List<OralCheckEntity>>,
    characterLabels: Map<String, String>,
    onRecord: (String, OralStatus, String?) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("parent_oral"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(characterIds.ifEmpty { listOf("暂无可抽检字") }) { characterId ->
            if (!characterId.startsWith("char_")) {
                Text(characterId)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(characterLabels[characterId] ?: "已学字", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onRecord(characterId, OralStatus.INDEPENDENT_PASS, null) }, modifier = Modifier.weight(1f)) {
                            Text("独立读出")
                        }
                        Button(onClick = { onRecord(characterId, OralStatus.PROMPTED, null) }, modifier = Modifier.weight(1f)) {
                            Text("提示后")
                        }
                        Button(onClick = { onRecord(characterId, OralStatus.FAIL, null) }, modifier = Modifier.weight(1f)) {
                            Text("未读出")
                        }
                    }
                    val history = oralHistories[characterId].orEmpty().filter { !it.isSuperseded }
                    if (history.isNotEmpty()) {
                        Text("历史记录（点击修订）：", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        history.forEach { check ->
                            val resultText = when (check.result) {
                                OralStatus.INDEPENDENT_PASS -> "独立"
                                OralStatus.PROMPTED -> "提示"
                                OralStatus.FAIL -> "未读出"
                                OralStatus.NOT_TESTED -> "未抽检"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${check.localDate} | $resultText",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Button(
                                    onClick = { onRecord(characterId, check.result, check.id) },
                                    modifier = Modifier.testTag("oral_revise_${check.id}"),
                                ) {
                                    Text("修订", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun characterLabel(content: com.family.shizi.data.content.ContentPackage, id: String): String =
    content.characters.firstOrNull { it.id == id }?.let { "${it.character}（${it.pinyin}）" } ?: "已学字"

@Composable
private fun ParentSettings(
    diagnosticsPreview: String,
    clearArmed: Boolean,
    onSetDailyNew: (Int) -> Unit,
    onSetLimit: (Int) -> Unit,
    onExport: () -> Unit,
    onClearClick: () -> Unit,
) {
    var dailyNewText by remember { mutableStateOf(TextFieldValue("3")) }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("parent_settings"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("每日新字数（1–5 个）", style = MaterialTheme.typography.titleSmall)
        }
        item {
            OutlinedTextField(
                value = dailyNewText,
                onValueChange = { value -> dailyNewText = value.copy(text = value.text.filter(Char::isDigit).take(1)) },
                modifier = Modifier.fillMaxWidth().testTag("parent_daily_new_input"),
                label = { Text("输入每天想学几个字") },
                supportingText = { Text("5字原型最多可选择 5 个；复习任务会优先安排。") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { dailyNewText.text.toIntOrNull()?.takeIf { it in 1..5 }?.let(onSetDailyNew) },
                    enabled = dailyNewText.text.toIntOrNull() in 1..5,
                    modifier = Modifier.weight(1f).testTag("parent_save_daily_new"),
                ) { Text("保存每日字数") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { count ->
                    TextButton(onClick = { dailyNewText = TextFieldValue(count.toString()) }, modifier = Modifier.weight(1f)) {
                        Text("$count")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8, 10, 12).forEach { minutes ->
                    Button(onClick = { onSetLimit(minutes) }, modifier = Modifier.weight(1f)) { Text("${minutes}分钟") }
                }
            }
        }
        item {
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth().testTag("parent_export_diagnostics")) {
                Text("导出诊断信息")
            }
        }
        item {
            Button(onClick = onClearClick, modifier = Modifier.fillMaxWidth().testTag("parent_clear_learning_data")) {
                Text(if (clearArmed) "确认清空学习数据" else "清空学习数据")
            }
        }
        if (diagnosticsPreview.isNotBlank()) {
            item {
                Text(
                    diagnosticsPreview.take(1000),
                    modifier = Modifier.testTag("diagnostics_preview"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
