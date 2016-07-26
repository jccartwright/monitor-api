package gov.noaa.ncei.gis

import gov.noaa.ncei.gis.domain.HealthCheck
import gov.noaa.ncei.gis.domain.HealthCheckRepository
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
//@WebAppConfiguration
class ApplicationTests {

	@Autowired
    HealthCheckRepository healthCheckRepository

    @Autowired
    JdbcTemplate jdbcTemplate

	@Test
	void contextLoads() {
        Assert.assertNotNull("the HealthCheck repository should be non-null", this.healthCheckRepository)
        Assert.assertNotNull("the JdbcTemplate should be non-null", this.jdbcTemplate)
	}

}
