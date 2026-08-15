package com.family.shizi.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.family.shizi.navigation.ShiziRoute

private data class ChildTab(val route: ShiziRoute, val label: String, val icon: String)

private val childTabs = listOf(
    ChildTab(ShiziRoute.Home, "学习", "学"),
    ChildTab(ShiziRoute.StageTest, "挑战", "挑"),
    ChildTab(ShiziRoute.Learned, "字卡", "卡"),
)

@Composable
fun ChildBottomNavigation(currentRoute: String?, onNavigate: (ShiziRoute) -> Unit) {
    NavigationBar(modifier = Modifier.testTag("child_bottom_navigation")) {
        childTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route.route,
                onClick = { onNavigate(tab.route) },
                icon = { Text(tab.icon) },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("child_tab_${tab.route.route}"),
            )
        }
    }
}
