package com.codingtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.codingtracker.model")
public class CodingtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodingtrackerApplication.class, args);
	}

}
