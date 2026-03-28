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
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val characterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMonggleBlue,
    MongleMongglePink,
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
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

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
                    .size(eyeSize + 3.dp)
                    .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
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
                    .size(eyeSize + 3.dp)
                    .offset(x = eyeOffset, y = -eyeSize * 0.3f)
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
    modifier: Modifier = Modifier
) {
    val bodyColor = characterColors[index % characterColors.size]
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

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
                .size(eyeSize + 3.dp)
                .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
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
                .size(eyeSize + 3.dp)
                .offset(x = eyeOffset, y = -eyeSize * 0.3f)
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
 * iOS MongleSceneView와 동일한 로직:
 * 각 몽글 캐릭터가 화면 위를 자유롭게 floating하며 이동.
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
    val stepSize = 2f
    val collisionRadius = 60f
    val targetThreshold = 10f
    val wallPadding = 36f
    val charSizePx = 52f // 캐릭터 크기 (dp→px 근사, offset 계산용)

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

    // 애니메이션 루프 (iOS interval=0.12s)
    LaunchedEffect(sceneMembers.isNotEmpty()) {
        if (sceneMembers.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(120)
            val s = sceneSize
            if (s.width <= 0 || s.height <= 0) continue
            val w = s.width.toFloat()
            val h = s.height.toFloat()
            val current = sceneMembers.toList()
            sceneMembers = current.mapIndexed { i, m ->
                var member = m
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

                // 벽 충돌: 현재 위치 고정 + 잠시 멈춤 → 새 방향 선택
                if (newX < wallPadding || newX > w - wallPadding ||
                    newY < wallPadding || newY > h - wallPadding
                ) {
                    return@mapIndexed member.copy(
                        x = member.x.coerceIn(wallPadding, w - wallPadding),
                        y = member.y.coerceIn(wallPadding, h - wallPadding),
                        restFramesLeft = (3..6).random(),
                        overlapCounter = 0
                    )
                }

                // 캐릭터 충돌: 즉시 멈추고 잠시 휴식 → 새 방향 선택
                val collides = current.indices.any { j ->
                    if (j == i) false
                    else hypot(newX - current[j].x, newY - current[j].y) < collisionRadius
                }
                if (collides) {
                    return@mapIndexed member.copy(
                        restFramesLeft = (3..7).random(),
                        overlapCounter = 0
                    )
                }

                member.copy(x = newX, y = newY, stepCount = member.stepCount + 1, overlapCounter = 0)
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
            val half = (charSizePx / 2).roundToInt()
            val hopY = (-abs(sin(member.stepCount * PI / 5.0)) * 8).toFloat()
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (member.x - half).roundToInt(),
                            (member.y + hopY - half).roundToInt()
                        )
                    }
                    .clickable {
                        val id = member.id
                        val memberInfo = members.find { it.id == id } ?: return@clickable
                        val isCurrentUser = id == currentUserId
                        if (isCurrentUser) { onSelfTap(); return@clickable }
                        val canView = hasCurrentUserAnswered || hasCurrentUserSkipped
                        when {
                            memberInfo.hasAnswered && canView -> onViewAnswer(memberInfo)
                            memberInfo.hasAnswered && !canView -> onAnswerFirstToView(member.name)
                            !memberInfo.hasAnswered && canView -> onNudge(memberInfo)
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
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

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
                    .size(eyeSize + 3.dp)
                    .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
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
                    .size(eyeSize + 3.dp)
                    .offset(x = eyeOffset, y = -eyeSize * 0.3f)
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
