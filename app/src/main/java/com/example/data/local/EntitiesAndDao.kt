package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.math.SplitMath
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseItem
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.data.model.MemberSplit
import com.example.data.model.PastelTone
import com.example.data.model.PaymentMethod
import com.example.data.model.SplitMode
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val initials: String,
    val tone: String,
    val upiId: String,
    val isCurrentUser: Boolean,
    val contact: String = "",
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val memberIdsCsv: String,
    val createdAt: Long,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amountPaise: Long,
    val paidByMemberId: String,
    val splitMode: String,
    val category: String,
    val splitsEncoded: String,
    val itemsEncoded: String,
    val createdAt: Long,
    val isSettlement: Boolean,
    val settlementMethod: String?,
)

@Dao
interface SliceSplitDao {
    @Query("SELECT * FROM members ORDER BY isCurrentUser DESC, name ASC")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM groups ORDER BY createdAt ASC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun groupCount(): Int

    @Query("SELECT COUNT(*) FROM members")
    suspend fun memberCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM groups")
    suspend fun clearAllGroups()

    @Query("DELETE FROM members")
    suspend fun clearAllMembers()
}

@Database(
    entities = [MemberEntity::class, GroupEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SliceSplitDatabase : RoomDatabase() {
    abstract fun sliceSplitDao(): SliceSplitDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE members ADD COLUMN contact TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: SliceSplitDatabase? = null

        fun getInstance(context: Context): SliceSplitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SliceSplitDatabase::class.java,
                    "slicesplit.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}

object EntityMappers {
    fun MemberEntity.toDomain(): Member = Member(
        id = id,
        name = name,
        initials = initials,
        tone = runCatching { PastelTone.valueOf(tone) }.getOrDefault(PastelTone.LAVENDER),
        upiId = upiId,
        isCurrentUser = isCurrentUser,
        contact = contact,
    )

    fun Member.toEntity(): MemberEntity = MemberEntity(
        id = id,
        name = name,
        initials = initials,
        tone = tone.name,
        upiId = upiId,
        isCurrentUser = isCurrentUser,
        contact = contact,
    )

    fun GroupEntity.toDomain(): Group = Group(
        id = id,
        name = name,
        iconKey = iconKey,
        memberIds = memberIdsCsv.split(",").filter { it.isNotBlank() },
        createdAt = createdAt,
    )

    fun Group.toEntity(): GroupEntity = GroupEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        memberIdsCsv = memberIds.joinToString(","),
        createdAt = createdAt,
    )

    fun ExpenseEntity.toDomain(): Expense = Expense(
        id = id,
        groupId = groupId,
        title = title,
        amountPaise = amountPaise,
        paidByMemberId = paidByMemberId,
        splitMode = runCatching { SplitMode.valueOf(splitMode) }.getOrDefault(SplitMode.EQUAL),
        category = runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.DINING),
        splits = decodeSplits(splitsEncoded),
        items = decodeItems(itemsEncoded),
        createdAt = createdAt,
        isSettlement = isSettlement,
        settlementMethod = settlementMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
    )

    fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        title = title,
        amountPaise = amountPaise,
        paidByMemberId = paidByMemberId,
        splitMode = splitMode.name,
        category = category.name,
        splitsEncoded = encodeSplits(splits),
        itemsEncoded = encodeItems(items),
        createdAt = createdAt,
        isSettlement = isSettlement,
        settlementMethod = settlementMethod?.name,
    )

    private fun encodeSplits(splits: List<MemberSplit>): String =
        splits.joinToString(";") { "${it.memberId}:${it.amountPaise}:${it.shareUnits}:${it.percentage}" }

    private fun decodeSplits(encoded: String): List<MemberSplit> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { part ->
            val fields = part.split(":")
            if (fields.size < 2) return@mapNotNull null
            MemberSplit(
                memberId = fields[0],
                amountPaise = fields[1].toLongOrNull() ?: 0L,
                shareUnits = fields.getOrNull(2)?.toIntOrNull() ?: 1,
                percentage = fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
            )
        }
    }

    private fun encodeItems(items: List<ExpenseItem>): String =
        items.joinToString("||") { item ->
            val safeName = item.name.replace("|", " ").replace("~", " ")
            "${item.id}~$safeName~${item.amountPaise}~${item.assignedMemberIds.joinToString(",")}"
        }

    private fun decodeItems(encoded: String): List<ExpenseItem> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("||").mapNotNull { part ->
            val fields = part.split("~")
            if (fields.size < 4) return@mapNotNull null
            ExpenseItem(
                id = fields[0],
                name = fields[1],
                amountPaise = fields[2].toLongOrNull() ?: 0L,
                assignedMemberIds = fields[3].split(",").filter { it.isNotBlank() },
            )
        }
    }
}

object SeedData {
    fun currentUser(): Member = Member(
        id = "alex",
        name = "You",
        initials = "YO",
        tone = PastelTone.LAVENDER,
        upiId = "",
        isCurrentUser = true,
    )

    fun members(): List<Member> = listOf(
        Member(
            id = "alex",
            name = "Alex",
            initials = "AS",
            tone = PastelTone.LAVENDER,
            upiId = "alex.s@okaxis",
            isCurrentUser = true,
        ),
        Member(
            id = "rohan",
            name = "Rohan",
            initials = "RK",
            tone = PastelTone.MINT,
            upiId = "rohan.k@oksbi",
        ),
        Member(
            id = "meera",
            name = "Meera",
            initials = "MN",
            tone = PastelTone.CORAL,
            upiId = "meera.n@okicici",
        ),
        Member(
            id = "kabir",
            name = "Kabir",
            initials = "KV",
            tone = PastelTone.SKY,
            upiId = "kabir.v@okhdfcbank",
        ),
        Member(
            id = "ananya",
            name = "Ananya",
            initials = "AP",
            tone = PastelTone.AMBER,
            upiId = "ananya.p@ybl",
        ),
    )

    fun groups(now: Long = System.currentTimeMillis()): List<Group> = listOf(
        Group(
            id = "hsr_flatmates",
            name = "HSR Flatmates",
            iconKey = "home",
            memberIds = listOf("alex", "rohan", "meera", "kabir"),
            createdAt = now - 20 * 86_400_000L,
        ),
        Group(
            id = "goa_weekend",
            name = "Goa Weekend",
            iconKey = "travel",
            memberIds = listOf("alex", "rohan", "ananya", "kabir"),
            createdAt = now - 10 * 86_400_000L,
        ),
        Group(
            id = "office_lunch",
            name = "Office Lunch",
            iconKey = "dining",
            memberIds = listOf("alex", "meera", "ananya"),
            createdAt = now - 5 * 86_400_000L,
        ),
        Group(
            id = "weekend_trek",
            name = "Weekend Trek",
            iconKey = "trek",
            memberIds = listOf("alex", "kabir"),
            createdAt = now - 1 * 86_400_000L,
        ),
    )

    fun expenses(now: Long = System.currentTimeMillis()): List<Expense> = listOf(
        Expense(
            id = "exp_coffee",
            groupId = "office_lunch",
            title = "Third Wave Coffee",
            amountPaise = 72000L,
            paidByMemberId = "alex",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.COFFEE,
            splits = SplitMath.equalSplit(72000L, listOf("alex", "meera", "ananya")),
            createdAt = now - 2 * 3_600_000L,
        ),
        Expense(
            id = "exp_truffles",
            groupId = "office_lunch",
            title = "Truffles Koramangala",
            amountPaise = 330000L,
            paidByMemberId = "meera",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.DINING,
            splits = SplitMath.equalSplit(330000L, listOf("alex", "meera", "ananya")),
            createdAt = now - 18 * 3_600_000L,
        ),
        Expense(
            id = "exp_goa_shack",
            groupId = "goa_weekend",
            title = "Beach Shack Dinner",
            amountPaise = 248000L,
            paidByMemberId = "alex",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.DINING,
            splits = SplitMath.equalSplit(248000L, listOf("alex", "rohan")),
            createdAt = now - 2 * 86_400_000L,
        ),
        Expense(
            id = "exp_goa_scooters",
            groupId = "goa_weekend",
            title = "Scooter Rentals & Fuel",
            amountPaise = 180000L,
            paidByMemberId = "ananya",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.TRAVEL,
            splits = SplitMath.equalSplit(180000L, listOf("ananya", "kabir")),
            createdAt = now - 3 * 86_400_000L,
        ),
        Expense(
            id = "exp_hsr_wifi",
            groupId = "hsr_flatmates",
            title = "ACT Fibernet & Power",
            amountPaise = 128000L,
            paidByMemberId = "alex",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.UTILITIES,
            splits = SplitMath.equalSplit(128000L, listOf("alex", "rohan", "meera", "kabir")),
            createdAt = now - 4 * 86_400_000L,
        ),
        Expense(
            id = "exp_hsr_groceries",
            groupId = "hsr_flatmates",
            title = "BigBasket Groceries",
            amountPaise = 120000L,
            paidByMemberId = "alex",
            splitMode = SplitMode.EQUAL,
            category = ExpenseCategory.GROCERIES,
            splits = SplitMath.equalSplit(120000L, listOf("alex", "rohan", "meera", "kabir")),
            createdAt = now - 6 * 86_400_000L,
        ),
    )
}
