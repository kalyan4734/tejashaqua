package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.utils.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSendOtp: (String) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    isLoading: Boolean = false
) {
    var mobileNumber by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(value = false) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Phone,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Text
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.welcome_to))
                withStyle(style = SpanStyle(color = AquaBlue, fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.tejash_aqua))
                }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Label
        Text(
            text = stringResource(R.string.enter_mobile_number),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Country Code Card
            OutlinedCard(
                modifier = Modifier
                    .height(56.dp)
                    .width(86.dp),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true).copy(width = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.country_code), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Phone Number Input
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { 
                    if ((it.length <= 10) && it.all { char -> char.isDigit() }) {
                        mobileNumber = it
                        isError = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.phone_placeholder), color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = keyboardOptions,
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    {
                        Text(
                            text = stringResource(R.string.invalid_phone_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = AquaBlue,
                    cursorColor = AquaBlue
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Helper Text
        Text(
            text = stringResource(R.string.otp_helper_text),
            fontSize = 14.sp,
            color = GrayText
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Send OTP Button
        Button(
            onClick = { 
                keyboardController?.hide()
                if (mobileNumber.length == 10) {
                    onSendOtp(mobileNumber)
                } else {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.send_otp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Terms and Privacy Footer
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append(stringResource(R.string.agree_terms_prefix))
                append(" ")
            }

            val linkStyle = TextLinkStyles(
                style = SpanStyle(color = AquaBlue, fontWeight = FontWeight.Bold)
            )

            withLink(
                LinkAnnotation.Clickable(
                    tag = "TERMS",
                    styles = linkStyle,
                    linkInteractionListener = {
                        onTermsClick()
                    }
                )
            ) {
                append(stringResource(R.string.terms_conditions))
            }

            withStyle(style = SpanStyle(color = Color.Black)) {
                append(" ")
                append(stringResource(R.string.and))
                append(" ")
            }

            withLink(
                LinkAnnotation.Clickable(
                    tag = "PRIVACY",
                    styles = linkStyle,
                    linkInteractionListener = {
                        onPrivacyPolicyClick()
                    }
                )
            ) {
                append(stringResource(R.string.privacy_policy))
            }
        }

        Text(
            text = annotatedText,
            style = TextStyle(fontSize = 14.sp),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
