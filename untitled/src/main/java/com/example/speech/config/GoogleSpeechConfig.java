package com.example.speech.config;

import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class GoogleSpeechConfig {

    private static String googleCredentialsPath;
    private final ResourceLoader resourceLoader;

    @Value("${google.cloud.credentials.path}") // application.properties에서 값 가져오기
    private String credentialsPath;

    public GoogleSpeechConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public SpeechClient speechClient() throws IOException {
        System.out.println("✅ SpeechClient 생성 시작...");

        // 1️⃣ 환경 변수가 이미 설정되어 있는지 확인
        if (googleCredentialsPath == null) {
            System.out.println("🔄 환경 변수가 설정되지 않음. JSON 파일을 로드하여 환경 변수 설정 시작...");

            // 2️⃣ JSON 키 파일을 classpath에서 읽기
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                throw new IOException("❌ Google Cloud JSON 키 파일을 찾을 수 없습니다: " + credentialsPath);
            }

            // 3️⃣ 임시 파일로 변환 후 환경 변수 설정
            Path tempFile = Files.createTempFile("google-credentials", ".json");
            Files.copy(resource.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            googleCredentialsPath = tempFile.toAbsolutePath().toString();
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", googleCredentialsPath);

            System.out.println("✅ GOOGLE_APPLICATION_CREDENTIALS 설정 완료: " + googleCredentialsPath);
        } else {
            System.out.println("✅ 환경 변수가 이미 설정되어 있음: " + googleCredentialsPath);
        }

        // 4️⃣ SpeechClient 생성
        return SpeechClient.create(SpeechSettings.newBuilder().build());
    }
}
