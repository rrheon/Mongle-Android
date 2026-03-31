package com.mongle.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleButtonStyle
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MongleMoodCalm
import com.mongle.android.ui.theme.MongleMoodHappy
import com.mongle.android.ui.theme.MongleMoodLoved
import com.mongle.android.ui.theme.MongleMoodSad
import com.mongle.android.ui.theme.MongleMoodTired
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import java.util.UUID

// ── 진입점 ──────────────────────────────────────────────────────────────────

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
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUser, loginProviderType) {
        viewModel.initialize(currentUser, loginProviderType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Logout -> onLogout()
                SettingsEvent.AccountDeleted -> onAccountDeleted()
                SettingsEvent.LeftGroup -> {
                    navController.popBackStack("my", inclusive = false)
                    onGroupLeft()
                }
                is SettingsEvent.CopiedInviteCode -> {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", event.code))
                    snackbarHostState.showSnackbar("초대 코드가 복사되었습니다.")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "my") {
            composable("my") {
                MyScreen(
                    uiState = uiState,
                    onProfileEditTapped = {
                        viewModel.onEditProfileTapped()
                        navController.navigate("profile_edit")
                    },
                    onNotificationsTapped = { navController.navigate("notifications") },
                    onGroupManagementTapped = { navController.navigate("group_management") },
                    onAccountManagementTapped = { navController.navigate("account_management") }
                )
            }
            composable("profile_edit") {
                ProfileEditScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNameChanged = viewModel::onEditNameChanged,
                    onConfirm = {
                        viewModel.onEditProfileConfirmed()
                        navController.popBackStack()
                    }
                )
            }
            composable("notifications") {
                NotificationSettingsScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNotificationsToggled = viewModel::onNotificationsToggled
                )
            }
            composable("group_management") {
                GroupManagementScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onCopyInviteCode = viewModel::onCopyInviteCode,
                    onKickMemberTapped = viewModel::onKickMemberTapped,
                    onKickMemberConfirmed = viewModel::onKickMemberConfirmed,
                    onKickMemberCancelled = viewModel::onKickMemberCancelled,
                    onLeaveGroupTapped = viewModel::onLeaveGroupTapped,
                    onLeaveGroupConfirmed = viewModel::onLeaveGroupConfirmed,
                    onLeaveGroupCancelled = viewModel::onLeaveGroupCancelled,
                    onTransferMemberSelected = viewModel::onTransferMemberSelected,
                    onConfirmTransferAndLeave = viewModel::onConfirmTransferAndLeave,
                    onDismissTransferSheet = viewModel::onDismissTransferSheet
                )
            }
            composable("account_management") {
                AccountManagementScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onLogoutTapped = viewModel::onLogoutTapped,
                    onLogoutConfirmed = viewModel::onLogoutConfirmed,
                    onLogoutCancelled = viewModel::onLogoutCancelled,
                    onDeleteAccountTapped = viewModel::onDeleteAccountTapped,
                    onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
                    onDeleteAccountCancelled = viewModel::onDeleteAccountCancelled
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── MY 메인 화면 ─────────────────────────────────────────────────────────────

@Composable
private fun MyScreen(
    uiState: SettingsUiState,
    onProfileEditTapped: () -> Unit,
    onNotificationsTapped: () -> Unit,
    onGroupManagementTapped: () -> Unit,
    onAccountManagementTapped: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 헤더 "MY"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MongleSpacing.md)
                .padding(top = MongleSpacing.md, bottom = MongleSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.lg)
        ) {
            // 프로필 카드
            uiState.currentUser?.let { user ->
                MyProfileCard(user = user)
            }

            // 프로필 섹션
            SettingsSection(
                title = "프로필",
                rows = listOf(
                    SettingsRowData(
                        icon = Icons.Default.Person,
                        iconBgColor = MonglePrimaryLight,
                        iconTint = MonglePrimary,
                        title = "프로필 편집",
                        subtitle = "이름을 변경할 수 있어요",
                        onClick = onProfileEditTapped
                    )
                )
            )

            // 앱 설정 섹션
            SettingsSection(
                title = "앱 설정",
                rows = listOf(
                    SettingsRowData(
                        icon = Icons.Default.Notifications,
                        iconBgColor = MonglePrimaryLight,
                        iconTint = MonglePrimary,
                        title = "알림 설정",
                        subtitle = "답변 알림, 리마인더",
                        onClick = onNotificationsTapped
                    ),
                    SettingsRowData(
                        icon = Icons.Default.Group,
                        iconBgColor = MonglePrimaryLight,
                        iconTint = MonglePrimary,
                        title = "그룹 관리",
                        subtitle = "멤버 초대, 그룹 설정",
                        onClick = onGroupManagementTapped
                    ),
                    SettingsRowData(
                        icon = Icons.Default.Settings,
                        iconBgColor = MonglePrimaryLight,
                        iconTint = MonglePrimary,
                        title = "계정 관리",
                        subtitle = "로그아웃, 탈퇴",
                        onClick = onAccountManagementTapped
                    )
                )
            )

            // 버전
            Text(
                text = "몽글 v${uiState.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MongleSpacing.sm)
            )
        }
    }
}

// ── 프로필 편집 화면 ──────────────────────────────────────────────────────────

@Composable
private fun ProfileEditScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsNavigationHeader(
            title = "프로필 편집",
            onBack = onBack,
            rightContent = {
                TextButton(
                    onClick = onConfirm,
                    enabled = uiState.editName.isNotBlank()
                ) {
                    Text(
                        text = "저장",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (uiState.editName.isNotBlank()) MonglePrimary else MongleTextHint
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 아바타 섹션
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoodCircle(color = moodColorFor(uiState.currentUser?.moodId), size = 80.dp)
                Text(
                    text = "답변 수정 시 색상을 변경할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextHint
                )
            }

            // 이름 섹션
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "이름",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                MongleTextField(
                    value = uiState.editName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "이름 입력"
                )
                Text(
                    text = "다른 멤버에게 보여지는 이름이에요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextHint
                )
            }
        }
    }
}

// ── 알림 설정 화면 ────────────────────────────────────────────────────────────

@Composable
private fun NotificationSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNotificationsToggled: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsNavigationHeader(title = "알림 설정", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(MongleSpacing.md)
                .padding(bottom = MongleSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            // 각 토글 섹션 (iOS settingsSection 패턴 동일)
            NotifToggleSection(
                sectionTitle = "답변 알림",
                title = "멤버가 답변했을 때",
                subtitle = "가족이 오늘의 질문에 답변하면 알려드려요",
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsToggled
            )
            NotifToggleSection(
                sectionTitle = "재촉 알림",
                title = "재촉 알림을 받았을 때",
                subtitle = "가족이 답변을 재촉하면 알려드려요",
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsToggled
            )
            NotifToggleSection(
                sectionTitle = "시스템 알림",
                title = "새 질문 알림",
                subtitle = "새로운 오늘의 질문이 도착하면 알려드려요",
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsToggled
            )

            // 방해 금지 시간 — iOS와 동일: 단독 monglePanel 행
            MonglePanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "방해 금지 시간",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MongleTextPrimary
                        )
                        Text(
                            text = "오후 10:00 - 오전 8:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MongleTextSecondary
                        )
                    }
                    Switch(
                        checked = false,
                        onCheckedChange = {},
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MonglePrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifToggleSection(
    sectionTitle: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xs)) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextSecondary,
            modifier = Modifier.padding(horizontal = MongleSpacing.xxs)
        )
        MonglePanel {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MongleSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextSecondary
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MonglePrimary
                    )
                )
            }
        }
    }
}

// ── 그룹 관리 화면 ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupManagementScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onKickMemberTapped: (User) -> Unit,
    onKickMemberConfirmed: () -> Unit,
    onKickMemberCancelled: () -> Unit,
    onLeaveGroupTapped: () -> Unit,
    onLeaveGroupConfirmed: () -> Unit,
    onLeaveGroupCancelled: () -> Unit,
    onTransferMemberSelected: (UUID) -> Unit,
    onConfirmTransferAndLeave: () -> Unit,
    onDismissTransferSheet: () -> Unit
) {
    val context = LocalContext.current

    // 멤버 내보내기 확인 — iOS MonglePopupView 스타일
    if (uiState.showKickConfirmation) {
        AlertDialog(
            onDismissRequest = onKickMemberCancelled,
            title = {
                Text(
                    text = uiState.kickTargetMember?.let { "${it.name}님을 내보낼까요?" }
                        ?: "멤버 내보내기"
                )
            },
            text = { Text("해당 멤버는 그룹에서 제외됩니다.") },
            confirmButton = {
                TextButton(onClick = onKickMemberConfirmed) {
                    Text("내보내기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onKickMemberCancelled) { Text("취소") }
            }
        )
    }

    // 그룹 나가기 확인
    if (uiState.showLeaveGroupConfirmation) {
        AlertDialog(
            onDismissRequest = onLeaveGroupCancelled,
            title = { Text("그룹 나가기") },
            text = { Text("그룹을 나가면 모든 가족과의 답변 기록이 연결 해제됩니다.") },
            confirmButton = {
                TextButton(onClick = onLeaveGroupConfirmed) {
                    Text("나가기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onLeaveGroupCancelled) { Text("취소") }
            }
        )
    }

    // 방장 위임 시트
    if (uiState.showTransferSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissTransferSheet,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(MongleSpacing.md)
                    .padding(bottom = MongleSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
            ) {
                Text(
                    text = "그룹을 나가기 전에 방장을 위임할 멤버를 선택해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary
                )
                val monggleColors = listOf(
                    MongleMoodHappy, MongleMoodCalm, MongleMoodLoved, MongleMoodSad, MongleMoodTired
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(uiState.transferCandidates) { index, member ->
                        val selected = uiState.selectedTransferMemberId == member.id
                        val bgColor = if (selected) MonglePrimaryLight else Color.White
                        val borderColor = if (selected) MonglePrimary else Color(0xFFE0D8D0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = MongleSpacing.sm)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                .selectable(
                                    selected = selected,
                                    onClick = { onTransferMemberSelected(member.id) }
                                )
                                .padding(MongleSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md)
                        ) {
                            MoodCircle(
                                color = monggleColors[(index + 1) % monggleColors.size],
                                size = 40.dp
                            )
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                                color = MongleTextPrimary
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MonglePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                MongleButton(
                    text = "위임하고 나가기",
                    onClick = onConfirmTransferAndLeave,
                    enabled = uiState.selectedTransferMemberId != null,
                    style = MongleButtonStyle.PRIMARY
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsNavigationHeader(title = "그룹 관리", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(MongleSpacing.md)
                .padding(bottom = MongleSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            if (uiState.family == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MongleSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "현재 참여 중인 그룹이 없어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextHint
                    )
                }
            } else {
                // 그룹 초대 섹션 — iOS groupInfoSection 구조 동일
                GroupInfoSection(
                    inviteCode = uiState.family.inviteCode,
                    onCopyCode = onCopyInviteCode,
                    onShareLink = {
                        val shareIntent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "몽글에서 그룹을 만들었어요! 아래 링크로 들어오세요.\nhttps://mongle.app/invite/${uiState.family.inviteCode}"
                                )
                            }, null
                        )
                        context.startActivity(shareIntent)
                    }
                )

                // 멤버 섹션 — iOS membersSection 구조 동일
                MembersSection(
                    members = uiState.familyMembers,
                    currentUser = uiState.currentUser,
                    isOwner = uiState.isOwner,
                    onKickMemberTapped = onKickMemberTapped
                )

                // 그룹 나가기 버튼
                MongleButton(
                    text = "그룹 나가기",
                    onClick = onLeaveGroupTapped,
                    style = MongleButtonStyle.SECONDARY
                )
            }
        }
    }
}

// 그룹 초대 섹션 — iOS: 두 InviteRow를 하나의 monglePanel 안에 VStack으로
@Composable
private fun GroupInfoSection(
    inviteCode: String,
    onCopyCode: () -> Unit,
    onShareLink: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
    ) {
        SectionHeader(title = "그룹 초대", subtitle = "초대 코드나 링크를 공유해 가족을 초대하세요")

        // iOS: 두 InviteRow가 하나의 monglePanel 안에 VStack(spacing: sm)
        MonglePanel {
            Column(
                modifier = Modifier.padding(MongleSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
            ) {
                InviteRow(
                    title = "초대 코드",
                    value = inviteCode,
                    buttonIcon = Icons.Default.ContentCopy,
                    buttonLabel = "복사",
                    onClick = onCopyCode
                )
                InviteRow(
                    title = "초대 링크",
                    value = "https://mongle.app/invite/$inviteCode",
                    buttonIcon = Icons.Default.Share,
                    buttonLabel = "공유",
                    onClick = onShareLink
                )
            }
        }
    }
}

// 초대 행 — iOS MongleInviteRowView 동일 구조
@Composable
private fun InviteRow(
    title: String,
    value: String,
    buttonIcon: ImageVector,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MonglePrimaryLight.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(MongleSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MonglePrimary,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = MongleSpacing.sm, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                tint = MongleTextHint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextHint
            )
        }
    }
}

// 멤버 섹션 — iOS membersSection 구조 동일
@Composable
private fun MembersSection(
    members: List<User>,
    currentUser: User?,
    isOwner: Boolean,
    onKickMemberTapped: (User) -> Unit
) {
    if (members.isEmpty()) return

    val monggleColors = listOf(
        MongleMoodHappy, MongleMoodCalm, MongleMoodLoved, MongleMoodSad, MongleMoodTired
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
    ) {
        SectionHeader(title = "멤버", subtitle = "현재 이 공간에 연결된 사람들")

        // iOS: 각 멤버가 개별 monglePanel
        members.forEachIndexed { index, member ->
            val isCurrentUser = member.id == currentUser?.id
            val isOwnerMember = isCurrentUser && isOwner

            MonglePanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md)
                ) {
                    MoodCircle(
                        color = monggleColors[index % monggleColors.size],
                        size = 40.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isCurrentUser) "${member.name} (나)" else member.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MongleTextPrimary
                        )
                        // iOS: 방장은 primary bg 뱃지, 일반은 textSecondary 텍스트
                        if (isOwnerMember) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MonglePrimary)
                                    .padding(horizontal = MongleSpacing.xs, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "방장",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = member.role.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MongleTextSecondary
                            )
                        }
                    }

                    // 내보내기 버튼 — iOS: error text + error.opacity(0.1) bg + Capsule
                    if (isOwner && !isCurrentUser) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .clickable { onKickMemberTapped(member) }
                                .padding(horizontal = MongleSpacing.sm, vertical = MongleSpacing.xxs)
                        ) {
                            Text(
                                text = "내보내기",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 계정 관리 화면 ────────────────────────────────────────────────────────────

@Composable
private fun AccountManagementScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onLogoutTapped: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    onLogoutCancelled: () -> Unit,
    onDeleteAccountTapped: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onDeleteAccountCancelled: () -> Unit
) {
    // 로그아웃 확인 다이얼로그
    if (uiState.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = onLogoutCancelled,
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃할까요?") },
            confirmButton = {
                TextButton(onClick = onLogoutConfirmed) {
                    Text("로그아웃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutCancelled) { Text("취소") }
            }
        )
    }

    // 계정 탈퇴 확인 다이얼로그
    if (uiState.showDeleteAccountConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteAccountCancelled,
            title = { Text("계정 탈퇴") },
            text = { Text("탈퇴하면 모든 데이터가 삭제돼요.\n이 작업은 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = onDeleteAccountConfirmed) {
                    Text("탈퇴하기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteAccountCancelled) { Text("취소") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsNavigationHeader(title = "계정 관리", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MongleSpacing.md)
                .padding(top = MongleSpacing.md, bottom = MongleSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            // iOS AccountManagementView accountSection 구조 동일
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)) {
                Text(
                    text = "계정",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary,
                    modifier = Modifier.padding(horizontal = MongleSpacing.xxs)
                )
                // iOS: VStack(spacing: 0) + white bg + RoundedCornerShape
                // 두 행을 하나의 흰 패널에
                MonglePanel(padding = 0.dp) {
                    Column {
                        AccountRow(
                            icon = Icons.Default.Logout,
                            iconBgColor = MonglePrimaryLight,
                            iconTint = MonglePrimary,
                            title = "로그아웃",
                            subtitle = "기기에서 로그아웃해요",
                            onClick = onLogoutTapped
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            color = Color(0xFFE0E0E0)
                        )
                        AccountRow(
                            icon = Icons.Default.Delete,
                            iconBgColor = MonglePrimaryLight,
                            iconTint = MonglePrimary,
                            title = "계정 탈퇴",
                            subtitle = "모든 데이터가 삭제되며 복구할 수 없어요",
                            onClick = onDeleteAccountTapped
                        )
                    }
                }
            }
        }
    }
}

// 계정 관리 행 — iOS accountRow와 동일 구조
@Composable
private fun AccountRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = MongleSpacing.md)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MongleTextHint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── 공통 컴포넌트 ─────────────────────────────────────────────────────────────

// iOS monglePanel 스타일: 흰 배경 + 테두리 + 둥근 모서리 (elevation 없음)
@Composable
private fun MonglePanel(
    padding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8E0D8), RoundedCornerShape(16.dp))
            .then(if (padding > 0.dp) Modifier.padding(padding) else Modifier)
    ) {
        content()
    }
}

// iOS sectionTitle - title bold + subtitle hint
@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MongleTextSecondary
        )
    }
}

// 네비게이션 헤더 (iOS MongleNavigationHeader)
@Composable
private fun SettingsNavigationHeader(
    title: String,
    onBack: () -> Unit,
    rightContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로",
                tint = MongleTextPrimary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextPrimary,
            modifier = Modifier.weight(1f)
        )
        rightContent()
    }
}

// 기분 색상 매핑
private fun moodLabelFor(moodId: String?) = when (moodId) {
    "happy" -> "행복"
    "calm"  -> "평온"
    "loved" -> "사랑"
    "sad"   -> "슬픔"
    "tired" -> "피곤"
    else    -> null
}

@Composable
private fun moodColorFor(moodId: String?) = when (moodId) {
    "happy" -> MongleMoodHappy
    "calm"  -> MongleMoodCalm
    "loved" -> MongleMoodLoved
    "sad"   -> MongleMoodSad
    "tired" -> MongleMoodTired
    else    -> MongleMoodLoved
}

// MongleMonggle 캐릭터 (iOS MongleMonggle 동일)
@Composable
private fun MoodCircle(color: Color, size: Dp) {
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f
    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, CircleShape, ambientColor = color.copy(0.3f), spotColor = color.copy(0.3f))
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(eyeSize + 3.dp)
                .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(eyeSize).background(MongleTextPrimary, CircleShape))
        }
        Box(
            modifier = Modifier
                .size(eyeSize + 3.dp)
                .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(eyeSize).background(MongleTextPrimary, CircleShape))
        }
    }
}

// iOS HStack 스타일 프로필 카드 (monglePanel)
@Composable
private fun MyProfileCard(user: User) {
    val moodColor = moodColorFor(user.moodId)
    val moodLabel = moodLabelFor(user.moodId)

    MonglePanel(padding = MongleSpacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            MoodCircle(color = moodColor, size = 56.dp)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                if (moodLabel != null) {
                    Text(
                        text = moodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// iOS settingsSection 패턴: 섹션타이틀 + monglePanel 안에 행들
private data class SettingsRowData(
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun SettingsSection(
    title: String,
    rows: List<SettingsRowData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextSecondary,
            modifier = Modifier.padding(horizontal = MongleSpacing.xxs)
        )
        MonglePanel(padding = 0.dp) {
            Column {
                rows.forEachIndexed { index, row ->
                    SettingsRow(row = row)
                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
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
            .then(if (row.onClick != null) Modifier.clickable { row.onClick.invoke() } else Modifier)
            .padding(horizontal = MongleSpacing.md)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md)
    ) {
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
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextPrimary
            )
            if (row.subtitle.isNotEmpty()) {
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextHint
                )
            }
        }

        if (row.onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MongleTextHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
