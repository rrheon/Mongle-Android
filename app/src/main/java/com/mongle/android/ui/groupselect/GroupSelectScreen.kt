package com.mongle.android.ui.groupselect

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mongle.android.ui.common.MonglePopup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.mongle.android.ui.common.MongleToastData
import com.mongle.android.ui.common.MongleToastHost
import com.mongle.android.ui.common.MongleToastType
import com.mongle.android.ui.common.defaultMessage
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions as KbOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleButtonStyle
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MongleBgNeutral
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MongleError
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.mongle.android.R
import java.util.UUID

// ─── Color data ──────────────────────────────────────────────────────────────

private data class MonggleColorOption(val id: String, val color: Color, val labelResId: Int)

private val monggleColorOptions = listOf(
    MonggleColorOption("calm",  MongleMonggleGreenLight, R.string.group_color_green),
    MonggleColorOption("happy", MongleMonggleYellow,     R.string.group_color_yellow),
    MonggleColorOption("loved", MongleMongglePink,       R.string.group_color_pink),
    MonggleColorOption("sad",   MongleMonggleBlue,       R.string.group_color_blue),
    MonggleColorOption("tired", MongleMonggleOrange,     R.string.group_color_orange),
)

private val fallbackMonggleColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMongglePink,
    MongleMonggleBlue,
    MongleMonggleOrange,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun GroupSelectScreen(
    pendingInviteCode: String? = null,
    showGroupLeftToast: Boolean = false,
    onBack: (() -> Unit)? = null,
    onGroupSelected: (UUID) -> Unit,
    onCreatedOrJoined: () -> Unit,
    onPendingCodeConsumed: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: GroupSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    // 화면 진입 시 이전 플로우 상태(CREATED 등) 초기화
    LaunchedEffect(Unit) { viewModel.resetToSelect() }

    // SELECT step에서 onBack이 제공된 경우 시스템 뒤로가기 연결
    if (onBack != null && uiState.step == GroupSelectStep.SELECT) {
        BackHandler { onBack() }
    }

    LaunchedEffect(Unit) { viewModel.loadGroups() }

    LaunchedEffect(showGroupLeftToast) {
        if (showGroupLeftToast) {
            toastData = MongleToastData(
                message = MongleToastType.GROUP_LEFT.defaultMessage,
                type = MongleToastType.GROUP_LEFT
            )
        }
    }

    LaunchedEffect(pendingInviteCode, uiState.step) {
        if (pendingInviteCode != null && uiState.step == GroupSelectStep.SELECT) {
            viewModel.goToJoin(prefillCode = pendingInviteCode)
            onPendingCodeConsumed()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            val msg = uiState.errorMessage!!
            val type = MongleToastType.fromErrorMessage(msg)
            toastData = MongleToastData(
                message = if (type.defaultMessage.isNotEmpty()) type.defaultMessage else msg,
                type = type
            )
            viewModel.clearError()
        }
    }

    if (uiState.showMaxGroupsAlert) {
        Dialog(
            onDismissRequest = { viewModel.dismissMaxGroupsAlert() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.group_max_title),
                description = stringResource(R.string.group_max_desc),
                primaryLabel = stringResource(R.string.common_confirm),
                onPrimary = { viewModel.dismissMaxGroupsAlert() }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.step) {
            GroupSelectStep.SELECT -> SelectStep(
                groups = uiState.groups,
                isLoading = uiState.isLoading,
                onBack = onBack,
                onGroupSelected = onGroupSelected,
                onCreateClick = { viewModel.goToCreate() },
                onJoinClick = { viewModel.goToJoin() },
                onNotificationClick = onNotificationClick
            )
            GroupSelectStep.CREATE -> CreateStep(
                groupName = uiState.groupName,
                nickname = uiState.nickname,
                selectedColorId = uiState.selectedColorId,
                groupNameError = uiState.groupNameError,
                nicknameError = uiState.nicknameError,
                isLoading = uiState.isLoading,
                onGroupNameChanged = viewModel::onGroupNameChanged,
                onNicknameChanged = viewModel::onNicknameChanged,
                onColorChanged = viewModel::onColorChanged,
                onCreateClick = { viewModel.createGroup(onCreatedOrJoined) },
                onBack = { viewModel.goBack() }
            )
            GroupSelectStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(
                onAllow = { viewModel.onNotificationPermissionAllowed() },
                onSkip = { viewModel.onNotificationPermissionSkipped() }
            )
            GroupSelectStep.QUIET_HOURS -> QuietHoursStep(
                onAccept = { viewModel.onQuietHoursAccepted() },
                onSkip = { viewModel.onQuietHoursSkipped() }
            )
            GroupSelectStep.CREATED -> CreatedStep(
                inviteCode = uiState.inviteCode,
                onContinue = { onCreatedOrJoined() },
                onCopied = {
                    toastData = MongleToastData(
                        message = MongleToastType.INVITE_CODE_COPIED.defaultMessage,
                        type = MongleToastType.INVITE_CODE_COPIED
                    )
                }
            )
            GroupSelectStep.JOIN -> JoinStep(
                joinCode = uiState.joinCode,
                nickname = uiState.nickname,
                selectedColorId = uiState.selectedColorId,
                joinCodeError = uiState.joinCodeError,
                nicknameError = uiState.nicknameError,
                isLoading = uiState.isLoading,
                onJoinCodeChanged = viewModel::onJoinCodeChanged,
                onNicknameChanged = viewModel::onNicknameChanged,
                onColorChanged = viewModel::onColorChanged,
                onJoinClick = { viewModel.joinWithCode(onCreatedOrJoined) },
                onBack = { viewModel.goBack() }
            )
        }

        MongleToastHost(
            toastData = toastData,
            onDismiss = { toastData = null }
        )
    }
}

// ─── MongleMonggle Circle ─────────────────────────────────────────────────────

@Composable
private fun MongleMonggleCircle(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape,
                ambientColor = color.copy(alpha = 0.25f),
                spotColor = color.copy(alpha = 0.25f))
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Left eye: white ring + black pupil
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .absoluteOffset(x = -eyeOffset, y = -(eyeSize * 0.3f))
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
        // Right eye
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .absoluteOffset(x = eyeOffset, y = -(eyeSize * 0.3f))
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
    }
}

// ─── Overlapping Monggle Row ──────────────────────────────────────────────────

@Composable
private fun OverlappingMonggleRow(colors: List<Color>, size: Dp = 36.dp) {
    val overlap = 10.dp
    val step = size - overlap
    val count = colors.size.coerceIn(1, 5)
    val totalWidth = size + step * (count - 1)

    Box(modifier = Modifier.width(totalWidth).height(size)) {
        colors.take(count).forEachIndexed { i, color ->
            Box(modifier = Modifier
                .absoluteOffset(x = step * i)
                .size(size)
                .zIndex((count - i).toFloat())
            ) {
                MongleMonggleCircle(color = color, size = size)
                // White separator border for overlap
                Box(modifier = Modifier.size(size).border(2.dp, Color.White, CircleShape))
            }
        }
    }
}

// ─── Monggle Color Picker ─────────────────────────────────────────────────────

@Composable
private fun MonggleColorPicker(selectedId: String, onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.xs)
    ) {
        Text(
            text = stringResource(R.string.group_color_label),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextSecondary
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            monggleColorOptions.forEach { option ->
                val isSelected = selectedId == option.id
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(), label = "scale_${option.id}"
                )
                // weight(1f)로 5개를 균등 배분 — 화면 너비에 상관없이 동일 간격
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(option.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .then(
                                if (isSelected) Modifier.border(3.dp, option.color, CircleShape)
                                else Modifier
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MongleMonggleCircle(
                            color = option.color,
                            size = 48.dp,
                            modifier = Modifier.scale(scale)
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.group_color_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MongleTextHint
        )
    }
}

// ─── Select Step ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectStep(
    groups: List<MongleGroup>,
    isLoading: Boolean,
    onBack: (() -> Unit)? = null,
    onGroupSelected: (UUID) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    var showActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header — iOS MongleNavigationHeader: 항상 "몽글" 타이틀
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MongleSpacing.md)
                .padding(top = MongleSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.group_mongle_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary
            )
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.home_notifications), tint = MongleTextPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MongleSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.lg)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.xs))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MonglePrimary)
                }
            } else if (groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.group_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MongleSpacing.lg)
                )
            } else {
                groups.forEach { group ->
                    MongleGroupCard(group = group, onClick = { onGroupSelected(group.id) })
                }
            }

            // New Space Button (single card → opens action sheet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MongleRadius.xl))
                    .background(Color.White)
                    .border(1.dp, MongleBorder, RoundedCornerShape(MongleRadius.xl))
                    .clickable { showActionSheet = true }
                    .padding(MongleSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MongleMonggleGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(MongleSpacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.group_create),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.group_join),
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.lg))
        }
    }

    // Action Sheet BottomSheet
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(bottom = MongleSpacing.xl)) {
                Text(
                    text = stringResource(R.string.group_what_to_do),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary,
                    modifier = Modifier.padding(
                        horizontal = MongleSpacing.lg,
                        vertical = MongleSpacing.md
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MongleSpacing.md)
                        .clip(RoundedCornerShape(MongleRadius.xl))
                        .background(Color.White)
                ) {
                    ActionSheetRow(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.group_create),
                        subtitle = stringResource(R.string.group_create_desc),
                        onClick = { showActionSheet = false; onCreateClick() }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        color = MongleBorder
                    )
                    ActionSheetRow(
                        icon = Icons.Default.PersonAdd,
                        title = stringResource(R.string.group_join),
                        subtitle = stringResource(R.string.group_join_desc),
                        onClick = { showActionSheet = false; onJoinClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MongleSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MongleMonggleGreenLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(MongleSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MongleTextSecondary)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null,
            tint = MongleTextHint, modifier = Modifier.size(20.dp))
    }
}

// ─── MongleGroupCard (iOS MongleCardGroup 스타일) ──────────────────────────────

@Composable
private fun moodColor(moodId: String?, fallback: Color): Color = when (moodId) {
    "happy" -> MongleMonggleYellow
    "calm" -> MongleMonggleGreenLight
    "loved" -> MongleMongglePink
    "sad" -> MongleMonggleBlue
    "tired" -> MongleMonggleOrange
    else -> fallback
}

@Composable
private fun MongleGroupCard(group: MongleGroup, onClick: () -> Unit) {
    val memberColors = if (group.memberIds.isEmpty()) {
        listOf(fallbackMonggleColors[0])
    } else {
        group.memberIds.mapIndexed { i, _ ->
            val fallback = fallbackMonggleColors[i % fallbackMonggleColors.size]
            val moodId = group.memberMoodIds.getOrNull(i)
            moodColor(moodId, fallback)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(MongleRadius.xl),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(Color.White, RoundedCornerShape(MongleRadius.xl))
            .border(1.dp, MongleBorder, RoundedCornerShape(MongleRadius.xl))
            .clickable(onClick = onClick)
            .padding(horizontal = MongleSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MongleTextPrimary
            )
            OverlappingMonggleRow(colors = memberColors)
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MongleTextHint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Create Step ──────────────────────────────────────────────────────────────

@Composable
private fun CreateStep(
    groupName: String,
    nickname: String,
    selectedColorId: String,
    groupNameError: Boolean,
    nicknameError: Boolean,
    isLoading: Boolean,
    onGroupNameChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MongleSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = MongleTextPrimary)
            }
            Text(
                text = stringResource(R.string.group_create_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
        }

        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MongleSpacing.md)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // Progress bar (step 1 of 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MongleSpacing.xs)
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(MongleRadius.full))
                        .background(MongleMonggleGreenLight)
                )
                Box(
                    modifier = Modifier.weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(MongleRadius.full))
                        .background(MongleMonggleGreenLight.copy(alpha = 0.3f))
                )
            }

            MongleLogo(
                size = MongleLogoSize.LARGE,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_create_headline),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                Text(
                    text = stringResource(R.string.group_create_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary
                )
            }

            // 공간 이름
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.group_name_label),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleTextSecondary
                    )
                    Text(
                        text = "${groupName.length}/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (groupName.length >= 10) MongleError else MongleTextHint
                    )
                }
                MongleTextField(
                    value = groupName,
                    onValueChange = { if (it.length <= 10) onGroupNameChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_name_placeholder),
                    isError = groupNameError,
                    errorMessage = if (groupNameError) stringResource(R.string.group_name_error) else null,
                    keyboardOptions = KbOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                if (!groupNameError) {
                    Text(
                        text = stringResource(R.string.group_name_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextHint
                    )
                }
            }

            // 닉네임
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_nickname_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary
                )
                MongleTextField(
                    value = nickname,
                    onValueChange = onNicknameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_nickname_placeholder),
                    isError = nicknameError,
                    errorMessage = if (nicknameError) stringResource(R.string.group_nickname_error) else null,
                    keyboardOptions = KbOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                if (!nicknameError) {
                    Text(
                        text = stringResource(R.string.group_nickname_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextHint
                    )
                }
            }

            MonggleColorPicker(selectedId = selectedColorId, onSelected = onColorChanged)

            Spacer(modifier = Modifier.height(MongleSpacing.md))
        }

        // Bottom CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md)
        ) {
            MongleButton(
                text = stringResource(R.string.group_create_btn),
                onClick = onCreateClick,
                isLoading = isLoading,
                enabled = !isLoading,
                style = MongleButtonStyle.PRIMARY
            )
        }
    }
}

// ─── Join Step ────────────────────────────────────────────────────────────────

@Composable
private fun JoinStep(
    joinCode: String,
    nickname: String,
    selectedColorId: String,
    joinCodeError: Boolean,
    nicknameError: Boolean,
    isLoading: Boolean,
    onJoinCodeChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onJoinClick: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MongleSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = MongleTextPrimary)
            }
            Text(
                text = stringResource(R.string.group_join_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MongleSpacing.md)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.md))

            MongleLogo(
                size = MongleLogoSize.LARGE,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_join_headline),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                Text(
                    text = stringResource(R.string.group_join_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary
                )
            }

            // 초대코드
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_code_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary
                )
                MongleTextField(
                    value = joinCode,
                    onValueChange = { onJoinCodeChanged(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_code_placeholder),
                    isError = joinCodeError,
                    errorMessage = if (joinCodeError) stringResource(R.string.group_code_error) else null,
                    keyboardOptions = KbOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                if (!joinCodeError) {
                    Text(
                        text = stringResource(R.string.group_code_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextHint
                    )
                }
            }

            // 닉네임
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_nickname_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary
                )
                MongleTextField(
                    value = nickname,
                    onValueChange = onNicknameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_nickname_placeholder),
                    isError = nicknameError,
                    errorMessage = if (nicknameError) stringResource(R.string.group_nickname_error) else null,
                    keyboardOptions = KbOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                if (!nicknameError) {
                    Text(
                        text = stringResource(R.string.group_nickname_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextHint
                    )
                }
            }

            MonggleColorPicker(selectedId = selectedColorId, onSelected = onColorChanged)

            Spacer(modifier = Modifier.height(MongleSpacing.md))
        }

        // Bottom CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md)
        ) {
            MongleButton(
                text = stringResource(R.string.group_join_btn),
                onClick = onJoinClick,
                isLoading = isLoading,
                enabled = !isLoading,
                style = MongleButtonStyle.PRIMARY
            )
        }
    }
}

// ─── Created Step ─────────────────────────────────────────────────────────────

@Composable
private fun CreatedStep(
    inviteCode: String,
    onContinue: () -> Unit,
    onCopied: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val inviteLink = "${BuildConfig.BASE_URL}invite/${inviteCode}"
    val shareText = context.getString(R.string.group_share_text, inviteCode, inviteLink)
    var codeCopied by remember { mutableStateOf(false) }
    var linkCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header (뒤로가기 없음)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MongleSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.group_create_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MongleSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // Progress bar (both filled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MongleSpacing.xs)
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(MongleRadius.full))
                        .background(MongleMonggleGreenLight)
                )
                Box(
                    modifier = Modifier.weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(MongleRadius.full))
                        .background(MongleMonggleGreenLight)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_created_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                Text(
                    text = stringResource(R.string.group_created_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary
                )
            }

            // Invite code row card
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_invite_code),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MongleRadius.large))
                        .background(Color.White)
                        .border(1.dp, MongleBorder, RoundedCornerShape(MongleRadius.large))
                        .padding(MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inviteCode,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MonglePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CopyPillButton(
                        isCopied = codeCopied,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inviteCode))
                            codeCopied = true
                            onCopied()
                        }
                    )
                }
            }

            // Invite link row card
            Column(verticalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.group_invite_link),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MongleRadius.large))
                        .background(Color.White)
                        .border(1.dp, MongleBorder, RoundedCornerShape(MongleRadius.large))
                        .padding(MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inviteLink,
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(MongleSpacing.sm))
                    CopyPillButton(
                        isCopied = linkCopied,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inviteLink))
                            linkCopied = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
        ) {
            // Share button (outline style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MongleRadius.full))
                    .border(1.2.dp, MonglePrimary, RoundedCornerShape(MongleRadius.full))
                    .clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, context.getString(R.string.common_share))
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MongleSpacing.xxs)
                ) {
                    Icon(
                        Icons.Default.Share, contentDescription = null,
                        tint = MonglePrimary, modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.common_share),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                }
            }
            MongleButton(text = stringResource(R.string.group_go_home), onClick = onContinue, style = MongleButtonStyle.PRIMARY)
        }
    }
}

@Composable
private fun CopyPillButton(isCopied: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MongleRadius.full))
            .background(if (isCopied) MongleMonggleGreenLight.copy(alpha = 0.8f) else MongleMonggleGreenLight)
            .clickable(onClick = onClick)
            .padding(horizontal = MongleSpacing.sm, vertical = MongleSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (isCopied) stringResource(R.string.common_copied) else stringResource(R.string.common_copy),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}

// ─── Notification Permission Step ────────────────────────────────────────────

@Composable
private fun NotificationPermissionStep(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 허용/거부 상관없이 다음 단계로 이동
        onAllow()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = MongleSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = stringResource(R.string.perm_notif_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.perm_notif_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Notification reason rows
        NotificationReasonRow(emoji = "\uD83D\uDCAC", text = stringResource(R.string.perm_notif_answer))
        Spacer(modifier = Modifier.height(16.dp))
        NotificationReasonRow(emoji = "\uD83D\uDC4B", text = stringResource(R.string.perm_notif_nudge))
        Spacer(modifier = Modifier.height(16.dp))
        NotificationReasonRow(emoji = "\u2753", text = stringResource(R.string.perm_notif_question))

        Spacer(modifier = Modifier.weight(1f))

        MongleButton(
            text = stringResource(R.string.perm_notif_allow),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Android 12 이하에서는 권한 요청 필요 없음
                    onAllow()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            style = MongleButtonStyle.PRIMARY
        )

        Spacer(modifier = Modifier.height(12.dp))

        MongleButton(
            text = stringResource(R.string.perm_notif_later),
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            style = MongleButtonStyle.SECONDARY
        )

        Spacer(modifier = Modifier.height(MongleSpacing.xl))
    }
}

@Composable
private fun NotificationReasonRow(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MongleBgNeutral,
                shape = RoundedCornerShape(MongleRadius.medium)
            )
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm + 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MongleTextPrimary
        )
    }
}

// ─── Quiet Hours Step ────────────────────────────────────────────────────────

@Composable
private fun QuietHoursStep(
    onAccept: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = MongleSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = stringResource(R.string.perm_dnd_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.perm_dnd_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Moon icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MongleBgNeutral,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83C\uDF19",
                fontSize = 56.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Time display
        Text(
            text = stringResource(R.string.perm_dnd_time),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        MongleButton(
            text = stringResource(R.string.perm_dnd_use),
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            style = MongleButtonStyle.PRIMARY
        )

        Spacer(modifier = Modifier.height(12.dp))

        MongleButton(
            text = stringResource(R.string.perm_dnd_skip),
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            style = MongleButtonStyle.SECONDARY
        )

        Spacer(modifier = Modifier.height(MongleSpacing.xl))
    }
}
