package com.example.hhplus_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync  // 비동기 메서드 활성화 (@Async)
@EnableScheduling  // 스케줄러 활성화 (@Scheduled)
@SpringBootApplication
public class HhplusEcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HhplusEcommerceApplication.class, args);
	}

}
