package com.example.data.math

import com.example.data.model.CurrencyOption
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.data.model.MemberSplit
import com.example.data.model.SimplifiedTransfer
import kotlin.math.abs
import kotlin.math.roundToLong

object SplitMath {

    fun equalSplit(totalPaise: Long, participantIds: List<String>): List<MemberSplit> {
        if (participantIds.isEmpty() || totalPaise <= 0L) {
            return participantIds.map { MemberSplit(it, 0L) }
        }
        val count = participantIds.size.toLong()
        val base = totalPaise / count
        var remainder = (totalPaise % count).toInt()
        return participantIds.map { id ->
            val extra = if (remainder > 0) {
                remainder--
                1L
            } else {
                0L
            }
            MemberSplit(memberId = id, amountPaise = base + extra)
        }
    }

    fun percentageSplit(totalPaise: Long, percentages: Map<String, Double>): List<MemberSplit> {
        if (percentages.isEmpty()) return emptyList()
        val entries = percentages.entries.toList()
        val raw = entries.map { (id, pct) ->
            id to ((totalPaise * pct) / 100.0).roundToLong()
        }.toMutableList()
        val allocated = raw.sumOf { it.second }
        val diff = totalPaise - allocated
        if (diff != 0L && raw.isNotEmpty()) {
            val idx = raw.indexOfFirst { (percentages[it.first] ?: 0.0) > 0.0 }.takeIf { it >= 0 } ?: 0
            raw[idx] = raw[idx].first to (raw[idx].second + diff).coerceAtLeast(0L)
        }
        return raw.map { (id, amt) ->
            MemberSplit(
                memberId = id,
                amountPaise = amt,
                percentage = percentages[id] ?: 0.0,
            )
        }
    }

    fun sharesSplit(totalPaise: Long, sharesMap: Map<String, Int>): List<MemberSplit> {
        val active = sharesMap.filterValues { it > 0 }
        if (active.isEmpty() || totalPaise <= 0L) {
            return sharesMap.map { (id, units) -> MemberSplit(id, 0L, shareUnits = units) }
        }
        val totalShares = active.values.sum().toLong()
        val result = mutableMapOf<String, Long>()
        var allocated = 0L
        active.entries.forEachIndexed { index, (id, units) ->
            val amount = if (index == active.size - 1) {
                totalPaise - allocated
            } else {
                (totalPaise * units) / totalShares
            }
            allocated += amount
            result[id] = amount
        }
        return sharesMap.map { (id, units) ->
            MemberSplit(
                memberId = id,
                amountPaise = result[id] ?: 0L,
                shareUnits = units,
            )
        }
    }

    fun itemizedSplit(participantIds: List<String>, items: List<ExpenseItem>): List<MemberSplit> {
        val totals = participantIds.associateWith { 0L }.toMutableMap()
        for (item in items) {
            val assignees = item.assignedMemberIds.filter { it in totals }
            if (assignees.isNotEmpty() && item.amountPaise > 0L) {
                val itemSplits = equalSplit(item.amountPaise, assignees)
                for (split in itemSplits) {
                    totals[split.memberId] = (totals[split.memberId] ?: 0L) + split.amountPaise
                }
            }
        }
        return participantIds.map { id ->
            MemberSplit(memberId = id, amountPaise = totals[id] ?: 0L)
        }
    }

    /**
     * Calculates net balance in paise for every member in a group:
     * positive => member is owed money
     * negative => member owes money
     */
    fun computeMemberNets(
        memberIds: List<String>,
        expenses: List<Expense>,
    ): Map<String, Long> {
        val nets = memberIds.associateWith { 0L }.toMutableMap()
        for (expense in expenses) {
            nets[expense.paidByMemberId] = (nets[expense.paidByMemberId] ?: 0L) + expense.amountPaise
            for (split in expense.splits) {
                nets[split.memberId] = (nets[split.memberId] ?: 0L) - split.amountPaise
            }
        }
        return nets
    }

    /**
     * Computes minimum set of transfers to settle all debts in a group.
     */
    fun simplifyDebts(
        memberIds: List<String>,
        expenses: List<Expense>,
    ): List<SimplifiedTransfer> {
        val nets = computeMemberNets(memberIds, expenses)
        val creditors = nets.filterValues { it > 0L }
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
            .toMutableList()
        val debtors = nets.filterValues { it < 0L }
            .entries
            .sortedBy { it.value } // most negative first
            .map { it.key to -it.value }
            .toMutableList()

        val transfers = mutableListOf<SimplifiedTransfer>()
        var cIdx = 0
        var dIdx = 0
        while (cIdx < creditors.size && dIdx < debtors.size) {
            val (creditorId, creditAmt) = creditors[cIdx]
            val (debtorId, debtAmt) = debtors[dIdx]
            val settleAmt = minOf(creditAmt, debtAmt)
            if (settleAmt > 0L) {
                transfers += SimplifiedTransfer(
                    fromMemberId = debtorId,
                    toMemberId = creditorId,
                    amountPaise = settleAmt,
                )
            }
            val remCredit = creditAmt - settleAmt
            val remDebt = debtAmt - settleAmt
            if (remCredit <= 0L) cIdx++ else creditors[cIdx] = creditorId to remCredit
            if (remDebt <= 0L) dIdx++ else debtors[dIdx] = debtorId to remDebt
        }
        return transfers
    }

    fun formatMoney(
        paise: Long,
        currency: CurrencyOption = CurrencyOption.INR,
        paisePrecision: Boolean = true,
        showPlusSign: Boolean = false,
    ): String {
        val sign = when {
            paise < 0L -> "-"
            paise > 0L && showPlusSign -> "+"
            else -> ""
        }
        val absPaise = abs(paise)
        val effectivePaise = if (paisePrecision) {
            absPaise
        } else {
            ((absPaise + 50L) / 100L) * 100L
        }
        val whole = effectivePaise / 100L
        val frac = (effectivePaise % 100L).toInt()

        val formattedWhole = if (currency == CurrencyOption.INR) {
            formatIndianGrouping(whole)
        } else {
            formatWesternGrouping(whole)
        }

        val decimalPart = if (paisePrecision && frac != 0) {
            ".${frac.toString().padStart(2, '0')}"
        } else {
            ""
        }
        return "$sign${currency.symbol}$formattedWhole$decimalPart"
    }

    fun formatMoneyAlwaysDecimals(
        paise: Long,
        currency: CurrencyOption = CurrencyOption.INR,
        paisePrecision: Boolean = true,
    ): String {
        val sign = if (paise < 0L) "-" else ""
        val absPaise = abs(paise)
        if (!paisePrecision) {
            val roundedWhole = (absPaise + 50L) / 100L
            val wholeStr = if (currency == CurrencyOption.INR) {
                formatIndianGrouping(roundedWhole)
            } else {
                formatWesternGrouping(roundedWhole)
            }
            return "$sign${currency.symbol}$wholeStr"
        }
        val whole = absPaise / 100L
        val frac = (absPaise % 100L).toInt()
        val wholeStr = if (currency == CurrencyOption.INR) {
            formatIndianGrouping(whole)
        } else {
            formatWesternGrouping(whole)
        }
        return "$sign${currency.symbol}$wholeStr.${frac.toString().padStart(2, '0')}"
    }

    private fun formatIndianGrouping(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val lastThree = raw.takeLast(3)
        val rest = raw.dropLast(3)
        val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRest,$lastThree"
    }

    private fun formatWesternGrouping(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        return raw.reversed().chunked(3).joinToString(",").reversed()
    }

    fun formatRelativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diff = (nowMs - timestampMs).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        return when {
            minutes < 2 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }

    fun exportAsJson(
        groups: List<Group>,
        expenses: List<Expense>,
        members: Map<String, Member>,
        currency: CurrencyOption,
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"Bhai Paise\",\n")
        sb.append("  \"currency\": \"${currency.code}\",\n")
        sb.append("  \"groups\": [\n")
        groups.forEachIndexed { gi, g ->
            sb.append("    {\"id\": \"${escapeJson(g.id)}\", \"name\": \"${escapeJson(g.name)}\", \"members\": [${g.memberIds.joinToString { "\"${escapeJson(members[it]?.name ?: it)}\"" }}]}")
            if (gi < groups.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"expenses\": [\n")
        expenses.forEachIndexed { ei, e ->
            val payerName = members[e.paidByMemberId]?.name ?: e.paidByMemberId
            sb.append("    {\"id\": \"${escapeJson(e.id)}\", \"groupId\": \"${escapeJson(e.groupId)}\", \"title\": \"${escapeJson(e.title)}\", \"amount\": ${e.amountPaise / 100.0}, \"paidBy\": \"${escapeJson(payerName)}\", \"mode\": \"${e.splitMode.name}\", \"isSettlement\": ${e.isSettlement}}")
            if (ei < expenses.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun exportAsCsv(
        groups: List<Group>,
        expenses: List<Expense>,
        members: Map<String, Member>,
        currency: CurrencyOption,
    ): String {
        val groupNames = groups.associate { it.id to it.name }
        val sb = StringBuilder()
        sb.append("id,group,title,amount_${currency.code},paid_by,split_mode,type\n")
        for (e in expenses) {
            val gName = escapeCsv(groupNames[e.groupId] ?: e.groupId)
            val title = escapeCsv(e.title)
            val payer = escapeCsv(members[e.paidByMemberId]?.name ?: e.paidByMemberId)
            val amount = "%.2f".format(e.amountPaise / 100.0)
            val type = if (e.isSettlement) "settlement" else "expense"
            sb.append("${escapeCsv(e.id)},$gName,$title,$amount,$payer,${e.splitMode.name},$type\n")
        }
        return sb.toString()
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun escapeCsv(s: String): String {
        val clean = s.replace("\"", "\"\"")
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) "\"$clean\"" else clean
    }
}
