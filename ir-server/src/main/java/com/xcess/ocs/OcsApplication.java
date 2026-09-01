package com.xcess.ocs;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaAuditing
@OpenAPIDefinition(
		info = @Info(
				title = "Rating Engine",
				version = "1.0",
				description = "API for Online Charging System",
				contact = @Contact(
						name = "OCS Team",
						email = "rudragdhamecha@gmail.com",
						url = "https://unifyxcess.ai"
				),
				license = @License(
						name = "Apache 2.0",
						url = "https://www.apache.org/licenses/LICENSE-2.0"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "Online Charging System Documentation",
				url = "http://198.38.87.170:8060/swagger-ui/index.html"
		)
)
public class OcsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OcsApplication.class, args);
		System.out.println("************************************************* Application INTERCONNECT ROAMING SERVER Started Successfully *************************************************");
	}

}