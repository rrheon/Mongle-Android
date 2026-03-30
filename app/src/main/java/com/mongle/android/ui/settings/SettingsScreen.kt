package com.mongle.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.FamilyRole
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
    onGroupLeft: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(currentUser, loginProviderType) {
        viewModel.initialize(currentUser, loginProviderType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Logout -> onLogout()
                SettingsEvent.AccountDeleted -> onAccountDeleted()
                SettingsEvent.LeftGroup -> {
                    onGroupLeft()
                }
                is SettingsEvent.CopiedInviteCode -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", event.code))
                    snackbarHostState.showSnackbar("초대 코드가 복사되었습니다.")
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // ── 다이얼로그들 ──────────────────────────────────────────

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
                TextButton(onClick = viewModel::onLogoutCancelled) { Text("취소") }
            }
        )
    }

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
                TextButton(onClick = viewModel::onDeleteAccountCancelled) { Text("취소") }
            }
        )
    }

    if (uiState.showLeaveGroupConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onLeaveGroupCancelled,
            title = { Text("그룹 탈퇴") },
            text = { Text("정말 그룹에서 탈퇴하시겠어요?") },
            confirmButton = {
                TextButton(onClick = viewModel::onLeaveGroupConfirmed) {
                    Text("탈퇴", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLeaveGroupCancelled) { Text("취소") }
            }
        )
    }

    if (uiState.showTransferSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissTransferSheet,
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(MongleSpacing.md)) {
                Text(
                    text = "방장 위임",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(MongleSpacing.xs))
                Text(
                    text = "방장을 위임할 멤버를 선택하세요. 위임 후 그룹에서 나갑니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                LazyColumn {
                    items(uiState.transferCandidates) { member ->
                        val selected = uiState.selectedTransferMemberId == member.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { viewModel.onTransferMemberSelected(member.id) }
                                )
                                .padding(vertical = MongleSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = { viewModel.onTransferMemberSelected(member.id) })
                            Text(
                                text = "${member.name} (${member.role.displayName})",
                                modifier = Modifier.padding(start = MongleSpacing.sm)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                TextButton(
                    onClick = viewModel::onConfirmTransferAndLeave,
                    enabled = uiState.selectedTransferMemberId != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("위임하고 나가기", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(MongleSpacing.sm))
            }
        }
    }

    if (uiState.showEditProfile) {
        ProfileEditBottomSheet(
            name = uiState.editName,
            role = uiState.editRole,
            onNameChanged = viewModel::onEditNameChanged,
            onRoleChanged = viewModel::onEditRoleChanged,
            onConfirm = viewModel::onEditProfileConfirmed,
            onDismiss = viewModel::onEditProfileCancelled
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 프로필 섹션 ──────────────────────────────────────────
            uiState.currentUser?.let { user ->
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                SectionHeader(title = "프로필")
                ListItem(
                    headlineContent = { Text(user.name, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("${user.email} · ${user.role.displayName}") },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = viewModel::onEditProfileTapped) {
                            Icon(Icons.Default.Edit, contentDescription = "프로필 편집")
                        }
                    }
                )
                HorizontalDivider()
            }

            // ── 그룹 관리 섹션 ──────────────────────────────────────────
            uiState.family?.let { family ->
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                SectionHeader(title = "그룹 관리")

                // 그룹명
                ListItem(
                    headlineContent = { Text(family.name, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("그룹 이름") },
                    leadingContent = { Icon(Icons.Default.Group, contentDescription = null) }
                )
                HorizontalDivider()

                // 초대 코드
                ListItem(
                    headlineContent = { Text("초대 코드") },
                    supportingContent = { Text(family.inviteCode, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = viewModel::onCopyInviteCode) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "복사")
                        }
                    }
                )
                HorizontalDivider()

                // 멤버 목록 (방장일 때 내보내기 버튼 표시)
                uiState.familyMembers.forEach { member ->
                    val isCurrentUser = member.id == uiState.currentUser?.id
                    ListItem(
                        headlineContent = {
                            Text(
                                text = if (isCurrentUser) "${member.name} (나)" else member.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(member.role.displayName) },
                        trailingContent = if (uiState.isOwner && !isCurrentUser) {
                            {
                                IconButton(onClick = { viewModel.onKickMember(member) }) {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = "내보내기",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else null
                    )
                    HorizontalDivider()
                }

                // 그룹 탈퇴
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "그룹 탈퇴",
                    onClick = viewModel::onLeaveGroupTapped,
                    isDestructive = true
                )
                HorizontalDivider()
            }

            // ── 알림 섹션 ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "알림")
            ListItem(
                headlineContent = { Text("알림 허용") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::onNotificationsToggled
                    )
                }
            )
            HorizontalDivider()

            // ── 계정 섹션 ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "계정")
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "로그아웃",
                onClick = viewModel::onLogoutTapped
            )
            HorizontalDivider()
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "계정 삭제",
                onClick = viewModel::onDeleteAccountTapped,
                isDestructive = true
            )
            HorizontalDivider()

            // ── 앱 정보 ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(MongleSpacing.md))
            SectionHeader(title = "앱 정보")
            SettingsItem(icon = null, title = "버전", subtitle = uiState.appVersion)
            HorizontalDivider()

            Spacer(modifier = Modifier.height(MongleSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditBottomSheet(
    name: String,
    role: FamilyRole,
    onNameChanged: (String) -> Unit,
    onRoleChanged: (FamilyRole) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var roleExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MongleSpacing.md)
                .padding(bottom = MongleSpacing.xl)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "프로필 편집",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = onConfirm,
                    enabled = name.isNotBlank()
                ) {
                    Text("저장", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // 이름 필드
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(MongleSpacing.sm))

            // 역할 선택
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }
            ) {
                OutlinedTextField(
                    value = role.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("역할") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    FamilyRole.entries.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.displayName) },
                            onClick = {
                                onRoleChanged(r)
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            // 취소 버튼
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable { onClick() }
        else Modifier.fillMaxWidth()
    )
}
