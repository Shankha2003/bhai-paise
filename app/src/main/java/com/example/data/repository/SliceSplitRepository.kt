package com.example.data.repository

import com.example.data.local.EntityMappers.toDomain
import com.example.data.local.EntityMappers.toEntity
import com.example.data.local.SeedData
import com.example.data.local.SliceSplitDao
import com.example.data.model.Expense
import com.example.data.model.Group
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SliceSplitRepository(private val dao: SliceSplitDao) {

    val members: Flow<List<Member>> = dao.observeMembers().map { list ->
        list.map { it.toDomain() }
    }

    val groups: Flow<List<Group>> = dao.observeGroups().map { list ->
        list.map { it.toDomain() }
    }

    val expenses: Flow<List<Expense>> = dao.observeExpenses().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun ensureSeeded() {
        // A new install starts with the current user only. Demo groups and bills are
        // available through the explicit Reset Demo action in Profile.
        if (dao.memberCount() == 0) {
            dao.insertMembers(listOf(SeedData.currentUser().toEntity()))
        }
    }

    suspend fun addMembers(members: List<Member>) {
        if (members.isNotEmpty()) dao.insertMembers(members.map { it.toEntity() })
    }

    suspend fun updateMember(member: Member) {
        dao.insertMembers(listOf(member.toEntity()))
    }

    suspend fun seedDemoData() {
        val now = System.currentTimeMillis()
        dao.clearAllExpenses()
        dao.clearAllGroups()
        dao.clearAllMembers()
        dao.insertMembers(SeedData.members().map { it.toEntity() })
        dao.insertGroups(SeedData.groups(now).map { it.toEntity() })
        dao.insertExpenses(SeedData.expenses(now).map { it.toEntity() })
    }

    suspend fun addOrUpdateExpense(expense: Expense) {
        dao.insertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpenseById(expenseId)
    }

    suspend fun createGroup(group: Group) {
        dao.insertGroup(group.toEntity())
    }

    suspend fun applyCloudSnapshot(snapshot: CloudGroupSnapshot) {
        dao.insertMembers(snapshot.members.map { it.toEntity() })
        dao.insertGroup(snapshot.group.toEntity())
        dao.insertExpenses(snapshot.expenses.map { it.toEntity() })
    }
}
