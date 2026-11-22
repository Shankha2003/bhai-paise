package com.example

import com.example.data.local.SeedData
import com.example.data.math.SplitMath
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun seedDataProducesExpectedNetBalances() {
        val groups = SeedData.groups(1_000_000L)
        val expenses = SeedData.expenses(1_000_000L)
        var owed = 0L
        var youOwe = 0L
        for (group in groups) {
            val groupExpenses = expenses.filter { it.groupId == group.id }
            val nets = SplitMath.computeMemberNets(group.memberIds, groupExpenses)
            val alexNet = nets["alex"] ?: 0L
            if (alexNet > 0L) owed += alexNet else youOwe += -alexNet
        }
        assertEquals(310_000L, owed) // ₹3,100
        assertEquals(62_000L, youOwe) // ₹620
        assertEquals(248_000L, owed - youOwe) // +₹2,480
    }

    @Test
    fun equalSplitDistributesRemainderPaiseCleanly() {
        val splits = SplitMath.equalSplit(100_00L, listOf("a", "b", "c"))
        assertEquals(100_00L, splits.sumOf { it.amountPaise })
        assertEquals(3334L, splits[0].amountPaise)
        assertEquals(3333L, splits[1].amountPaise)
        assertEquals(3333L, splits[2].amountPaise)
    }

    @Test
    fun itemizedSplitAssignsItemsPerMember() {
        val items = listOf(
            ExpenseItem("1", "Burger", 40_000L, listOf("alex", "rohan")),
            ExpenseItem("2", "Coffee", 24_000L, listOf("meera")),
        )
        val splits = SplitMath.itemizedSplit(listOf("alex", "rohan", "meera"), items)
        assertEquals(20_000L, splits.first { it.memberId == "alex" }.amountPaise)
        assertEquals(20_000L, splits.first { it.memberId == "rohan" }.amountPaise)
        assertEquals(24_000L, splits.first { it.memberId == "meera" }.amountPaise)
    }

    @Test
    fun simplifyDebtsMatchesGoaWeekendRohanToAlex() {
        val groups = SeedData.groups(1_000_000L)
        val goa = groups.first { it.id == "goa_weekend" }
        val goaExpenses = SeedData.expenses(1_000_000L).filter { it.groupId == "goa_weekend" }
        val transfers = SplitMath.simplifyDebts(goa.memberIds, goaExpenses)
        assertTrue(transfers.any { it.fromMemberId == "rohan" && it.toMemberId == "alex" && it.amountPaise == 124_000L })
    }

    @Test
    fun formatIndianCurrencyGrouping() {
        assertEquals("+₹2,480", SplitMath.formatMoney(248_000L, CurrencyOption.INR, paisePrecision = true, showPlusSign = true))
        assertEquals("₹1,24,050.50", SplitMath.formatMoney(12_405_050L, CurrencyOption.INR, paisePrecision = true))
    }
}
