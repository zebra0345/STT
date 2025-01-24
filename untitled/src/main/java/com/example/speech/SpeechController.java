package com.example.speech;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/speech")
public class SpeechController {

    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 6400;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAudio() {
        SseEmitter emitter = new SseEmitter();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        final boolean[] isConnectionClosed = {false};

        // SSE 이벤트 핸들러
        emitter.onCompletion(() -> handleConnectionClosed("SSE connection completed by client.", isConnectionClosed));
        emitter.onTimeout(() -> handleConnectionClosed("SSE connection timed out.", isConnectionClosed));
        emitter.onError((ex) -> handleConnectionClosed("SSE connection error: " + ex.getMessage(), isConnectionClosed));

        executor.execute(() -> {
            TargetDataLine targetDataLine = null;
            try (SpeechClient speechClient = SpeechClient.create()) {
                // Streaming recognition setup
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(SAMPLE_RATE)
                        .setLanguageCode("ko-KR")
                        .build();

                StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                        .setConfig(config)
                        .setInterimResults(true)
                        .build();

                ClientStream<StreamingRecognizeRequest> clientStream =
                        speechClient.streamingRecognizeCallable().splitCall(new ResponseObserver<StreamingRecognizeResponse>() {
                            @Override
                            public void onStart(StreamController controller) {
                                System.out.println("SSE stream started.");
                            }

                            @Override
                            public void onResponse(StreamingRecognizeResponse response) {
                                for (StreamingRecognitionResult result : response.getResultsList()) {
                                    String transcript = result.getAlternativesList().get(0).getTranscript();
                                    try {
                                        emitter.send(transcript);
                                    } catch (IOException e) {
                                        System.err.println("Error sending SSE data: " + e.getMessage());
                                        emitter.completeWithError(e);
                                    }
                                }
                            }

                            @Override
                            public void onComplete() {
                                System.out.println("SSE stream completed.");
                                emitter.complete();
                            }

                            @Override
                            public void onError(Throwable t) {
                                System.err.println("SSE stream error: " + t.getMessage());
                                emitter.completeWithError(t);
                            }
                        });

                // 첫 번째 요청: StreamingRecognitionConfig를 전송
                StreamingRecognizeRequest initialRequest = StreamingRecognizeRequest.newBuilder()
                        .setStreamingConfig(streamingConfig)
                        .build();
                clientStream.send(initialRequest);

                // 마이크 입력 설정
                AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
                targetDataLine.open(audioFormat);
                targetDataLine.start();

                byte[] buffer = new byte[BUFFER_SIZE];
                while (!isConnectionClosed[0]) { // 연결 상태 확인
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        ByteString audioBytes = ByteString.copyFrom(buffer, 0, bytesRead);
                        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(audioBytes)
                                .build();
                        clientStream.send(request);
                    }
                }

                System.out.println("SSE connection disposed. Stopping microphone input.");
            } catch (Exception e) {
                System.err.println("Error in SSE stream: " + e.getMessage());
                emitter.completeWithError(e);
            } finally {
                // 리소스 정리
                if (targetDataLine != null) {
                    try {
                        targetDataLine.stop();
                        targetDataLine.close();
                        System.out.println("Microphone input stopped and closed.");
                    } catch (Exception e) {
                        System.err.println("Error closing TargetDataLine: " + e.getMessage());
                    }
                }
                executor.shutdown();
            }
        });

        return emitter;
    }

    private void handleConnectionClosed(String message, boolean[] isConnectionClosed) {
        System.out.println(message);
        isConnectionClosed[0] = true; // 연결 종료 상태 업데이트
    }
}
