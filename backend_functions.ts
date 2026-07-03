import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const requestMatch = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be logged in.');
    }

    const uid = context.auth.uid;
    const userRef = db.collection('users').doc(uid);

    try {
        return await db.runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            if (!userDoc.exists) {
                throw new functions.https.HttpsError('not-found', 'User not found.');
            }

            const userData = userDoc.data()!;
            
            // 1. Premium Guard
            if (userData.preference !== 'everyone' && !userData.is_premium) {
                return { status: 'PAYWALL_REQUIRED' };
            }

            // Update caller to searching
            transaction.update(userRef, { status: 'searching' });

            // Note: In Firestore, we cannot run arbitrary queries inside a transaction 
            // if we want to guarantee conflict-free matching across many concurrent users without contention on the whole collection.
            // However, for the scope of this requirement, we will perform a query to find a match.
            // To ensure atomicity and avoid double-matching, we read the potential match inside the transaction.
            
            let matchQuery = db.collection('users')
                .where('status', '==', 'searching')
                .where('uid', '!=', uid)
                .limit(1);

            // If Premium Male looking for Female
            if (userData.gender === 'male' && userData.preference === 'female') {
                matchQuery = db.collection('users')
                    .where('status', '==', 'searching')
                    .where('gender', '==', 'female')
                    // The matched female must either prefer everyone or males
                    .where('preference', 'in', ['everyone', 'male'])
                    .limit(1);
            }

            const matchSnapshot = await transaction.get(matchQuery);

            if (!matchSnapshot.empty) {
                const matchDoc = matchSnapshot.docs[0];
                const matchId = matchDoc.id;

                const roomId = db.collection('chat_rooms').doc().id;

                // Create the room
                const roomRef = db.collection('chat_rooms').doc(roomId);
                transaction.set(roomRef, {
                    room_id: roomId,
                    user_1: uid,
                    user_2: matchId,
                    created_at: Date.now(),
                    messages: []
                });

                // Update both users
                transaction.update(userRef, {
                    status: 'chatting',
                    current_room_id: roomId
                });
                transaction.update(matchDoc.ref, {
                    status: 'chatting',
                    current_room_id: roomId
                });

                return { status: 'MATCHED', room_id: roomId };
            }

            return { status: 'SEARCHING' };
        });
    } catch (error) {
        console.error("Matchmaking error:", error);
        throw new functions.https.HttpsError('internal', 'Matchmaking transaction failed.');
    }
});

export const disconnectUser = functions.https.onCall(async (data, context) => {
    if (!context.auth) return { status: 'unauthenticated' };

    const uid = context.auth.uid;
    const userRef = db.collection('users').doc(uid);

    await db.runTransaction(async (transaction) => {
        const userDoc = await transaction.get(userRef);
        if (!userDoc.exists) return;

        const userData = userDoc.data()!;
        const roomId = userData.current_room_id;

        transaction.update(userRef, {
            status: 'idle',
            current_room_id: null
        });

        if (roomId) {
            const roomRef = db.collection('chat_rooms').doc(roomId);
            const roomDoc = await transaction.get(roomRef);
            
            if (roomDoc.exists) {
                const roomData = roomDoc.data()!;
                const otherUserId = roomData.user_1 === uid ? roomData.user_2 : roomData.user_1;
                
                const otherUserRef = db.collection('users').doc(otherUserId);
                transaction.update(otherUserRef, {
                    status: 'idle',
                    current_room_id: null
                });

                // Ephemeral Cleanup
                transaction.delete(roomRef);
            }
        }
    });

    return { status: 'DISCONNECTED' };
});

import * as express from "express";
import * as crypto from "crypto";

const app = express();
app.use(express.json({ verify: (req: any, res, buf) => { req.rawBody = buf; } }));

const WEBHOOK_SECRET = process.env.PAYMENT_WEBHOOK_SECRET || "fallback_secret_do_not_use_in_prod";

app.post("/webhook/payment", async (req, res) => {
    try {
        // 1. Cryptographic Signature Validation
        const signature = req.headers["x-razorpay-signature"] || req.headers["x-webhook-signature"];
        if (!signature) {
            console.error("Missing webhook signature");
            return res.status(400).send("Bad Request: Missing signature");
        }

        const expectedSignature = crypto
            .createHmac("sha256", WEBHOOK_SECRET)
            .update(req.rawBody || JSON.stringify(req.body))
            .digest("hex");

        if (signature !== expectedSignature) {
            console.error("Invalid webhook signature");
            return res.status(400).send("Bad Request: Invalid signature");
        }

        const event = req.body.event;
        const payload = req.body.payload;

        // 2. Map Payload Metadata on 'payment.captured'
        if (event === "payment.captured" || event === "PAYMENT_SUCCESS") {
            const paymentId = payload?.payment?.entity?.id || req.body.data?.payment?.payment_id;
            const metadata = payload?.payment?.entity?.notes || req.body.data?.payment?.payment_meta;
            
            const userId = metadata?.user_id;
            const subscriptionType = metadata?.subscription_type || "monthly";

            if (!userId) {
                console.error("Missing user_id in webhook metadata");
                return res.status(200).send("Acknowledged but ignored (Missing metadata)"); // 200 to prevent retries
            }

            const userRef = db.collection("users").doc(userId);
            const paymentRef = db.collection("processed_payments").doc(paymentId);

            // 3. Atomically flip 'is_premium' and handle multi-delivery events
            await db.runTransaction(async (transaction) => {
                // Multi-delivery mitigation (Idempotency)
                const paymentDoc = await transaction.get(paymentRef);
                if (paymentDoc.exists) {
                    console.log(`Payment ${paymentId} already processed.`);
                    return; // Abort transaction smoothly
                }

                const userDoc = await transaction.get(userRef);
                if (!userDoc.exists) {
                    console.error(`User ${userId} not found during payment processing.`);
                    return;
                }

                // Register payment to prevent duplicate processing
                transaction.set(paymentRef, {
                    processed_at: admin.firestore.FieldValue.serverTimestamp(),
                    user_id: userId,
                    subscription_type: subscriptionType
                });

                // Calculate subscription window extension
                const now = new Date();
                const activeUntil = new Date(now.setMonth(now.getMonth() + (subscriptionType === 'annual' ? 12 : 1)));

                // Update user document
                transaction.update(userRef, {
                    is_premium: true,
                    subscription_authorization_windows: admin.firestore.FieldValue.arrayUnion({
                        payment_id: paymentId,
                        plan: subscriptionType,
                        granted_at: Date.now(),
                        active_until: activeUntil.getTime()
                    })
                });
            });

            console.log(`Successfully upgraded user ${userId} to premium via payment ${paymentId}`);
        }

        // 4. Explicit HTTP 200 status back to gateway
        return res.status(200).json({ status: "ok" });

    } catch (error) {
        console.error("Webhook processing failed:", error);
        // We still return 200 for certain expected database conflicts to prevent infinite retries
        // However, for internal failures we might return 500 so the gateway retries.
        return res.status(500).send("Internal Server Error");
    }
});

export const paymentWebhook = functions.https.onRequest(app);
