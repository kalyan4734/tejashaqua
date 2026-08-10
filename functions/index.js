const {onCall} = require("firebase-functions/v2/https");
const {onDocumentUpdated, onDocumentCreated} = require("firebase-functions/v2/firestore");
const {defineSecret} = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

const MSG91_AUTH_KEY = defineSecret("MSG91_AUTH_KEY");
const MSG91_TEMPLATE_ID = defineSecret("MSG91_TEMPLATE_ID");

// --- OTP FUNCTIONS (MSG91) ---

const normalizePhoneNumber = (phoneNumber) => {
  if (!phoneNumber) return "";
  let cleaned = phoneNumber.replace(/\D/g, "");
  if (cleaned.length === 10) return "91" + cleaned;
  return cleaned;
};

exports.sendOtp = onCall({secrets: [MSG91_AUTH_KEY, MSG91_TEMPLATE_ID]}, async (request) => {
  const phoneNumber = request.data.phoneNumber;
  if (!phoneNumber) return {success: false, message: "Phone number is required"};
  const formattedPhone = normalizePhoneNumber(phoneNumber);
  try {
    const response = await axios.get("https://control.msg91.com/api/v5/otp", {
      params: { template_id: MSG91_TEMPLATE_ID.value(), mobile: formattedPhone, authkey: MSG91_AUTH_KEY.value() }
    });
    return response.data.type === "success" ? {success: true} : {success: false, message: response.data.message};
  } catch (error) {
    return {success: false, message: "Server error"};
  }
});

exports.verifyOtp = onCall({secrets: [MSG91_AUTH_KEY]}, async (request) => {
  const {phoneNumber, otp} = request.data;
  const formattedPhone = normalizePhoneNumber(phoneNumber);
  try {
    const verifyResponse = await axios.get("https://control.msg91.com/api/v5/otp/verify", {
      params: { otp: otp, mobile: formattedPhone, authkey: MSG91_AUTH_KEY.value() }
    });
    if (verifyResponse.data.type !== "success") return {success: false, message: "Invalid OTP"};

    const fullPhoneNumber = "+" + formattedPhone;
    let userRecord;
    try {
      userRecord = await admin.auth().getUserByPhoneNumber(fullPhoneNumber);
    } catch (e) {
      userRecord = await admin.auth().createUser({ phoneNumber: fullPhoneNumber });
    }
    const customToken = await admin.auth().createCustomToken(userRecord.uid);
    return { success: true, customToken: customToken };
  } catch (error) {
    return {success: false, message: "Verification failed"};
  }
});

// --- NOTIFICATION FUNCTIONS ---

/**
 * Triggered when rates are updated.
 * Prevents multiple notifications by checking the 'notify' flag.
 */
exports.onRateUpdated = onDocumentUpdated("aqua_rates/{type}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();

    // ONLY send notification if 'notify' flag is true AND it wasn't true before (or price changed)
    if (newData.notify === true) {
        const type = event.params.type; // e.g., "Rohu" or "Prawns"
        const price = newData.price;

        logger.info(`Sending single notification for ${type}`);

        const payload = {
            topic: "all_users", // Assuming all users subscribe to this
            notification: {
                title: `Today's ${type} Rates`,
                body: `Latest price: ${price}. Tap to see all rates.`
            },
            data: {
                type: "rates",
                rateType: type
            }
        };

        // Reset the notify flag in DB so it doesn't trigger again on minor edits
        await event.data.after.ref.update({ notify: false });

        return admin.messaging().send(payload);
    }
    return null;
});

/**
 * Triggered on new chat messages.
 * Navigates user to the specific chat.
 */
exports.onChatMessageCreated = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const message = event.data.data();
    const chatId = event.params.chatId;

    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    const chatData = chatDoc.data();
    if (!chatData) return null;

    const recipientId = (message.senderId === chatData.sellerId) ? chatData.buyerId : chatData.sellerId;
    const senderName = (message.senderId === chatData.sellerId) ? chatData.sellerName : chatData.buyerName;

    const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) return null;

    const payload = {
        token: fcmToken,
        notification: {
            title: `New message from ${senderName}`,
            body: message.text
        },
        data: {
            type: "chat",
            chatId: chatId,
            click_action: "OPEN_CHAT"
        },
        android: {
            priority: "high",
            notification: {
                clickAction: "OPEN_CHAT"
            }
        }
    };

    return admin.messaging().send(payload);
});
