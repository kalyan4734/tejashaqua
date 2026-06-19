package com.tejashaqua.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsReceiver(
    private val onOtpReceived: (String) -> Unit,
    private val onConsentIntentReceived: (Intent) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras ?: return
            val smsRetrieverStatus = extras.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (smsRetrieverStatus?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // This is for SMS User Consent API
                    val consentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    }

                    if (consentIntent != null) {
                        onConsentIntentReceived(consentIntent)
                    } else {
                        // This might be SMS Retriever API (if hash matches)
                        val message = extras.get(SmsRetriever.EXTRA_SMS_MESSAGE) as? String
                        message?.let { extractOtp(it) }
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    // Handle timeout
                }
            }
        }
    }

    private fun extractOtp(message: String) {
        val otpPattern = Regex("(\\d{6})")
        val match = otpPattern.find(message)
        match?.let {
            onOtpReceived(it.value)
        }
    }
}
