package com.example.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.Expense
import com.example.data.model.GroupSummary
import com.example.data.model.Member
import com.example.data.model.NewGroupPerson
import com.example.data.model.PastelTone
import com.example.data.model.SimplifiedTransfer
import com.example.ui.components.AvatarStack
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.MascotBadge
import com.example.ui.components.MascotMood
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SliceStatusPill
import com.example.ui.components.groupIcon
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.SliceSplitUiState

@Composable
fun GroupsTabScreen(
    state: SliceSplitUiState,
    onGroupClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val prefs = state.preferences

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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "groups_tab_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Groups",
                            style = MaterialTheme.typography.headlineLarge,
                            color = c.inkPrimary,
                        )
                        Text(
                            text = "${state.groupSummaries.size} shared circles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSecondary,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onJoinGroupClick, modifier = Modifier.testTag("join_group_button")) {
                            Text("Join")
                        }
                        Surface(
                            shape = CircleShape,
                            color = c.inkPrimary,
                            modifier = Modifier
                                .height(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onCreateGroupClick)
                                .testTag("new_group_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create group",
                                    tint = c.card,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "New Group",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.card,
                                )
                            }
                        }
                    }
                }
            }

            items(state.groupSummaries, key = { it.group.id }) { summary ->
                GroupListCard(
                    summary = summary,
                    preferences = prefs,
                    onClick = { onGroupClick(summary.group.id) },
                )
            }
        }
    }
}

@Composable
private fun GroupListCard(
    summary: GroupSummary,
    preferences: AppPreferences,
    onClick: () -> Unit,
) {
    val c = SliceTheme.colors
    val net = summary.currentUserNetPaise
    val statusText = when {
        summary.expenseCount == 0 -> "No splits yet"
        net == 0L && summary.simplifiedTransfers.isEmpty() -> "All settled ✓"
        net == 0L -> "Even ✓"
        else -> SplitMath.formatMoney(net, preferences.currency, preferences.paisePrecision, showPlusSign = true)
    }
    val (fill, ink) = when {
        summary.expenseCount == 0 -> c.mist to c.inkSecondary
        net > 0L -> c.mintFill to c.mintInk
        net < 0L -> c.coralFill to c.coralInk
        else -> c.mintFill to c.mintInk
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("groups_tab_item_${summary.group.id}"),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(c.mist),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = groupIcon(summary.group.iconKey),
                    contentDescription = summary.group.name,
                    tint = c.inkPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvatarStack(members = summary.members, avatarSize = 22.dp)
                    Text(
                        text = "${summary.expenseCount} bills",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkSecondary,
                    )
                }
            }

            SliceStatusPill(
                text = statusText,
                fillColor = fill,
                textColor = ink,
            )
        }
    }
}

private enum class LedgerTab {
    SIMPLIFIED_PLAN,
    ALL_EXPENSES,
}

@Composable
fun GroupLedgerScreen(
    groupId: String,
    state: SliceSplitUiState,
    onBack: () -> Unit,
    onSettleUp: (SimplifiedTransfer?) -> Unit,
    onRemind: (Member, Long) -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onCopyInviteCode: (String) -> Unit,
    onShareInviteCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val c = SliceTheme.colors
    val prefs = state.preferences
    val summary = state.groupSummaries.firstOrNull { it.group.id == groupId }
    val groupExpenses = state.expenses.filter { it.groupId == groupId }
    var selectedTab by remember { mutableStateOf(LedgerTab.SIMPLIFIED_PLAN) }
    val currentUserId = state.currentUser?.id ?: "alex"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (summary == null) {
            Text("Group not found", modifier = Modifier.padding(24.dp))
            return@Box
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.groupInviteCodes[groupId]?.let { inviteCode ->
                item(key = "ledger_invite_code") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = c.lavenderFill,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Invite code", style = MaterialTheme.typography.labelMedium, color = c.lavenderInk)
                                Text(inviteCode, style = MaterialTheme.typography.titleLarge, color = c.lavenderInk)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onCopyInviteCode(inviteCode) }) { Text("Copy") }
                                TextButton(onClick = { onShareInviteCode(inviteCode) }) { Text("Share") }
                            }
                        }
                    }
                }
            }

            // Top Bar
            item(key = "ledger_topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = c.card,
                            modifier = Modifier.size(48.dp),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("ledger_back_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = c.inkPrimary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = summary.group.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = c.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AvatarStack(members = summary.members, avatarSize = 24.dp)
                        }
                    }

                    if (summary.simplifiedTransfers.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = c.inkPrimary,
                            modifier = Modifier
                                .height(48.dp)
                                .clip(CircleShape)
                                .clickable { onSettleUp(null) }
                                .testTag("ledger_settle_up_pill"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            ) {
                                Text(
                                    text = "Settle Up",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.card,
                                )
                            }
                        }
                    }
                }
            }

            // Minimal Summary Card + Segmented Pill Toggle
            item(key = "ledger_summary_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.card,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Total Group Spend",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.inkSecondary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = SplitMath.formatMoney(
                                        summary.totalSpendPaise,
                                        prefs.currency,
                                        prefs.paisePrecision,
                                    ),
                                    style = MaterialTheme.typography.displayMedium.tabular(),
                                    color = c.inkPrimary,
                                )
                            }
                            val net = summary.currentUserNetPaise
                            if (summary.expenseCount > 0) {
                                SliceStatusPill(
                                    text = when {
                                        net > 0L -> "Owed ${SplitMath.formatMoney(net, prefs.currency, prefs.paisePrecision)}"
                                        net < 0L -> "Owe ${SplitMath.formatMoney(-net, prefs.currency, prefs.paisePrecision)}"
                                        else -> "Settled ✓"
                                    },
                                    fillColor = when {
                                        net > 0L -> c.mintFill
                                        net < 0L -> c.coralFill
                                        else -> c.mintFill
                                    },
                                    textColor = when {
                                        net > 0L -> c.mintInk
                                        net < 0L -> c.coralInk
                                        else -> c.mintInk
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Segmented Pill Toggle
                        Surface(
                            shape = CircleShape,
                            color = c.mist,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                            ) {
                                SegmentedPillOption(
                                    label = "Simplified Plan",
                                    selected = selectedTab == LedgerTab.SIMPLIFIED_PLAN,
                                    onClick = { selectedTab = LedgerTab.SIMPLIFIED_PLAN },
                                    testTag = "tab_simplified_plan",
                                    modifier = Modifier.weight(1f),
                                )
                                SegmentedPillOption(
                                    label = "All Expenses",
                                    selected = selectedTab == LedgerTab.ALL_EXPENSES,
                                    onClick = { selectedTab = LedgerTab.ALL_EXPENSES },
                                    testTag = "tab_all_expenses",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            if (groupExpenses.isEmpty()) {
                item(key = "group_empty_state") {
                    EmptySplitsCard(
                        title = "No splits yet",
                        subtitle = "Tap + below to add your first shared bill.",
                        showAction = true,
                        onAddClick = onAddExpense,
                    )
                }
            } else {
                item(key = "ledger_animated_content") {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(spring(dampingRatio = 0.92f, stiffness = 420f)) togetherWith
                                fadeOut(spring(dampingRatio = 1f, stiffness = 560f))
                        },
                        label = "ledgerTabSwitch",
                    ) { tab ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (tab) {
                                LedgerTab.SIMPLIFIED_PLAN -> {
                                    if (summary.simplifiedTransfers.isEmpty()) {
                                        AllSettledLedgerCard()
                                    } else {
                                        summary.simplifiedTransfers.forEachIndexed { idx, transfer ->
                                            val fromMember = state.membersMap[transfer.fromMemberId]
                                            val toMember = state.membersMap[transfer.toMemberId]
                                            if (fromMember != null && toMember != null) {
                                                SimplifiedTransferCard(
                                                    transfer = transfer,
                                                    fromMember = fromMember,
                                                    toMember = toMember,
                                                    currentUserId = currentUserId,
                                                    preferences = prefs,
                                                    onRemind = { onRemind(fromMember, transfer.amountPaise) },
                                                    onSettle = { onSettleUp(transfer) },
                                                    testTag = "transfer_card_$idx",
                                                )
                                            }
                                        }
                                    }
                                }

                                LedgerTab.ALL_EXPENSES -> {
                                    groupExpenses.forEach { expense ->
                                        ExpenseActivityRow(
                                            expense = expense,
                                            groupName = null,
                                            membersMap = state.membersMap,
                                            currentUserId = currentUserId,
                                            preferences = prefs,
                                            onClick = { onExpenseClick(expense.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Add Expense FAB in Ledger
        Surface(
            shape = CircleShape,
            color = c.lavenderFill,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(58.dp)
                .clip(CircleShape)
                .clickable(onClick = onAddExpense)
                .testTag("ledger_add_expense_fab"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add expense to ${summary.group.name}",
                    tint = c.lavenderInk,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun SegmentedPillOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    Surface(
        shape = CircleShape,
        color = if (selected) c.card else androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) c.inkPrimary else c.inkSecondary,
            )
        }
    }
}

@Composable
private fun SimplifiedTransferCard(
    transfer: SimplifiedTransfer,
    fromMember: Member,
    toMember: Member,
    currentUserId: String,
    preferences: AppPreferences,
    onRemind: () -> Unit,
    onSettle: () -> Unit,
    testTag: String,
) {
    val c = SliceTheme.colors
    val fromLabel = if (fromMember.id == currentUserId) "You" else fromMember.name
    val toLabel = if (toMember.id == currentUserId) "You" else toMember.name

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
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
                        )
                        Text(
                            text = "Minimum transfer",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.inkSecondary,
                        )
                    }
                }

                Text(
                    text = SplitMath.formatMoney(
                        transfer.amountPaise,
                        preferences.currency,
                        preferences.paisePrecision,
                    ),
                    style = MaterialTheme.typography.titleLarge.tabular(),
                    color = if (toMember.id == currentUserId) c.mintInk else c.coralInk,
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
                        .clickable(onClick = onSettle)
                        .testTag("${testTag}_settle"),
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
private fun AllSettledLedgerCard() {
    val c = SliceTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MascotBadge(
                mood = MascotMood.HAPPY,
                tone = PastelTone.MINT,
                size = 64.dp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "All settled ✓",
                style = MaterialTheme.typography.headlineMedium,
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Everyone in this group is completely square.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailSheet(
    expense: Expense,
    groupName: String,
    membersMap: Map<String, Member>,
    currentUserId: String,
    preferences: AppPreferences,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = SliceTheme.colors
    val payer = membersMap[expense.paidByMemberId]
    val payerName = if (payer?.id == currentUserId) "You" else payer?.name ?: "Member"

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CategoryIconBadge(
                    category = expense.category,
                    isSettlement = expense.isSettlement,
                    size = 52.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.inkPrimary,
                    )
                    Text(
                        text = "$groupName • ${SplitMath.formatRelativeTime(expense.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = c.mist,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = if (expense.isSettlement) "Settlement Transfer" else "Paid by $payerName",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.inkSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SplitMath.formatMoney(
                                expense.amountPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            ),
                            style = MaterialTheme.typography.displayMedium.tabular(),
                            color = c.inkPrimary,
                        )
                    }
                    SliceStatusPill(
                        text = if (expense.isSettlement) {
                            expense.settlementMethod?.label ?: "Settled"
                        } else {
                            "${expense.splitMode.label} split"
                        },
                        fillColor = c.lavenderFill,
                        textColor = c.lavenderInk,
                    )
                }
            }

            if (expense.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Itemized Bills",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.inkSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                expense.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.inkPrimary,
                        )
                        Text(
                            text = SplitMath.formatMoney(
                                item.amountPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            ),
                            style = MaterialTheme.typography.bodyLarge.tabular(),
                            color = c.inkPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.labelLarge,
                color = c.inkSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            expense.splits.forEachIndexed { idx, split ->
                val member = membersMap[split.memberId]
                if (member != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MemberAvatar(member = member, size = 34.dp)
                            Text(
                                text = if (member.id == currentUserId) "You (${member.name})" else member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.inkPrimary,
                            )
                        }
                        Text(
                            text = SplitMath.formatMoneyAlwaysDecimals(
                                split.amountPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            ),
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            color = c.inkPrimary,
                        )
                    }
                    if (idx < expense.splits.lastIndex) {
                        HorizontalDivider(color = c.divider)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!expense.isSettlement) {
                    Surface(
                        shape = CircleShape,
                        color = c.mist,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onEdit)
                            .testTag("expense_detail_edit"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = c.inkPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.inkPrimary,
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = c.coralFill,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .testTag("expense_detail_delete"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = c.coralInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.coralInk,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupSheet(
    members: List<Member>,
    onDismiss: () -> Unit,
    onCreate: (name: String, iconKey: String, memberIds: List<String>, newPeople: List<NewGroupPerson>) -> Unit,
) {
    val c = SliceTheme.colors
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("travel") }
    var personName by remember { mutableStateOf("") }
    var personContact by remember { mutableStateOf("") }
    val newPeople = remember { mutableStateListOf<NewGroupPerson>() }
    val selectedMemberIds = remember {
        mutableStateListOf<String>().apply {
            addAll(members.filter { !it.isCurrentUser }.take(2).map { it.id })
        }
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                personName = cursor.getString(0).orEmpty()
                personContact = cursor.getString(1).orEmpty()
            }
        }
    }

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
                text = "New group",
                style = MaterialTheme.typography.headlineMedium,
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Start a fresh ledger for a trip, flat, or dinner.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                placeholder = { Text("e.g. Coorg Roadtrip") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.inkPrimary,
                    unfocusedBorderColor = c.divider,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_group_name_input"),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Icon", style = MaterialTheme.typography.labelLarge, color = c.inkSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("home" to "Home", "travel" to "Trip", "dining" to "Food", "trek" to "Trek").forEach { (key, label) ->
                    val selected = iconKey == key
                    FilterChip(
                        selected = selected,
                        onClick = { iconKey = key },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(groupIcon(key), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.lavenderFill,
                            selectedLabelColor = c.lavenderInk,
                            selectedLeadingIconColor = c.lavenderInk,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Add people", style = MaterialTheme.typography.labelLarge, color = c.inkSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Enter each person's name and phone number or email address.",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkSecondary,
            )
            TextButton(
                onClick = {
                    contactPicker.launch(
                        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                    )
                },
                modifier = Modifier.testTag("choose_group_contact"),
            ) {
                Text("Choose from contacts")
            }
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("create_person_name_input"),
            )
            OutlinedTextField(
                value = personContact,
                onValueChange = { personContact = it },
                label = { Text("Phone number or email") },
                placeholder = { Text("name@example.com or +91 98765 43210") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("create_person_contact_input"),
            )
            val normalizedContact = personContact.trim()
            val contactValid = normalizedContact.contains("@")
                .let { hasAt ->
                    if (hasAt) normalizedContact.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                    else normalizedContact.filter { it.isDigit() }.length in 7..15
                }
            Button(
                onClick = {
                    val cleanName = personName.trim()
                    val duplicate = newPeople.any { it.contact.equals(normalizedContact, ignoreCase = true) } ||
                        members.any { it.contact.equals(normalizedContact, ignoreCase = true) && it.contact.isNotBlank() }
                    if (cleanName.isNotEmpty() && contactValid && !duplicate) {
                        newPeople.add(NewGroupPerson(cleanName, normalizedContact))
                        personName = ""
                        personContact = ""
                    }
                },
                enabled = personName.isNotBlank() && contactValid,
                shape = CircleShape,
            ) { Text("Add person") }
            newPeople.forEach { person ->
                Text("✓ ${person.name} · ${person.contact}", style = MaterialTheme.typography.bodyMedium, color = c.mintInk)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Saved people", style = MaterialTheme.typography.labelLarge, color = c.inkSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                members.filter { !it.isCurrentUser }.forEach { friend ->
                    val chosen = friend.id in selectedMemberIds
                    FilterChip(
                        selected = chosen,
                        onClick = {
                            if (chosen) selectedMemberIds.remove(friend.id) else selectedMemberIds.add(friend.id)
                        },
                        label = { Text(friend.name) },
                        leadingIcon = if (chosen) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.inkPrimary,
                            selectedLabelColor = c.card,
                            selectedLeadingIconColor = c.card,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = { onCreate(name, iconKey, selectedMemberIds.toList(), newPeople.toList()) },
                enabled = name.isNotBlank() && (selectedMemberIds.isNotEmpty() || newPeople.isNotEmpty()),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.inkPrimary,
                    contentColor = c.card,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_create_group"),
            ) {
                Text("Create Group", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    SETTLEMENTS("Settlements"),
    YOU_PAID("You Paid"),
}

@Composable
fun FullActivityScreen(
    state: SliceSplitUiState,
    onBack: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val c = SliceTheme.colors
    val prefs = state.preferences
    val currentUserId = state.currentUser?.id ?: "alex"
    var filter by remember { mutableStateOf(ActivityFilter.ALL) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(state.expenses, filter, search, state.groups, state.membersMap) {
        val q = search.trim().lowercase()
        state.expenses.filter { exp ->
            val matchesFilter = when (filter) {
                ActivityFilter.ALL -> true
                ActivityFilter.EXPENSES -> !exp.isSettlement
                ActivityFilter.SETTLEMENTS -> exp.isSettlement
                ActivityFilter.YOU_PAID -> exp.paidByMemberId == currentUserId
            }
            if (!matchesFilter) return@filter false
            if (q.isEmpty()) return@filter true
            val groupName = state.groups.firstOrNull { it.id == exp.groupId }?.name?.lowercase().orEmpty()
            val payerName = state.membersMap[exp.paidByMemberId]?.name?.lowercase().orEmpty()
            exp.title.lowercase().contains(q) || groupName.contains(q) || payerName.contains(q)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "full_activity_topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = c.card,
                            modifier = Modifier.size(48.dp),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("full_activity_back"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = c.inkPrimary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Full Activity",
                                style = MaterialTheme.typography.headlineMedium,
                                color = c.inkPrimary,
                            )
                            Text(
                                text = "${filtered.size} items across groups",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.inkSecondary,
                            )
                        }
                    }
                }
            }

            item(key = "full_activity_search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search bills, friends, or groups") },
                        singleLine = true,
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = c.inkSecondary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = c.card,
                            unfocusedContainerColor = c.card,
                            focusedBorderColor = c.inkPrimary,
                            unfocusedBorderColor = c.divider,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_activity_search"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActivityFilter.entries.forEach { option ->
                            val selected = filter == option
                            Surface(
                                shape = CircleShape,
                                color = if (selected) c.inkPrimary else c.card,
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .clickable { filter = option },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) c.card else c.inkSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "full_activity_empty") {
                    EmptySplitsCard(
                        title = "No matching activity",
                        subtitle = if (search.isNotBlank()) "Try clearing your search filter." else "Tap + to add your first split.",
                        showAction = search.isBlank(),
                        onAddClick = onAddExpense,
                    )
                }
            } else {
                items(filtered, key = { it.id }) { expense ->
                    val groupName = state.groups.firstOrNull { it.id == expense.groupId }?.name
                    ExpenseActivityRow(
                        expense = expense,
                        groupName = groupName,
                        membersMap = state.membersMap,
                        currentUserId = currentUserId,
                        preferences = prefs,
                        onClick = { onExpenseClick(expense.id) },
                    )
                }
            }
        }
    }
}
