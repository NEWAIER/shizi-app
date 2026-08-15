package com.family.shizi.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.db.SessionStatus
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.components.BadgeCard
import com.family.shizi.ui.components.ChildPage
import com.family.shizi.ui.components.ChildPrimaryButton
import com.family.shizi.ui.components.StarReward
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(onNavigate: (ShiziRoute) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ShiziApplication
    val repo = app.repository
    if (repo == null) {
        Text("数据库不可用，请联系家长", modifier = Modifier.padding(24.dp))
        return
    }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("保存处理中") }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var sessionStatus by remember { mutableStateOf<SessionStatus?>(null) }
    var isStageTest by remember { mutableStateOf(false) }
    var submissionFailed by remember { mutableStateOf(false) }
    var reviewHint by remember { mutableStateOf("") }
    var totalStars by remember { mutableStateOf(0) }
    var honorLevel by remember { mutableStateOf(1) }
    var learnedCount by remember { mutableStateOf(0) }
    var newlyUnlockedBadges by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val session = repo.getLatestOpenOrTodaySession(java.time.LocalDate.now())
        sessionId = session?.id
        sessionStatus = session?.status
        isStageTest = session?.plannedNewCount == 0 && session.plannedReviewCount == 0
        if (session != null && session.status !in setOf(SessionStatus.COMPLETED, SessionStatus.ENDED_EARLY)) {
            runCatching { repo.completeSession(session.id) }
                .onSuccess {
                    sessionStatus = SessionStatus.COMPLETED
                    submissionFailed = false
                }
                .onFailure {
                    submissionFailed = true
                    message = "学习结果保存失败，请重试"
                }
        }
        if (!submissionFailed) message = when (sessionStatus) {
            SessionStatus.COMPLETED -> if (isStageTest) "阶段测试完成啦" else "今天完成啦"
            SessionStatus.ENDED_EARLY -> "今天先到这里"
            SessionStatus.PAUSED -> "已经休息，回首页可以继续"
            null -> "没有正在保存的课程"
            else -> "练习已保存"
        }
        val progress = repo.getCharacterProgress()
        learnedCount = progress.count { it.initialLessonCompleted }
        val masteredCount = progress.count { it.state.name.contains("MASTERED") }
        val learningDays = repo.getCompletedLearningDayCount()
        totalStars = learnedCount * 10 + masteredCount * 5 + learningDays * 2
        val level = com.family.shizi.domain.engine.HonorLevels.progressFor(totalStars)
        honorLevel = level.level
        val counts = com.family.shizi.domain.engine.BadgeCounts(
            learnedCount = learnedCount,
            learningDays = learningDays,
        )
        newlyUnlockedBadges = com.family.shizi.domain.engine.BadgeCatalog.all
            .filter { it.id in com.family.shizi.domain.engine.BadgeCatalog.unlockedIds(counts) }
            .takeLast(2)
            .map { it.title }
        val upcoming = repo.getCharacterProgress()
            .mapNotNull { it.nextReviewDate }
            .filter { !it.isBefore(java.time.LocalDate.now()) }
            .minOrNull()
        reviewHint = if (isStageTest) "测试记录已保存在本机。按时复习，记得会更牢。" else when (upcoming) {
            null -> "明天还可以再认识新的字。"
            java.time.LocalDate.now().plusDays(1) -> "明天来复习今天认识的字，记得会更牢。"
            else -> "下次复习：${upcoming.monthValue}月${upcoming.dayOfMonth}日。"
        }
    }

    BackHandler { onNavigate(ShiziRoute.Home) }

    ChildPage {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (isStageTest) "测试完成！" else "今天真棒！", modifier = Modifier.testTag("page_result"), style = MaterialTheme.typography.headlineMedium)
            Text(message, modifier = Modifier.padding(top = 12.dp).testTag("result_status"), textAlign = TextAlign.Center)
            StarReward(totalStars, modifier = Modifier.padding(top = 16.dp))
            Text("Lv.$honorLevel · 已认识 $learnedCount 个字", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
            if (newlyUnlockedBadges.isNotEmpty()) {
                Text("已经点亮的徽章", modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.titleMedium)
                newlyUnlockedBadges.forEach { title ->
                    BadgeCard(title, "继续学习解锁更多成长奖励", unlocked = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            Text(reviewHint, modifier = Modifier.padding(top = 12.dp).testTag("result_review_hint"), textAlign = TextAlign.Center)
            ChildPrimaryButton(
                text = if (submissionFailed) "重新保存" else "回到首页",
                onClick = {
                    if (!submissionFailed) {
                        onNavigate(ShiziRoute.Home)
                    } else {
                        sessionId?.let { id ->
                            scope.launch {
                                runCatching { repo.completeSession(id) }
                                    .onSuccess {
                                        submissionFailed = false
                                        sessionStatus = SessionStatus.COMPLETED
                                        message = "今天完成啦"
                                    }
                                    .onFailure {
                                        message = "学习结果保存失败，请重试"
                                    }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 20.dp).testTag("result_complete"),
            )
        }
    }
}
