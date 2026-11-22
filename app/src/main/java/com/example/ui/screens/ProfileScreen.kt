package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.CurrencyOption
import com.example.data.model.GroupSummary
import com.example.data.model.ThemeMode
import com.example.ui.components.AvatarStack
import com.example.ui.components.HeroNetBalanceCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SliceStatusPill
import com.example.ui.components.groupIcon
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.SliceSplitUiState

@Composable
fun ProfileScreen(
    state: SliceSplitUiState,
    onGroupClick: (String) -> Unit,
    onOpenFullActivity: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onOpenCurrencySettings: () -> Unit,
    onOpenExportData: () -> Unit,
    onOpenSourceInfo: () -> Unit,
    onShowToast: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onUpdateUpiId: (String) -> Unit,
    accountEmail: String?,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    val context = LocalContext.current
    val currentUser = state.currentUser
    val prefs = state.preferences
    var editingUpi by remember(currentUser?.id) { mutableStateOf(false) }
    var upiDraft by remember(currentUser?.id, currentUser?.upiId) { mutableStateOf(currentUser?.upiId.orEmpty()) }

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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "account_access") {
                Button(
                    onClick = onOpenAccount,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.inkPrimary, contentColor = c.card),
                ) {
                    Text(if (accountEmail == null) "Connect account for syncing" else "Account: $accountEmail")
                }
            }

            // User Header Card
            item(key = "profile_header") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.card,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (currentUser != null) {
                            MemberAvatar(member = currentUser, size = 76.dp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentUser?.name ?: "You",
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // UPI ID Pill with one-tap copy
                        val upiId = currentUser?.upiId.orEmpty()
                        Surface(
                            shape = CircleShape,
                            color = c.lavenderFill,
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(if (upiId.isNotBlank()) Modifier.clickable {
                                    copyToClipboard(context, "UPI ID", upiId)
                                    onShowToast("Copied $upiId")
                                } else Modifier)
                                .testTag("copy_upi_pill"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = upiId.ifBlank { "UPI ID not set" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.lavenderInk,
                                )
                                if (upiId.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy UPI ID",
                                        tint = c.lavenderInk,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }

                        if (editingUpi) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = upiDraft,
                                onValueChange = { upiDraft = it },
                                label = { Text("UPI ID") },
                                placeholder = { Text("name@bank") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("profile_upi_input"),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editingUpi = false }) { Text("Cancel") }
                                val validUpi = upiDraft.isBlank() || upiDraft.trim().matches(
                                    Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$"),
                                )
                                Button(
                                    enabled = validUpi,
                                    onClick = {
                                        onUpdateUpiId(upiDraft)
                                        editingUpi = false
                                    },
                                    shape = CircleShape,
                                ) { Text("Save UPI ID") }
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    upiDraft = currentUser?.upiId.orEmpty()
                                    editingUpi = true
                                },
                                modifier = Modifier.testTag("edit_profile_upi"),
                            ) {
                                Text(if (upiId.isBlank()) "Add UPI ID" else "Edit UPI ID")
                            }
                        }
                        Text(
                            "Shared with members of your groups",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSecondary,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3 minimal stat blocks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ProfileStatBlock(
                                label = "Groups",
                                value = state.groups.size.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatBlock(
                                label = "Splits",
                                value = state.expenses.count { !it.isSettlement }.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatBlock(
                                label = "Settled",
                                value = state.totalSettledCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Net Balance Card moved to Profile
            item(key = "profile_net_balance") {
                HeroNetBalanceCard(
                    netPaise = state.netBalancePaise,
                    owedPaise = state.totalOwedToYouPaise,
                    youOwePaise = state.totalYouOwePaise,
                    preferences = prefs,
                )
            }

            // Clickable Activity Summary Option -> Full Activity View
            item(key = "profile_activity_summary_option") {
                val totalBills = state.expenses.count { !it.isSettlement }
                val totalSpend = state.expenses.filterNot { it.isSettlement }.sumOf { it.amountPaise }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.card,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenFullActivity)
                        .testTag("profile_activity_summary_option"),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c.lavenderFill),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = "Activity Summary",
                                tint = c.lavenderInk,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activity Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.inkPrimary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalBills bills • ${
                                    SplitMath.formatMoney(totalSpend, prefs.currency, prefs.paisePrecision)
                                } total",
                                style = MaterialTheme.typography.bodyMedium.tabular(),
                                color = c.inkSecondary,
                            )
                        }
                        SliceStatusPill(
                            text = "Full view",
                            fillColor = c.mist,
                            textColor = c.inkPrimary,
                        )
                    }
                }
            }

            // Active Groups Spend & Receive Breakdown Section
            item(key = "active_groups_header") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Active Groups Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.inkPrimary,
                    )
                    Text(
                        text = "Spent & Received",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkSecondary,
                    )
                }
            }

            items(state.groupSummaries, key = { "profile_group_${it.group.id}" }) { summary ->
                ProfileActiveGroupCard(
                    summary = summary,
                    preferences = prefs,
                    onClick = { onGroupClick(summary.group.id) },
                )
            }

            // Grouped Settings Card
            item(key = "profile_settings_header") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.inkPrimary,
                )
            }

            item(key = "profile_settings_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = c.card,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        // Row 1: Default Currency & Exact Rounding
                        ProfileSettingRow(
                            icon = Icons.Outlined.CurrencyRupee,
                            title = "Default Currency & Exact Rounding",
                            subtitle = "${prefs.currency.code} ${prefs.currency.symbol} • ${
                                if (prefs.paisePrecision) "Paise precision" else "Whole rupee rounding"
                            }",
                            onClick = onOpenCurrencySettings,
                            testTag = "setting_currency_row",
                        )

                        HorizontalDivider(color = c.divider, modifier = Modifier.padding(horizontal = 18.dp))

                        // Row 2: Theme Mode interactive toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(c.mist),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = c.inkPrimary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = c.inkPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = c.mist,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(modifier = Modifier.padding(4.dp)) {
                                    ThemeMode.entries.forEach { mode ->
                                        val selected = prefs.themeMode == mode
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selected) c.card else androidx.compose.ui.graphics.Color.Transparent,
                                            shadowElevation = if (selected) 1.dp else 0.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(CircleShape)
                                                .clickable { onSetThemeMode(mode) }
                                                .testTag("theme_mode_${mode.name.lowercase()}"),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = mode.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) c.inkPrimary else c.inkSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = c.divider, modifier = Modifier.padding(horizontal = 18.dp))

                        // Row 3: Export Data
                        ProfileSettingRow(
                            icon = Icons.Outlined.FileDownload,
                            title = "Export Data",
                            subtitle = "JSON / CSV portable ledger",
                            onClick = onOpenExportData,
                            testTag = "setting_export_row",
                        )

                        HorizontalDivider(color = c.divider, modifier = Modifier.padding(horizontal = 18.dp))

                        // Row 4: Open Source & Transparency
                        ProfileSettingRow(
                            icon = Icons.Outlined.Code,
                            title = "Open Source & Transparency",
                            subtitle = "MIT License • GitHub Repo • Zero Tracking",
                            onClick = onOpenSourceInfo,
                            testTag = "setting_opensource_row",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActiveGroupCard(
    summary: GroupSummary,
    preferences: AppPreferences,
    onClick: () -> Unit,
) {
    val c = SliceTheme.colors
    val net = summary.currentUserNetPaise
    val statusText = when {
        summary.expenseCount == 0 -> "No splits"
        net == 0L && summary.simplifiedTransfers.isEmpty() -> "All settled ✓"
        net == 0L -> "Even ✓"
        else -> SplitMath.formatMoney(net, preferences.currency, preferences.paisePrecision, showPlusSign = true)
    }
    val (statusFill, statusInk) = when {
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
            .testTag("profile_group_card_${summary.group.id}"),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c.mist),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = groupIcon(summary.group.iconKey),
                            contentDescription = summary.group.name,
                            tint = c.inkPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = summary.group.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = c.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AvatarStack(members = summary.members, avatarSize = 20.dp)
                    }
                }

                SliceStatusPill(
                    text = statusText,
                    fillColor = statusFill,
                    textColor = statusInk,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.mist,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSecondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SplitMath.formatMoney(
                                summary.totalSpendPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            ),
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            color = c.inkPrimary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.mintFill.copy(alpha = if (c.isDark) 0.55f else 0.7f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Received / Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.mintInk,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SplitMath.formatMoney(
                                summary.totalReceivedPaise,
                                preferences.currency,
                                preferences.paisePrecision,
                            ),
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            color = c.mintInk,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val c = SliceTheme.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = c.mist,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.tabular(),
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSecondary,
            )
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val c = SliceTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(c.mist),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.inkPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = c.inkPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = c.inkSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsSheet(
    preferences: AppPreferences,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onTogglePaise: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = SliceTheme.colors
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
            Text("Currency & Rounding", style = MaterialTheme.typography.headlineMedium, color = c.inkPrimary)
            Spacer(modifier = Modifier.height(14.dp))

            CurrencyOption.entries.forEach { option ->
                val selected = preferences.currency == option
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) c.lavenderFill else c.mist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectCurrency(option) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) c.lavenderInk else c.inkPrimary,
                        )
                        if (selected) {
                            SliceStatusPill("Active", c.card, c.inkPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = c.mist,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paise / Decimal Precision",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.inkPrimary,
                        )
                        Text(
                            text = "Show exact decimals instead of rounding",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSecondary,
                        )
                    }
                    Switch(
                        checked = preferences.paisePrecision,
                        onCheckedChange = onTogglePaise,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.card,
                            checkedTrackColor = c.inkPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataSheet(
    state: SliceSplitUiState,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = SliceTheme.colors
    val context = LocalContext.current
    var format by remember { mutableStateOf("JSON") }
    val payload = remember(format, state.groups, state.expenses, state.preferences.currency) {
        if (format == "JSON") {
            SplitMath.exportAsJson(state.groups, state.expenses, state.membersMap, state.preferences.currency)
        } else {
            SplitMath.exportAsCsv(state.groups, state.expenses, state.membersMap, state.preferences.currency)
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
            Text("Export Ledger", style = MaterialTheme.typography.headlineMedium, color = c.inkPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your bills belong to you. Export anytime with zero lock-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("JSON", "CSV").forEach { fmt ->
                    val selected = format == fmt
                    Surface(
                        shape = CircleShape,
                        color = if (selected) c.inkPrimary else c.mist,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { format = fmt },
                    ) {
                        Text(
                            text = fmt,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) c.card else c.inkPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = c.mist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                Text(
                    text = payload,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = c.inkPrimary,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = c.mist,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .clickable {
                            copyToClipboard(context, "Bhai Paise $format", payload)
                            onShowToast("Copied $format export")
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Bhai Paise $format Export")
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Bhai Paise Data"))
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.inkPrimary, contentColor = c.card),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceInfoSheet(
    onReplayOnboarding: () -> Unit,
    onResetDemo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = SliceTheme.colors
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Bhai Paise", style = MaterialTheme.typography.headlineMedium, color = c.inkPrimary)
                SliceStatusPill("MIT License", c.mintFill, c.mintInk)
            }
            Text(
                text = "Local-first bill splitting with zero ads, zero paywalls, and zero analytics SDKs. All calculations run deterministically in integer paise on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = c.mist,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Repository: github.com/bhaipaise/bhaipaise-android", style = MaterialTheme.typography.bodyMedium, color = c.inkPrimary)
                    Text("• Telemetry & Tracking: None (0 bytes collected)", style = MaterialTheme.typography.bodyMedium, color = c.inkPrimary)
                    Text("• Storage: Local SQLite (Room) + DataStore", style = MaterialTheme.typography.bodyMedium, color = c.inkPrimary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = c.mist,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onReplayOnboarding),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("View Welcome", style = MaterialTheme.typography.labelLarge, color = c.inkPrimary)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = c.lavenderFill,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onResetDemo)
                        .testTag("reset_demo_data_button"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = c.lavenderInk, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Demo", style = MaterialTheme.typography.labelLarge, color = c.lavenderInk)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
