package com.mongle.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleCharacterAvatar
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

private val BgStart = Color(0xFFFFF8F0)
private val BgMid   = Color(0xFFFFF2EB)
private val BgEnd   = Color(0xFFEFF8F1)

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
                SettingsEvent.LeftGroup -> onGroupLeft()
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

    // ── 다이얼로그들 ──

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

    // ── 메인 UI ──

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgStart, BgMid, BgEnd)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── 헤더 "설정" ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                Column(
                    modifier = Modifier.padding(horizontal = MongleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
                ) {
                    // ── 프로필 카드 ──
                    uiState.currentUser?.let { user ->
                        ProfileCard(
                            user = user,
                            userIndex = 0
                        )
                    }

                    // ── 프로필 설정 섹션 ──
                    SettingsSection(
                        title = "프로필",
                        rows = listOf(
                            SettingsRowData(
                                icon = Icons.Default.Settings,
                                iconBgColor = MonglePrimaryLight,
                                iconTint = MonglePrimary,
                                title = "프로필 편집",
                                subtitle = "이름과 역할을 변경할 수 있어요",
                                onClick = viewModel::onEditProfileTapped
                            )
                        )
                    )

                    // ── 알림 섹션 ──
                    SettingsSection(
                        title = "알림",
                        rows = listOf(
                            SettingsRowData(
                                icon = Icons.Default.Notifications,
                                iconBgColor = MonglePrimaryLight,
                                iconTint = MonglePrimary,
                                title = "알림 허용",
                                subtitle = "답변 알림, 리마인더",
                                trailing = {
                                    Switch(
                                        checked = uiState.notificationsEnabled,
                                        onCheckedChange = viewModel::onNotificationsToggled
                                    )
                                }
                            )
                        )
                    )

                    // ── 그룹 관리 섹션 ──
                    uiState.family?.let { family ->
                        SettingsSection(
                            title = "그룹 ${family.name}",
                            rows = buildList {
                                // 초대 코드 복사
                                add(
                                    SettingsRowData(
                                        icon = Icons.Default.ContentCopy,
                                        iconBgColor = Color(0xFFE3F2FD),
                                        iconTint = Color(0xFF1976D2),
                                        title = "초대 코드 복사",
                                        subtitle = family.inviteCode,
                                        onClick = viewModel::onCopyInviteCode
                                    )
                                )
                                // 멤버 목록 (방장이면 내보내기)
                                uiState.familyMembers.forEachIndexed { index, member ->
                                    val isCurrentUser = member.id == uiState.currentUser?.id
                                    add(
                                        SettingsRowData(
                                            icon = null,
                                            iconBgColor = Color.Transparent,
                                            iconTint = Color.Transparent,
                                            title = if (isCurrentUser) "${member.name} (나)" else member.name,
                                            subtitle = member.role.displayName,
                                            trailing = if (uiState.isOwner && !isCurrentUser) {
                                                {
                                                    IconButton(
                                                        onClick = { viewModel.onKickMember(member) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PersonRemove,
                                                            contentDescription = "내보내기",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            } else null,
                                            memberIndex = index
                                        )
                                    )
                                }
                                // 그룹 탈퇴
                                add(
                                    SettingsRowData(
                                        icon = Icons.Default.ExitToApp,
                                        iconBgColor = Color(0xFFFFEBEE),
                                        iconTint = MaterialTheme.colorScheme.error,
                                        title = "그룹 탈퇴",
                                        subtitle = "",
                                        isDestructive = true,
                                        onClick = viewModel::onLeaveGroupTapped
                                    )
                                )
                            }
                        )
                    }

                    // ── 계정 섹션 ──
                    SettingsSection(
                        title = "계정",
                        rows = listOf(
                            SettingsRowData(
                                icon = Icons.Default.PowerSettingsNew,
                                iconBgColor = Color(0xFFFFF3E0),
                                iconTint = Color(0xFFFF6D00),
                                title = "로그아웃",
                                subtitle = "",
                                onClick = viewModel::onLogoutTapped
                            ),
                            SettingsRowData(
                                icon = Icons.Default.Delete,
                                iconBgColor = Color(0xFFFFEBEE),
                                iconTint = MaterialTheme.colorScheme.error,
                                title = "계정 삭제",
                                subtitle = "",
                                isDestructive = true,
                                onClick = viewModel::onDeleteAccountTapped
                            )
                        )
                    )

                    // ── 버전 ──
                    Text(
                        text = "몽글 v${uiState.appVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MongleSpacing.sm, bottom = MongleSpacing.xl)
                    )
                }
            }
        }
    }
}

// ── 프로필 카드 ──

@Composable
private fun ProfileCard(user: User, userIndex: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(MongleSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MongleCharacterAvatar(
                name = user.name,
                index = userIndex,
                size = 52.dp
            )
            Spacer(modifier = Modifier.width(MongleSpacing.md))
            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                Text(
                    text = user.role.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextSecondary
                )
            }
        }
    }
}

// ── 설정 섹션 ──

private data class SettingsRowData(
    val icon: ImageVector?,
    val iconBgColor: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val isDestructive: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val memberIndex: Int = 0
)

@Composable
private fun SettingsSection(
    title: String,
    rows: List<SettingsRowData>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextSecondary,
            modifier = Modifier.padding(bottom = MongleSpacing.xs)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    SettingsRow(row = row)
                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = if (row.icon != null) 60.dp else 16.dp),
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(row: SettingsRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (row.onClick != null) Modifier.clickable { row.onClick.invoke() }
                else Modifier
            )
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        if (row.icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(row.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = row.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
        } else {
            // 멤버 아바타
            MongleCharacterAvatar(
                name = row.title,
                index = row.memberIndex,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
        }

        // 텍스트
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (row.isDestructive) MaterialTheme.colorScheme.error else MongleTextPrimary
            )
            if (row.subtitle.isNotEmpty()) {
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextHint
                )
            }
        }

        // trailing
        when {
            row.trailing != null -> row.trailing.invoke()
            row.onClick != null -> {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── 프로필 편집 BottomSheet ──

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "프로필 편집",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                    Text("저장", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(MongleSpacing.sm))

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

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
