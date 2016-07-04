package gov.noaa.ncei.gis.service

import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import java.text.SimpleDateFormat;
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import gov.noaa.ncei.gis.domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Log4j
@Component
public class ScheduledTasksService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss")
    private HealthCheckService healthCheckService
    private HealthCheckRepository healthCheckRepository

    @Autowired
    ScheduledTasksService(HealthCheckService healthCheckService, HealthCheckRepository healthCheckRepository) {
        this.healthCheckService = healthCheckService
        this.healthCheckRepository = healthCheckRepository
    }

    @Scheduled(fixedRate = 5000L)
    public void reportCurrentTime() {
        println("The time is now ${dateFormat.format(new Date())}")
    }


    @Scheduled(cron="0 */5 * * * *")
    public void checkEvery5Minutes() {
        List<HealthCheck> checks = healthCheckRepository.findByCheckInterval(CheckIntervalEnum.FIVEMINUTES)
        if (checks.size == 0) {
            log.info("no 5 minute interval checks to run")
            return
        }

        healthCheckService.runChecks(checks)
    }


    @Scheduled(cron="0 */15 * * * *")
    public void checkEvery15Minutes() {
        List<HealthCheck> checks = healthCheckRepository.findByCheckInterval(CheckIntervalEnum.FIFTEENMINUTES)
        if (checks.size == 0) {
            log.info("no 15 minute interval checks to run")
            return
        }

        healthCheckService.runChecks(checks)
    }


    @Scheduled(cron="0 0 * * * *")
    public void checkHourly() {
        List<HealthCheck> checks = healthCheckRepository.findByCheckInterval(CheckIntervalEnum.HOURLY)
        if (checks.size == 0) {
            log.info("no hourly checks to run")
            return
        }

        healthCheckService.runChecks(checks)
    }

    @Scheduled(cron="0 0 0 * * *")
    public void checkDaily() {
        List<HealthCheck> checks = healthCheckRepository.findByCheckInterval(CheckIntervalEnum.DAILY)
        if (checks.size == 0) {
            log.info("no daily checks to run")
            return
        }

        healthCheckService.runChecks(checks)
    }

}