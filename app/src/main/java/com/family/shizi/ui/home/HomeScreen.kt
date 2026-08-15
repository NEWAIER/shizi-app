package com.family.shizi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
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
import com.family.shizi.R
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.ui.components.ChildPrimaryButton
import com.family.shizi.ui.components.MascotBubble
import com.family.shizi.ui.theme.ShiziShapes
import kotlinx.coroutines.coroutineScope

@Composable
fun HomeScreen(
    onNavigate: (ShiziRoute) -> Unit,
    onOpenStageTest: (Int) -> Unit = { _ -> onNavigate(ShiziRoute.Learned) },
    onParentAuthorized: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val content = remember { ContentLoader.load(context) }
    var parentPanelVisible by remember { mutableStateOf(false) }
    var profileMenuVisible by remember { mutableStateOf(false) }
    var parentBubbleExpanded by remember { mutableStateOf(true) }
    var adultGateOpen by remember { mutableStateOf(false) }
    var adultAnswer by remember { mutableStateOf(TextFieldValue("")) }
    var gateMessage by remember { mutableStateOf("长按 3 秒，再输入算式答案") }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.xiaohe_launcher),
                    contentDescription = "小禾头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp).clip(CircleShape).clickable { profileMenuVisible = !profileMenuVisible }.testTag("home_avatar_menu"),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("欢迎回来", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("小朋友，今天认识${state.todayCharacter}宝宝吗？", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (profileMenuVisible) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("home_profile_menu"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { profileMenuVisible = false; onNavigate(ShiziRoute.Profile) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("我的星球", style = MaterialTheme.typography.titleMedium)
                        Text("头像 · 星星 · 徽章", modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("今天认识一个新朋友", modifier = Modifier.padding(top = 18.dp).testTag("page_home"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("${state.todayCharacter}宝宝正在等你打招呼", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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
            Text("我的成长树", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleLarge)
            Text("毛毛虫每学会一个字，就吃掉一个果子。每 10 个字会长出一个树洞挑战", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            GrowthTree(
                characters = content.characters,
                learnedCount = state.learnedCount,
                dailyTarget = state.dailyNewTarget,
                onLearn = { viewModel.startOrContinue { onNavigate(it) } },
                onOpenStageTest = onOpenStageTest,
            )
            Text("已认识 ${state.learnedCount} 个字宝宝", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (state.learnedCount / 50f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp).clip(RoundedCornerShape(99.dp)).testTag("home_progress"),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("今日进度 ${state.learnedCount}/50", style = MaterialTheme.typography.labelLarge)
                Text("星星 ${state.totalStars}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
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

@Composable
private fun GrowthTree(
    characters: List<com.family.shizi.data.content.CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    onLearn: () -> Unit,
    onOpenStageTest: (Int) -> Unit,
) {
    // 树只长到「已学 + 今日目标」的位置，随学习进度从下往上生长。
    val visibleCount = (learnedCount + dailyTarget).coerceIn(1, characters.size)
    val xPositions = listOf(156, 224, 118, 244, 88, 190, 260, 132, 72, 210)
    val step = 84
    val fruitSize = 54.dp
    val topMargin = 64
    val bottomMargin = 160 // 为树根与毛毛虫预留空间
    val treeHeight = topMargin.dp + ((visibleCount - 1) * step).dp + bottomMargin.dp
    val trunk = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val branch = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val leafColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    val fruitY = { index: Int -> (visibleCount - 1 - index) * step } // index 0 在最底部
    val caterpillarSegments = 2 + learnedCount.coerceAtMost(6)

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("home_growth_tree"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(treeHeight)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val points = (0 until visibleCount).map { index ->
                    Offset(xPositions[index % xPositions.size].dp.toPx(), (topMargin + fruitY(index)).dp.toPx())
                }
                if (points.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        var previous = points.first()
                        points.drop(1).forEach { point ->
                            quadraticTo((previous.x + point.x) / 2f, previous.y, point.x, point.y)
                            previous = point
                        }
                    }
                    drawPath(path, color = trunk, style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(path, color = branch, style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round))
                    points.forEachIndexed { index, point ->
                        if (index % 3 == 0) {
                            drawCircle(color = leafColor, radius = 10.dp.toPx(), center = point + Offset(22.dp.toPx(), -18.dp.toPx()))
                        }
                    }
                }
            }
            characters.take(visibleCount).forEachIndexed { index, character ->
                val number = index + 1
                val x = xPositions[index % xPositions.size]
                val y = fruitY(index)
                val learned = number <= learnedCount
                val toLearn = number > learnedCount && number <= learnedCount + dailyTarget
                val sparkleAlpha = rememberInfiniteTransition(label = "tree_fruit_$number").animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
                    label = "tree_fruit_alpha_$number",
                ).value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = (x - 27).dp, y = (topMargin + y).dp),
                ) {
                    if (learned) {
                        // 被毛毛虫吃掉的果子：保留汉字，换成金色星星果皮肤。
                        Box(
                            modifier = Modifier
                                .size(fruitSize)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            com.family.shizi.ui.theme.StarYellow,
                                            com.family.shizi.ui.theme.StarYellow.copy(alpha = 0.72f),
                                        ),
                                    ),
                                )
                                .testTag("home_tree_fruit_$number"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                character.character,
                                color = com.family.shizi.ui.theme.PrimaryText,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "★",
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = com.family.shizi.ui.theme.Coral,
                            )
                        }
                        Text("已吃掉", style = MaterialTheme.typography.labelSmall, color = com.family.shizi.ui.theme.Coral)
                    } else if (toLearn) {
                        Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Box(
                            modifier = Modifier
                                .size(fruitSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .alpha(sparkleAlpha)
                                .clickable(enabled = true) { onLearn() }
                                .testTag("home_tree_fruit_$number"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(character.character, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                            Text("★", modifier = Modifier.align(Alignment.TopEnd).padding(2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text("快来吃我", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f))
                        Box(
                            modifier = Modifier
                                .size(fruitSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .alpha(0.55f)
                                .testTag("home_tree_fruit_$number"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                if (number % com.family.shizi.domain.engine.StageTestBatches.BATCH_SIZE == 0) {
                    val batchIndex = number / com.family.shizi.domain.engine.StageTestBatches.BATCH_SIZE - 1
                    val holeUnlocked = learnedCount >= number
                    Card(
                        modifier = Modifier
                            .offset(x = if (x < 150) 188.dp else 12.dp, y = (topMargin + y + 14).dp)
                            .size(width = 118.dp, height = 58.dp)
                            .clickable(enabled = holeUnlocked) { onOpenStageTest(batchIndex) }
                            .testTag("home_tree_hole_$number"),
                        shape = RoundedCornerShape(topStart = 99.dp, topEnd = 99.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (holeUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("树洞 · 第${batchIndex + 1}关", style = MaterialTheme.typography.labelLarge)
                            Text(if (holeUnlocked) "挑战已开启" else "再学 ${number - learnedCount} 个字", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            // 毛毛虫：停在下一个待学果子旁边，吃一个长一截。
            if (learnedCount < visibleCount) {
                val caterpillarIndex = learnedCount
                val cx = xPositions[caterpillarIndex % xPositions.size]
                val cy = topMargin + fruitY(caterpillarIndex) + 92
                Caterpillar(
                    segmentCount = caterpillarSegments,
                    modifier = Modifier.offset(x = (cx - 26).dp, y = cy.dp),
                )
            }
        }
    }
}

/** 用 Canvas 绘制一只卡通毛毛虫：圆头 + 分段身体 + 触角，吃一个果子长一截。 */
@Composable
private fun Caterpillar(segmentCount: Int, modifier: Modifier = Modifier) {
    val headSize = 30.dp
    val segmentSize = 22.dp
    val green = com.family.shizi.ui.theme.SuccessGreen
    val darkGreen = Color(0xFF3FA371)
    val bob = rememberInfiniteTransition(label = "caterpillar_bob").animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "caterpillar_bob_alpha",
    ).value
    Box(modifier.size(width = (segmentCount * 14 + 30).dp, height = 46.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyStartY = 30.dp.toPx() + bob.dp.toPx()
            // 分段身体：从尾部向头部排列。
            (segmentCount - 1 downTo 1).forEach { seg ->
                val cx = seg * 14.dp.toPx() + 10.dp.toPx()
                val cy = bodyStartY - (seg % 2) * 4.dp.toPx()
                drawCircle(
                    color = if (seg % 2 == 0) green else darkGreen,
                    radius = segmentSize.toPx() / 2f,
                    center = Offset(cx, cy),
                )
            }
            // 头部：大圆 + 眼睛 + 触角。
            val headX = (segmentCount * 14 + 10).dp.toPx()
            val headY = bodyStartY - 8.dp.toPx()
            drawCircle(color = green, radius = headSize.toPx() / 2f, center = Offset(headX, headY))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(headX + 5.dp.toPx(), headY - 5.dp.toPx()))
            drawCircle(color = Color(0xFF2F3B45), radius = 2.dp.toPx(), center = Offset(headX + 6.dp.toPx(), headY - 5.dp.toPx()))
            // 触角。
            drawLine(
                color = darkGreen,
                start = Offset(headX + 2.dp.toPx(), headY - 12.dp.toPx()),
                end = Offset(headX + 6.dp.toPx(), headY - 20.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = darkGreen, radius = 3.dp.toPx(), center = Offset(headX + 6.dp.toPx(), headY - 21.dp.toPx()))
            drawLine(
                color = darkGreen,
                start = Offset(headX - 6.dp.toPx(), headY - 12.dp.toPx()),
                end = Offset(headX - 10.dp.toPx(), headY - 20.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = darkGreen, radius = 3.dp.toPx(), center = Offset(headX - 10.dp.toPx(), headY - 21.dp.toPx()))
            // 小脚。
            drawCircle(color = darkGreen, radius = 2.5.dp.toPx(), center = Offset(headX - 8.dp.toPx(), headY + 12.dp.toPx()))
            drawCircle(color = darkGreen, radius = 2.5.dp.toPx(), center = Offset(headX - 14.dp.toPx(), headY + 14.dp.toPx()))
        }
    }
}
