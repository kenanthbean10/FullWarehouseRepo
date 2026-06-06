package com.example.Streaming.Client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients
public class StreamingClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamingClientApplication.class, args);
	}

}
