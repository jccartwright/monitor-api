package gov.noaa.ncei.gis.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class HealthCheckLastResponseNotFoundException extends RuntimeException {
    HealthCheckLastResponseNotFoundException(Long id) {
        super("could not find lastResponse for HealthCheck ${id}.")
    }
}
