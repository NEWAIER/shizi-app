package com.family.shizi.ui.learn

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.shizi.R
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentRepository
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
    var content by remember { mutableStateOf(ContentLoader.load(context)) }
    var assetRoot by remember { mutableStateOf(ContentRepository.get(context).active().descriptor.assetRoot) }
    val scope = rememberCoroutineScope()
    val player = remember {
        AssetAudioPlayer(context, { error: AudioPlayerError -> scope.launch { repo.logAudioError(error) } }) { assetRoot }
            .also { it.attachLifecycle(lifecycleOwner) }
    }
    var characterId by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(InitialTeachingStep.A_CONTEXT) }
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var timeLimitMessage by remember { mutableStateOf("") }
    var timeLimitPending by remember { mutableStateOf(false) }
    var pauseDialogVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("准备好了吗？") }
    var playbackId by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val snapshot = repo.getCurrentItemSnapshot()
        if (snapshot == null) {
            repo.logError("NAVIGATION_NO_ACTIVE_ITEM", "{\"route\":\"learn\"}")
            onNavigate(ShiziRoute.Home)
            return@LaunchedEffect
        }
        currentSessionId = snapshot.session?.id
        snapshot.session?.let { session ->
            val loaded = ContentRepository.get(context).loadForSession(session)
            content = loaded.content
            assetRoot = loaded.descriptor.assetRoot
        }
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
    val bitmap = remember(character?.imageAsset, assetRoot) {
        character?.imageAsset?.let { asset -> context.assets.open("${assetRoot.trimEnd('/')}/$asset").use(BitmapFactory::decodeStream) }
    }
    val display = learningDisplay(character, step)

    suspend fun advanceTeachingStep() {
        val currentId = characterId ?: return
        runCatching {
            val next = when (step) {
                InitialTeachingStep.A_CONTEXT -> InitialTeachingStep.B_SOUND_MEANING
                InitialTeachingStep.B_SOUND_MEANING -> InitialTeachingStep.C_WORD_SENTENCE
                else -> InitialTeachingStep.PRACTICE
            }
            repo.saveTeachingStep(currentId, next, Instant.now())
            step = next
            if (timeLimitPending) {
                currentSessionId?.let { repo.endEarly(it, EarlyEndReason.TIME_LIMIT) }
                onNavigate(ShiziRoute.Result)
            } else if (next == InitialTeachingStep.PRACTICE) {
                onNavigate(ShiziRoute.Practice)
            }
        }.onFailure { statusMessage = "保存失败，请再试一次" }
    }

    fun playCurrentStep() {
        if (display.audioAssets.isEmpty()) return
        playbackId += 1
        val thisPlayback = playbackId
        statusMessage = "认真听一听"
        player.stop()
        player.playSequence(display.audioAssets) {
            if (thisPlayback != playbackId) return@playSequence
            scope.launch {
                statusMessage = "听完啦！"
                // Give the child a small, predictable pause before the next learning beat.
                delay(1_500)
                if (thisPlayback == playbackId && !pauseDialogVisible) {
                    advanceTeachingStep()
                }
            }
        }
    }

    LaunchedEffect(characterId, step) {
        if (characterId != null && display.audioAssets.isNotEmpty()) {
            delay(250)
            playCurrentStep()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "和字宝宝做朋友",
                            modifier = Modifier.testTag("page_learn"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("听一听、看一看，慢慢认识它", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Image(
                        painter = painterResource(R.drawable.caterpillar_mascot_main),
                        contentDescription = "陪伴学习的小禾",
                        modifier = Modifier.size(56.dp),
                    )
                }
                LearningStepRail(currentStep = display.number)

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260))) togetherWith
                        (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.04f, animationSpec = tween(180)))
                },
                label = "learn_step_transition",
            ) { targetStep ->
                val animatedDisplay = learningDisplay(character, targetStep)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(30.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF3)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                animatedDisplay.eyebrow,
                                modifier = Modifier.background(Color(0xFFE7F6D8), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF4C8A50),
                            )
                            Text(
                                character?.character ?: "？",
                                modifier = Modifier.padding(top = 4.dp).testTag("learn_character"),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E5E35),
                            )
                            Text(character?.pinyin ?: "", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            if (animatedDisplay.showImage) {
                                bitmap?.let {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(184.dp).padding(top = 12.dp)
                                            .clip(RoundedCornerShape(24.dp)).background(Color(0xFFFFEAC2))
                                            .border(3.dp, Color(0xFFFFD58A), RoundedCornerShape(24.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            it.asImageBitmap(), character?.imageAlt,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).testTag("learn_image"),
                                        )
                                    }
                                }
                            }
                            Text(
                                animatedDisplay.mainText,
                                modifier = Modifier.padding(top = 14.dp).testTag("learn_step_text"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            animatedDisplay.detailText?.let {
                                Text(it, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color(0xFF6C7774))
                            }
                        }
                    }
                }

                if (timeLimitMessage.isNotBlank()) Text(timeLimitMessage, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Text(statusMessage, modifier = Modifier.padding(top = 10.dp).testTag("learn_status"), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color(0xFF6C7774))

                Button(
                    onClick = { playCurrentStep() },
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 10.dp).testTag("learn_play_audio"),
                    shape = RoundedCornerShape(22.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9B58), contentColor = Color.White),
                ) { Text("再听一次", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LearningStepRail(currentStep: Int) {
    val labels = listOf("认识字", "了解意思", "一起读")
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        labels.forEachIndexed { index, label ->
            val active = index < currentStep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(if (active) 15.dp else 11.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) Color(0xFFFFB84D) else Color(0xFFD8E8D0)),
                )
                Text(label, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color(0xFF8E5E35) else Color(0xFF8BA18D))
            }
            if (index < labels.lastIndex) {
                Spacer(Modifier.width(34.dp).height(2.dp).background(if (index + 1 < currentStep) Color(0xFFFFB84D) else Color(0xFFD8E8D0)))
            }
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
            c.words.map { it.audioAsset } + c.sentence.audioAsset, "听词语和句子", true)
        else -> LearningDisplay(3, 1f, "准备闯关", "我们来练一练", null, listOf(c.audio.character), "再听一遍", false)
    }
}
