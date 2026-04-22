package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onSendOtp: (String) -> Unit) {
    var mobileNumber by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.Start
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
                append("Welcome to ")
                withStyle(style = SpanStyle(color = AquaBlue, fontWeight = FontWeight.Bold)) {
                    append("Tejash Aqua!")
                }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Label
        Text(
            text = "Enter Your Mobile Number",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                    Text(text = "+91", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        mobileNumber = it
                        isError = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "9876543210", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = AquaBlue,
                    cursorColor = AquaBlue
                )
            )
        }

        if (isError) {
            Text(
                text = "Please enter a valid 10-digit mobile number",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 100.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Helper Text
        Text(
            text = "We will send a 6-digit OTP to verify your number",
            fontSize = 14.sp,
            color = GrayText
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Send OTP Button
        Button(
            onClick = { 
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
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
        ) {
            Text(text = "Send OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Terms and Privacy Footer
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("By continuing you agree to our ")
            }
            pushStringAnnotation(tag = "TERMS", annotation = "terms")
            withStyle(style = SpanStyle(color = AquaBlue, fontWeight = FontWeight.Bold)) {
                append("Terms & Conditions")
            }
            pop()
            withStyle(style = SpanStyle(color = Color.Black)) {
                append(" and ")
            }
            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
            withStyle(style = SpanStyle(color = AquaBlue, fontWeight = FontWeight.Bold)) {
                append("Privacy Policy")
            }
            pop()
        }

        Text(
            text = annotatedText,
            style = TextStyle(fontSize = 14.sp),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
