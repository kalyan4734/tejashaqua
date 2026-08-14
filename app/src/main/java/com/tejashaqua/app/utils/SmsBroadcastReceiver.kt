package com.tejashaqua.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private var otpListener: OtpListener? = null

        fun setOtpListener(listener: OtpListener) {
            this.otpListener = listener
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            @Suppress("DEPRECATION")
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Check if it's the User Consent intent (Popup)
                    val consentIntent = extras?.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    if (consentIntent != null) {
                        otpListener?.onConsentIntentReceived(consentIntent)
                        return
                    }

                    // Else, try to get the message directly (SMS Retriever / Silent)
                    @Suppress("DEPRECATION")
                    val message = extras?.get(SmsRetriever.EXTRA_SMS_MESSAGE) as? String
                    message?.let {
                        android.util.Log.d("SmsReceiver", "SMS Received: $it")
                        // Extract 6-digit OTP
                        val otp = extractOtp(it)
                        if (otp != null) {
                            android.util.Log.d("SmsReceiver", "OTP Extracted: $otp")
                            otpListener?.onOtpReceived(otp)
                        } else {
                            android.util.Log.w("SmsReceiver", "OTP not found in message")
                        }
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    android.util.Log.e("SmsReceiver", "SMS Retrieval timed out")
                    otpListener?.onOtpTimeout()
                }
            }
        }
    }

    private fun extractOtp(message: String): String? {
        // Look for exactly 6 digits that are not part of another number
        val pattern = Regex("(?<!\\d)(\\d{6})(?!\\d)")
        val match = pattern.find(message)
        return match?.groupValues?.get(1)
    }

    interface OtpListener {
        fun onOtpReceived(otp: String)
        fun onConsentIntentReceived(intent: Intent)
        fun onOtpTimeout()
    }
}
