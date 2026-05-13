package com.tejashaqua.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.tejashaqua.app.R
import com.tejashaqua.app.SmsReceiver
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    mobileNumber: String,
    onVerifyClick: (String) -> Unit,
    onResendClick: () -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf(24) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            message?.let {
                val otpPattern = Regex("(\\d{6})")
                val match = otpPattern.find(it)
                match?.let { m -> otpValue = m.value }
            }
        }
    }

    DisposableEffect(Unit) {
        SmsRetriever.getClient(context).startSmsUserConsent(null)

        val smsReceiver = SmsReceiver(
            onOtpReceived = { otp -> otpValue = otp },
            onConsentIntentReceived = { intent -> launcher.launch(intent) }
        )
        
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        
        // Version-safe registration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                smsReceiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null,
                Context.RECEIVER_EXPORTED
            )
        } else {
            // For API 32 and below, the exported flag is not required and will crash if used
            context.registerReceiver(
                smsReceiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null
            )
        }
        
        onDispose {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(key1 = timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter OTP",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1B3E)
        )

        Text(
            text = "Sent to +91 $mobileNumber",
            fontSize = 16.sp,
            color = GrayText,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Enter 6-digit code",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = otpValue,
            onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    otpValue = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val char = when {
                            index >= otpValue.length -> ""
                            else -> otpValue[index].toString()
                        }
                        val isFocused = otpValue.length == index
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isFocused) AquaBlue else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(char, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { 
                if (timerSeconds == 0) {
                    onResendClick()
                    timerSeconds = 24
                    SmsRetriever.getClient(context).startSmsUserConsent(null)
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (timerSeconds > 0) "Resend OTP in $timerSeconds Sec" else "Resend OTP",
                fontSize = 14.sp,
                color = if (timerSeconds > 0) GrayText else AquaBlue,
                fontWeight = if (timerSeconds > 0) FontWeight.Normal else FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { if (otpValue.length == 6) onVerifyClick(otpValue) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
            enabled = otpValue.length == 6
        ) {
            Text(text = "Verify & Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
