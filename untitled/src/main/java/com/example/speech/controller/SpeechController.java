package com.example.speech.controller;

import com.example.speech.service.AudioCaptureService;
import com.example.speech.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/speech")
public class SpeechController {

    private final AudioCaptureService audioCaptureService;
    private final SseEmitterService sseEmitterService;

    public SpeechController(AudioCaptureService audioCaptureService, SseEmitterService sseEmitterService) {
        this.audioCaptureService = audioCaptureService;
        this.sseEmitterService = sseEmitterService;
    }

    // 🎤 음성 인식 시작 (녹음 + SSE 스트리밍)
    @GetMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startSpeechRecognition() {
        SseEmitter emitter = sseEmitterService.createEmitter();
        audioCaptureService.startStreaming(emitter);
        return emitter;
    }

    // 🛑 음성 인식 종료 (녹음 중지)
    @PostMapping("/stop")
    public String stopSpeechRecognition() {
        audioCaptureService.stopStreaming();
        return "Speech recognition stopped.";
    }
}
