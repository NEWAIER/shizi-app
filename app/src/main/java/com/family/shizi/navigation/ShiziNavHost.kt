package com.family.shizi.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.family.shizi.ui.home.HomeScreen
import com.family.shizi.ui.learn.LearnScreen
import com.family.shizi.ui.parent.ParentScreen
import com.family.shizi.ui.practice.PracticeScreen
import com.family.shizi.ui.result.ResultScreen
import com.family.shizi.ui.components.ChildBottomNavigation
import com.family.shizi.ui.learned.LearnedScreen
import com.family.shizi.ui.profile.ProfileScreen
import com.family.shizi.ui.stagetest.StageTestScreen

@Composable
fun ShiziNavHost() {
    val navController = rememberNavController()
    var parentAuthorizationToken by remember { mutableIntStateOf(0) }
    val navigate: (ShiziRoute) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(ShiziRoute.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    val navEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navEntry?.destination?.route
    val showChildTabs = currentRoute in setOf(
        ShiziRoute.Home.route, ShiziRoute.StageTest.route, ShiziRoute.Learned.route, ShiziRoute.Profile.route,
    )
    Scaffold(
        bottomBar = {
            if (showChildTabs) {
                ChildBottomNavigation(currentRoute) { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(ShiziRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        },
    ) { padding ->
    NavHost(
        navController = navController,
        startDestination = ShiziRoute.startDestination,
        modifier = androidx.compose.ui.Modifier.padding(padding),
    ) {
        composable(ShiziRoute.Home.route) {
            HomeScreen(
                onNavigate = navigate,
                onParentAuthorized = {
                    parentAuthorizationToken += 1
                    navigate(ShiziRoute.Parent)
                },
            )
        }
        composable(ShiziRoute.Learn.route) { LearnScreen(onNavigate = navigate) }
        composable(ShiziRoute.Practice.route) { PracticeScreen(onNavigate = navigate) }
        composable(ShiziRoute.Result.route) { ResultScreen(onNavigate = navigate) }
        composable(ShiziRoute.Parent.route) {
            val authorizationToken = parentAuthorizationToken
            ParentScreen(
                onNavigate = navigate,
                initiallyAdultVerified = authorizationToken > 0,
                onAuthorizationConsumed = {
                    if (parentAuthorizationToken == authorizationToken) {
                        parentAuthorizationToken = 0
                    }
                },
            )
        }
        composable(ShiziRoute.StageTest.route) { StageTestScreen(onNavigate = navigate) }
        composable(ShiziRoute.Learned.route) { LearnedScreen(onNavigate = navigate) }
        composable(ShiziRoute.Profile.route) { ProfileScreen() }
    }
    }
}
