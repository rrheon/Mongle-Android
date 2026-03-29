package com.mongle.android.ui.groupselect

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleButtonStyle
import com.mongle.android.ui.common.MongleCard
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MongleCardHighlightLight
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun GroupSelectScreen(
    pendingInviteCode: String? = null,
    showGroupLeftToast: Boolean = false,
    onBack: (() -> Unit)? = null,
    onGroupSelected: (UUID) -> Unit,
    onCreatedOrJoined: () -> Unit,
    onPendingCodeConsumed: () -> Unit = {},
    viewModel: GroupSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    LaunchedEffect(showGroupLeftToast) {
        if (showGroupLeftToast) {
            snackbarHostState.showSnackbar("그룹에서 나왔어요")
        }
    }

    // Handle pending invite code from deep link
    LaunchedEffect(pendingInviteCode, uiState.step) {
        if (pendingInviteCode != null && uiState.step == GroupSelectStep.SELECT) {
            viewModel.goToJoin(prefillCode = pendingInviteCode)
            onPendingCodeConsumed()
        }
    }

    // Show error messages as snackbar
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.clearError()
        }
    }

    // Max groups alert dialog
    if (uiState.showMaxGroupsAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMaxGroupsAlert() },
            title = { Text("그룹 최대 개수 초과") },
            text = { Text("최대 3개의 그룹에만 참여할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissMaxGroupsAlert() }) {
                    Text("확인", color = MonglePrimary)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.step) {
            GroupSelectStep.SELECT -> SelectStep(
                groups = uiState.groups,
                isLoading = uiState.isLoading,
                onBack = onBack,
                onGroupSelected = onGroupSelected,
                onCreateClick = { viewModel.goToCreate() },
                onJoinClick = { viewModel.goToJoin() }
            )

            GroupSelectStep.CREATE -> CreateStep(
                groupName = uiState.groupName,
                nickname = uiState.nickname,
                groupNameError = uiState.groupNameError,
                nicknameError = uiState.nicknameError,
                isLoading = uiState.isLoading,
                onGroupNameChanged = viewModel::onGroupNameChanged,
                onNicknameChanged = viewModel::onNicknameChanged,
                onCreateClick = { viewModel.createGroup(onCreatedOrJoined) },
                onBack = { viewModel.goBack() }
            )

            GroupSelectStep.CREATED -> CreatedStep(
                inviteCode = uiState.inviteCode,
                onContinue = {
                    onCreatedOrJoined()
                },
                onCopied = {
                    scope.launch {
                        snackbarHostState.showSnackbar("초대 코드가 복사되었어요!")
                    }
                }
            )

            GroupSelectStep.JOIN -> JoinStep(
                joinCode = uiState.joinCode,
                nickname = uiState.nickname,
                joinCodeError = uiState.joinCodeError,
                nicknameError = uiState.nicknameError,
                isLoading = uiState.isLoading,
                onJoinCodeChanged = viewModel::onJoinCodeChanged,
                onNicknameChanged = viewModel::onNicknameChanged,
                onJoinClick = { viewModel.joinWithCode(onCreatedOrJoined) },
                onBack = { viewModel.goBack() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(MongleSpacing.md)
        )
    }
}

@Composable
private fun SelectStep(
    groups: List<MongleGroup>,
    isLoading: Boolean,
    onBack: (() -> Unit)? = null,
    onGroupSelected: (UUID) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 헤더 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MongleSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로",
                        tint = MongleTextPrimary
                    )
                }
            } else {
                Text(
                    text = "몽글",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MonglePrimary
                )
            }
            IconButton(onClick = { /* TODO: 알림 */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = MongleTextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MongleSpacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(MongleSpacing.xl))

        Text(
            text = "내 공간",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary
        )
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = "참여할 공간을 선택하거나 새로 만들어 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MongleSpacing.xl))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MonglePrimary)
            }
        } else if (groups.isEmpty()) {
            EmptyGroupsPlaceholder()
        } else {
            groups.forEach { group ->
                GroupCard(group = group, onClick = { onGroupSelected(group.id) })
                Spacer(modifier = Modifier.height(MongleSpacing.sm))
            }
        }

        Spacer(modifier = Modifier.height(MongleSpacing.xl))

        if (groups.size < 3) {
            MongleButton(
                text = "새 공간 만들기",
                onClick = onCreateClick,
                style = MongleButtonStyle.PRIMARY
            )
            Spacer(modifier = Modifier.height(MongleSpacing.sm))
            MongleButton(
                text = "초대 코드로 참여하기",
                onClick = onJoinClick,
                style = MongleButtonStyle.SECONDARY
            )
        } else {
            Text(
                text = "최대 3개의 공간에 참여할 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(MongleSpacing.xl))
        } // inner scrollable Column
    } // outer Column
}

@Composable
private fun EmptyGroupsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MongleRadius.xl))
            .background(MonglePrimaryLight)
            .padding(MongleSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = MonglePrimary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(MongleSpacing.sm))
        Text(
            text = "아직 참여한 공간이 없어요",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = "새 공간을 만들거나 초대 코드로 참여해 보세요",
            style = MaterialTheme.typography.bodySmall,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GroupCard(
    group: MongleGroup,
    onClick: () -> Unit
) {
    MongleCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MongleSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(MongleRadius.medium))
                    .background(MongleCardHighlightLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MonglePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(MongleSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "멤버 ${group.memberIds.size}명",
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextSecondary
                )
            }
        }
    }
}

@Composable
private fun CreateStep(
    groupName: String,
    nickname: String,
    groupNameError: Boolean,
    nicknameError: Boolean,
    isLoading: Boolean,
    onGroupNameChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MongleSpacing.md, start = MongleSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MongleTextPrimary
                )
            }
            Text(
                text = "새 공간 만들기",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MongleSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.md))

            Text(
                text = "우리 가족만의 공간 이름을 정해 보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(modifier = Modifier.height(MongleSpacing.sm))

            MongleTextField(
                value = groupName,
                onValueChange = onGroupNameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "공간 이름 (예: 우리 가족)",
                label = "공간 이름",
                isError = groupNameError,
                errorMessage = if (groupNameError) "공간 이름을 입력해 주세요" else null
            )

            MongleTextField(
                value = nickname,
                onValueChange = onNicknameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "이 공간에서 사용할 이름",
                label = "닉네임",
                isError = nicknameError,
                errorMessage = if (nicknameError) "닉네임을 입력해 주세요" else null
            )

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            MongleButton(
                text = "공간 만들기",
                onClick = onCreateClick,
                isLoading = isLoading,
                enabled = !isLoading,
                style = MongleButtonStyle.PRIMARY
            )

            Spacer(modifier = Modifier.height(MongleSpacing.xl))
        }
    }
}

@Composable
private fun CreatedStep(
    inviteCode: String,
    onContinue: () -> Unit,
    onCopied: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MongleSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "공간이 만들어졌어요!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = "초대 코드를 공유해서 가족을 초대해 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MongleSpacing.xxl))

        // Invite code card
        MongleCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MongleSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "초대 코드",
                    style = MaterialTheme.typography.labelMedium,
                    color = MongleTextSecondary
                )
                Spacer(modifier = Modifier.height(MongleSpacing.sm))
                Text(
                    text = inviteCode,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = MonglePrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MongleSpacing.md))
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(inviteCode))
                        onCopied()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "복사",
                        tint = MonglePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MongleSpacing.xs))
                    Text(text = "코드 복사", color = MonglePrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(MongleSpacing.xxl))

        MongleButton(
            text = "공간으로 이동",
            onClick = onContinue,
            style = MongleButtonStyle.PRIMARY
        )
    }
}

@Composable
private fun JoinStep(
    joinCode: String,
    nickname: String,
    joinCodeError: Boolean,
    nicknameError: Boolean,
    isLoading: Boolean,
    onJoinCodeChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onJoinClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MongleSpacing.md, start = MongleSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MongleTextPrimary
                )
            }
            Text(
                text = "초대 코드로 참여하기",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MongleSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(MongleSpacing.md))

            Text(
                text = "가족에게 받은 초대 코드를 입력해 주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(modifier = Modifier.height(MongleSpacing.sm))

            MongleTextField(
                value = joinCode,
                onValueChange = { onJoinCodeChanged(it.uppercase()) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "초대 코드 입력",
                label = "초대 코드",
                isError = joinCodeError,
                errorMessage = if (joinCodeError) "초대 코드를 입력해 주세요" else null
            )

            MongleTextField(
                value = nickname,
                onValueChange = onNicknameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "이 공간에서 사용할 이름",
                label = "닉네임",
                isError = nicknameError,
                errorMessage = if (nicknameError) "닉네임을 입력해 주세요" else null
            )

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            MongleButton(
                text = "참여하기",
                onClick = onJoinClick,
                isLoading = isLoading,
                enabled = !isLoading,
                style = MongleButtonStyle.PRIMARY
            )

            Spacer(modifier = Modifier.height(MongleSpacing.xl))
        }
    }
}

