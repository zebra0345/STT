package com.example.speech;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner checkGoogleCredentials() {
        return args -> {
            System.out.println("✅ Spring Boot 애플리케이션 실행 시작...");

            // 환경 변수 확인
            String credentialsPath = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
            if (credentialsPath == null || credentialsPath.isEmpty()) {
                System.err.println("❌ GOOGLE_APPLICATION_CREDENTIALS 환경 변수가 설정되지 않았습니다.");
                System.exit(1); // 애플리케이션 강제 종료
            }

            System.out.println("✅ GOOGLE_APPLICATION_CREDENTIALS 설정 확인 완료: " + credentialsPath);
            System.out.println("🚀 Spring Boot 애플리케이션이 정상적으로 시작되었습니다!");
        };
    }
}
