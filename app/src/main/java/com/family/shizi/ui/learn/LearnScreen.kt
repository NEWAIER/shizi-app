package com.family.shizi.ui.learn

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.db.EarlyEndReason
import com.family.shizi.data.db.InitialTeachingStep
import com.family.shizi.data.db.ItemKind
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.audio.AssetAudioPlayer
import com.family.shizi.ui.audio.AudioPlayerError
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LearnScreen(onNavigate: (ShiziRoute) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ShiziApplication
    val repo = app.repository
    if (repo == null) {
        Text("数据库不可用，请联系家长", modifier = Modifier.padding(24.dp))
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val content = remember { ContentLoader.load(context) }
    val scope = rememberCoroutineScope()
    val player = remember {
        AssetAudioPlayer(context) { error: AudioPlayerError -> scope.launch { repo.logAudioError(error) } }
            .also { it.attachLifecycle(lifecycleOwner) }
    }
    var characterId by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(InitialTeachingStep.A_CONTEXT) }
    var audioCompleted by remember { mutableStateOf(false) }
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var timeLimitMessage by remember { mutableStateOf("") }
    var timeLimitPending by remember { mutableStateOf(false) }
    var pauseDialogVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("准备好了吗？") }

    LaunchedEffect(Unit) {
        val snapshot = repo.getCurrentItemSnapshot()
        if (snapshot == null) {
            repo.logError("NAVIGATION_NO_ACTIVE_ITEM", "{\"route\":\"learn\"}")
            onNavigate(ShiziRoute.Home)
            return@LaunchedEffect
        }
        currentSessionId = snapshot.session?.id
        if (snapshot.item.kind != ItemKind.NEW) {
            onNavigate(ShiziRoute.Practice)
            return@LaunchedEffect
        }
        characterId = snapshot.item.characterId
        repo.startInitialLearning(snapshot.item.characterId, InitialTeachingStep.A_CONTEXT, Instant.now())
        step = repo.getCurrentItemSnapshot()?.progress?.initialTeachingStep ?: InitialTeachingStep.A_CONTEXT
    }
    LaunchedEffect(currentSessionId) {
        val sessionId = currentSessionId ?: return@LaunchedEffect
        while (true) {
            delay(5_000)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                if (repo.tickActiveSession(sessionId)?.endPendingReason == EarlyEndReason.TIME_LIMIT) {
                    timeLimitMessage = "这一小步完成后，我们就休息。"
                    timeLimitPending = true
                    return@LaunchedEffect
                }
            }
        }
    }
    DisposableEffect(player) { onDispose { player.stop() } }
    BackHandler { pauseDialogVisible = true }

    if (pauseDialogVisible) {
        AlertDialog(
            onDismissRequest = { pauseDialogVisible = false },
            title = { Text("要先休息吗？") }, text = { Text("这一页会为你保留。") },
            confirmButton = { TextButton(onClick = { pauseDialogVisible = false }) { Text("继续") } },
            dismissButton = { TextButton(onClick = {
                pauseDialogVisible = false
                scope.launch {
                    player.stop()
                    currentSessionId?.let { repo.pauseForRest(it) }
                    onNavigate(ShiziRoute.Home)
                }
            }) { Text("先休息") } },
        )
    }

    val character = content.characters.firstOrNull { it.id == characterId }
    val bitmap = remember(character?.imageAsset) {
        character?.imageAsset?.let { asset -> context.assets.open("content/v1/$asset").use(BitmapFactory::decodeStream) }
    }
    val display = learningDisplay(character, step)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "今天认识一个新朋友",
                modifier = Modifier.testTag("page_learn"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { display.progress },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp)).testTag("learn_progress"),
            )
            Text("第 ${display.number} 步，共 3 步", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium)

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(display.eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(character?.character ?: "？", modifier = Modifier.padding(top = 4.dp).testTag("learn_character"),
                        style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text(character?.pinyin ?: "", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    if (display.showImage) {
                        bitmap?.let {
                            Image(it.asImageBitmap(), character?.imageAlt,
                                modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 12.dp).clip(RoundedCornerShape(20.dp)).testTag("learn_image"))
                        }
                    }
                    Text(display.mainText, modifier = Modifier.padding(top = 14.dp).testTag("learn_step_text"),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    display.detailText?.let { Text(it, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
                }
            }

            if (timeLimitMessage.isNotBlank()) Text(timeLimitMessage, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Text(statusMessage, modifier = Modifier.padding(top = 12.dp).testTag("learn_status"), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

            Button(
                onClick = {
                    audioCompleted = false
                    statusMessage = "认真听一听"
                    player.playSequence(display.audioAssets) {
                        audioCompleted = true
                        statusMessage = "听完啦！点击下面按钮继续。"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 12.dp).testTag("learn_play_audio"),
                shape = RoundedCornerShape(20.dp),
            ) { Text("🔊  ${display.playLabel}", style = MaterialTheme.typography.titleMedium) }

            Button(
                onClick = {
                    val currentId = characterId ?: return@Button
                    scope.launch {
                        runCatching {
                            val next = when (step) {
                                InitialTeachingStep.A_CONTEXT -> InitialTeachingStep.B_SOUND_MEANING
                                InitialTeachingStep.B_SOUND_MEANING -> InitialTeachingStep.C_WORD_SENTENCE
                                else -> InitialTeachingStep.PRACTICE
                            }
                            repo.saveTeachingStep(currentId, next, Instant.now())
                            step = next
                            audioCompleted = false
                            if (timeLimitPending) {
                                currentSessionId?.let { repo.endEarly(it, EarlyEndReason.TIME_LIMIT) }
                                onNavigate(ShiziRoute.Result)
                            } else if (next == InitialTeachingStep.PRACTICE) onNavigate(ShiziRoute.Practice)
                        }.onFailure { statusMessage = "保存失败，请再试一次" }
                    }
                },
                enabled = audioCompleted,
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 10.dp).testTag("learn_next"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) { Text(if (step == InitialTeachingStep.C_WORD_SENTENCE) "去闯关 →" else "我知道了 →", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

private data class LearningDisplay(
    val number: Int, val progress: Float, val eyebrow: String, val mainText: String, val detailText: String?,
    val audioAssets: List<String>, val playLabel: String, val showImage: Boolean,
)

private fun learningDisplay(character: CharacterContent?, step: InitialTeachingStep): LearningDisplay {
    val c = character ?: return LearningDisplay(1, 0.33f, "", "正在准备", null, emptyList(), "听一听", false)
    return when (step) {
        // A displays the introduction, B displays the child-friendly meaning. The previous UI reversed them.
        InitialTeachingStep.A_CONTEXT -> LearningDisplay(1, 1f / 3f, "先认识这个字", c.teachingPrompt, "它读作：${c.pinyin}", listOf(c.audio.character), "听字音", true)
        InitialTeachingStep.B_SOUND_MEANING -> LearningDisplay(2, 2f / 3f, "再看看它的意思", c.meaningForChild, "跟着声音想一想。", listOf(c.audio.meaning), "听意思", true)
        InitialTeachingStep.C_WORD_SENTENCE -> LearningDisplay(3, 1f, "最后一起读", c.words.joinToString("   ") { it.text }, c.sentence.text,
            c.words.map { it.audioAsset } + c.sentence.audioAsset, "听词语和句子", false)
        else -> LearningDisplay(3, 1f, "准备闯关", "我们来练一练", null, listOf(c.audio.character), "再听一遍", false)
    }
}
