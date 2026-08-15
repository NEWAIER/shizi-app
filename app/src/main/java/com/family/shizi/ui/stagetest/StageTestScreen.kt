package com.family.shizi.ui.stagetest

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.family.shizi.domain.engine.StageTestBatches
import com.family.shizi.data.db.CharacterProgressEntity
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.components.ChildPage
import com.family.shizi.ui.components.ChildPrimaryButton
import com.family.shizi.ui.components.ChildTopBar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StageTestUiState(
    val loading: Boolean = true,
    val batchIndex: Int = 0,
    val batchLearned: List<CharacterProgressEntity> = emptyList(),
    val batchIds: List<String> = emptyList(),
    val latestSummary: com.family.shizi.data.repository.ShiziRepository.StageTestSummary? = null,
)

class StageTestViewModel(
    application: Application,
    private val batchIndex: Int,
) : AndroidViewModel(application) {
    private val application = application
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(StageTestUiState())
    val state: StateFlow<StageTestUiState> = _state
    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val content = ContentLoader.load(application)
        val batchIds = StageTestBatches.characterIdsOf(content.learningOrder, batchIndex)
        val all = repository?.getCharacterProgress().orEmpty().associateBy { it.characterId }
        _state.value = StageTestUiState(
            loading = false,
            batchIndex = batchIndex,
            batchLearned = batchIds.mapNotNull { all[it] }.filter { it.initialLessonCompleted },
            batchIds = batchIds,
            latestSummary = repository?.getLatestStageTestSummary(),
        )
    }

    fun start(onReady: () -> Unit) = viewModelScope.launch {
        val repo = repository ?: return@launch
        runCatching {
            repo.createStageTestSession(
                content = ContentLoader.load(application),
                randomProvider = KotlinRandomProvider(),
                idProvider = IdProvider { UUID.randomUUID().toString() },
                batchIndex = batchIndex,
            )
        }.onSuccess { onReady() }
    }

    class Factory(
        private val application: Application,
        private val batchIndex: Int,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            StageTestViewModel(application, batchIndex) as T
    }
}

@Composable
fun StageTestScreen(batchIndex: Int, onNavigate: (ShiziRoute) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: StageTestViewModel = viewModel(
        key = "stage_test_$batchIndex",
        factory = StageTestViewModel.Factory(app, batchIndex),
    )
    val state by viewModel.state.collectAsState()
    val count = state.batchLearned.size
    val total = state.batchIds.size
    val batchNo = state.batchIndex + 1
    val latest = state.latestSummary
    ChildPage {
      Column(
        modifier = Modifier.fillMaxSize().testTag("page_stage_test"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ChildTopBar("挑战")
        Text("第 $batchNo 关 · 树洞闯关", style = MaterialTheme.typography.titleLarge)
        Text(
            "树洞里的字宝宝小游戏，不是考试。这一关认识第 ${state.batchIndex * StageTestBatches.BATCH_SIZE + 1} 到 ${state.batchIndex * StageTestBatches.BATCH_SIZE + total} 个字。",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        latest?.let { summary ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("stage_test_latest_result")) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("最近一次挑战", style = MaterialTheme.typography.titleMedium)
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
                Text("本关已认识 $count / $total 个字", style = MaterialTheme.typography.titleLarge)
                if (count < total) {
                    Text("把这一批 ${total - count} 个字都学会，树洞才会打开。", modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
                    ChildPrimaryButton("去学习", onClick = { onNavigate(ShiziRoute.Home) }, modifier = Modifier.padding(top = 20.dp).testTag("stage_test_go_learn"))
                } else {
                    Text("树洞打开啦！找出和声音一样的字宝宝。", modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
                    Text("每找到一个朋友，都会收获鼓励。", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    ChildPrimaryButton("开始闯关", onClick = { viewModel.start { onNavigate(ShiziRoute.Practice) } }, modifier = Modifier.padding(top = 20.dp).testTag("stage_test_start"))
                }
            }
        }
      }
    }
}
