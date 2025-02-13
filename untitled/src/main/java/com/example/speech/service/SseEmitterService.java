package com.example.speech.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter();
        emitter.onCompletion(() -> System.out.println("SSE connection completed."));
        emitter.onTimeout(() -> System.out.println("SSE connection timed out."));
        emitter.onError((ex) -> System.out.println("SSE connection error: " + ex.getMessage()));
        return emitter;
    }
}
