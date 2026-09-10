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

const TEST_PHONE = "919848182726";
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
 * Returns listings sorted by distance from the user's location.
 */
exports.getListingsByLocation = onCall({
    enforceAppCheck: false
}, async (request) => {
    const { lat, lng, locationName, category, page = 0, pageSize = 10 } = request.data;

    try {
        let query = admin.firestore().collection("listings");

        const snapshot = await query.get();
        let listings = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        // Filter by category using the same logic as the app
        if (category && category !== "All") {
            listings = listings.filter(l => {
                const lCat = (l.category || "").toUpperCase();
                const lServiceType = (l.serviceType || "").trim();
                const lBusSubCat = (l.businessSubCategory || "").trim();

                switch (category.toUpperCase()) {
                    case "VEHICLES":
                        return lCat === "VEHICLES" || (lCat === "SERVICES" &&
                            (lServiceType === "Live Fish Vehicles" || lServiceType === "లైవ్ ఫిష్ వెహికల్స్" ||
                             lServiceType === "Bore Well" || lServiceType === "బోర్ వెల్" ||
                             lServiceType === "Earth Movers" || lServiceType === "ఎర్త్ మూవర్స్"));
                    case "FEED":
                        return lCat === "FEED" || (lCat === "BUSINESS" && (lBusSubCat === "Feed" || lBusSubCat === "మేత"));
                    case "BUSINESS":
                        return lCat === "BUSINESS" && (lBusSubCat !== "Feed" && lBusSubCat !== "మేత");
                    case "SERVICES":
                        return lCat === "SERVICES" &&
                            !(lServiceType === "Live Fish Vehicles" || lServiceType === "లైవ్ ఫిష్ వెహికల్స్" ||
                              lServiceType === "Bore Well" || lServiceType === "బోర్ వెల్" ||
                              lServiceType === "Earth Movers" || lServiceType === "ఎర్త్ మూవర్స్");
                    default:
                        return lCat === category.toUpperCase();
                }
            });
        }

        const normalizedUserLocation = (locationName || "").toLowerCase().trim();

        if (lat && lng) {
            // Distance-based sorting
            listings.forEach(listing => {
                const lLat = listing.lat;
                const lLng = listing.lng;
                const lLoc = (listing.location || "").toLowerCase();

                // Check if coordinates exist and are not exactly 0 (invalid for India)
                const hasCoords = typeof lLat === 'number' && typeof lLng === 'number' && lLat !== 0;

                if (hasCoords) {
                    const dLat = lLat - lat;
                    const dLng = lLng - lng;
                    listing.distance = Math.sqrt(dLat * dLat + dLng * dLng);
                } else {
                    // Fallback distance for posts without coordinates (approx 500km)
                    listing.distance = 5.0;
                }

                // PRIORITY: If the village/city name matches exactly, move it to the front
                // This handles very close villages like Chataparru/Sriparru where distance is tiny
                if (normalizedUserLocation && lLoc.includes(normalizedUserLocation)) {
                    listing.distance = listing.distance * 0.001; // Drastic reduction to top
                }
            });

            // Sort by distance first, then by timestamp (latest first) for items at same distance
            listings.sort((a, b) => {
                if (Math.abs(a.distance - b.distance) < 0.0001) {
                    return (b.timestamp || 0) - (a.timestamp || 0);
                }
                return a.distance - b.distance;
            });
        } else {
            // Fallback: Latest first sorting
            listings.sort((a, b) => {
                // If locationName is provided, prioritize matching strings even without lat/lng
                if (normalizedUserLocation) {
                    const aMatch = (a.location || "").toLowerCase().includes(normalizedUserLocation);
                    const bMatch = (b.location || "").toLowerCase().includes(normalizedUserLocation);
                    if (aMatch && !bMatch) return -1;
                    if (!aMatch && bMatch) return 1;
                }
                return (b.timestamp || 0) - (a.timestamp || 0);
            });
        }

        // Pagination
        const start = page * pageSize;
        const paginatedListings = listings.slice(start, start + pageSize);
        const isLastPage = start + pageSize >= listings.length;

        return {
            success: true,
            listings: paginatedListings,
            isLastPage: isLastPage,
            totalCount: listings.length
        };
    } catch (error) {
        logger.error("getListingsByLocation error:", error);
        return { success: false, message: "Failed to fetch listings" };
    }
});

/**
 * Triggered on new listings.
 * Notifies all users about the new post.
 */
exports.onListingCreated = onDocumentCreated("listings/{listingId}", async (event) => {
    const listing = event.data.data();
    if (!listing) {
        logger.error("No listing data found for event:", event.params.listingId);
        return null;
    }

    const title = listing.title || "New Ad";
    const category = listing.category || "Listing";
    const userId = listing.userId || "";
    const posterName = listing.posterName || (userId ? `User_${userId.slice(-4)}` : "User");
    const location = (listing.location || "").split(",")[0].trim() || "Local";

    logger.info(`Processing new listing: ${title} by ${posterName} from ${location} (UserID: ${userId})`);

    const notificationPayload = {
        notification: {
            title: `New Ad in ${category}`,
            body: `${title} posted by ${posterName} from ${location}`
        },
        data: {
            type: "listing",
            listingId: event.params.listingId,
            category: category,
            posterId: userId,
            title: `New Ad in ${category}`,
            body: `${title} posted by ${posterName} from ${location}`,
            click_action: "OPEN_LISTING"
        },
        android: {
            priority: "high",
            notification: {
                sound: "default",
                channelId: "general_notifications_v2",
                clickAction: "OPEN_LISTING"
            }
        }
    };

    // Send only to all_users topic.
    // This reaches everyone, including the poster's other devices.
    // We remove the duplicate sends to all_listings and personal topics.
    return admin.messaging().send({ topic: "all_users", ...notificationPayload })
        .then(res => logger.info("Notification sent to all_users:", res))
        .catch(err => logger.error("Error sending notification:", err));
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
                    channelId: "general_notifications",
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
 * Triggered when a new user is created.
 * Assigns a random default name like User_1234.
 */
exports.onUserCreated = onDocumentCreated("users/{userId}", async (event) => {
    const userData = event.data.data();
    if (!userData) return null;

    if (!userData.name || userData.name === "User") {
        const userId = event.params.userId;
        const randomName = `User_${Math.floor(1000 + Math.random() * 9000)}`;
        logger.info(`Assigning default name ${randomName} to user ${userId}`);
        return event.data.ref.update({ name: randomName });
    }
    return null;
});

/**
 * Synchronizes user profile changes (name, privacy) across all listings and chats.
 */
exports.onUserUpdated = onDocumentUpdated("users/{userId}", async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    const userId = event.params.userId;

    const batch = admin.firestore().batch();
    let needsUpdate = false;

    // 1. Sync Mobile Visibility to Listings
    if (before.showMobileNumber !== after.showMobileNumber) {
        const newValue = after.showMobileNumber || false;
        logger.info(`Updating mobile visibility for user ${userId} to ${newValue}`);

        const listingsSnapshot = await admin.firestore()
            .collection("listings")
            .where("userId", "==", userId)
            .get();

        listingsSnapshot.docs.forEach((doc) => {
            batch.update(doc.ref, { sellerShowMobile: newValue });
        });
        needsUpdate = true;
    }

    // 2. Sync Name to Listings and Chats
    if (before.name !== after.name && after.name) {
        const newName = after.name;
        logger.info(`Syncing name change for user ${userId} to ${newName}`);

        // Update all listings by this user
        const listingsSnapshot = await admin.firestore()
            .collection("listings")
            .where("userId", "==", userId)
            .get();
        listingsSnapshot.docs.forEach((doc) => {
            batch.update(doc.ref, { posterName: newName });
        });

        // Update all chats where user is seller
        const chatsAsSeller = await admin.firestore()
            .collection("chats")
            .where("sellerId", "==", userId)
            .get();
        chatsAsSeller.docs.forEach((doc) => {
            batch.update(doc.ref, { sellerName: newName });
        });

        // Update all chats where user is buyer
        const chatsAsBuyer = await admin.firestore()
            .collection("chats")
            .where("buyerId", "==", userId)
            .get();
        chatsAsBuyer.docs.forEach((doc) => {
            batch.update(doc.ref, { buyerName: newName });
        });

        needsUpdate = true;
    }

    if (needsUpdate) {
        return batch.commit();
    }
    return null;
});
