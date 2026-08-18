const {onCall} = require("firebase-functions/v2/https");
const {onDocumentUpdated, onDocumentCreated} = require("firebase-functions/v2/firestore");
const {defineSecret} = require("firebase-functions/params");
const {setGlobalOptions} = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

// Mumbai region for best performance in India
setGlobalOptions({ region: "asia-south1" });

const MSG91_AUTH_KEY = defineSecret("MSG91_AUTH_KEY");
const MSG91_TEMPLATE_ID = defineSecret("MSG91_TEMPLATE_ID");

const TEST_PHONE = "919999999999";
const TEST_OTP = "123456";

const normalizePhoneNumber = (phoneNumber) => {
  if (!phoneNumber) return "";
  let cleaned = phoneNumber.replace(/\D/g, "");
  if (cleaned.length === 10) return "91" + cleaned;
  return cleaned;
};

exports.sendOtp = onCall({
  secrets: [MSG91_AUTH_KEY, MSG91_TEMPLATE_ID],
  enforceAppCheck: false
}, async (request) => {
  const phoneNumber = request.data.phoneNumber;
  if (!phoneNumber) return {success: false, message: "Phone number is required"};
  const formattedPhone = normalizePhoneNumber(phoneNumber);

  if (formattedPhone === TEST_PHONE) {
    logger.info(`Test account login attempt: ${formattedPhone}`);
    return {success: true};
  }

  try {
    const response = await axios.get("https://control.msg91.com/api/v5/otp", {
      params: {
        template_id: MSG91_TEMPLATE_ID.value(),
        mobile: formattedPhone,
        authkey: MSG91_AUTH_KEY.value(),
        otp_length: 6
      }
    });

    logger.info(`OTP Request: Phone=${formattedPhone}`);
    return response.data.type === "success" ? {success: true} : {success: false, message: response.data.message};
  } catch (error) {
    logger.error("sendOtp error:", error.response ? error.response.data : error.message);
    return {success: false, message: "Server error"};
  }
});

exports.verifyOtp = onCall({
  secrets: [MSG91_AUTH_KEY],
  enforceAppCheck: false
}, async (request) => {
  const {phoneNumber, otp} = request.data;
  if (!phoneNumber || !otp) return {success: false, message: "Phone and OTP required"};

  const formattedPhone = normalizePhoneNumber(phoneNumber);

  if (formattedPhone === TEST_PHONE && otp === TEST_OTP) {
    logger.info(`Test account login success: ${formattedPhone}`);
    const fullPhoneNumber = "+" + formattedPhone;
    let userRecord;
    try {
      userRecord = await admin.auth().getUserByPhoneNumber(fullPhoneNumber);
    } catch (e) {
      userRecord = await admin.auth().createUser({ phoneNumber: fullPhoneNumber });
    }
    const customToken = await admin.auth().createCustomToken(userRecord.uid);
    return { success: true, customToken: customToken };
  }

  try {
    // 1. Verify with MSG91
    const verifyResponse = await axios.get("https://control.msg91.com/api/v5/otp/verify", {
      params: { otp: otp, mobile: formattedPhone, authkey: MSG91_AUTH_KEY.value() }
    });

    logger.info("MSG91 Verify Response:", verifyResponse.data);

    if (verifyResponse.data.type !== "success") {
      return { success: false, message: verifyResponse.data.message || "Invalid OTP" };
    }

    // 2. Create or Get Firebase User
    const fullPhoneNumber = "+" + formattedPhone;
    let userRecord;
    try {
      userRecord = await admin.auth().getUserByPhoneNumber(fullPhoneNumber);
    } catch (e) {
      if (e.code === "auth/user-not-found") {
        userRecord = await admin.auth().createUser({ phoneNumber: fullPhoneNumber });
      } else {
        throw e;
      }
    }

    // 3. Generate Custom Token
    const customToken = await admin.auth().createCustomToken(userRecord.uid);
    return { success: true, customToken: customToken };

  } catch (error) {
    logger.error("verifyOtp error details:", error);
    // If it's a permission error, we'll see it in the logs
    return {
      success: false,
      message: error.code === "auth/insufficient-permission"
        ? "Server permission error. Check IAM roles."
        : "Verification failed. Please try again."
    };
  }
});

exports.resendOtp = onCall({
  secrets: [MSG91_AUTH_KEY],
  enforceAppCheck: false
}, async (request) => {
  const phoneNumber = request.data.phoneNumber;
  if (!phoneNumber) return {success: false, message: "Phone number is required"};
  const formattedPhone = normalizePhoneNumber(phoneNumber);

  if (formattedPhone === TEST_PHONE) return {success: true};

  try {
    const response = await axios.get("https://control.msg91.com/api/v5/otp/retry", {
      params: { authkey: MSG91_AUTH_KEY.value(), mobile: formattedPhone, retrytype: "text" }
    });
    return response.data.type === "success" ? {success: true} : {success: false, message: response.data.message};
  } catch (error) {
    logger.error("resendOtp error:", error);
    return {success: false, message: "Server error"};
  }
});

/**
 * Triggered on new listings.
 * Notifies all users about the new post.
 */
exports.onListingCreated = onDocumentCreated("listings/{listingId}", async (event) => {
    const listing = event.data.data();
    if (!listing) return null;

    const title = listing.title || "New Ad";
    const category = listing.category || "Listing";
    const posterName = listing.posterName || "User";

    logger.info(`New listing created: ${title} in ${category} by ${posterName}`);

    const payload = {
        topic: "all_listings",
        notification: {
            title: `New Ad in ${category}`,
            body: `${title} posted by ${posterName}`
        },
        data: {
            type: "listing",
            listingId: event.params.listingId,
            category: category
        },
        android: {
            priority: "high",
            notification: {
                sound: "default"
            }
        }
    };

    return admin.messaging().send(payload);
});

/**
 * Triggered when rates are updated.
 */
exports.onRateUpdated = onDocumentUpdated("aqua_rates/{type}", async (event) => {
    const newData = event.data.after.data();
    if (newData.notify === true) {
        const type = event.params.type;
        const price = newData.price || "--";

        // Capitalize for display and generalize Rohu to Fish
        let displayType = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
        if (displayType === "Rohu") displayType = "Fish";

        // Filter out "no change" from notification body
        const isNoChange = price.toLowerCase().includes("no change") || price.includes("మార్పు లేదు");
        const body = isNoChange
            ? `New rates updated. Tap to see all rates.`
            : `Latest price: ${price}. Tap to see all rates.`;

        logger.info(`Sending single notification for ${displayType}`);
        const payload = {
            topic: "all_users",
            notification: {
                title: `Today's ${displayType} Rates`,
                body: body
            },
            data: {
                type: "rates",
                rateType: type
            },
            android: {
                priority: "high",
                notification: {
                    sound: "default",
                    clickAction: "OPEN_RATES"
                }
            }
        };
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

    logger.info(`New message in chat ${chatId}: ${message.text}`);

    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    const chatData = chatDoc.data();
    if (!chatData) {
        logger.error(`Chat metadata not found for ${chatId}`);
        return null;
    }

    const recipientId = (message.senderId === chatData.sellerId) ? chatData.buyerId : chatData.sellerId;
    const senderName = (message.senderId === chatData.sellerId) ? chatData.sellerName : chatData.buyerName;

    logger.info(`Sending notification to ${recipientId} from ${senderName}`);

    const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    const notificationPayload = {
        notification: {
            title: `New message from ${senderName}`,
            body: message.text
        },
        data: {
            type: "chat",
            chatId: chatId,
            senderId: message.senderId,
            click_action: "OPEN_CHAT"
        },
        android: {
            priority: "high",
            notification: {
                sound: "default",
                clickAction: "OPEN_CHAT"
            }
        }
    };

    if (fcmToken) {
        try {
            await admin.messaging().send({
                token: fcmToken,
                ...notificationPayload
            });
            logger.info(`Notification sent to token for ${recipientId}`);
            return null;
        } catch (error) {
            logger.error(`Error sending to token for ${recipientId}:`, error);
            // If token is invalid, consider removing it from user doc
            if (error.code === 'messaging/registration-token-not-registered' ||
                error.code === 'messaging/invalid-registration-token') {
                await admin.firestore().collection("users").doc(recipientId).update({ fcmToken: admin.firestore.FieldValue.delete() });
            }
            // Fall through to topic delivery if token fails
        }
    }

    // Fallback to the personal topic as a secondary mechanism
    // This is more reliable than individual tokens which expire often.
    try {
        await admin.messaging().send({
            topic: `user_${recipientId}`,
            ...notificationPayload
        });
        logger.info(`Notification sent to topic user_${recipientId}`);
    } catch (error) {
        logger.error(`Error sending to topic user_${recipientId}:`, error);
    }

    return null;
});

/**
 * Synchronizes privacy settings across all listings when a user updates their profile.
 */
exports.onUserUpdated = onDocumentUpdated("users/{userId}", async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();

    if (before.showMobileNumber !== after.showMobileNumber) {
        const userId = event.params.userId;
        const newValue = after.showMobileNumber || false;

        logger.info(`Updating mobile visibility for user ${userId} to ${newValue}`);

        const listingsSnapshot = await admin.firestore()
            .collection("listings")
            .where("userId", "==", userId)
            .get();

        if (listingsSnapshot.empty) return null;

        const batch = admin.firestore().batch();
        listingsSnapshot.docs.forEach((doc) => {
            batch.update(doc.ref, { sellerShowMobile: newValue });
        });

        return batch.commit();
    }
    return null;
});
