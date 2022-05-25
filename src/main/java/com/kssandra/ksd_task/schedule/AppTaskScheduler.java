package com.kssandra.ksd_task.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppTaskScheduler {
	
	@Scheduled(cron = "${maintask.cron.expression}")
	public void scheduleTask() {
		System.out.println("I´m currently executing this task!");
	}

}
