package com.kssandra.ksd_task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kssandra.ksd_common.util.Generated;

@SpringBootApplication
@ComponentScan({ "com.kssandra" })
@EntityScan("com.kssandra.ksd_persistence.domain")
@EnableJpaRepositories("com.kssandra.ksd_persistence.repository")
@EnableScheduling
@Generated
public class KsdTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(KsdTaskApplication.class, args);
	}

}
