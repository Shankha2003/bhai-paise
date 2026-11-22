package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.Member
import com.example.data.model.PastelTone
import com.example.ui.components.AvatarStack
import com.example.ui.components.MascotBadge
import com.example.ui.components.MascotMood
import com.example.ui.components.SliceSplitMark
import com.example.ui.components.SliceStatusPill
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular

@Composable
fun SplashScreen(
    progress: Float,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 1f, stiffness = 600f),
        label = "splashProgress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = c.card,
                shadowElevation = 2.dp,
                modifier = Modifier.size(92.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SliceSplitMark(size = 56.dp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Thin 3dp smooth progress bar
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(c.mist),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(c.inkPrimary),
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    members: List<Member>,
    preferences: AppPreferences,
    onStartSplitting: () -> Unit,
    onOpenJoinInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    var previewCount by remember { mutableIntStateOf(3) }
    val previewTotalPaise = 180_000L // ₹1,800
    val perPersonPaise = previewTotalPaise / previewCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top subtle brand header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SliceSplitMark(size = 32.dp)
                    Text(
                        text = "Bhai Paise",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.inkPrimary,
                    )
                }
                SliceStatusPill(
                    text = "Open Source",
                    fillColor = c.lavenderFill,
                    textColor = c.lavenderInk,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Headline & Subtitle
            Column {
                Text(
                    text = "Split bills at\nthe speed of thought.",
                    style = MaterialTheme.typography.displayMedium,
                    color = c.inkPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Zero paywalls. Fair math. Open source.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.inkSecondary,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Tactile Hero Card previewing a live interactive split
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = c.card,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SliceStatusPill(
                            text = "Live Split Preview",
                            fillColor = c.lavenderFill,
                            textColor = c.lavenderInk,
                        )
                        AvatarStack(
                            members = members.take(previewCount),
                            avatarSize = 28.dp,
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Dinner at Truffles",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.inkPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paid by You • Split equally",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSecondary,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.mist)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Each person pays",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.inkSecondary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = SplitMath.formatMoney(
                                    paise = perPersonPaise,
                                    currency = preferences.currency,
                                    paisePrecision = preferences.paisePrecision,
                                ),
                                style = MaterialTheme.typography.headlineMedium.tabular(),
                                color = c.inkPrimary,
                            )
                        }

                        // Interactive people stepper pills right on the preview card
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4).forEach { count ->
                                val selected = previewCount == count
                                Surface(
                                    shape = CircleShape,
                                    color = if (selected) c.inkPrimary else c.card,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { previewCount = count },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${count}x",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) c.card else c.inkPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Obsidian CTA + Invite Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onStartSplitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_splitting_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.inkPrimary,
                        contentColor = c.card,
                    ),
                ) {
                    Text(
                        text = "Start Splitting",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onOpenJoinInvite,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("join_invite_link"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = c.inkSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Join via Invite Link",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.inkSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinInviteBottomSheet(
    onDismiss: () -> Unit,
    onJoinCode: (String) -> Unit,
) {
    val c = SliceTheme.colors
    var inviteCode by remember { mutableStateOf("") }
    val normalizedCode = inviteCode.trim().uppercase().filter { it.isLetterOrDigit() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Join a group",
                style = MaterialTheme.typography.headlineMedium,
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Paste an invite link or enter a short code from a friend.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                singleLine = true,
                label = { Text("8-character invite code") },
                placeholder = { Text("ABC123XY") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.inkPrimary,
                    unfocusedBorderColor = c.divider,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invite_code_input"),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { onJoinCode(normalizedCode) },
                enabled = normalizedCode.length == 8,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.inkPrimary,
                    contentColor = c.card,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_join_invite"),
            ) {
                Text(
                    text = "Join Group",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
