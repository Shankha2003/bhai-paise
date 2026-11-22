package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SliceSplitDatabase
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.CurrencyOption
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseItem
import com.example.data.model.Group
import com.example.data.model.GroupSummary
import com.example.data.model.Member
import com.example.data.model.MemberSplit
import com.example.data.model.NewGroupPerson
import com.example.data.model.PaymentMethod
import com.example.data.model.PendingPayment
import com.example.data.model.SimplifiedTransfer
import com.example.data.model.SplitMode
import com.example.data.model.ThemeMode
import com.example.data.prefs.PreferencesRepository
import com.example.data.repository.SliceSplitRepository
import com.example.data.repository.FirebaseAccountRepository
import com.example.data.repository.FirebaseCollaborationRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppStage {
    SPLASH,
    ONBOARDING,
    MAIN,
}

enum class MainTab {
    HOME,
    GROUPS,
    PROFILE,
}

sealed interface OverlayScreen {
    data object None : OverlayScreen
    data object FullActivity : OverlayScreen
    data class GroupLedger(val groupId: String) : OverlayScreen
    data class SettleUp(
        val groupId: String,
        val fromMemberId: String? = null,
        val toMemberId: String? = null,
        val amountPaise: Long = 0L,
    ) : OverlayScreen
}

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data class AddExpense(
        val preselectedGroupId: String? = null,
        val editingExpenseId: String? = null,
    ) : ActiveSheet

    data class ExpenseDetail(val expenseId: String) : ActiveSheet
    data object JoinInvite : ActiveSheet
    data object CreateGroup : ActiveSheet
    data object CurrencySettings : ActiveSheet
    data object ExportData : ActiveSheet
    data object OpenSourceInfo : ActiveSheet
    data object Account : ActiveSheet
}

data class SliceSplitUiState(
    val stage: AppStage = AppStage.SPLASH,
    val splashProgress: Float = 0f,
    val activeTab: MainTab = MainTab.HOME,
    val overlayScreen: OverlayScreen = OverlayScreen.None,
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val members: List<Member> = emptyList(),
    val membersMap: Map<String, Member> = emptyMap(),
    val currentUser: Member? = null,
    val groups: List<Group> = emptyList(),
    val groupSummaries: List<GroupSummary> = emptyList(),
    val pendingPayments: List<PendingPayment> = emptyList(),
    val lastPaymentUserOwes: PendingPayment? = null,
    val expenses: List<Expense> = emptyList(),
    val totalOwedToYouPaise: Long = 0L,
    val totalYouOwePaise: Long = 0L,
    val netBalancePaise: Long = 0L,
    val totalSettledCount: Int = 0,
    val preferences: AppPreferences = AppPreferences(),
    val firebaseConfigured: Boolean = false,
    val accountEmail: String? = null,
    val accountBusy: Boolean = false,
    val accountMessage: String? = null,
    val groupInviteCodes: Map<String, String> = emptyMap(),
)

class SliceSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SliceSplitDatabase.getInstance(application)
    private val repository = SliceSplitRepository(database.sliceSplitDao())
    private val prefsRepository = PreferencesRepository(application)
    private val accountRepository = FirebaseAccountRepository(application)
    private val collaborationRepository = FirebaseCollaborationRepository(application)
    private var cloudGroupsListener: ListenerRegistration? = null
    private val _firebaseConfigured = MutableStateFlow(accountRepository.isConfigured)
    private val _accountEmail = MutableStateFlow(accountRepository.verifiedEmail)
    private val _accountBusy = MutableStateFlow(false)
    private val _accountMessage = MutableStateFlow<String?>(null)
    private val _groupInviteCodes = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _stage = MutableStateFlow(AppStage.SPLASH)
    private val _splashProgress = MutableStateFlow(0f)
    private val _activeTab = MutableStateFlow(MainTab.HOME)
    private val _overlayScreen = MutableStateFlow<OverlayScreen>(OverlayScreen.None)
    private val _activeSheet = MutableStateFlow<ActiveSheet>(ActiveSheet.None)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    private data class CoreUiInputs(
        val nav: NavSubState,
        val members: List<Member>,
        val groups: List<Group>,
        val expenses: List<Expense>,
        val preferences: AppPreferences,
    )

    private data class AccountUiInputs(
        val firebaseConfigured: Boolean,
        val email: String?,
        val busy: Boolean,
        val message: String?,
        val inviteCodes: Map<String, String>,
    )

    private data class NavSubState(
        val stage: AppStage,
        val splashProgress: Float,
        val activeTab: MainTab,
        val overlayScreen: OverlayScreen,
        val activeSheet: ActiveSheet,
        val isSearchActive: Boolean,
        val searchQuery: String,
    )

    private val navFlow = combine(
        _stage,
        _splashProgress,
        _activeTab,
        _overlayScreen,
        _activeSheet,
    ) { stage, progress, tab, overlay, sheet ->
        NavSubState(
            stage = stage,
            splashProgress = progress,
            activeTab = tab,
            overlayScreen = overlay,
            activeSheet = sheet,
            isSearchActive = false,
            searchQuery = "",
        )
    }.combine(
        combine(_isSearchActive, _searchQuery) { searchOn, query ->
            Pair(searchOn, query)
        },
    ) { nav, extra ->
        nav.copy(
            isSearchActive = extra.first,
            searchQuery = extra.second,
        )
    }

    val uiState: StateFlow<SliceSplitUiState> = combine(
        navFlow,
        repository.members,
        repository.groups,
        repository.expenses,
        prefsRepository.preferences,
    ) { nav, members, groups, expenses, prefs ->
        CoreUiInputs(nav, members, groups, expenses, prefs)
    }.combine(
        combine(_firebaseConfigured, _accountEmail, _accountBusy, _accountMessage, _groupInviteCodes) { configured, email, busy, message, invites ->
            AccountUiInputs(configured, email, busy, message, invites)
        },
    ) { core, account ->
        val nav = core.nav
        val members = core.members
        val groups = core.groups
        val expenses = core.expenses
        val prefs = core.preferences
        val firebaseConfigured = account.firebaseConfigured
        val accountEmail = account.email
        val accountBusy = account.busy
        val accountMessage = account.message
        val membersMap = members.associateBy { it.id }
        val currentUser = members.firstOrNull { it.isCurrentUser } ?: members.firstOrNull()
        val currentUserId = currentUser?.id ?: "alex"

        val summaries = groups.map { group ->
            val groupExpenses = expenses.filter { it.groupId == group.id }
            val nets = SplitMath.computeMemberNets(group.memberIds, groupExpenses)
            val transfers = SplitMath.simplifyDebts(group.memberIds, groupExpenses)
            val totalSpend = groupExpenses.filterNot { it.isSettlement }.sumOf { it.amountPaise }
            val totalReceived = groupExpenses.filter { it.isSettlement }.sumOf { it.amountPaise }
            GroupSummary(
                group = group,
                members = group.memberIds.mapNotNull { membersMap[it] },
                totalSpendPaise = totalSpend,
                totalReceivedPaise = totalReceived,
                currentUserNetPaise = nets[currentUserId] ?: 0L,
                simplifiedTransfers = transfers,
                expenseCount = groupExpenses.count { !it.isSettlement },
            )
        }

        val pendingPayments = summaries
            .flatMap { summary ->
                summary.simplifiedTransfers.map { transfer ->
                    PendingPayment(
                        groupId = summary.group.id,
                        groupName = summary.group.name,
                        transfer = transfer,
                    )
                }
            }
            .sortedByDescending {
                it.transfer.fromMemberId == currentUserId || it.transfer.toMemberId == currentUserId
            }

        val latestGroupActivityTime = expenses
            .groupBy { it.groupId }
            .mapValues { (_, list) -> list.maxOfOrNull { it.createdAt } ?: 0L }

        val lastPaymentUserOwes = pendingPayments
            .filter { it.transfer.fromMemberId == currentUserId }
            .maxByOrNull { latestGroupActivityTime[it.groupId] ?: 0L }

        val totalOwed = summaries.sumOf { it.currentUserNetPaise.coerceAtLeast(0L) }
        val totalOwe = summaries.sumOf { (-it.currentUserNetPaise).coerceAtLeast(0L) }
        val netBalance = totalOwed - totalOwe
        val settledGroupsCount = summaries.count { it.expenseCount > 0 && it.simplifiedTransfers.isEmpty() } +
            expenses.count { it.isSettlement }

        SliceSplitUiState(
            stage = nav.stage,
            splashProgress = nav.splashProgress,
            activeTab = nav.activeTab,
            overlayScreen = nav.overlayScreen,
            activeSheet = nav.activeSheet,
            isSearchActive = nav.isSearchActive,
            searchQuery = nav.searchQuery,
            members = members,
            membersMap = membersMap,
            currentUser = currentUser,
            groups = groups,
            groupSummaries = summaries,
            pendingPayments = pendingPayments,
            lastPaymentUserOwes = lastPaymentUserOwes,
            expenses = expenses,
            totalOwedToYouPaise = totalOwed,
            totalYouOwePaise = totalOwe,
            netBalancePaise = netBalance,
            totalSettledCount = settledGroupsCount,
            preferences = prefs,
            firebaseConfigured = firebaseConfigured,
            accountEmail = accountEmail,
            accountBusy = accountBusy,
            accountMessage = accountMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SliceSplitUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            if (collaborationRepository.isSignedIn) startCloudSync()
            // Smooth 1.0s splash progress animation
            val steps = 20
            for (i in 1..steps) {
                _splashProgress.value = i / steps.toFloat()
                delay(45L)
            }
            val currentPrefs = uiState.value.preferences
            _stage.value = AppStage.MAIN
        }
    }

    fun skipSplash() {
        val currentPrefs = uiState.value.preferences
        _stage.value = AppStage.MAIN
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepository.setOnboardingCompleted(true)
            _stage.value = AppStage.MAIN
        }
    }

    fun previewSplashAndOnboarding() {
        _activeSheet.value = ActiveSheet.None
        _overlayScreen.value = OverlayScreen.None
        _stage.value = AppStage.ONBOARDING
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
        _overlayScreen.value = OverlayScreen.None
    }

    fun openGroupLedger(groupId: String) {
        _overlayScreen.value = OverlayScreen.GroupLedger(groupId)
    }

    fun openFullActivity() {
        _overlayScreen.value = OverlayScreen.FullActivity
    }

    fun openSettleUp(
        groupId: String,
        transfer: SimplifiedTransfer? = null,
    ) {
        val summary = uiState.value.groupSummaries.firstOrNull { it.group.id == groupId }
        val chosen = transfer ?: summary?.simplifiedTransfers?.firstOrNull()
        _overlayScreen.value = OverlayScreen.SettleUp(
            groupId = groupId,
            fromMemberId = chosen?.fromMemberId,
            toMemberId = chosen?.toMemberId,
            amountPaise = chosen?.amountPaise ?: 0L,
        )
    }

    fun closeOverlay() {
        when (val current = _overlayScreen.value) {
            is OverlayScreen.SettleUp -> {
                _overlayScreen.value = OverlayScreen.GroupLedger(current.groupId)
            }
            is OverlayScreen.GroupLedger,
            OverlayScreen.FullActivity,
            -> {
                _overlayScreen.value = OverlayScreen.None
            }
            OverlayScreen.None -> Unit
        }
    }

    fun openAddExpense(preselectedGroupId: String? = null, editingExpenseId: String? = null) {
        val ledgerGroupId = (overlayScreenGroupId()) ?: preselectedGroupId
        _activeSheet.value = ActiveSheet.AddExpense(
            preselectedGroupId = ledgerGroupId,
            editingExpenseId = editingExpenseId,
        )
    }

    fun openExpenseDetail(expenseId: String) {
        _activeSheet.value = ActiveSheet.ExpenseDetail(expenseId)
    }

    fun openSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun dismissSheet() {
        _activeSheet.value = ActiveSheet.None
    }

    fun toggleSearch() {
        val next = !_isSearchActive.value
        _isSearchActive.value = next
        if (!next) _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveExpense(
        editingId: String?,
        groupId: String,
        title: String,
        amountPaise: Long,
        paidByMemberId: String,
        splitMode: SplitMode,
        category: ExpenseCategory,
        splits: List<MemberSplit>,
        items: List<ExpenseItem>,
    ) {
        viewModelScope.launch {
            val existing = editingId?.let { id -> uiState.value.expenses.firstOrNull { it.id == id } }
            val expense = Expense(
                id = existing?.id ?: "exp_${UUID.randomUUID().toString().take(8)}",
                groupId = groupId,
                title = title.trim().ifBlank { category.label },
                amountPaise = amountPaise,
                paidByMemberId = paidByMemberId,
                splitMode = splitMode,
                category = category,
                splits = splits,
                items = items,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                isSettlement = false,
            )
            repository.addOrUpdateExpense(expense)
            _activeSheet.value = ActiveSheet.None
            _toastEvents.tryEmit(if (existing != null) "Expense updated" else "Split saved")
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _activeSheet.value = ActiveSheet.None
            _toastEvents.tryEmit("Expense deleted")
        }
    }

    fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountPaise: Long,
        method: PaymentMethod,
    ) {
        viewModelScope.launch {
            val fromName = uiState.value.membersMap[fromMemberId]?.name ?: "Member"
            val toName = uiState.value.membersMap[toMemberId]?.name ?: "Member"
            val settlement = Expense(
                id = "stl_${UUID.randomUUID().toString().take(8)}",
                groupId = groupId,
                title = "Settled: $fromName → $toName",
                amountPaise = amountPaise,
                paidByMemberId = fromMemberId,
                splitMode = SplitMode.EXACT,
                category = ExpenseCategory.SETTLEMENT,
                splits = listOf(MemberSplit(memberId = toMemberId, amountPaise = amountPaise)),
                createdAt = System.currentTimeMillis(),
                isSettlement = true,
                settlementMethod = method,
            )
            repository.addOrUpdateExpense(settlement)
        }
    }

    fun createGroup(
        name: String,
        iconKey: String,
        memberIds: List<String>,
        newPeople: List<NewGroupPerson> = emptyList(),
    ) {
        viewModelScope.launch {
            val currentUserId = uiState.value.currentUser?.id ?: "alex"
            val newMembers = newPeople.mapNotNull { person ->
                val cleanName = person.name.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val initials = cleanName.split(Regex("\\s+")).take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("").ifBlank { "?" }
                Member(
                    id = "mem_${UUID.randomUUID().toString().take(8)}",
                    name = cleanName,
                    initials = initials,
                    tone = com.example.data.model.PastelTone.values()[uiState.value.members.size % com.example.data.model.PastelTone.values().size],
                    upiId = "",
                    contact = person.contact.trim(),
                )
            }
            repository.addMembers(newMembers)
            val fullMembers = (listOf(currentUserId) + memberIds + newMembers.map { it.id }).distinct()
            val group = Group(
                id = "grp_${UUID.randomUUID().toString().take(8)}",
                name = name.trim().ifBlank { "New Group" },
                iconKey = iconKey,
                memberIds = fullMembers,
                createdAt = System.currentTimeMillis(),
            )
            repository.createGroup(group)
            val cloudMembers = (uiState.value.members.filter { it.id in fullMembers } + newMembers)
                .distinctBy { it.id }
            val inviteCode = if (collaborationRepository.isSignedIn) {
                runCatching { collaborationRepository.createGroup(group, cloudMembers) }.getOrNull()
            } else null
            _activeSheet.value = ActiveSheet.None
            _stage.value = AppStage.MAIN
            _activeTab.value = MainTab.GROUPS
            _overlayScreen.value = OverlayScreen.GroupLedger(group.id)
            if (inviteCode != null) _groupInviteCodes.value = _groupInviteCodes.value + (group.id to inviteCode)
            _toastEvents.tryEmit(
                when {
                    inviteCode != null -> "Invite code for ${group.name}: $inviteCode"
                    collaborationRepository.isSignedIn -> "Created ${group.name}; group stayed on this phone because cloud setup failed."
                    else -> "Created ${group.name} on this phone. Sign in to share it."
                },
            )
        }
    }

    fun joinByInviteCode(code: String) {
        viewModelScope.launch {
            if (!collaborationRepository.isSignedIn) {
                _activeSheet.value = ActiveSheet.Account
                _accountMessage.value = "Sign in with a verified email before joining a shared group."
                return@launch
            }
            val upiId = uiState.value.currentUser?.upiId.orEmpty()
            runCatching { collaborationRepository.joinByInviteCode(code, upiId) }
                .onSuccess { snapshot ->
                    repository.applyCloudSnapshot(snapshot)
                    _activeSheet.value = ActiveSheet.None
                    _stage.value = AppStage.MAIN
                    _activeTab.value = MainTab.GROUPS
                    _overlayScreen.value = OverlayScreen.GroupLedger(snapshot.group.id)
                    _toastEvents.tryEmit("Joined ${snapshot.group.name}")
                }
                .onFailure { error -> _toastEvents.tryEmit(error.message ?: "Could not join this group.") }
        }
    }

    fun updateMyUpiId(rawUpiId: String) {
        viewModelScope.launch {
            val member = uiState.value.currentUser ?: run {
                _toastEvents.tryEmit("Could not find your profile.")
                return@launch
            }
            val upiId = rawUpiId.trim()
            if (upiId.isNotEmpty() && !upiId.matches(Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$"))) {
                _toastEvents.tryEmit("Enter a valid UPI ID, such as name@bank.")
                return@launch
            }
            val cloudResult = if (collaborationRepository.isSignedIn) {
                runCatching { collaborationRepository.updateMyUpiId(member.id, upiId) }
            } else null
            repository.updateMember(member.copy(upiId = upiId))
            when {
                cloudResult?.isFailure == true -> _toastEvents.tryEmit("UPI ID saved here, but group members could not be updated.")
                collaborationRepository.isSignedIn -> _toastEvents.tryEmit("UPI ID saved and shared with your groups.")
                else -> _toastEvents.tryEmit("UPI ID saved on this phone. Sign in to share it with groups.")
            }
        }
    }

    fun clearAccountMessage() {
        _accountMessage.value = null
    }

    fun createAccount(name: String, email: String, password: String) {
        viewModelScope.launch {
            _accountBusy.value = true
            _accountMessage.value = null
            val result = accountRepository.createAccount(name, email, password)
            _accountMessage.value = result.fold(
                onSuccess = { "Check your email and verify your address before signing in." },
                onFailure = { it.message ?: "Could not create account. Please try again." },
            )
            _accountEmail.value = accountRepository.verifiedEmail
            _accountBusy.value = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _accountBusy.value = true
            _accountMessage.value = null
            val result = accountRepository.signIn(email, password)
            _accountEmail.value = accountRepository.verifiedEmail
            if (result.isSuccess) startCloudSync()
            _accountMessage.value = result.fold(
                onSuccess = { _activeSheet.value = ActiveSheet.None; "Signed in as $it" },
                onFailure = { it.message ?: "Could not sign in. Please try again." },
            )
            _accountBusy.value = false
        }
    }

    private fun startCloudSync() {
        cloudGroupsListener?.remove()
        cloudGroupsListener = collaborationRepository.listenForMyGroups(
            onGroups = { snapshots ->
                _groupInviteCodes.value = snapshots.mapNotNull { snapshot ->
                    snapshot.inviteCode?.let { snapshot.group.id to it }
                }.toMap()
                viewModelScope.launch { snapshots.forEach { repository.applyCloudSnapshot(it) } }
            },
            onError = { error -> _toastEvents.tryEmit("Cloud sync error: ${error.message ?: "check connection"}") },
        )
    }

    override fun onCleared() {
        cloudGroupsListener?.remove()
        super.onCleared()
    }

    fun resetAccountPassword(email: String) {
        viewModelScope.launch {
            _accountBusy.value = true
            _accountMessage.value = null
            val result = accountRepository.sendPasswordReset(email)
            _accountMessage.value = result.fold(
                onSuccess = { "Password reset link sent if the account exists." },
                onFailure = { it.message ?: "Could not send reset link." },
            )
            _accountBusy.value = false
        }
    }

    fun signOut() {
        cloudGroupsListener?.remove()
        cloudGroupsListener = null
        accountRepository.signOut()
        _accountEmail.value = null
        _accountMessage.value = "Signed out. Your local bills are still on this device."
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setPaisePrecision(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setPaisePrecision(enabled) }
    }

    fun setCurrency(currency: CurrencyOption) {
        viewModelScope.launch { prefsRepository.setCurrency(currency) }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            _activeSheet.value = ActiveSheet.None
            _overlayScreen.value = OverlayScreen.None
            _toastEvents.tryEmit("Demo data restored")
        }
    }

    fun showToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    private fun overlayScreenGroupId(): String? = when (val ov = _overlayScreen.value) {
        is OverlayScreen.GroupLedger -> ov.groupId
        is OverlayScreen.SettleUp -> ov.groupId
        OverlayScreen.FullActivity,
        OverlayScreen.None,
        -> null
    }
}
