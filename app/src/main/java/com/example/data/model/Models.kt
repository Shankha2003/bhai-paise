package com.example.data.model

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val label: String,
) {
    INR("INR", "₹", "Indian Rupee (INR ₹)"),
    USD("USD", "$", "US Dollar (USD $)"),
    EUR("EUR", "€", "Euro (EUR €)"),
}

enum class SplitMode(val label: String) {
    EQUAL("Equal"),
    EXACT("Exact"),
    PERCENTAGE("%"),
    SHARES("Shares"),
    ITEMIZED("Itemized"),
}

enum class ExpenseCategory(val label: String) {
    COFFEE("Coffee"),
    DINING("Dining"),
    HOME("Home"),
    TRAVEL("Travel"),
    GROCERIES("Groceries"),
    UTILITIES("Utilities"),
    SETTLEMENT("Settlement"),
}

enum class PaymentMethod(val label: String) {
    UPI("UPI App"),
    CASH("Cash"),
    MARK_PAID("Mark Paid"),
}

enum class PastelTone {
    LAVENDER,
    MINT,
    CORAL,
    SKY,
    AMBER,
}

data class Member(
    val id: String,
    val name: String,
    val initials: String,
    val tone: PastelTone,
    val upiId: String,
    val isCurrentUser: Boolean = false,
    val contact: String = "",
)

data class NewGroupPerson(val name: String, val contact: String)

data class Group(
    val id: String,
    val name: String,
    val iconKey: String,
    val memberIds: List<String>,
    val createdAt: Long,
)

data class MemberSplit(
    val memberId: String,
    val amountPaise: Long,
    val shareUnits: Int = 1,
    val percentage: Double = 0.0,
)

data class ExpenseItem(
    val id: String,
    val name: String,
    val amountPaise: Long,
    val assignedMemberIds: List<String>,
)

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amountPaise: Long,
    val paidByMemberId: String,
    val splitMode: SplitMode,
    val category: ExpenseCategory,
    val splits: List<MemberSplit>,
    val items: List<ExpenseItem> = emptyList(),
    val createdAt: Long,
    val isSettlement: Boolean = false,
    val settlementMethod: PaymentMethod? = null,
)

data class SimplifiedTransfer(
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
)

data class PendingPayment(
    val groupId: String,
    val groupName: String,
    val transfer: SimplifiedTransfer,
)

data class GroupSummary(
    val group: Group,
    val members: List<Member>,
    val totalSpendPaise: Long,
    val totalReceivedPaise: Long,
    val currentUserNetPaise: Long,
    val simplifiedTransfers: List<SimplifiedTransfer>,
    val expenseCount: Int,
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val paisePrecision: Boolean = true,
    val currency: CurrencyOption = CurrencyOption.INR,
    val hasCompletedOnboarding: Boolean = false,
)
