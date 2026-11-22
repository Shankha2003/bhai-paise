package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.ExpenseCategory
import com.example.data.model.Member
import com.example.data.model.PastelTone
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainTab

enum class MascotMood {
    HAPPY,
    CALM,
    EXCITED,
}

@Composable
fun pastelColorsFor(tone: PastelTone): Pair<Color, Color> {
    val c = SliceTheme.colors
    return when (tone) {
        PastelTone.LAVENDER -> c.lavenderFill to c.lavenderInk
        PastelTone.MINT -> c.mintFill to c.mintInk
        PastelTone.CORAL -> c.coralFill to c.coralInk
        PastelTone.SKY -> c.skyFill to c.skyInk
        PastelTone.AMBER -> c.amberFill to c.amberInk
    }
}

@Composable
fun SliceSplitMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val c = SliceTheme.colors
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = minOf(w, h) * 0.42f
        val topLeft1 = Offset(w * 0.5f - radius - w * 0.03f, h * 0.5f - radius - h * 0.03f)
        drawArc(
            color = c.lavenderFill,
            startAngle = 155f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = topLeft1,
            size = Size(radius * 2f, radius * 2f),
        )
        val topLeft2 = Offset(w * 0.5f - radius + w * 0.03f, h * 0.5f - radius + h * 0.03f)
        drawArc(
            color = c.mintFill,
            startAngle = 335f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = topLeft2,
            size = Size(radius * 2f, radius * 2f),
        )
    }
}

@Composable
fun MascotBadge(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.CALM,
    tone: PastelTone = PastelTone.LAVENDER,
    size: Dp = 56.dp,
) {
    val (fill, ink) = pastelColorsFor(tone)
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "SliceSplit friendly status badge" },
    ) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = fill, radius = minOf(w, h) / 2f)

        val strokeWidth = w * 0.055f
        val eyeY = h * 0.43f
        val leftEyeX = w * 0.36f
        val rightEyeX = w * 0.64f

        when (mood) {
            MascotMood.CALM -> {
                val dotRadius = w * 0.042f
                drawCircle(color = ink, radius = dotRadius, center = Offset(leftEyeX, eyeY))
                drawCircle(color = ink, radius = dotRadius, center = Offset(rightEyeX, eyeY))
                drawArc(
                    color = ink,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.38f, h * 0.48f),
                    size = Size(w * 0.24f, h * 0.16f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            MascotMood.HAPPY, MascotMood.EXCITED -> {
                // 2-line smiling eyes
                drawArc(
                    color = ink,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(leftEyeX - w * 0.08f, eyeY - h * 0.04f),
                    size = Size(w * 0.16f, h * 0.11f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = ink,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - w * 0.08f, eyeY - h * 0.04f),
                    size = Size(w * 0.16f, h * 0.11f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = ink,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(w * 0.35f, h * 0.47f),
                    size = Size(w * 0.30f, h * 0.22f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                if (mood == MascotMood.EXCITED) {
                    drawCircle(
                        color = ink,
                        radius = w * 0.03f,
                        center = Offset(w * 0.80f, h * 0.25f),
                    )
                }
            }
        }
    }
}

@Composable
fun MemberAvatar(
    member: Member,
    size: Dp = 36.dp,
    borderColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val (fill, ink) = pastelColorsFor(member.tone)
    val ring = borderColor ?: SliceTheme.colors.card
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (member.isCurrentUser) {
            Image(
                painter = painterResource(id = R.drawable.img_avatar_alex),
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = member.initials,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.34f).sp,
                ),
                color = ink,
            )
        }
    }
}

@Composable
fun AvatarStack(
    members: List<Member>,
    avatarSize: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    val visible = members.take(3)
    val overflow = (members.size - 3).coerceAtLeast(0)
    val overlap = avatarSize * 0.68f
    val totalCount = visible.size + if (overflow > 0) 1 else 0
    val stackWidth = if (totalCount <= 1) avatarSize else avatarSize + overlap * (totalCount - 1)
    val c = SliceTheme.colors

    Box(
        modifier = modifier
            .width(stackWidth)
            .height(avatarSize),
    ) {
        visible.forEachIndexed { index, member ->
            MemberAvatar(
                member = member,
                size = avatarSize,
                modifier = Modifier.offset(x = overlap * index),
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = overlap * visible.size)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(c.mist)
                    .border(1.5.dp, c.card, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = c.inkSecondary,
                )
            }
        }
    }
}

fun categoryIcon(category: ExpenseCategory): ImageVector = when (category) {
    ExpenseCategory.COFFEE -> Icons.Outlined.Coffee
    ExpenseCategory.DINING -> Icons.Outlined.Restaurant
    ExpenseCategory.HOME -> Icons.Outlined.Home
    ExpenseCategory.TRAVEL -> Icons.Outlined.FlightTakeoff
    ExpenseCategory.GROCERIES -> Icons.Outlined.ShoppingBag
    ExpenseCategory.UTILITIES -> Icons.Outlined.Bolt
    ExpenseCategory.SETTLEMENT -> Icons.Filled.CheckCircle
}

fun groupIcon(iconKey: String): ImageVector = when (iconKey) {
    "home" -> Icons.Outlined.Home
    "travel" -> Icons.Outlined.FlightTakeoff
    "dining" -> Icons.Outlined.Restaurant
    "trek" -> Icons.Outlined.Landscape
    else -> Icons.Outlined.Group
}

@Composable
fun CategoryIconBadge(
    category: ExpenseCategory,
    isSettlement: Boolean = false,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val bg = if (isSettlement) c.mintFill else c.mist
    val tint = if (isSettlement) c.mintInk else c.inkPrimary
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.label,
            tint = tint,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
fun SliceStatusPill(
    text: String,
    fillColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = fillColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun FloatingBottomDock(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dark pill dock with 3 clean icons
        Surface(
            shape = CircleShape,
            color = c.dockBackground,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockItem(
                    selected = activeTab == MainTab.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    label = "Home",
                    testTag = "dock_home",
                    onClick = { onSelectTab(MainTab.HOME) },
                )
                DockItem(
                    selected = activeTab == MainTab.GROUPS,
                    selectedIcon = Icons.Filled.Group,
                    unselectedIcon = Icons.Outlined.Group,
                    label = "Groups",
                    testTag = "dock_groups",
                    onClick = { onSelectTab(MainTab.GROUPS) },
                )
                DockItem(
                    selected = activeTab == MainTab.PROFILE,
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    label = "Profile",
                    testTag = "dock_profile",
                    onClick = { onSelectTab(MainTab.PROFILE) },
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Detached circular + action button for instant expense entry
        Surface(
            modifier = Modifier
                .size(58.dp)
                .testTag("dock_add_expense")
                .clip(CircleShape)
                .clickable(onClick = onAddClick),
            shape = CircleShape,
            color = c.lavenderFill,
            shadowElevation = 10.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    tint = c.lavenderInk,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val c = SliceTheme.colors
    val pillBg by animateColorAsState(
        targetValue = if (selected) c.dockContent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = spring(dampingRatio = 1f, stiffness = 520f),
        label = "dockPillBg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) c.dockContent else c.dockInactive,
        animationSpec = spring(dampingRatio = 1f, stiffness = 520f),
        label = "dockIconTint",
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(pillBg)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            if (selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = iconTint,
                )
            }
        }
    }
}

@Composable
fun SkeletonHomeLoading(modifier: Modifier = Modifier) {
    val c = SliceTheme.colors
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(c.shimmerBase, c.shimmerHighlight, c.shimmerBase),
        start = Offset(shift - 300f, 0f),
        end = Offset(shift, 200f),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Hero skeleton card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(shimmerBrush),
        )

        // Undone payments skeleton
        Box(
            modifier = Modifier
                .width(156.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(shimmerBrush),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(shimmerBrush),
                )
            }
        }
    }
}

@Composable
fun HeroNetBalanceCard(
    netPaise: Long,
    owedPaise: Long,
    youOwePaise: Long,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val cardBrush = Brush.linearGradient(
        colors = listOf(c.card, c.lavenderFill.copy(alpha = if (c.isDark) 0.45f else 0.55f)),
    )

    val statusText = when {
        netPaise > 0L -> "You're owed overall"
        netPaise < 0L -> "You owe overall"
        else -> "All settled overall"
    }
    val statusFill = when {
        netPaise > 0L -> c.lavenderFill
        netPaise < 0L -> c.coralFill
        else -> c.mintFill
    }
    val statusInk = when {
        netPaise > 0L -> c.lavenderInk
        netPaise < 0L -> c.coralInk
        else -> c.mintInk
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_net_balance_card"),
    ) {
        Column(
            modifier = Modifier
                .background(cardBrush)
                .padding(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.inkSecondary,
                )
                SliceStatusPill(
                    text = statusText,
                    fillColor = statusFill,
                    textColor = statusInk,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = SplitMath.formatMoney(
                    paise = netPaise,
                    currency = preferences.currency,
                    paisePrecision = preferences.paisePrecision,
                    showPlusSign = true,
                ),
                style = MaterialTheme.typography.displayLarge.tabular(),
                color = c.inkPrimary,
                modifier = Modifier.testTag("hero_net_balance_amount"),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = c.mintFill,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Owed: ${
                            SplitMath.formatMoney(
                                owedPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            )
                        }",
                        style = MaterialTheme.typography.labelLarge.tabular(),
                        color = c.mintInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = c.coralFill,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "You owe: ${
                            SplitMath.formatMoney(
                                youOwePaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            )
                        }",
                        style = MaterialTheme.typography.labelLarge.tabular(),
                        color = c.coralInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
