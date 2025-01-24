package com.example.speech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // 시스템 프로퍼티로 설정
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "C:/Users/SSAFY/Desktop/stt/untitled/src/main/resources/civic-wharf-442502-r6-2ea2d372c14d.json");

        // 시스템 프로퍼티 값 출력
        System.out.println("GOOGLE_APPLICATION_CREDENTIALS (Property): " + System.getProperty("GOOGLE_APPLICATION_CREDENTIALS"));

        // 운영 체제 환경 변수 값 출력
        System.out.println("GOOGLE_APPLICATION_CREDENTIALS (Env): " + System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

        SpringApplication.run(Application.class, args);
    }
}