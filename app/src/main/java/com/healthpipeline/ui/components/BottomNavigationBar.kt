package com.healthpipeline.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Navigation items data class
data class NavigationItem(
    val route: String,
    val icon: String,
    val label: String
)

// Navigation items list
val navigationItems = listOf(
    NavigationItem("dashboard", "🏠", "Dashboard"),
    NavigationItem("data_selection", "📋", "Data"),
    NavigationItem("pipeline_status", "⚡", "Pipeline"),
    NavigationItem("analytics", "📊", "Analytics"),
    NavigationItem("settings", "⚙️", "Settings")
)

@Composable
fun BottomNavigationBar(
    navController: NavController
) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route
    
    NavigationBar {
        navigationItems.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Text(
                        text = item.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                label = { 
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
