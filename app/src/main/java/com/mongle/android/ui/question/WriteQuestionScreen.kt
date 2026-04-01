package com.mongle.android.ui.question

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleToast
import com.mongle.android.ui.common.MongleToastType
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.delay

@Composable
fun WriteQuestionScreen(
    onClose: () -> Unit,
    onQuestionSubmitted: (Question) -> Unit,
    viewModel: WriteQuestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WriteQuestionEvent.QuestionSubmitted -> onQuestionSubmitted(event.question)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastMessage = it
            viewModel.dismissError()
            delay(3000)
            toastMessage = null
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            // 네비게이션 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로",
                        tint = MongleTextPrimary
                    )
                }
                Text(
                    text = "질문 작성하기",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextPrimary
                )
            }

            HorizontalDivider(color = MongleBorder)

            // 스크롤 컨텐츠
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(MongleSpacing.md)
            ) {
                Spacer(modifier = Modifier.height(MongleSpacing.md))

                // 설명 섹션
                Text(
                    text = "나만의 질문을 작성해요",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextPrimary
                )
                Spacer(modifier = Modifier.height(MongleSpacing.xs))
                Text(
                    text = "작성한 질문은 오늘의 질문으로 등록돼요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                Spacer(modifier = Modifier.height(MongleSpacing.lg))

                // 텍스트 에디터 섹션
                Text(
                    text = "질문",
                    style = MaterialTheme.typography.labelMedium,
                    color = MongleTextHint
                )
                Spacer(modifier = Modifier.height(MongleSpacing.xs))

                OutlinedTextField(
                    value = uiState.questionText,
                    onValueChange = viewModel::onQuestionTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = {
                        Text(
                            text = "예) 오늘 하루 가장 기억에 남는 순간은 무엇인가요?",
                            color = MongleTextHint
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MonglePrimary,
                        unfocusedBorderColor = MongleBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${uiState.questionText.length} 자",
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextHint,
                        modifier = Modifier.padding(top = MongleSpacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(MongleSpacing.xl))
            }

            // 하단 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .imePadding()
            ) {
                HorizontalDivider(color = MongleBorder)
                MongleButton(
                    text = if (uiState.isSubmitting) "등록 중..." else "질문 등록하기",
                    onClick = viewModel::onSubmit,
                    enabled = uiState.canSubmit,
                    isLoading = uiState.isSubmitting,
                    modifier = Modifier.padding(MongleSpacing.md)
                )
            }
        }

        // 커스텀 에러 토스트
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
        ) {
            toastMessage?.let {
                MongleToast(message = it, type = MongleToastType.ERROR)
            }
        }
        }
    }
}
