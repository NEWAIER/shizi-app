package com.family.shizi.ui.practice

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.OptionContent
import com.family.shizi.data.content.OptionKind
import com.family.shizi.data.content.QuestionType
import com.family.shizi.data.db.EarlyEndReason
import com.family.shizi.data.db.ItemKind
import com.family.shizi.data.db.QuestionInstanceEntity
import com.family.shizi.data.db.ReviewStage
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.audio.AssetAudioPlayer
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// ─────────────────── Main Practice Screen ───────────────────

@Composable
fun PracticeScreen(onNavigate: (ShiziRoute) -> Unit) {
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
        AssetAudioPlayer(context) { error ->
            scope.launch { repo.logAudioError(error) }
        }.also { it.attachLifecycle(lifecycleOwner) }
    }
    var question by remember { mutableStateOf<QuestionInstanceEntity?>(null) }
    var options by remember { mutableStateOf<List<OptionContent>>(emptyList()) }
    var status by remember { mutableStateOf("读取题目") }
    var selectedAudioOptionId by remember { mutableStateOf<String?>(null) }
    var questionShownAtMs by remember { mutableStateOf(0L) }
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var submissionInFlight by remember { mutableStateOf(false) }
    var teachingCorrectId by remember { mutableStateOf<String?>(null) }
    var pauseDialogVisible by remember { mutableStateOf(false) }
    var multiTouchDetected by remember { mutableStateOf(false) }
    var currentItemKind by remember { mutableStateOf<ItemKind?>(null) }
    var currentReviewStage by remember { mutableStateOf(ReviewStage.NONE) }
    var questionNumber by remember { mutableStateOf(0) }
    var questionTotal by remember { mutableStateOf(0) }
    val json = remember { Json { ignoreUnknownKeys = true } }

    suspend fun reload() {
        val snapshot = repo.getCurrentPracticeSnapshot()
        val session = snapshot.session
        currentSessionId = session?.id
        currentItemKind = snapshot.item?.kind
        currentReviewStage = snapshot.item?.reviewStageAtStart ?: ReviewStage.NONE
        val pending = snapshot.question
        question = pending
        val allQuestions = snapshot.item?.let { repo.getQuestionsForItem(it.id) }.orEmpty()
        questionTotal = allQuestions.size
        questionNumber = pending?.let { current -> allQuestions.indexOfFirst { it.id == current.id } + 1 } ?: questionTotal
        val optionIds = pending?.optionIdsJson?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }.orEmpty()
        val seededOptions = optionIds.mapNotNull { id -> content.optionCatalog.firstOrNull { it.id == id } }
        // Every picture question is a stable 2×2 board: the original correct answer plus three real picture distractors.
        options = if (pending?.questionType == QuestionType.CHARACTER_CHOOSE_IMAGE.name) {
            (seededOptions + content.optionCatalog
                .filter { it.kind == OptionKind.IMAGE && seededOptions.none { seeded -> seeded.id == it.id } }
                .take(4 - seededOptions.size))
                .take(4)
        } else seededOptions
        selectedAudioOptionId = null
        questionShownAtMs = SystemClock.elapsedRealtime()
        submissionInFlight = false
        teachingCorrectId = null
        multiTouchDetected = false
        status = if (pending == null) "本组练习完成" else "请选择"
    }

    LaunchedEffect(Unit) {
        reload()
        if (currentSessionId == null) {
            repo.logError("NAVIGATION_NO_ACTIVE_ITEM", "{\"route\":\"practice\"}")
            onNavigate(ShiziRoute.Home)
        }
    }
    LaunchedEffect(currentSessionId) {
        val sessionId = currentSessionId ?: return@LaunchedEffect
        while (true) {
            delay(5_000)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                val ticked = repo.tickActiveSession(sessionId)
                if (ticked?.endPendingReason == EarlyEndReason.TIME_LIMIT) {
                    status = "时间到了，完成当前题后休息"
                    return@LaunchedEffect
                }
            }
        }
    }
    LaunchedEffect(question?.id) {
        val seed = question?.questionSeedId?.let { seedId ->
            content.characters.flatMap { it.questionSeeds }.firstOrNull { it.id == seedId }
        }
        seed?.promptAudio?.let {
            runCatching { player.play(it) }
        }
    }
    LaunchedEffect(currentSessionId, question?.id) {
        val sessionId = currentSessionId ?: return@LaunchedEffect
        if (question == null) {
            val nextRoute = repo.resolveNextRoute(sessionId)
            if (nextRoute != ShiziRoute.Practice) {
                onNavigate(nextRoute)
            }
        }
    }
    DisposableEffect(player) { onDispose { player.stop() } }
    BackHandler { pauseDialogVisible = true }

    if (pauseDialogVisible) {
        AlertDialog(
            onDismissRequest = { pauseDialogVisible = false },
            title = { Text("要先休息吗？") },
            text = { Text("当前题还没有提交，不会产生作答记录。") },
            confirmButton = {
                TextButton(onClick = { pauseDialogVisible = false }) {
                    Text("继续练习")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pauseDialogVisible = false
                        val sessionId = currentSessionId
                        if (sessionId == null) {
                            onNavigate(ShiziRoute.Home)
                        } else {
                            scope.launch {
                                player.stop()
                                repo.pauseForRest(sessionId)
                                onNavigate(ShiziRoute.Home)
                            }
                        }
                    },
                ) {
                    Text("先休息")
                }
            },
        )
    }

    fun submitAnswer(optionId: String) {
        if (submissionInFlight) return
        val q = question ?: return
        submissionInFlight = true
        scope.launch {
            runCatching {
                val responseMs = (SystemClock.elapsedRealtime() - questionShownAtMs).coerceAtLeast(0)
                val result = repo.submitAnswer(
                    questionInstanceId = q.id,
                    selectedOptionId = optionId,
                    answeredAt = Instant.now(),
                    localDate = LocalDate.now(),
                    responseTimeMs = responseMs,
                    isAccidental = responseMs < 300 || multiTouchDetected,
                )
                val attempt = result.attempt
                submissionInFlight = false
                when {
                    attempt.isAccidental -> {
                        multiTouchDetected = false
                        questionShownAtMs = SystemClock.elapsedRealtime()
                        status = "再来一次"
                    }
                    !attempt.isCorrect && attempt.attemptNumber == 1 -> {
                        status = "没关系，再试一次"
                    }
                    !attempt.isCorrect -> {
                        // Second wrong: show teaching with correct answer
                        val reducedOptionIds = buildSet {
                            add(q.correctOptionId)
                            options.firstOrNull { it.id != q.correctOptionId }?.let { add(it.id) }
                        }
                        options = options.filter { it.id in reducedOptionIds }
                        teachingCorrectId = q.correctOptionId
                        status = "正确答案是这个，跟老师读一遍"
                        // Play correct option audio if available
                        val correctOpt = options.firstOrNull { it.id == q.correctOptionId }
                        val correctAudio = correctOpt?.asset?.takeIf { it.endsWith(".mp3") }
                            ?: correctOpt?.characterId?.let { correctCharacterId ->
                                content.characters.firstOrNull { it.id == correctCharacterId }?.audio?.character
                            }
                        correctAudio?.let { runCatching { player.play(it) } }
                        delay(2500)
                        when {
                            result.sessionCompleted || result.endedEarly -> onNavigate(ShiziRoute.Result)
                            result.itemCompleted -> onNavigate(repo.resolveNextRoute(currentSessionId ?: return@runCatching))
                            else -> reload()
                        }
                    }
                    attempt.isCorrect -> {
                        teachingCorrectId = q.correctOptionId
                        status = "找到了！"
                        val correctOption = options.firstOrNull { it.id == q.correctOptionId }
                        val targetAudio = correctOption?.asset?.takeIf { it.endsWith(".mp3") }
                            ?: correctOption?.characterId?.let { correctCharacterId ->
                                content.characters.firstOrNull { it.id == correctCharacterId }?.audio?.character
                            }
                        targetAudio?.let { runCatching { player.play(it) } }
                        delay(800)
                        when {
                            result.sessionCompleted || result.endedEarly -> onNavigate(ShiziRoute.Result)
                            result.itemCompleted -> onNavigate(repo.resolveNextRoute(currentSessionId ?: return@runCatching))
                            else -> reload()
                        }
                    }
                }
            }.onFailure {
                submissionInFlight = false
                status = "保存失败：${it.message}"
            }
        }
    }

    val character = question?.let { q ->
        val correctOption = content.optionCatalog.firstOrNull { it.id == q.correctOptionId }
        content.characters.firstOrNull { it.id == correctOption?.characterId }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChildReviewHeader(
                itemKind = currentItemKind,
                reviewStage = currentReviewStage,
                questionNumber = questionNumber,
                questionTotal = questionTotal,
            )
            Text(
                text = status,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp).testTag("practice_status"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            question?.let { q ->
                // Multi-touch protection: consume extra pointers
                Box(
                    modifier = Modifier.pointerInput(q.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.count { it.pressed } > 1) {
                                    multiTouchDetected = true
                                }
                            }
                        }
                    },
                ) {
                    when (QuestionType.valueOf(q.questionType)) {
                        QuestionType.CHARACTER_CHOOSE_IMAGE ->
                            CharChooseImageQuestion(q, options, character, teachingCorrectId, submissionInFlight, ::submitAnswer)
                        QuestionType.LISTEN_CHOOSE_CHARACTER ->
                            ListenChooseCharQuestion(q, options, character, player, teachingCorrectId, submissionInFlight, ::submitAnswer)
                        QuestionType.CHARACTER_CHOOSE_AUDIO ->
                            CharChooseAudioQuestion(q, options, character, player, selectedAudioOptionId, { selectedAudioOptionId = it }, teachingCorrectId, submissionInFlight, ::submitAnswer)
                        QuestionType.SHAPE_RECOGNITION ->
                            ShapeRecognitionQuestion(q, options, character, teachingCorrectId, submissionInFlight, ::submitAnswer)
                        QuestionType.LIFE_WORD_CONTEXT ->
                            LifeWordContextQuestion(q, options, character, player, teachingCorrectId, submissionInFlight, ::submitAnswer)
                    }
                }
            } ?: Button(
                onClick = {
                    val sessionId = currentSessionId
                    if (sessionId == null) {
                        onNavigate(ShiziRoute.Home)
                    } else {
                        scope.launch {
                            onNavigate(repo.resolveNextRoute(sessionId))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).testTag("practice_finish"),
            ) { Text("看结果") }
        }
    }
}

@Composable
private fun ChildReviewHeader(
    itemKind: ItemKind?,
    reviewStage: ReviewStage,
    questionNumber: Int,
    questionTotal: Int,
) {
    val isReview = itemKind == ItemKind.REVIEW
    val isStageTest = itemKind == ItemKind.TEST
    val title = when {
        isStageTest -> "阶段测试"
        isReview -> "复习老朋友"
        else -> "新字闯关"
    }
    val subtitle = if (isStageTest) {
        "只考已经认识的字，慢慢想就好"
    } else if (isReview) {
        when (reviewStage) {
            ReviewStage.D1 -> "昨天认识的字，再来见一面"
            ReviewStage.D3 -> "隔几天再想一想，记得更牢"
            ReviewStage.D7 -> "一周小复习，看看还记得吗"
            ReviewStage.D14 -> "两周挑战，真棒！"
            else -> "把学过的字再想一想"
        }
    } else "学完马上玩一关，记得更牢"
    Card(
        modifier = Modifier.fillMaxWidth().testTag("child_review_header"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReview) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isStageTest) "🏆 小小测试" else if (isReview) "🌟 复习时间" else "🚀 闯关时间", style = MaterialTheme.typography.labelLarge)
                if (questionTotal > 0) Text("第 $questionNumber / $questionTotal 关", style = MaterialTheme.typography.labelLarge)
            }
            Text(title, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge)
            Text(subtitle, modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─────────────────── 1. 看字选图 ───────────────────

@Composable
private fun CharChooseImageQuestion(
    question: QuestionInstanceEntity,
    options: List<OptionContent>,
    character: CharacterContent?,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("看字选图", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("practice_question_type"))
        // Show target character prominently
        Text(
            text = character?.character ?: "？",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(16.dp).testTag("practice_target_character"),
        )
        Text("这个字对应的图片是哪张？", style = MaterialTheme.typography.bodySmall)
        ImageOptionGrid(options, teachingCorrectId, disabled, onSubmit)
    }
}

// ─────────────────── 2. 听音选字 ───────────────────

@Composable
private fun ListenChooseCharQuestion(
    question: QuestionInstanceEntity,
    options: List<OptionContent>,
    character: CharacterContent?,
    player: AssetAudioPlayer,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    val seed = remember(question.id) { question.id }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("听音选字", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("practice_question_type"))
        // Replay audio button
        Button(
            onClick = {
                character?.audio?.character?.let { runCatching { player.play(it) } }
            },
            modifier = Modifier.padding(16.dp).testTag("practice_replay_audio"),
        ) { Text("再听一遍") }
        Text("听到的字是哪个？", style = MaterialTheme.typography.bodySmall)
        TextOptionList(options, teachingCorrectId, disabled, onSubmit)
    }
}

// ─────────────────── 3. 看字选音 ───────────────────

@Composable
private fun CharChooseAudioQuestion(
    question: QuestionInstanceEntity,
    options: List<OptionContent>,
    character: CharacterContent?,
    player: AssetAudioPlayer,
    selectedAudioOptionId: String?,
    onSelectAudio: (String) -> Unit,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("看字选音", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("practice_question_type"))
        // Show target character
        Text(
            text = character?.character ?: "？",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(16.dp).testTag("practice_target_character"),
        )
        Text("这个字的读音是哪个？先试听再确认", style = MaterialTheme.typography.bodySmall)
        // Audio options: preview then confirm
        options.filter { it.kind == OptionKind.AUDIO }.forEach { option ->
            val isSelected = selectedAudioOptionId == option.id
            val isCorrect = teachingCorrectId == option.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("practice_option_${option.id}")
                    .pointerInput(option.id) {
                        detectTapGestures(onTap = {
                            if (!disabled && teachingCorrectId == null) {
                                runCatching { player.play(option.asset ?: return@detectTapGestures) }
                                onSelectAudio(option.id)
                            }
                        })
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCorrect -> MaterialTheme.colorScheme.primaryContainer
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isSelected) "已试听" else "点击试听", style = MaterialTheme.typography.bodyMedium)
                    if (isCorrect) Text("正确答案", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // Confirm button
        if (selectedAudioOptionId != null && teachingCorrectId == null) {
            Button(
                onClick = { selectedAudioOptionId.let(onSubmit) },
                enabled = !disabled,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("practice_confirm_audio"),
            ) { Text("确认") }
        }
    }
}

// ─────────────────── 4. 字图配对 ───────────────────

@Composable
private fun ShapeRecognitionQuestion(
    question: QuestionInstanceEntity,
    options: List<OptionContent>,
    character: CharacterContent?,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("字图配对", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("practice_question_type"))
        Text(
            text = character?.character ?: "？",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(16.dp).testTag("practice_target_character"),
        )
        Text("找出和这个字形状匹配的图片", style = MaterialTheme.typography.bodySmall)
        ImageOptionGrid(options, teachingCorrectId, disabled, onSubmit)
    }
}

// ─────────────────── 5. 听音选图 ───────────────────

@Composable
private fun LifeWordContextQuestion(
    question: QuestionInstanceEntity,
    options: List<OptionContent>,
    character: CharacterContent?,
    player: AssetAudioPlayer,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("听音选图", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("practice_question_type"))
        // Play the context audio (sentence or word)
        Button(
            onClick = {
                character?.sentence?.audioAsset?.let { runCatching { player.play(it) } }
            },
            modifier = Modifier.padding(16.dp).testTag("practice_replay_audio"),
        ) { Text("再听一遍") }
        Text("听到的内容对应哪张图？", style = MaterialTheme.typography.bodySmall)
        ImageOptionGrid(options, teachingCorrectId, disabled, onSubmit)
    }
}

// ─────────────────── Shared Components ───────────────────

@Composable
private fun ImageOptionGrid(
    options: List<OptionContent>,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    val context = LocalContext.current
    options.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { option ->
                val bitmap = remember(option.asset) {
                    option.asset?.takeIf { it.endsWith(".webp") }?.let { asset ->
                        context.assets.open("content/v1/$asset").use { BitmapFactory.decodeStream(it) }
                    }
                }
                val isCorrect = teachingCorrectId == option.id
                Card(
                    modifier = Modifier.weight(1f).aspectRatio(1f).testTag("practice_option_${option.id}").pointerInput(option.id) {
                        detectTapGestures(onTap = { if (!disabled && teachingCorrectId == null) onSubmit(option.id) })
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(), contentDescription = option.text ?: option.id,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                            )
                        }
                        if (isCorrect) Text("答对啦！", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TextOptionList(
    options: List<OptionContent>,
    teachingCorrectId: String?,
    disabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    options.forEach { option ->
        val isCorrect = teachingCorrectId == option.id
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("practice_option_${option.id}")
                .pointerInput(option.id) {
                    detectTapGestures(onTap = {
                        if (!disabled && teachingCorrectId == null) {
                            onSubmit(option.id)
                        }
                    })
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = option.text ?: option.id,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            if (isCorrect) {
                Text("正确答案", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
