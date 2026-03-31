package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mongle.android.domain.model.User
import com.mongle.android.ui.theme.MongleAccentOrange
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleTextPrimary
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// iOS monggleColor(for:) 순서와 동일: [Green, Yellow, Pink, Blue, Orange]
private val characterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMongglePink,
    MongleMonggleBlue,
    MongleMonggleOrange
)

@Composable
fun MongleCharacter(
    user: User,
    index: Int,
    size: Dp = 56.dp,
    hasAnswered: Boolean = false,
    showName: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bodyColor = characterColors[index % characterColors.size]
    val eyeSize = size * 0.14f
    val eyeOffset = size * 0.13f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = bodyColor.copy(alpha = 0.3f),
                    spotColor = bodyColor.copy(alpha = 0.3f)
                )
                .background(bodyColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 왼쪽 눈 (흰 테두리 + 검정 원)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = -eyeOffset)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }
            // 오른쪽 눈 (흰 테두리 + 검정 원)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = eyeOffset)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }

        }

        if (showName) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.role.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MongleCharacterAvatar(
    name: String,
    index: Int,
    size: Dp = 44.dp,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val bodyColor = color ?: characterColors[index % characterColors.size]
    val eyeSize = size * 0.14f
    val eyeOffset = size * 0.13f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = bodyColor.copy(alpha = 0.2f),
                spotColor = bodyColor.copy(alpha = 0.2f)
            )
            .background(bodyColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 왼쪽 눈 (흰 테두리)
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .offset(x = -eyeOffset)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(eyeSize)
                    .background(Color.Black, CircleShape)
            )
        }
        // 오른쪽 눈 (흰 테두리)
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .offset(x = eyeOffset)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(eyeSize)
                    .background(Color.Black, CircleShape)
            )
        }
    }
}

// ─── MongleSceneView ────────────────────────────────────────────────────────

/** 씬에 표시할 멤버 정보 */
data class SceneMemberInfo(
    val id: UUID,
    val name: String,
    val color: Color,
    val hasAnswered: Boolean
)

/** 씬 내부 상태 (mutable) */
private data class SceneMember(
    val id: UUID,
    val name: String,
    val color: Color,
    var hasAnswered: Boolean,
    var x: Float,
    var y: Float,
    var targetX: Float,
    var targetY: Float,
    var stepCount: Int = 0,
    var restFramesLeft: Int = 0,
    var overlapCounter: Int = 0
)

private fun randomInRange(min: Float, max: Float): Float =
    if (max > min) Random.nextFloat() * (max - min) + min else min

private fun initSceneMembers(
    members: List<SceneMemberInfo>,
    width: Float,
    height: Float,
    wallPadding: Float,
    collisionRadius: Float
): List<SceneMember> {
    val placed = mutableListOf<Pair<Float, Float>>()
    return members.map { info ->
        var px = randomInRange(wallPadding, width - wallPadding)
        var py = randomInRange(wallPadding, height - wallPadding)
        repeat(30) {
            val overlaps = placed.any { (ox, oy) -> hypot((px - ox), (py - oy)) < collisionRadius }
            if (!overlaps) return@repeat
            px = randomInRange(wallPadding, width - wallPadding)
            py = randomInRange(wallPadding, height - wallPadding)
        }
        placed.add(px to py)
        SceneMember(
            id = info.id,
            name = info.name,
            color = info.color,
            hasAnswered = info.hasAnswered,
            x = px, y = py,
            targetX = randomInRange(wallPadding, width - wallPadding),
            targetY = randomInRange(wallPadding, height - wallPadding)
        )
    }
}

/**
 * iOS MongleSceneView와 동일한 로직.
 * - animateFloatAsState로 부드러운 120ms 보간 이동 (iOS .animation(.linear, value: stepCount) 대응)
 * - 벽 충돌: 클램핑 + 새 목표 즉시 설정 (멈추지 않음)
 * - 캐릭터 충돌: overlapCounter >= 10 도달 시 새 방향 설정 (iOS 동일)
 */
@Composable
fun MongleSceneView(
    members: List<SceneMemberInfo>,
    currentUserId: UUID?,
    hasCurrentUserAnswered: Boolean,
    hasCurrentUserSkipped: Boolean = false,
    onViewAnswer: (SceneMemberInfo) -> Unit = {},
    onNudge: (SceneMemberInfo) -> Unit = {},
    onSelfTap: () -> Unit = {},
    onAnswerFirstToView: (String) -> Unit = {},
    onAnswerFirstToNudge: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // 화면 밀도에 맞는 실제 픽셀 크기 사용 (화면 밖 이탈 방지)
    val charSizePx = with(density) { 52.dp.toPx() }
    val collisionRadius = charSizePx * 1.5f
    val wallPadding = charSizePx * 0.9f
    val stepSize = with(density) { 1.8.dp.toPx() }
    val targetThreshold = charSizePx * 0.25f
    val overlapLimit = 10

    var sceneMembers by remember { mutableStateOf<List<SceneMember>>(emptyList()) }
    var sceneSize by remember { mutableStateOf(IntSize.Zero) }

    // 멤버 목록 변경 시 재초기화
    LaunchedEffect(members.map { it.id }) {
        val s = sceneSize
        if (s.width > 0 && s.height > 0) {
            sceneMembers = initSceneMembers(members, s.width.toFloat(), s.height.toFloat(), wallPadding, collisionRadius)
        }
    }

    // 답변 상태 변경 반영
    LaunchedEffect(members.map { it.hasAnswered }) {
        sceneMembers = sceneMembers.map { sm ->
            val updated = members.find { it.id == sm.id }
            if (updated != null && sm.hasAnswered != updated.hasAnswered)
                sm.copy(hasAnswered = updated.hasAnswered)
            else sm
        }
    }

    // 물리 루프 - iOS와 동일하게 120ms 간격
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            if (sceneMembers.isEmpty()) continue
            val s = sceneSize
            if (s.width <= 0 || s.height <= 0) continue
            val w = s.width.toFloat()
            val h = s.height.toFloat()
            val current = sceneMembers.toList()
            sceneMembers = current.mapIndexed { i, member ->
                // 휴식 중
                if (member.restFramesLeft > 0) {
                    val newRest = member.restFramesLeft - 1
                    return@mapIndexed if (newRest == 0) {
                        member.copy(
                            restFramesLeft = 0,
                            targetX = randomInRange(wallPadding, w - wallPadding),
                            targetY = randomInRange(wallPadding, h - wallPadding)
                        )
                    } else member.copy(restFramesLeft = newRest)
                }

                val dx = member.targetX - member.x
                val dy = member.targetY - member.y
                val dist = hypot(dx, dy)

                // 목표 도달: 휴식 또는 새 목표
                if (dist < targetThreshold) {
                    return@mapIndexed if (Random.nextBoolean()) {
                        member.copy(restFramesLeft = (10..50).random())
                    } else {
                        member.copy(
                            targetX = randomInRange(wallPadding, w - wallPadding),
                            targetY = randomInRange(wallPadding, h - wallPadding)
                        )
                    }
                }

                var newX = member.x + (dx / dist) * stepSize
                var newY = member.y + (dy / dist) * stepSize
                var newTargetX = member.targetX
                var newTargetY = member.targetY

                // iOS 벽 충돌: 클램핑 + 새 목표 (멈추지 않음, stepCount 증가)
                if (newX < wallPadding || newX > w - wallPadding ||
                    newY < wallPadding || newY > h - wallPadding
                ) {
                    newX = newX.coerceIn(wallPadding, w - wallPadding)
                    newY = newY.coerceIn(wallPadding, h - wallPadding)
                    newTargetX = randomInRange(wallPadding, w - wallPadding)
                    newTargetY = randomInRange(wallPadding, h - wallPadding)
                }

                // iOS 캐릭터 충돌: 위치 갱신 안 함, overlapCounter 증가
                val collides = current.indices.any { j ->
                    if (j == i) false
                    else hypot(newX - current[j].x, newY - current[j].y) < collisionRadius
                }
                if (collides) {
                    val newOverlap = member.overlapCounter + 1
                    return@mapIndexed if (newOverlap >= overlapLimit) {
                        member.copy(
                            overlapCounter = 0,
                            targetX = randomInRange(wallPadding, w - wallPadding),
                            targetY = randomInRange(wallPadding, h - wallPadding)
                        )
                    } else member.copy(overlapCounter = newOverlap)
                }

                member.copy(
                    x = newX, y = newY,
                    stepCount = member.stepCount + 1,
                    overlapCounter = 0,
                    targetX = newTargetX,
                    targetY = newTargetY
                )
            }
        }
    }

    Box(
        modifier = modifier.onSizeChanged { size ->
            sceneSize = size
            if (sceneMembers.isEmpty() && members.isNotEmpty()) {
                sceneMembers = initSceneMembers(
                    members, size.width.toFloat(), size.height.toFloat(), wallPadding, collisionRadius
                )
            }
        }
    ) {
        sceneMembers.forEach { member ->
            val info = members.find { it.id == member.id } ?: return@forEach
            key(member.id) {
                AnimatedSceneMemberBox(
                    member = member,
                    info = info,
                    charSizePx = charSizePx,
                    currentUserId = currentUserId,
                    hasCurrentUserAnswered = hasCurrentUserAnswered,
                    hasCurrentUserSkipped = hasCurrentUserSkipped,
                    onViewAnswer = onViewAnswer,
                    onNudge = onNudge,
                    onSelfTap = onSelfTap,
                    onAnswerFirstToView = onAnswerFirstToView,
                    onAnswerFirstToNudge = onAnswerFirstToNudge
                )
            }
        }
    }
}

/** 각 캐릭터를 부드럽게 보간 이동 - iOS .animation(.linear(duration: 0.12), value: stepCount) 대응 */
@Composable
private fun AnimatedSceneMemberBox(
    member: SceneMember,
    info: SceneMemberInfo,
    charSizePx: Float,
    currentUserId: UUID?,
    hasCurrentUserAnswered: Boolean,
    hasCurrentUserSkipped: Boolean,
    onViewAnswer: (SceneMemberInfo) -> Unit,
    onNudge: (SceneMemberInfo) -> Unit,
    onSelfTap: () -> Unit,
    onAnswerFirstToView: (String) -> Unit,
    onAnswerFirstToNudge: (String) -> Unit,
) {
    val animSpec = tween<Float>(durationMillis = 110, easing = LinearEasing)
    val animX by animateFloatAsState(targetValue = member.x, animationSpec = animSpec, label = "x")
    val animY by animateFloatAsState(targetValue = member.y, animationSpec = animSpec, label = "y")
    val hopTarget = (-abs(sin(member.stepCount * PI / 4.0)) * 14).toFloat()
    val animHop by animateFloatAsState(targetValue = hopTarget, animationSpec = animSpec, label = "hop")
    val half = (charSizePx / 2).roundToInt()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (animX - half).roundToInt(),
                    (animY + animHop - half).roundToInt()
                )
            }
            .clickable {
                val isCurrentUser = member.id == currentUserId
                if (isCurrentUser) { onSelfTap(); return@clickable }
                val canView = hasCurrentUserAnswered || hasCurrentUserSkipped
                when {
                    info.hasAnswered && canView -> onViewAnswer(info)
                    info.hasAnswered && !canView -> onAnswerFirstToView(member.name)
                    !info.hasAnswered && canView -> onNudge(info)
                    else -> onAnswerFirstToNudge(member.name)
                }
            }
    ) {
        SceneMongleItem(
            name = member.name,
            color = member.color,
            hasAnswered = member.hasAnswered,
            isCurrentUser = member.id == currentUserId,
            hasCurrentUserAnswered = hasCurrentUserAnswered,
            hasCurrentUserSkipped = hasCurrentUserSkipped
        )
    }
}

@Composable
private fun SceneMongleItem(
    name: String,
    color: Color,
    hasAnswered: Boolean,
    isCurrentUser: Boolean,
    hasCurrentUserAnswered: Boolean,
    hasCurrentUserSkipped: Boolean = false
) {
    val size = 52.dp
    val eyeSize = size * 0.14f
    val eyeOffset = size * 0.13f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 상태 배지
        if (isCurrentUser) {
            when {
                hasAnswered -> {
                    Row(
                        modifier = Modifier
                            .background(MonglePrimary.copy(alpha = 0.85f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = " 답변완료",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                hasCurrentUserSkipped -> {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF9C27B0).copy(alpha = 0.7f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = " 질문 넘김",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .background(MongleAccentOrange.copy(alpha = 0.85f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = " 답변하기",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            if (hasAnswered) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50).copy(alpha = 0.85f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = " 답변완료",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = " 미답변",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(size)
                .shadow(8.dp, CircleShape, ambientColor = color.copy(alpha = 0.3f), spotColor = color.copy(alpha = 0.3f))
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 왼쪽 눈 (흰 테두리 + 검정 원)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = -eyeOffset)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }
            // 오른쪽 눈 (흰 테두리 + 검정 원)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = eyeOffset)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = MongleTextPrimary
        )
    }
}
