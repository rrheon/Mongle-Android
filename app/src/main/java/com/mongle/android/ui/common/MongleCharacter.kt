package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.hypot
import kotlin.math.roundToInt
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

            // 답변 완료 시 초록 뺨 표시
            if (hasAnswered) {
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = -eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
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
    onMemberTapped: (SceneMemberInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val stepSize = 2f
    val collisionRadius = 76f
    val targetThreshold = 12f
    val wallPadding = 50f
    val charSizePx = 76f // 캐릭터 크기 (dp→px 근사, offset 계산용)

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

                if (newX < wallPadding || newX > w - wallPadding ||
                    newY < wallPadding || newY > h - wallPadding
                ) {
                    newX = newX.coerceIn(wallPadding, w - wallPadding)
                    newY = newY.coerceIn(wallPadding, h - wallPadding)
                    return@mapIndexed member.copy(
                        x = newX, y = newY,
                        targetX = randomInRange(wallPadding, w - wallPadding),
                        targetY = randomInRange(wallPadding, h - wallPadding)
                    )
                }

                val collides = current.indices.any { j ->
                    if (j == i) false
                    else hypot(newX - current[j].x, newY - current[j].y) < collisionRadius
                }
                if (collides) {
                    val newOverlap = member.overlapCounter + 1
                    return@mapIndexed if (newOverlap >= 10) {
                        member.copy(
                            overlapCounter = 0,
                            targetX = randomInRange(wallPadding, w - wallPadding),
                            targetY = randomInRange(wallPadding, h - wallPadding)
                        )
                    } else member.copy(overlapCounter = newOverlap)
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
            Box(
                modifier = Modifier
                    .offset { IntOffset((member.x.roundToInt() - half), (member.y.roundToInt() - half)) }
                    .clickable { onMemberTapped(info) }
            ) {
                SceneMongleItem(
                    name = member.name,
                    color = member.color,
                    hasAnswered = member.hasAnswered,
                    isCurrentUser = member.id == currentUserId,
                    hasCurrentUserAnswered = hasCurrentUserAnswered
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
    hasCurrentUserAnswered: Boolean
) {
    val size = 76.dp
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 답변 완료 뱃지
        if (hasAnswered) {
            Text(
                text = "✓ 답변하기",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MonglePrimary,
                modifier = Modifier
                    .background(MonglePrimaryLight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

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
            // 답변 완료 시 초록 뺨
            if (hasAnswered) {
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = -eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
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
