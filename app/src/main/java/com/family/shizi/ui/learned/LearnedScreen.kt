package com.family.shizi.ui.learned

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.db.CharacterProgressEntity
import com.family.shizi.data.db.LearningState
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.audio.AssetAudioPlayer
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LearnedUiState(val loading: Boolean = true, val characters: List<CharacterProgressEntity> = emptyList())

class LearnedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(LearnedUiState())
    val state: StateFlow<LearnedUiState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = LearnedUiState(loading = false, characters = repository?.getCharacterProgress().orEmpty())
    }
}

@Composable
fun LearnedScreen(onNavigate: (ShiziRoute) -> Unit) {
    val viewModel: LearnedViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as ShiziApplication
    val content = remember { ContentLoader.load(context) }
    val scope = rememberCoroutineScope()
    val player = remember {
        AssetAudioPlayer(context, onError = { error -> scope.launch { app.repository?.logAudioError(error) } })
            .also { it.attachLifecycle(lifecycleOwner) }
    }
    DisposableEffect(player) { onDispose { player.stop() } }
    val learned = state.characters.filter { it.initialLessonCompleted }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp).testTag("page_learned"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("我认识的字", style = MaterialTheme.typography.headlineMedium)
        Text("已经认识 ${learned.size} 个字", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium)
        if (!state.loading && learned.isEmpty()) {
            Text("先去学习第一个字吧！", modifier = Modifier.padding(top = 48.dp), style = MaterialTheme.typography.titleLarge)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(learned, key = { it.characterId }) { progress ->
                    LearnedCharacterCard(
                        progress = progress,
                        character = content.characters.firstOrNull { it.id == progress.characterId },
                        onPlay = { character -> player.play(character.audio.character) },
                    )
                }
                if (learned.size >= content.course.stageTestThreshold) {
                    item {
                        Button(
                            onClick = { onNavigate(ShiziRoute.StageTest) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("learned_go_stage_test"),
                        ) { Text("去参加阶段测试") }
                    }
                }
            }
        }
    }
}

@Composable
fun LearnedCharacterCard(
    progress: CharacterProgressEntity,
    character: CharacterContent?,
    onPlay: (CharacterContent) -> Unit,
) {
    val nextReview = progress.nextReviewDate?.let { date ->
        when {
            date.isBefore(LocalDate.now()) || date == LocalDate.now() -> "今天该来复习啦"
            date == LocalDate.now().plusDays(1) -> "明天来复习"
            else -> "下次复习：${date.monthValue}月${date.dayOfMonth}日"
        }
    } ?: "继续巩固，会记得更牢"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                childCharacterTitle(character),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.testTag("learned_character_title"),
            )
            Text(stateName(progress.state), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
            character?.let {
                Text("${it.pinyin} · ${it.meaningForChild}", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium)
                if (it.words.isNotEmpty()) {
                    Text("词语：${it.words.joinToString("、") { word -> word.text }}", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { onPlay(it) },
                    modifier = Modifier.padding(top = 10.dp).testTag("learned_play_${it.id}"),
                ) { Text("听一听") }
            }
            Text(nextReview, modifier = Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun childCharacterTitle(character: CharacterContent?): String = character?.character ?: "已学字"

private fun stateName(state: LearningState): String = when (state) {
    LearningState.FIRST_LEARNING -> "正在认识"
    LearningState.REVIEWING -> "正在复习"
    LearningState.TEMP_MASTERED -> "已经掌握"
    LearningState.STABLE_MASTERED -> "记得很牢"
    LearningState.UNLEARNED -> "刚开始"
}
