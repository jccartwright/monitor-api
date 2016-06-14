package gov.noaa.ncei.gis.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class HealthCheckNotFoundException extends RuntimeException {
    HealthCheckNotFoundException(Long id ) {
        super("could not find HealthCheck ${id}.")
    }
}
