package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.math.SplitMath
import com.example.data.model.ThemeMode
import com.example.ui.components.FloatingBottomDock
import com.example.ui.screens.AddExpenseSheet
import com.example.ui.screens.AccountSheet
import com.example.ui.screens.CreateGroupSheet
import com.example.ui.screens.CurrencySettingsSheet
import com.example.ui.screens.ExpenseDetailSheet
import com.example.ui.screens.ExportDataSheet
import com.example.ui.screens.FullActivityScreen
import com.example.ui.screens.GroupLedgerScreen
import com.example.ui.screens.GroupsTabScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinInviteBottomSheet
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OpenSourceInfoSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettleUpScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.SliceSplitTheme
import com.example.ui.theme.SliceTheme
import com.example.ui.viewmodel.ActiveSheet
import com.example.ui.viewmodel.AppStage
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.OverlayScreen
import com.example.ui.viewmodel.SliceSplitUiState
import com.example.ui.viewmodel.SliceSplitViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SliceSplitViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val isDark = when (state.preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
                onDispose {}
            }

            SliceSplitTheme(themeMode = state.preferences.themeMode) {
                SliceSplitApp(
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
fun SliceSplitApp(
    state: SliceSplitUiState,
    viewModel: SliceSplitViewModel,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accountEmail) {
        if (state.accountEmail != null && Build.VERSION.SDK_INT >= 33) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val askedBefore = context.getSharedPreferences("notification_permission", android.content.Context.MODE_PRIVATE)
                .getBoolean("asked", false)
            if (!askedBefore && ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                context.getSharedPreferences("notification_permission", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("asked", true).apply()
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            toastMessage = msg
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(if (toastMessage?.startsWith("Invite code for") == true) 9000L else 2200L)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        state.updateAvailable?.let { update ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdate() },
                title = { Text("Bhai Paise update available") },
                text = { Text("Version ${update.version} is ready to download.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissUpdate()
                        val target = update.downloadUrl ?: update.releaseUrl
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                        }
                    }) { Text("Download update") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdate() }) { Text("Later") }
                },
            )
        }

        // Primary stage transition (Splash -> Onboarding -> Main)
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = {
                fadeIn(spring(dampingRatio = 0.9f, stiffness = 420f)) togetherWith
                    fadeOut(spring(dampingRatio = 1f, stiffness = 520f))
            },
            label = "appStageTransition",
        ) { stage ->
            when (stage) {
                AppStage.SPLASH -> {
                    SplashScreen(
                        progress = state.splashProgress,
                        onSkip = { viewModel.skipSplash() },
                    )
                }

                AppStage.ONBOARDING -> {
                    OnboardingScreen(
                        members = state.members,
                        preferences = state.preferences,
                        onStartSplitting = { viewModel.completeOnboarding() },
                        onOpenJoinInvite = { viewModel.openSheet(ActiveSheet.JoinInvite) },
                    )
                }

                AppStage.MAIN -> {
                    MainWorkspace(
                        state = state,
                        viewModel = viewModel,
                    )
                }
            }
        }

        // Full-height Add / Edit Expense Sheet
        AnimatedVisibility(
            visible = state.activeSheet is ActiveSheet.AddExpense,
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) { it / 3 } +
                fadeIn(spring(dampingRatio = 1f, stiffness = 520f)),
            exit = slideOutVertically(spring(dampingRatio = 0.94f, stiffness = 520f)) { it / 3 } +
                fadeOut(spring(dampingRatio = 1f, stiffness = 620f)),
        ) {
            val addArgs = state.activeSheet as? ActiveSheet.AddExpense
            val editingExpense = addArgs?.editingExpenseId?.let { id ->
                state.expenses.firstOrNull { it.id == id }
            }
            AddExpenseSheet(
                groups = state.groups,
                membersMap = state.membersMap,
                currentUserId = state.currentUser?.id ?: "alex",
                preferences = state.preferences,
                preselectedGroupId = addArgs?.preselectedGroupId,
                editingExpense = editingExpense,
                onDismiss = { viewModel.dismissSheet() },
                onSave = { editingId, groupId, title, amountPaise, paidById, mode, category, splits, items ->
                    viewModel.saveExpense(
                        editingId = editingId,
                        groupId = groupId,
                        title = title,
                        amountPaise = amountPaise,
                        paidByMemberId = paidById,
                        splitMode = mode,
                        category = category,
                        splits = splits,
                        items = items,
                    )
                },
                onValidationMessage = { viewModel.showToast(it) },
            )
        }

        // Modal Bottom Sheets
        when (val sheet = state.activeSheet) {
            ActiveSheet.Account -> {
                AccountSheet(
                    state = state,
                    onDismiss = { viewModel.dismissSheet() },
                    onCreateAccount = { name, email, password -> viewModel.createAccount(name, email, password) },
                    onSignIn = { email, password -> viewModel.signIn(email, password) },
                    onResetPassword = { email -> viewModel.resetAccountPassword(email) },
                    onSignOut = { viewModel.signOut() },
                    onClearMessage = { viewModel.clearAccountMessage() },
                )
            }

            ActiveSheet.JoinInvite -> {
                JoinInviteBottomSheet(
                    onDismiss = { viewModel.dismissSheet() },
                    onJoinCode = { viewModel.joinByInviteCode(it) },
                )
            }

            ActiveSheet.CreateGroup -> {
                CreateGroupSheet(
                    members = state.members,
                    onDismiss = { viewModel.dismissSheet() },
                    onCreate = { name, iconKey, memberIds, newPeople ->
                        viewModel.createGroup(name, iconKey, memberIds, newPeople)
                    },
                )
            }

            is ActiveSheet.ExpenseDetail -> {
                val expense = state.expenses.firstOrNull { it.id == sheet.expenseId }
                if (expense != null) {
                    val groupName = state.groups.firstOrNull { it.id == expense.groupId }?.name ?: "Group"
                    ExpenseDetailSheet(
                        expense = expense,
                        groupName = groupName,
                        membersMap = state.membersMap,
                        currentUserId = state.currentUser?.id ?: "alex",
                        preferences = state.preferences,
                        onDismiss = { viewModel.dismissSheet() },
                        onEdit = {
                            viewModel.openAddExpense(
                                preselectedGroupId = expense.groupId,
                                editingExpenseId = expense.id,
                            )
                        },
                        onDelete = { viewModel.deleteExpense(expense.id) },
                    )
                }
            }

            ActiveSheet.CurrencySettings -> {
                CurrencySettingsSheet(
                    preferences = state.preferences,
                    onSelectCurrency = { viewModel.setCurrency(it) },
                    onTogglePaise = { viewModel.setPaisePrecision(it) },
                    onDismiss = { viewModel.dismissSheet() },
                )
            }

            ActiveSheet.ExportData -> {
                ExportDataSheet(
                    state = state,
                    onShowToast = { viewModel.showToast(it) },
                    onDismiss = { viewModel.dismissSheet() },
                )
            }

            ActiveSheet.OpenSourceInfo -> {
                OpenSourceInfoSheet(
                    onReplayOnboarding = { viewModel.previewSplashAndOnboarding() },
                    onResetDemo = { viewModel.resetDemoData() },
                    onDismiss = { viewModel.dismissSheet() },
                )
            }

            is ActiveSheet.AddExpense,
            ActiveSheet.None,
            -> Unit
        }

        // Floating minimal feedback pill toast
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(spring(dampingRatio = 1f, stiffness = 600f)) +
                slideInVertically(spring(dampingRatio = 0.86f, stiffness = 480f)) { 24 },
            exit = fadeOut(spring(dampingRatio = 1f, stiffness = 700f)) +
                slideOutVertically(spring(dampingRatio = 1f, stiffness = 700f)) { 16 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            val c = SliceTheme.colors
            Surface(
                shape = CircleShape,
                color = c.inkPrimary,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.card,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun MainWorkspace(
    state: SliceSplitUiState,
    viewModel: SliceSplitViewModel,
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Tabs
        AnimatedContent(
            targetState = state.activeTab,
            transitionSpec = {
                fadeIn(spring(dampingRatio = 0.96f, stiffness = 460f)) togetherWith
                    fadeOut(spring(dampingRatio = 1f, stiffness = 600f))
            },
            label = "mainTabTransition",
        ) { tab ->
            when (tab) {
                MainTab.HOME -> {
                    HomeScreen(
                        state = state,
                        onToggleSearch = { viewModel.toggleSearch() },
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onGroupClick = { viewModel.openGroupLedger(it) },
                        onSettlePending = { groupId, transfer -> viewModel.openSettleUp(groupId, transfer) },
                        onRemind = { member, amountPaise ->
                            val formatted = SplitMath.formatMoney(
                                amountPaise,
                                state.preferences.currency,
                                state.preferences.paisePrecision,
                            )
                            viewModel.showToast("Reminder ready for ${member.name} ($formatted)")
                        },
                        onAddFirstExpense = { viewModel.openAddExpense() },
                    )
                }

                MainTab.GROUPS -> {
                    GroupsTabScreen(
                        state = state,
                        onGroupClick = { viewModel.openGroupLedger(it) },
                        onCreateGroupClick = { viewModel.openSheet(ActiveSheet.CreateGroup) },
                        onJoinGroupClick = { viewModel.openSheet(ActiveSheet.JoinInvite) },
                    )
                }

                MainTab.PROFILE -> {
                    ProfileScreen(
                        state = state,
                        onGroupClick = { viewModel.openGroupLedger(it) },
                        onOpenFullActivity = { viewModel.openFullActivity() },
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onOpenCurrencySettings = { viewModel.openSheet(ActiveSheet.CurrencySettings) },
                        onOpenExportData = { viewModel.openSheet(ActiveSheet.ExportData) },
                        onOpenSourceInfo = { viewModel.openSheet(ActiveSheet.OpenSourceInfo) },
                        onShowToast = { viewModel.showToast(it) },
                        onOpenAccount = { viewModel.openSheet(ActiveSheet.Account) },
                        onUpdateUpiId = { viewModel.updateMyUpiId(it) },
                        accountEmail = state.accountEmail,
                    )
                }
            }
        }

        // Overlay Screens: Group Ledger & Settle Up
        AnimatedContent(
            targetState = state.overlayScreen,
            transitionSpec = {
                fadeIn(spring(dampingRatio = 0.88f, stiffness = 360f)) togetherWith
                    fadeOut(spring(dampingRatio = 1f, stiffness = 520f))
            },
            label = "overlayTransition",
        ) { overlay ->
            when (overlay) {
                OverlayScreen.None -> Unit

                OverlayScreen.FullActivity -> {
                    FullActivityScreen(
                        state = state,
                        onBack = { viewModel.closeOverlay() },
                        onExpenseClick = { viewModel.openExpenseDetail(it) },
                        onAddExpense = { viewModel.openAddExpense() },
                    )
                }

                is OverlayScreen.GroupLedger -> {
                    GroupLedgerScreen(
                        groupId = overlay.groupId,
                        state = state,
                        onBack = { viewModel.closeOverlay() },
                        onSettleUp = { transfer -> viewModel.openSettleUp(overlay.groupId, transfer) },
                        onRemind = { member, amountPaise ->
                            val formatted = SplitMath.formatMoney(
                                amountPaise,
                                state.preferences.currency,
                                state.preferences.paisePrecision,
                            )
                            viewModel.showToast("Reminder ready for ${member.name} ($formatted)")
                        },
                        onAddExpense = { viewModel.openAddExpense(preselectedGroupId = overlay.groupId) },
                        onExpenseClick = { viewModel.openExpenseDetail(it) },
                        onCopyInviteCode = { code ->
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Bhai Paise invite code", code))
                            viewModel.showToast("Invite code copied")
                        },
                        onShareInviteCode = { code ->
                            val groupName = state.groups.firstOrNull { it.id == overlay.groupId }?.name ?: "my group"
                            val inviteMessage = "Join $groupName on Bhai Paise. Open Groups → Join and enter this code: $code. The code expires in 7 days."
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, inviteMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Invite to $groupName"))
                        },
                    )
                }

                is OverlayScreen.SettleUp -> {
                    val summary = state.groupSummaries.firstOrNull { it.group.id == overlay.groupId }
                    SettleUpScreen(
                        groupName = summary?.group?.name ?: "Group",
                        transfers = summary?.simplifiedTransfers ?: emptyList(),
                        initialFromId = overlay.fromMemberId,
                        initialToId = overlay.toMemberId,
                        initialAmountPaise = overlay.amountPaise,
                        membersMap = state.membersMap,
                        currentUserId = state.currentUser?.id ?: "alex",
                        preferences = state.preferences,
                        onBack = { viewModel.closeOverlay() },
                        onConfirmSettlement = { fromId, toId, amountPaise, method ->
                            viewModel.recordSettlement(
                                groupId = overlay.groupId,
                                fromMemberId = fromId,
                                toMemberId = toId,
                                amountPaise = amountPaise,
                                method = method,
                            )
                        },
                        onCopyUpiId = { upiId ->
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", upiId))
                            viewModel.showToast("UPI ID copied")
                        },
                        onPayWithUpi = { upiId, payeeName, amountPaise ->
                            if (state.preferences.currency.code != "INR") {
                                viewModel.showToast("UPI payments are available for INR bills.")
                            } else {
                                val amount = String.format(java.util.Locale.US, "%.2f", amountPaise / 100.0)
                                val paymentUri = Uri.Builder()
                                    .scheme("upi")
                                    .authority("pay")
                                    .appendQueryParameter("pa", upiId)
                                    .appendQueryParameter("pn", payeeName)
                                    .appendQueryParameter("am", amount)
                                    .appendQueryParameter("cu", "INR")
                                    .appendQueryParameter("tn", "Bhai Paise bill split")
                                    .build()
                                try {
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, paymentUri), "Pay with UPI"))
                                } catch (_: ActivityNotFoundException) {
                                    viewModel.showToast("No UPI payment app is installed.")
                                }
                            }
                        },
                    )
                }
            }
        }

        // Floating Dark Pill Bottom Dock + Detached Circular + FAB
        AnimatedVisibility(
            visible = state.overlayScreen == OverlayScreen.None,
            enter = fadeIn(spring(dampingRatio = 1f, stiffness = 520f)) +
                slideInVertically(spring(dampingRatio = 0.84f, stiffness = 360f)) { it / 3 },
            exit = fadeOut(spring(dampingRatio = 1f, stiffness = 620f)) +
                slideOutVertically(spring(dampingRatio = 0.96f, stiffness = 520f)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBottomDock(
                activeTab = state.activeTab,
                onSelectTab = { viewModel.selectTab(it) },
                onAddClick = { viewModel.openAddExpense() },
            )
        }
    }
}
