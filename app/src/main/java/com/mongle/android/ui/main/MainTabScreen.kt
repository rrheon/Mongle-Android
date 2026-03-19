package com.mongle.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.ui.history.HistoryScreen
import com.mongle.android.ui.home.HomeScreen
import com.mongle.android.ui.home.HomeViewModel
import com.mongle.android.ui.root.RootUiState
import com.mongle.android.ui.settings.SettingsScreen

enum class MainTab(val label: String) {
    HOME("HOME"),
    HISTORY("HISTORY"),
    SETTINGS("MY")
}

@Composable
fun MainTabScreen(
    rootUiState: RootUiState,
    onNavigateToQuestionDetail: (Question) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onLogout: () -> Unit,
    onGroupLeft: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeViewModel: HomeViewModel = hiltViewModel()

    // RootViewModel 데이터를 HomeViewModel에 주입
    LaunchedEffect(rootUiState) {
        homeViewModel.initialize(
            todayQuestion = rootUiState.todayQuestion,
            familyTree = rootUiState.familyTree,
            family = rootUiState.family,
            familyMembers = rootUiState.familyMembers,
            currentUser = rootUiState.currentUser,
            hasAnsweredToday = rootUiState.hasAnsweredToday
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "HOME") },
                    label = { Text("HOME") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.HISTORY,
                    onClick = { selectedTab = MainTab.HISTORY },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "HISTORY") },
                    label = { Text("HISTORY") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Person, contentDescription = "MY") },
                    label = { Text("MY") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            MainTab.HOME -> HomeScreen(
                onNavigateToQuestionDetail = onNavigateToQuestionDetail,
                onNavigateToNotifications = onNavigateToNotifications,
                viewModel = homeViewModel
            )
            MainTab.HISTORY -> HistoryScreen(
                onNavigateToQuestionDetail = onNavigateToQuestionDetail
            )
            MainTab.SETTINGS -> SettingsScreen(
                currentUser = rootUiState.currentUser,
                loginProviderType = null,
                onLogout = onLogout,
                onAccountDeleted = onLogout,
                onGroupLeft = onGroupLeft
            )
        }
    }
}
