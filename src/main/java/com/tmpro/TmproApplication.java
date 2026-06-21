package com.tmpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.tmpro"})
public class TmproApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmproApplication.class, args);
	}
}
