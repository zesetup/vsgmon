package com.gmail.zerosetup.vsgmon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VsgtempApplication {

	public static void main(String[] args) {
		SpringApplication.run(VsgtempApplication.class, args);
	}

}
