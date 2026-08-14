package com.family.shizi.ui.profile

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.family.shizi.ShiziApplication
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.BadgeMilestone
import com.family.shizi.data.db.CharacterProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.family.shizi.ui.components.BadgeCard
import com.family.shizi.ui.components.ChildPage
import com.family.shizi.ui.components.ChildTopBar
import com.family.shizi.ui.components.AvatarCard

data class ProfileUiState(
    val loading: Boolean = true,
    val nickname: String = "小朋友",
    val learnedCount: Int = 0,
    val masteredCount: Int = 0,
    val learningDays: Int = 0,
    val dailyTarget: Int = 3,
    val badgeMilestones: List<BadgeMilestone> = emptyList(),
    val avatarId: String = "bear",
    val totalStars: Int = 0,
    val honorLevel: Int = 1,
    val nextLevelStars: Int = 50,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state
    init { viewModelScope.launch {
        val progress = repository?.getCharacterProgress().orEmpty()
        val settings = repository?.settings?.first()
        val content = ContentLoader.load(application)
        val learnedCount = progress.count { it.initialLessonCompleted }
        val masteredCount = progress.count { it.state.name.contains("MASTERED") }
        val learningDays = repository?.getCompletedLearningDayCount() ?: 0
        val totalStars = learnedCount * 10 + masteredCount * 5 + learningDays * 2
        val levels = listOf(0, 50, 120, 250, 450, 700)
        val levelIndex = levels.indexOfLast { totalStars >= it }.coerceAtLeast(0)
        _state.value = ProfileUiState(
            loading = false,
            nickname = settings?.nickname?.ifBlank { "小朋友" } ?: "小朋友",
            learnedCount = learnedCount,
            masteredCount = masteredCount,
            learningDays = learningDays,
            dailyTarget = settings?.dailyNewCharacterCount ?: 3,
            badgeMilestones = content.course.badgeMilestones,
            avatarId = settings?.avatarId ?: "bear",
            totalStars = totalStars,
            honorLevel = levelIndex + 1,
            nextLevelStars = levels.getOrNull(levelIndex + 1) ?: levels.last(),
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
    ChildPage {
      Column(
        modifier = Modifier.fillMaxSize().testTag("page_profile"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ChildTopBar("我的")
        AvatarCard(name = avatarName(state.avatarId), selected = true, onClick = {}, modifier = Modifier.padding(top = 8.dp))
        Text("${state.nickname}，你真棒！", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleLarge)
        Text("Lv.${state.honorLevel} · ${state.totalStars} 星星能量", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium)
        Text("距离下一等级还差 ${((state.nextLevelStars - state.totalStars).coerceAtLeast(0))} 颗星星", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("bear" to "小熊", "rabbit" to "小兔", "fox" to "小狐").forEach { (id, name) ->
                AvatarCard(name = name, selected = state.avatarId == id, onClick = { viewModel.selectAvatar(id) })
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("认识了 ${state.learnedCount} 个字", style = MaterialTheme.typography.titleLarge)
                Text("掌握了 ${state.masteredCount} 个字", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge)
                Text("坚持学习 ${state.learningDays} 天", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge)
                Text("今天最多认识 ${state.dailyTarget} 个新字", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Text("每次认真学习，都会点亮一颗小星星。", modifier = Modifier.padding(top = 18.dp), textAlign = TextAlign.Center)
            }
        }
        Text("我的徽章", modifier = Modifier.padding(top = 28.dp), style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.badgeMilestones) { milestone ->
                BadgeCard(milestone.title, milestone.detail, state.learnedCount >= milestone.learnedCount)
            }
        }
      }
    }
}

private fun avatarName(id: String): String = when (id) {
    "rabbit" -> "小兔"
    "fox" -> "小狐"
    else -> "小熊"
}
