package com.mongle.android.ui.settings

import com.ycompany.Monggle.BuildConfig
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.mongle.android.ui.common.AdBannerSection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mongle.android.ui.common.MonglePopup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import com.mongle.android.ui.common.MongleToastData
import com.mongle.android.ui.common.MongleToastHost
import com.mongle.android.ui.common.MongleToastType
import com.mongle.android.ui.common.defaultMessage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.mongle.android.ui.theme.pastelColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.ycompany.Monggle.R
import java.util.UUID

// ── 진입점 ──────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    currentUser: User?,
    loginProviderType: SocialProviderType?,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
    onGroupLeft: () -> Unit = {},
    familyId: java.util.UUID? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rawUiState by viewModel.uiState.collectAsState()
    // rootUiState 의 currentUser 를 즉시 반영 (LaunchedEffect 지연 없이)
    val uiState = if (currentUser != null && currentUser != rawUiState.currentUser) {
        rawUiState.copy(currentUser = currentUser)
    } else {
        rawUiState
    }
    val context = LocalContext.current
    val navController = rememberNavController()
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    LaunchedEffect(currentUser, loginProviderType, familyId) {
        viewModel.initialize(currentUser, loginProviderType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Logout -> onLogout()
                SettingsEvent.AccountDeleted -> onAccountDeleted()
                SettingsEvent.LeftGroup -> {
                    // GroupSelectScreen으로 직접 이동 (토스트는 GroupSelectScreen에서 표시)
                    onGroupLeft()
                }
                is SettingsEvent.CopiedInviteCode -> {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", event.code))
                    toastData = MongleToastData(
                        message = MongleToastType.INVITE_CODE_COPIED.defaultMessage,
                        type = MongleToastType.INVITE_CODE_COPIED
                    )
                }
                is SettingsEvent.LeaveTooSoon -> {
                    toastData = MongleToastData(
                        message = context.getString(R.string.group_leave_too_soon, event.daysLeft),
                        type = MongleToastType.LEAVE_TOO_SOON
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            val type = MongleToastType.fromErrorMessage(it)
            toastData = MongleToastData(
                message = if (type.defaultMessage.isNotEmpty()) type.defaultMessage else it,
                type = type
            )
            viewModel.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "my") {
            composable("my") {
                // UMP 개인정보 옵션 행 노출 여부 초기 체크 (화면 진입 시마다 재확인)
                val activity = LocalContext.current as? Activity
                LaunchedEffect(Unit) {
                    activity?.let { viewModel.refreshPrivacyOptionsVisibility(it) }
                }
                MyScreen(
                    uiState = uiState,
                    onProfileEditTapped = {
                        viewModel.onEditProfileTapped()
                        navController.navigate("profile_edit")
                    },
                    onNotificationsTapped = { navController.navigate("notifications") },
                    onGroupManagementTapped = { navController.navigate("group_management") },
                    onAccountManagementTapped = { navController.navigate("account_management") },
                    onPrivacyOptionsTapped = { act -> viewModel.onPrivacyOptionsTapped(act) }
                )
            }
            composable("profile_edit") {
                ProfileEditScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNameChanged = viewModel::onEditNameChanged,
                    onConfirm = {
                        val nameChanged = uiState.editName.trim() != uiState.currentUser?.name
                        if (!nameChanged || uiState.canChangeName) {
                            viewModel.onEditProfileConfirmed()
                            navController.popBackStack()
                        } else {
                            viewModel.onEditProfileConfirmed() // ViewModel에서 에러 메시지 설정
                        }
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
                LaunchedEffect(uiState.showTransferSheet) {
                    if (uiState.showTransferSheet) {
                        navController.navigate("transfer_admin")
                    }
                }
                GroupManagementScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onCopyInviteCode = viewModel::onCopyInviteCode,
                    onKickMemberTapped = viewModel::onKickMemberTapped,
                    onKickMemberConfirmed = viewModel::onKickMemberConfirmed,
                    onKickMemberCancelled = viewModel::onKickMemberCancelled,
                    onLeaveGroupTapped = viewModel::onLeaveGroupTapped,
                    onLeaveGroupFirstConfirmed = viewModel::onLeaveGroupFirstConfirmed,
                    onLeaveGroupConfirmed = viewModel::onLeaveGroupConfirmed,
                    onLeaveGroupFinalCancelled = viewModel::onLeaveGroupFinalCancelled,
                    onLeaveGroupCancelled = viewModel::onLeaveGroupCancelled
                )
            }
            composable("transfer_admin") {
                TransferAdminScreen(
                    uiState = uiState,
                    onBack = {
                        viewModel.onDismissTransferSheet()
                        navController.popBackStack()
                    },
                    onTransferMemberSelected = viewModel::onTransferMemberSelected,
                    onConfirmTransferAndLeave = viewModel::onConfirmTransferAndLeave
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
                    onDeleteAccountFirstConfirmed = viewModel::onDeleteAccountFirstConfirmed,
                    onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
                    onDeleteAccountFinalCancelled = viewModel::onDeleteAccountFinalCancelled,
                    onDeleteAccountCancelled = viewModel::onDeleteAccountCancelled
                )
            }
        }

        MongleToastHost(
            toastData = toastData,
            onDismiss = { toastData = null }
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
    onAccountManagementTapped: () -> Unit,
    onPrivacyOptionsTapped: (Activity) -> Unit = {}
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
                text = stringResource(R.string.settings_my),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

            // 광고 배너
            AdBannerSection(modifier = Modifier.padding(horizontal = 4.dp))

            // 프로필 섹션
            SettingsSection(
                title = stringResource(R.string.settings_profile),
                rows = listOf(
                    SettingsRowData(
                        icon = Icons.Default.Person,
                        iconBgColor = MongleMonggleGreenLight,
                        iconTint = Color.White,
                        title = stringResource(R.string.settings_profile_edit),
                        subtitle = stringResource(R.string.settings_profile_edit_desc),
                        onClick = onProfileEditTapped
                    )
                )
            )

            // 앱 설정 섹션
            SettingsSection(
                title = stringResource(R.string.settings_app_settings),
                rows = listOf(
                    SettingsRowData(
                        icon = Icons.Default.Notifications,
                        iconBgColor = MongleMonggleGreenLight,
                        iconTint = Color.White,
                        title = stringResource(R.string.settings_notifications),
                        subtitle = stringResource(R.string.settings_notifications_desc),
                        onClick = onNotificationsTapped
                    ),
                    SettingsRowData(
                        icon = Icons.Default.Group,
                        iconBgColor = MongleMonggleGreenLight,
                        iconTint = Color.White,
                        title = stringResource(R.string.settings_group),
                        subtitle = stringResource(R.string.settings_group_desc),
                        onClick = onGroupManagementTapped
                    ),
                    SettingsRowData(
                        icon = Icons.Default.Settings,
                        iconBgColor = MongleMonggleGreenLight,
                        iconTint = Color.White,
                        title = stringResource(R.string.settings_account),
                        subtitle = stringResource(R.string.settings_account_desc),
                        onClick = onAccountManagementTapped
                    )
                )
            )

            // 약관/개인정보 섹션 — 노션 페이지를 Custom Tab 으로 오픈
            run {
                val ctx = LocalContext.current
                val rows = buildList {
                    add(
                        SettingsRowData(
                            icon = Icons.Default.Description,
                            iconBgColor = MongleMonggleGreenLight,
                            iconTint = Color.White,
                            title = stringResource(R.string.settings_terms),
                            subtitle = "",
                            onClick = { openLegalUrl(ctx, com.mongle.android.ui.consent.LegalLinks.termsUrl()) }
                        )
                    )
                    add(
                        SettingsRowData(
                            icon = Icons.Default.Lock,
                            iconBgColor = MongleMonggleGreenLight,
                            iconTint = Color.White,
                            title = stringResource(R.string.settings_privacy),
                            subtitle = "",
                            onClick = { openLegalUrl(ctx, com.mongle.android.ui.consent.LegalLinks.privacyUrl()) }
                        )
                    )
                    // GDPR/CCPA 대상 사용자에게만 UMP 개인정보 옵션 행을 노출.
                    if (uiState.showPrivacyOptionsRow) {
                        add(
                            SettingsRowData(
                                icon = Icons.Default.Settings,
                                iconBgColor = MongleMonggleGreenLight,
                                iconTint = Color.White,
                                title = stringResource(R.string.settings_privacy_options),
                                subtitle = stringResource(R.string.settings_privacy_options_desc),
                                onClick = {
                                    (ctx as? Activity)?.let { activity ->
                                        onPrivacyOptionsTapped(activity)
                                    }
                                }
                            )
                        )
                    }
                }
                SettingsSection(
                    title = stringResource(R.string.settings_legal),
                    rows = rows
                )
            }

            // 버전
            Text(
                text = stringResource(R.string.settings_version, uiState.appVersion),
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
            title = stringResource(R.string.settings_profile_edit),
            onBack = onBack,
            rightContent = {
                TextButton(
                    onClick = onConfirm,
                    enabled = uiState.editName.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.common_save),
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
                MoodCircle(color = moodColorFor(uiState.currentUser?.moodId, uiState.currentUser?.role?.ordinal ?: 0), size = 80.dp)
                Text(
                    text = stringResource(R.string.settings_mood_hint),
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
                    text = stringResource(R.string.settings_name_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                MongleTextField(
                    value = uiState.editName,
                    onValueChange = if (uiState.canChangeName) onNameChanged else { _ -> },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.settings_name_placeholder),
                    enabled = uiState.canChangeName
                )
                Text(
                    text = if (uiState.canChangeName) stringResource(R.string.settings_name_hint)
                           else stringResource(R.string.settings_name_cooldown, uiState.daysUntilNameChange),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.canChangeName) MongleTextHint else pastelColor(0xFFFF6B6B)
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
        SettingsNavigationHeader(title = stringResource(R.string.notif_settings_title), onBack = onBack)

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
                sectionTitle = stringResource(R.string.notif_settings_answer),
                title = stringResource(R.string.notif_settings_answer_desc),
                subtitle = stringResource(R.string.notif_settings_answer_detail),
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsToggled
            )
            NotifToggleSection(
                sectionTitle = stringResource(R.string.notif_settings_nudge),
                title = stringResource(R.string.notif_settings_nudge_desc),
                subtitle = stringResource(R.string.notif_settings_nudge_detail),
                checked = uiState.notificationsEnabled,
                onCheckedChange = onNotificationsToggled
            )
            NotifToggleSection(
                sectionTitle = stringResource(R.string.notif_settings_question),
                title = stringResource(R.string.notif_settings_question_desc),
                subtitle = stringResource(R.string.notif_settings_question_detail),
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
                            text = stringResource(R.string.notif_settings_dnd),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MongleTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.perm_dnd_time),
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

// ── 방장 위임 화면 (Push) ────────────────────────────────────────────────────

@Composable
private fun TransferAdminScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onTransferMemberSelected: (UUID) -> Unit,
    onConfirmTransferAndLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SettingsNavigationHeader(
            title = stringResource(R.string.mgmt_transfer_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(MongleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.mgmt_transfer_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )
            val monggleColors = listOf(
                MongleMonggleGreenLight, MongleMonggleYellow, MongleMongglePink, MongleMonggleBlue, MongleMonggleOrange
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(uiState.transferCandidates) { index, member ->
                    val selected = uiState.selectedTransferMemberId == member.id
                    val bgColor = if (selected) MonglePrimaryLight else Color.White
                    val borderColor = if (selected) MonglePrimary else pastelColor(0xFFE0D8D0)
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
                            color = moodColorFor(member.moodId, index + 1),
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
                text = stringResource(R.string.mgmt_transfer_btn),
                onClick = onConfirmTransferAndLeave,
                enabled = uiState.selectedTransferMemberId != null,
                style = MongleButtonStyle.PRIMARY
            )
        }
    }
}

// ── 그룹 관리 화면 ──────────────────────────────────────────────────────────

@Composable
private fun GroupManagementScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onKickMemberTapped: (User) -> Unit,
    onKickMemberConfirmed: () -> Unit,
    onKickMemberCancelled: () -> Unit,
    onLeaveGroupTapped: () -> Unit,
    onLeaveGroupFirstConfirmed: () -> Unit,
    onLeaveGroupConfirmed: () -> Unit,
    onLeaveGroupFinalCancelled: () -> Unit,
    onLeaveGroupCancelled: () -> Unit
) {
    val context = LocalContext.current

    // 멤버 내보내기 확인 — iOS MonglePopupView 스타일
    if (uiState.showKickConfirmation) {
        Dialog(
            onDismissRequest = onKickMemberCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = uiState.kickTargetMember?.let { stringResource(R.string.mgmt_kick_title, it.name) } ?: stringResource(R.string.mgmt_kick_fallback_title),
                description = stringResource(R.string.mgmt_kick_desc),
                primaryLabel = stringResource(R.string.mgmt_kick_btn),
                onPrimary = onKickMemberConfirmed,
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = onKickMemberCancelled,
                isDestructive = true
            )
        }
    }

    // 그룹 나가기 1차 확인
    if (uiState.showLeaveGroupConfirmation) {
        Dialog(
            onDismissRequest = onLeaveGroupCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.mgmt_leave_title),
                description = stringResource(R.string.mgmt_leave_desc),
                primaryLabel = stringResource(R.string.mgmt_leave_btn),
                onPrimary = onLeaveGroupFirstConfirmed,
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = onLeaveGroupCancelled,
                isDestructive = true
            )
        }
    }

    // 그룹 나가기 2차 확인 (Context-aware final confirm)
    if (uiState.showLeaveGroupFinalConfirmation) {
        Dialog(
            onDismissRequest = onLeaveGroupFinalCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.group_leave_final_title),
                description = stringResource(R.string.group_leave_final_desc),
                primaryLabel = stringResource(R.string.group_leave_final_confirm),
                onPrimary = onLeaveGroupConfirmed,
                secondaryLabel = stringResource(R.string.group_leave_final_cancel),
                onSecondary = onLeaveGroupFinalCancelled,
                isDestructive = true
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SettingsNavigationHeader(title = stringResource(R.string.mgmt_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(MongleSpacing.md)
                .padding(bottom = MongleSpacing.md),
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
                        text = stringResource(R.string.settings_no_group),
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
                        val shareText = context.getString(R.string.mgmt_share_invite, uiState.family.inviteCode)
                        val shareIntent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
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
            }
        }

        // 그룹 나가기 버튼 — 하단 고정
        if (uiState.family != null) {
            MongleButton(
                text = stringResource(R.string.mgmt_leave),
                onClick = onLeaveGroupTapped,
                style = MongleButtonStyle.SECONDARY,
                modifier = Modifier.padding(MongleSpacing.md)
            )
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
        SectionHeader(title = stringResource(R.string.mgmt_invite_section), subtitle = stringResource(R.string.mgmt_invite_desc))

        // iOS: 두 InviteRow가 하나의 monglePanel 안에 VStack(spacing: sm)
        MonglePanel {
            Column(
                modifier = Modifier.padding(MongleSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
            ) {
                InviteRow(
                    title = stringResource(R.string.group_invite_code),
                    value = inviteCode,
                    buttonIcon = Icons.Default.ContentCopy,
                    buttonLabel = stringResource(R.string.common_copy),
                    onClick = onCopyCode
                )
                InviteRow(
                    title = stringResource(R.string.group_invite_link),
                    value = "${BuildConfig.BASE_URL}invite/$inviteCode",
                    buttonIcon = Icons.Default.Share,
                    buttonLabel = stringResource(R.string.common_share),
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
    ) {
        SectionHeader(title = stringResource(R.string.mgmt_members_title), subtitle = stringResource(R.string.mgmt_members_desc))

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
                        color = moodColorFor(member.moodId, index),
                        size = 40.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isCurrentUser) stringResource(R.string.mgmt_member_me, member.name) else member.name,
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
                                    text = stringResource(R.string.mgmt_owner),
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
                                text = stringResource(R.string.mgmt_kick_btn),
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
    onDeleteAccountFirstConfirmed: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onDeleteAccountFinalCancelled: () -> Unit,
    onDeleteAccountCancelled: () -> Unit
) {
    // 로그아웃 확인 다이얼로그
    if (uiState.showLogoutConfirmation) {
        Dialog(
            onDismissRequest = onLogoutCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.settings_logout),
                description = stringResource(R.string.settings_logout_confirm),
                primaryLabel = stringResource(R.string.settings_logout),
                onPrimary = onLogoutConfirmed,
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = onLogoutCancelled,
                isDestructive = true
            )
        }
    }

    // 계정 탈퇴 1차 확인 다이얼로그
    if (uiState.showDeleteAccountConfirmation) {
        Dialog(
            onDismissRequest = onDeleteAccountCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.settings_delete_account),
                description = stringResource(R.string.settings_delete_confirm),
                primaryLabel = stringResource(R.string.settings_delete_btn),
                onPrimary = onDeleteAccountFirstConfirmed,
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = onDeleteAccountCancelled,
                isDestructive = true
            )
        }
    }

    // 계정 탈퇴 2차 확인 다이얼로그 (Context-aware final confirm)
    if (uiState.showDeleteAccountFinalConfirmation) {
        Dialog(
            onDismissRequest = onDeleteAccountFinalCancelled,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.settings_delete_final_title),
                description = stringResource(R.string.settings_delete_final_desc),
                primaryLabel = stringResource(R.string.settings_delete_final_confirm),
                onPrimary = onDeleteAccountConfirmed,
                secondaryLabel = stringResource(R.string.settings_delete_final_cancel),
                onSecondary = onDeleteAccountFinalCancelled,
                isDestructive = true
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsNavigationHeader(title = stringResource(R.string.settings_account), onBack = onBack)

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
                    text = stringResource(R.string.settings_account_section),
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
                            iconBgColor = MongleMonggleGreenLight,
                            iconTint = Color.White,
                            title = stringResource(R.string.settings_logout),
                            subtitle = stringResource(R.string.settings_logout_desc),
                            onClick = onLogoutTapped
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            color = pastelColor(0xFFE0E0E0)
                        )
                        AccountRow(
                            icon = Icons.Default.Delete,
                            iconBgColor = MongleMonggleGreenLight,
                            iconTint = Color.White,
                            title = stringResource(R.string.settings_delete_account),
                            subtitle = stringResource(R.string.settings_delete_account_desc),
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
            .heightIn(min = 56.dp)
            .padding(vertical = MongleSpacing.sm),
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
                color = MongleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            .border(1.dp, pastelColor(0xFFE8E0D8), RoundedCornerShape(16.dp))
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
                contentDescription = stringResource(R.string.common_back),
                tint = MongleTextPrimary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        rightContent()
    }
}

// 기분 라벨 리소스 매핑
private fun moodLabelResIdFor(moodId: String?): Int? = when (moodId) {
    "happy" -> R.string.mood_happy
    "calm"  -> R.string.mood_calm
    "loved" -> R.string.mood_loved
    "sad"   -> R.string.mood_sad
    "tired" -> R.string.mood_tired
    else    -> null
}

private val characterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMongglePink,
    MongleMonggleBlue,
    MongleMonggleOrange
)

@Composable
private fun moodColorFor(moodId: String?, roleIndex: Int = 0) = when (moodId) {
    "happy" -> MongleMonggleYellow
    "calm"  -> MongleMonggleGreenLight
    "loved" -> MongleMongglePink
    "sad"   -> MongleMonggleBlue
    "tired" -> MongleMonggleOrange
    else    -> characterColors[roleIndex % characterColors.size]
}

// MongleMonggle 캐릭터 (iOS MongleMonggle 동일)
@Composable
private fun MoodCircle(color: Color, size: Dp) {
    val eyeSize = size * 0.18f
    val eyeHOffset = size * 0.144f
    val eyeVOffset = size * 0.07f
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
                .offset(x = -eyeHOffset, y = eyeVOffset)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(eyeSize).background(MongleTextPrimary, CircleShape))
        }
        Box(
            modifier = Modifier
                .size(eyeSize + 3.dp)
                .offset(x = eyeHOffset, y = eyeVOffset)
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
    val moodColor = moodColorFor(user.moodId, user.role.ordinal)
    val moodLabelResId = moodLabelResIdFor(user.moodId)

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
                if (moodLabelResId != null) {
                    Text(
                        text = stringResource(moodLabelResId),
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
                            color = pastelColor(0xFFE0E0E0)
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
            .heightIn(min = 56.dp)
            .padding(vertical = MongleSpacing.sm),
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
                color = MongleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

/**
 * 노션 약관 페이지를 Custom Tab 으로 오픈. Custom Tabs 미설치 환경은 일반 브라우저로 폴백.
 */
private fun openLegalUrl(context: Context, url: String) {
    runCatching {
        androidx.browser.customtabs.CustomTabsIntent.Builder()
            .build()
            .launchUrl(context, android.net.Uri.parse(url))
    }.onFailure {
        // Custom Tabs 미설치 시 일반 브라우저 폴백 (브라우저도 없으면 조용히 무시)
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
    }
}
