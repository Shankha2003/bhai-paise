package com.example.data.repository

import android.content.Context
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseItem
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.data.model.MemberSplit
import com.example.data.model.PastelTone
import com.example.data.model.PaymentMethod
import com.example.data.model.SplitMode
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseCollaborationRepository(context: Context) {
    private val auth = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }.getOrNull()
    private val firestore = if (auth == null) null else FirebaseFirestore.getInstance()

    val isConfigured: Boolean get() = firestore != null
    val isSignedIn: Boolean get() = auth?.currentUser?.let { it.isEmailVerified } == true
    private val uid: String? get() = auth?.currentUser?.takeIf { it.isEmailVerified }?.uid
    val currentUserUid: String? get() = uid

    suspend fun createGroup(group: Group, members: List<Member>): String? {
        val currentUid = uid ?: return null
        val db = firestore ?: return null
        val code = newInviteCode()
        val groupRef = db.collection("sharedGroups").document(group.id)
        val membersMap = members.associate { member ->
            member.id to mapOf(
                "name" to member.name,
                "initials" to member.initials,
                "tone" to member.tone.name,
                "contact" to member.contact,
                "upiId" to member.upiId,
                "userUid" to if (member.isCurrentUser) currentUid else "",
            )
        }
        val batch = db.batch()
        batch.set(groupRef, mapOf(
            "name" to group.name,
            "iconKey" to group.iconKey,
            "createdAt" to group.createdAt,
            "ownerUid" to currentUid,
            "memberUids" to listOf(currentUid),
            "memberIds" to group.memberIds,
            "members" to membersMap,
            "expenses" to emptyMap<String, Any>(),
            "inviteCode" to code,
            "updatedAt" to System.currentTimeMillis(),
        ))
        batch.set(db.collection("groupInvites").document(code), mapOf(
            "groupId" to group.id,
            "createdBy" to currentUid,
            "expiresAt" to Timestamp(Date(System.currentTimeMillis() + INVITE_LIFETIME_MS)),
            "joinedUids" to emptyList<String>(),
        ))
        batch.commit().awaitResult()
        return code
    }

    suspend fun joinByInviteCode(rawCode: String, upiId: String = ""): CloudGroupSnapshot {
        val currentUid = uid ?: error("Sign in with a verified email before joining a shared group.")
        val db = firestore ?: error("Firebase is not connected yet.")
        val code = rawCode.trim().uppercase().replace("-", "").replace(" ", "")
        require(code.length == INVITE_CODE_LENGTH) { "Enter the 8-character invite code." }
        val inviteRef = db.collection("groupInvites").document(code)
        val groupId = inviteRef.get().awaitResult().let { invite ->
            require(invite.exists()) { "That invite code was not found." }
            require((invite.getTimestamp("expiresAt")?.toDate()?.time ?: 0L) > System.currentTimeMillis()) { "That invite code has expired." }
            invite.getString("groupId") ?: error("Invite is missing its group.")
        }
        val groupRef = db.collection("sharedGroups").document(groupId)
        val user = auth?.currentUser ?: error("Sign in again to join this group.")
        val memberId = memberIdForUid(currentUid)
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Member"
        val member = mapOf(
            "name" to displayName,
            "initials" to displayName.split(Regex("\\s+")).take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "ME" },
            "tone" to PastelTone.SKY.name,
            "contact" to (user.email ?: ""),
            "upiId" to upiId,
            "userUid" to currentUid,
            "joinedAt" to System.currentTimeMillis(),
        )
        db.runTransaction { transaction ->
            val inviteSnap = transaction.get(inviteRef)
            check(inviteSnap.exists()) { "That invite code was not found." }
            check(inviteSnap.getString("groupId") == groupId) { "That invite no longer matches its group." }
            check((inviteSnap.getTimestamp("expiresAt")?.toDate()?.time ?: 0L) > System.currentTimeMillis()) { "That invite code has expired." }
            val joined = (inviteSnap.get("joinedUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (currentUid !in joined) {
                transaction.update(groupRef, mapOf(
                    "memberUids" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUid),
                    "memberIds" to com.google.firebase.firestore.FieldValue.arrayUnion(memberId),
                    "members.$memberId" to member,
                    "updatedAt" to System.currentTimeMillis(),
                ))
                transaction.update(inviteRef, "joinedUids", joined + currentUid)
            }
            groupId
        }.awaitResult()
        return groupRef.get().awaitResult().toCloudSnapshot(currentUid)
    }

    suspend fun saveExpense(expense: Expense) {
        if (uid == null) return
        val ref = firestore?.collection("sharedGroups")?.document(expense.groupId) ?: return
        ref.update(mapOf(
            "expenses.${expense.id}" to expense.toCloudMap(),
            "updatedAt" to System.currentTimeMillis(),
        )).awaitResult()
    }

    suspend fun deleteExpense(groupId: String, expenseId: String) {
        if (uid == null) return
        val ref = firestore?.collection("sharedGroups")?.document(groupId) ?: return
        ref.update(mapOf(
            "expenses.$expenseId" to com.google.firebase.firestore.FieldValue.delete(),
            "updatedAt" to System.currentTimeMillis(),
        )).awaitResult()
    }

    fun listenForMyGroups(onGroups: (List<CloudGroupSnapshot>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration? {
        val currentUid = uid ?: return null
        return firestore?.collection("sharedGroups")?.whereArrayContains("memberUids", currentUid)
            ?.addSnapshotListener { snapshots, error ->
                if (error != null) onError(error)
                else onGroups(snapshots?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toCloudSnapshot(currentUid) }.getOrNull()
                })
            }
    }

    suspend fun registerPushToken() {
        val currentUid = uid ?: return
        val db = firestore ?: return
        val token = FirebaseMessaging.getInstance().token.awaitResult()
        db.collection("userDevices").document(currentUid).collection("tokens").document(token)
            .set(mapOf("updatedAt" to System.currentTimeMillis())).awaitResult()
    }

    suspend fun unregisterPushToken() {
        val currentUid = uid ?: return
        val db = firestore ?: return
        val token = FirebaseMessaging.getInstance().token.awaitResult()
        db.collection("userDevices").document(currentUid).collection("tokens").document(token)
            .delete().awaitResult()
    }

    fun inviteCodeFor(groupId: String, onResult: (String?) -> Unit) {
        firestore?.collection("sharedGroups")?.document(groupId)?.get()
            ?.addOnSuccessListener { onResult(it.getString("inviteCode")) }
            ?.addOnFailureListener { onResult(null) }
    }

    suspend fun updateMyUpiId(memberId: String, upiId: String) {
        val currentUid = uid ?: return
        val db = firestore ?: error("Firebase is not connected yet.")
        val groups = db.collection("sharedGroups")
            .whereArrayContains("memberUids", currentUid)
            .get()
            .awaitResult()
        groups.documents.forEach { group ->
            val memberMap = group.get("members") as? Map<*, *> ?: return@forEach
            val matchingId = memberMap.entries.firstOrNull { (_, value) ->
                (value as? Map<*, *>)?.get("userUid") == currentUid
            }?.key as? String ?: memberId.takeIf { memberMap.containsKey(it) } ?: return@forEach
            group.reference.update(mapOf(
                "members.$matchingId.upiId" to upiId,
                "updatedAt" to System.currentTimeMillis(),
            )).awaitResult()
        }
    }

    private fun newInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(INVITE_CODE_LENGTH) {
            repeat(INVITE_CODE_LENGTH) { append(chars[SecureRandom().nextInt(chars.length)]) }
        }
    }

    private fun memberIdForUid(uid: String) = "user_${uid.take(16)}"

    private fun Expense.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "groupId" to groupId,
        "title" to title,
        "amountPaise" to amountPaise,
        "paidByMemberId" to paidByMemberId,
        "splitMode" to splitMode.name,
        "category" to category.name,
        "splits" to splits.map { mapOf("memberId" to it.memberId, "amountPaise" to it.amountPaise, "shareUnits" to it.shareUnits, "percentage" to it.percentage) },
        "items" to items.map { mapOf("id" to it.id, "name" to it.name, "amountPaise" to it.amountPaise, "assignedMemberIds" to it.assignedMemberIds) },
        "createdAt" to createdAt,
        "isSettlement" to isSettlement,
        "settlementMethod" to settlementMethod?.name,
    )

    private fun DocumentSnapshot.toCloudSnapshot(currentUid: String): CloudGroupSnapshot {
        val data = data ?: error("Shared group is empty.")
        val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        val memberData = data["members"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val members = memberData.mapNotNull { (key, value) ->
            val id = key as? String ?: return@mapNotNull null
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            val userUid = fields["userUid"] as? String
            Member(
                id = id,
                name = fields["name"] as? String ?: "Member",
                initials = fields["initials"] as? String ?: "ME",
                tone = runCatching { PastelTone.valueOf(fields["tone"] as? String ?: "LAVENDER") }.getOrDefault(PastelTone.LAVENDER),
                upiId = fields["upiId"] as? String ?: "",
                isCurrentUser = userUid == currentUid || id == "user_${currentUid.take(16)}",
                contact = fields["contact"] as? String ?: "",
            )
        }
        val joinEvents = memberData.mapNotNull { (key, value) ->
            val memberId = key as? String ?: return@mapNotNull null
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            val userUid = fields["userUid"] as? String ?: return@mapNotNull null
            val joinedAt = (fields["joinedAt"] as? Number)?.toLong()?.takeIf { it > 0L } ?: return@mapNotNull null
            CloudGroupJoinEvent(
                id = "${memberId}_$joinedAt",
                userUid = userUid,
                memberName = fields["name"] as? String ?: "A member",
                joinedAt = joinedAt,
            )
        }.sortedBy { it.joinedAt }
        val expenseData = data["expenses"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val expenses = expenseData.mapNotNull { (_, value) ->
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            val id = fields["id"] as? String ?: return@mapNotNull null
            val splits = (fields["splits"] as? List<*>)?.mapNotNull { item ->
                val split = item as? Map<*, *> ?: return@mapNotNull null
                val splitMember = split["memberId"] as? String ?: return@mapNotNull null
                MemberSplit(splitMember, (split["amountPaise"] as? Number)?.toLong() ?: 0L,
                    (split["shareUnits"] as? Number)?.toInt() ?: 1, (split["percentage"] as? Number)?.toDouble() ?: 0.0)
            }.orEmpty()
            val items = (fields["items"] as? List<*>)?.mapNotNull { item ->
                val row = item as? Map<*, *> ?: return@mapNotNull null
                val itemId = row["id"] as? String ?: return@mapNotNull null
                ExpenseItem(itemId, row["name"] as? String ?: "Item", (row["amountPaise"] as? Number)?.toLong() ?: 0L,
                    (row["assignedMemberIds"] as? List<*>)?.filterIsInstance<String>().orEmpty())
            }.orEmpty()
            Expense(
                id = id,
                groupId = fields["groupId"] as? String ?: this.id,
                title = fields["title"] as? String ?: "Expense",
                amountPaise = (fields["amountPaise"] as? Number)?.toLong() ?: 0L,
                paidByMemberId = fields["paidByMemberId"] as? String ?: "",
                splitMode = runCatching { SplitMode.valueOf(fields["splitMode"] as? String ?: "EQUAL") }.getOrDefault(SplitMode.EQUAL),
                category = runCatching { ExpenseCategory.valueOf(fields["category"] as? String ?: "DINING") }.getOrDefault(ExpenseCategory.DINING),
                splits = splits,
                items = items,
                createdAt = (fields["createdAt"] as? Number)?.toLong() ?: 0L,
                isSettlement = fields["isSettlement"] as? Boolean ?: false,
                settlementMethod = (fields["settlementMethod"] as? String)?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
            )
        }
        return CloudGroupSnapshot(
            group = Group(id = id, name = data["name"] as? String ?: "Group", iconKey = data["iconKey"] as? String ?: "travel",
                memberIds = memberIds, createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L),
            members = members,
            expenses = expenses,
            inviteCode = data["inviteCode"] as? String,
            joinEvents = joinEvents,
        )
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(task.result as T)
                } else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud request failed."))
            }
        }

    companion object {
        private const val INVITE_CODE_LENGTH = 8
        private const val INVITE_LIFETIME_MS = 7L * 24 * 60 * 60 * 1000
    }
}

data class CloudGroupSnapshot(
    val group: Group,
    val members: List<Member>,
    val expenses: List<Expense>,
    val inviteCode: String?,
    val joinEvents: List<CloudGroupJoinEvent> = emptyList(),
)

data class CloudGroupJoinEvent(
    val id: String,
    val userUid: String,
    val memberName: String,
    val joinedAt: Long,
)
