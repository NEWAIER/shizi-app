package com.family.shizi.ui.profile

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import com.family.shizi.data.db.CharacterProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val nickname: String = "小朋友",
    val learnedCount: Int = 0,
    val masteredCount: Int = 0,
    val learningDays: Int = 0,
    val dailyTarget: Int = 3,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ShiziApplication).repository
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state
    init { viewModelScope.launch {
        val progress = repository?.getCharacterProgress().orEmpty()
        val settings = repository?.settings?.first()
        _state.value = ProfileUiState(
            loading = false,
            nickname = settings?.nickname?.ifBlank { "小朋友" } ?: "小朋友",
            learnedCount = progress.count { it.initialLessonCompleted },
            masteredCount = progress.count { it.state.name.contains("MASTERED") },
            learningDays = repository?.getCompletedLearningDayCount() ?: 0,
            dailyTarget = settings?.dailyNewCharacterCount ?: 3,
        )
    } }
}

@Composable
fun ProfileScreen() {
    val viewModel: ProfileViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp).testTag("page_profile"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("我的小星球", style = MaterialTheme.typography.headlineMedium)
        Text("${state.nickname}，你真棒！", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐ 认识了 ${state.learnedCount} 个字", style = MaterialTheme.typography.titleLarge)
                Text("🏅 掌握了 ${state.masteredCount} 个字", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge)
                Text("📅 坚持学习 ${state.learningDays} 天", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge)
                Text("今天最多认识 ${state.dailyTarget} 个新字", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Text("每次认真学习，都会点亮一颗小星星。", modifier = Modifier.padding(top = 18.dp), textAlign = TextAlign.Center)
            }
        }
        Text("我的徽章", modifier = Modifier.padding(top = 28.dp), style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AchievementBadge("启蒙星", "认识第一个字", state.learnedCount >= 1)
            AchievementBadge("三字小能手", "认识3个字", state.learnedCount >= 3)
            AchievementBadge("五字达人", "认识5个字", state.learnedCount >= 5)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AchievementBadge(title: String, detail: String, unlocked: Boolean) {
    Card(modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (unlocked) "🏆" else "☆", style = MaterialTheme.typography.headlineSmall)
            Text(title, modifier = Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Text(if (unlocked) "已点亮" else detail, modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
