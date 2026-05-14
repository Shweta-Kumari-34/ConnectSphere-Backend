package com.connectsphere.notification.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>SseEmitterService</h1>
 * <p>Handles real-time, server-sent events (SSE) to provide instant notification delivery 
 * to active browser sessions without polling.</p>
 * 
 * <h2>Real-time Broadcast Flow:</h2>
 * <pre>
 * graph LR
 *     Client[Frontend Browser] -->|Subscribe| Service[SseEmitterService]
 *     Service -->|Registry| Map[(ConcurrentHashMap)]
 *     App[Event Source] -->|Dispatch| Service
 *     Service -->|Lookup Key| Map
 *     Map -->|Push| Client
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Persistence:</b> Maintains a thread-safe registry of active emitters using {@link ConcurrentHashMap}.</li>
 *     <li><b>Self-Healing:</b> Automatically removes stale or timed-out emitters on completion or IO failure.</li>
 *     <li><b>Stateless:</b> Emitters are keyed by user identifiers (usually email) for targeted delivery.</li>
 *     <li><b>Reliability:</b> Handles client disconnects gracefully to prevent memory leaks.</li>
 * </ul>
 */
@Service
public class SseEmitterService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitters.put(key, emitter);
        return emitter;
    }

    public void sendEvent(String key, Object data) {
        SseEmitter e = emitters.get(key);
        if (e == null) return;
        try {
            e.send(data);
        } catch (IOException ex) {
            emitters.remove(key);
        }
    }

    public void sendEventToAll(Object data) {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try { entry.getValue().send(data); } catch (IOException ex) { emitters.remove(entry.getKey()); }
        }
    }
}
