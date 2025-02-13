package com.example.speech.service;

import com.google.api.gax.rpc.ClientStream;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AudioCaptureService {

    private final SpeechRecognitionService speechRecognitionService;
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 6400;
    private volatile boolean isStreaming = false;

    public AudioCaptureService(SpeechRecognitionService speechRecognitionService) {
        this.speechRecognitionService = speechRecognitionService;
    }

    public void startStreaming(SseEmitter emitter) {
        if (isStreaming) {
            return; // 이미 스트리밍 중이라면 중복 실행 방지
        }
        isStreaming = true;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            TargetDataLine targetDataLine = null;
            try {
                // 오디오 설정
                AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
                targetDataLine.open(audioFormat);
                targetDataLine.start();

                // Google Speech-to-Text 스트림 시작
                ClientStream<StreamingRecognizeRequest> clientStream = speechRecognitionService.startRecognition(emitter);

                byte[] buffer = new byte[BUFFER_SIZE];
                while (isStreaming) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        speechRecognitionService.sendAudioData(clientStream, buffer, bytesRead);
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                if (targetDataLine != null) {
                    targetDataLine.stop();
                    targetDataLine.close();
                }
                isStreaming = false;
                executor.shutdown();
            }
        });
    }

    public void stopStreaming() {
        isStreaming = false; // 녹음 종료
    }
}
