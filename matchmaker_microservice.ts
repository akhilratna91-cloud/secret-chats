import express, { Request, Response } from 'express';
import * as crypto from 'crypto';

// --- Type Definitions ---
interface UserProfile {
    uid: string;
    username: string;
    gender: 'male' | 'female';
    preference: 'everyone' | 'male' | 'female';
    is_premium: boolean;
    status: 'idle' | 'searching' | 'chatting';
    matched_room_id?: string;
}

interface MatchSession {
    roomId: string;
    user1: UserProfile;
    user2: UserProfile;
    createdAt: number;
}

// 1. Volatile In-Memory Storage & State Architecture
class MemoryStorage {
    // Categorized arrays for optimized queue shifting
    activeQueues: {
        global: UserProfile[];
        male: UserProfile[];
        female: UserProfile[];
    } = {
        global: [],
        male: [],
        female: []
    };

    // Live active sessions
    liveSessions: Map<string, MatchSession> = new Map();

    // Registry of online users for tracking states and preventing duplicates
    users: Map<string, UserProfile> = new Map();
}

const storage = new MemoryStorage();
const app = express();
app.use(express.json());

// 2. Target Matchmaking Function
app.post('/api/v1/match/initiate', (req: Request, res: Response): any => {
    try {
        const { uid, username, gender, preference, is_premium } = req.body as UserProfile;

        if (!uid || !username || !gender || !preference) {
            return res.status(400).json({ error: 'Malformed request payload. Missing required identity fields.' });
        }

        // 3. Anti-Fraud Paywall Check
        if (preference !== 'everyone' && !is_premium) {
            return res.status(403).json({
                status: 'PAYWALL_TRIGGERED',
                gateway: 'RAZORPAY_INTENT',
                message: 'Premium subscription required to activate gender filters.'
            });
        }

        // Cleanup existing queue footprint if re-entering
        if (storage.users.has(uid)) {
            const existingUser = storage.users.get(uid)!;
            if (existingUser.status === 'searching') {
                storage.activeQueues.global = storage.activeQueues.global.filter(u => u.uid !== uid);
                storage.activeQueues.male = storage.activeQueues.male.filter(u => u.uid !== uid);
                storage.activeQueues.female = storage.activeQueues.female.filter(u => u.uid !== uid);
            }
        }

        const currentUser: UserProfile = {
            uid,
            username,
            gender,
            preference,
            is_premium,
            status: 'searching'
        };

        storage.users.set(uid, currentUser);

        // 4. Cross-Matching Concurrency Guard
        let matchFound: UserProfile | undefined = undefined;

        // Resolve which queue to search based on target preference
        // If preference is 'everyone', we search all pools (or a dedicated global pool).
        let targetQueue: UserProfile[];
        if (preference === 'everyone') {
            targetQueue = [
                ...storage.activeQueues.global,
                ...storage.activeQueues.male,
                ...storage.activeQueues.female
            ];
        } else {
            targetQueue = storage.activeQueues[preference]; // 'male' or 'female'
        }

        // Dynamic array shifting with mutual alignment verification
        for (let i = 0; i < targetQueue.length; i++) {
            const potentialMatch = targetQueue[i];

            // Validation 1: Concurrency Guard - Verify they are still searching
            if (potentialMatch.status !== 'searching' || potentialMatch.uid === currentUser.uid) {
                continue;
            }

            // Validation 2: Mutual Alignment
            // The potential match must either prefer 'everyone' or explicitly match the currentUser's gender
            const isMutual = potentialMatch.preference === 'everyone' || potentialMatch.preference === currentUser.gender;

            if (isMutual) {
                // Atomic Lock Acquired
                matchFound = potentialMatch;
                
                // Atomically remove matched user from their queue
                const queueToRemoveFrom = potentialMatch.preference === 'everyone' ? 'global' : potentialMatch.gender;
                storage.activeQueues[queueToRemoveFrom] = storage.activeQueues[queueToRemoveFrom].filter(u => u.uid !== matchFound!.uid);
                
                break;
            }
        }

        // Finalize transaction
        if (matchFound) {
            const roomId = crypto.randomUUID(); // Fast cryptographic UUID

            // Assign identical transaction states
            currentUser.status = 'chatting';
            currentUser.matched_room_id = roomId;
            
            matchFound.status = 'chatting';
            matchFound.matched_room_id = roomId;

            // Commit to live sessions map
            storage.liveSessions.set(roomId, {
                roomId,
                user1: currentUser,
                user2: matchFound,
                createdAt: Date.now()
            });

            return res.status(200).json({
                status: 'MATCH_ESTABLISHED',
                roomId,
                peer: {
                    uid: matchFound.uid,
                    username: matchFound.username,
                    gender: matchFound.gender
                }
            });
        } else {
            // Re-queue explicitly based on own preference constraints
            const ownQueue = currentUser.preference === 'everyone' ? 'global' : currentUser.gender;
            storage.activeQueues[ownQueue].push(currentUser);

            return res.status(202).json({
                status: 'SEARCHING_QUEUED',
                pool_size: targetQueue.length
            });
        }

    } catch (error) {
        console.error("Critical fault in /api/v1/match/initiate:", error);
        return res.status(500).json({ status: 'INTERNAL_SERVER_ERROR' });
    }
});

// 5. Ephemeral Zero-Cost Flush
app.post('/api/v1/match/terminate', (req: Request, res: Response): any => {
    try {
        const { roomId, initiator_uid } = req.body;

        if (!roomId) {
            return res.status(400).json({ error: 'Missing roomId parameters for termination payload.' });
        }

        const session = storage.liveSessions.get(roomId);
        if (!session) {
            return res.status(404).json({ error: 'Active session instance not found or already terminated.' });
        }

        // Reset both interconnected user states back to idle
        [session.user1.uid, session.user2.uid].forEach(targetUid => {
            const user = storage.users.get(targetUid);
            if (user) {
                user.status = 'idle';
                user.matched_room_id = undefined;
            }
        });

        // Completely purge the session map item
        storage.liveSessions.delete(roomId);

        return res.status(200).json({
            status: 'SESSION_TERMINATED',
            message: 'Memory buffers successfully flushed. Zero persistent state remaining.',
            terminated_at: Date.now()
        });

    } catch (error) {
        console.error("Critical fault in /api/v1/match/terminate:", error);
        return res.status(500).json({ status: 'INTERNAL_SERVER_ERROR' });
    }
});

export default app;
