package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.viewmodel.AuthViewModel
import com.tejashaqua.app.utils.LocaleHelper
import com.tejashaqua.app.utils.SmsBroadcastReceiver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    mobileNumber: String,
    onVerifyClick: (String) -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    var otpValue by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(24) }

    val autoOtp by authViewModel.autoOtp.collectAsState()

    // Launcher for the SMS User Consent "Allow" popup
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            message?.let {
                // Extract 6-digit OTP from message using the same regex
                val pattern = Regex("(?<!\\d)(\\d{6})(?!\\d)")
                val match = pattern.find(it)
                match?.groupValues?.get(1)?.let { code ->
                    otpValue = code
                }
            }
        }
    }

    // Listen for auto-filled OTP
    LaunchedEffect(autoOtp) {
        autoOtp?.let {
            otpValue = it
            authViewModel.clearAutoOtp()
        }
    }

    // Initialize SMS Retriever
    DisposableEffect(Unit) {
        authViewModel.startOtpRetriever()

        SmsBroadcastReceiver.setOtpListener(object : SmsBroadcastReceiver.OtpListener {
            override fun onOtpReceived(otp: String) {
                authViewModel.setAutoOtp(otp)
            }

            override fun onConsentIntentReceived(intent: android.content.Intent) {
                consentLauncher.launch(intent)
            }

            override fun onOtpTimeout() {
                android.util.Log.d("OtpScreen", "SMS Retrieval timed out")
            }
        })

        onDispose {
            SmsBroadcastReceiver.setOtpListener(object : SmsBroadcastReceiver.OtpListener {
                override fun onOtpReceived(otp: String) {}
                override fun onConsentIntentReceived(intent: android.content.Intent) {}
                override fun onOtpTimeout() {}
            })
            authViewModel.clearAutoOtp()
        }
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.NumberPassword,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    // Automatically verify when OTP is complete (6 digits)
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6 && !isLoading) {
            keyboardController?.hide()
            onVerifyClick(otpValue)
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
            fontSize = 14.sp,
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

        if (otpValue.isEmpty() && !isLoading) {
            Text(
                text = stringResource(R.string.waiting_for_otp),
                fontSize = 14.sp,
                color = AquaBlue.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

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
