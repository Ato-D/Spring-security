package com.example.SpringSecurity;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
		title = "USER SERVICE API",
		version = "1.0",
		description = "This service handles the management of all system users i.e. create, read, update and delete operations on users and roles information",
		contact = @io.swagger.v3.oas.annotations.info.Contact(
				name = "Derrick Donkoh",
				email = "derrickdo@stlghana.com"
		),
		termsOfService = "http://swagger.io/terms/",
		license = @io.swagger.v3.oas.annotations.info.License(
				name = "Apache 2.0",
				url = "http://springdoc.org"
		)
))
public class SpringSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityApplication.class, args);
	}

}
