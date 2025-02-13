package com.example.speech.service;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class SpeechRecognitionService {

    private final SpeechClient speechClient;

    // GoogleSpeechConfig에서 Bean으로 주입받음
    public SpeechRecognitionService(SpeechClient speechClient) {
        this.speechClient = speechClient;
    }

    public ClientStream<StreamingRecognizeRequest> startRecognition(SseEmitter emitter) {
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
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
                        System.out.println("Google Speech-to-Text streaming started.");
                    }

                    @Override
                    public void onResponse(StreamingRecognizeResponse response) {
                        for (StreamingRecognitionResult result : response.getResultsList()) {
                            String transcript = result.getAlternativesList().get(0).getTranscript();
                            try {
                                emitter.send(transcript);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable t) {
                        emitter.completeWithError(t);
                    }
                });

        StreamingRecognizeRequest initialRequest = StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingConfig)
                .build();
        clientStream.send(initialRequest);

        return clientStream;
    }

    public void sendAudioData(ClientStream<StreamingRecognizeRequest> clientStream, byte[] buffer, int bytesRead) {
        ByteString audioBytes = ByteString.copyFrom(buffer, 0, bytesRead);
        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
                .setAudioContent(audioBytes)
                .build();
        clientStream.send(request);
    }
}
