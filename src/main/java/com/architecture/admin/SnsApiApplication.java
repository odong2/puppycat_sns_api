package com.architecture.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
public class SnsApiApplication {
	public static void main(String[] args) {
		// 타임존 셋팅
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		Locale.setDefault(Locale.KOREA);

		SpringApplication.run(SnsApiApplication.class, args);
	}
}
