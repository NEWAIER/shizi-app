package com.family.shizi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.ShiziApplication
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.home.components.GrowthMap
import com.family.shizi.ui.home.components.HomeHero
import com.family.shizi.ui.home.components.HomeProgressSummary
import com.family.shizi.ui.home.components.ParentEntry
import com.family.shizi.ui.home.components.ProfileMenuCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 首页只负责组合：
 * HomeHero（欢迎与今日任务）+ GrowthMap（成长森林地图）+ ParentEntry（家长入口）。
 * 视觉细节全部下沉到 ui/home/components/。
 */
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
    val app = context.applicationContext as ShiziApplication
    val scope = rememberCoroutineScope()
    var profileMenuVisible by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        val childId = app.settingsStore.settings.first().testChildId
        app.repository?.recordUxEvent("HOME_OPEN", childId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            HomeHero(
                todayCharacter = state.todayCharacter,
                todayPinyin = state.todayPinyin,
                message = state.message,
                primaryAction = state.primaryAction,
                canStart = state.canStart,
                error = state.error,
                dueReviewCount = state.dueReviewCount,
                onPrimary = { viewModel.startOrContinue { onNavigate(it) } },
                onOpenProfile = { profileMenuVisible = !profileMenuVisible },
            )
            if (profileMenuVisible) {
                ProfileMenuCard(onOpenProfile = { profileMenuVisible = false; onNavigate(ShiziRoute.Profile) })
            }
            Text("我的成长森林", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleLarge)
            Text("毛毛虫每学会一个字，就吃掉一个果子。每 10 个字会长出一个树洞挑战", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            GrowthMap(
                characters = content.characters,
                learnedCount = state.learnedCount,
                dailyTarget = state.dailyNewTarget,
                onLearn = {
                    app.repository?.let { repo ->
                        scope.launch {
                            val childId = app.settingsStore.settings.first().testChildId
                            repo.recordUxEvent("MAP_NODE_CLICK", childId)
                        }
                    }
                    viewModel.startOrContinue { onNavigate(it) }
                },
                onOpenStageTest = onOpenStageTest,
            )
            Text("已认识 ${state.learnedCount} 个字宝宝", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (state.learnedCount / 50f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp).clip(RoundedCornerShape(99.dp)).testTag("home_progress"),
            )
            HomeProgressSummary(
                learnedCount = state.learnedCount,
                totalStars = state.totalStars,
                dailyTarget = state.dailyNewTarget,
            )
        }

        // 家长入口悬浮在右上角，避开儿童主区域。
        Box(
            modifier = Modifier.padding(end = 12.dp),
            contentAlignment = androidx.compose.ui.Alignment.TopEnd,
        ) {
            ParentEntry(onParentAuthorized = onParentAuthorized)
        }
    }
}
