package com.kssandra.ksd_task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Test {

	public static void main(String[] args) {
		System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM")));

	}

}
