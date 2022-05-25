package com.kssandra.ksd_task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KsdTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(KsdTaskApplication.class, args);
	}

}
