import express, { Request, Response } from 'express';
import * as crypto from 'crypto';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin if not already initialized
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();

const app = express();

// Required to get the raw body for signature verification
app.use(express.json({
    verify: (req: any, res, buf) => {
        req.rawBody = buf.toString('utf8');
    }
}));

const WEBHOOK_SECRET = process.env.RAZORPAY_WEBHOOK_SECRET || 'fallback_secret_do_not_use_in_prod';

// 1. Strict Identity Hiding (Configuration Example / Note)
// In Razorpay Dashboard, set the billing label to "VIBE-CONNECT-SERVICES"
// so the consumer sees this generic string rather than personal banking names.

// 2. Webhook Signature Handshake Verification
app.post('/api/v1/billing/webhook', async (req: Request, res: Response): Promise<any> => {
    try {
        const signature = req.headers['x-razorpay-signature'] as string;
        
        if (!signature) {
            console.error('[Billing] Missing x-razorpay-signature header. Potential malicious bypass attempt.');
            return res.status(400).json({ error: 'Missing signature' });
        }

        const rawBody = (req as any).rawBody;
        if (!rawBody) {
            console.error('[Billing] Raw body is missing. Cannot verify signature.');
            return res.status(400).json({ error: 'Missing raw payload' });
        }

        const expectedSignature = crypto
            .createHmac('sha256', WEBHOOK_SECRET)
            .update(rawBody)
            .digest('hex');

        // Prevent timing attacks by using timingSafeEqual
        if (crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSignature)) === false) {
            console.error('[Billing] Signature mismatch. Malicious payload detected.');
            return res.status(403).json({ error: 'Invalid signature' });
        }

        const payload = req.body;
        const event = payload.event;

        // Exhaustive logging for duplicate events or other failure codes
        console.log(`[Billing] Received valid Webhook Event: ${event}`);

        if (event === 'payment.captured') {
            const paymentEntity = payload.payload.payment.entity;
            const paymentId = paymentEntity.id;
            
            // Custom user identifier metadata parameters
            const metadata = paymentEntity.notes;
            const userId = metadata?.user_id;

            if (!userId) {
                console.error(`[Billing] Missing user_id in payment notes for payment ${paymentId}`);
                // Return 200 so Razorpay stops retrying this fundamentally broken record
                return res.status(200).send('Ignored: Missing metadata');
            }

            const paymentRef = db.collection('processed_payments').doc(paymentId);
            const userRef = db.collection('users').doc(userId);

            // 3. Atomic Profile Elevation
            await db.runTransaction(async (transaction) => {
                const paymentDoc = await transaction.get(paymentRef);
                
                if (paymentDoc.exists) {
                    console.log(`[Billing] Duplicate webhook event detected for payment ${paymentId}. Skipping.`);
                    return; // Gracefully abort transaction, payment already processed
                }

                const userDoc = await transaction.get(userRef);
                if (!userDoc.exists) {
                    console.error(`[Billing] User ${userId} not found in database for payment ${paymentId}`);
                    return;
                }

                // Register payment to prevent duplicate processing (Idempotency)
                transaction.set(paymentRef, {
                    processed_at: admin.firestore.FieldValue.serverTimestamp(),
                    user_id: userId,
                    amount: paymentEntity.amount,
                    currency: paymentEntity.currency,
                    status: 'captured'
                });

                // Execute isolated update command to flip is_premium to true
                transaction.update(userRef, {
                    is_premium: true,
                    premium_granted_at: admin.firestore.FieldValue.serverTimestamp(),
                    last_payment_id: paymentId
                });
            });

            console.log(`[Billing] Successfully elevated profile for user ${userId} via payment ${paymentId}`);
        } else if (event === 'payment.failed') {
            const paymentEntity = payload.payload.payment.entity;
            console.warn(`[Billing] Payment failed: ${paymentEntity.id}. Reason: ${paymentEntity.error_description}`);
        }

        // Return explicit HTTP 200 to acknowledge receipt and close the loop
        return res.status(200).json({ status: 'ok' });

    } catch (error) {
        console.error('[Billing] Critical exception during webhook processing:', error);
        // Returning 500 signals to Razorpay to retry the webhook later
        return res.status(500).json({ error: 'Internal server error processing webhook' });
    }
});

export default app;
