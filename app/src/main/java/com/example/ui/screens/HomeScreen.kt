package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.PastelTone
import com.example.data.model.PendingPayment
import com.example.data.model.SimplifiedTransfer
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.MascotBadge
import com.example.ui.components.MascotMood
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SliceStatusPill
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.SliceSplitUiState

@Composable
fun HomeScreen(
    state: SliceSplitUiState,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onSettlePending: (String, SimplifiedTransfer) -> Unit,
    onRemind: (Member, Long) -> Unit,
    onAddFirstExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val prefs = state.preferences
    val query = state.searchQuery.trim().lowercase()
    val currentUserId = state.currentUser?.id ?: "alex"

    val filteredPending = if (query.isEmpty()) {
        state.pendingPayments
    } else {
        state.pendingPayments.filter { pending ->
            val fromName = state.membersMap[pending.transfer.fromMemberId]?.name?.lowercase().orEmpty()
            val toName = state.membersMap[pending.transfer.toMemberId]?.name?.lowercase().orEmpty()
            pending.groupName.lowercase().contains(query) ||
                fromName.contains(query) ||
                toName.contains(query)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        ) {
            // Minimal Header
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Hey, ${state.currentUser?.name ?: "Alex"}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = c.inkPrimary,
                        )
                        Text(
                            text = "Only undone payments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSecondary,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (state.isSearchActive) c.inkPrimary else c.card,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(48.dp),
                        ) {
                            IconButton(
                                onClick = onToggleSearch,
                                modifier = Modifier.testTag("home_search_button"),
                            ) {
                                Icon(
                                    imageVector = if (state.isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                                    contentDescription = if (state.isSearchActive) "Close search" else "Search undone payments",
                                    tint = if (state.isSearchActive) c.card else c.inkPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Search Input
            item(key = "search_bar") {
                AnimatedVisibility(
                    visible = state.isSearchActive,
                    enter = expandVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) +
                        fadeIn(spring(dampingRatio = 1f, stiffness = 540f)),
                    exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = 580f)) +
                        fadeOut(spring(dampingRatio = 1f, stiffness = 620f)),
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search undone payments or groups") },
                        singleLine = true,
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = c.inkSecondary,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = c.card,
                            unfocusedContainerColor = c.card,
                            focusedBorderColor = c.inkPrimary,
                            unfocusedBorderColor = c.divider,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("home_search_input"),
                    )
                }
            }

            // Hero Last Payment Due Card (replaces Net Balance on Home)
                item(key = "hero_last_payment") {
                    Spacer(modifier = Modifier.height(10.dp))
                    HeroLastPaymentDueCard(
                        lastPayment = state.lastPaymentUserOwes,
                        membersMap = state.membersMap,
                        preferences = prefs,
                        onPayNow = { payment ->
                            onSettlePending(payment.groupId, payment.transfer)
                        },
                        onOpenGroup = { groupId -> onGroupClick(groupId) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                // Undone Payments Section Header
                item(key = "undone_header") {
                    Spacer(modifier = Modifier.height(26.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Undone Payments",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.inkPrimary,
                        )
                        SliceStatusPill(
                            text = "${filteredPending.size} pending",
                            fillColor = if (filteredPending.isEmpty()) c.mintFill else c.lavenderFill,
                            textColor = if (filteredPending.isEmpty()) c.mintInk else c.lavenderInk,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (filteredPending.isEmpty()) {
                    item(key = "empty_undone") {
                        EmptySplitsCard(
                            title = if (query.isNotEmpty()) "No matching payments" else "All settled! 🎉",
                            subtitle = if (query.isNotEmpty()) {
                                "Try searching another friend or group name."
                            } else {
                                "Zero undone payments right now. Tap + to add a bill."
                            },
                            showAction = query.isEmpty(),
                            onAddClick = onAddFirstExpense,
                            mood = if (query.isEmpty()) MascotMood.HAPPY else MascotMood.CALM,
                            tone = if (query.isEmpty()) PastelTone.MINT else PastelTone.LAVENDER,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = filteredPending,
                        key = { idx, item ->
                            "${item.groupId}_${item.transfer.fromMemberId}_${item.transfer.toMemberId}_$idx"
                        },
                    ) { idx, pending ->
                        val fromMember = state.membersMap[pending.transfer.fromMemberId]
                        val toMember = state.membersMap[pending.transfer.toMemberId]
                        if (fromMember != null && toMember != null) {
                            PendingPaymentCard(
                                pending = pending,
                                fromMember = fromMember,
                                toMember = toMember,
                                currentUserId = currentUserId,
                                preferences = prefs,
                                onGroupClick = { onGroupClick(pending.groupId) },
                                onRemind = { onRemind(fromMember, pending.transfer.amountPaise) },
                                onSettle = { onSettlePending(pending.groupId, pending.transfer) },
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .testTag("home_pending_card_$idx"),
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun HeroLastPaymentDueCard(
    lastPayment: PendingPayment?,
    membersMap: Map<String, Member>,
    preferences: AppPreferences,
    onPayNow: (PendingPayment) -> Unit,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            c.card,
            if (lastPayment != null) {
                c.coralFill.copy(alpha = if (c.isDark) 0.35f else 0.45f)
            } else {
                c.mintFill.copy(alpha = if (c.isDark) 0.35f else 0.45f)
            },
        ),
    )

    val receiver = lastPayment?.transfer?.toMemberId?.let { membersMap[it] }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_last_payment_card"),
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
                    text = "Last Payment Due",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.inkSecondary,
                )
                SliceStatusPill(
                    text = if (lastPayment != null) "You have to pay" else "All clear ✓",
                    fillColor = if (lastPayment != null) c.coralFill else c.mintFill,
                    textColor = if (lastPayment != null) c.coralInk else c.mintInk,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (lastPayment != null) {
                    SplitMath.formatMoney(
                        paise = lastPayment.transfer.amountPaise,
                        currency = preferences.currency,
                        paisePrecision = preferences.paisePrecision,
                    )
                } else {
                    SplitMath.formatMoney(0L, preferences.currency, preferences.paisePrecision)
                },
                style = MaterialTheme.typography.displayLarge.tabular(),
                color = c.inkPrimary,
                modifier = Modifier.testTag("hero_last_payment_amount"),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (lastPayment != null && receiver != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MemberAvatar(member = receiver, size = 24.dp)
                    Text(
                        text = "Pay ${receiver.name} • ${lastPayment.groupName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = c.inkPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { onPayNow(lastPayment) }
                            .testTag("hero_pay_now_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Pay Now",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.card,
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = c.card.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { onOpenGroup(lastPayment.groupId) }
                            .testTag("hero_open_group_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "View Group",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.inkPrimary,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "You don't owe anyone right now.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun PendingPaymentCard(
    pending: PendingPayment,
    fromMember: Member,
    toMember: Member,
    currentUserId: String,
    preferences: AppPreferences,
    onGroupClick: () -> Unit,
    onRemind: () -> Unit,
    onSettle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val fromLabel = if (fromMember.id == currentUserId) "You" else fromMember.name
    val toLabel = if (toMember.id == currentUserId) "You" else toMember.name
    val isOwedToYou = toMember.id == currentUserId
    val isYouOwe = fromMember.id == currentUserId

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    MemberAvatar(member = fromMember, size = 38.dp)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = c.inkSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    MemberAvatar(member = toMember, size = 38.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "$fromLabel → pays $toLabel",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = c.mist,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onGroupClick),
                        ) {
                            Text(
                                text = pending.groupName,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Text(
                    text = SplitMath.formatMoney(
                        pending.transfer.amountPaise,
                        preferences.currency,
                        preferences.paisePrecision,
                    ),
                    style = MaterialTheme.typography.titleLarge.tabular(),
                    color = when {
                        isOwedToYou -> c.mintInk
                        isYouOwe -> c.coralInk
                        else -> c.inkPrimary
                    },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = c.mist,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemind),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = c.inkPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Remind",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.inkPrimary,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = c.inkPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSettle),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Settle",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.card,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseActivityRow(
    expense: Expense,
    groupName: String?,
    membersMap: Map<String, Member>,
    currentUserId: String,
    preferences: AppPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val payerName = if (expense.paidByMemberId == currentUserId) {
        "You"
    } else {
        membersMap[expense.paidByMemberId]?.name ?: "Someone"
    }

    val subtext = if (expense.isSettlement) {
        "${expense.settlementMethod?.label ?: "Payment"} • ${groupName ?: "Group"}"
    } else {
        val count = expense.splits.size.coerceAtLeast(1)
        val groupPrefix = groupName?.let { "$it • " } ?: ""
        "$groupPrefix$payerName paid • Split with $count"
    }

    val userSplit = expense.splits.firstOrNull { it.memberId == currentUserId }?.amountPaise ?: 0L
    val userNetOnExpense = if (expense.paidByMemberId == currentUserId) {
        expense.amountPaise - userSplit
    } else {
        -userSplit
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = c.card,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("expense_row_${expense.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconBadge(
                category = expense.category,
                isSettlement = expense.isSettlement,
                size = 44.dp,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SplitMath.formatMoney(
                        expense.amountPaise,
                        preferences.currency,
                        preferences.paisePrecision,
                    ),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = c.inkPrimary,
                )
                if (!expense.isSettlement && userNetOnExpense != 0L) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (userNetOnExpense > 0L) {
                            "lent ${
                                SplitMath.formatMoney(
                                    userNetOnExpense,
                                    preferences.currency,
                                    preferences.paisePrecision,
                                )
                            }"
                        } else {
                            "owe ${
                                SplitMath.formatMoney(
                                    -userNetOnExpense,
                                    preferences.currency,
                                    preferences.paisePrecision,
                                )
                            }"
                        },
                        style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
                        color = if (userNetOnExpense > 0L) c.mintInk else c.coralInk,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySplitsCard(
    title: String,
    subtitle: String,
    showAction: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.CALM,
    tone: PastelTone = PastelTone.LAVENDER,
) {
    val c = SliceTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MascotBadge(
                mood = mood,
                tone = tone,
                size = 64.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )
            if (showAction) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = CircleShape,
                    color = c.inkPrimary,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAddClick)
                        .testTag("empty_add_first_expense"),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    ) {
                        Text(
                            text = "+ Add First Expense",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.card,
                        )
                    }
                }
            }
        }
    }
}
