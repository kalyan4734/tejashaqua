package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    title: String,
    content: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = content,
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    lineHeight = 24.sp
                )
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

object LegalConstants {
    const val PRIVACY_POLICY = """
Privacy Policy for TejashAqua
Effective Date: May 20, 2026

TejashAqua ("we," "our," or "us") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and disclose your information when you use our mobile application.

1. Information We Collect
• Personal Data: We collect your mobile number for authentication via OTP. We also collect your name if you choose to provide it for your profile.
• Location Information: With your permission, we collect precise or approximate location data to show you relevant listings in your area and to tag your posts with a location.
• Media: When you create a listing, we collect the photos you upload to display them to other users.
• Usage Data: Information on how you interact with the app (e.g., search queries, viewed listings).

2. How We Use Information
• To provide and maintain the marketplace service.
• To enable communication between buyers and sellers via chat.
• To display real-time aqua rates and market trends.
• To improve our app's features and user experience.

3. Data Sharing
We do not sell your personal data. Your name, location, and contact number (if provided in a listing) will be visible to other users of the marketplace to facilitate transactions.

4. Security
We use industry-standard security (Firebase/Google Cloud) to protect your data, but no method of transmission over the internet is 100% secure.

5. Changes to This Policy
We may update our Privacy Policy from time to time. You are advised to review this page periodically for any changes.
    """

    const val TERMS_CONDITIONS = """
Terms and Conditions for TejashAqua

By downloading or using the TejashAqua app, you agree to these terms:

1. Eligibility
You must be at least 18 years old to use this app or have parental supervision.

2. User Content
You are solely responsible for the content of your listings (titles, prices, images). You must not post illegal, fraudulent, or misleading information.

3. Transactions
TejashAqua is a marketplace platform. We do not participate in the actual transaction between buyers and sellers. We are not responsible for the quality, safety, or legality of the items advertised.

4. Prohibited Activities
You may not use the app for any unauthorized or illegal purpose, including harassing other users or scraping data.

5. Termination
We reserve the right to ban or suspend accounts that violate these terms or engage in fraudulent activity.

6. Limitation of Liability
TejashAqua shall not be liable for any indirect, incidental, or consequential damages arising out of your use of the app.

7. Governing Law
These terms shall be governed by and construed in accordance with the laws of India.
    """
}
