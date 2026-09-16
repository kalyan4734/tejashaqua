const {onCall} = require("firebase-functions/v2/https");
const {onDocumentUpdated, onDocumentCreated} = require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
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
    const { lat, lng, locationName, category, radius, minPrice, maxPrice, sortBy, page = 0, pageSize = 10 } = request.data;

    try {
        const snapshot = await admin.firestore().collection("listings").get();
        let listings = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        // 1. Filter by category
        if (category && category !== "All") {
            listings = listings.filter(l => {
                const lCat = (l.category || "").toUpperCase();
                const lServiceType = (l.serviceType || "").trim().toLowerCase();
                const lBusSubCat = (l.businessSubCategory || "").trim().toLowerCase();
                const catUpper = category.toUpperCase();

                switch (catUpper) {
                    case "VEHICLES":
                        return lCat === "VEHICLES" || lCat === "VEHICLE" || (lCat === "SERVICES" &&
                            (lServiceType.includes("vehicle") || lServiceType.includes("వెహికల్") ||
                             lServiceType.includes("bore well") || lServiceType.includes("బోర్ వెల్") ||
                             lServiceType.includes("earth mover") || lServiceType.includes("ఎర్త్ మూవర్")));
                    case "FEED":
                        return lCat === "FEED" || (lCat === "BUSINESS" && (lBusSubCat === "feed" || lBusSubCat === "మేత"));
                    case "MEDICINE":
                        return lCat === "MEDICINE" || (lCat === "BUSINESS" && (lBusSubCat === "medicine" || lBusSubCat === "మెడిసిన్" || lBusSubCat === "మందులు"));
                    case "BUSINESS":
                        return (lCat === "BUSINESS" || lCat === "BIZ") &&
                               !(lBusSubCat === "feed" || lBusSubCat === "మేత" || lBusSubCat === "medicine" || lBusSubCat === "మెడిసిన్" || lBusSubCat === "మందులు");
                    case "SERVICES":
                        return lCat === "SERVICES" &&
                            !(lServiceType.includes("vehicle") || lServiceType.includes("వెహికల్") ||
                              lServiceType.includes("bore well") || lServiceType.includes("బోర్ వెల్") ||
                              lServiceType.toLowerCase().includes("earth mover") || lServiceType.includes("ఎర్త్ మూవర్"));
                    case "PRAWNS":
                        return lCat === "PRAWNS" || lCat === "PRAWN" || lCat === "HATCHERY";
                    case "FISH":
                        return lCat === "FISH" || lCat === "SEED";
                    case "TANKS":
                        return lCat === "TANKS" || lCat === "TANK" || lCat === "POND" || lCat === "LAND";
                    default:
                        return lCat === catUpper;
                }
            });
        }

        // 2. Filter by Price Range
        if (minPrice !== undefined && minPrice !== null) {
            listings = listings.filter(l => {
                const p = parseFloat(String(l.price || l.rateValue || l.ratePerTon || l.salary || l.estPricePerAcre || 0).replace(/,/g, ""));
                return !isNaN(p) && p >= minPrice;
            });
        }
        if (maxPrice !== undefined && maxPrice !== null) {
            listings = listings.filter(l => {
                const p = parseFloat(String(l.price || l.rateValue || l.ratePerTon || l.salary || l.estPricePerAcre || 0).replace(/,/g, ""));
                return !isNaN(p) && p <= maxPrice;
            });
        }

        const normalizedUserLocation = (locationName || "").toLowerCase().trim();

        // 3. Distance and Distance Filtering
        if (lat && lng) {
            listings.forEach(listing => {
                const lLat = listing.lat;
                const lLng = listing.lng;
                const lLoc = (listing.location || "").toLowerCase();

                if (typeof lLat === "number" && typeof lLng === "number" && lLat !== 0) {
                    const dLat = (lLat - lat) * 111.32;
                    const dLng = (lLng - lng) * 111.32 * Math.cos(lat * Math.PI / 180);
                    listing.distance = Math.sqrt(dLat * dLat + dLng * dLng);
                } else {
                    listing.distance = 9999;
                }

                if (normalizedUserLocation && lLoc.includes(normalizedUserLocation)) {
                    listing.distance = listing.distance * 0.01;
                }
            });

            if (radius) {
                listings = listings.filter(l => l.distance <= radius || (normalizedUserLocation && (l.location || "").toLowerCase().includes(normalizedUserLocation)));
            }
        }

        // 4. Sorting
        switch (sortBy) {
            case "Price: Low to High":
                listings.sort((a, b) => {
                    const pa = parseFloat(String(a.price || a.rateValue || a.ratePerTon || a.salary || a.estPricePerAcre || 0).replace(/,/g, ""));
                    const pb = parseFloat(String(b.price || b.rateValue || b.ratePerTon || b.salary || b.estPricePerAcre || 0).replace(/,/g, ""));
                    return pa - pb;
                });
                break;
            case "Price: High to Low":
                listings.sort((a, b) => {
                    const pa = parseFloat(String(a.price || a.rateValue || a.ratePerTon || a.salary || a.estPricePerAcre || 0).replace(/,/g, ""));
                    const pb = parseFloat(String(b.price || b.rateValue || b.ratePerTon || b.salary || b.estPricePerAcre || 0).replace(/,/g, ""));
                    return pb - pa;
                });
                break;
            case "Nearest First":
                if (lat && lng) {
                    listings.sort((a, b) => (a.distance || 9999) - (b.distance || 9999));
                } else {
                    listings.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
                }
                break;
            case "Most Viewed":
                listings.sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0));
                break;
            case "Newest First":
            default:
                // Default: Sort by distance if available, then by time
                if (lat && lng && (!sortBy || sortBy === "Newest First")) {
                    listings.sort((a, b) => {
                        if (Math.abs((a.distance || 9999) - (b.distance || 9999)) < 0.1) {
                            return (b.timestamp || 0) - (a.timestamp || 0);
                        }
                        return (a.distance || 9999) - (b.distance || 9999);
                    });
                } else {
                    listings.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
                }
                break;
        }

        const start = page * pageSize;
        const paginated = listings.slice(start, start + pageSize);

        return {
            success: true,
            listings: paginated,
            isLastPage: start + pageSize >= listings.length,
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
                    channelId: "general_notifications_v2",
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

/**
 * Sends a notification to a specific user and logs it.
 */
exports.sendDirectNotification = onCall({
    enforceAppCheck: false
}, async (request) => {
    const { userId, title, body, imageUrl } = request.data;

    if (!userId || !title || !body) {
        return { success: false, message: "UserID, title and body are required" };
    }

    try {
        // 1. Get user's FCM token
        const userDoc = await admin.firestore().collection("users").doc(userId).get();
        const fcmToken = userDoc.data()?.fcmToken;

        if (!fcmToken) {
            return { success: false, message: "User does not have a valid FCM token" };
        }

        const message = {
            token: fcmToken,
            notification: {
                title: title,
                body: body,
                image: imageUrl || undefined
            },
            android: {
                notification: {
                    image: imageUrl || undefined,
                    channelId: "general_notifications_v2"
                }
            },
            data: {
                title: title,
                body: body,
                imageUrl: imageUrl || ""
            }
        };

        // 2. Send the message
        await admin.messaging().send(message);

        // 3. Log the notification for admin visibility
        await admin.firestore().collection("notification_logs").add({
            type: "direct",
            targetUserId: userId,
            targetUserName: userDoc.data()?.name || "Unknown",
            title: title,
            body: body,
            imageUrl: imageUrl || "",
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            status: "success"
        });

        logger.info(`Direct notification sent to ${userId}`);
        return { success: true };
    } catch (error) {
        logger.error("Error sending direct notification:", error);
        return { success: false, error: error.message };
    }
});

/**
 * Sends a notification to all users immediately.
 */
exports.sendAdminNotification = onCall({
    enforceAppCheck: false
}, async (request) => {
    const { title, body, imageUrl } = request.data;

    if (!title || !body) {
        return { success: false, message: "Title and body are required" };
    }

    const message = {
        topic: "all_users",
        notification: {
            title: title,
            body: body,
            image: imageUrl || undefined
        },
        android: {
            notification: {
                image: imageUrl || undefined,
                channelId: "general_notifications_v2"
            }
        },
        data: {
            title: title,
            body: body,
            imageUrl: imageUrl || ""
        }
    };

    try {
        await admin.messaging().send(message);

        // Log broadcast for admin visibility
        await admin.firestore().collection("notification_logs").add({
            type: "broadcast",
            targetTopic: "all_users",
            title: title,
            body: body,
            imageUrl: imageUrl || "",
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            status: "success"
        });

        logger.info("Admin notification sent successfully");
        return { success: true };
    } catch (error) {
        logger.error("Error sending admin notification:", error);
        return { success: false, error: error.message };
    }
});

/**
 * Scheduled task that runs every minute to send pending notifications.
 */
exports.sendScheduledNotifications = onSchedule({
    schedule: "* * * * *",
    timeZone: "Asia/Kolkata"
}, async (event) => {
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();

    try {
        const snapshot = await db.collection("scheduled_notifications")
            .where("status", "==", "pending")
            .where("scheduledTime", "<=", now)
            .get();

        if (snapshot.empty) {
            logger.info("No pending scheduled notifications to send.");
            return null;
        }

        logger.info(`Found ${snapshot.size} scheduled notifications to process.`);

        const results = await Promise.all(snapshot.docs.map(async (doc) => {
            const data = doc.data();
            const notificationId = doc.id;

            const message = {
                topic: "all_users",
                notification: {
                    title: data.title,
                    body: data.body,
                    image: data.imageUrl || undefined
                },
                android: {
                    priority: "high",
                    notification: {
                        image: data.imageUrl || undefined,
                        channelId: "general_notifications_v2",
                        clickAction: "OPEN_LISTING"
                    }
                },
                data: {
                    type: "announcement",
                    title: data.title,
                    body: data.body,
                    imageUrl: data.imageUrl || ""
                }
            };

            try {
                // Update status to "sending" first to prevent double-processing
                await doc.ref.update({ status: "sending" });

                await admin.messaging().send(message);

                await doc.ref.update({
                    status: "sent",
                    sentAt: admin.firestore.FieldValue.serverTimestamp()
                });

                logger.info(`Successfully sent scheduled notification: ${notificationId}`);
                return { id: notificationId, success: true };
            } catch (error) {
                logger.error(`Failed to send notification ${notificationId}:`, error);
                await doc.ref.update({ status: "failed", error: error.message });
                return { id: notificationId, success: false, error: error.message };
            }
        }));

        return results;
    } catch (err) {
        logger.error("Error in sendScheduledNotifications cron:", err);
        return null;
    }
});
