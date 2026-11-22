package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseItem
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.data.model.MemberSplit
import com.example.data.model.SplitMode
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SliceStatusPill
import com.example.ui.components.categoryIcon
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import java.util.UUID
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    groups: List<Group>,
    membersMap: Map<String, Member>,
    currentUserId: String,
    preferences: AppPreferences,
    preselectedGroupId: String?,
    editingExpense: Expense?,
    onDismiss: () -> Unit,
    onSave: (
        editingId: String?,
        groupId: String,
        title: String,
        amountPaise: Long,
        paidByMemberId: String,
        splitMode: SplitMode,
        category: ExpenseCategory,
        splits: List<MemberSplit>,
        items: List<ExpenseItem>,
    ) -> Unit,
    onValidationMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)

    val c = SliceTheme.colors
    val initialGroup = editingExpense?.let { exp -> groups.firstOrNull { it.id == exp.groupId } }
        ?: preselectedGroupId?.let { gid -> groups.firstOrNull { it.id == gid } }
        ?: groups.firstOrNull()

    var selectedGroupId by remember { mutableStateOf(initialGroup?.id ?: "") }
    val activeGroup = groups.firstOrNull { it.id == selectedGroupId } ?: groups.firstOrNull()
    val groupMembers = remember(activeGroup, membersMap) {
        activeGroup?.memberIds?.mapNotNull { membersMap[it] } ?: emptyList()
    }

    var amountInput by remember {
        mutableStateOf(
            editingExpense?.let {
                val whole = it.amountPaise / 100L
                val frac = it.amountPaise % 100L
                if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(2, '0')}"
            } ?: "",
        )
    }
    var title by remember { mutableStateOf(editingExpense?.title ?: "") }
    var selectedCategory by remember {
        mutableStateOf(editingExpense?.category ?: ExpenseCategory.DINING)
    }
    var paidByMemberId by remember {
        mutableStateOf(editingExpense?.paidByMemberId ?: currentUserId)
    }
    var splitMode by remember {
        mutableStateOf(editingExpense?.splitMode ?: SplitMode.EQUAL)
    }
    var showKeypad by remember { mutableStateOf(editingExpense == null) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showPayerPicker by remember { mutableStateOf(false) }

    // Per-mode participant tracking
    val includedIds = remember { mutableStateMapOf<String, Boolean>() }
    val exactInputs = remember { mutableStateMapOf<String, String>() }
    val pctInputs = remember { mutableStateMapOf<String, String>() }
    val shareUnits = remember { mutableStateMapOf<String, Int>() }
    val itemizedRows = remember {
        mutableStateListOf<ExpenseItem>().apply {
            if (editingExpense != null && editingExpense.items.isNotEmpty()) {
                addAll(editingExpense.items)
            }
        }
    }

    // Re-sync member maps whenever active group changes
    LaunchedEffect(activeGroup?.id) {
        val memberIds = activeGroup?.memberIds ?: emptyList()
        if (paidByMemberId !in memberIds && memberIds.isNotEmpty()) {
            paidByMemberId = if (currentUserId in memberIds) currentUserId else memberIds.first()
        }
        memberIds.forEach { id ->
            if (id !in includedIds) {
                val preIncluded = editingExpense?.splits?.any { it.memberId == id } ?: true
                includedIds[id] = preIncluded
            }
            if (id !in shareUnits) {
                val preShare = editingExpense?.splits?.firstOrNull { it.memberId == id }?.shareUnits ?: 1
                shareUnits[id] = preShare.coerceAtLeast(1)
            }
            if (id !in exactInputs) {
                val preExact = editingExpense?.splits?.firstOrNull { it.memberId == id }?.amountPaise ?: 0L
                exactInputs[id] = if (preExact > 0L) (preExact / 100.0).toString() else ""
            }
            if (id !in pctInputs) {
                val defaultPct = if (memberIds.isNotEmpty()) (100.0 / memberIds.size) else 0.0
                val prePct = editingExpense?.splits?.firstOrNull { it.memberId == id }?.percentage
                    ?.takeIf { it > 0.0 } ?: defaultPct
                pctInputs[id] = "%.0f".format(prePct)
            }
        }
        if (itemizedRows.isEmpty() && memberIds.isNotEmpty()) {
            itemizedRows.add(
                ExpenseItem(
                    id = "item_1",
                    name = "Shared Item",
                    amountPaise = 0L,
                    assignedMemberIds = memberIds,
                ),
            )
        }
    }

    val totalPaise: Long = if (splitMode == SplitMode.ITEMIZED) {
        itemizedRows.sumOf { it.amountPaise }
    } else {
        parseRupeesToPaise(amountInput)
    }

    val activeParticipants = groupMembers.map { it.id }.filter { includedIds[it] == true }

    val computedSplits: List<MemberSplit> = remember(
        totalPaise,
        splitMode,
        activeParticipants,
        exactInputs.toMap(),
        pctInputs.toMap(),
        shareUnits.toMap(),
        itemizedRows.toList(),
        groupMembers,
    ) {
        when (splitMode) {
            SplitMode.EQUAL -> SplitMath.equalSplit(totalPaise, activeParticipants)
            SplitMode.EXACT -> groupMembers.map { m ->
                val p = parseRupeesToPaise(exactInputs[m.id].orEmpty())
                MemberSplit(memberId = m.id, amountPaise = p)
            }.filter { it.amountPaise > 0L }
            SplitMode.PERCENTAGE -> {
                val pcts = groupMembers.associate { m ->
                    m.id to (pctInputs[m.id]?.toDoubleOrNull() ?: 0.0)
                }
                SplitMath.percentageSplit(totalPaise, pcts).filter { it.amountPaise > 0L }
            }
            SplitMode.SHARES -> {
                val shares = groupMembers.associate { m ->
                    m.id to (if (includedIds[m.id] == true) (shareUnits[m.id] ?: 1) else 0)
                }
                SplitMath.sharesSplit(totalPaise, shares).filter { it.amountPaise > 0L }
            }
            SplitMode.ITEMIZED -> {
                SplitMath.itemizedSplit(groupMembers.map { it.id }, itemizedRows)
                    .filter { it.amountPaise > 0L }
            }
        }
    }

    val allocatedPaise = computedSplits.sumOf { it.amountPaise }
    val splitDeltaPaise = totalPaise - allocatedPaise

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
        ) {
            // Top bar: close × and Save pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = c.card,
                    modifier = Modifier.size(48.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("add_expense_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = c.inkPrimary,
                        )
                    }
                }

                Text(
                    text = if (editingExpense != null) "Edit Split" else "New Split",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.inkPrimary,
                )

                Surface(
                    shape = CircleShape,
                    color = if (totalPaise > 0L && activeGroup != null) c.inkPrimary else c.mist,
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            when {
                                activeGroup == null -> onValidationMessage("Create a group first")
                                totalPaise <= 0L -> onValidationMessage("Enter an amount greater than 0")
                                computedSplits.isEmpty() -> onValidationMessage("Select at least one person to split")
                                splitMode == SplitMode.EXACT && splitDeltaPaise != 0L ->
                                    onValidationMessage("Exact amounts must add up to total")
                                splitMode == SplitMode.PERCENTAGE -> {
                                    val sumPct = groupMembers.sumOf { pctInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
                                    if (kotlin.math.abs(sumPct - 100.0) > 0.5) {
                                        onValidationMessage("Percentages must total 100%")
                                    } else {
                                        onSave(
                                            editingExpense?.id,
                                            activeGroup.id,
                                            title,
                                            totalPaise,
                                            paidByMemberId,
                                            splitMode,
                                            selectedCategory,
                                            computedSplits,
                                            emptyList(),
                                        )
                                    }
                                }
                                else -> onSave(
                                    editingExpense?.id,
                                    activeGroup.id,
                                    title,
                                    totalPaise,
                                    paidByMemberId,
                                    splitMode,
                                    selectedCategory,
                                    computedSplits,
                                    if (splitMode == SplitMode.ITEMIZED) itemizedRows.toList() else emptyList(),
                                )
                            }
                        }
                        .testTag("add_expense_save"),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (totalPaise > 0L && activeGroup != null) c.card else c.inkSecondary,
                        )
                    }
                }
            }

            // Scrollable form body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                // Centerpiece Amount Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            if (splitMode != SplitMode.ITEMIZED) showKeypad = !showKeypad
                        }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val displayAmount = if (splitMode == SplitMode.ITEMIZED) {
                        SplitMath.formatMoney(totalPaise, preferences.currency, preferences.paisePrecision)
                    } else {
                        val raw = amountInput.ifBlank { "0" }
                        "${preferences.currency.symbol}$raw"
                    }
                    Text(
                        text = displayAmount,
                        style = MaterialTheme.typography.displayLarge.tabular().copy(fontSize = 48.sp),
                        color = if (totalPaise > 0L) c.inkPrimary else c.inkSecondary.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("add_expense_amount_display"),
                    )
                    if (splitMode != SplitMode.ITEMIZED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (showKeypad) "Hide keypad" else "Tap to enter amount",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.inkSecondary,
                            )
                            Icon(
                                imageVector = if (showKeypad) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null,
                                tint = c.inkSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Clean Pill Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Dinner at Truffles") },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.card,
                        unfocusedContainerColor = c.card,
                        focusedBorderColor = c.inkPrimary,
                        unfocusedBorderColor = c.divider,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_expense_title_input"),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Icon Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpenseCategory.entries.filter { it != ExpenseCategory.SETTLEMENT }.forEach { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            shape = CircleShape,
                            color = if (selected) c.lavenderFill else c.card,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .clickable { selectedCategory = cat },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = cat.label,
                                    tint = if (selected) c.lavenderInk else c.inkSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) c.lavenderInk else c.inkPrimary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Group & Payer Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = c.card,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { showGroupPicker = !showGroupPicker; showPayerPicker = false }
                            .testTag("select_group_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = activeGroup?.name ?: "Select Group",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.inkPrimary,
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = c.inkSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    val payerMember = membersMap[paidByMemberId]
                    val payerLabel = if (paidByMemberId == currentUserId) {
                        "Paid by You"
                    } else {
                        "Paid by ${payerMember?.name ?: "Friend"}"
                    }
                    Surface(
                        shape = CircleShape,
                        color = c.card,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { showPayerPicker = !showPayerPicker; showGroupPicker = false }
                            .testTag("select_payer_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = payerLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = c.inkPrimary,
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = c.inkSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Inline Expandable Group Picker
                AnimatedVisibility(visible = showGroupPicker) {
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groups.forEach { g ->
                            val chosen = g.id == selectedGroupId
                            Surface(
                                shape = CircleShape,
                                color = if (chosen) c.inkPrimary else c.mist,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedGroupId = g.id
                                        showGroupPicker = false
                                    },
                            ) {
                                Text(
                                    text = g.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (chosen) c.card else c.inkPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }

                // Inline Expandable Payer Picker
                AnimatedVisibility(visible = showPayerPicker) {
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groupMembers.forEach { m ->
                            val chosen = m.id == paidByMemberId
                            Surface(
                                shape = CircleShape,
                                color = if (chosen) c.inkPrimary else c.mist,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        paidByMemberId = m.id
                                        showPayerPicker = false
                                    },
                            ) {
                                Text(
                                    text = if (m.id == currentUserId) "You (${m.name})" else m.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (chosen) c.card else c.inkPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Split Mode Selector Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SplitMode.entries.forEach { mode ->
                        val selected = splitMode == mode
                        Surface(
                            shape = CircleShape,
                            color = if (selected) c.inkPrimary else c.card,
                            modifier = Modifier
                                .height(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    splitMode = mode
                                    if (mode == SplitMode.ITEMIZED) showKeypad = false
                                }
                                .testTag("split_mode_${mode.name.lowercase()}"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) c.card else c.inkSecondary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Itemized Editor if in ITEMIZED mode
                if (splitMode == SplitMode.ITEMIZED) {
                    ItemizedBuilderCard(
                        items = itemizedRows,
                        groupMembers = groupMembers,
                        preferences = preferences,
                        onAddItem = {
                            itemizedRows.add(
                                ExpenseItem(
                                    id = "item_${UUID.randomUUID().toString().take(6)}",
                                    name = "Item ${itemizedRows.size + 1}",
                                    amountPaise = 0L,
                                    assignedMemberIds = groupMembers.map { it.id },
                                ),
                            )
                        },
                        onUpdateItem = { idx, updated -> itemizedRows[idx] = updated },
                        onDeleteItem = { idx ->
                            if (itemizedRows.size > 1) itemizedRows.removeAt(idx)
                        },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Member Split Rows Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.card,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Split Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.inkPrimary,
                            )
                            if (splitMode == SplitMode.EXACT && totalPaise > 0L) {
                                SliceStatusPill(
                                    text = if (splitDeltaPaise == 0L) {
                                        "Matched ✓"
                                    } else {
                                        "Left: ${SplitMath.formatMoney(splitDeltaPaise, preferences.currency, preferences.paisePrecision)}"
                                    },
                                    fillColor = if (splitDeltaPaise == 0L) c.mintFill else c.amberFill,
                                    textColor = if (splitDeltaPaise == 0L) c.mintInk else c.amberInk,
                                )
                            } else if (splitMode == SplitMode.PERCENTAGE) {
                                val sumPct = groupMembers.sumOf { pctInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
                                val ok = kotlin.math.abs(sumPct - 100.0) <= 0.5
                                SliceStatusPill(
                                    text = "${sumPct.toInt()}% / 100%",
                                    fillColor = if (ok) c.mintFill else c.amberFill,
                                    textColor = if (ok) c.mintInk else c.amberInk,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        groupMembers.forEachIndexed { index, member ->
                            val isIncluded = includedIds[member.id] ?: true
                            val memberSplitPaise = computedSplits
                                .firstOrNull { it.memberId == member.id }?.amountPaise ?: 0L

                            MemberSplitRow(
                                member = member,
                                isCurrentUser = member.id == currentUserId,
                                splitMode = splitMode,
                                isIncluded = isIncluded,
                                computedPaise = memberSplitPaise,
                                exactText = exactInputs[member.id] ?: "",
                                pctText = pctInputs[member.id] ?: "0",
                                shareUnit = shareUnits[member.id] ?: 1,
                                preferences = preferences,
                                onToggleInclude = {
                                    includedIds[member.id] = !isIncluded
                                },
                                onExactChange = { exactInputs[member.id] = it },
                                onPctChange = { pctInputs[member.id] = it },
                                onShareDelta = { delta ->
                                    val next = ((shareUnits[member.id] ?: 1) + delta).coerceIn(1, 20)
                                    shareUnits[member.id] = next
                                },
                            )
                            if (index < groupMembers.lastIndex) {
                                HorizontalDivider(color = c.divider)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Custom Minimal Numeric Keypad (Slice-style)
            AnimatedVisibility(visible = showKeypad && splitMode != SplitMode.ITEMIZED) {
                SliceNumericKeypad(
                    onDigit = { ch ->
                        if (ch == ".") {
                            if (!amountInput.contains(".")) {
                                amountInput = if (amountInput.isEmpty()) "0." else "$amountInput."
                            }
                        } else {
                            val parts = amountInput.split(".")
                            val decimalLenOk = parts.size < 2 || parts[1].length < 2
                            if (decimalLenOk && amountInput.length < 9) {
                                amountInput = if (amountInput == "0") ch else "$amountInput$ch"
                            }
                        }
                    },
                    onBackspace = {
                        if (amountInput.isNotEmpty()) {
                            amountInput = amountInput.dropLast(1)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MemberSplitRow(
    member: Member,
    isCurrentUser: Boolean,
    splitMode: SplitMode,
    isIncluded: Boolean,
    computedPaise: Long,
    exactText: String,
    pctText: String,
    shareUnit: Int,
    preferences: AppPreferences,
    onToggleInclude: () -> Unit,
    onExactChange: (String) -> Unit,
    onPctChange: (String) -> Unit,
    onShareDelta: (Int) -> Unit,
) {
    val c = SliceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = splitMode == SplitMode.EQUAL || splitMode == SplitMode.SHARES) {
                onToggleInclude()
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (splitMode == SplitMode.EQUAL || splitMode == SplitMode.SHARES) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isIncluded) c.inkPrimary else c.mist),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isIncluded) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Included",
                            tint = c.card,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            MemberAvatar(member = member, size = 34.dp)
            Text(
                text = if (isCurrentUser) "You" else member.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = c.inkPrimary,
            )
        }

        when (splitMode) {
            SplitMode.EQUAL, SplitMode.ITEMIZED -> {
                Text(
                    text = SplitMath.formatMoneyAlwaysDecimals(
                        computedPaise,
                        preferences.currency,
                        preferences.paisePrecision,
                    ),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = if (computedPaise > 0L) c.inkPrimary else c.inkSecondary,
                )
            }
            SplitMode.EXACT -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.mist)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = preferences.currency.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSecondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = exactText,
                        onValueChange = { next ->
                            if (next.all { it.isDigit() || it == '.' } && next.length <= 8) {
                                onExactChange(next)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(
                            color = c.inkPrimary,
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(c.inkPrimary),
                        modifier = Modifier.width(68.dp),
                    )
                }
            }
            SplitMode.PERCENTAGE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = SplitMath.formatMoney(computedPaise, preferences.currency, preferences.paisePrecision),
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = c.inkSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.mist)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        BasicTextField(
                            value = pctText,
                            onValueChange = { next ->
                                if (next.all { it.isDigit() || it == '.' } && next.length <= 5) {
                                    onPctChange(next)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.labelLarge.tabular().copy(
                                color = c.inkPrimary,
                                textAlign = TextAlign.End,
                            ),
                            cursorBrush = SolidColor(c.inkPrimary),
                            modifier = Modifier.width(42.dp),
                        )
                        Text("%", style = MaterialTheme.typography.labelLarge, color = c.inkSecondary)
                    }
                }
            }
            SplitMode.SHARES -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = SplitMath.formatMoney(computedPaise, preferences.currency, preferences.paisePrecision),
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = c.inkSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.mist)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onShareDelta(-1) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease share", modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = "${if (isIncluded) shareUnit else 0}x",
                            style = MaterialTheme.typography.labelLarge.tabular(),
                            color = c.inkPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onShareDelta(1) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase share", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemizedBuilderCard(
    items: List<ExpenseItem>,
    groupMembers: List<Member>,
    preferences: AppPreferences,
    onAddItem: () -> Unit,
    onUpdateItem: (Int, ExpenseItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
) {
    val c = SliceTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Line Items", style = MaterialTheme.typography.titleMedium, color = c.inkPrimary)
                Surface(
                    shape = CircleShape,
                    color = c.lavenderFill,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAddItem),
                ) {
                    Text(
                        text = "+ Add Item",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = c.lavenderInk,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            items.forEachIndexed { idx, item ->
                var itemPriceText by remember(item.id) {
                    mutableStateOf(
                        if (item.amountPaise > 0L) (item.amountPaise / 100L).toString() else "",
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.mist)
                        .padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BasicTextField(
                            value = item.name,
                            onValueChange = { onUpdateItem(idx, item.copy(name = it)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = c.inkPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(c.inkPrimary),
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.card)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(preferences.currency.symbol, color = c.inkSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            BasicTextField(
                                value = itemPriceText,
                                onValueChange = { next ->
                                    if (next.all { it.isDigit() || it == '.' } && next.length <= 7) {
                                        itemPriceText = next
                                        onUpdateItem(idx, item.copy(amountPaise = parseRupeesToPaise(next)))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.labelLarge.tabular().copy(
                                    color = c.inkPrimary,
                                    textAlign = TextAlign.End,
                                ),
                                cursorBrush = SolidColor(c.inkPrimary),
                                modifier = Modifier.width(56.dp),
                            )
                        }
                        if (items.size > 1) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove item",
                                tint = c.inkSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDeleteItem(idx) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        groupMembers.forEach { member ->
                            val assigned = member.id in item.assignedMemberIds
                            Surface(
                                shape = CircleShape,
                                color = if (assigned) c.inkPrimary else c.card,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val next = if (assigned) {
                                            item.assignedMemberIds - member.id
                                        } else {
                                            item.assignedMemberIds + member.id
                                        }
                                        onUpdateItem(idx, item.copy(assignedMemberIds = next))
                                    },
                            ) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (assigned) c.card else c.inkSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
                if (idx < items.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SliceNumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val c = SliceTheme.colors
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "BACK"),
    )

    Surface(
        color = c.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.mist.copy(alpha = 0.7f))
                                .clickable {
                                    if (key == "BACK") onBackspace() else onDigit(key)
                                }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key == "BACK") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Backspace",
                                    tint = c.inkPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineMedium.tabular(),
                                    color = c.inkPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseRupeesToPaise(input: String): Long {
    val value = input.toDoubleOrNull() ?: return 0L
    return (value * 100.0).roundToLong().coerceAtLeast(0L)
}
