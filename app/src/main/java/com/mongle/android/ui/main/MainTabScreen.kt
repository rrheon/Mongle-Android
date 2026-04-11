package com.mongle.android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
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
import com.mongle.android.domain.model.User
import com.mongle.android.ui.history.HistoryScreen
import com.mongle.android.ui.history.HistoryViewModel
import com.mongle.android.ui.home.HomeScreen
import com.mongle.android.ui.home.HomeViewModel
import com.mongle.android.ui.root.RootUiState
import com.mongle.android.ui.search.SearchScreen
import com.mongle.android.ui.settings.SettingsScreen
import com.mongle.android.util.AdManager

enum class MainTab(val label: String) {
    HOME("HOME"),
    HISTORY("HISTORY"),
    SEARCH("SEARCH"),
    SETTINGS("MY")
}

@Composable
fun MainTabScreen(
    rootUiState: RootUiState,
    onNavigateToQuestionDetail: (Question) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToNudge: (User) -> Unit = {},
    onNavigateToWriteQuestion: () -> Unit = {},
    onNavigateToGroupSelect: () -> Unit = {},
    onGroupSelected: (java.util.UUID) -> Unit = {},
    onQuestionSkipped: (Int) -> Unit = {},
    onLogout: () -> Unit = {},
    onGroupLeft: () -> Unit = {},
    answerSubmittedCount: Int = 0,
    adManager: AdManager? = null
) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeViewModel: HomeViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()

    // 답변 제출 시 History 강제 새로고침
    LaunchedEffect(answerSubmittedCount) {
        if (answerSubmittedCount > 0) historyViewModel.refresh()
    }

    // 그룹 전환 시 History 데이터 새로고침
    LaunchedEffect(rootUiState.family?.id) {
        historyViewModel.refresh()
    }

    // 질문 넘기기 성공 시 RootViewModel에 전파
    LaunchedEffect(Unit) {
        homeViewModel.skipEvents.collect { heartsRemaining ->
            onQuestionSkipped(heartsRemaining)
        }
    }

    // RootViewModel 데이터를 HomeViewModel에 주입
    LaunchedEffect(rootUiState) {
        homeViewModel.initialize(
            todayQuestion = rootUiState.todayQuestion,
            lastQuestion = rootUiState.lastQuestion,
            familyTree = rootUiState.familyTree,
            family = rootUiState.family,
            familyMembers = rootUiState.familyMembers,
            allFamilies = rootUiState.allFamilies,
            currentUser = rootUiState.currentUser,
            hasAnsweredToday = rootUiState.hasAnsweredToday,
            hasSkippedToday = rootUiState.hasSkippedToday
        )
    }

    Scaffold(
        bottomBar = {
            val tabSelectedColor = Color(0xFF7CC8A0)
            val tabColors = NavigationBarItemDefaults.colors(
                selectedIconColor = tabSelectedColor,
                selectedTextColor = tabSelectedColor,
                indicatorColor = tabSelectedColor.copy(alpha = 0.12f),
                unselectedIconColor = Color(0xFF9E9E9E),
                unselectedTextColor = Color(0xFF9E9E9E)
            )
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "HOME") },
                    label = { Text("HOME", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = tabColors
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.HISTORY,
                    onClick = { selectedTab = MainTab.HISTORY },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "HISTORY") },
                    label = { Text("HISTORY", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = tabColors
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SEARCH,
                    onClick = { selectedTab = MainTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "SEARCH") },
                    label = { Text("SEARCH", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = tabColors
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Person, contentDescription = "MY") },
                    label = { Text("MY", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = tabColors
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    onNavigateToQuestionDetail = onNavigateToQuestionDetail,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToNudge = onNavigateToNudge,
                    onNavigateToWriteQuestion = onNavigateToWriteQuestion,
                    onNavigateToGroupSelect = onNavigateToGroupSelect,
                    onGroupSelected = onGroupSelected,
                    viewModel = homeViewModel,
                    adManager = adManager
                )
                MainTab.HISTORY -> HistoryScreen(
                    onNavigateToQuestionDetail = onNavigateToQuestionDetail,
                    viewModel = historyViewModel
                )
                MainTab.SEARCH -> SearchScreen(familyId = rootUiState.family?.id?.toString())
                MainTab.SETTINGS -> SettingsScreen(
                    currentUser = rootUiState.currentUser,
                    loginProviderType = null,
                    onLogout = onLogout,
                    onAccountDeleted = onLogout,
                    onGroupLeft = onGroupLeft,
                    familyId = rootUiState.family?.id
                )
            }
        }
    }
}
