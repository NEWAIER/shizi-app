package com.family.shizi.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.ShiziApplication
import com.family.shizi.navigation.ShiziRoute
import com.family.shizi.ui.home.components.GrowthMap
import com.family.shizi.ui.home.components.ChildHud
import com.family.shizi.ui.home.components.ParentEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 地图本身就是首页。儿童侧不再显示任务卡、欢迎区或重复进度模块。
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
            modifier = Modifier.fillMaxSize(),
        )
        ChildHud(
            stars = state.totalStars,
            onOpenProfile = { onNavigate(ShiziRoute.Profile) },
            onOpenGallery = { onNavigate(ShiziRoute.Learned) },
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
        )
        Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 68.dp, end = 8.dp)) {
            ParentEntry(onParentAuthorized = onParentAuthorized)
        }
    }
}
