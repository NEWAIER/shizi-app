package com.family.shizi.ui.learned

import android.app.Application
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.family.shizi.ui.components.ChildPage
import com.family.shizi.ui.components.ChildTopBar
import com.family.shizi.ui.components.EmptyState
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
    var detailCharacter by remember { mutableStateOf<CharacterContent?>(null) }
    DisposableEffect(player) { onDispose { player.stop() } }
    val learned = state.characters.filter { it.initialLessonCompleted }
    ChildPage {
        Column(modifier = Modifier.fillMaxSize().testTag("page_learned"), horizontalAlignment = Alignment.CenterHorizontally) {
            ChildTopBar("字宝宝图鉴")
            Text("已经认识 ${learned.size} 个字 · 轻点听一听，长按看详情", modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (state.loading) {
            EmptyState("正在打开字宝宝图鉴", "马上就好。", Modifier.padding(top = 24.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(content.characters.size, key = { content.characters[it].id }) { index ->
                    val character = content.characters[index]
                    val progress = state.characters.firstOrNull { it.characterId == character.id }
                    CatalogCharacterCard(
                        character = character,
                        learned = progress?.initialLessonCompleted == true,
                        onPlay = { player.play(character.audio.character) },
                        onLongPress = { detailCharacter = character },
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
    detailCharacter?.let { character ->
        AlertDialog(
            onDismissRequest = { detailCharacter = null },
            title = { Text("${character.character}  ${character.pinyin}") },
            text = {
                Column {
                    Text(character.meaningForChild, style = MaterialTheme.typography.bodyLarge)
                    Text("词语：${character.words.joinToString("、") { word -> word.text }}", modifier = Modifier.padding(top = 10.dp))
                    Text("例句：${character.sentence.text}", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { detailCharacter = null }) { Text("知道啦") } },
        )
    }
}

@Composable
private fun CatalogCharacterCard(
    character: CharacterContent,
    learned: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_character_${character.id}")
            .pointerInput(character.id) {
                detectTapGestures(onTap = { if (learned) onPlay() }, onLongPress = { onLongPress() })
            },
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(character.character, style = MaterialTheme.typography.headlineMedium)
            Text(if (learned) "已认识" else "待认识", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LearnedCharacterCard(
    progress: CharacterProgressEntity,
    character: CharacterContent?,
    onPlay: (CharacterContent) -> Unit,
    onLongPress: (CharacterContent) -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("learned_play_${character?.id ?: progress.characterId}").pointerInput(character?.id) {
            detectTapGestures(
                onTap = { character?.let(onPlay) },
                onLongPress = { character?.let(onLongPress) },
            )
        },
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                childCharacterTitle(character),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("learned_character_title"),
            )
            Text(stateName(progress.state), modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelSmall)
            Text("轻点听声音 · 长按看详情", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
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
