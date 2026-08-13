package com.family.shizi.ui.stagetest

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.domain.core.IdProvider
import com.family.shizi.domain.core.KotlinRandomProvider
import com.family.shizi.data.db.CharacterProgressEntity
import com.family.shizi.navigation.ShiziRoute
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StageTestUiState(
    val loading: Boolean = true,
    val learned: List<CharacterProgressEntity> = emptyList(),
    val latestSummary: com.family.shizi.data.repository.ShiziRepository.StageTestSummary? = null,
    val stageTestThreshold: Int = 3,
)

class StageTestViewModel(application: Application) : AndroidViewModel(application) {
    private val application = application
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(StageTestUiState())
    val state: StateFlow<StageTestUiState> = _state
    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val content = ContentLoader.load(application)
        _state.value = StageTestUiState(
            loading = false,
            learned = repository?.getCharacterProgress().orEmpty().filter { it.initialLessonCompleted },
            latestSummary = repository?.getLatestStageTestSummary(),
            stageTestThreshold = content.course.stageTestThreshold,
        )
    }

    fun start(onReady: () -> Unit) = viewModelScope.launch {
        val repo = repository ?: return@launch
        runCatching {
            repo.createStageTestSession(
                content = ContentLoader.load(application),
                randomProvider = KotlinRandomProvider(),
                idProvider = IdProvider { UUID.randomUUID().toString() },
            )
        }.onSuccess { onReady() }
    }
}

@Composable
fun StageTestScreen(onNavigate: (ShiziRoute) -> Unit) {
    val viewModel: StageTestViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val count = state.learned.size
    val threshold = state.stageTestThreshold
    val latest = state.latestSummary
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp).testTag("page_stage_test"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("阶段测试", style = MaterialTheme.typography.headlineMedium)
        latest?.let { summary ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("stage_test_latest_result")) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("最近一次测试", style = MaterialTheme.typography.titleMedium)
                    Text("${summary.localDate.monthValue}月${summary.localDate.dayOfMonth}日 · 第一次答对 ${summary.firstTryCorrectCount} / ${summary.totalQuestions} 题", modifier = Modifier.padding(top = 7.dp), textAlign = TextAlign.Center)
                    val reinforce = summary.reinforceCharacterIds
                    Text(
                        if (reinforce.isEmpty()) "全部一次答对，真棒！" else "再复习一下：${reinforce.joinToString("、")}",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = if (latest == null) 22.dp else 14.dp)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已认识 $count / $threshold 个字", style = MaterialTheme.typography.titleLarge)
                if (count < threshold) {
                    Text("再认识 ${threshold - count} 个字，就可以开启第一关测试。", modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
                    Button(onClick = { onNavigate(ShiziRoute.Home) }, modifier = Modifier.padding(top = 20.dp).testTag("stage_test_go_learn")) { Text("去学习") }
                } else {
                    Text("第一关准备好了！测试只会从已经认识的字中出题。", modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
                    Text("答题结果会保存到本机，但不会跳过之后该做的复习。", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.start { onNavigate(ShiziRoute.Practice) } }, modifier = Modifier.padding(top = 20.dp).testTag("stage_test_start")) { Text("开始测试") }
                }
            }
        }
    }
}
