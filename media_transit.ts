import express, { Request, Response } from 'express';
import * as crypto from 'crypto';

const app = express();
app.use(express.raw({ type: 'image/*', limit: '10mb' }));

interface MediaPayload {
    id: string;
    buffer: Buffer;
    mimeType: string;
    createdAt: number;
}

// 1. Translucent Content Pipeline
const mediaBufferMap: Map<string, MediaPayload> = new Map();

// 2. Immediate AI Scan Layer (Mock Lightweight Filter)
const checkExplicitContent = async (buffer: Buffer): Promise<boolean> => {
    // Integrate cloud vision APIs here
    return true; // Assume safe for this simulation
};

app.post('/api/v1/media/transit', async (req: Request, res: Response): Promise<any> => {
    try {
        const buffer = req.body;
        if (!buffer || !Buffer.isBuffer(buffer)) {
            return res.status(400).json({ error: 'Invalid or missing media buffer' });
        }

        const isSafe = await checkExplicitContent(buffer);
        if (!isSafe) {
            return res.status(406).json({ error: 'NSFW_CONTENT_DETECTED' });
        }

        const id = crypto.randomUUID();
        mediaBufferMap.set(id, {
            id,
            buffer,
            mimeType: req.headers['content-type'] || 'image/jpeg',
            createdAt: Date.now()
        });

        // Fail-safe auto-purge
        setTimeout(() => {
            mediaBufferMap.delete(id);
        }, 5 * 60 * 1000);

        return res.status(200).json({
            status: 'TRANSIT_READY',
            url: `/api/v1/media/view/${id}`
        });

    } catch (error) {
        console.error("Media transit failed:", error);
        return res.status(500).json({ error: 'INTERNAL_SERVER_ERROR' });
    }
});

// View media endpoint
app.get('/api/v1/media/view/:id', (req: Request, res: Response): any => {
    const { id } = req.params;
    const payload = mediaBufferMap.get(id);

    if (!payload) {
        return res.status(404).send('Media not found or already purged.');
    }

    res.setHeader('Content-Type', payload.mimeType);
    return res.send(payload.buffer);
});

// 4. Auto-Purge Event Execution
app.post('/api/v1/media/purge/:id', (req: Request, res: Response): any => {
    const { id } = req.params;
    if (mediaBufferMap.has(id)) {
        mediaBufferMap.delete(id);
        return res.status(200).json({ status: 'PURGED' });
    }
    return res.status(404).json({ error: 'Not found or already purged' });
});

export default app;
