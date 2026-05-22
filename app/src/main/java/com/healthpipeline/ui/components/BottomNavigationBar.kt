package com.healthpipeline.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PHIABottomNavigation(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val navColor = Color(0xFF16181E)
    val accentColor = Color(0xFFFF6B6B)
    val inactiveColor = Color(0xFF6E7481)

    NavigationBar(
        containerColor = navColor,
        tonalElevation = 12.dp
    ) {
        val items = listOf(
            Triple("Home", "home", Icons.Default.Home),
            Triple("Activity", "activity", Icons.Default.List),
            Triple("Profile", "profile", Icons.Default.Person),
            Triple("Settings", "settings", Icons.Default.Settings)
        )

        items.forEach { (label, route, icon) ->
            NavigationBarItem(
                selected = currentScreen == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accentColor,
                    selectedTextColor = accentColor,
                    unselectedIconColor = inactiveColor,
                    unselectedTextColor = inactiveColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
