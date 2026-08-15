package com.family.shizi.ui.profile

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.ui.components.DecorativeStar
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.family.shizi.ShiziApplication
import com.family.shizi.data.db.CharacterProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.family.shizi.ui.components.ChildPage
import com.family.shizi.ui.components.ChildTopBar
import com.family.shizi.R

data class ProfileUiState(
    val loading: Boolean = true,
    val nickname: String = "小朋友",
    val learnedCount: Int = 0,
    val masteredCount: Int = 0,
    val learningDays: Int = 0,
    val dailyTarget: Int = 3,
    val avatarId: String = "bear",
    val totalStars: Int = 0,
    val levelProgress: com.family.shizi.domain.engine.HonorLevels.LevelProgress =
        com.family.shizi.domain.engine.HonorLevels.progressFor(0),
    val unlockedBadgeIds: Set<String> = emptySet(),
    val awaitingBadgeIds: Set<String> = com.family.shizi.domain.engine.BadgeCatalog.awaitingIds().toSet(),
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state
    init { viewModelScope.launch {
        val progress = repository?.getCharacterProgress().orEmpty()
        val settings = repository?.settings?.first()
        val learnedCount = progress.count { it.initialLessonCompleted }
        val masteredCount = progress.count { it.state.name.contains("MASTERED") }
        val learningDays = repository?.getCompletedLearningDayCount() ?: 0
        val totalStars = learnedCount * 10 + masteredCount * 5 + learningDays * 2
        val counts = com.family.shizi.domain.engine.BadgeCounts(
            learnedCount = learnedCount,
            learningDays = learningDays,
        )
        _state.value = ProfileUiState(
            loading = false,
            nickname = settings?.nickname?.ifBlank { "小朋友" } ?: "小朋友",
            learnedCount = learnedCount,
            masteredCount = masteredCount,
            learningDays = learningDays,
            dailyTarget = settings?.dailyNewCharacterCount ?: 3,
            avatarId = settings?.avatarId ?: "bear",
            totalStars = totalStars,
            levelProgress = com.family.shizi.domain.engine.HonorLevels.progressFor(totalStars),
            unlockedBadgeIds = com.family.shizi.domain.engine.BadgeCatalog.unlockedIds(counts).toSet(),
        )
    } }

    fun selectAvatar(avatarId: String) = viewModelScope.launch {
        repository?.updateSettings { it.copy(avatarId = avatarId) }
        _state.value = _state.value.copy(avatarId = avatarId)
    }
}

@Composable
fun ProfileScreen() {
    val viewModel: ProfileViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val level = state.levelProgress
    val learnedProgress = (state.learnedCount.toFloat() / 50f).coerceIn(0f, 1f)
    ChildPage {
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("page_profile"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ChildTopBar("我的星球")
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("欢迎来到我的星球", style = MaterialTheme.typography.titleLarge)
                Image(
                    painter = painterResource(avatarDrawable(state.avatarId)),
                    contentDescription = avatarName(state.avatarId),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(108.dp).clip(CircleShape).padding(top = 8.dp),
                )
                Text("${state.nickname}，你真棒！", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineSmall)
                Text("Lv.${level.level} · ${level.title}", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium)
                Text("${state.totalStars} 颗成长星星", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (level.nextLevelThreshold > level.currentLevelStart)
                        "再收集 ${com.family.shizi.domain.engine.HonorLevels.starsToNext(state.totalStars)} 颗成长星星就升级啦"
                    else "已经到达最高等级，真了不起！",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { level.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("profile_level_progress"),
                )
                Text(
                    "Lv.${level.level} 起点 ${level.currentLevelStart} 颗 · 下一级 ${level.nextLevelThreshold} 颗",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text("选择一个小伙伴", modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatarOptions.forEach { (id, name) ->
                ProfileAvatarCard(id = id, name = name, selected = state.avatarId == id, onClick = { viewModel.selectAvatar(id) })
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("我的成长小记录", style = MaterialTheme.typography.titleLarge)
                Text("认识了 ${state.learnedCount} 个字宝宝", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                Text("掌握了 ${state.masteredCount} 个字", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
                Text("陪伴学习 ${state.learningDays} 天", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
                Text("每天认识 ${state.dailyTarget} 个新朋友", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(
                    progress = { learnedProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).testTag("profile_learned_progress"),
                )
                Text("50 个字宝宝的星球旅程", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("我的星星徽章", modifier = Modifier.padding(top = 28.dp), style = MaterialTheme.typography.titleLarge)
        val catalog = com.family.shizi.domain.engine.BadgeCatalog.all
        catalog.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { badge ->
                    val unlocked = badge.id in state.unlockedBadgeIds
                    VisualBadgeCard(
                        title = badge.title,
                        detail = if (badge.id in state.awaitingBadgeIds) "等待点亮" else badge.detail,
                        unlocked = unlocked,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
      }
    }
}

@Composable
private fun ProfileAvatarCard(id: String, name: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(width = 86.dp, height = 104.dp).clip(MaterialTheme.shapes.large),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(6.dp)) {
            Image(
                painter = painterResource(avatarDrawable(id)),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(62.dp).clip(CircleShape),
            )
            Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun VisualBadgeCard(title: String, detail: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (unlocked) DecorativeStar(modifier = Modifier.size(26.dp), color = MaterialTheme.colorScheme.primary)
                else Text("等待", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(if (unlocked) "已点亮" else detail, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

private val avatarOptions = listOf(
    "bear" to "小熊", "rabbit" to "小兔", "fox" to "小狐狸", "cat" to "小猫",
    "dog" to "小狗", "panda" to "熊猫", "dino" to "小恐龙", "lion" to "小狮子",
)

private fun avatarDrawable(id: String): Int = when (id) {
    "rabbit" -> R.drawable.avatar_rabbit
    "fox" -> R.drawable.avatar_fox
    "cat" -> R.drawable.avatar_cat
    "dog" -> R.drawable.avatar_dog
    "panda" -> R.drawable.avatar_panda
    "dino" -> R.drawable.avatar_dino
    "lion" -> R.drawable.avatar_lion
    else -> R.drawable.avatar_bear
}

private fun avatarName(id: String): String = when (id) {
    "rabbit" -> "小兔"
    "fox" -> "小狐狸"
    "cat" -> "小猫"
    "dog" -> "小狗"
    "panda" -> "熊猫"
    "dino" -> "小恐龙"
    "lion" -> "小狮子"
    else -> "小熊"
}
