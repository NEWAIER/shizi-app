package com.family.shizi.ui.profile

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    val levelTitle: String = "字宝宝",
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
        val levelTitles = listOf("字宝宝", "识字小芽", "汉字朋友", "识字探险家", "汉字收藏家", "汉字达人")
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
            levelTitle = levelTitles[levelIndex.coerceIn(0, levelTitles.lastIndex)],
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
                AvatarCard(name = avatarName(state.avatarId), selected = true, onClick = {}, modifier = Modifier.padding(top = 12.dp))
                Text("${state.nickname}，你真棒！", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineSmall)
                Text("Lv.${state.honorLevel} · ${state.levelTitle}", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium)
                Text("${state.totalStars} 颗星星", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text("再收集 ${((state.nextLevelStars - state.totalStars).coerceAtLeast(0))} 颗星星就升级啦", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("选择一个小伙伴", modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatarOptions.forEach { (id, name) ->
                AvatarCard(name = name, selected = state.avatarId == id, onClick = { viewModel.selectAvatar(id) })
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("我的成长小记录", style = MaterialTheme.typography.titleLarge)
                Text("认识了 ${state.learnedCount} 个字宝宝", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                Text("掌握了 ${state.masteredCount} 个字", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
                Text("陪伴学习 ${state.learningDays} 天", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
                Text("每天认识 ${state.dailyTarget} 个新朋友", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text("我的星星徽章", modifier = Modifier.padding(top = 28.dp), style = MaterialTheme.typography.titleLarge)
        val badges = if (state.badgeMilestones.isEmpty()) childBadgeMilestones else state.badgeMilestones
        badges.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { milestone ->
                    BadgeCard(milestone.title, milestone.detail, state.learnedCount >= milestone.learnedCount, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
      }
    }
}

private val avatarOptions = listOf(
    "bear" to "小熊", "rabbit" to "小兔", "fox" to "小狐狸", "cat" to "小猫",
    "dog" to "小狗", "panda" to "熊猫", "dino" to "小恐龙", "lion" to "小狮子",
)

private val childBadgeMilestones = listOf(
    BadgeMilestone("first", "第一颗星", "第一次完成学习", 1),
    BadgeMilestone("three", "三字小芽", "认识3个字", 3),
    BadgeMilestone("five", "五字朋友", "认识5个字", 5),
    BadgeMilestone("ten", "十字花园", "认识10个字", 10),
    BadgeMilestone("twenty", "二十字树", "认识20个字", 20),
    BadgeMilestone("thirty", "三十字云朵", "认识30个字", 30),
    BadgeMilestone("forty", "四十字星球", "认识40个字", 40),
    BadgeMilestone("fifty", "五十字达人", "认识50个字", 50),
    BadgeMilestone("day1", "出发啦", "完成第1天学习", 1),
    BadgeMilestone("day3", "连续三天", "坚持3天", 3),
    BadgeMilestone("day7", "一周相伴", "坚持7天", 7),
    BadgeMilestone("day14", "两周闪耀", "坚持14天", 14),
    BadgeMilestone("review1", "老朋友你好", "完成第一次复习", 1),
    BadgeMilestone("review5", "复习小能手", "完成5次复习", 5),
    BadgeMilestone("challenge1", "勇敢挑战", "完成第一次挑战", 1),
    BadgeMilestone("challenge5", "挑战之星", "完成5次挑战", 5),
    BadgeMilestone("collect10", "小小收藏家", "收集10张字卡", 10),
    BadgeMilestone("collect25", "字卡花园", "收集25张字卡", 25),
    BadgeMilestone("collect40", "星球探险家", "收集40张字卡", 40),
    BadgeMilestone("collect50", "汉字收藏家", "收集50张字卡", 50),
)

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
