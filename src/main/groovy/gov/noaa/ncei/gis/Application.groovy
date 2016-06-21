package gov.noaa.ncei.gis

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
class Application {

	static void main(String[] args) {
		SpringApplication.run Application, args
	}
}
