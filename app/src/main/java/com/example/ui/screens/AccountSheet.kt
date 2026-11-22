package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SliceTheme
import com.example.ui.viewmodel.SliceSplitUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    state: SliceSplitUiState,
    onDismiss: () -> Unit,
    onCreateAccount: (String, String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val c = SliceTheme.colors
    var isCreating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(state.accountEmail.orEmpty()) }
    var password by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Your account", style = MaterialTheme.typography.headlineMedium, color = c.inkPrimary)
            Text(
                text = state.accountEmail?.let { "Verified as $it" }
                    ?: "Verify your email to use the same account on more than one phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSecondary,
            )

            if (!state.firebaseConfigured) {
                Text(
                    "Firebase is not connected yet. Add your Firebase app configuration to enable accounts and syncing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.coralInk,
                )
            }

            state.accountMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.lavenderInk)
            }

            if (state.accountEmail != null) {
                Button(
                    onClick = { onSignOut(); onClearMessage() },
                    enabled = !state.accountBusy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.inkPrimary, contentColor = c.card),
                ) { Text("Sign out") }
            } else {
                if (isCreating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; onClearMessage() },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; onClearMessage() },
                    label = { Text("Password (at least 6 characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (isCreating) onCreateAccount(name, email, password) else onSignIn(email, password)
                    },
                    enabled = state.firebaseConfigured && !state.accountBusy && email.contains("@") && password.length >= 6 && (!isCreating || name.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = c.inkPrimary, contentColor = c.card),
                ) {
                    if (state.accountBusy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                    else Text(if (isCreating) "Create account" else "Sign in")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { isCreating = !isCreating; onClearMessage() }) {
                        Text(if (isCreating) "I already have an account" else "Create an account")
                    }
                    if (!isCreating) TextButton(onClick = { onResetPassword(email) }, enabled = email.contains("@") && !state.accountBusy) {
                        Text("Forgot password?")
                    }
                }
                HorizontalDivider()
                Text(
                    "Email verification helps confirm account ownership. It does not verify that a bill or payment is accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSecondary,
                )
            }
        }
    }
}
