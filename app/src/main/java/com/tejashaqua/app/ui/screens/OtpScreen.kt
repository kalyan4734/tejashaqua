package com.tejashaqua.app.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.tejashaqua.app.utils.AppSignatureHelper
import com.google.android.gms.tasks.Task
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.utils.LocaleHelper
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    mobileNumber: String,
    onVerifyClick: (String) -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false
) {
    var otpValue by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(24) }
    // Removed isAdminChecked state and isAdminNumber logic

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.NumberPassword,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    // Launcher for the "Allow" popup
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            message?.let {
                val otpPattern = Regex("\\b(\\d{6})\\b")
                val match = otpPattern.find(it)
                match?.let { m -> otpValue = m.value }
            }
        }
    }

    // Automatically verify when OTP is complete (6 digits)
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6 && !isLoading) {
            keyboardController?.hide()
            onVerifyClick(otpValue)
        }
    }

    // Log hash once for debugging and start SMS User Consent
    DisposableEffect(Unit) {
        val appSignatureHelper = AppSignatureHelper(context)
        Log.d("OtpScreen", "App Hash for SMS Retriever: ${appSignatureHelper.appSignatures}")

        // Start SMS User Consent API safely
        try {
            val task: Task<Void> = SmsRetriever.getClient(context).startSmsUserConsent(null)
            task.addOnSuccessListener { Log.d("OtpScreen", "SMS User Consent started successfully") }
            task.addOnFailureListener { e -> Log.e("OtpScreen", "Failed to start SMS User Consent", e) }
        } catch (e: Exception) {
            Log.e("OtpScreen", "Error starting SMS User Consent", e)
        }

        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                        val extras = intent.extras ?: return
                        
                        // IF Firebase Auth's zzafa class is present in the extras, we ignore this broadcast.
                        // This prevents the NullPointerException in their internal matcher.
                        if (extras.containsKey("com.google.android.gms.auth.api.phone.EXTRA_SMS_MESSAGE")) {
                             // This is likely the hash-based retriever message which Firebase handles.
                             // To avoid the NPE crash in Firebase, we should not touch this if it's already being processed.
                             Log.d("OtpScreen", "Ignoring hash-based broadcast to prevent Firebase NPE crash")
                             return 
                        }

                        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            extras.getParcelable(SmsRetriever.EXTRA_STATUS, Status::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            extras.getParcelable(SmsRetriever.EXTRA_STATUS)
                        }

                        when (status?.statusCode) {
                            CommonStatusCodes.SUCCESS -> {
                                val consentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                                }
                                try {
                                    consentIntent?.let { launcher.launch(it) }
                                } catch (e: Exception) {
                                    Log.e("OtpScreen", "Error launching consent intent", e)
                                }
                            }
                            CommonStatusCodes.TIMEOUT -> {
                                Log.d("OtpScreen", "SMS Retrieval Timeout - Restarting Consent")
                                context?.let { 
                                    try {
                                        SmsRetriever.getClient(it).startSmsUserConsent(null)
                                    } catch (e: Exception) {
                                        Log.e("OtpScreen", "Error restarting consent on timeout", e)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // CATCH EVERYTHING to prevent crash on main thread
                    Log.e("OtpScreen", "CRITICAL: Suppressed exception in onReceive to prevent crash", e)
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(smsReceiver, intentFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(smsReceiver, intentFilter)
            }
        } catch (e: Exception) {
            Log.e("OtpScreen", "Error registering receiver", e)
        }

        onDispose {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (_: Exception) {
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
            .padding(24.dp)
            .clickable { keyboardController?.hide() },
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(
            onClick = {
                keyboardController?.hide()
                onBackClick()
            },
            modifier = Modifier.offset(x = (-12).dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.enter_otp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1B3E)
        )

        Text(
            text = stringResource(R.string.sent_to, mobileNumber),
            fontSize = 16.sp,
            color = GrayText,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.enter_6_digit_code),
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
            keyboardOptions = keyboardOptions,
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
                if (timerSeconds == 0 && !isLoading) {
                    onResendClick()
                    timerSeconds = 24
                    // Re-trigger User Consent on resend
                    SmsRetriever.getClient(context).startSmsUserConsent(null)
                }
            },
            contentPadding = PaddingValues(0.dp),
            enabled = (timerSeconds == 0) && !isLoading
        ) {
            Text(
                text = if (timerSeconds > 0) stringResource(R.string.resend_otp_in, timerSeconds) else stringResource(R.string.resend_otp),
                fontSize = 14.sp,
                color = if (timerSeconds > 0) GrayText else AquaBlue,
                fontWeight = if (timerSeconds > 0) FontWeight.Normal else FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                if (otpValue.length == 6) onVerifyClick(otpValue)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
            enabled = otpValue.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.verify_continue), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
