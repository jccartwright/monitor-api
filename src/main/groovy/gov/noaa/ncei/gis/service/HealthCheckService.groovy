package gov.noaa.ncei.gis.service

import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import gov.noaa.ncei.gis.domain.*

@Log4j
@Service
class HealthCheckService {
    private HealthCheckRepository healthCheckRepository

    @Autowired
    HealthCheckService(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository
    }


    void runCheck(Long id) {
        log.info "running check for HealthCheck ${id}..."
    }


    void runChecks(List<HealthCheck> checks) {
        if (checks.size() == 0) {
            log.warn "Empty list of HeathChecks received"
            return
        }
        checks.each {
            runCheck(it.id)
        }
    }
}
