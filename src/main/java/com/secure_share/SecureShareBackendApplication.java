package com.secure_share;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableAsync
@EnableWebSocket
@SpringBootApplication
public class SecureShareBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureShareBackendApplication.class, args);
	}

}
