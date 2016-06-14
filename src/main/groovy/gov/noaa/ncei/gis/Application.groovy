package gov.noaa.ncei.gis

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
class Application {

	static void main(String[] args) {
		SpringApplication.run Application, args
	}
}
