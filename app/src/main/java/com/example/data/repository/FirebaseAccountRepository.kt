package com.example.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAccountRepository(context: Context) {
    private val auth: FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }.getOrNull()

    val isConfigured: Boolean get() = auth != null
    val verifiedEmail: String? get() = auth?.currentUser?.takeIf { it.isEmailVerified }?.email

    suspend fun createAccount(name: String, email: String, password: String): Result<Unit> = runCatching {
        val firebaseAuth = requireAuth()
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
        val user = result.user ?: error("Account creation did not return a user.")
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()).awaitResult()
        }
        user.sendEmailVerification().awaitResult()
        firebaseAuth.signOut()
    }

    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val firebaseAuth = requireAuth()
        val user = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).awaitResult().user
            ?: error("Sign in did not return a user.")
        if (!user.isEmailVerified) {
            runCatching { user.sendEmailVerification().awaitResult() }
            firebaseAuth.signOut()
            error("Verify your email first. We sent another verification link.")
        }
        user.email ?: email.trim()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        requireAuth().sendPasswordResetEmail(email.trim()).awaitResult()
    }

    fun signOut() {
        auth?.signOut()
    }

    private fun requireAuth(): FirebaseAuth = auth
        ?: error("Firebase is not connected. Add app/google-services.json from your Firebase project, then sync Gradle.")

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(task.result as T)
                } else {
                    continuation.resumeWithException(task.exception ?: IllegalStateException("Request failed."))
                }
            }
        }
}
