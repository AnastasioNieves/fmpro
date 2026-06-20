package com.tmpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.tmpro"})
@EntityScan(basePackages = {"com.tmpro.model"})
@EnableJpaRepositories(basePackages = {"com.tmpro.repository"})
public class TmproApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmproApplication.class, args);
	}

}
