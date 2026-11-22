package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.math.SplitMath
import com.example.data.model.AppPreferences
import com.example.data.model.Member
import com.example.data.model.PastelTone
import com.example.data.model.PaymentMethod
import com.example.data.model.SimplifiedTransfer
import com.example.ui.components.MascotBadge
import com.example.ui.components.MascotMood
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SliceStatusPill
import com.example.ui.theme.SliceTheme
import com.example.ui.theme.tabular
import kotlin.math.roundToLong

@Composable
fun SettleUpScreen(
    groupName: String,
    transfers: List<SimplifiedTransfer>,
    initialFromId: String?,
    initialToId: String?,
    initialAmountPaise: Long,
    membersMap: Map<String, Member>,
    currentUserId: String,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onConfirmSettlement: (fromId: String, toId: String, amountPaise: Long, method: PaymentMethod) -> Unit,
    onCopyUpiId: (String) -> Unit,
    onPayWithUpi: (upiId: String, payeeName: String, amountPaise: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val c = SliceTheme.colors
    var selectedTransfer by remember {
        mutableStateOf(
            if (initialFromId != null && initialToId != null && initialAmountPaise > 0L) {
                SimplifiedTransfer(initialFromId, initialToId, initialAmountPaise)
            } else {
                transfers.firstOrNull()
            },
        )
    }
    var amountText by remember(selectedTransfer) {
        val paise = selectedTransfer?.amountPaise ?: initialAmountPaise
        val whole = paise / 100L
        val frac = paise % 100L
        mutableStateOf(
            if (paise <= 0L) "0"
            else if (frac == 0L) whole.toString()
            else "$whole.${frac.toString().padStart(2, '0')}",
        )
    }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var isSettledSuccess by remember { mutableStateOf(false) }
    var settledAmountSnapshot by remember { mutableStateOf(0L) }

    val fromMember = selectedTransfer?.fromMemberId?.let { membersMap[it] }
    val toMember = selectedTransfer?.toMemberId?.let { membersMap[it] }
    val upiAmountPaise = ((amountText.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = isSettledSuccess,
            transitionSpec = {
                fadeIn(spring(dampingRatio = 0.9f, stiffness = 420f)) togetherWith
                    fadeOut(spring(dampingRatio = 1f, stiffness = 560f))
            },
            label = "settleSuccessTransition",
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize(),
        ) { settled ->
            if (settled) {
                // Joyful minimal All Square confirmation card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = c.card,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MascotBadge(
                                mood = MascotMood.EXCITED,
                                tone = PastelTone.MINT,
                                size = 80.dp,
                                modifier = Modifier.testTag("all_square_mascot"),
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "All Square!",
                                style = MaterialTheme.typography.displayMedium,
                                color = c.inkPrimary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val fromName = if (fromMember?.id == currentUserId) "You" else fromMember?.name ?: "Payer"
                            val toName = if (toMember?.id == currentUserId) "You" else toMember?.name ?: "Receiver"
                            Text(
                                text = "$fromName paid $toName via ${selectedMethod.label}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.inkSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SliceStatusPill(
                                text = SplitMath.formatMoney(
                                    settledAmountSnapshot,
                                    preferences.currency,
                                    preferences.paisePrecision,
                                ),
                                fillColor = c.mintFill,
                                textColor = c.mintInk,
                            )
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(
                                onClick = onBack,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = c.inkPrimary,
                                    contentColor = c.card,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("settle_done_button"),
                            ) {
                                Text("Done", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = c.card,
                                modifier = Modifier.size(48.dp),
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("settle_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = c.inkPrimary,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Settle Up",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = c.inkPrimary,
                                )
                                Text(
                                    text = groupName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.inkSecondary,
                                )
                            }
                        }

                        if (transfers.size > 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                transfers.forEach { t ->
                                    val active = t == selectedTransfer
                                    val fName = if (t.fromMemberId == currentUserId) "You" else membersMap[t.fromMemberId]?.name ?: ""
                                    val tName = if (t.toMemberId == currentUserId) "You" else membersMap[t.toMemberId]?.name ?: ""
                                    Surface(
                                        shape = CircleShape,
                                        color = if (active) c.inkPrimary else c.card,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedTransfer = t },
                                    ) {
                                        Text(
                                            text = "$fName → $tName",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (active) c.card else c.inkPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Minimal Transfer Card: Payer -> Receiver
                        if (fromMember != null && toMember != null) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = c.card,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            MemberAvatar(member = fromMember, size = 58.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (fromMember.id == currentUserId) "You" else fromMember.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = c.inkPrimary,
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = c.lavenderFill,
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "pays",
                                                    tint = c.lavenderInk,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            MemberAvatar(member = toMember, size = 58.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (toMember.id == currentUserId) "You" else toMember.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = c.inkPrimary,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(26.dp))

                                    Text(
                                        text = "Settlement Amount",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = c.inkSecondary,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Editable large tabular amount
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(c.mist)
                                            .padding(horizontal = 22.dp, vertical = 14.dp),
                                    ) {
                                        Text(
                                            text = preferences.currency.symbol,
                                            style = MaterialTheme.typography.displayMedium.tabular(),
                                            color = c.inkSecondary,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BasicTextField(
                                            value = amountText,
                                            onValueChange = { next ->
                                                if (next.all { it.isDigit() || it == '.' } && next.length <= 9) {
                                                    amountText = next
                                                }
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            textStyle = MaterialTheme.typography.displayMedium.tabular().copy(
                                                color = c.inkPrimary,
                                                fontSize = 36.sp,
                                            ),
                                            cursorBrush = SolidColor(c.inkPrimary),
                                            modifier = Modifier
                                                .widthIn(min = 60.dp, max = 180.dp)
                                                .testTag("settle_amount_input"),
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(22.dp))

                                    Text(
                                        text = "Payment Method",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = c.inkSecondary,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        PaymentMethod.entries.forEach { method ->
                                            val selected = selectedMethod == method
                                            Surface(
                                                shape = CircleShape,
                                                color = if (selected) c.inkPrimary else c.mist,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(CircleShape)
                                                    .clickable { selectedMethod = method }
                                                    .testTag("payment_method_${method.name.lowercase()}"),
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = method.label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (selected) c.card else c.inkPrimary,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    if (toMember.upiId.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = c.lavenderFill,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Pay ${toMember.name}", style = MaterialTheme.typography.labelLarge, color = c.lavenderInk)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(toMember.upiId, style = MaterialTheme.typography.bodyLarge, color = c.lavenderInk)
                                                    TextButton(onClick = { onCopyUpiId(toMember.upiId) }) { Text("Copy") }
                                                }
                                                if (fromMember.id == currentUserId && selectedMethod == PaymentMethod.UPI) {
                                                    Button(
                                                        onClick = { onPayWithUpi(toMember.upiId, toMember.name, upiAmountPaise) },
                                                        enabled = upiAmountPaise > 0L,
                                                        shape = CircleShape,
                                                        modifier = Modifier.fillMaxWidth().testTag("pay_with_upi_button"),
                                                    ) {
                                                        Text("Pay with UPI app")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            "${toMember.name} hasn’t added a UPI ID yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.inkSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val parsedPaise = ((amountText.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong()
                    Button(
                        onClick = {
                            if (fromMember != null && toMember != null && parsedPaise > 0L) {
                                settledAmountSnapshot = parsedPaise
                                onConfirmSettlement(fromMember.id, toMember.id, parsedPaise, selectedMethod)
                                isSettledSuccess = true
                            }
                        },
                        enabled = fromMember != null && toMember != null && parsedPaise > 0L,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.inkPrimary,
                            contentColor = c.card,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("confirm_settlement_button"),
                    ) {
                        Text(
                            text = "Mark as settled",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
