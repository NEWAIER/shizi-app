package com.family.shizi.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.db.SessionStatus
import com.family.shizi.domain.core.IdProvider
import com.family.shizi.domain.core.KotlinRandomProvider
import com.family.shizi.domain.health.AppReadinessChecker
import com.family.shizi.navigation.ShiziRoute
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val onboardingCompleted: Boolean = false,
    val title: String = "儿童首页",
    val message: String = "正在读取本地数据",
    val primaryAction: String = "开始",
    val canStart: Boolean = false,
    val learnedCount: Int = 0,
    val dueReviewCount: Int = 0,
    val dailyNewTarget: Int = 3,
    val stageTestThreshold: Int = 3,
    val canTakeStageTest: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ShiziApplication
    private val content by lazy { ContentLoader.load(application) }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        refresh()
    }

    fun refresh(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val repo = app.repository
            if (repo == null) {
                _uiState.value = HomeUiState(
                    message = "数据库不可用，请家长处理",
                    canStart = false,
                    error = "数据库打开失败",
                )
                return@launch
            }
            runCatching {
                val readiness = AppReadinessChecker(getApplication()).check()
                if (!readiness.ready) {
                    repo.logError("APP_READINESS_FAILED", """{"message":"${readiness.message}"}""")
                    _uiState.value = HomeUiState(
                        onboardingCompleted = false,
                        message = "需要家长处理",
                        canStart = false,
                        error = readiness.message,
                    )
                    return@launch
                }
                if (!repo.reconcileLaunchDate(today)) {
                    _uiState.value = HomeUiState(
                        onboardingCompleted = true,
                        message = "需要家长处理",
                        canStart = false,
                        error = "检测到系统日期回拨，请家长确认设备日期后再继续",
                    )
                    return@launch
                }
                repo.seedCharacterProgressIfMissing(content.learningOrder)
                val settings = repo.settings.first()
                val progress = repo.getCharacterProgress()
                val learnedCount = progress.count { it.initialLessonCompleted }
                val dueReviewCount = progress.count { it.nextReviewDate?.let { date -> date <= today } == true }
                val base = HomeUiState(
                    onboardingCompleted = true,
                    learnedCount = learnedCount,
                    dueReviewCount = dueReviewCount,
                    dailyNewTarget = settings.dailyNewCharacterCount,
                    stageTestThreshold = content.course.stageTestThreshold,
                    canTakeStageTest = learnedCount >= content.course.stageTestThreshold,
                )
                val existing = repo.getUsableSession(today)
                _uiState.value = when {
                    existing?.status == SessionStatus.COMPLETED -> base.copy(
                        message = "今天完成啦",
                        primaryAction = "今天完成啦",
                        canStart = false,
                    )
                    existing?.status == SessionStatus.ENDED_EARLY -> base.copy(
                        message = "今天先到这里",
                        primaryAction = "今天先到这里",
                        canStart = false,
                    )
                    existing != null -> base.copy(
                        message = "今天还有任务可以继续",
                        primaryAction = "继续",
                        canStart = true,
                    )
                    else -> base.copy(
                        message = "今天认识新朋友",
                        primaryAction = "开始",
                        canStart = true,
                    )
                }
            }.onFailure {
                _uiState.value = HomeUiState(message = "本地数据读取失败", canStart = false, error = it.message)
            }
        }
    }

    fun completeOnboarding(nickname: String = "") {
        viewModelScope.launch {
            val repo = app.repository ?: return@launch
            repo.completeOnboarding(nickname = nickname, contentVersion = content.contentVersion)
            refresh()
        }
    }

    fun startOrContinue(onReady: (ShiziRoute) -> Unit) {
        viewModelScope.launch {
            val repo = app.repository
            if (repo == null) {
                _uiState.value = HomeUiState(message = "数据库不可用，请家长处理", canStart = false, error = "数据库打开失败")
                return@launch
            }
            val readiness = AppReadinessChecker(getApplication()).check()
            if (!readiness.ready) {
                _uiState.value = HomeUiState(message = "需要家长处理", canStart = false, error = readiness.message)
                return@launch
            }
            if (!repo.reconcileLaunchDate(LocalDate.now())) {
                _uiState.value = HomeUiState(
                    onboardingCompleted = true,
                    message = "需要家长处理",
                    canStart = false,
                    error = "检测到系统日期回拨，请家长确认设备日期后再继续",
                )
                return@launch
            }
            val settings = repo.settings.first()
            val session = repo.getOrCreateDailySession(
                date = LocalDate.now(),
                settings = settings,
                content = content,
                randomProvider = KotlinRandomProvider(),
                idProvider = IdProvider { UUID.randomUUID().toString() },
            )
            repo.markSessionActive(session.id)
            val route = repo.resolveNextRoute(session.id)
            refresh()
            onReady(route)
        }
    }
}
