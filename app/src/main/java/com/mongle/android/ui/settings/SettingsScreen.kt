package com.mongle.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.ui.theme.MongleSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: User?,
    loginProviderType: SocialProviderType?,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(currentUser, loginProviderType) {
        viewModel.initialize(currentUser, loginProviderType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Logout -> onLogout()
                SettingsEvent.AccountDeleted -> onAccountDeleted()
            }
        }
    }

    // 로그아웃 확인 다이얼로그
    if (uiState.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onLogoutCancelled,
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠어요?") },
            confirmButton = {
                TextButton(onClick = viewModel::onLogoutConfirmed) {
                    Text("로그아웃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLogoutCancelled) {
                    Text("취소")
                }
            }
        )
    }

    // 계정 삭제 확인 다이얼로그
    if (uiState.showDeleteAccountConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteAccountCancelled,
            title = { Text("계정 삭제") },
            text = { Text("계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 계속하시겠어요?") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteAccountConfirmed) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteAccountCancelled) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 프로필 섹션
            uiState.currentUser?.let { user ->
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                SectionHeader(title = "프로필")
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = user.name,
                    subtitle = "${user.email} · ${user.role.displayName}"
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "알림")
            ListItem(
                headlineContent = { Text("알림 허용") },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::onNotificationsToggled
                    )
                }
            )
            Divider()

            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "계정")
            SettingsItem(
                icon = Icons.Default.PowerSettingsNew,
                title = "로그아웃",
                onClick = viewModel::onLogoutTapped,
                isDestructive = false
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "계정 삭제",
                onClick = viewModel::onDeleteAccountTapped,
                isDestructive = true
            )
            Divider()

            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "앱 정보")
            SettingsItem(
                icon = null,
                title = "버전",
                subtitle = uiState.appVersion
            )
            Divider()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = MongleSpacing.md,
            vertical = MongleSpacing.xs
        )
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = if (onClick != null) {
            { Icon(Icons.Default.ChevronRight, contentDescription = null) }
        } else null,
        modifier = modifier
    )
}
